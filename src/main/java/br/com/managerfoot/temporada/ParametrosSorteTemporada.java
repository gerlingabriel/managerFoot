package br.com.managerfoot.temporada;

/**
 * Quem tem a sorte e o azar da temporada (SPEC §5.3.1, rev. 108 e 109).
 *
 * @param beneficiados quantos clubes cada divisão sorteia como beneficiados, entre todos, inclusive os de usuário
 *        (usuário: dois)
 * @param prejudicados quantos clubes da máquina cada divisão sorteia como prejudicados; clube de usuário nunca
 *        (usuário, rev. 109: o prejudicado volta, como era, um por divisão)
 * @param forcaMediaLimiteDoUsuario o clube de usuário com força média do plantel inteiro abaixo disto no início da
 *        temporada tem o benefício a temporada toda (usuário: 40)
 */
public record ParametrosSorteTemporada(int beneficiados, int prejudicados, double forcaMediaLimiteDoUsuario) {

	public ParametrosSorteTemporada {
		if (beneficiados < 0 || prejudicados < 0) {
			throw new IllegalArgumentException("beneficiados e prejudicados não podem ser negativos");
		}
	}

	public static ParametrosSorteTemporada padrao() {
		return new ParametrosSorteTemporada(2, 1, 40);
	}
}
