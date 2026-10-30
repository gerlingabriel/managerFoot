package br.com.managerfoot.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegraEstrangeirosTest {

	@Test
	void jogadorDoPaisDaLigaNaoEEstrangeiroNemEquiparado() {
		assertThat(RegraEstrangeiros.deOutroPais("BRA", "BRA")).isFalse();
		assertThat(RegraEstrangeiros.equiparado("BRA", "BRA")).isFalse();
		assertThat(RegraEstrangeiros.contaComoEstrangeiro("BRA", "BRA")).isFalse();
		assertThat(RegraEstrangeiros.equiparado("ITA", "ITA")).isFalse();
	}

	@Test
	void noBrasilSoOPortuguesEEquiparado() {
		assertThat(RegraEstrangeiros.equiparado("POR", "BRA")).isTrue();
		assertThat(RegraEstrangeiros.contaComoEstrangeiro("POR", "BRA")).isFalse();
		assertThat(RegraEstrangeiros.contaComoEstrangeiro("ARG", "BRA")).isTrue();
		assertThat(RegraEstrangeiros.contaComoEstrangeiro("ITA", "BRA")).isTrue();
	}

	@Test
	void naLigaEuropeiaQuemEDaUniaoEuropeiaNaoContaMasORestoConta() {
		assertThat(RegraEstrangeiros.equiparado("POR", "ITA")).isTrue();
		assertThat(RegraEstrangeiros.equiparado("GER", "ESP")).isTrue();
		assertThat(RegraEstrangeiros.contaComoEstrangeiro("BRA", "ITA")).isTrue();
		assertThat(RegraEstrangeiros.contaComoEstrangeiro("ENG", "ITA")).isTrue();
		assertThat(RegraEstrangeiros.contaComoEstrangeiro("NOR", "POR")).isTrue();
	}

	@Test
	void inglaterraEquiparaAUniaoEuropeiaSemSerDela() {
		assertThat(RegraEstrangeiros.equiparado("ITA", "ENG")).isTrue();
		assertThat(RegraEstrangeiros.contaComoEstrangeiro("ARG", "ENG")).isTrue();
	}

	@Test
	void foraDoBrasilEDaEuropaNinguemEEquiparado() {
		assertThat(RegraEstrangeiros.contaComoEstrangeiro("POR", "ARG")).isTrue();
		assertThat(RegraEstrangeiros.contaComoEstrangeiro("BRA", "ARG")).isTrue();
	}
}
