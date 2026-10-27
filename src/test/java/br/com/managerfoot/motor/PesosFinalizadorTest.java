package br.com.managerfoot.motor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Posicao;

class PesosFinalizadorTest {

	@ParameterizedTest
	@EnumSource(Formacao.class)
	void parametrosPadraoTemPesosParaTodaFormacao(Formacao formacao) {
		assertThat(ParametrosMotor.padrao().pesosFinalizador()).containsKey(formacao);
	}

	@Test
	void pesosSeguemATabelaDaSpec() {
		PesosFinalizador p442 = ParametrosMotor.padrao().pesosFinalizador().get(Formacao.F_4_4_2);
		assertThat(p442.peso(Posicao.ATACANTE)).isEqualTo(64);
		assertThat(p442.peso(Posicao.MEIO_CAMPISTA)).isEqualTo(31);
		assertThat(p442.peso(Posicao.DEFENSOR)).isEqualTo(4.99);
		assertThat(p442.peso(Posicao.GOLEIRO)).isEqualTo(0.01);
	}

	@Test
	void recusaPesosQueNaoSomam100() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PesosFinalizador(75, 21, 3.95, 0.01))
			.withMessageContaining("somar 100");
	}

	@Test
	void recusaPesoNegativo() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PesosFinalizador(110, -10, 0, 0));
	}
}
