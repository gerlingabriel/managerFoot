package br.com.managerfoot.motor;

/**
 * Zebras da copa nacional (SPEC §8.2, rev. 117). Com a força em campo (média dos 11 titulares no início
 * da partida) do mais fraco pelo menos {@code diferencaMinima} abaixo da do outro, cada jogador do mais
 * fraco joga com {@code fracaoBonus} do que a diferença passa do limite a mais: com 80%, a diferença de 20
 * vale como 16 e a de 30 como 18, sem salto no limite. Nas primeiras fases (16 avos e oitavas), o mais
 * fraco ainda tem {@code chanceSuperBonus} de jogar a partida inteira como se todos tivessem
 * {@code forcaSuperBonus}.
 *
 * @param diferencaMinima diferença de força em campo a partir da qual o bônus vale
 * @param fracaoBonus parte do que a diferença passa do limite, somada à força de cada jogador do mais fraco (0 a 1)
 * @param chanceSuperBonus chance, por partida das primeiras fases, do super bônus
 * @param forcaSuperBonus força de todos os jogadores do mais fraco no super bônus; também o teto do bônus
 */
public record ParametrosZebraCopa(double diferencaMinima, double fracaoBonus, double chanceSuperBonus,
		double forcaSuperBonus) {

	public ParametrosZebraCopa {
		if (diferencaMinima < 0) {
			throw new IllegalArgumentException("diferencaMinima não pode ser negativa");
		}
		if (fracaoBonus < 0 || fracaoBonus >= 1) {
			throw new IllegalArgumentException("fracaoBonus deve ficar entre 0 e 1 (exclusive): o mais fraco não fica igual");
		}
		if (chanceSuperBonus < 0 || chanceSuperBonus > 1) {
			throw new IllegalArgumentException("chanceSuperBonus deve estar entre 0 e 1");
		}
		if (forcaSuperBonus <= 0) {
			throw new IllegalArgumentException("forcaSuperBonus deve ser positiva");
		}
	}

	/**
	 * Pelo usuário (rev. 117): a partir de 15 pontos, 80% do que passa deles (opção "sem salto", escolhida pela
	 * simulação), super bônus em 10% das partidas, como força 50.
	 */
	public static ParametrosZebraCopa padrao() {
		return new ParametrosZebraCopa(15, 0.8, 0.10, 50);
	}

	/** Sem bônus nem super bônus (calibração e testes). */
	public static final ParametrosZebraCopa NENHUMA = new ParametrosZebraCopa(Double.MAX_VALUE, 0, 0, 50);
}
