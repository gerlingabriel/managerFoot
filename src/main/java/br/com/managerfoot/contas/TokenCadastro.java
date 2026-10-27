package br.com.managerfoot.contas;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token do link de cadastro (docs/decisions/0007): 32 bytes aleatórios em Base64 para URL. O banco
 * guarda só o {@link #hash(String) hash}, e o token aparece uma única vez, ao ser gerado.
 */
public final class TokenCadastro {

	private static final int BYTES = 32;
	private static final SecureRandom ALEATORIO = new SecureRandom();

	private TokenCadastro() {
	}

	public static String gerar() {
		byte[] bytes = new byte[BYTES];
		ALEATORIO.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** SHA-256 em hexadecimal minúsculo. */
	public static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 indisponível na JVM", e);
		}
	}
}
