package br.com.managerfoot.motor;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

class ParametrosAsteriscoTest {

	@Test
	void aceitaValoresNosLimites() {
		assertThatNoException().isThrownBy(() -> new ParametrosAsterisco(0, 1, 0, 0, 0));
		assertThatNoException().isThrownBy(ParametrosAsterisco::padrao);
	}

	@Test
	void recusaValoresForaDosLimites() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosAsterisco(-0.1, 1.5, 0.65, 3, 0.01));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosAsterisco(0.15, 0.9, 0.65, 3, 0.01));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosAsterisco(0.15, 1.5, 1.1, 3, 0.01));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosAsterisco(0.15, 1.5, 0.65, -1, 0.01));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosAsterisco(0.15, 1.5, 0.65, 3, 1));
	}
}
