package br.com.managerfoot.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class MoralTest {

	/** Sem retorno ao centro, para testar só as variações da SPEC §6.2. */
	private final ParametrosMoral p = new ParametrosMoral(0, 30, 18, 2, 3, -1, 0, -3, -2, 15, 6, 0);

	@Test
	void comecaEm18() {
		assertThat(Moral.inicial(p).valor()).isEqualTo(18);
	}

	@Test
	void variacoesDaSpec() {
		Moral m = new Moral(18);
		assertThat(m.aposJogo(true, 2, 0, p).valor()).isEqualTo(20);
		assertThat(m.aposJogo(false, 1, 0, p).valor()).isEqualTo(21);
		assertThat(m.aposJogo(true, 1, 1, p).valor()).isEqualTo(17);
		assertThat(m.aposJogo(false, 0, 0, p).valor()).isEqualTo(18);
		assertThat(m.aposJogo(true, 0, 1, p).valor()).isEqualTo(15);
		assertThat(m.aposJogo(false, 0, 3, p).valor()).isEqualTo(16);
	}

	@Test
	void ficaPresaEntre0E30() {
		assertThat(new Moral(29).aposJogo(false, 1, 0, p).valor()).isEqualTo(30);
		assertThat(new Moral(1).aposJogo(true, 0, 1, p).valor()).isZero();
	}

	@Test
	void faixas() {
		assertThat(new Moral(30).faixa(p)).isEqualTo(Moral.Faixa.VERDE);
		assertThat(new Moral(15).faixa(p)).isEqualTo(Moral.Faixa.VERDE);
		assertThat(new Moral(14).faixa(p)).isEqualTo(Moral.Faixa.AMARELO);
		assertThat(new Moral(6).faixa(p)).isEqualTo(Moral.Faixa.AMARELO);
		assertThat(new Moral(5).faixa(p)).isEqualTo(Moral.Faixa.VERMELHO);
		assertThat(new Moral(0).faixa(p)).isEqualTo(Moral.Faixa.VERMELHO);
	}

	@Test
	void retornoLevePuxaParaOValorInicial() {
		ParametrosMoral comRetorno = ParametrosMoral.padrao();
		assertThat(comRetorno.divisorRetorno()).isEqualTo(12);
		// 0 + empate fora (0) + (18 - 0) / 12 = 1
		assertThat(new Moral(0).aposJogo(false, 0, 0, comRetorno).valor()).isEqualTo(1);
		// 30 + empate fora (0) + (18 - 30) / 12 = 29
		assertThat(new Moral(30).aposJogo(false, 0, 0, comRetorno).valor()).isEqualTo(29);
		// perto do centro o retorno é zero
		assertThat(new Moral(20).aposJogo(false, 0, 0, comRetorno).valor()).isEqualTo(20);
	}

	@Test
	void recusaParametrosIncoerentes() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosMoral(0, 30, 31, 2, 3, -1, 0, -3, -2, 15, 6, 0));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosMoral(0, 30, 18, 2, 3, -1, 0, -3, -2, 6, 15, 0));
	}
}
