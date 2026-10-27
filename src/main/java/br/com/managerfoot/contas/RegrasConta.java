package br.com.managerfoot.contas;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Regras de cadastro (SPEC §3.4, rev. 24). Devolve todos os problemas de uma vez, para a tela. */
public final class RegrasConta {

	public static final int SENHA_MINIMA = 8;
	/** Limite do BCrypt: bytes além do 72º seriam ignorados. */
	public static final int SENHA_MAXIMA_BYTES = 72;
	public static final int NOME_MAXIMO = 60;
	public static final int EMAIL_MAXIMO = 254;

	private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

	private RegrasConta() {
	}

	/** E-mail sem espaços nas pontas e em minúsculas: é assim que ele é gravado e procurado. */
	public static String normalizarEmail(String email) {
		return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
	}

	public static List<String> problemas(String email, String nome, String senha) {
		List<String> problemas = new ArrayList<>();
		String normalizado = normalizarEmail(email);
		if (!EMAIL.matcher(normalizado).matches() || normalizado.length() > EMAIL_MAXIMO) {
			problemas.add("Informe um e-mail válido.");
		}
		if (nome == null || nome.isBlank()) {
			problemas.add("Informe o nome.");
		} else if (nome.strip().length() > NOME_MAXIMO) {
			problemas.add("O nome pode ter até " + NOME_MAXIMO + " caracteres.");
		}
		problemas.addAll(problemasSenha(senha));
		return problemas;
	}

	public static List<String> problemasSenha(String senha) {
		if (senha == null || senha.length() < SENHA_MINIMA) {
			return List.of("A senha precisa ter pelo menos " + SENHA_MINIMA + " caracteres.");
		}
		if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAXIMA_BYTES) {
			return List.of("A senha pode ter até " + SENHA_MAXIMA_BYTES + " bytes (cerca de " + SENHA_MAXIMA_BYTES
					+ " letras sem acento).");
		}
		return List.of();
	}

	/** @throws IllegalArgumentException com todos os problemas */
	public static void exigirValidos(String email, String nome, String senha) {
		exigir(problemas(email, nome, senha));
	}

	/** @throws IllegalArgumentException com o problema da senha */
	public static void exigirSenhaValida(String senha) {
		exigir(problemasSenha(senha));
	}

	private static void exigir(List<String> problemas) {
		if (!problemas.isEmpty()) {
			throw new IllegalArgumentException(String.join(" ", problemas));
		}
	}
}
