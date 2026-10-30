package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;

/** Quem ocupa as vagas de cada país (SPEC §8.3.2, §8.3.3 e §8.3.6). */
class VagasContinentalTest {

	static final List<Clube> TABELA = ContinentalTest.clubes(20);

	@Test
	void campeaoDaCopaForaDoG7EntraNoLugarDoSetimo() {
		Clube campeao = TABELA.get(12);
		assertThat(VagasContinental.peloDesempenho(TABELA, Optional.of(campeao), 7))
			.containsExactlyInAnyOrder(TABELA.get(0), TABELA.get(1), TABELA.get(2), TABELA.get(3), TABELA.get(4),
					TABELA.get(5), campeao);
	}

	@Test
	void campeaoDaCopaJaClassificadoPelaLigaPassaAVagaAoSeguinte() {
		assertThat(VagasContinental.peloDesempenho(TABELA, Optional.of(TABELA.get(2)), 7))
			.containsExactlyInAnyOrderElementsOf(TABELA.subList(0, 7));
	}

	@Test
	void campeaoDaCopaDeFuraDaPrimeiraDivisaoTambemOcupaUmaVaga() {
		Clube daSegunda = new Clube(99, "Da 2ª", 25);
		assertThat(VagasContinental.peloDesempenho(TABELA, Optional.of(daSegunda), 3))
			.containsExactlyInAnyOrder(daSegunda, TABELA.get(0), TABELA.get(1));
	}

	@Test
	void semCopaOuSemVagas() {
		assertThat(VagasContinental.peloDesempenho(TABELA, Optional.empty(), 2)).containsExactly(TABELA.get(0), TABELA.get(1));
		assertThat(VagasContinental.peloDesempenho(TABELA, Optional.of(TABELA.get(5)), 0)).isEmpty();
	}

	@Test
	void paisComMenosClubesQueVagasLevaSoOsQueTem() {
		List<Clube> dois = TABELA.subList(0, 2);
		assertThat(VagasContinental.peloDesempenho(dois, Optional.empty(), 6)).hasSize(2);
		assertThat(VagasContinental.porForca(dois, Map.of(), 6, 3, new SplittableRandom(1))).hasSize(2);
	}

	@Test
	void porForcaPrefereOsFortesSemSerSempreOsMesmos() {
		List<Clube> clubes = ContinentalTest.clubes(8);
		// o clube 1 tem força 45 e o 8, 31: a faixa do nível B
		Map<Long, List<Jogador>> planteis = ContinentalTest.planteis(clubes, c -> 47 - 2 * (int) c.id());
		Map<Long, Integer> vezes = new HashMap<>();
		List<List<Clube>> resultados = new java.util.ArrayList<>();
		for (int semente = 0; semente < 2000; semente++) {
			List<Clube> escolhidos = VagasContinental.porForca(clubes, planteis, 4, 3, new SplittableRandom(semente));
			assertThat(escolhidos).hasSize(4).doesNotHaveDuplicates();
			escolhidos.forEach(c -> vezes.merge(c.id(), 1, Integer::sum));
			resultados.add(escolhidos);
		}
		assertThat(vezes.get(1L)).isGreaterThan(vezes.get(4L)).isGreaterThan(0);
		assertThat(vezes.get(4L)).isGreaterThan(vezes.get(8L));
		assertThat(vezes.get(8L)).isGreaterThan(0);
		assertThat(resultados.stream().map(java.util.Set::copyOf).distinct().count()).isGreaterThan(10);
	}

	@Test
	void mesmaSementeMesmaEscolha() {
		Map<Long, List<Jogador>> planteis = ContinentalTest.planteis(TABELA, c -> 30);
		assertThat(VagasContinental.porForca(TABELA, planteis, 5, 3, new SplittableRandom(4)))
			.isEqualTo(VagasContinental.porForca(TABELA, planteis, 5, 3, new SplittableRandom(4)));
	}
}
