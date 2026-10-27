package br.com.managerfoot.dominio;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Escudo simples do clube: um modelo e duas cores (SPEC §10.2). Cores no formato {@code #RRGGBB},
 * guardadas em maiúsculas.
 */
public record Escudo(ModeloEscudo modelo, String corPrimaria, String corSecundaria) {

	private static final Pattern COR = Pattern.compile("#[0-9A-F]{6}");

	/** Usado enquanto o clube não tem escudo cadastrado (provisório: liso branco com borda preta). */
	public static final Escudo NEUTRO = new Escudo(ModeloEscudo.LISO_COM_BORDA, "#FFFFFF", "#000000");

	public Escudo {
		Objects.requireNonNull(modelo, "modelo do escudo é obrigatório");
		corPrimaria = cor("cor primária", corPrimaria);
		corSecundaria = cor("cor secundária", corSecundaria);
		if (corPrimaria.equals(corSecundaria)) {
			throw new IllegalArgumentException("as duas cores do escudo devem ser diferentes, mas ambas são " + corPrimaria);
		}
	}

	private static String cor(String nome, String valor) {
		String normalizada = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
		if (!COR.matcher(normalizada).matches()) {
			throw new IllegalArgumentException(nome + " do escudo deve estar no formato #RRGGBB, mas foi " + valor);
		}
		return normalizada;
	}
}
