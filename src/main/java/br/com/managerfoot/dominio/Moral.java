package br.com.managerfoot.dominio;

/**
 * Moral numérica do time (SPEC §6.2), sempre entre o mínimo e o máximo configurados.
 * Os efeitos (demissão, público) entram nas etapas seguintes.
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
}
