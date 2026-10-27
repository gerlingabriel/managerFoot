package br.com.managerfoot.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FormacaoTest {

	@ParameterizedTest
	@EnumSource(Formacao.class)
	void todaFormacaoTemUmGoleiroEOnzeTitulares(Formacao formacao) {
		int total = Arrays.stream(Posicao.values()).mapToInt(formacao::quantidade).sum();
		assertThat(formacao.quantidade(Posicao.GOLEIRO)).isEqualTo(1);
		assertThat(total).isEqualTo(Formacao.TITULARES);
	}

	@Test
	void classificacaoDeEstiloSegueASpec() {
		assertThat(Arrays.stream(Formacao.values()).filter(f -> f.estilo() == EstiloTatico.NORMAL).map(Formacao::rotulo))
			.containsExactlyInAnyOrder("4-4-2", "3-5-2");
		assertThat(Arrays.stream(Formacao.values()).filter(f -> f.estilo() == EstiloTatico.OFENSIVO).map(Formacao::rotulo))
			.containsExactlyInAnyOrder("4-3-3", "3-4-3", "4-2-4");
		assertThat(Arrays.stream(Formacao.values()).filter(f -> f.estilo() == EstiloTatico.DEFENSIVO).map(Formacao::rotulo))
			.containsExactlyInAnyOrder("5-4-1", "4-5-1", "3-6-1");
	}

	@Test
	void buscaPorRotulo() {
		assertThat(Formacao.porRotulo("4-3-3")).contains(Formacao.F_4_3_3);
		assertThat(Formacao.porRotulo("5-0-5")).isEmpty();
	}
}
