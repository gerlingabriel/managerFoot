package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;
import br.com.managerfoot.temporada.Continental;
import br.com.managerfoot.temporada.FaseCopa;
import br.com.managerfoot.temporada.Mundial;

/** O Mundial na temporada (SPEC §8.3.4, rev. 88) contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MundialNaTemporadaIT {

	@Autowired
	BancoPadraoRepositorio bancoPadrao;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	ContinentalRepositorio continentais;

	@Autowired
	MundialRepositorio mundiais;

	@Autowired
	FimDeTemporadaRepositorio fins;

	@Autowired
	JdbcClient jdbc;

	long jogo;
	long temporada;

	@BeforeEach
	void jogarAteOMundial() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());
		CampeonatoDisponivel brasil = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow();
		jogo = jogos.criarJogoComClubeSorteado(brasil.paisId(), LigaDeTeste.participante(contas).id(), 5);
		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		temporada = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :j").param("j", jogo).query(Long.class).single();
		while (!(rodadas.proximo(jogo) instanceof RodadaRepositorio.Proximo.EtapaMundial)) {
			RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
			switch (rodadas.proximo(jogo)) {
				case RodadaRepositorio.Proximo.Liga(int r) -> rodadas.avancar(jogo, r);
				case RodadaRepositorio.Proximo.EtapaCopa e -> rodadas.avancarCopa(jogo, e.indice(), List.of());
				case RodadaRepositorio.Proximo.EtapaContinental e -> rodadas.avancarContinental(jogo, e.indice(), List.of());
				default -> throw new IllegalStateException("a temporada acabou sem Mundial");
			}
		}
	}

	String regiao(Clube clube) {
		return jdbc.sql("SELECT regiao_mundial FROM jogo_clube WHERE id = :c").param("c", clube.id()).query(String.class).optional().orElse(null);
	}

	@Test
	void semifinaisTemAsRegioesDefinidasEOCampeaoContinentalRepresentaASua() {
		Continental continental = continentais.carregar(jogo, temporada).orElseThrow();
		Mundial mundial = mundiais.carregar(jogo, temporada).orElseThrow();

		Mundial.Confronto europa = mundial.semifinais().get(0);
		assertThat(regiao(europa.primeiro())).isEqualTo("EUROPA");
		assertThat(regiao(europa.segundo())).isIn("ASIA", "AFRICA", "OCEANIA");
		Mundial.Confronto americas = mundial.semifinais().get(1);
		assertThat(regiao(americas.primeiro())).isEqualTo("AMERICA_DO_NORTE");
		assertThat(americas.segundo()).isEqualTo(continental.campeao().orElseThrow());
		assertThat(mundial.decisao()).isEmpty();
	}

	@Test
	void temporadaEsperaOMundialEAFinalDaOTituloUmaVezSo() {
		assertThatIllegalStateException().isThrownBy(() -> fins.avancarTemporada(jogo, 1, 99)).withMessageContaining("Mundial");

		Mundial depoisDasSemifinais = rodadas.avancarMundial(jogo, 0, List.of());
		assertThat(depoisDasSemifinais.semifinaisEncerradas()).isTrue();
		assertThat(mundiais.carregar(jogo, temporada).orElseThrow()).isEqualTo(depoisDasSemifinais);
		assertThat(rodadas.proximo(jogo)).isEqualTo(new RodadaRepositorio.Proximo.EtapaMundial(1, FaseCopa.FINAL));
		// repetir a etapa não joga de novo
		assertThatIllegalStateException().isThrownBy(() -> rodadas.avancarMundial(jogo, 0, List.of()));

		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
		Mundial fim = rodadas.avancarMundial(jogo, 1, List.of());
		assertThat(rodadas.proximo(jogo)).isInstanceOf(RodadaRepositorio.Proximo.Fim.class);
		Clube campeao = fim.campeao().orElseThrow();
		Map<String, Object> historico = jdbc.sql("SELECT clube_id, vice_id FROM jogo_campeao_mundial WHERE jogo_id = :j")
			.param("j", jogo).query().singleRow();
		assertThat(historico.get("clube_id")).isEqualTo(campeao.id());
		assertThat(historico.get("vice_id")).isEqualTo(fim.vice().orElseThrow().id());
		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_partida p JOIN jogo_copa_confronto k ON k.id = p.confronto_id
				WHERE p.temporada_id = :t AND k.competicao = 'MUNDIAL' AND p.gols_mandante IS NOT NULL AND p.publico IS NOT NULL""")
			.param("t", temporada).query(Long.class).single()).isEqualTo(3);

		// a temporada seguinte começa do zero, com novo Mundial só depois da continental dela
		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
		fins.avancarTemporada(jogo, 1, 99);
		long seguinte = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :j AND numero = 2").param("j", jogo).query(Long.class).single();
		assertThat(mundiais.quadro(seguinte)).isEmpty();
		assertThat(rodadas.proximo(jogo)).isNotInstanceOf(RodadaRepositorio.Proximo.Fim.class);
	}

	@Test
	void clubeDoPaisRecebeOPremioDoMundialNaRodadaDaFinal() {
		rodadas.avancarMundial(jogo, 0, List.of());
		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
		Mundial fim = rodadas.avancarMundial(jogo, 1, List.of());
		List<Clube> finalistas = List.of(fim.campeao().orElseThrow(), fim.vice().orElseThrow());
		long lancadosParaEstrangeiros = jdbc.sql("""
				SELECT count(*) FROM jogo_lancamento l JOIN jogo_clube c ON c.id = l.clube_id
				WHERE l.temporada_id = :t AND l.competicao = 'MUNDIAL' AND c.pais_codigo IS NOT NULL""")
			.param("t", temporada).query(Long.class).single();
		assertThat(lancadosParaEstrangeiros).isZero();
		Long doPais = jdbc.sql("SELECT count(*) FROM jogo_clube WHERE id IN (:ids) AND pais_codigo IS NULL")
			.param("ids", finalistas.stream().map(Clube::id).toList()).query(Long.class).single();
		long premios = jdbc.sql("SELECT count(*) FROM jogo_lancamento WHERE temporada_id = :t AND competicao = 'MUNDIAL' AND tipo = 'PREMIO'")
			.param("t", temporada).query(Long.class).single();
		assertThat(premios).isEqualTo(doPais);
	}
}
