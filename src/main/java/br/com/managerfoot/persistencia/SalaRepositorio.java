package br.com.managerfoot.persistencia;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.contas.AcessoNegadoException;
import br.com.managerfoot.contas.Papel;
import br.com.managerfoot.contas.TokenCadastro;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.multiplayer.ModoRede;
import br.com.managerfoot.multiplayer.Sala;
import br.com.managerfoot.multiplayer.Sala.ClubeNaSala;
import br.com.managerfoot.multiplayer.Sala.Participante;
import br.com.managerfoot.temporada.ClubeInicial;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;

/**
 * Sala do jogo em rede (SPEC §3.3, rev. 90 e 92; docs/decisions/0020): abrir, convidar por link, entrar,
 * escolher o clube, tirar alguém e começar a temporada. Tudo o que muda a sala trava o jogo, para dois
 * pedidos ao mesmo tempo não darem o mesmo clube a dois treinadores nem começarem a temporada duas vezes.
 */
@Repository
public class SalaRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final CaixaRepositorio caixas;
	private final AumentoRepositorio aumentos;
	private final MercadoRepositorio mercado;
	private final ContasRepositorio contas;
	private final int candidatosClubeInicial;
	private final Duration validadeConvite;

	public SalaRepositorio(JdbcClient jdbc, JogoRepositorio jogos, CaixaRepositorio caixas, AumentoRepositorio aumentos,
			MercadoRepositorio mercado, ContasRepositorio contas,
			@Value("${managerfoot.jogo.candidatos-clube-inicial}") int candidatosClubeInicial,
			@Value("${managerfoot.rede.validade-convite}") Duration validadeConvite) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.caixas = caixas;
		this.aumentos = aumentos;
		this.mercado = mercado;
		this.contas = contas;
		this.candidatosClubeInicial = candidatosClubeInicial;
		this.validadeConvite = validadeConvite;
	}

	/**
	 * Põe na sala um jogo recém-criado, ainda sem patrocínio nem salários: eles só são fixados ao começar
	 * a temporada, quando se sabe que clubes têm usuário. O dono entra na sala com o clube que já tem.
	 *
	 * @throws AcessoNegadoException se o dono do jogo não é administrador
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void abrir(long jogoId, ModoRede modo, Instant agora) {
		long dono = jdbc.sql("SELECT dono_id FROM jogo WHERE id = :jogo").param("jogo", jogoId).query(Long.class).single();
		if (contas.buscar(dono).orElseThrow().papel() != Papel.ADMINISTRADOR) {
			throw new AcessoNegadoException("só o administrador cria jogo em rede");
		}
		jdbc.sql("INSERT INTO jogo_rede (jogo_id, modo) VALUES (:jogo, :modo)")
			.param("jogo", jogoId)
			.param("modo", modo.name())
			.update();
		inserirConvite(jogoId, agora);
		inserirParticipante(jogoId, dono, agora);
	}

	/** Jogo em rede que ainda não começou: a rodada espera o administrador começar a temporada. */
	@Transactional(readOnly = true)
	public boolean naSala(long jogoId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_rede WHERE jogo_id = :jogo AND comecou_em IS NULL)")
			.param("jogo", jogoId)
			.query(Boolean.class)
			.single();
	}

	/** O navegador do participante consultou o servidor: está conectado. */
	@Transactional
	public void marcarVisto(long jogoId, long usuarioId, Instant agora) {
		jdbc.sql("UPDATE jogo_rede_participante SET visto_em = :agora WHERE jogo_id = :jogo AND usuario_id = :usuario")
			.param("agora", hora(agora))
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.update();
	}

	/**
	 * A sala vista por um participante. Vazio se o jogo não é em rede ou se ele não está na sala: a tela
	 * não distingue os casos.
	 */
	@Transactional(readOnly = true)
	public Optional<VisaoSala> visao(long jogoId, long usuarioId) {
		Optional<Cabecalho> cabecalho = cabecalho(jogoId);
		if (cabecalho.isEmpty()) {
			return Optional.empty();
		}
		Sala sala = sala(jogoId, cabecalho.get());
		if (sala.participantes().stream().noneMatch(p -> p.usuarioId() == usuarioId)) {
			return Optional.empty();
		}
		boolean administrador = cabecalho.get().donoId() == usuarioId;
		Optional<ConviteAtual> convite = administrador ? conviteAtual(jogoId) : Optional.empty();
		return Optional.of(new VisaoSala(sala, cabecalho.get().pais(), cabecalho.get().ano(), cabecalho.get().divisoes(),
				cabecalho.get().comecou(), administrador, convite));
	}

	/**
	 * Gera um link novo; o anterior deixa de valer (SPEC §3.3, rev. 92).
	 *
	 * @return o token novo
	 * @throws AcessoNegadoException se quem pede não é o administrador do jogo
	 * @throws SalaRecusadaException se a temporada já começou
	 */
	@Transactional
	public String novoConvite(long jogoId, long administradorId, Instant agora) {
		exigirSalaDoAdministrador(jogoId, administradorId);
		jdbc.sql("UPDATE jogo_rede_convite SET trocado_em = :agora WHERE jogo_id = :jogo AND trocado_em IS NULL")
			.param("agora", hora(agora))
			.param("jogo", jogoId)
			.update();
		return inserirConvite(jogoId, agora);
	}

	/**
	 * O administrador tira alguém da sala antes de começar (SPEC §3.3, rev. 92). O clube volta à máquina,
	 * com o treinador dela. Pedido repetido não faz nada.
	 *
	 * @throws AcessoNegadoException se quem pede não é o administrador do jogo
	 * @throws SalaRecusadaException se a temporada já começou ou se ele tenta tirar a si mesmo
	 */
	@Transactional
	public void tirar(long jogoId, long administradorId, long usuarioId) {
		exigirSalaDoAdministrador(jogoId, administradorId);
		if (usuarioId == administradorId) {
			throw new SalaRecusadaException("O administrador não sai da própria sala.");
		}
		jogos.clubeDoTreinador(jogoId, usuarioId).ifPresent(clube -> {
			// clube da máquina não tem escalação gravada (SPEC §10.3, rev. 34)
			jdbc.sql("DELETE FROM jogo_escalacao WHERE clube_id = :clube").param("clube", clube).update();
			jdbc.sql("DELETE FROM jogo_treinador WHERE jogo_id = :jogo AND usuario_id = :usuario")
				.param("jogo", jogoId)
				.param("usuario", usuarioId)
				.update();
		});
		jdbc.sql("DELETE FROM jogo_rede_participante WHERE jogo_id = :jogo AND usuario_id = :usuario")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.update();
	}

	/**
	 * Começa a temporada: fixa o patrocínio, os salários e os jogadores à venda pelos clubes que agora têm
	 * usuário. Depois disso ninguém entra nem sai (SPEC §3.3, rev. 90). Pedido repetido não faz nada.
	 *
	 * @throws AcessoNegadoException se quem pede não é o administrador do jogo
	 * @throws SalaRecusadaException se falta alguém ficar pronto, com o que falta
	 */
	@Transactional
	public void comecar(long jogoId, long administradorId, Instant agora, Duration semConexaoApos) {
		Cabecalho cabecalho = cabecalho(jogoId).orElseThrow(() -> new AcessoNegadoException("o jogo não é em rede"));
		if (cabecalho.donoId() != administradorId) {
			throw new AcessoNegadoException("só o administrador começa a temporada");
		}
		jogos.travar(jogoId);
		if (!naSala(jogoId)) {
			return;
		}
		Sala sala = sala(jogoId, cabecalho);
		if (!sala.podeComecar(agora, semConexaoApos)) {
			throw new SalaRecusadaException("Ainda falta: " + sala.falta(agora, semConexaoApos) + ".");
		}
		caixas.pagarPatrocinio(jogoId, Map.of());
		aumentos.fixarSalarios(jogoId);
		long semente = jdbc.sql("SELECT semente FROM jogo_temporada WHERE jogo_id = :jogo AND numero = 1")
			.param("jogo", jogoId)
			.query(Long.class)
			.single();
		// quem entrou na sala pegou clube da máquina, que pode ter jogadores anunciados
		mercado.reporAVenda(jogoId, MercadoRepositorio.semente(semente, 0));
		jdbc.sql("UPDATE jogo_rede SET comecou_em = :agora WHERE jogo_id = :jogo")
			.param("agora", hora(agora))
			.param("jogo", jogoId)
			.update();
	}

	/**
	 * O convite visto por quem abriu o link. Vazio se o link não existe.
	 */
	@Transactional(readOnly = true)
	public Optional<Convite> convite(String token, long usuarioId, Instant agora) {
		Optional<LinhaConvite> linha = linhaConvite(token);
		if (linha.isEmpty()) {
			return Optional.empty();
		}
		long jogoId = linha.get().jogoId();
		Cabecalho cabecalho = cabecalho(jogoId).orElseThrow();
		Sala sala = sala(jogoId, cabecalho);
		if (sala.participantes().stream().anyMatch(p -> p.usuarioId() == usuarioId)) {
			return Optional.of(new Convite.JaNaSala(jogoId));
		}
		Optional<Convite.Motivo> motivo = motivo(linha.get(), cabecalho, agora);
		if (motivo.isPresent()) {
			return Optional.of(new Convite.Invalido(cabecalho.administrador(), motivo.get(), linha.get().valeAte()));
		}
		long comClube = sala.participantes().stream().filter(p -> p.clube().isPresent()).count();
		return Optional.of(new Convite.Valido(jogoId, cabecalho.administrador(), cabecalho.pais(), cabecalho.divisoes(),
				cabecalho.modo(), sala.participantes().stream().map(Participante::nome).toList(),
				Math.toIntExact(candidatosClubeInicial + comClube)));
	}

	/**
	 * Entra na sala pelo link. No modo sorteio recebe o clube na hora (SPEC §3.3, rev. 92); no modo
	 * escolha fica na sala sem clube até escolher. Quem já está na sala só volta para ela.
	 *
	 * @return o id do jogo
	 * @throws SalaRecusadaException se o link não vale mais ou não há clube livre para sortear
	 */
	@Transactional
	public long entrar(String token, long usuarioId, Instant agora) {
		LinhaConvite linha = linhaConvite(token).orElseThrow(() -> new SalaRecusadaException("Este convite não existe."));
		long jogoId = linha.jogoId();
		jogos.travar(jogoId);
		Cabecalho cabecalho = cabecalho(jogoId).orElseThrow();
		if (estaNaSala(jogoId, usuarioId)) {
			return jogoId;
		}
		Optional<Convite.Motivo> motivo = motivo(linha, cabecalho, agora);
		if (motivo.isPresent()) {
			throw new SalaRecusadaException(motivo.get().texto());
		}
		contas.buscar(usuarioId).orElseThrow(() -> new AcessoNegadoException("usuário " + usuarioId + " não existe"));
		inserirParticipante(jogoId, usuarioId, agora);
		if (cabecalho.modo() == ModoRede.SORTEIO) {
			Set<Long> ocupados = ocupados(jogoId);
			Divisao ultima = jogos.carregarLiga(jogoId).divisoes().getLast();
			List<Clube> candidatos = ClubeInicial.candidatos(ultima, candidatosClubeInicial, ocupados);
			if (candidatos.isEmpty()) {
				throw new SalaRecusadaException("Não há mais clube livre para sortear na " + ultima.nome() + ".");
			}
			// semente do jogo e quantos já têm clube: o sorteio de cada entrada pode ser refeito
			Clube clube = candidatos.get(new SplittableRandom(~cabecalho.semente() + ocupados.size()).nextInt(candidatos.size()));
			jogos.inserirTreinador(jogoId, usuarioId, clube.id());
		}
		return jogoId;
	}

	/**
	 * O participante sem clube escolhe um (modo escolha). O clube precisa estar numa divisão e sem
	 * usuário; se dois escolherem o mesmo, fica com quem chegou primeiro (SPEC §3.2, rev. 29).
	 *
	 * @throws SalaRecusadaException se o clube não pode ser escolhido, com o motivo
	 */
	@Transactional
	public void escolher(long jogoId, long usuarioId, long clubeId) {
		jogos.travar(jogoId);
		Cabecalho cabecalho = cabecalho(jogoId).orElseThrow(() -> new SalaRecusadaException("Este jogo não é em rede."));
		if (!estaNaSala(jogoId, usuarioId)) {
			throw new SalaRecusadaException("Você não está nesta sala.");
		}
		if (cabecalho.comecou()) {
			throw new SalaRecusadaException("A temporada já começou.");
		}
		if (cabecalho.modo() != ModoRede.ESCOLHA) {
			throw new SalaRecusadaException("Neste jogo o clube é sorteado.");
		}
		if (jogos.clubeDoTreinador(jogoId, usuarioId).isPresent()) {
			throw new SalaRecusadaException("Você já tem clube neste jogo.");
		}
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		if (liga.divisoes().stream().flatMap(d -> d.clubes().stream()).noneMatch(c -> c.id() == clubeId)) {
			throw new SalaRecusadaException("Escolha um clube de divisão.");
		}
		if (ocupados(jogoId).contains(clubeId)) {
			throw new SalaRecusadaException("Esse clube já tem treinador. Escolha outro.");
		}
		jogos.inserirTreinador(jogoId, usuarioId, clubeId);
	}

	/** Clubes que já têm treinador usuário no jogo. */
	@Transactional(readOnly = true)
	public Set<Long> ocupados(long jogoId) {
		return new HashSet<>(jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query(Long.class)
			.list());
	}

	private Optional<Convite.Motivo> motivo(LinhaConvite linha, Cabecalho cabecalho, Instant agora) {
		if (cabecalho.comecou()) {
			return Optional.of(Convite.Motivo.COMECOU);
		}
		if (linha.trocado()) {
			return Optional.of(Convite.Motivo.TROCADO);
		}
		if (!agora.isBefore(linha.valeAte())) {
			return Optional.of(Convite.Motivo.VENCEU);
		}
		return Optional.empty();
	}

	private void exigirSalaDoAdministrador(long jogoId, long administradorId) {
		jogos.travar(jogoId);
		Cabecalho cabecalho = cabecalho(jogoId).orElseThrow(() -> new AcessoNegadoException("o jogo não é em rede"));
		if (cabecalho.donoId() != administradorId) {
			throw new AcessoNegadoException("só o administrador muda a sala");
		}
		if (cabecalho.comecou()) {
			throw new SalaRecusadaException("A temporada já começou: ninguém mais entra nem sai.");
		}
	}

	private boolean estaNaSala(long jogoId, long usuarioId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_rede_participante WHERE jogo_id = :jogo AND usuario_id = :usuario)")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.query(Boolean.class)
			.single();
	}

	private String inserirConvite(long jogoId, Instant agora) {
		String token = TokenCadastro.gerar();
		jdbc.sql("""
				INSERT INTO jogo_rede_convite (token, jogo_id, criado_em, vale_ate)
				VALUES (:token, :jogo, :agora, :vale)""")
			.param("token", token)
			.param("jogo", jogoId)
			.param("agora", hora(agora))
			.param("vale", hora(agora.plus(validadeConvite)))
			.update();
		return token;
	}

	private void inserirParticipante(long jogoId, long usuarioId, Instant agora) {
		jdbc.sql("""
				INSERT INTO jogo_rede_participante (jogo_id, usuario_id, entrou_em, visto_em)
				VALUES (:jogo, :usuario, :agora, :agora)""")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.param("agora", hora(agora))
			.update();
	}

	private Optional<ConviteAtual> conviteAtual(long jogoId) {
		return jdbc.sql("SELECT token, vale_ate FROM jogo_rede_convite WHERE jogo_id = :jogo AND trocado_em IS NULL")
			.param("jogo", jogoId)
			.query((rs, n) -> new ConviteAtual(rs.getString("token"), rs.getObject("vale_ate", OffsetDateTime.class).toInstant()))
			.optional();
	}

	private Optional<LinhaConvite> linhaConvite(String token) {
		if (token == null || token.isBlank() || token.length() > 100) {
			return Optional.empty();
		}
		return jdbc.sql("SELECT jogo_id, vale_ate, trocado_em IS NOT NULL AS trocado FROM jogo_rede_convite WHERE token = :token")
			.param("token", token)
			.query((rs, n) -> new LinhaConvite(rs.getLong("jogo_id"), rs.getObject("vale_ate", OffsetDateTime.class).toInstant(),
					rs.getBoolean("trocado")))
			.optional();
	}

	private Optional<Cabecalho> cabecalho(long jogoId) {
		return jdbc.sql("""
				SELECT j.dono_id, u.nome AS administrador, r.modo, r.comecou_em IS NOT NULL AS comecou, p.nome AS pais,
				       j.ano_inicial, j.semente_criacao,
				       (SELECT count(*) FROM jogo_divisao d WHERE d.jogo_id = j.id) AS divisoes
				FROM jogo_rede r
				JOIN jogo j ON j.id = r.jogo_id
				JOIN usuario u ON u.id = j.dono_id
				JOIN pais_fifa p ON p.codigo = j.pais_codigo
				WHERE r.jogo_id = :jogo""")
			.param("jogo", jogoId)
			.query((rs, n) -> new Cabecalho(rs.getLong("dono_id"), rs.getString("administrador"), ModoRede.valueOf(rs.getString("modo")),
					rs.getBoolean("comecou"), rs.getString("pais"), rs.getInt("ano_inicial"), rs.getLong("semente_criacao"),
					rs.getInt("divisoes")))
			.optional();
	}

	private Sala sala(long jogoId, Cabecalho cabecalho) {
		List<Participante> participantes = jdbc.sql("""
				SELECT p.usuario_id, u.nome AS treinador, p.visto_em, d.nome AS divisao,
				       c.id, c.nome, c.forca, c.escudo_modelo, c.escudo_cor_1, c.escudo_cor_2
				FROM jogo_rede_participante p
				JOIN usuario u ON u.id = p.usuario_id
				LEFT JOIN jogo_treinador t ON t.jogo_id = p.jogo_id AND t.usuario_id = p.usuario_id
				LEFT JOIN jogo_clube c ON c.id = t.clube_id
				LEFT JOIN jogo_divisao d ON d.id = c.divisao_id
				WHERE p.jogo_id = :jogo
				ORDER BY p.usuario_id <> :dono, p.entrou_em, p.usuario_id""")
			.param("jogo", jogoId)
			.param("dono", cabecalho.donoId())
			.query((rs, n) -> {
				long usuario = rs.getLong("usuario_id");
				rs.getLong("id");
				Optional<ClubeNaSala> clube = rs.wasNull() ? Optional.empty()
						: Optional.of(new ClubeNaSala(TabelasLiga.clube(rs), rs.getString("divisao")));
				return new Participante(usuario, rs.getString("treinador"), usuario == cabecalho.donoId(), clube,
						rs.getObject("visto_em", OffsetDateTime.class).toInstant());
			})
			.list();
		return new Sala(jogoId, cabecalho.administrador(), cabecalho.modo(), participantes);
	}

	private static OffsetDateTime hora(Instant instante) {
		return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
	}

	private record Cabecalho(long donoId, String administrador, ModoRede modo, boolean comecou, String pais, int ano,
			long semente, int divisoes) {
	}

	private record LinhaConvite(long jogoId, Instant valeAte, boolean trocado) {
	}

	/** O link que vale agora, que o administrador vê na sala. */
	public record ConviteAtual(String token, Instant valeAte) {
	}

	/**
	 * A sala vista por um participante.
	 *
	 * @param convite o link atual; só o administrador vê
	 */
	public record VisaoSala(Sala sala, String pais, int ano, int divisoes, boolean comecou, boolean administrador,
			Optional<ConviteAtual> convite) {
	}

	/** O que quem abriu o link vê (modelo 18d). */
	public sealed interface Convite {

		/**
		 * @param grupoSorteio quantos clubes mais fracos entram no sorteio de quem entrar agora
		 */
		record Valido(long jogoId, String administrador, String pais, int divisoes, ModoRede modo, List<String> naSala,
				int grupoSorteio) implements Convite {
		}

		record JaNaSala(long jogoId) implements Convite {
		}

		record Invalido(String administrador, Motivo motivo, Instant valeAte) implements Convite {
		}

		enum Motivo {
			COMECOU("A temporada já começou. Só dá para entrar antes da 1ª rodada."),
			VENCEU("O convite venceu."),
			TROCADO("O administrador gerou um link novo. Peça o link atual.");

			private final String texto;

			Motivo(String texto) {
				this.texto = texto;
			}

			public String texto() {
				return texto;
			}
		}
	}
}
