package br.com.managerfoot.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JogadorTest {

	@ParameterizedTest
	@ValueSource(ints = { 1, 25, 50 })
	void aceitaForcaDentroDaEscala(int forca) {
		assertThat(new Jogador(1, "Ana", Posicao.ATACANTE, forca, Temperamento.CALMO, "BRA").forca()).isEqualTo(forca);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, -1, 51 })
	void recusaForcaForaDaEscala(int forca) {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new Jogador(1, "Ana", Posicao.ATACANTE, forca, Temperamento.CALMO, "BRA"))
			.withMessageContaining("entre 1 e 50")
			.withMessageContaining(String.valueOf(forca));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   " })
	void recusaNomeVazio(String nome) {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new Jogador(1, nome, Posicao.ATACANTE, 10, Temperamento.CALMO, "BRA"))
			.withMessageContaining("nome");
	}

	@Test
	void exigePosicaoETemperamento() {
		assertThatNullPointerException().isThrownBy(() -> new Jogador(1, "Ana", null, 10, Temperamento.CALMO, "BRA"));
		assertThatNullPointerException().isThrownBy(() -> new Jogador(1, "Ana", Posicao.GOLEIRO, 10, null, "BRA"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "BR", "BRAS", "bra", "B1A" })
	void recusaNacionalidadeForaDoCodigoDePais(String nacionalidade) {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new Jogador(1, "Ana", Posicao.ATACANTE, 10, Temperamento.CALMO, nacionalidade))
			.withMessageContaining("nacionalidade de Ana");
	}

	@Test
	void exigeNacionalidade() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new Jogador(1, "Ana", Posicao.ATACANTE, 10, Temperamento.CALMO, null));
	}

	@Test
	void comAsteriscoMantemOsDemaisDados() {
		Jogador ana = new Jogador(7, "Ana", Posicao.ATACANTE, 30, Temperamento.ANSIOSO, "POR");

		assertThat(ana.comAsterisco(true))
			.isEqualTo(new Jogador(7, "Ana", Posicao.ATACANTE, 30, Temperamento.ANSIOSO, true, "POR"));
		assertThat(ana.comAsterisco(true).comAsterisco(false)).isEqualTo(ana);
	}
}
