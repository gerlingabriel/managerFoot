package br.com.managerfoot.contas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegrasContaTest {

	@Test
	void emailEhNormalizadoEmMinusculasSemEspacos() {
		assertThat(RegrasConta.normalizarEmail("  Ana.Silva@Exemplo.COM ")).isEqualTo("ana.silva@exemplo.com");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "ana", "ana@", "ana@exemplo", "ana silva@exemplo.com", "@exemplo.com" })
	void recusaEmailInvalido(String email) {
		assertThat(RegrasConta.problemas(email, "Ana", "12345678")).containsExactly("Informe um e-mail válido.");
	}

	@Test
	void senhaDe8A72Bytes() {
		assertThat(RegrasConta.problemasSenha("1234567")).hasSize(1);
		assertThat(RegrasConta.problemasSenha("12345678")).isEmpty();
		assertThat(RegrasConta.problemasSenha("a".repeat(72))).isEmpty();
		assertThat(RegrasConta.problemasSenha("a".repeat(73))).hasSize(1);
		// "ç" ocupa 2 bytes: 37 deles passam de 72 bytes
		assertThat(RegrasConta.problemasSenha("ç".repeat(37))).hasSize(1);
		assertThat(RegrasConta.problemasSenha(null)).hasSize(1);
	}

	@Test
	void juntaTodosOsProblemas() {
		assertThat(RegrasConta.problemas("x", " ", "123")).hasSize(3);
		assertThat(RegrasConta.problemas("ana@exemplo.com", "A".repeat(61), "12345678"))
			.containsExactly("O nome pode ter até 60 caracteres.");
		assertThatIllegalArgumentException().isThrownBy(() -> RegrasConta.exigirValidos("x", "Ana", "12345678"))
			.withMessageContaining("e-mail");
	}

	@Test
	void problemasPorCampoParaATela() {
		assertThat(RegrasConta.problemasPorCampo("x", " ", "123")).containsOnlyKeys("email", "nome", "senha");
		assertThat(RegrasConta.problemasPorCampo("ana@exemplo.com", "Ana", "1234"))
			.containsExactly(entry("senha", "A senha precisa ter pelo menos 8 caracteres."));
		assertThat(RegrasConta.problemasPorCampo("ana@exemplo.com", "Ana", "12345678")).isEmpty();
	}
}
