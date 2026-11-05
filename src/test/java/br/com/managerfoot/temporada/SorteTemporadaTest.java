package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Sorte;
import br.com.managerfoot.dominio.Temperamento;

/** Sorte da temporada (SPEC §5.3.1, rev. 108). */
class SorteTemporadaTest {

	private static final ParametrosSorteTemporada PADRAO = ParametrosSorteTemporada.padrao();

	private static List<Clube> clubes(int quantidade) {
		List<Clube> lista = new ArrayList<>();
		for (int i = 1; i <= quantidade; i++) {
			lista.add(new Clube(i, "C" + i, i % 50 + 1));
		}
		return lista;
	}

	/** Plantel de 20 jogadores com a média dada (metade com média − 1, metade com média + 1). */
	private static List<Jogador> plantel(long clube, int media) {
		List<Jogador> jogadores = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			jogadores.add(new Jogador(clube * 100 + i, "J" + i, Posicao.MEIO_CAMPISTA, media + (i % 2 == 0 ? -1 : 1),
					Temperamento.NORMAL, "BRA"));
		}
		return jogadores;
	}

	@Test
	void doisClubesDaMaquinaDistintosSemPrejudicado() {
		List<Clube> divisao = clubes(20);
		for (long s = 0; s < 500; s++) {
			SorteTemporada sorte = SorteTemporada.sortear(divisao, Set.of(), Map.of(), PADRAO, new SplittableRandom(s));
			assertThat(sorte.beneficiados()).hasSize(2).allMatch(divisao::contains);
			divisao.stream().filter(c -> !sorte.beneficiados().contains(c))
				.forEach(c -> assertThat(sorte.de(c)).isEqualTo(Sorte.NEUTRA));
		}
	}

	@Test
	void todoClubeDaMaquinaPodeSerSorteado() {
		List<Clube> divisao = clubes(20);
		Set<Clube> sorteados = new HashSet<>();
		for (long s = 0; s < 500; s++) {
			sorteados.addAll(SorteTemporada.sortear(divisao, Set.of(), Map.of(), PADRAO, new SplittableRandom(s)).beneficiados());
		}
		assertThat(sorteados).containsExactlyInAnyOrderElementsOf(divisao);
	}

	@Test
	void clubeDeUsuarioNuncaEntraNoSorteioEForteNaoTemOBeneficio() {
		List<Clube> divisao = clubes(20);
		Clube doUsuario = divisao.get(4);
		Map<Long, List<Jogador>> planteis = Map.of(doUsuario.id(), plantel(doUsuario.id(), 40));
		for (long s = 0; s < 500; s++) {
			SorteTemporada sorte = SorteTemporada.sortear(divisao, Set.of(doUsuario.id()), planteis, PADRAO, new SplittableRandom(s));
			assertThat(sorte.beneficiados()).hasSize(2).doesNotContain(doUsuario);
		}
	}

	@Test
	void clubeDeUsuarioComMediaAbaixoDe40TemOBeneficioAlemDosDoisSorteados() {
		List<Clube> divisao = clubes(20);
		Clube fraco = divisao.get(2);
		Clube forte = divisao.get(3);
		Map<Long, List<Jogador>> planteis = new HashMap<>();
		planteis.put(fraco.id(), plantel(fraco.id(), 39));
		planteis.put(forte.id(), plantel(forte.id(), 41));

		SorteTemporada sorte = SorteTemporada.sortear(divisao, Set.of(fraco.id(), forte.id()), planteis, PADRAO,
				new SplittableRandom(7));

		assertThat(sorte.beneficiados()).hasSize(3).contains(fraco).doesNotContain(forte);
		assertThat(sorte.de(fraco)).isEqualTo(Sorte.BENEFICIADO);
		assertThat(sorte.de(forte)).isEqualTo(Sorte.NEUTRA);
	}

	@Test
	void mediaExatamente40NaoTemOBeneficio() {
		Clube doUsuario = new Clube(1, "U", 40);
		List<Clube> divisao = List.of(doUsuario, new Clube(2, "A", 30), new Clube(3, "B", 30));
		SorteTemporada sorte = SorteTemporada.sortear(divisao, Set.of(1L), Map.of(1L, plantel(1, 40)), PADRAO,
				new SplittableRandom(1));
		assertThat(sorte.beneficiados()).doesNotContain(doUsuario).hasSize(2);
	}

	@Test
	void mesmaSementeMesmoSorteio() {
		List<Clube> divisao = clubes(20);
		assertThat(SorteTemporada.sortear(divisao, Set.of(), Map.of(), PADRAO, new SplittableRandom(9)))
			.isEqualTo(SorteTemporada.sortear(divisao, Set.of(), Map.of(), PADRAO, new SplittableRandom(9)));
	}

	@Test
	void divisaoComPoucosClubesDaMaquinaSorteiaOsQueHa() {
		List<Clube> divisao = clubes(3);
		SorteTemporada sorte = SorteTemporada.sortear(divisao, Set.of(1L, 2L), Map.of(), PADRAO, new SplittableRandom(1));
		assertThat(sorte.beneficiados()).containsExactly(divisao.get(2));
	}
}
