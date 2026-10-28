package br.com.managerfoot.temporada;

/**
 * Velocidade da divisão com usuário na rodada ao vivo (SPEC rev. 35; docs/decisions/0010).
 * Valores provisórios, em milissegundos por minuto de jogo.
 */
public enum Velocidade {
	LENTA(1000),
	NORMAL(500),
	RAPIDA(200);

	private final int msPorMinuto;

	Velocidade(int msPorMinuto) {
		this.msPorMinuto = msPorMinuto;
	}

	public int msPorMinuto() {
		return msPorMinuto;
	}
}
