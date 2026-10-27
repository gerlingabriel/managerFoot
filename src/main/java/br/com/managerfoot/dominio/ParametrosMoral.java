package br.com.managerfoot.dominio;

/**
 * Valores da moral (SPEC §6.2). Provisórios: devem ser validados por simulação de temporadas.
 *
 * @param inicial moral ao assumir um time
 * @param limiteVerde a partir deste valor a faixa é verde
 * @param limiteAmarelo a partir deste valor (e abaixo do verde) a faixa é amarela; abaixo, vermelha
 */
public record ParametrosMoral(int minimo, int maximo, int inicial, int vitoriaEmCasa, int vitoriaFora,
		int empateEmCasa, int empateFora, int derrotaEmCasa, int derrotaFora, int limiteVerde, int limiteAmarelo) {

	public ParametrosMoral {
		if (minimo >= maximo || inicial < minimo || inicial > maximo) {
			throw new IllegalArgumentException("a moral inicial deve estar entre o mínimo e o máximo");
		}
		if (!(minimo < limiteAmarelo && limiteAmarelo < limiteVerde && limiteVerde <= maximo)) {
			throw new IllegalArgumentException("as faixas devem respeitar mínimo < amarelo < verde <= máximo");
		}
	}

	/** Escala 0-30, início 18, faixas verde 15-30 / amarelo 6-14 / vermelho 0-5. */
	public static ParametrosMoral padrao() {
		return new ParametrosMoral(0, 30, 18, 2, 3, -1, 0, -3, -2, 15, 6);
	}
}
