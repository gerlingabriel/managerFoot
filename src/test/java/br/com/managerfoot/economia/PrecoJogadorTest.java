package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

class PrecoJogadorTest {

	private final PrecoJogador preco = PrecoJogador.padrao();

	@Test
	void precoSegueOSalarioEOLeilaoComecaVintePorCentoAbaixo() {
		// força 17: salário sem faixa de R$ 2.000 por rodada (rev. 66), 80 rodadas
		assertThat(preco.preco(jogador(17), "BRA")).isEqualTo(160_000_00L);
		assertThat(preco.lanceInicialLeilao(jogador(17), "BRA")).isEqualTo(128_000_00L);
	}

	@Test
	void comFaixaOPrecoSegueOSalarioDaFaixaDoJogador() {
		PrecoJogador comFaixa = new PrecoJogador(ParametrosMercado.padrao().comPrecoSemFaixa(false), CaixaDaRodada.padrao());
		// força 17 com a faixa de −15%: R$ 1.700 por rodada, 80 rodadas
		assertThat(comFaixa.preco(jogador(17), "BRA")).isEqualTo(136_000_00L);
	}

	@Test
	void precoCresceComAForcaEFicaRedondo() {
		long anterior = 0;
		for (int forca = Jogador.FORCA_MINIMA; forca <= Jogador.FORCA_MAXIMA; forca++) {
			long p = preco.preco(jogador(forca), "BRA");
			assertThat(p).isGreaterThanOrEqualTo(anterior).isPositive();
			assertThat(p % 1_000_00L).isZero();
			assertThat(preco.lanceInicialLeilao(jogador(forca), "BRA")).isLessThan(p);
			anterior = p;
		}
	}

	@Test
	void estrangeiroSaiVinteECincoPorCentoMaisBarato() {
		// R$ 160.000 do nacional menos 25%; no leilão, mais 20% abaixo
		assertThat(preco.preco(jogador(17), "ARG")).isEqualTo(120_000_00L);
		assertThat(preco.lanceInicialLeilao(jogador(17), "ARG")).isEqualTo(96_000_00L);
	}

	@Test
	void asteriscoSaiVinteECincoPorCentoMaisCaroEOLeilaoComecaAbaixo() {
		assertThat(preco.preco(jogador(17).comAsterisco(true), "BRA")).isEqualTo(200_000_00L);
		assertThat(preco.lanceInicialLeilao(jogador(17).comAsterisco(true), "BRA")).isEqualTo(160_000_00L);
		// estrangeiro com asterisco: os dois ajustes se combinam (160.000 × 0,75 × 1,25 = 150.000)
		assertThat(preco.preco(jogador(17).comAsterisco(true), "ARG")).isEqualTo(150_000_00L);
	}

	private static Jogador jogador(int forca) {
		return new Jogador(1, "Teste", Posicao.ATACANTE, forca, Temperamento.NORMAL, "BRA");
	}
}
