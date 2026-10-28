package br.com.managerfoot.persistencia;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
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
	private final int candidatosClubeInicial;

	public JogosDoTreinadorRepositorio(JdbcClient jdbc, JogoRepositorio jogos, TemporadaRepositorio temporadas,
			@Value("${managerfoot.jogo.candidatos-clube-inicial}") int candidatosClubeInicial) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.temporadas = temporadas;
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
		long jogoId = jogos.criarJogoComClubeSorteado(paisMoldeId, usuarioId, semente);
		// os candidatos saem da última divisão como ela foi criada, antes de qualquer rodada
		List<Clube> candidatos = ClubeInicial.candidatos(jogos.carregarLiga(jogoId).divisoes().getLast(),
				candidatosClubeInicial);
		temporadas.iniciarPrimeiraTemporada(jogoId, new SplittableRandom(semente).nextLong());
		return new NovoJogo(jogoId, candidatos);
	}

	/** Jogos em que o usuário é treinador, do último acesso para o mais antigo. */
	@Transactional(readOnly = true)
	public List<JogoGravado> listar(long usuarioId) {
		return jdbc.sql(SELECAO + " WHERE t.usuario_id = :usuario ORDER BY t.ultimo_acesso_em DESC, j.id DESC")
			.param("usuario", usuarioId)
			.query((rs, n) -> new JogoGravado(rs.getLong("jogo_id"), TabelasLiga.clube(rs), rs.getString("pais"),
					rs.getString("divisao"), rs.getInt("temporada"), rodadaAtual(rs.getInt("processadas"),
							rs.getInt("total")),
					rs.getObject("ultimo_acesso_em", OffsetDateTime.class).toInstant()))
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
			       p.nome AS pais, d.nome AS divisao, tp.numero AS temporada,
			       tp.rodadas_processadas AS processadas, t.ultimo_acesso_em,
			       (SELECT coalesce(max(pa.rodada), 0) FROM jogo_partida pa WHERE pa.temporada_id = tp.id) AS total
			FROM jogo_treinador t
			JOIN jogo j ON j.id = t.jogo_id
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
	public record JogoGravado(long jogoId, Clube clube, String pais, String divisao, int temporada, int rodada,
			Instant ultimoAcesso) {
	}
}
