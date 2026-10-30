package br.com.managerfoot.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.LongPredicate;

import org.junit.jupiter.api.Test;

class EscalacaoDoTreinadorTest {

	/** 3 goleiros, 7 defensores, 7 meias e 6 atacantes, com ids de 1 a 23 nessa ordem. */
	private static final List<Jogador> PLANTEL = plantel();
	private static final Escalacao DA_MAQUINA = new Escalacao(Formacao.F_4_4_2,
			List.of(j(1), j(4), j(5), j(6), j(7), j(11), j(12), j(13), j(14), j(18), j(19)),
			List.of(j(2), j(8), j(15), j(20)), j(18));

	@Test
	void partindoDaMaquinaEhValidaEDevolveAMesmaEscalacao() {
		EscalacaoDoTreinador escalacao = EscalacaoDoTreinador.de(DA_MAQUINA);

		assertThat(escalacao.problemas(PLANTEL, id -> false)).isEmpty();
		assertThat(escalacao.escalacao(PLANTEL, id -> false, 0.5)).isEqualTo(DA_MAQUINA);
	}

	@Test
	void trocarDeFormacaoSemMudarOsJogadoresDizOQueFaltaESobra() {
		EscalacaoDoTreinador escalacao = EscalacaoDoTreinador.de(DA_MAQUINA);
		EscalacaoDoTreinador emTresCincoDois = new EscalacaoDoTreinador(Formacao.F_3_5_2, escalacao.titulares(),
				escalacao.reservas(), null);

		assertThat(emTresCincoDois.problemas(PLANTEL, id -> false))
			.containsExactlyInAnyOrder("sobram 1 defensor", "faltam 1 meio-campista");
		assertThatIllegalStateException().isThrownBy(() -> emTresCincoDois.escalacao(PLANTEL, id -> false, 0.5))
			.withMessageContaining("faltam 1 meio-campista");
	}

	@Test
	void suspensoEJogadorDeOutroClubeSaoProblemas() {
		EscalacaoDoTreinador escalacao = EscalacaoDoTreinador.de(DA_MAQUINA);

		assertThat(escalacao.problemas(PLANTEL, id -> id == 19)).containsExactly("J19 está suspenso");
		EscalacaoDoTreinador comEstranho = new EscalacaoDoTreinador(Formacao.F_4_4_2, escalacao.titulares(), Set.of(999L),
				null);
		assertThat(comEstranho.problemas(PLANTEL, id -> false)).containsExactly("o jogador 999 não é do clube");
	}

	@Test
	void lesionadoNaoPodeJogar() {
		EscalacaoDoTreinador escalacao = EscalacaoDoTreinador.de(DA_MAQUINA);

		assertThat(escalacao.problemas(PLANTEL, id -> false, id -> id == 19)).containsExactly("J19 está lesionado");
		assertThatIllegalStateException()
			.isThrownBy(() -> escalacao.escalacao(PLANTEL, id -> false, id -> id == 19, 0.5))
			.withMessageContaining("J19 está lesionado");
	}

	@Test
	void semTiraDoTimeEDoPenalti() {
		EscalacaoDoTreinador sem = EscalacaoDoTreinador.de(DA_MAQUINA).sem(Set.of(18L, 2L));

		assertThat(sem.titulares()).doesNotContain(18L).hasSize(10);
		assertThat(sem.reservas()).doesNotContain(2L).hasSize(3);
		assertThat(sem.batedor()).isNull();
		assertThat(sem.problemas(PLANTEL, id -> false))
			.containsExactly("são necessários 11 titulares, mas há 10", "faltam 1 atacante");
	}

	@Test
	void semAtacanteDisponivelJogaComUmAMenos() {
		// atacantes 18 a 23: o 18 está no time incompleto e os outros 5 estão suspensos (o 20 sai do banco)
		EscalacaoDoTreinador semUm = EscalacaoDoTreinador.de(DA_MAQUINA).sem(Set.of(19L, 20L));

		assertThat(semUm.problemas(PLANTEL, id -> id >= 19)).isEmpty();
		Escalacao escalacao = semUm.escalacao(PLANTEL, id -> id >= 19, 0.5);
		assertThat(escalacao.incompleta()).isTrue();
		assertThat(escalacao.titulares()).hasSize(10);
		// o banco (goleiro, defensor e meia) não cabe na vaga do ataque
		assertThat(escalacao.reservas()).hasSize(3);
	}

	@Test
	void naoJogaComUmAMenosSeHaQuemCaibaNaVaga() {
		// o 20 está no banco e o 21 fora: qualquer um deles completa o ataque
		EscalacaoDoTreinador semUm = EscalacaoDoTreinador.de(DA_MAQUINA).sem(Set.of(19L));

		assertThat(semUm.problemas(PLANTEL, id -> id == 19 || id >= 22))
			.containsExactly("são necessários 11 titulares, mas há 10", "faltam 1 atacante");
	}

	@Test
	void batedorPrecisaEstarNoTimeEJogadorNaoEstaEmDoisLugares() {
		EscalacaoDoTreinador escalacao = EscalacaoDoTreinador.de(DA_MAQUINA);
		EscalacaoDoTreinador batedorDeFora = new EscalacaoDoTreinador(Formacao.F_4_4_2, escalacao.titulares(),
				escalacao.reservas(), 23L);

		assertThat(batedorDeFora.problemas(PLANTEL, id -> false))
			.containsExactly("o batedor de pênalti precisa estar entre os titulares ou no banco");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new EscalacaoDoTreinador(Formacao.F_4_4_2, Set.of(1L), Set.of(1L), null));
	}

	@Test
	void semGoleiroDisponivelODefensorMaisFracoVaiProGolComForcaReduzida() {
		// goleiros 1 a 3 suspensos; 5 defensores no time de um 4-4-2
		EscalacaoDoTreinador semGoleiro = new EscalacaoDoTreinador(Formacao.F_4_4_2,
				Set.of(4L, 5L, 6L, 7L, 8L, 11L, 12L, 13L, 14L, 18L, 19L), Set.of(20L), 4L);

		assertThat(semGoleiro.problemas(PLANTEL, id -> id <= 3)).isEmpty();
		assertThat(semGoleiro.goleiroImprovisado(PLANTEL, id -> id <= 3)).contains(j(4));
		Escalacao escalacao = semGoleiro.escalacao(PLANTEL, id -> id <= 3, 0.5);
		Jogador noGol = escalacao.jogadoresNa(Posicao.GOLEIRO).getFirst();
		assertThat(noGol.id()).isEqualTo(4);
		assertThat(noGol.forca()).isEqualTo(10);
		assertThat(escalacao.batedorPreferido()).contains(noGol);
	}

	@Test
	void comGoleiroDisponivelNaoImprovisa() {
		EscalacaoDoTreinador semGoleiro = new EscalacaoDoTreinador(Formacao.F_4_4_2,
				Set.of(4L, 5L, 6L, 7L, 8L, 11L, 12L, 13L, 14L, 18L, 19L), Set.of(), null);

		assertThat(semGoleiro.goleiroImprovisado(PLANTEL, id -> id <= 2)).isEmpty();
		assertThat(semGoleiro.problemas(PLANTEL, id -> id <= 2)).contains("faltam 1 goleiro", "sobram 1 defensor");
	}

	@Test
	void improvisadoQueDesfalcaASetorAvisaQuemVaiProGol() {
		// só 4 defensores: o mais fraco vai para o gol e falta um na defesa
		EscalacaoDoTreinador semGoleiro = new EscalacaoDoTreinador(Formacao.F_4_4_2,
				Set.of(4L, 5L, 6L, 7L, 11L, 12L, 13L, 14L, 15L, 18L, 19L), Set.of(), null);

		assertThat(semGoleiro.problemas(PLANTEL, id -> id <= 3)).containsExactly("faltam 1 defensor",
				"sobram 1 meio-campista", "sem goleiro disponível, J4 vai para o gol");
	}

	@Test
	void bancoComMaisDeSeteEhProblema() {
		EscalacaoDoTreinador escalacao = EscalacaoDoTreinador.de(DA_MAQUINA);
		Set<Long> oito = Set.of(2L, 3L, 8L, 9L, 10L, 15L, 16L, 20L);

		assertThat(new EscalacaoDoTreinador(Formacao.F_4_4_2, escalacao.titulares(), oito, null).problemas(PLANTEL, id -> false))
			.containsExactly("o banco aceita até 7 reservas, mas há 8");
	}

	@Test
	void taticaComTimeVazioEscalaOsMaisFortesEOBancoComGoleiro() {
		List<Jogador> plantel = comForcas(Map.of(8L, 41, 9L, 42, 10L, 43, 4L, 44, 15L, 45, 16L, 46, 17L, 47, 14L, 48,
				22L, 49, 23L, 50));

		EscalacaoDoTreinador time = vazia(Formacao.F_4_4_2).completada(plantel, id -> false, 3, new Random(1));

		assertThat(time.titulares()).hasSize(11).contains(4L, 8L, 9L, 10L, 14L, 15L, 16L, 17L, 22L, 23L);
		assertThat(time.reservas()).hasSize(Escalacao.MAXIMO_RESERVAS)
			.anyMatch(id -> plantel.get((int) (long) id - 1).posicao() == Posicao.GOLEIRO);
		assertThat(time.problemas(plantel, id -> false)).isEmpty();
	}

	@Test
	void quemJaEstaEscaladoFicaESoAVagaEhCompletada() {
		EscalacaoDoTreinador antes = EscalacaoDoTreinador.de(DA_MAQUINA).com(4, Lugar.FORA);
		List<Jogador> plantel = comForcas(Map.of(10L, 45));

		EscalacaoDoTreinador time = antes.completada(plantel, id -> false, 3, new Random(1));

		assertThat(time.titulares()).containsAll(antes.titulares()).contains(10L).hasSize(11);
		assertThat(time.reservas()).containsAll(antes.reservas()).hasSize(Escalacao.MAXIMO_RESERVAS);
	}

	@Test
	void trocarDeTaticaManteOsMaisFortesEOQueSobraVaiParaOBanco() {
		List<Jogador> plantel = comForcas(Map.of(5L, 10, 15L, 45));

		EscalacaoDoTreinador time = EscalacaoDoTreinador.de(DA_MAQUINA).com(Formacao.F_3_5_2)
			.completada(plantel, id -> false, 3, new Random(1));

		assertThat(time.titulares()).doesNotContain(5L).contains(15L).hasSize(11);
		assertThat(time.reservas()).contains(5L).doesNotContain(15L);
		assertThat(time.problemas(plantel, id -> false)).isEmpty();
	}

	@Test
	void semTitularFaltandoNadaMuda() {
		EscalacaoDoTreinador time = new EscalacaoDoTreinador(Formacao.F_4_4_2, EscalacaoDoTreinador.de(DA_MAQUINA).titulares(),
				Set.of(), null);

		assertThat(time.completada(PLANTEL, id -> false, 3, new Random(1))).isSameAs(time);
	}

	@Test
	void noEmpateDeForcaOAsteriscoVemPrimeiroEDepoisASorte() {
		List<Jogador> plantel = new ArrayList<>(PLANTEL);
		plantel.set(2, new Jogador(3, "J3", Posicao.GOLEIRO, 20, Temperamento.NORMAL, true, "BRA"));

		assertThat(vazia(Formacao.F_4_4_2).completada(plantel, id -> false, 3, new Random(1)).titulares()).contains(3L);

		Set<Long> goleiros = new HashSet<>();
		for (long semente = 0; semente < 50; semente++) {
			Set<Long> titulares = vazia(Formacao.F_4_4_2).completada(PLANTEL, id -> false, 3, new Random(semente)).titulares();
			goleiros.addAll(titulares.stream().filter(id -> id <= 3).toList());
		}
		assertThat(goleiros).containsExactlyInAnyOrder(1L, 2L, 3L);
		assertThat(vazia(Formacao.F_4_4_2).completada(PLANTEL, id -> false, 3, new Random(7)))
			.isEqualTo(vazia(Formacao.F_4_4_2).completada(PLANTEL, id -> false, 3, new Random(7)));
	}

	@Test
	void osEscolhidosRespeitamOLimiteDeAsteriscosSeHouverOutroNaPosicao() {
		List<Jogador> plantel = new ArrayList<>(PLANTEL);
		for (long id : List.of(4L, 5L, 6L, 7L)) {
			plantel.set((int) id - 1, new Jogador(id, "J" + id, Posicao.DEFENSOR, 30, Temperamento.NORMAL, true, "BRA"));
		}

		EscalacaoDoTreinador time = vazia(Formacao.F_4_4_2).completada(plantel, id -> false, 3, new Random(1));

		assertThat(time.titulares().stream().filter(id -> id >= 4 && id <= 7)).hasSize(3);
		assertThat(time.titulares()).hasSize(11);
	}

	@Test
	void suspensoELesionadoNaoSaoEscolhidos() {
		List<Jogador> plantel = comForcas(Map.of(22L, 50, 23L, 49));

		EscalacaoDoTreinador time = vazia(Formacao.F_4_4_2).completada(plantel, id -> id == 22 || id == 23, 3, new Random(1));

		assertThat(time.titulares()).doesNotContain(22L, 23L).hasSize(11);
		assertThat(time.reservas()).doesNotContain(22L, 23L);
	}

	@Test
	void semGoleiroDisponivelEscalaUmDefensorAMaisParaOGol() {
		LongPredicate semGoleiro = id -> id <= 3;

		EscalacaoDoTreinador time = vazia(Formacao.F_4_4_2).completada(PLANTEL, semGoleiro, 3, new Random(1));

		assertThat(time.titulares()).hasSize(11);
		assertThat(time.goleiroImprovisado(PLANTEL, semGoleiro)).isPresent();
		assertThat(time.problemas(PLANTEL, semGoleiro)).isEmpty();
	}

	@Test
	void faltandoJogadorNaPosicaoAVagaFicaVazia() {
		LongPredicate soDoisAtacantes = id -> id >= 20 && id <= 23;

		EscalacaoDoTreinador time = vazia(Formacao.F_3_4_3).completada(PLANTEL, soDoisAtacantes, 3, new Random(1));

		assertThat(time.titulares()).hasSize(10);
		assertThat(time.problemas(PLANTEL, soDoisAtacantes)).isEmpty();
	}

	@Test
	void retirarTodosDeixaSoAFormacao() {
		EscalacaoDoTreinador vazia = EscalacaoDoTreinador.de(DA_MAQUINA).vazia();

		assertThat(vazia).isEqualTo(new EscalacaoDoTreinador(Formacao.F_4_4_2, Set.of(), Set.of(), null));
	}

	private static EscalacaoDoTreinador vazia(Formacao formacao) {
		return new EscalacaoDoTreinador(formacao, Set.of(), Set.of(), null);
	}

	private static List<Jogador> comForcas(Map<Long, Integer> forcas) {
		return PLANTEL.stream().map(j -> new Jogador(j.id(), j.nome(), j.posicao(), forcas.getOrDefault(j.id(), j.forca()),
				j.temperamento(), j.asterisco(), j.nacionalidade())).toList();
	}

	private static Jogador j(long id) {
		return PLANTEL.get((int) id - 1);
	}

	private static List<Jogador> plantel() {
		int[] quantidades = { 3, 7, 7, 6 };
		List<Jogador> plantel = new ArrayList<>();
		for (Posicao posicao : Posicao.values()) {
			for (int i = 0; i < quantidades[posicao.ordinal()]; i++) {
				long id = plantel.size() + 1;
				plantel.add(new Jogador(id, "J" + id, posicao, 20, Temperamento.NORMAL, "BRA"));
			}
		}
		return plantel;
	}
}
