package br.com.managerfoot.motor;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

class ParametrosSorteTest {

	@Test
	void aceitaValoresNosLimites() {
		assertThatNoException().isThrownBy(() -> new ParametrosSorte(0, 1));
		assertThatNoException().isThrownBy(() -> new ParametrosSorte(1, 0.01));
	}

	@Test
	void recusaValoresForaDosLimites() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosSorte(-0.1, 0.8));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosSorte(1.1, 0.8));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosSorte(0.1, 0));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosSorte(0.1, 1.2));
	}
}
