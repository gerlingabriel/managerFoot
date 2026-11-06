package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

class BonusTreinadorTest {

	private final ParametrosBonusTreinador parametros = ParametrosBonusTreinador.padrao();

	@Test
	void ateTrintaSorteiaTresComMaisDezDeTrintaEUmAQuarentaDoisComMaisCincoAcimaNada() {
		assertThat(BonusTreinador.sortear(plantel(30, false), Set.of(), parametros, new SplittableRandom(1)))
			.hasSize(3).allMatch(s -> s.forcaExtra() == 10);
		assertThat(BonusTreinador.sortear(plantel(31, false), Set.of(), parametros, new SplittableRandom(1)))
			.hasSize(2).allMatch(s -> s.forcaExtra() == 5);
		assertThat(BonusTreinador.sortear(plantel(40, false), Set.of(), parametros, new SplittableRandom(1))).hasSize(2);
		assertThat(BonusTreinador.sortear(plantel(41, false), Set.of(), parametros, new SplittableRandom(1))).isEmpty();
	}

	@Test
	void goleiroNuncaEQuemJaTemFicaDeFora() {
		List<Jogador> plantel = plantel(20, false);
		for (int s = 0; s < 200; s++) {
			List<BonusTreinador.Sorteado> sorteados = BonusTreinador.sortear(plantel, Set.of(4L, 5L), parametros,
					new SplittableRandom(s));
			assertThat(sorteados).extracting(BonusTreinador.Sorteado::jogadorId)
				.doesNotContain(1L, 2L, 3L, 4L, 5L)
				.doesNotHaveDuplicates();
		}
	}

	@Test
	void quemJaTemAsteriscoGanhaOBonusExtraEOsOutrosOAsteriscoTemporario() {
		List<BonusTreinador.Sorteado> comAsterisco = BonusTreinador.sortear(plantel(20, true), Set.of(), parametros,
				new SplittableRandom(3));
		assertThat(comAsterisco).allMatch(BonusTreinador.Sorteado::extra);
		BonusTreinador bonus = BonusTreinador.de(comAsterisco);
		assertThat(bonus.bonusExtra()).hasSize(3);

		BonusTreinador semAsterisco = BonusTreinador.de(List.of(new BonusTreinador.Sorteado(9, 10, false)));
		Jogador emCampo = semAsterisco.aplicar(jogador(9, Posicao.ATACANTE, 22, false));
		assertThat(emCampo.forca()).isEqualTo(32);
		assertThat(emCampo.asterisco()).isTrue();
		assertThat(semAsterisco.bonusExtra()).isEmpty();
		// quem não foi sorteado entra igual
		Jogador outro = jogador(8, Posicao.ATACANTE, 22, false);
		assertThat(semAsterisco.aplicar(outro)).isSameAs(outro);
	}

	@Test
	void aForcaEmCampoNaoPassaDeCinquenta() {
		BonusTreinador bonus = BonusTreinador.de(List.of(new BonusTreinador.Sorteado(9, 10, true)));
		Jogador emCampo = bonus.aplicar(jogador(9, Posicao.MEIO_CAMPISTA, 45, true));
		assertThat(emCampo.forca()).isEqualTo(50);
		assertThat(emCampo.asterisco()).isTrue();
	}

	@Test
	void mesmaSementeMesmoSorteio() {
		List<Jogador> plantel = plantel(25, false);
		assertThat(BonusTreinador.sortear(plantel, Set.of(), parametros, new SplittableRandom(7)))
			.isEqualTo(BonusTreinador.sortear(plantel.reversed(), Set.of(), parametros, new SplittableRandom(7)));
	}

	/** 3 goleiros (ids 1 a 3) e 20 de linha (4 a 23), todos com a força dada: a força em campo é ela. */
	private static List<Jogador> plantel(int forca, boolean asterisco) {
		List<Jogador> p = new ArrayList<>();
		for (long id = 1; id <= 23; id++) {
			p.add(jogador(id, id <= 3 ? Posicao.GOLEIRO : Posicao.values()[1 + (int) (id % 3)], forca, asterisco));
		}
		return p;
	}

	private static Jogador jogador(long id, Posicao posicao, int forca, boolean asterisco) {
		return new Jogador(id, "J" + id, posicao, forca, Temperamento.NORMAL, asterisco, "BRA");
	}
}
