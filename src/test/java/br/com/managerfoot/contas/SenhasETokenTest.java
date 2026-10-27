package br.com.managerfoot.contas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SenhasETokenTest {

	@Test
	void hashConfereSoComASenhaCerta() {
		String hash = Senhas.hash("segredo123");

		assertThat(hash).doesNotContain("segredo123");
		assertThat(Senhas.confere("segredo123", hash)).isTrue();
		assertThat(Senhas.confere("segredo124", hash)).isFalse();
		assertThat(Senhas.confere(null, hash)).isFalse();
		assertThat(Senhas.confere("segredo123", null)).isFalse();
	}

	@Test
	void hashRecusaSenhaForaDaRegra() {
		assertThatIllegalArgumentException().isThrownBy(() -> Senhas.hash("curta"));
	}

	@Test
	void tokensSaoUnicosEOHashEhSha256Hex() {
		Set<String> tokens = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			tokens.add(TokenCadastro.gerar());
		}
		String token = tokens.iterator().next();

		assertThat(tokens).hasSize(1000);
		assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
		assertThat(TokenCadastro.hash(token)).matches("[0-9a-f]{64}").isEqualTo(TokenCadastro.hash(token));
		assertThat(TokenCadastro.hash("abc"))
			.isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
	}
}
