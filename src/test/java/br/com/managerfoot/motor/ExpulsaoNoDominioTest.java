package br.com.managerfoot.motor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** Expulsão dá ao adversário 50% a mais da chance de ficar com a jogada, até 99/1 (SPEC §7.4, rev. 98). */
class ExpulsaoNoDominioTest {

	private static final ParametrosDisciplina DISCIPLINA = ParametrosDisciplina.padrao();

	@Test
	void exemplosDoUsuario() {
		// visitante expulso: o mandante fica com 50% a mais
		assertThat(Partida.comExpulsoes(0.5, -1, DISCIPLINA)).isCloseTo(0.75, within(1e-9));
		assertThat(Partida.comExpulsoes(0.6, -1, DISCIPLINA)).isCloseTo(0.90, within(1e-9));
		assertThat(Partida.comExpulsoes(0.7, -1, DISCIPLINA)).as("passa de 100%: fica 99/1").isCloseTo(0.99, within(1e-9));
		// mandante expulso: a chance do visitante cresce
		assertThat(Partida.comExpulsoes(0.6, 1, DISCIPLINA)).as("40% do visitante vira 60%").isCloseTo(0.40, within(1e-9));
	}

	@Test
	void semDiferencaDeExpulsosNadaMudaECadaExpulsoAMaisSoma() {
		assertThat(Partida.comExpulsoes(0.55, 0, DISCIPLINA)).isEqualTo(0.55);
		assertThat(Partida.comExpulsoes(0.4, -2, DISCIPLINA)).as("0,4 × 1,5 × 1,5").isCloseTo(0.90, within(1e-9));
		assertThat(Partida.comExpulsoes(0.995, -1, DISCIPLINA)).as("o teto não tira de quem já passava dele").isEqualTo(0.995);
	}
}
