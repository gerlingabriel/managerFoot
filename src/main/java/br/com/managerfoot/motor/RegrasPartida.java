package br.com.managerfoot.motor;

/**
 * Regras que variam por competição (SPEC §7.4.1). Diferente de {@link ParametrosMotor}, não são
 * balanceamento: vêm da configuração do campeonato.
 *
 * @param substituicoes quantas substituições cada time pode fazer
 */
public record RegrasPartida(int substituicoes) {

	public static final int SUBSTITUICOES_PADRAO = 5;

	public RegrasPartida {
		if (substituicoes < 0) {
			throw new IllegalArgumentException("substituições não podem ser negativas, mas foram " + substituicoes);
		}
	}

	public static RegrasPartida padrao() {
		return new RegrasPartida(SUBSTITUICOES_PADRAO);
	}
}
