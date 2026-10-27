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
		assertThat(new Jogador(1, "Ana", Posicao.ATACANTE, forca, Temperamento.CALMO).forca()).isEqualTo(forca);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, -1, 51 })
	void recusaForcaForaDaEscala(int forca) {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new Jogador(1, "Ana", Posicao.ATACANTE, forca, Temperamento.CALMO))
			.withMessageContaining("entre 1 e 50")
			.withMessageContaining(String.valueOf(forca));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   " })
	void recusaNomeVazio(String nome) {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new Jogador(1, nome, Posicao.ATACANTE, 10, Temperamento.CALMO))
			.withMessageContaining("nome");
	}

	@Test
	void exigePosicaoETemperamento() {
		assertThatNullPointerException().isThrownBy(() -> new Jogador(1, "Ana", null, 10, Temperamento.CALMO));
		assertThatNullPointerException().isThrownBy(() -> new Jogador(1, "Ana", Posicao.GOLEIRO, 10, null));
	}
}
