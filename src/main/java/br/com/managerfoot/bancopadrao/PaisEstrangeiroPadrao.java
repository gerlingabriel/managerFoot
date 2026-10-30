package br.com.managerfoot.bancopadrao;

import java.util.List;
import java.util.Map;

/**
 * País sem liga jogada, gerado só com clubes para a competição continental (SPEC §8.3.6, rev. 73).
 * Os clubes não evoluem nem têm caixa; deixam só uns dois à venda por temporada (rev. 84); quantos são gerados vem de
 * {@link ContinentalPadrao#clubesGerados(String)}.
 *
 * @param codigo código FIFA do país
 * @param lugares lugares fictícios que entram nos nomes dos clubes
 */
public record PaisEstrangeiroPadrao(String codigo, String nome, Idioma idioma, NivelEstrangeiro nivel,
		List<String> modelosClube, List<String> lugares) {

	public PaisEstrangeiroPadrao {
		modelosClube = List.copyOf(modelosClube);
		lugares = List.copyOf(lugares);
	}

	private static final List<String> MODELOS_ESPANHOL = List.of("Club Atlético %s", "Deportivo %s", "Club %s",
			"%s FC", "Sporting %s", "Unión %s", "Juventud %s", "Independiente de %s");
	private static final List<String> MODELOS_ALEMANHA = List.of("FC %s", "SV %s", "VfB %s", "Borussia %s",
			"Eintracht %s", "TSV %s", "SC %s", "Fortuna %s");
	private static final List<String> MODELOS_FRANCA = List.of("FC %s", "Olympique de %s", "Stade %s", "AS %s",
			"Racing Club de %s", "US %s", "SC %s");
	private static final List<String> MODELOS_NEERLANDES = List.of("FC %s", "SV %s", "VV %s", "Sportclub %s",
			"Racing %s", "KV %s");
	private static final List<String> MODELOS_TURQUIA = List.of("%sspor", "%s SK", "%s Belediyespor", "%s FK",
			"%s Gençlik");
	private static final List<String> MODELOS_GRECIA = List.of("Athlitikos %s", "PAS %s", "AE %s", "Asteras %s");
	private static final List<String> MODELOS_TCHEQUIA = List.of("FK %s", "SK %s", "Slovan %s", "Dukla %s");
	private static final List<String> MODELOS_POLONIA = List.of("KS %s", "Polonia %s", "Górnik %s", "Lech %s");
	private static final List<String> MODELOS_NORUEGA = List.of("%s IL", "%s FK", "%s BK", "IF %s");

	private static final List<String> MODELOS_INGLES = List.of("%s United", "%s City", "%s FC", "%s Rangers", "%s Athletic",
			"%s Rovers");
	private static final List<String> MODELOS_JAPAO = List.of("%s FC", "%s SC", "FC %s", "%s United", "%s Athletic");
	private static final List<String> MODELOS_COREIA = List.of("%s FC", "%s United", "FC %s", "%s Citizen", "%s Hyundae");
	private static final List<String> MODELOS_ARABE = List.of("Al-%s", "Al-%s Club", "Ittihad %s", "Nadi %s", "Ahli %s");
	private static final List<String> MODELOS_FRANCES_AFRICA = List.of("AS %s", "ASC %s", "Jaraaf de %s", "US %s", "Stade de %s");

	/** Clubes gerados de cada país que só joga o Mundial (rev. 88). */
	public static final int CLUBES_DO_MUNDIAL = 4;

	/** Países sem continental, gerados para representar a região deles no Mundial (rev. 88). */
	public static final List<PaisEstrangeiroPadrao> DO_MUNDIAL = List.of(
			new PaisEstrangeiroPadrao("MEX", "México", Idioma.ESPANHOL, NivelEstrangeiro.B, MODELOS_ESPANHOL,
					List.of("Sierra Alta", "Valle Dorado", "Puerto Azteca", "Altamira Norte")),
			new PaisEstrangeiroPadrao("USA", "Estados Unidos", Idioma.INGLES, NivelEstrangeiro.B, MODELOS_INGLES,
					List.of("Pine Harbor", "Red Valley", "Lakeview", "Stone Creek")),
			new PaisEstrangeiroPadrao("CRC", "Costa Rica", Idioma.ESPANHOL, NivelEstrangeiro.C, MODELOS_ESPANHOL,
					List.of("Monteverde Alto", "Puerto Limón Viejo", "Valle Central", "Río Celeste")),
			new PaisEstrangeiroPadrao("JPN", "Japão", Idioma.JAPONES, NivelEstrangeiro.B, MODELOS_JAPAO,
					List.of("Sakura Minato", "Aoyama", "Kitahama", "Shirakawa")),
			new PaisEstrangeiroPadrao("KOR", "Coreia do Sul", Idioma.COREANO, NivelEstrangeiro.B, MODELOS_COREIA,
					List.of("Hanbit", "Namhae", "Seorak", "Dongmun")),
			new PaisEstrangeiroPadrao("KSA", "Arábia Saudita", Idioma.ARABE, NivelEstrangeiro.C, MODELOS_ARABE,
					List.of("Nakheel", "Wadi", "Sahra", "Rimal")),
			new PaisEstrangeiroPadrao("MAR", "Marrocos", Idioma.ARABE, NivelEstrangeiro.B, MODELOS_ARABE,
					List.of("Atlas", "Maghreb", "Medina", "Saidia Nova")),
			new PaisEstrangeiroPadrao("EGY", "Egito", Idioma.ARABE, NivelEstrangeiro.C, MODELOS_ARABE,
					List.of("Nil", "Delta", "Wahat", "Sinai Norte")),
			new PaisEstrangeiroPadrao("SEN", "Senegal", Idioma.FRANCES, NivelEstrangeiro.C, MODELOS_FRANCES_AFRICA,
					List.of("Casamance Nord", "Petite Côte", "Ngor Neuf", "Saloum")),
			new PaisEstrangeiroPadrao("NZL", "Nova Zelândia", Idioma.INGLES, NivelEstrangeiro.D, MODELOS_INGLES,
					List.of("Kauri Bay", "Southern Lakes", "Tasman Point", "Rangi")),
			new PaisEstrangeiroPadrao("FIJ", "Fiji", Idioma.INGLES, NivelEstrangeiro.D, MODELOS_INGLES,
					List.of("Coral Coast", "Nadi Bay", "Viti Levu Sul", "Lautoka Nova")));

	/** A região de cada país de {@link #DO_MUNDIAL}. */
	public static final Map<String, Regiao> REGIOES_DO_MUNDIAL = Map.ofEntries(Map.entry("MEX", Regiao.AMERICA_DO_NORTE),
			Map.entry("USA", Regiao.AMERICA_DO_NORTE), Map.entry("CRC", Regiao.AMERICA_DO_NORTE), Map.entry("JPN", Regiao.ASIA),
			Map.entry("KOR", Regiao.ASIA), Map.entry("KSA", Regiao.ASIA), Map.entry("MAR", Regiao.AFRICA),
			Map.entry("EGY", Regiao.AFRICA), Map.entry("SEN", Regiao.AFRICA), Map.entry("NZL", Regiao.OCEANIA),
			Map.entry("FIJ", Regiao.OCEANIA));

	/** Os países gerados da rev. 73: os da tabela de vagas que não têm liga no banco padrão. */
	public static final List<PaisEstrangeiroPadrao> TODOS = List.of(
			new PaisEstrangeiroPadrao("URU", "Uruguai", Idioma.ESPANHOL, NivelEstrangeiro.B, MODELOS_ESPANHOL,
					List.of("Arroyo Seco", "Cerro Chico", "Playa Honda", "Las Piedras Viejas", "Santa Lucía Norte")),
			new PaisEstrangeiroPadrao("COL", "Colômbia", Idioma.ESPANHOL, NivelEstrangeiro.B, MODELOS_ESPANHOL,
					List.of("Valle Alto", "Río Verde", "Sierra Nevada Sur", "Puerto Claro", "San Jacinto")),
			new PaisEstrangeiroPadrao("CHI", "Chile", Idioma.ESPANHOL, NivelEstrangeiro.B, MODELOS_ESPANHOL,
					List.of("Valle Andino", "Puerto Austral", "Costa Azul", "Los Copihues", "Cordillera")),
			new PaisEstrangeiroPadrao("ECU", "Equador", Idioma.ESPANHOL, NivelEstrangeiro.B, MODELOS_ESPANHOL,
					List.of("Altiplano", "Río Grande", "Costa Verde", "Montecristi Alto")),
			new PaisEstrangeiroPadrao("PAR", "Paraguai", Idioma.ESPANHOL, NivelEstrangeiro.B, MODELOS_ESPANHOL,
					List.of("Lago Azul", "Chaco Norte", "San Lorenzo Viejo", "Itá Verá")),
			new PaisEstrangeiroPadrao("BOL", "Bolívia", Idioma.ESPANHOL, NivelEstrangeiro.C, MODELOS_ESPANHOL,
					List.of("Altura", "Valle Sereno", "Los Yungas", "Cerro Rico Norte")),
			new PaisEstrangeiroPadrao("PER", "Peru", Idioma.ESPANHOL, NivelEstrangeiro.C, MODELOS_ESPANHOL,
					List.of("Costa del Sol", "Valle Sagrado Sur", "Puerto Callao Viejo", "Arequipa Alta")),
			new PaisEstrangeiroPadrao("VEN", "Venezuela", Idioma.ESPANHOL, NivelEstrangeiro.C, MODELOS_ESPANHOL,
					List.of("Llano Alto", "Puerto Cabello Viejo", "Monte Ávila", "Lago Claro")),
			new PaisEstrangeiroPadrao("GER", "Alemanha", Idioma.ALEMAO, NivelEstrangeiro.A, MODELOS_ALEMANHA,
					List.of("Waldheim", "Steinbach", "Rosenfeld", "Lindenau", "Bergstadt", "Neuhafen", "Eichwald",
							"Altmühl")),
			new PaisEstrangeiroPadrao("FRA", "França", Idioma.FRANCES, NivelEstrangeiro.A, MODELOS_FRANCA,
					List.of("Beauval", "Montclair", "Valmont", "Saint-Aubin", "Rochefort-Neuf", "Clairfontaine")),
			new PaisEstrangeiroPadrao("BEL", "Bélgica", Idioma.NEERLANDES, NivelEstrangeiro.B, MODELOS_NEERLANDES,
					List.of("Lindebeek", "Oosterveld", "Hoogstraten", "Zandhoven")),
			new PaisEstrangeiroPadrao("NED", "Países Baixos", Idioma.NEERLANDES, NivelEstrangeiro.B, MODELOS_NEERLANDES,
					List.of("Westerdam", "Molenveld", "Zeewolde Oost", "Heidebeek")),
			new PaisEstrangeiroPadrao("TUR", "Turquia", Idioma.TURCO, NivelEstrangeiro.B, MODELOS_TURQUIA,
					List.of("Yeşilköy", "Karadere", "Akdeniz", "Çamlıca")),
			new PaisEstrangeiroPadrao("GRE", "Grécia", Idioma.GREGO, NivelEstrangeiro.C, MODELOS_GRECIA,
					List.of("Kallithea Nea", "Agios Petros", "Limni")),
			new PaisEstrangeiroPadrao("CZE", "Tchéquia", Idioma.TCHECO, NivelEstrangeiro.C, MODELOS_TCHEQUIA,
					List.of("Horní Lhota", "Nové Pole", "Lesná")),
			new PaisEstrangeiroPadrao("POL", "Polônia", Idioma.POLONES, NivelEstrangeiro.C, MODELOS_POLONIA,
					List.of("Zielona Dolina", "Stary Gród", "Brzozów Nowy")),
			new PaisEstrangeiroPadrao("NOR", "Noruega", Idioma.NORUEGUES, NivelEstrangeiro.C, MODELOS_NORUEGA,
					List.of("Fjellvik", "Nordstrand", "Sjøholm")));
}
