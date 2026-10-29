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

	/** Moral depois de um jogo contra um adversário de mesma força, limitada à escala. */
	public Moral aposJogo(boolean emCasa, int golsPro, int golsContra, ParametrosMoral parametros) {
		return aposJogo(emCasa, golsPro, golsContra, 0, 0, parametros);
	}

	/**
	 * Moral depois de um jogo, limitada à escala. A surpresa vale mais
	 * ({@link ParametrosMoral#diferencaPorPonto()}): o favorito que empata ou perde perde mais, e o azarão
	 * que empata ou vence ganha mais; o resultado esperado (o favorito vence, o azarão perde) vale o
	 * normal.
	 *
	 * @param forcaPropria força em campo do clube (média dos 11 mais fortes)
	 * @param forcaAdversario força em campo do adversário
	 */
	public Moral aposJogo(boolean emCasa, int golsPro, int golsContra, double forcaPropria, double forcaAdversario,
			ParametrosMoral parametros) {
		// positivo: o adversário é mais forte (azarão); negativo: mais fraco (favorito)
		int ajuste = parametros.diferencaPorPonto() == 0 ? 0
				: (int) Math.clamp(Math.round((forcaAdversario - forcaPropria) / parametros.diferencaPorPonto()),
						-parametros.ajusteMaximo(), parametros.ajusteMaximo());
		int variacao;
		if (golsPro > golsContra) {
			variacao = (emCasa ? parametros.vitoriaEmCasa() : parametros.vitoriaFora()) + Math.max(ajuste, 0);
		} else if (golsPro == golsContra) {
			variacao = (emCasa ? parametros.empateEmCasa() : parametros.empateFora()) + ajuste;
		} else {
			variacao = (emCasa ? parametros.derrotaEmCasa() : parametros.derrotaFora()) + Math.min(ajuste, 0);
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
