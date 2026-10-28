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
	void lanceDigitadoNaMoedaViraCentavosEOMinimoArredondaParaCima() {
		assertThat(Moeda.REAL.centavos(339_000)).isEqualTo(339_000_00L);
		assertThat(Moeda.EURO.centavos(1)).isEqualTo(6_00L);
		// R$ 339.000 em euros: 56.500; R$ 339.001 já pede 56.501
		assertThat(Moeda.EURO.unidadesAcima(339_000_00L)).isEqualTo(56_500);
		assertThat(Moeda.EURO.unidadesAcima(339_001_00L)).isEqualTo(56_501);
		assertThat(Moeda.REAL.unidadesAcima(0)).isZero();
	}

	@Test
	void formataConvertendoPelaProporcaoFixa() {
		long tresMilReais = 3_000_00L;
		assertThat(Moeda.REAL.formatar(tresMilReais).replace(' ', ' ')).isEqualTo("R$ 3.000");
		assertThat(Moeda.DOLAR.formatar(tresMilReais)).isEqualTo("US$ 600");
		assertThat(Moeda.EURO.formatar(tresMilReais)).isEqualTo("€ 500");
		assertThat(Moeda.REAL.formatar(-1_234_567_00L)).isEqualTo("R$ -1.234.567");
	}

	@Test
	void formataComSinalParaOExtrato() {
		assertThat(Moeda.REAL.formatarComSinal(129_204_00L)).isEqualTo("+ R$ 129.204");
		assertThat(Moeda.REAL.formatarComSinal(-227_634_00L)).isEqualTo("− R$ 227.634");
		assertThat(Moeda.EURO.formatarComSinal(-6_00L)).isEqualTo("− € 1");
		assertThat(Moeda.EURO.formatarComSinal(2_00L)).isEqualTo("€ 0");
	}
}
