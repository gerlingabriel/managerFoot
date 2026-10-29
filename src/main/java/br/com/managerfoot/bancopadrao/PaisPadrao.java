package br.com.managerfoot.bancopadrao;

import java.util.ArrayList;
import java.util.List;

import br.com.managerfoot.temporada.FormatoDivisao;
import br.com.managerfoot.temporada.FormatoLiga;

/**
 * País do banco padrão (SPEC §4.1, rev. 31): formato adaptado de {@code docs/banco-padrao-formatos.md}
 * e os ingredientes dos nomes fictícios. Cada modelo de nome de clube tem um {@code %s} para o lugar.
 *
 * @param codigo código FIFA do país
 * @param lugares lugares fictícios que entram nos nomes dos clubes
 */
public record PaisPadrao(String codigo, String nome, Idioma idioma, FormatoLiga formato, List<String> modelosClube,
		List<String> lugares) {

	public PaisPadrao {
		modelosClube = List.copyOf(modelosClube);
		lugares = List.copyOf(lugares);
	}

	/** Clubes que o país precisa ter: as divisões e a fila (SPEC §4.1). */
	public int clubesNecessarios() {
		return formato.clubesNecessarios();
	}

	private static final List<String> MODELOS_BRASIL = List.of("Esporte Clube %s", "Atlético %s", "%s Futebol Clube",
			"União %s", "Grêmio %s", "Sport Club %s", "%s Esporte Clube", "Associação %s", "Operário %s",
			"Ferroviário %s", "América de %s", "Comercial de %s");
	private static final List<String> MODELOS_PORTUGAL = List.of("Sporting Clube de %s", "%s FC", "Académico de %s",
			"União de %s", "Desportivo de %s", "Clube Futebol %s", "Atlético de %s", "Grupo Desportivo de %s",
			"Juventude de %s", "Leões de %s");
	private static final List<String> MODELOS_ESPANHOL = List.of("Club Atlético %s", "Deportivo %s", "Racing %s",
			"Real %s", "Sporting %s", "Unión %s", "Club %s", "%s CF", "Atlético %s", "Juventud %s", "Independiente de %s",
			"Estudiantes de %s");
	private static final List<String> MODELOS_INGLATERRA = List.of("%s United", "%s City", "%s Town", "%s Rovers",
			"%s Athletic", "%s Albion", "%s Wanderers", "%s Rangers", "%s County", "%s FC");
	private static final List<String> MODELOS_ITALIA = List.of("AC %s", "US %s", "Sporting %s", "Atletico %s",
			"Virtus %s", "Pro %s", "%s Calcio", "Real %s", "Polisportiva %s", "Juventus %s");

	/** Divisões aos pares (clubes, rebaixados), com o teto de cada uma no topo da faixa da rev. 31. */
	private static List<FormatoDivisao> divisoes(int... quantidadeERebaixados) {
		List<FormatoDivisao> lista = new ArrayList<>();
		for (int i = 0; i < quantidadeERebaixados.length / 2; i++) {
			lista.add(new FormatoDivisao((i + 1) + "ª Divisão", i + 1, quantidadeERebaixados[2 * i],
					ParametrosBancoPadrao.FAIXAS.get(i).maxima(), quantidadeERebaixados[2 * i + 1]));
		}
		return lista;
	}

	/** Os 6 países aprovados na rev. 31, com a adaptação de {@code docs/banco-padrao-formatos.md}. */
	public static final List<PaisPadrao> TODOS = List.of(
			new PaisPadrao("BRA", "Brasil", Idioma.PORTUGUES,
					new FormatoLiga("Liga do Brasil", divisoes(20, 4, 20, 4)), MODELOS_BRASIL,
					List.of("Vale Verde", "Serra Alta", "Porto Firme", "Rio das Pedras", "Ponte Alta", "Monte Claro",
							"Lagoa Funda", "Campo Belo", "Bela Vista", "Santa Luzia", "São Bento", "Vila Nova",
							"Pedra Branca", "Boa Esperança", "Cachoeira", "Três Barras", "Morro Azul", "Areia Branca",
							"Palmeiral", "Cerrado", "Sertão", "Litoral", "Planalto", "Ribeirão", "Arraial", "Guará",
							"Itaúna Velha", "Bom Jardim", "Vargem Grande", "Mata Escura")),
			new PaisPadrao("ARG", "Argentina", Idioma.ESPANHOL,
					new FormatoLiga("Liga de Argentina", divisoes(20, 2, 20, 4)), MODELOS_ESPANHOL,
					List.of("Villa Serena", "Puerto Viejo", "Las Lomas", "Monte Grande Sur", "Río Claro", "Los Álamos",
							"Cañada Honda", "Pampa Alta", "Laguna Seca", "San Roque", "Santa Rita", "Valle Hermoso",
							"Cerro Azul", "Los Robles", "Bahía Blanca Norte", "Altos del Sol", "La Ribera", "El Molino",
							"Las Toscas", "Villa del Lago", "Tres Cruces", "Barrio Parque", "Los Pinos", "El Palomar")),
			new PaisPadrao("ENG", "Inglaterra", Idioma.INGLES,
					new FormatoLiga("League of England", divisoes(20, 3, 20, 3)), MODELOS_INGLATERRA,
					List.of("Ashbridge", "Kingsmoor", "Westerby", "Holloway Vale", "Marlow Heath", "Stonefield",
							"Brackenford", "Eastleigh Park", "Redcliffe", "Thornbury", "Oakhampton", "Fairhaven",
							"Millbrook", "Ravensdale", "Blackwater", "Hartwell", "Elmstead", "Crowley", "Northgate",
							"Sandmouth", "Wexley", "Copperfield", "Highbury Vale", "Lowmoor", "Greystone", "Bramley")),
			new PaisPadrao("ESP", "Espanha", Idioma.ESPANHOL,
					new FormatoLiga("Liga de España", divisoes(20, 3, 20, 4)), MODELOS_ESPANHOL,
					List.of("Villaverde", "Montealto", "Río Seco", "Puertollano Viejo", "Sierra Blanca", "Valdeluna",
							"Castillejo", "Torremar", "Peñarroja", "Fuentesol", "Lomaverde", "Mirasierra", "Costa Brava",
							"Valle del Río", "Almenara", "Campoalegre", "Santa Brígida", "Torrelobos", "Alcazarén",
							"Navalmoral", "Robledal", "Olivares", "Marisma", "Peñaflor")),
			new PaisPadrao("ITA", "Itália", Idioma.ITALIANO,
					new FormatoLiga("Lega d'Italia", divisoes(20, 3, 20, 4)), MODELOS_ITALIA,
					List.of("Valdora", "Montefiore", "Portovecchio", "Castelrosso", "Sanvito", "Roccalta", "Pietrasanta",
							"Belmonte", "Fontebella", "Torrenova", "Lagomare", "Serravalle", "Campobasso Alto",
							"Villafranca", "Marinella", "Collebianco", "Acquaviva", "Borgonuovo", "Castelverde",
							"Pianoro", "Rivalta", "Spinea", "Montalto", "Casalmaggiore")),
			new PaisPadrao("POR", "Portugal", Idioma.PORTUGUES,
					new FormatoLiga("Liga de Portugal", divisoes(18, 3, 18, 3)), MODELOS_PORTUGAL,
					List.of("Vale do Sol", "Monte Alto", "Ribeira Grande", "Serra da Estrela Nova", "Póvoa Velha",
							"Vila Franca", "Alverca Nova", "Santa Marta", "Cova da Moura", "Penedo", "Quinta do Lago",
							"Castelo Branco Novo", "Ponte da Barca", "Torre Alta", "Freixo", "Mondim", "Cabeceiras",
							"Lagoa", "Esposende Velha", "Moinhos", "Carvalhal", "Belmonte", "Arrifana", "Souto")));
}
