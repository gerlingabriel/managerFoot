package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.titulares;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

class TimeEmCampoTest {

	private static final double FATOR_IMPROVISADO = 0.5;

	/** 4-4-2 com força 20, trocando jogadores específicos por versões com outra força/asterisco. */
	private static List<Jogador> base() {
		return new ArrayList<>(titulares(Formacao.F_4_4_2, 20, 1, Temperamento.NORMAL));
	}

	private static Jogador com(Jogador j, int forca, boolean asterisco) {
		return new Jogador(j.id(), j.nome(), j.posicao(), forca, j.temperamento(), asterisco, j.nacionalidade());
	}

	@Test
	void expulsaoDeMeiaReduzOSetor() {
		TimeEmCampo time = new TimeEmCampo(new Escalacao(Formacao.F_4_4_2, base()), FATOR_IMPROVISADO, 5);
		Jogador meia = time.jogadoresNa(Posicao.MEIO_CAMPISTA).getFirst();

		time.expulsar(meia);

		assertThat(time.jogadoresNa(Posicao.MEIO_CAMPISTA)).hasSize(3).doesNotContain(meia);
		assertThat(time.soma(Posicao.MEIO_CAMPISTA)).isEqualTo(60);
		assertThat(time.emCampo()).hasSize(10);
	}

	@Test
	void goleiroExpulsoEhSubstituidoPeloDefensorMaisFracoComMetadeDaForca() {
		List<Jogador> titulares = base();
		Jogador defensorFraco = com(titulares.get(2), 12, false); // índice 1..4 = defensores
		titulares.set(2, defensorFraco);
		TimeEmCampo time = new TimeEmCampo(new Escalacao(Formacao.F_4_4_2, titulares), FATOR_IMPROVISADO, 5);

		time.expulsar(time.goleiro());

		assertThat(time.goleiro()).isEqualTo(defensorFraco);
		assertThat(time.forcaGoleiro()).isEqualTo(6);
		assertThat(time.jogadoresNa(Posicao.DEFENSOR)).hasSize(3).doesNotContain(defensorFraco);
		assertThat(time.emCampo()).hasSize(10);
	}

	@Test
	void semDefensoresOJogadorDeLinhaMaisFracoVaiParaOGol() {
		List<Jogador> titulares = base();
		Jogador atacanteFraco = com(titulares.get(10), 5, false);
		titulares.set(10, atacanteFraco);
		TimeEmCampo time = new TimeEmCampo(new Escalacao(Formacao.F_4_4_2, titulares), FATOR_IMPROVISADO, 5);
		List.copyOf(time.jogadoresNa(Posicao.DEFENSOR)).forEach(time::expulsar);

		time.expulsar(time.goleiro());

		assertThat(time.goleiro()).isEqualTo(atacanteFraco);
	}

	@Test
	void batedorEscolhidoCobraEnquantoEstiverEmCampo() {
		List<Jogador> titulares = base();
		Jogador escolhido = titulares.get(3); // um defensor
		TimeEmCampo time = new TimeEmCampo(new Escalacao(Formacao.F_4_4_2, titulares, escolhido), FATOR_IMPROVISADO, 5);

		assertThat(time.batedorPenalti(new SplittableRandom(1))).isEqualTo(escolhido);

		time.expulsar(escolhido);
		assertThat(time.batedorPenalti(new SplittableRandom(1))).isNotEqualTo(escolhido);
	}

	@Test
	void semEscolhaBateOMaisForteDeLinhaIgnorandoOGoleiro() {
		List<Jogador> titulares = base();
		titulares.set(0, com(titulares.get(0), 50, false)); // goleiro mais forte de todos
		Jogador meiaForte = com(titulares.get(6), 35, false);
		titulares.set(6, meiaForte);
		TimeEmCampo time = new TimeEmCampo(new Escalacao(Formacao.F_4_4_2, titulares), FATOR_IMPROVISADO, 5);

		assertThat(time.batedorPenalti(new SplittableRandom(1))).isEqualTo(meiaForte);
	}

	@Test
	void empateDeForcaDaPrioridadeAoAsterisco() {
		List<Jogador> titulares = base();
		titulares.set(5, com(titulares.get(5), 30, false));
		Jogador comAsterisco = com(titulares.get(9), 30, true);
		titulares.set(9, comAsterisco);
		TimeEmCampo time = new TimeEmCampo(new Escalacao(Formacao.F_4_4_2, titulares), FATOR_IMPROVISADO, 5);

		for (long s = 0; s < 50; s++) {
			assertThat(time.batedorPenalti(new SplittableRandom(s))).isEqualTo(comAsterisco);
		}
	}

	@Test
	void empatePersistenteEhSorteado() {
		List<Jogador> titulares = base();
		Jogador a = com(titulares.get(5), 30, false);
		Jogador b = com(titulares.get(9), 30, false);
		titulares.set(5, a);
		titulares.set(9, b);
		TimeEmCampo time = new TimeEmCampo(new Escalacao(Formacao.F_4_4_2, titulares), FATOR_IMPROVISADO, 5);

		Set<Jogador> sorteados = new HashSet<>();
		for (long s = 0; s < 50; s++) {
			sorteados.add(time.batedorPenalti(new SplittableRandom(s)));
		}
		assertThat(sorteados).containsExactlyInAnyOrder(a, b);
	}
}
