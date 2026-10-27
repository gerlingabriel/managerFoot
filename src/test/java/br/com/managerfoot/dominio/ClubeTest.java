package br.com.managerfoot.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

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

	@Test
	void guardaOEscudoEUsaONeutroQuandoNaoInformado() {
		Escudo escudo = new Escudo(ModeloEscudo.QUARTOS, "#B91C1C", "#111827");
		assertThat(new Clube(1, "Azul", 30, escudo).escudo()).isEqualTo(escudo);
		assertThat(new Clube(1, "Azul", 30).escudo()).isEqualTo(Escudo.NEUTRO);
		assertThatNullPointerException().isThrownBy(() -> new Clube(1, "Azul", 30, null));
	}
}
