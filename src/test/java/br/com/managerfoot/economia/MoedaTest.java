package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MoedaTest {

	@Test
	void moedaPeloPaisDaLiga() {
		assertThat(Moeda.doPais("BRA")).isEqualTo(Moeda.REAL);
		assertThat(Moeda.doPais("ARG")).isEqualTo(Moeda.DOLAR);
		assertThat(Moeda.doPais("ENG")).isEqualTo(Moeda.EURO);
		assertThat(Moeda.doPais("POR")).isEqualTo(Moeda.EURO);
		assertThat(Moeda.doPais("JPN")).isEqualTo(Moeda.REAL);
	}

	@Test
	void formataConvertendoPelaProporcaoFixa() {
		long tresMilReais = 3_000_00L;
		assertThat(Moeda.REAL.formatar(tresMilReais).replace(' ', ' ')).isEqualTo("R$ 3.000");
		assertThat(Moeda.DOLAR.formatar(tresMilReais)).isEqualTo("US$ 600");
		assertThat(Moeda.EURO.formatar(tresMilReais)).isEqualTo("€ 500");
		assertThat(Moeda.REAL.formatar(-1_234_567_00L)).isEqualTo("R$ -1.234.567");
	}
}
