package br.com.managerfoot.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.temporada.FormatoDivisao;
import br.com.managerfoot.temporada.FormatoLiga;

class JogosControllerTest {

	@Test
	void detalheDoCampeonato() {
		assertThat(JogosController.detalhe(new FormatoLiga("L", List.of(new FormatoDivisao("A", 1, 20, 50, 4),
				new FormatoDivisao("B", 2, 20, 30, 4), new FormatoDivisao("C", 3, 20, 22, 4)))))
			.isEqualTo("3 divisões · 20, 20 e 20 clubes · 4 sobem e 4 descem");
		assertThat(JogosController.detalhe(new FormatoLiga("L", List.of(new FormatoDivisao("A", 1, 18, 50, 2),
				new FormatoDivisao("B", 2, 16, 30, 3), new FormatoDivisao("C", 3, 20, 22, 4)))))
			.isEqualTo("3 divisões · 18, 16 e 20 clubes");
		assertThat(JogosController.detalhe(new FormatoLiga("L", List.of(new FormatoDivisao("A", 1, 20, 50, 2)))))
			.isEqualTo("1 divisão · 20 clubes");
	}
}
