package br.com.managerfoot.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class EscudoTest {

	@Test
	void normalizaAsCoresParaMaiusculas() {
		Escudo escudo = new Escudo(ModeloEscudo.LISTRAS_VERTICAIS, " #1d4ed8", "#facc15");
		assertThat(escudo.corPrimaria()).isEqualTo("#1D4ED8");
		assertThat(escudo.corSecundaria()).isEqualTo("#FACC15");
	}

	@Test
	void recusaCorForaDoFormatoOuCoresIguais() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Escudo(ModeloEscudo.CRUZ, "azul", "#FFFFFF"))
			.withMessageContaining("cor primária").withMessageContaining("#RRGGBB");
		assertThatIllegalArgumentException().isThrownBy(() -> new Escudo(ModeloEscudo.CRUZ, "#FFFFFF", "#FFF"))
			.withMessageContaining("cor secundária");
		assertThatIllegalArgumentException().isThrownBy(() -> new Escudo(ModeloEscudo.CRUZ, "#FFFFFF", null));
		assertThatIllegalArgumentException().isThrownBy(() -> new Escudo(ModeloEscudo.CRUZ, "#ffffff", "#FFFFFF"))
			.withMessageContaining("diferentes");
		assertThatNullPointerException().isThrownBy(() -> new Escudo(null, "#FFFFFF", "#000000"));
	}

	@Test
	void temDezModelos() {
		assertThat(ModeloEscudo.values()).hasSize(10);
	}
}
