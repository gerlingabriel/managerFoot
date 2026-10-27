package br.com.managerfoot.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class ClubeTest {

	@Test
	void aceitaForcaNaEscala() {
		assertThat(new Clube(1, "Azul", 1).forca()).isEqualTo(1);
		assertThat(new Clube(1, "Azul", 50).forca()).isEqualTo(50);
	}

	@Test
	void recusaForcaForaDaEscalaENomeVazio() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Clube(1, "Azul", 0)).withMessageContaining("entre 1 e 50");
		assertThatIllegalArgumentException().isThrownBy(() -> new Clube(1, "Azul", 51));
		assertThatIllegalArgumentException().isThrownBy(() -> new Clube(1, " ", 20)).withMessageContaining("nome");
	}
}
