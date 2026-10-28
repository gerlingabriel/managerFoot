package br.com.managerfoot.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
		assertThat(escalacao.escalacao(PLANTEL, id -> false)).isEqualTo(DA_MAQUINA);
	}

	@Test
	void trocarDeFormacaoSemMudarOsJogadoresDizOQueFaltaESobra() {
		EscalacaoDoTreinador escalacao = EscalacaoDoTreinador.de(DA_MAQUINA);
		EscalacaoDoTreinador emTresCincoDois = new EscalacaoDoTreinador(Formacao.F_3_5_2, escalacao.titulares(),
				escalacao.reservas(), null);

		assertThat(emTresCincoDois.problemas(PLANTEL, id -> false))
			.containsExactlyInAnyOrder("sobram 1 defensor", "faltam 1 meio-campista");
		assertThatIllegalStateException().isThrownBy(() -> emTresCincoDois.escalacao(PLANTEL, id -> false))
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
	void semTiraDoTimeEDoPenalti() {
		EscalacaoDoTreinador sem = EscalacaoDoTreinador.de(DA_MAQUINA).sem(Set.of(18L, 2L));

		assertThat(sem.titulares()).doesNotContain(18L).hasSize(10);
		assertThat(sem.reservas()).doesNotContain(2L).hasSize(3);
		assertThat(sem.batedor()).isNull();
		assertThat(sem.problemas(PLANTEL, id -> false))
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
	void bancoComMaisDeSeteEhProblema() {
		EscalacaoDoTreinador escalacao = EscalacaoDoTreinador.de(DA_MAQUINA);
		Set<Long> oito = Set.of(2L, 3L, 8L, 9L, 10L, 15L, 16L, 20L);

		assertThat(new EscalacaoDoTreinador(Formacao.F_4_4_2, escalacao.titulares(), oito, null).problemas(PLANTEL, id -> false))
			.containsExactly("o banco aceita até 7 reservas, mas há 8");
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
