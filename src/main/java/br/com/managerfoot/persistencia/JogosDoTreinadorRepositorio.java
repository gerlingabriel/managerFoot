package br.com.managerfoot.persistencia;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.multiplayer.ModoRede;
import br.com.managerfoot.temporada.ClubeInicial;

/**
 * Jogos vistos pelo treinador humano (SPEC §10.5, rev. 29): criar um jogo novo já com a 1ª
 * temporada, listar os jogos gravados e abrir um deles. Um usuário só enxerga os jogos em que é
 * treinador.
 */
@Repository
public class JogosDoTreinadorRepositorio {

	private static final SecureRandom SEMENTES = new SecureRandom();

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final TemporadaRepositorio temporadas;
	private final CaixaRepositorio caixas;
	private final AumentoRepositorio aumentos;
	private final SalaRepositorio salas;
	private final int candidatosClubeInicial;

	public JogosDoTreinadorRepositorio(JdbcClient jdbc, JogoRepositorio jogos, TemporadaRepositorio temporadas,
			CaixaRepositorio caixas, AumentoRepositorio aumentos, SalaRepositorio salas,
			@Value("${managerfoot.jogo.candidatos-clube-inicial}") int candidatosClubeInicial) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.temporadas = temporadas;
		this.caixas = caixas;
		this.aumentos = aumentos;
		this.salas = salas;
		this.candidatosClubeInicial = candidatosClubeInicial;
	}

	/** Jogo novo com clube sorteado e a 1ª temporada iniciada, com uma semente aleatória. */
	@Transactional
	public NovoJogo criarComSorteio(long paisMoldeId, long usuarioId) {
		return criarComSorteio(paisMoldeId, usuarioId, SEMENTES.nextLong());
	}

	/**
	 * Como {@link #criarComSorteio(long, long)}, com a semente dada: ela decide a distribuição, o
	 * clube e, por uma semente derivada, os sorteios da temporada. Devolve também os clubes que
	 * concorreram no sorteio, para a tela mostrar.
	 */
	@Transactional
	public NovoJogo criarComSorteio(long paisMoldeId, long usuarioId, long semente) {
		return criarComSorteio(paisMoldeId, usuarioId, semente, null);
	}

	/**
	 * Jogo em rede no modo sorteio (SPEC §3.3, rev. 92): o administrador recebe o clube sorteado e o jogo
	 * fica na sala até ele começar a temporada.
	 */
	@Transactional
	public NovoJogo criarEmRedeComSorteio(long paisMoldeId, long administradorId, Instant agora) {
		return criarComSorteio(paisMoldeId, administradorId, SEMENTES.nextLong(), agora);
	}

	/** @param salaAbertaEm nulo no jogo individual */
	private NovoJogo criarComSorteio(long paisMoldeId, long usuarioId, long semente, Instant salaAbertaEm) {
		long jogoId = jogos.criarJogoComClubeSorteado(paisMoldeId, usuarioId, semente);
		// os candidatos saem da última divisão como ela foi criada, antes de qualquer rodada
		List<Clube> candidatos = ClubeInicial.candidatos(jogos.carregarLiga(jogoId).divisoes().getLast(),
				candidatosClubeInicial);
		iniciar(jogoId, semente, salaAbertaEm == null ? null : ModoRede.SORTEIO, salaAbertaEm);
		return new NovoJogo(jogoId, candidatos);
	}

	/**
	 * Jogo novo com o clube escolhido pelo administrador no quadro de
	 * {@link JogoRepositorio#distribuicao} da mesma semente, e a 1ª temporada iniciada.
	 *
	 * @param clubeMoldeId id do clube no molde
	 */
	@Transactional
	public long criarComEscolha(long paisMoldeId, long administradorId, long semente, long clubeMoldeId) {
		long jogoId = jogos.criarJogoComClubeEscolhido(paisMoldeId, administradorId, semente, clubeMoldeId);
		iniciar(jogoId, semente, null, null);
		return jogoId;
	}

	/** Como {@link #criarComEscolha}, num jogo em rede no modo escolha, que fica na sala. */
	@Transactional
	public long criarEmRedeComEscolha(long paisMoldeId, long administradorId, long semente, long clubeMoldeId, Instant agora) {
		long jogoId = jogos.criarJogoComClubeEscolhido(paisMoldeId, administradorId, semente, clubeMoldeId);
		iniciar(jogoId, semente, ModoRede.ESCOLHA, agora);
		return jogoId;
	}

	/**
	 * Sorteia a 1ª temporada. O jogo individual já fixa patrocínio e salários; o jogo em rede abre a sala e
	 * deixa os dois para quando a temporada começar, com todos os treinadores nos clubes.
	 */
	private void iniciar(long jogoId, long semente, ModoRede rede, Instant agora) {
		temporadas.iniciarPrimeiraTemporada(jogoId, new SplittableRandom(semente).nextLong());
		if (rede == null) {
			caixas.pagarPatrocinio(jogoId, Map.of());
			aumentos.fixarSalarios(jogoId);
		} else {
			salas.abrir(jogoId, rede, agora);
		}
	}

	/** Jogos em que o usuário é treinador, do último acesso para o mais antigo. */
	@Transactional(readOnly = true)
	public List<JogoGravado> listar(long usuarioId) {
		return jdbc.sql(SELECAO + " WHERE t.usuario_id = :usuario ORDER BY t.ultimo_acesso_em DESC, j.id DESC")
			.param("usuario", usuarioId)
			.query((rs, n) -> new JogoGravado(rs.getLong("jogo_id"), TabelasLiga.clube(rs), rs.getString("pais"),
					rs.getString("divisao"), rs.getInt("ano"), rodadaAtual(rs.getInt("processadas"),
							rs.getInt("total")),
					rs.getObject("ultimo_acesso_em", OffsetDateTime.class).toInstant(),
					rs.getBoolean("em_rede") ? Optional.of(new Rede(rs.getString("administrador"), rs.getLong("dono_id") == usuarioId,
							rs.getBoolean("na_sala"), rs.getInt("na_sala_treinadores"))) : Optional.empty()))
			.list();
	}

	/**
	 * O jogo do usuário, registrando o acesso. Vazio se o jogo não existe ou se o usuário não é
	 * treinador nele: a tela não distingue os dois casos.
	 */
	@Transactional
	public Optional<JogoGravado> abrir(long jogoId, long usuarioId) {
		int atualizados = jdbc.sql("""
				UPDATE jogo_treinador SET ultimo_acesso_em = now() WHERE jogo_id = :jogo AND usuario_id = :usuario""")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.update();
		if (atualizados == 0) {
			return Optional.empty();
		}
		return listar(usuarioId).stream().filter(j -> j.jogoId() == jogoId).findFirst();
	}

	/** Rodada em que o jogo está: a próxima a jogar, ou a última quando todas já foram jogadas. */
	private static int rodadaAtual(int processadas, int total) {
		return Math.max(1, Math.min(processadas + 1, total));
	}

	/** Uma linha por jogo do treinador; total de rodadas = as da divisão com mais rodadas. */
	private static final String SELECAO = """
			SELECT j.id AS jogo_id, c.id, c.nome, c.forca, c.escudo_modelo, c.escudo_cor_1, c.escudo_cor_2,
			       p.nome AS pais, d.nome AS divisao, j.ano_inicial + tp.numero - 1 AS ano,
			       tp.rodadas_processadas AS processadas, t.ultimo_acesso_em,
			       (SELECT coalesce(max(pa.rodada), 0) FROM jogo_partida pa WHERE pa.temporada_id = tp.id) AS total,
			       r.jogo_id IS NOT NULL AS em_rede, r.comecou_em IS NULL AS na_sala, j.dono_id, dono.nome AS administrador,
			       (SELECT count(*) FROM jogo_rede_participante rp WHERE rp.jogo_id = j.id) AS na_sala_treinadores
			FROM jogo_treinador t
			JOIN jogo j ON j.id = t.jogo_id
			LEFT JOIN jogo_rede r ON r.jogo_id = j.id
			LEFT JOIN usuario dono ON dono.id = j.dono_id
			JOIN jogo_clube c ON c.id = t.clube_id
			JOIN pais_fifa p ON p.codigo = j.pais_codigo
			LEFT JOIN jogo_divisao d ON d.id = c.divisao_id
			JOIN jogo_temporada tp ON tp.jogo_id = j.id
			    AND tp.numero = (SELECT max(numero) FROM jogo_temporada WHERE jogo_id = j.id)""";

	/** Jogo recém-criado e os clubes que concorreram no sorteio do clube inicial. */
	public record NovoJogo(long jogoId, List<Clube> candidatos) {
	}

	/**
	 * Jogo na lista do treinador.
	 *
	 * @param divisao nula se o clube do treinador está na fila de espera
	 */
	public record JogoGravado(long jogoId, Clube clube, String pais, String divisao, int ano, int rodada,
			Instant ultimoAcesso, Optional<Rede> rede) {
	}

	/**
	 * O jogo é em rede (modelo 18a).
	 *
	 * @param naSala a temporada ainda não começou
	 * @param treinadores quantos estão na sala
	 */
	public record Rede(String administrador, boolean souAdministrador, boolean naSala, int treinadores) {
	}

	/** Sala de jogo em rede em que o usuário entrou e ainda não escolheu o clube. */
	public record SalaSemClube(long jogoId, String administrador, String pais, int treinadores) {
	}

	/** Salas em que o usuário está sem clube (modo escolha), da entrada mais recente para a mais antiga. */
	@Transactional(readOnly = true)
	public List<SalaSemClube> salasSemClube(long usuarioId) {
		return jdbc.sql("""
				SELECT j.id, u.nome AS administrador, pf.nome AS pais,
				       (SELECT count(*) FROM jogo_rede_participante o WHERE o.jogo_id = j.id) AS treinadores
				FROM jogo_rede_participante p
				JOIN jogo j ON j.id = p.jogo_id
				JOIN usuario u ON u.id = j.dono_id
				JOIN pais_fifa pf ON pf.codigo = j.pais_codigo
				WHERE p.usuario_id = :usuario
				  AND NOT EXISTS (SELECT 1 FROM jogo_treinador t WHERE t.jogo_id = j.id AND t.usuario_id = p.usuario_id)
				ORDER BY p.entrou_em DESC, j.id DESC""")
			.param("usuario", usuarioId)
			.query((rs, n) -> new SalaSemClube(rs.getLong("id"), rs.getString("administrador"), rs.getString("pais"),
					rs.getInt("treinadores")))
			.list();
	}
}
