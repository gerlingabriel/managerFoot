package br.com.managerfoot.dominio;

import java.util.Set;

/**
 * Quem conta como estrangeiro numa liga (SPEC §8.4, rev. 87). O jogador de outro país pode ser
 * equiparado a nacional: português no Brasil; cidadão da União Europeia nas ligas europeias. Equiparado
 * não tem o desconto de estrangeiro no preço e não conta entre os estrangeiros em campo.
 */
public final class RegraEstrangeiros {

	/** Os 27 países da União Europeia, em códigos FIFA (o Reino Unido saiu em 31/01/2020). */
	public static final Set<String> UNIAO_EUROPEIA = Set.of("AUT", "BEL", "BUL", "CRO", "CYP", "CZE", "DEN", "EST", "FIN",
			"FRA", "GER", "GRE", "HUN", "IRL", "ITA", "LTU", "LUX", "LVA", "MLT", "NED", "POL", "POR", "ROU", "SVK", "SVN",
			"ESP", "SWE");

	/** Liga europeia de fora da União que também equipara os cidadãos dela (rev. 87, provisório). */
	private static final String INGLATERRA = "ENG";

	private RegraEstrangeiros() {
	}

	/** O jogador é de outro país, seja qual for a regra da liga: é o que a sigla no plantel mostra. */
	public static boolean deOutroPais(String nacionalidade, String paisDaLiga) {
		return !nacionalidade.equals(paisDaLiga);
	}

	/** De outro país, mas não conta como estrangeiro nesta liga. */
	public static boolean equiparado(String nacionalidade, String paisDaLiga) {
		if (!deOutroPais(nacionalidade, paisDaLiga)) {
			return false;
		}
		if ("BRA".equals(paisDaLiga)) {
			return "POR".equals(nacionalidade);
		}
		boolean ligaEuropeia = UNIAO_EUROPEIA.contains(paisDaLiga) || INGLATERRA.equals(paisDaLiga);
		return ligaEuropeia && UNIAO_EUROPEIA.contains(nacionalidade);
	}

	/** Conta no limite de estrangeiros e sai com o desconto de estrangeiro no preço. */
	public static boolean contaComoEstrangeiro(String nacionalidade, String paisDaLiga) {
		return deOutroPais(nacionalidade, paisDaLiga) && !equiparado(nacionalidade, paisDaLiga);
	}
}
