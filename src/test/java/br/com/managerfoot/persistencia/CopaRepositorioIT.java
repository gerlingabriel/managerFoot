package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.temporada.ConfrontoCopa;
import br.com.managerfoot.temporada.Copa;
import br.com.managerfoot.temporada.FaseCopa;

/** Migração V30 e {@link CopaRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CopaRepositorioIT {

	@Autowired
	CopaRepositorio copas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	TransactionTemplate transacao;

	long jogo;
	long temporada;

	@BeforeEach
	void criaJogo() {
		// Série A de 16 e Série B de 15: 31 clubes, a copa começa nas oitavas com 16
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		temporadas.iniciarPrimeiraTemporada(jogo, 7);
		temporada = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :jogo").param("jogo", jogo)
			.query(Long.class).single();
	}

	@Test
	void temporadaNovaTemCopaSorteadaComOs16MaisFortes() {
		Copa copa = copas.carregar(jogo, temporada).orElseThrow();

		assertThat(copa.faseAtual()).isEqualTo(FaseCopa.OITAVAS);
		assertThat(copa.confrontos()).hasSize(8);
		Set<Clube> inscritos = new HashSet<>();
		copa.confrontos().forEach(c -> {
			inscritos.add(c.mandante());
			inscritos.add(c.visitante());
		});
		// os planteis da Série A são mais fortes: todos os 16 entram
		Set<Long> serieA = new HashSet<>(jdbc.sql("""
				SELECT c.id FROM jogo_clube c JOIN jogo_divisao d ON d.id = c.divisao_id
				WHERE c.jogo_id = :jogo AND d.prioridade = 1""").param("jogo", jogo).query(Long.class).list());
		assertThat(inscritos).extracting(Clube::id).containsExactlyInAnyOrderElementsOf(serieA);
		assertThat(copas.quadro(jogo, temporada)).extracting(Copa.Etapa::depoisDaRodada)
			.containsExactly(10, 12, 18, 20, 26, 28, 30);
	}

	@Test
	void partidasDaIdaEDaVoltaFicamNoCalendarioSemMexerNaLiga() {
		List<Integer> rodadas = jdbc.sql("""
				SELECT rodada FROM jogo_partida WHERE temporada_id = :t AND confronto_id IS NOT NULL ORDER BY rodada""")
			.param("t", temporada).query(Integer.class).list();
		assertThat(rodadas).hasSize(16).containsOnly(10, 12);
		long voltaInvertida = jdbc.sql("""
				SELECT count(*) FROM jogo_partida p JOIN jogo_copa_confronto c ON c.id = p.confronto_id
				WHERE p.perna = 'VOLTA' AND p.mandante_id = c.visitante_id AND p.visitante_id = c.mandante_id
				  AND c.temporada_id = :t""").param("t", temporada).query(Long.class).single();
		assertThat(voltaInvertida).isEqualTo(8);
		assertThat(temporadas.carregarAtual(jogo).divisoes().getFirst().calendario().rodadas()).hasSize(30);
	}

	@Test
	void placaresGravadosVoltamNaCopaEAFaseSeguinteECriada() {
		// ida 1 x 0 e volta 0 x 0 em todos: passa o mandante da ida
		jdbc.sql("""
				UPDATE jogo_partida SET gols_mandante = CASE WHEN perna = 'IDA' THEN 1 ELSE 0 END, gols_visitante = 0,
				       semente = 1
				WHERE temporada_id = :t AND confronto_id IS NOT NULL""").param("t", temporada).update();
		Copa copa = copas.carregar(jogo, temporada).orElseThrow();
		assertThat(copa.confrontosDa(FaseCopa.OITAVAS)).allMatch(ConfrontoCopa::decidido);

		Copa depois = transacao.execute(s -> copas.avancarFase(temporada, copa, 7));

		assertThat(depois.faseAtual()).isEqualTo(FaseCopa.QUARTAS);
		assertThat(copas.carregar(jogo, temporada)).contains(depois);
		assertThat(depois.confrontosDa(FaseCopa.QUARTAS).getFirst().envolve(copa.confrontos().get(0).mandante())).isTrue();
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_partida WHERE temporada_id = :t AND rodada IN (18, 20) "
				+ "AND confronto_id IS NOT NULL").param("t", temporada).query(Long.class).single()).isEqualTo(8);
		// fase sem terminar não avança
		Copa deNovo = transacao.execute(s -> copas.avancarFase(temporada, depois, 7));
		assertThat(deNovo).isEqualTo(depois);
	}
}
