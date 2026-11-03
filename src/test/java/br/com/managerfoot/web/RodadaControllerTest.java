package br.com.managerfoot.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RodadaControllerTest {

	@Test
	void tituloMostraOTurnoEARodada() {
		assertThat(RodadaController.titulo(1, 38)).isEqualTo("1º turno · 1ª rodada");
		assertThat(RodadaController.titulo(19, 38)).isEqualTo("1º turno · 19ª rodada");
		assertThat(RodadaController.titulo(20, 38)).isEqualTo("2º turno · 20ª rodada");
		assertThat(RodadaController.titulo(38, 38)).isEqualTo("2º turno · 38ª rodada");
		assertThat(RodadaController.titulo(17, 34)).isEqualTo("1º turno · 17ª rodada");
		assertThat(RodadaController.titulo(18, 34)).isEqualTo("2º turno · 18ª rodada");
	}
}
