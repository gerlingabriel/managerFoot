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
	void recusaMenosDeOnzeTitulares() {
		List<Jogador> titulares = new ArrayList<>(titularesPara(Formacao.F_4_4_2, 20));
		titulares.removeIf(j -> j.posicao() == Posicao.GOLEIRO);

		assertThatIllegalArgumentException().isThrownBy(() -> new Escalacao(Formacao.F_4_4_2, titulares))
			.withMessageContaining("são necessários 11 titulares, mas há 10")
			.withMessageContaining("faltam 1 goleiro");
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
	void titularesNaoPodemSerAlteradosDepoisDeCriada() {
		List<Jogador> titulares = new ArrayList<>(titularesPara(Formacao.F_4_4_2, 20));
		Escalacao escalacao = new Escalacao(Formacao.F_4_4_2, titulares);
		titulares.clear();

		assertThat(escalacao.titulares()).hasSize(11);
	}
}
