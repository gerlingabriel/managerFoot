package br.com.managerfoot.economia;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Moedas do jogo (SPEC §9.1.1): valores guardados em centavos de Real e convertidos só na
 * exibição, pela proporção fixa. A moeda exibida segue o país da liga (rev. 37).
 */
public enum Moeda {
	REAL("R$", 1),
	DOLAR("US$", 5),
	EURO("€", 6);

	private static final Map<String, Moeda> POR_PAIS = Map.of("BRA", REAL, "ARG", DOLAR, "ENG", EURO, "ESP", EURO,
			"ITA", EURO, "POR", EURO);

	private final String simbolo;
	private final int reaisPorUnidade;

	Moeda(String simbolo, int reaisPorUnidade) {
		this.simbolo = simbolo;
		this.reaisPorUnidade = reaisPorUnidade;
	}

	/** A moeda da liga do país; países fora do banco padrão usam o Real até haver regra. */
	public static Moeda doPais(String codigoPais) {
		return POR_PAIS.getOrDefault(codigoPais, REAL);
	}

	public String simbolo() {
		return simbolo;
	}

	/** O valor nesta moeda, sem centavos: "R$ 1.234.567", "€ 205.761". */
	public String formatar(long centavosDeReal) {
		long unidades = Math.round(centavosDeReal / 100.0 / reaisPorUnidade);
		return simbolo + " " + NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(unidades);
	}

	/**
	 * Centavos de Real de um valor inteiro nesta moeda, como o treinador digita um lance.
	 *
	 * @throws ArithmeticException se o valor é grande demais
	 */
	public long centavos(long unidades) {
		return Math.multiplyExact(unidades, reaisPorUnidade * 100L);
	}

	/** O menor valor inteiro nesta moeda que cobre os centavos: o lance mínimo que a tela aceita. */
	public long unidadesAcima(long centavosDeReal) {
		long porUnidade = reaisPorUnidade * 100L;
		return Math.floorDiv(centavosDeReal + porUnidade - 1, porUnidade);
	}

	/** Resultado com sinal à frente, para extratos: "+ R$ 129.204", "− R$ 227.634"; zero sem sinal. */
	public String formatarComSinal(long centavosDeReal) {
		String valor = formatar(Math.abs(centavosDeReal));
		if (valor.endsWith(" 0")) {
			return valor;
		}
		return (centavosDeReal < 0 ? "− " : "+ ") + valor;
	}
}
