package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

class BonusArtilhariaTest {

	/** Elenco padrão de 23 (SPEC §5.2) para cada clube; o primeiro atacante e o primeiro defensor com asterisco. */
	private static List<Jogador> divisao(int clubes) {
		List<Jogador> jogadores = new ArrayList<>();
		long id = 1;
		for (int c = 0; c < clubes; c++) {
			int[] quantidades = { 3, 7, 7, 6 };
			Posicao[] posicoes = Posicao.values();
			for (int p = 0; p < posicoes.length; p++) {
				for (int i = 0; i < quantidades[p]; i++) {
					boolean asterisco = i == 0 && (posicoes[p] == Posicao.ATACANTE || posicoes[p] == Posicao.DEFENSOR);
					jogadores.add(new Jogador(id, "J" + id, posicoes[p], 25, Temperamento.NORMAL, asterisco));
					id++;
				}
			}
		}
		return jogadores;
	}

	@Test
	void sorteiaQuinzeJogadoresDeLinhaSemRepetir() {
		List<Jogador> divisao = divisao(20);
		BonusArtilharia bonus = BonusArtilharia.sortear(divisao, BonusArtilharia.QUANTIDADE_POR_DIVISAO, new SplittableRandom(3));

		Set<Long> todos = new HashSet<>(bonus.asteriscoTemporario());
		todos.addAll(bonus.bonusExtra());
		assertThat(todos).hasSize(15);
		assertThat(bonus.asteriscoTemporario()).doesNotContainAnyElementsOf(bonus.bonusExtra());
		assertThat(divisao.stream().filter(j -> todos.contains(j.id())))
			.noneMatch(j -> j.posicao() == Posicao.GOLEIRO);
	}

	@Test
	void separaQuemGanhaAsteriscoDeQuemGanhaBonusExtra() {
		List<Jogador> divisao = divisao(20);
		BonusArtilharia bonus = BonusArtilharia.sortear(divisao, 15, new SplittableRandom(5));
		for (Jogador j : divisao) {
			if (bonus.asteriscoTemporario().contains(j.id())) {
				assertThat(j.asterisco()).isFalse();
			}
			if (bonus.bonusExtra().contains(j.id())) {
				assertThat(j.asterisco()).isTrue();
			}
		}
	}

	@Test
	void defensoresComESemAsteriscoPodemSerSorteados() {
		List<Jogador> divisao = divisao(20);
		boolean defensorSem = false;
		boolean defensorCom = false;
		for (long s = 0; s < 200 && !(defensorSem && defensorCom); s++) {
			BonusArtilharia bonus = BonusArtilharia.sortear(divisao, 15, new SplittableRandom(s));
			for (Jogador j : divisao) {
				if (j.posicao() == Posicao.DEFENSOR) {
					defensorSem |= bonus.asteriscoTemporario().contains(j.id());
					defensorCom |= bonus.bonusExtra().contains(j.id());
				}
			}
		}
		assertThat(defensorSem).isTrue();
		assertThat(defensorCom).isTrue();
	}

	@Test
	void jogadorComAsteriscoTemPesoDobradoNoSorteio() {
		// 1 com asterisco e 1 sem, sorteando 1: o com asterisco deve sair ~2/3 das vezes
		Jogador com = new Jogador(1, "Com", Posicao.ATACANTE, 25, Temperamento.NORMAL, true);
		Jogador sem = new Jogador(2, "Sem", Posicao.ATACANTE, 25, Temperamento.NORMAL, false);
		int vezesCom = 0;
		for (long s = 0; s < 6000; s++) {
			vezesCom += BonusArtilharia.sortear(List.of(com, sem), 1, new SplittableRandom(s)).bonusExtra().size();
		}
		assertThat(vezesCom / 6000.0).isBetween(0.63, 0.70);
	}

	@Test
	void aplicaOAsteriscoTemporarioSoAosSorteadosSemAsterisco() {
		Jogador sem = new Jogador(1, "Sem", Posicao.MEIO_CAMPISTA, 25, Temperamento.NORMAL, false);
		Jogador outro = new Jogador(2, "Outro", Posicao.MEIO_CAMPISTA, 25, Temperamento.NORMAL, false);
		BonusArtilharia bonus = new BonusArtilharia(Set.of(1L), Set.of());

		assertThat(bonus.aplicar(sem).asterisco()).isTrue();
		assertThat(bonus.aplicar(sem).id()).isEqualTo(1);
		assertThat(bonus.aplicar(outro)).isSameAs(outro);
	}

	@Test
	void comPoucosElegiveisTodosSaoSorteadosEMesmaSementeRepete() {
		List<Jogador> poucos = divisao(1).subList(0, 6); // 3 goleiros e 3 defensores
		BonusArtilharia bonus = BonusArtilharia.sortear(poucos, 15, new SplittableRandom(1));
		assertThat(bonus.asteriscoTemporario().size() + bonus.bonusExtra().size()).isEqualTo(3);

		List<Jogador> divisao = divisao(20);
		assertThat(BonusArtilharia.sortear(divisao, 15, new SplittableRandom(8)))
			.isEqualTo(BonusArtilharia.sortear(divisao, 15, new SplittableRandom(8)));
		assertThatIllegalArgumentException().isThrownBy(() -> BonusArtilharia.sortear(divisao, -1, new SplittableRandom(1)));
	}
}
