package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Sorte;

class SorteTemporadaTest {

	private static List<Clube> clubes(int quantidade) {
		List<Clube> lista = new ArrayList<>();
		for (int i = 1; i <= quantidade; i++) {
			lista.add(new Clube(i, "C" + i, i % 50 + 1));
		}
		return lista;
	}

	@Test
	void beneficiadoEPrejudicadoSaoClubesDistintosDaDivisao() {
		List<Clube> divisao = clubes(20);
		for (long s = 0; s < 500; s++) {
			SorteTemporada sorte = SorteTemporada.sortear(divisao, new SplittableRandom(s));
			assertThat(sorte.beneficiado()).isNotEqualTo(sorte.prejudicado());
			assertThat(divisao).contains(sorte.beneficiado(), sorte.prejudicado());
		}
	}

	@Test
	void mesmaSementeGeraOMesmoSorteio() {
		List<Clube> divisao = clubes(20);
		assertThat(SorteTemporada.sortear(divisao, new SplittableRandom(9)))
			.isEqualTo(SorteTemporada.sortear(divisao, new SplittableRandom(9)));
	}

	@Test
	void qualquerClubePodeSerSorteadoNosDoisPapeis() {
		List<Clube> divisao = clubes(15);
		Set<Clube> beneficiados = new HashSet<>();
		Set<Clube> prejudicados = new HashSet<>();
		for (long s = 0; s < 2000; s++) {
			SorteTemporada sorte = SorteTemporada.sortear(divisao, new SplittableRandom(s));
			beneficiados.add(sorte.beneficiado());
			prejudicados.add(sorte.prejudicado());
		}
		assertThat(beneficiados).containsExactlyInAnyOrderElementsOf(divisao);
		assertThat(prejudicados).containsExactlyInAnyOrderElementsOf(divisao);
	}

	@Test
	void informaASorteDeCadaClube() {
		List<Clube> divisao = clubes(3);
		SorteTemporada sorte = new SorteTemporada(divisao.get(0), divisao.get(1));
		assertThat(sorte.de(divisao.get(0))).isEqualTo(Sorte.BENEFICIADO);
		assertThat(sorte.de(divisao.get(1))).isEqualTo(Sorte.PREJUDICADO);
		assertThat(sorte.de(divisao.get(2))).isEqualTo(Sorte.NEUTRA);
	}

	@Test
	void recusaDivisaoComMenosDeDoisClubesOuMesmoClubeNosDoisPapeis() {
		assertThatIllegalArgumentException().isThrownBy(() -> SorteTemporada.sortear(clubes(1), new SplittableRandom(1)));
		Clube c = clubes(1).getFirst();
		assertThatIllegalArgumentException().isThrownBy(() -> new SorteTemporada(c, c));
	}
}
