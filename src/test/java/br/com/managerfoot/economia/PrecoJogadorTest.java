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
		// força 17: salário de R$ 2.000 por rodada, 80 rodadas
		assertThat(preco.preco(jogador(17), "BRA")).isEqualTo(160_000_00L);
		assertThat(preco.lanceInicialLeilao(jogador(17), "BRA")).isEqualTo(128_000_00L);
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
	void estrangeiroSaiDezPorCentoMaisBarato() {
		// R$ 160.000 do nacional menos 10%; no leilão, mais 20% abaixo
		assertThat(preco.preco(jogador(17), "ARG")).isEqualTo(144_000_00L);
		assertThat(preco.lanceInicialLeilao(jogador(17), "ARG")).isEqualTo(115_000_00L);
	}

	private static Jogador jogador(int forca) {
		return new Jogador(1, "Teste", Posicao.ATACANTE, forca, Temperamento.NORMAL, "BRA");
	}
}
