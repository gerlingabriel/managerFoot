package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.LinhaClassificacao;
import br.com.managerfoot.temporada.Temporada;
import br.com.managerfoot.temporada.TrocaDeTreinador;
import br.com.managerfoot.temporada.TrocaDeTreinador.Candidato;

/**
 * Demissões pela moral e trocas de treinador (SPEC §5.4, rev. 59 a 62; modelos 11a e 11b). Depois de
 * cada rodada, quem estava no vermelho e continua é demitido, do clube mais forte ao mais fraco, e
 * chama o treinador de um clube inferior de moral verde ({@link TrocaDeTreinador}). Se o chamado é da
 * máquina, os dois trocam na hora; se é usuário, ele recebe um convite e a rodada seguinte espera a
 * resposta. O usuário demitido troca com a máquina (nunca com outro usuário) e vê um aviso. Sem
 * ninguém para chamar, o treinador continua.
 *
 * <p>Uma troca move o treinador da máquina de cada clube para o outro e, se houver, o usuário. O
 * clube do usuário guarda o seu treinador da máquina escondido (ADR 0013), então todo clube continua
 * com exatamente um treinador da máquina.
 */
@Repository
public class TrocaRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final EscalacaoRepositorio escalacoes;
	private final TemporadaRepositorio temporadas;
	private final ClassificacaoRepositorio classificacoes;
	private final ParametrosMoral parametrosMoral = ParametrosMoral.padrao();
	private final TrocaDeTreinador troca = TrocaDeTreinador.padrao();
	private final EscalacaoMaquina escalacaoMaquina = new EscalacaoMaquina(ParametrosMotor.padrao());

	public TrocaRepositorio(JdbcClient jdbc, JogoRepositorio jogos, EscalacaoRepositorio escalacoes,
			TemporadaRepositorio temporadas, ClassificacaoRepositorio classificacoes) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.escalacoes = escalacoes;
		this.temporadas = temporadas;
		this.classificacoes = classificacoes;
	}

	/** Convite aberto para o usuário (modelo 11a). */
	public record Convite(long id, ClubeNoConvite hoje, ClubeNoConvite convite, String treinadorDemitido, int rodada) {
	}

	/** Aviso ao usuário demitido (modelo 11b): {@code novo} vazio = continua no clube. */
	public record Aviso(long id, String clubeAntigo, Optional<ClubeNoConvite> novo, String treinadorQueChega,
			Moral moral, Moral.Faixa faixa, int rodada) {
	}

	/**
	 * Clube como aparece no convite e no aviso.
	 *
	 * @param clube com o escudo
	 * @param posicao posição na divisão com as rodadas jogadas
	 * @param forca força em campo
	 */
	public record ClubeNoConvite(Clube clube, String divisao, int posicao, double forca, Moral moral, Moral.Faixa faixa) {
	}

	/**
	 * Demite e troca depois da rodada, na transação do avanço. Só contam os clubes que jogaram: quem
	 * estava no vermelho antes do jogo e continua depois.
	 *
	 * @param antes moral de antes da rodada por clube; ausente = inicial
	 * @param jogaram clubes que jogaram a rodada
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void demitirDepoisDaRodada(long jogoId, long temporadaId, int rodada, Map<Long, Moral> antes,
			Set<Long> jogaram) {
		Estado estado = carregar(jogoId);
		List<Long> demitidos = jogaram.stream()
			.filter(c -> estado.forca.containsKey(c))
			.filter(c -> antes.getOrDefault(c, Moral.inicial(parametrosMoral)).demite(estado.moral(c), parametrosMoral))
			.sorted(Comparator.comparingDouble((Long c) -> estado.forca.get(c)).reversed().thenComparing(c -> c))
			.toList();
		// um convite ainda aberto (a rodada só avança sem resposta fora da tela) tira os dois clubes das trocas
		Set<Long> ocupados = new HashSet<>(jdbc.sql("""
				SELECT unnest(ARRAY[clube_id, outro_clube_id]) FROM jogo_troca_treinador
				WHERE jogo_id = :jogo AND situacao = 'CONVITE'""")
			.param("jogo", jogoId)
			.query(Long.class)
			.list());
		for (long clube : demitidos) {
			if (!ocupados.contains(clube)) {
				chamar(jogoId, temporadaId, rodada, clube, estado, ocupados, Set.of());
			}
		}
	}

	/** Há um convite sem resposta: a rodada seguinte espera. */
	public boolean algumConvitePendente(long jogoId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_troca_treinador WHERE jogo_id = :jogo AND situacao = 'CONVITE')")
			.param("jogo", jogoId)
			.query(Boolean.class)
			.single();
	}

	/** Convite aberto para o clube do usuário. */
	public Optional<Convite> convitePendente(long jogoId, long usuarioId) {
		return jdbc.sql("""
				SELECT id, clube_id, outro_clube_id, rodada FROM jogo_troca_treinador
				WHERE jogo_id = :jogo AND convidado_id = :usuario AND situacao = 'CONVITE'""")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.query((rs, n) -> new long[] { rs.getLong("id"), rs.getLong("clube_id"), rs.getLong("outro_clube_id"),
					rs.getInt("rodada") })
			.optional()
			.map(c -> {
				Estado estado = carregar(jogoId);
				return new Convite(c[0], clubeNoConvite(jogoId, estado, c[2]), clubeNoConvite(jogoId, estado, c[1]),
						treinadorDaMaquina(c[1]), (int) c[3]);
			});
	}

	/** Aviso de demissão ainda não visto pelo usuário (o mais recente). */
	public Optional<Aviso> avisoPendente(long jogoId, long usuarioId) {
		return jdbc.sql("""
				SELECT id, clube_id, outro_clube_id, rodada FROM jogo_troca_treinador
				WHERE jogo_id = :jogo AND demitido_id = :usuario AND NOT aviso_visto ORDER BY id DESC LIMIT 1""")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.query((rs, n) -> new long[] { rs.getLong("id"), rs.getLong("clube_id"), rs.getLong("outro_clube_id"),
					rs.getInt("rodada") })
			.optional()
			.map(a -> {
				Estado estado = carregar(jogoId);
				String antigo = nome(a[1]);
				if (a[2] == 0) {
					Moral moral = estado.moral(a[1]);
					return new Aviso(a[0], antigo, Optional.empty(), "", moral, moral.faixa(parametrosMoral), (int) a[3]);
				}
				Moral moral = estado.moral(a[2]);
				return new Aviso(a[0], antigo, Optional.of(clubeNoConvite(jogoId, estado, a[2])), treinadorDaMaquina(a[1]), moral,
						moral.faixa(parametrosMoral), (int) a[3]);
			});
	}

	/** O usuário viu o aviso; um segundo pedido não muda nada. */
	@Transactional
	public void marcarAvisoVisto(long jogoId, long usuarioId, long avisoId) {
		jdbc.sql("UPDATE jogo_troca_treinador SET aviso_visto = TRUE WHERE id = :id AND jogo_id = :jogo AND demitido_id = :usuario")
			.param("id", avisoId)
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.update();
	}

	/**
	 * Aceita o convite: o usuário assume o clube que demitiu, e o treinador demitido vai para o clube
	 * do usuário.
	 *
	 * @return falso se o convite já foi respondido (clique repetido)
	 */
	@Transactional
	public boolean aceitar(long jogoId, long usuarioId, long conviteId) {
		Optional<long[]> convite = travarConvite(jogoId, usuarioId, conviteId);
		if (convite.isEmpty()) {
			return false;
		}
		trocar(jogoId, convite.get()[0], convite.get()[1]);
		jdbc.sql("UPDATE jogo_troca_treinador SET situacao = 'TROCA' WHERE id = :id").param("id", conviteId).update();
		return true;
	}

	/**
	 * Recusa o convite: a vez passa ao seguinte na escala (rev. 62); sem ninguém, o treinador do clube
	 * que demitiu continua.
	 *
	 * @return falso se o convite já foi respondido (clique repetido)
	 */
	@Transactional
	public boolean recusar(long jogoId, long usuarioId, long conviteId) {
		Optional<long[]> convite = travarConvite(jogoId, usuarioId, conviteId);
		if (convite.isEmpty()) {
			return false;
		}
		long clube = convite.get()[0];
		long temporada = convite.get()[2];
		int rodada = (int) convite.get()[3];
		jdbc.sql("UPDATE jogo_troca_treinador SET situacao = 'RECUSADO' WHERE id = :id").param("id", conviteId).update();
		Set<Long> ocupados = new HashSet<>(jdbc.sql("""
				SELECT unnest(ARRAY[clube_id, outro_clube_id]) FROM jogo_troca_treinador
				WHERE temporada_id = :temporada AND rodada = :rodada AND situacao IN ('TROCA', 'CONVITE')""")
			.param("temporada", temporada)
			.param("rodada", rodada)
			.query(Long.class)
			.list());
		Set<Long> recusaram = new HashSet<>(jdbc.sql("""
				SELECT outro_clube_id FROM jogo_troca_treinador
				WHERE temporada_id = :temporada AND rodada = :rodada AND clube_id = :clube AND situacao = 'RECUSADO'""")
			.param("temporada", temporada)
			.param("rodada", rodada)
			.param("clube", clube)
			.query(Long.class)
			.list());
		chamar(jogoId, temporada, rodada, clube, carregar(jogoId), ocupados, recusaram);
		return true;
	}

	/** Clube que demitiu, clube do usuário, temporada e rodada; vazio se não há esse convite aberto. */
	private Optional<long[]> travarConvite(long jogoId, long usuarioId, long conviteId) {
		return jdbc.sql("""
				SELECT clube_id, outro_clube_id, temporada_id, rodada FROM jogo_troca_treinador
				WHERE id = :id AND jogo_id = :jogo AND convidado_id = :usuario AND situacao = 'CONVITE' FOR UPDATE""")
			.param("id", conviteId)
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.query((rs, n) -> new long[] { rs.getLong("clube_id"), rs.getLong("outro_clube_id"), rs.getLong("temporada_id"),
					rs.getInt("rodada") })
			.optional();
	}

	/**
	 * Procura quem substitui o treinador demitido do clube e resolve: troca com a máquina, convite ao
	 * usuário ou continua. Os clubes envolvidos entram em {@code ocupados}.
	 */
	private void chamar(long jogoId, long temporadaId, int rodada, long clube, Estado estado, Set<Long> ocupados,
			Set<Long> recusaram) {
		Long demitido = estado.usuarios.get(clube);
		List<Candidato> candidatos = new ArrayList<>();
		for (Map.Entry<Long, Double> outro : estado.forca.entrySet()) {
			long id = outro.getKey();
			boolean usuario = estado.usuarios.containsKey(id);
			// o usuário demitido troca só com a máquina: dois usuários não trocam de clube
			if (id != clube && !ocupados.contains(id) && !recusaram.contains(id) && !(demitido != null && usuario)) {
				candidatos.add(new Candidato(id, outro.getValue(), estado.moral(id), usuario));
			}
		}
		List<Candidato> chamados = troca.chamados(estado.forca.get(clube), candidatos);
		if (chamados.isEmpty()) {
			if (demitido != null) {
				registrar(jogoId, temporadaId, rodada, clube, null, null, demitido, "CONTINUA");
			}
			return;
		}
		Candidato chamado = chamados.getFirst();
		ocupados.add(clube);
		ocupados.add(chamado.clubeId());
		if (chamado.usuario()) {
			registrar(jogoId, temporadaId, rodada, clube, chamado.clubeId(), estado.usuarios.get(chamado.clubeId()), null,
					"CONVITE");
			return;
		}
		trocar(jogoId, clube, chamado.clubeId());
		registrar(jogoId, temporadaId, rodada, clube, chamado.clubeId(), null, demitido, "TROCA");
	}

	/**
	 * Os dois clubes trocam de treinador: o da máquina e, se houver, o usuário (no máximo um dos dois
	 * clubes tem usuário). Os dois recomeçam com a moral inicial (rev. 61). O clube que passa a ter
	 * usuário começa com a escalação da máquina e grava os salários; o que deixa de ter readequa.
	 */
	private void trocar(long jogoId, long a, long b) {
		Long maquinaA = jdbc.sql("UPDATE jogo_treinador_maquina SET clube_id = NULL WHERE clube_id = :a RETURNING id")
			.param("a", a).query(Long.class).optional().orElse(null);
		jdbc.sql("UPDATE jogo_treinador_maquina SET clube_id = :a WHERE clube_id = :b").param("a", a).param("b", b).update();
		if (maquinaA != null) {
			jdbc.sql("UPDATE jogo_treinador_maquina SET clube_id = :b WHERE id = :id").param("b", b).param("id", maquinaA)
				.update();
		}
		int movidos = jdbc.sql("""
				UPDATE jogo_treinador SET clube_id = CASE clube_id WHEN :a THEN :b ELSE :a END
				WHERE jogo_id = :jogo AND clube_id IN (:a, :b)""")
			.param("a", a).param("b", b).param("jogo", jogoId).update();
		if (movidos > 1) {
			throw new IllegalStateException("troca entre dois usuários nos clubes " + a + " e " + b);
		}
		jdbc.sql("UPDATE jogo_clube SET moral = :moral WHERE id IN (:a, :b)")
			.param("moral", troca.moralDepoisDaTroca().valor()).param("a", a).param("b", b).update();
		jdbc.sql("DELETE FROM jogo_escalacao WHERE clube_id IN (:a, :b)").param("a", a).param("b", b).update();
		Map<Long, List<Jogador>> planteis = jogos.carregarPlanteis(jogoId);
		for (long clube : List.of(a, b)) {
			boolean deUsuario = jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_treinador WHERE clube_id = :c)")
				.param("c", clube).query(Boolean.class).single();
			if (!deUsuario) {
				continue;
			}
			Set<Long> suspensos = new HashSet<>(jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :c AND suspensao > 0")
				.param("c", clube).query(Long.class).list());
			List<Jogador> disponiveis = planteis.getOrDefault(clube, List.of()).stream()
				.filter(j -> !suspensos.contains(j.id())).toList();
			if (disponiveis.size() >= Escalacao.MINIMO_TITULARES) {
				escalacoes.gravar(clube, EscalacaoDoTreinador.de(escalacaoMaquina.escalar(disponiveis)));
			}
		}
		// salário gravado no clube novo do usuário e solto no antigo (rev. 58, como AumentoRepositorio.fixarSalarios)
		jdbc.sql("""
				UPDATE jogo_jogador j SET forca_salario = j.forca
				FROM jogo_treinador t WHERE t.clube_id = j.clube_id AND j.clube_id IN (:a, :b) AND j.forca_salario IS NULL""")
			.param("a", a).param("b", b).update();
		jdbc.sql("""
				UPDATE jogo_jogador j SET forca_salario = NULL
				WHERE j.clube_id IN (:a, :b) AND j.forca_salario IS NOT NULL
				  AND NOT EXISTS (SELECT 1 FROM jogo_treinador t WHERE t.clube_id = j.clube_id)""")
			.param("a", a).param("b", b).update();
	}

	private void registrar(long jogoId, long temporadaId, int rodada, long clube, Long outro, Long convidado, Long demitido,
			String situacao) {
		jdbc.sql("""
				INSERT INTO jogo_troca_treinador
				  (jogo_id, temporada_id, rodada, clube_id, outro_clube_id, convidado_id, demitido_id, situacao)
				VALUES (:jogo, :temporada, :rodada, :clube, :outro, :convidado, :demitido, :situacao)""")
			.param("jogo", jogoId)
			.param("temporada", temporadaId)
			.param("rodada", rodada)
			.param("clube", clube)
			.param("outro", outro)
			.param("convidado", convidado)
			.param("demitido", demitido)
			.param("situacao", situacao)
			.update();
	}

	/** Clubes das divisões (a fila não joga nem troca), com força em campo, moral e usuários. */
	private record Estado(Map<Long, Double> forca, Map<Long, Moral> morais, Map<Long, Long> usuarios,
			ParametrosMoral parametros) {

		Moral moral(long clube) {
			return morais.getOrDefault(clube, Moral.inicial(parametros));
		}
	}

	private Estado carregar(long jogoId) {
		Map<Long, List<Jogador>> planteis = jogos.carregarPlanteis(jogoId);
		Map<Long, Double> forca = new HashMap<>();
		Map<Long, Moral> morais = new HashMap<>();
		jdbc.sql("SELECT id, moral FROM jogo_clube WHERE jogo_id = :jogo AND divisao_id IS NOT NULL")
			.param("jogo", jogoId)
			.query(rs -> {
				long id = rs.getLong("id");
				forca.put(id, Clube.forcaEmCampo(planteis.getOrDefault(id, List.of())));
				if (rs.getObject("moral") != null) {
					morais.put(id, new Moral(rs.getInt("moral")));
				}
			});
		Map<Long, Long> usuarios = new HashMap<>();
		jdbc.sql("SELECT clube_id, usuario_id FROM jogo_treinador WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query(rs -> {
				usuarios.put(rs.getLong("clube_id"), rs.getLong("usuario_id"));
			});
		return new Estado(forca, morais, usuarios, parametrosMoral);
	}

	/** O clube com escudo e a posição na tabela da divisão, com as rodadas já jogadas. */
	private ClubeNoConvite clubeNoConvite(long jogoId, Estado estado, long clube) {
		record Atual(long id, int processadas) {
		}
		Atual atual = jdbc.sql("""
				SELECT id, rodadas_processadas FROM jogo_temporada WHERE jogo_id = :jogo ORDER BY numero DESC LIMIT 1""")
			.param("jogo", jogoId)
			.query((rs, n) -> new Atual(rs.getLong("id"), rs.getInt("rodadas_processadas")))
			.single();
		Temporada temporada = temporadas.carregarAtual(jogoId);
		for (Map.Entry<Integer, Classificacao> tabela : classificacoes.calcular(temporada, atual.id(), atual.processadas() + 1)
			.entrySet()) {
			List<LinhaClassificacao> linhas = tabela.getValue().linhas();
			for (int i = 0; i < linhas.size(); i++) {
				if (linhas.get(i).clube().id() == clube) {
					String divisao = jdbc.sql("SELECT nome FROM jogo_divisao WHERE jogo_id = :jogo AND prioridade = :p")
						.param("jogo", jogoId).param("p", tabela.getKey()).query(String.class).single();
					Moral moral = estado.moral(clube);
					return new ClubeNoConvite(linhas.get(i).clube(), divisao, i + 1, estado.forca.getOrDefault(clube, 0.0),
							moral, moral.faixa(parametrosMoral));
				}
			}
		}
		throw new NoSuchElementException("clube " + clube + " fora das divisões");
	}

	private String nome(long clube) {
		return jdbc.sql("SELECT nome FROM jogo_clube WHERE id = :c").param("c", clube).query(String.class).single();
	}

	private String treinadorDaMaquina(long clube) {
		return jdbc.sql("SELECT nome FROM jogo_treinador_maquina WHERE clube_id = :c").param("c", clube).query(String.class)
			.optional().orElse("o treinador");
	}
}
