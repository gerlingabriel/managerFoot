package br.com.managerfoot.economia;

/**
 * Valores da premiação da liga e do patrocínio (SPEC §8.1 e §9.6, rev. 45 e 46).
 *
 * @param jogadoresNoPremio o prêmio do campeão compra este número de jogadores da força de referência
 *        com asterisco (definido pelo usuário: 2, de força 50)
 * @param segundoLugar prêmio do 2º sobre o do campeão (usuário: 30% menor, 70%)
 * @param terceiroLugar prêmio do 3º sobre o do campeão (usuário: 10%)
 * @param patrocinioSobrePremio patrocínio do clube melhor colocado sobre o prêmio do campeão (usuário:
 *        cerca de 10%)
 * @param patrocinioSegundo quanto o patrocínio do 2º é menor que o do 1º (usuário: 10%)
 * @param patrocinioPorPosicao quanto cada posição seguinte é menor que a anterior (usuário: 8%)
 */
public record ParametrosPremiacao(int jogadoresNoPremio, double segundoLugar, double terceiroLugar,
		double patrocinioSobrePremio, double patrocinioSegundo, double patrocinioPorPosicao) {

	public ParametrosPremiacao {
		if (jogadoresNoPremio < 0 || segundoLugar < 0 || terceiroLugar < 0 || patrocinioSobrePremio < 0
				|| patrocinioSegundo < 0 || patrocinioSegundo >= 1 || patrocinioPorPosicao < 0
				|| patrocinioPorPosicao >= 1) {
			throw new IllegalArgumentException("parâmetros de premiação inválidos");
		}
	}

	public static ParametrosPremiacao padrao() {
		return new ParametrosPremiacao(2, 0.70, 0.10, 0.10, 0.10, 0.08);
	}
}
