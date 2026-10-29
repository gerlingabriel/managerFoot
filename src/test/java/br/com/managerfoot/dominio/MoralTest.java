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
		assertThat(comRetorno.divisorRetorno()).isEqualTo(20);
		// 0 + empate fora (0) + (22 - 0) / 20 = 1
		assertThat(new Moral(0).aposJogo(false, 0, 0, comRetorno).valor()).isEqualTo(1);
		// 2 + vitória em casa (2) + (22 - 2) / 20 = 5
		assertThat(new Moral(2).aposJogo(true, 1, 0, comRetorno).valor()).isEqualTo(5);
		// 30 + empate fora (0) + (22 - 30) / 20 = 30: com a moral inicial 22, o topo quase não é puxado
		assertThat(new Moral(30).aposJogo(false, 0, 0, comRetorno).valor()).isEqualTo(30);
		// perto do centro o retorno é zero
		assertThat(new Moral(24).aposJogo(false, 0, 0, comRetorno).valor()).isEqualTo(24);
	}

	@Test
	void recusaParametrosIncoerentes() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosMoral(0, 30, 31, 2, 3, -1, 0, -3, -2, 15, 6, 0));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosMoral(0, 30, 18, 2, 3, -1, 0, -3, -2, 6, 15, 0));
	}

	@Test
	void padraoComecaEm22ComVerdeDe15AmareloDe7EVermelhoAte6() {
		ParametrosMoral padrao = ParametrosMoral.padrao();
		assertThat(Moral.inicial(padrao).valor()).isEqualTo(22);
		assertThat(new Moral(30).faixa(padrao)).isEqualTo(Moral.Faixa.VERDE);
		assertThat(new Moral(15).faixa(padrao)).isEqualTo(Moral.Faixa.VERDE);
		assertThat(new Moral(14).faixa(padrao)).isEqualTo(Moral.Faixa.AMARELO);
		assertThat(new Moral(7).faixa(padrao)).isEqualTo(Moral.Faixa.AMARELO);
		assertThat(new Moral(6).faixa(padrao)).isEqualTo(Moral.Faixa.VERMELHO);
		assertThat(new Moral(0).faixa(padrao)).isEqualTo(Moral.Faixa.VERMELHO);
	}

	@Test
	void quemEntraNoVermelhoEhDemitidoSeOJogoSeguinteNaoTiraDoVermelho() {
		ParametrosMoral padrao = ParametrosMoral.padrao();
		// 9 + derrota em casa (-3) + (22 - 9) / 20 = 6
		Moral entrouNoVermelho = new Moral(9).aposJogo(true, 0, 1, padrao);
		assertThat(entrouNoVermelho).isEqualTo(new Moral(6));
		// o jogo que leva ao vermelho não demite
		assertThat(new Moral(9).demite(entrouNoVermelho, padrao)).isFalse();

		// contra um adversário de mesma força: a vitória em casa (8) salva
		assertThat(entrouNoVermelho.demite(entrouNoVermelho.aposJogo(true, 2, 0, padrao), padrao)).isFalse();
		// derrota (4), empate fora (6) e empate em casa (5) não saem do vermelho e demitem
		assertThat(entrouNoVermelho.demite(entrouNoVermelho.aposJogo(false, 0, 1, padrao), padrao)).isTrue();
		assertThat(entrouNoVermelho.demite(entrouNoVermelho.aposJogo(false, 1, 1, padrao), padrao)).isTrue();
		assertThat(entrouNoVermelho.demite(entrouNoVermelho.aposJogo(true, 1, 1, padrao), padrao)).isTrue();
		// empate fora contra um adversário 6 pontos mais forte vale +2 e salva (8)
		assertThat(entrouNoVermelho.demite(entrouNoVermelho.aposJogo(false, 1, 1, 30, 36, padrao), padrao)).isFalse();
		// vitória que não basta para sair do vermelho também demite
		Moral zero = new Moral(0);
		assertThat(zero.aposJogo(true, 1, 0, padrao).faixa(padrao)).isEqualTo(Moral.Faixa.VERMELHO);
		assertThat(zero.demite(zero.aposJogo(true, 1, 0, padrao), padrao)).isTrue();
	}

	@Test
	void aSurpresaValeMaisEOResultadoEsperadoValeONormal() {
		ParametrosMoral padrao = ParametrosMoral.padrao();
		Moral meio = new Moral(22);
		// favorito (50 contra 41, 9 pontos = 3 a mais): vitória normal, empate e derrota piores
		assertThat(meio.aposJogo(true, 1, 0, 50, 41, padrao)).isEqualTo(new Moral(24));
		assertThat(meio.aposJogo(true, 1, 1, 50, 41, padrao)).isEqualTo(new Moral(18));
		assertThat(meio.aposJogo(true, 0, 1, 50, 41, padrao)).isEqualTo(new Moral(16));
		// azarão: derrota normal, empate e vitória melhores
		assertThat(meio.aposJogo(false, 0, 1, 41, 50, padrao)).isEqualTo(new Moral(20));
		assertThat(meio.aposJogo(false, 1, 1, 41, 50, padrao)).isEqualTo(new Moral(25));
		assertThat(meio.aposJogo(false, 1, 0, 41, 50, padrao)).isEqualTo(new Moral(28));
		// a diferença conta no máximo 3 pontos: 50 contra 20 é igual a 50 contra 41
		assertThat(meio.aposJogo(true, 0, 1, 50, 20, padrao)).isEqualTo(new Moral(16));
		// menos de 1,5 ponto de diferença não conta (arredonda para 0)
		assertThat(meio.aposJogo(true, 0, 1, 30, 28.6, padrao)).isEqualTo(meio.aposJogo(true, 0, 1, padrao));
	}
}
