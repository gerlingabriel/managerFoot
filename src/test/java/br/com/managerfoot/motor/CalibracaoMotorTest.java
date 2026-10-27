package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Formacao;

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
		assertThat(medir(10, 45).pctCasa()).isPositive();
	}
}
