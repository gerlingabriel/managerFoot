package br.com.managerfoot.contas;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Hash de senhas com BCrypt (docs/decisions/0007). A senha em texto nunca sai daqui. */
public final class Senhas {

	private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
	/** Comparado quando o e-mail não existe, para o tempo de resposta não revelar quem tem conta. */
	private static final String HASH_FICTICIO = BCRYPT.encode("senha-ficticia-para-tempo-constante");

	private Senhas() {
	}

	/** @throws IllegalArgumentException se a senha não atende às regras */
	public static String hash(String senha) {
		RegrasConta.exigirSenhaValida(senha);
		return BCRYPT.encode(senha);
	}

	/** @param hash nulo quando a conta não existe; a comparação é feita mesmo assim */
	public static boolean confere(String senha, String hash) {
		if (senha == null) {
			return false;
		}
		boolean confere = BCRYPT.matches(senha, hash == null ? HASH_FICTICIO : hash);
		return hash != null && confere;
	}
}
