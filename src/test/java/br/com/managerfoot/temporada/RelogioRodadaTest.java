package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

class RelogioRodadaTest {

	/** 3ª sem usuário, 2ª com usuário, 1ª sem usuário: a ordem de exibição. */
	private final RelogioRodada relogio = new RelogioRodada(List.of(false, true, false), 90);

	@Test
	void divisaoSemUsuarioLevaCercaDeCincoSegundos() {
		assertThat(relogio.avancar(0, 4_500, Velocidade.NORMAL)).isCloseTo(90, within(1e-9));
		assertThat(relogio.divisao(90)).isEqualTo(1);
		assertThat(relogio.minuto(90)).isZero();
	}

	@Test
	void divisaoDoUsuarioAndaNaVelocidadeEscolhida() {
		assertThat(relogio.avancar(90, 1_000, Velocidade.NORMAL)).isCloseTo(92, within(1e-9));
		assertThat(relogio.avancar(90, 1_000, Velocidade.LENTA)).isCloseTo(91, within(1e-9));
		assertThat(relogio.avancar(90, 1_000, Velocidade.RAPIDA)).isCloseTo(95, within(1e-9));
		assertThat(relogio.minuto(135.7)).isEqualTo(45);
	}

	@Test
	void tempoQueSobraDeUmaDivisaoPassaParaASeguinte() {
		// 3ª inteira (4,5 s) + 2ª inteira em rápida (18 s) + 1 s na 1ª (20 minutos)
		double progresso = relogio.avancar(0, 4_500 + 18_000 + 1_000, Velocidade.RAPIDA);

		assertThat(progresso).isCloseTo(200, within(1e-9));
		assertThat(relogio.divisao(progresso)).isEqualTo(2);
		assertThat(relogio.minuto(progresso)).isEqualTo(20);
	}

	@Test
	void paraNoFimEMostraOMinutoFinal() {
		double fim = relogio.avancar(0, 10_000_000, Velocidade.LENTA);

		assertThat(fim).isEqualTo(270);
		assertThat(relogio.terminado(fim)).isTrue();
		assertThat(relogio.divisao(fim)).isEqualTo(2);
		assertThat(relogio.minuto(fim)).isEqualTo(90);
		assertThat(relogio.avancar(fim, 1_000, Velocidade.NORMAL)).isEqualTo(270);
	}
}
