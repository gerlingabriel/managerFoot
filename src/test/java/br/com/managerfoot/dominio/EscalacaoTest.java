package br.com.managerfoot.dominio;

import static br.com.managerfoot.dominio.JogadoresDeTeste.titularesPara;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EscalacaoTest {

	@ParameterizedTest
	@EnumSource(Formacao.class)
	void aceitaEscalacaoQueRespeitaAFormacao(Formacao formacao) {
		Escalacao escalacao = new Escalacao(formacao, titularesPara(formacao, 20));

		assertThat(escalacao.titulares()).hasSize(11);
		assertThat(escalacao.jogadoresNa(Posicao.ATACANTE)).hasSize(formacao.quantidade(Posicao.ATACANTE));
	}

	@Test
	void recusaJogadoresForaDaPosicaoDaFormacao() {
		// titulares de um 4-4-2 usados num 4-3-3: um meia a mais e um atacante a menos
		List<Jogador> titulares = titularesPara(Formacao.F_4_4_2, 20);

		assertThatIllegalArgumentException().isThrownBy(() -> new Escalacao(Formacao.F_4_3_3, titulares))
			.withMessageStartingWith("Escalação inválida para 4-3-3")
			.withMessageContaining("faltam 1 atacante")
			.withMessageContaining("sobram 1 meio-campista");
	}

	@Test
	void semGoleiroERecusada() {
		List<Jogador> titulares = new ArrayList<>(titularesPara(Formacao.F_4_4_2, 20));
		titulares.removeIf(j -> j.posicao() == Posicao.GOLEIRO);

		assertThatIllegalArgumentException().isThrownBy(() -> new Escalacao(Formacao.F_4_4_2, titulares))
			.withMessageContaining("faltam 1 goleiro");
		assertThat(Escalacao.problemas(Formacao.F_4_4_2, titulares, List.of()))
			.contains("são necessários 11 titulares, mas há 10");
	}

	@Test
	void comMenosDeOnzeAVagaFicaVaziaComoUmExpulso() {
		List<Jogador> titulares = new ArrayList<>(titularesPara(Formacao.F_4_4_2, 20));
		titulares.remove(titulares.stream().filter(j -> j.posicao() == Posicao.ATACANTE).findFirst().orElseThrow());

		Escalacao escalacao = new Escalacao(Formacao.F_4_4_2, titulares);

		assertThat(escalacao.incompleta()).isTrue();
		assertThat(escalacao.jogadoresNa(Posicao.ATACANTE)).hasSize(1);
		assertThat(Escalacao.problemas(Formacao.F_4_4_2, titulares, List.of()))
			.containsExactly("são necessários 11 titulares, mas há 10", "faltam 1 atacante");
		assertThat(Escalacao.problemasPermitindoBuracos(Formacao.F_4_4_2, titulares, List.of())).isEmpty();
	}

	@Test
	void recusaMenosDeSeteTitulares() {
		List<Jogador> titulares = new ArrayList<>(titularesPara(Formacao.F_4_4_2, 20));
		titulares.removeIf(j -> j.posicao() == Posicao.MEIO_CAMPISTA);
		titulares.removeFirst();

		assertThatIllegalArgumentException().isThrownBy(() -> new Escalacao(Formacao.F_4_4_2, titulares))
			.withMessageContaining("são necessários de 7 a 11 titulares, mas há 6");
	}

	@Test
	void recusaJogadorRepetido() {
		List<Jogador> titulares = new ArrayList<>(titularesPara(Formacao.F_4_4_2, 20));
		Jogador atacante = titulares.get(10);
		titulares.set(9, atacante);

		assertThatIllegalArgumentException().isThrownBy(() -> new Escalacao(Formacao.F_4_4_2, titulares))
			.withMessageContaining(atacante.nome() + " aparece mais de uma vez");
	}

	@Test
	void aceitaAteSeteReservas() {
		List<Jogador> reservas = reservas(7);
		assertThat(new Escalacao(Formacao.F_4_4_2, titularesPara(Formacao.F_4_4_2, 20), reservas).reservas()).hasSize(7);
	}

	@Test
	void recusaMaisDeSeteReservas() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new Escalacao(Formacao.F_4_4_2, titularesPara(Formacao.F_4_4_2, 20), reservas(8)))
			.withMessageContaining("o banco aceita até 7 reservas, mas há 8");
	}

	@Test
	void recusaReservaQueTambemETitular() {
		List<Jogador> titulares = titularesPara(Formacao.F_4_4_2, 20);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new Escalacao(Formacao.F_4_4_2, titulares, List.of(titulares.get(5))))
			.withMessageContaining("aparece mais de uma vez");
	}

	private static List<Jogador> reservas(int quantidade) {
		List<Jogador> reservas = new ArrayList<>();
		for (int i = 0; i < quantidade; i++) {
			reservas.add(new Jogador(100 + i, "Reserva " + i, Posicao.MEIO_CAMPISTA, 15, Temperamento.NORMAL, "BRA"));
		}
		return reservas;
	}

	@Test
	void titularesNaoPodemSerAlteradosDepoisDeCriada() {
		List<Jogador> titulares = new ArrayList<>(titularesPara(Formacao.F_4_4_2, 20));
		Escalacao escalacao = new Escalacao(Formacao.F_4_4_2, titulares);
		titulares.clear();

		assertThat(escalacao.titulares()).hasSize(11);
	}
}
