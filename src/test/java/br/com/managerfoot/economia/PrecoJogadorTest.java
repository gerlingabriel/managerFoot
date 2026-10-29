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
		// força 17: salário de R$ 1.900 por rodada, 80 rodadas
		assertThat(preco.preco(jogador(17), "BRA")).isEqualTo(152_000_00L);
		assertThat(preco.lanceInicialLeilao(jogador(17), "BRA")).isEqualTo(122_000_00L);
	}

	@Test
	void comPrecoPeloCompradorCadaClubePagaASuaFaixaEOLeilaoComecaSemFaixa() {
		PrecoJogador peloComprador = new PrecoJogador(ParametrosMercado.padrao().comPrecoPeloComprador(true),
				CaixaDaRodada.padrao());
		// força 17 sem faixa: R$ 2.000 por rodada, 80 rodadas
		assertThat(peloComprador.preco(jogador(17), "BRA")).isEqualTo(160_000_00L);
		assertThat(peloComprador.lanceInicialLeilao(jogador(17), "BRA")).isEqualTo(128_000_00L);
		assertThat(peloComprador.preco(jogador(17), "BRA", 50)).isEqualTo(176_000_00L); // +10%
		assertThat(peloComprador.preco(jogador(17), "BRA", 35)).isEqualTo(168_000_00L); // +5%
		assertThat(peloComprador.preco(jogador(17), "BRA", 20)).isEqualTo(152_000_00L); // −5%
		// desligado (padrão): o comprador não muda o preço
		assertThat(preco.preco(jogador(17), "BRA", 50)).isEqualTo(152_000_00L);
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
		// R$ 152.000 do nacional menos 25%; no leilão, mais 20% abaixo
		assertThat(preco.preco(jogador(17), "ARG")).isEqualTo(114_000_00L);
		assertThat(preco.lanceInicialLeilao(jogador(17), "ARG")).isEqualTo(91_000_00L);
	}

	@Test
	void asteriscoSaiVinteECincoPorCentoMaisCaroEOLeilaoComecaAbaixo() {
		assertThat(preco.preco(jogador(17).comAsterisco(true), "BRA")).isEqualTo(190_000_00L);
		assertThat(preco.lanceInicialLeilao(jogador(17).comAsterisco(true), "BRA")).isEqualTo(152_000_00L);
		// estrangeiro com asterisco: os dois ajustes se combinam (152.000 × 0,75 × 1,25 = 142.500)
		assertThat(preco.preco(jogador(17).comAsterisco(true), "ARG")).isEqualTo(143_000_00L);
	}

	private static Jogador jogador(int forca) {
		return new Jogador(1, "Teste", Posicao.ATACANTE, forca, Temperamento.NORMAL, "BRA");
	}
}
