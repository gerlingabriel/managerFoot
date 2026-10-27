package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Sorte;

/**
 * Regressão da calibração (SPEC §7.6) com sementes fixas: determinístico, não falha ao acaso.
 * Se uma mudança no motor quebrar este teste, regenerar o relatório e revisar as metas.
 */
class CalibracaoMotorTest {

	private static final int PARTIDAS = 3000;
	private final MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());

	private EstatisticasLote medir(int forcaCasa, int forcaFora) {
		return EstatisticasLote.medir(motor, escalacao(Formacao.F_4_4_2, forcaCasa, 1),
				escalacao(Formacao.F_4_4_2, forcaFora, 100), 0, PARTIDAS);
	}

	@Test
	void timesEquilibradosAtingemAsMetasDefinidas() {
		EstatisticasLote e = medir(25, 25);

		assertThat(e.mediaGols()).isBetween(1.75, 2.5);
		assertThat(e.pctCasa()).isBetween(42.0, 48.0);
		assertThat(e.pctCasa()).isGreaterThan(e.pctFora());
		assertThat(e.porPartida(e.amarelos)).isBetween(1.8, 2.2);
		assertThat(e.porPartida(e.expulsoes)).isBetween(0.1, 0.22);
		assertThat(e.porPartida(e.penaltis)).isBetween(0.25, 0.35);
		assertThat(e.pctGoleadas()).isLessThan(2.0);
	}

	@Test
	void zebrasFicamNasFaixasDefinidas() {
		assertThat(medir(20, 30).pctCasa()).isBetween(15.0, 20.0);
		assertThat(medir(20, 40).pctCasa()).isBetween(5.0, 10.5);
		assertThat(medir(10, 45).pctCasa()).isBetween(1.0, 3.5);
	}

	@Test
	void taticaOfensivaFazESofreMaisGolsQueDefensiva() {
		EstatisticasLote ofensiva = EstatisticasLote.medir(motor, escalacao(Formacao.F_3_4_3, 25, 1),
				escalacao(Formacao.F_4_4_2, 25, 100), 0, PARTIDAS);
		EstatisticasLote defensiva = EstatisticasLote.medir(motor, escalacao(Formacao.F_5_4_1, 25, 1),
				escalacao(Formacao.F_4_4_2, 25, 100), 0, PARTIDAS);

		assertThat(ofensiva.golsCasa).isGreaterThan(defensiva.golsCasa);
		assertThat(ofensiva.golsFora).isGreaterThan(defensiva.golsFora);
	}

	@Test
	void nenhumaFormacaoSeDistanciaDasDemais() {
		// todas contra todas, ida e volta; diferença máxima de pontos por jogo entre formações
		Formacao[] fs = Formacao.values();
		double[] pontos = new double[fs.length];
		int porConfronto = 400;
		for (int i = 0; i < fs.length; i++) {
			for (int j = 0; j < fs.length; j++) {
				EstatisticasLote e = EstatisticasLote.medir(motor, escalacao(fs[i], 25, 1), escalacao(fs[j], 25, 100), 0,
						porConfronto);
				pontos[i] += (3.0 * e.vitoriasCasa + e.empates) / porConfronto;
				pontos[j] += (3.0 * e.vitoriasFora + e.empates) / porConfronto;
			}
		}
		double max = java.util.Arrays.stream(pontos).max().orElseThrow() / (2 * fs.length);
		double min = java.util.Arrays.stream(pontos).min().orElseThrow() / (2 * fs.length);
		assertThat(max - min).isLessThan(0.25);
	}

	@Test
	void sorteNeutraNaoMudaAPartida() {
		Escalacao a = escalacao(Formacao.F_4_4_2, 25, 1);
		Escalacao b = escalacao(Formacao.F_4_4_2, 25, 100);
		for (long s = 0; s < 200; s++) {
			assertThat(motor.simular(RegrasPartida.padrao(), a, b, Map.of(Lado.CASA, Sorte.NEUTRA, Lado.FORA, Sorte.NEUTRA), s))
				.isEqualTo(motor.simular(a, b, s));
		}
	}

	@Test
	void sorteMudaCercaDeCincoPontosPercentuais() {
		// SPEC §5.3.1: ~5 p.p. a mais de vitórias (beneficiado) e de derrotas (prejudicado), média casa/fora
		Escalacao a = escalacao(Formacao.F_4_4_2, 25, 1);
		Escalacao b = escalacao(Formacao.F_4_4_2, 25, 100);
		EstatisticasLote base = EstatisticasLote.medir(motor, a, b, 0, PARTIDAS);
		EstatisticasLote benCasa = EstatisticasLote.medir(motor, a, b, Map.of(Lado.CASA, Sorte.BENEFICIADO), 0, PARTIDAS);
		EstatisticasLote benFora = EstatisticasLote.medir(motor, a, b, Map.of(Lado.FORA, Sorte.BENEFICIADO), 0, PARTIDAS);
		EstatisticasLote preCasa = EstatisticasLote.medir(motor, a, b, Map.of(Lado.CASA, Sorte.PREJUDICADO), 0, PARTIDAS);
		EstatisticasLote preFora = EstatisticasLote.medir(motor, a, b, Map.of(Lado.FORA, Sorte.PREJUDICADO), 0, PARTIDAS);

		double maisVitorias = (benCasa.pctCasa() - base.pctCasa() + benFora.pctFora() - base.pctFora()) / 2;
		double maisDerrotas = (preCasa.pctFora() - base.pctFora() + preFora.pctCasa() - base.pctCasa()) / 2;
		assertThat(maisVitorias).isBetween(3.5, 6.5);
		assertThat(maisDerrotas).isBetween(3.5, 6.5);
	}
}
