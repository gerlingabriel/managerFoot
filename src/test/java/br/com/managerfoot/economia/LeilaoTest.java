package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.economia.Leilao.Lance;

class LeilaoTest {

	@Test
	void levaOMaiorLanceAcimaDoMinimo() {
		List<Lance> lances = List.of(new Lance(1, 100), new Lance(2, 150), new Lance(3, 120));
		assertThat(Leilao.vencedor(lances, 100, new SplittableRandom(1))).contains(new Lance(2, 150));
	}

	@Test
	void semLanceValidoNaoHaVenda() {
		assertThat(Leilao.vencedor(List.of(), 0, new SplittableRandom(1))).isEmpty();
		assertThat(Leilao.vencedor(List.of(new Lance(1, 99)), 100, new SplittableRandom(1))).isEmpty();
	}

	@Test
	void leilaoSemMinimoAceitaQualquerLance() {
		assertThat(Leilao.vencedor(List.of(new Lance(1, 0)), 0, new SplittableRandom(1))).contains(new Lance(1, 0));
	}

	@Test
	void empateESorteadoEReproduzivel() {
		List<Lance> empate = List.of(new Lance(1, 150), new Lance(2, 150), new Lance(3, 100));
		Set<Long> vencedores = new HashSet<>();
		for (int semente = 0; semente < 50; semente++) {
			Lance v = Leilao.vencedor(empate, 100, new SplittableRandom(semente)).orElseThrow();
			assertThat(Leilao.vencedor(empate.reversed(), 100, new SplittableRandom(semente))).contains(v);
			vencedores.add(v.clubeId());
		}
		assertThat(vencedores).containsExactlyInAnyOrder(1L, 2L);
	}
}
