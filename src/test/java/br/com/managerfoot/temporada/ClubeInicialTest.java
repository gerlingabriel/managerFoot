package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;

class ClubeInicialTest {

	private static Divisao divisao(int... forcas) {
		List<Clube> clubes = IntStream.range(0, forcas.length)
			.mapToObj(i -> new Clube(i + 1, "C" + (i + 1), forcas[i]))
			.toList();
		return new Divisao("Série C", 3, 20, 2, 2, clubes);
	}

	private static List<Long> ids(List<Clube> clubes) {
		return clubes.stream().map(Clube::id).toList();
	}

	@Test
	void candidatosSaoOsQuatroMaisFracos() {
		Divisao serieC = divisao(20, 8, 15, 3, 12, 5, 18, 9);
		assertThat(ids(ClubeInicial.candidatos(serieC, 4))).containsExactly(2L, 4L, 6L, 8L);
	}

	@Test
	void empatadosComOQuartoTambemEntram() {
		Divisao serieC = divisao(10, 5, 5, 20, 5, 5, 5, 30);
		assertThat(ids(ClubeInicial.candidatos(serieC, 4))).containsExactly(2L, 3L, 5L, 6L, 7L);
	}

	@Test
	void divisaoMenorQueOGrupoSorteiaEntreTodos() {
		Divisao pequena = divisao(10, 30, 20);
		assertThat(ids(ClubeInicial.candidatos(pequena, 4))).containsExactly(1L, 2L, 3L);
	}

	@Test
	void tamanhoDoGrupoEConfiguravel() {
		Divisao serieC = divisao(20, 8, 15, 3, 12);
		assertThat(ids(ClubeInicial.candidatos(serieC, 2))).containsExactly(2L, 4L);
		assertThatIllegalArgumentException().isThrownBy(() -> ClubeInicial.candidatos(serieC, 0));
		assertThatIllegalArgumentException().isThrownBy(() -> ClubeInicial.candidatos(divisao(), 4));
	}

	@Test
	void mesmaSementeSorteiaOMesmoClube() {
		Divisao serieC = divisao(20, 8, 15, 3, 12, 5, 18, 9);
		assertThat(ClubeInicial.sortear(serieC, 4, new SplittableRandom(7)))
			.isEqualTo(ClubeInicial.sortear(serieC, 4, new SplittableRandom(7)));
	}

	@Test
	void todosOsCandidatosPodemSairENenhumOutro() {
		Divisao serieC = divisao(20, 8, 15, 3, 12, 5, 18, 9);
		Set<Clube> sorteados = new HashSet<>();
		for (long s = 0; s < 500; s++) {
			sorteados.add(ClubeInicial.sortear(serieC, 4, new SplittableRandom(s)));
		}
		assertThat(sorteados).containsExactlyInAnyOrderElementsOf(ClubeInicial.candidatos(serieC, 4));
	}

	@Test
	void emRedeOGrupoCresceUmPorClubeOcupadoESemOsOcupados() {
		// forças 1 a 8: os 4 mais fracos são 1 a 4; com 2 ocupados o grupo vai a 6 e eles saem
		Divisao serieC = divisao(8, 7, 6, 5, 4, 3, 2, 1);
		assertThat(ids(ClubeInicial.candidatos(serieC, 4, Set.of()))).containsExactly(5L, 6L, 7L, 8L);
		assertThat(ids(ClubeInicial.candidatos(serieC, 4, Set.of(8L, 7L)))).containsExactly(3L, 4L, 5L, 6L);
	}

	@Test
	void emRedeOcupadoDeOutraDivisaoTambemAumentaOGrupo() {
		Divisao serieC = divisao(8, 7, 6, 5, 4, 3, 2, 1);
		assertThat(ids(ClubeInicial.candidatos(serieC, 4, Set.of(99L)))).containsExactly(4L, 5L, 6L, 7L, 8L);
	}

	@Test
	void emRedeSemClubeLivreNaoHaCandidatos() {
		Divisao serieC = divisao(5, 5);
		assertThat(ClubeInicial.candidatos(serieC, 4, Set.of(1L, 2L))).isEmpty();
	}
}
