package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PremiacaoTest {

	private final Premiacao premiacao = Premiacao.padrao();

	@Test
	void campeaoCompraDoisJogadoresDeForcaCinquentaComAsterisco() {
		// força 50 com asterisco: R$ 1.984.000
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
		assertThat(premiacao.patrocinio(1, 20, 50)).isEqualTo(397_000_00L);
		assertThat(premiacao.patrocinio(20, 20, 50)).isEqualTo(20_000_00L);
		long anterior = Long.MAX_VALUE;
		for (int posicao = 1; posicao <= 20; posicao++) {
			long p = premiacao.patrocinio(posicao, 20, 50);
			assertThat(p).isLessThan(anterior).isPositive();
			anterior = p;
		}
		// média de 1 a 20: 10,5/20 do cheio
		assertThat(premiacao.patrocinioInicial(20, 50)).isEqualTo(208_000_00L);
	}

	@Test
	void posicaoForaDaDivisaoEInvalida() {
		assertThatThrownBy(() -> premiacao.patrocinio(21, 20, 50)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> premiacao.patrocinio(0, 20, 50)).isInstanceOf(IllegalArgumentException.class);
	}
}
