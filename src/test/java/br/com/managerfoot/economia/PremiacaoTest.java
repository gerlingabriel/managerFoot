package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PremiacaoTest {

	private final Premiacao premiacao = Premiacao.padrao();

	@Test
	void campeaoCompraDoisJogadoresDeForcaCinquentaComAsterisco() {
		// força 50 com asterisco: R$ 1.984.000 (preço sem faixa, rev. 66)
		assertThat(premiacao.premioDoCampeao(50)).isEqualTo(3_968_000_00L);
		assertThat(premiacao.premio(1, 50)).isEqualTo(3_968_000_00L);
		assertThat(premiacao.premio(2, 50)).isEqualTo(2_778_000_00L);
		assertThat(premiacao.premio(3, 50)).isEqualTo(397_000_00L);
		assertThat(premiacao.premio(4, 50)).isZero();
	}

	@Test
	void premioAcompanhaAForcaDeReferencia() {
		assertThat(premiacao.premioDoCampeao(22)).isLessThan(premiacao.premioDoCampeao(30))
			.isLessThan(premiacao.premioDoCampeao(50));
	}

	@Test
	void patrocinioCaiComAPosicaoEOPrimeiroAnoEIgual() {
		// 1º: 10% do prêmio; 2º: 10% menos; 3º: 8% menos que o 2º; e assim até o 20º
		assertThat(premiacao.patrocinio(1, 20, 50)).isEqualTo(397_000_00L);
		assertThat(premiacao.patrocinio(2, 20, 50)).isEqualTo(357_000_00L);
		assertThat(premiacao.patrocinio(3, 20, 50)).isEqualTo(329_000_00L);
		assertThat(premiacao.patrocinio(20, 20, 50)).isEqualTo(80_000_00L);
		long anterior = Long.MAX_VALUE;
		for (int posicao = 1; posicao <= 20; posicao++) {
			long p = premiacao.patrocinio(posicao, 20, 50);
			assertThat(p).isLessThan(anterior).isPositive();
			anterior = p;
		}
		// média das 20 posições
		assertThat(premiacao.patrocinioInicial(20, 50)).isEqualTo(197_000_00L);
	}

	@Test
	void posicaoForaDaDivisaoEInvalida() {
		assertThatThrownBy(() -> premiacao.patrocinio(21, 20, 50)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> premiacao.patrocinio(0, 20, 50)).isInstanceOf(IllegalArgumentException.class);
	}
}
