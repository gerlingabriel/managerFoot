package br.com.managerfoot.dominio;

/**
 * Moral numérica do time (SPEC §6.2), sempre entre o mínimo e o máximo configurados.
 * Não influencia a partida (rev. 59); serve à demissão (§5.4).
 */
public record Moral(int valor) {

	public enum Faixa {
		VERDE,
		AMARELO,
		VERMELHO
	}

	public static Moral inicial(ParametrosMoral parametros) {
		return new Moral(parametros.inicial());
	}

	/** Moral depois de um jogo, limitada à escala. */
	public Moral aposJogo(boolean emCasa, int golsPro, int golsContra, ParametrosMoral parametros) {
		int variacao;
		if (golsPro > golsContra) {
			variacao = emCasa ? parametros.vitoriaEmCasa() : parametros.vitoriaFora();
		} else if (golsPro == golsContra) {
			variacao = emCasa ? parametros.empateEmCasa() : parametros.empateFora();
		} else {
			variacao = emCasa ? parametros.derrotaEmCasa() : parametros.derrotaFora();
		}
		int retorno = parametros.divisorRetorno() == 0 ? 0 : (parametros.inicial() - valor) / parametros.divisorRetorno();
		return new Moral(Math.clamp(valor + variacao + retorno, parametros.minimo(), parametros.maximo()));
	}

	public Faixa faixa(ParametrosMoral parametros) {
		if (valor >= parametros.limiteVerde()) {
			return Faixa.VERDE;
		}
		return valor >= parametros.limiteAmarelo() ? Faixa.AMARELO : Faixa.VERMELHO;
	}

	/**
	 * Demissão pela moral (SPEC §5.4, rev. 60): ao entrar no vermelho, o jogo seguinte é decisivo; se
	 * depois dele a moral continua vermelha (derrota, ou empate ou vitória que não tiram do vermelho), o
	 * treinador é demitido.
	 *
	 * @param depois a moral depois do jogo decisivo; esta é a de antes dele
	 */
	public boolean demite(Moral depois, ParametrosMoral parametros) {
		return faixa(parametros) == Faixa.VERMELHO && depois.faixa(parametros) == Faixa.VERMELHO;
	}
}
