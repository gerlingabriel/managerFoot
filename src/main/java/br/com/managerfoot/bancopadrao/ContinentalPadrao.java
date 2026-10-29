package br.com.managerfoot.bancopadrao;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Competição continental do banco padrão com as vagas por país (SPEC §8.3.6, rev. 72 e 73). As
 * vagas somam {@value #CLUBES} e vão direto aos grupos; o nome é fictício e o editor pode trocá-lo.
 *
 * @param vagas vagas por código FIFA, na ordem da tabela da SPEC
 */
public record ContinentalPadrao(Regiao regiao, String nome, Map<String, Integer> vagas) {

	/** Clubes na fase de grupos (rev. 72). */
	public static final int CLUBES = 32;
	/** O país gerado tem este múltiplo das suas vagas, para a escolha por força com sorteio variar (rev. 73). */
	public static final int CLUBES_GERADOS_POR_VAGA = 2;

	public ContinentalPadrao {
		vagas = Collections.unmodifiableMap(new LinkedHashMap<>(vagas));
		int soma = vagas.values().stream().mapToInt(Integer::intValue).sum();
		if (soma != CLUBES) {
			throw new IllegalArgumentException(nome + ": as vagas somam " + soma + ", e não " + CLUBES);
		}
	}

	/** Quantos clubes o banco padrão gera para um país sem liga desta competição. */
	public int clubesGerados(String codigo) {
		return vagas.getOrDefault(codigo, 0) * CLUBES_GERADOS_POR_VAGA;
	}

	private static Map<String, Integer> tabela(Object... codigoEVagas) {
		Map<String, Integer> mapa = new LinkedHashMap<>();
		for (int i = 0; i < codigoEVagas.length; i += 2) {
			mapa.put((String) codigoEVagas[i], (Integer) codigoEVagas[i + 1]);
		}
		return mapa;
	}

	/** Libertadores 2026 adaptada (§8.3.6). */
	public static final ContinentalPadrao AMERICA_DO_SUL = new ContinentalPadrao(Regiao.AMERICA_DO_SUL,
			"Copa dos Clubes da América do Sul", tabela("BRA", 7, "ARG", 6, "CHI", 3, "COL", 3, "URU", 3, "BOL", 2,
					"ECU", 2, "PAR", 2, "PER", 2, "VEN", 2));

	/** Ranking de países da UEFA de 22/10/2026 no modelo antigo de 32 clubes (§8.3.6). */
	public static final ContinentalPadrao EUROPA = new ContinentalPadrao(Regiao.EUROPA, "Copa dos Clubes da Europa",
			tabela("ENG", 4, "ITA", 4, "ESP", 4, "GER", 4, "FRA", 3, "POR", 3, "BEL", 2, "NED", 2, "TUR", 2, "GRE", 1,
					"CZE", 1, "POL", 1, "NOR", 1));

	public static final List<ContinentalPadrao> TODAS = List.of(AMERICA_DO_SUL, EUROPA);
}
