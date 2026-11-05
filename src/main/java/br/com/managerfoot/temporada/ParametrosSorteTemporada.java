package br.com.managerfoot.temporada;

/**
 * Quem tem a sorte da temporada (SPEC §5.3.1, rev. 108).
 *
 * @param sorteados quantos clubes da máquina cada divisão sorteia como beneficiados (usuário: dois)
 * @param forcaMediaLimiteDoUsuario o clube de usuário com força média do plantel inteiro abaixo disto no início da
 *        temporada tem o benefício a temporada toda (usuário: 40)
 */
public record ParametrosSorteTemporada(int sorteados, double forcaMediaLimiteDoUsuario) {

	public ParametrosSorteTemporada {
		if (sorteados < 0) {
			throw new IllegalArgumentException("sorteados não pode ser negativo: " + sorteados);
		}
	}

	public static ParametrosSorteTemporada padrao() {
		return new ParametrosSorteTemporada(2, 40);
	}
}
