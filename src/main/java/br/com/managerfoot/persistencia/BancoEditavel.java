package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import br.com.managerfoot.bancopadrao.Regiao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escudo;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.ModeloEscudo;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.temporada.FormatoDivisao;
import br.com.managerfoot.temporada.FormatoLiga;
import br.com.managerfoot.temporada.ValidadorLiga;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Banco editável (SPEC §3.5, rev. 89): países, ligas, clubes e jogadores num arquivo JSON que a pessoa guarda no
 * computador, edita e envia ao criar um jogo. Aqui só se lê, confere e escreve o arquivo; quem grava é
 * {@link BancoEditavelRepositorio}. A leitura junta todos os problemas, para a pessoa corrigir tudo de uma vez.
 */
public record BancoEditavel(List<Pais> paises, List<Continental> continentais) {

	/** Versão do formato; ao mudar, converter aqui os arquivos das versões anteriores. */
	public static final int FORMATO = 1;

	/** Mínimo de jogadores por clube no arquivo (Provisório, rev. 89): um time completo. */
	public static final int MINIMO_JOGADORES = 11;

	/** Limite dos nomes, o mesmo do banco. */
	public static final int TAMANHO_NOME = 60;

	/** Limites que só protegem o servidor de um arquivo enorme. */
	static final int MAXIMO_PAISES = 100;
	static final int MAXIMO_CLUBES_POR_PAIS = 100;

	/** Quantos problemas a tela mostra no máximo; o resto é resumido. */
	static final int MAXIMO_PROBLEMAS = 40;

	private static final JsonMapper JSON = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

	public BancoEditavel {
		paises = List.copyOf(paises);
		continentais = List.copyOf(continentais);
	}

	/**
	 * Um país.
	 *
	 * @param regiao nula se o país não joga competições continentais nem o Mundial
	 * @param liga nula se o país só tem clubes para as competições continentais
	 */
	public record Pais(String codigo, String nome, Regiao regiao, Optional<FormatoLiga> liga, List<ClubeEditavel> clubes) {

		public Pais {
			clubes = List.copyOf(clubes);
		}
	}

	/** Clube com o treinador e o plantel; a força é a cadastrada (§4.3). */
	public record ClubeEditavel(Clube clube, String treinador, List<Jogador> jogadores) {

		public ClubeEditavel {
			jogadores = List.copyOf(jogadores);
		}
	}

	/** Competição continental com as vagas por país, na ordem do arquivo. */
	public record Continental(Regiao regiao, String nome, Map<String, Integer> vagas) {
	}

	/** Os campeonatos do banco: os países com liga. */
	public List<Pais> comLiga() {
		return paises.stream().filter(p -> p.liga().isPresent()).toList();
	}

	public Optional<Pais> pais(String codigo) {
		return paises.stream().filter(p -> p.codigo().equals(codigo)).findFirst();
	}

	/**
	 * Os clubes do país numerados de 1 em diante, na ordem do arquivo. É a ordem em que eles são gravados no molde, e
	 * por isso a distribuição pela mesma semente dá o mesmo quadro aqui e no jogo criado.
	 */
	public static List<Clube> clubesNumerados(Pais pais) {
		List<Clube> clubes = new ArrayList<>();
		for (int i = 0; i < pais.clubes().size(); i++) {
			Clube c = pais.clubes().get(i).clube();
			clubes.add(new Clube(i + 1, c.nome(), c.forca(), c.escudo()));
		}
		return clubes;
	}

	/** O arquivo, em texto organizado para editar à mão. */
	public byte[] paraArquivo() {
		Map<String, Object> raiz = new LinkedHashMap<>();
		raiz.put("aplicacao", "managerFoot");
		raiz.put("tipo", "banco");
		raiz.put("formato", FORMATO);
		List<Object> listaPaises = new ArrayList<>();
		for (Pais pais : paises) {
			Map<String, Object> p = new LinkedHashMap<>();
			p.put("codigo", pais.codigo());
			p.put("nome", pais.nome());
			p.put("regiao", pais.regiao() == null ? null : pais.regiao().name());
			p.put("liga", pais.liga().map(FormatoLiga::nome).orElse(null));
			p.put("divisoes", pais.liga().map(l -> l.divisoes().stream().map(d -> {
				Map<String, Object> divisao = new LinkedHashMap<>();
				divisao.put("nome", d.nome());
				divisao.put("clubes", d.quantidadeClubes());
				divisao.put("limiteForca", d.limiteForca());
				divisao.put("rebaixados", d.rebaixados());
				return (Object) divisao;
			}).toList()).orElse(List.of()));
			p.put("clubes", pais.clubes().stream().map(c -> {
				Map<String, Object> clube = new LinkedHashMap<>();
				clube.put("nome", c.clube().nome());
				clube.put("forca", c.clube().forca());
				clube.put("treinador", c.treinador());
				clube.put("escudo", Map.of("modelo", c.clube().escudo().modelo().name(), "cor1", c.clube().escudo().corPrimaria(),
						"cor2", c.clube().escudo().corSecundaria()));
				clube.put("jogadores", c.jogadores().stream().map(j -> {
					Map<String, Object> jogador = new LinkedHashMap<>();
					jogador.put("nome", j.nome());
					jogador.put("posicao", j.posicao().name());
					jogador.put("forca", j.forca());
					jogador.put("temperamento", j.temperamento().name());
					jogador.put("asterisco", j.asterisco());
					jogador.put("nacionalidade", j.nacionalidade());
					return (Object) jogador;
				}).toList());
				return (Object) clube;
			}).toList());
			listaPaises.add(p);
		}
		raiz.put("paises", listaPaises);
		raiz.put("continentais", continentais.stream().map(c -> {
			Map<String, Object> continental = new LinkedHashMap<>();
			continental.put("regiao", c.regiao().name());
			continental.put("nome", c.nome());
			continental.put("vagas", c.vagas());
			return (Object) continental;
		}).toList());
		return JSON.writeValueAsBytes(raiz);
	}

	/**
	 * Lê e confere o arquivo.
	 *
	 * @param paisFifa se o código é de um país do catálogo (§3.5)
	 * @throws BancoRecusadoException com todos os problemas encontrados
	 */
	public static BancoEditavel ler(byte[] arquivo, Function<String, Boolean> paisFifa) {
		JsonNode raiz;
		try {
			raiz = JSON.readTree(arquivo);
		}
		catch (JacksonException e) {
			throw new BancoRecusadoException(List.of("Este arquivo não é um banco do managerFoot."));
		}
		if (raiz == null || !raiz.isObject() || !"managerFoot".equals(raiz.path("aplicacao").asString(""))) {
			throw new BancoRecusadoException(List.of("Este arquivo não é um banco do managerFoot."));
		}
		if (!"banco".equals(raiz.path("tipo").asString(""))) {
			throw new BancoRecusadoException(List.of("Este arquivo é um jogo salvo, não um banco. Use “Carregar de um arquivo” no início."));
		}
		int formato = raiz.path("formato").asInt(0);
		if (formato > FORMATO) {
			throw new BancoRecusadoException(List.of("Este banco veio de uma versão mais nova do jogo (formato " + formato + ")."));
		}
		if (formato < 1) {
			throw new BancoRecusadoException(List.of("Este banco tem um formato desconhecido."));
		}
		Leitor leitor = new Leitor(paisFifa);
		BancoEditavel banco = leitor.banco(raiz);
		if (!leitor.problemas.isEmpty()) {
			List<String> mostrados = new ArrayList<>(leitor.problemas.subList(0, Math.min(MAXIMO_PROBLEMAS, leitor.problemas.size())));
			if (leitor.problemas.size() > MAXIMO_PROBLEMAS) {
				mostrados.add("E mais " + (leitor.problemas.size() - MAXIMO_PROBLEMAS) + " problema(s) parecido(s).");
			}
			throw new BancoRecusadoException(mostrados);
		}
		return banco;
	}

	/** Junta os problemas com o lugar de cada um no arquivo. */
	private static final class Leitor {

		private final Function<String, Boolean> paisFifa;
		private final List<String> problemas = new ArrayList<>();

		Leitor(Function<String, Boolean> paisFifa) {
			this.paisFifa = paisFifa;
		}

		BancoEditavel banco(JsonNode raiz) {
			JsonNode nos = raiz.path("paises");
			if (!nos.isArray() || nos.isEmpty()) {
				problemas.add("O arquivo precisa da lista “paises”, com ao menos um país.");
				return new BancoEditavel(List.of(), List.of());
			}
			if (nos.size() > MAXIMO_PAISES) {
				problemas.add("O arquivo tem " + nos.size() + " países. O máximo é " + MAXIMO_PAISES + ".");
				return new BancoEditavel(List.of(), List.of());
			}
			List<Pais> paises = new ArrayList<>();
			Set<String> codigos = new HashSet<>();
			int i = 0;
			for (JsonNode no : nos) {
				i++;
				Optional<Pais> pais = pais(no, i);
				pais.ifPresent(p -> {
					if (!codigos.add(p.codigo())) {
						problemas.add("O país " + p.codigo() + " aparece mais de uma vez.");
					}
					paises.add(p);
				});
			}
			if (paises.stream().noneMatch(p -> p.liga().isPresent()) && problemas.isEmpty()) {
				problemas.add("Nenhum país tem liga: é preciso ao menos um campeonato para jogar.");
			}
			List<Continental> continentais = new ArrayList<>();
			JsonNode listaContinentais = raiz.path("continentais");
			if (!listaContinentais.isMissingNode() && !listaContinentais.isNull() && !listaContinentais.isArray()) {
				problemas.add("“continentais” precisa ser uma lista.");
			}
			Set<Regiao> regioes = new HashSet<>();
			for (JsonNode no : listaContinentais.isArray() ? listaContinentais : JSON.createArrayNode()) {
				continental(no, codigos).ifPresent(c -> {
					if (!regioes.add(c.regiao())) {
						problemas.add("Há mais de uma competição continental para " + c.regiao() + ".");
					}
					continentais.add(c);
				});
			}
			return new BancoEditavel(paises, continentais);
		}

		private Optional<Pais> pais(JsonNode no, int ordem) {
			String codigo = no.path("codigo").asString("");
			String onde = codigo.isBlank() ? "País " + ordem : no.path("nome").asString(codigo);
			int antes = problemas.size();
			if (!codigo.matches("[A-Z]{3}") || !paisFifa.apply(codigo)) {
				problemas.add(onde + ": o código “" + codigo + "” não é de um país do catálogo (três letras, como BRA).");
			}
			String nome = nome(no, "nome", onde, "o nome do país");
			Regiao regiao = null;
			String textoRegiao = no.path("regiao").asString("");
			if (!textoRegiao.isBlank()) {
				regiao = valor(Regiao.class, textoRegiao, onde + ": região");
			}
			Optional<FormatoLiga> liga = liga(no, onde);
			JsonNode nosClubes = no.path("clubes");
			if (!nosClubes.isArray() || nosClubes.size() > MAXIMO_CLUBES_POR_PAIS) {
				problemas.add(onde + ": “clubes” precisa ser uma lista de até " + MAXIMO_CLUBES_POR_PAIS + " clubes.");
				return Optional.empty();
			}
			List<ClubeEditavel> clubes = new ArrayList<>();
			Set<String> nomes = new HashSet<>();
			for (JsonNode noClube : nosClubes) {
				clube(noClube, onde, clubes.size() + 1).ifPresent(c -> {
					if (!nomes.add(c.clube().nome().strip().toLowerCase())) {
						problemas.add(onde + ": o clube “" + c.clube().nome() + "” aparece mais de uma vez.");
					}
					clubes.add(c);
				});
			}
			liga.ifPresent(l -> ValidadorLiga.problemas(l, nosClubes.size()).forEach(p -> problemas.add(onde + ": " + p)));
			if (problemas.size() > antes) {
				return Optional.empty();
			}
			return Optional.of(new Pais(codigo, nome, regiao, liga, clubes));
		}

		private Optional<FormatoLiga> liga(JsonNode no, String onde) {
			JsonNode divisoes = no.path("divisoes");
			String nomeLiga = no.path("liga").asString("");
			if (nomeLiga.isBlank()) {
				if (divisoes.isArray() && !divisoes.isEmpty()) {
					problemas.add(onde + ": tem divisões, mas falta o nome da liga.");
				}
				return Optional.empty();
			}
			nome(no, "liga", onde, "o nome da liga");
			if (!divisoes.isArray() || divisoes.isEmpty()) {
				problemas.add(onde + ": a liga precisa de ao menos uma divisão.");
				return Optional.empty();
			}
			List<FormatoDivisao> lista = new ArrayList<>();
			int prioridade = 0;
			for (JsonNode d : divisoes) {
				prioridade++;
				String nome = nome(d, "nome", onde + ", divisão " + prioridade, "o nome da divisão");
				Integer clubes = inteiro(d, "clubes", onde + ", " + nome);
				Integer limite = inteiro(d, "limiteForca", onde + ", " + nome);
				Integer rebaixados = inteiro(d, "rebaixados", onde + ", " + nome);
				if (clubes == null || limite == null || rebaixados == null) {
					return Optional.empty();
				}
				try {
					lista.add(new FormatoDivisao(nome, prioridade, clubes, limite, rebaixados));
				}
				catch (IllegalArgumentException e) {
					problemas.add(onde + ", " + nome + ": " + e.getMessage() + ".");
					return Optional.empty();
				}
			}
			try {
				return Optional.of(new FormatoLiga(nomeLiga, lista));
			}
			catch (IllegalArgumentException e) {
				problemas.add(onde + ": " + e.getMessage() + ".");
				return Optional.empty();
			}
		}

		private Optional<ClubeEditavel> clube(JsonNode no, String pais, int ordem) {
			String rotulo = no.path("nome").asString("").isBlank() ? "clube " + ordem : "clube “" + no.path("nome").asString() + "”";
			String onde = pais + ", " + rotulo;
			int antes = problemas.size();
			String nome = nome(no, "nome", pais + ", clube " + ordem, "o nome do clube");
			String treinador = nome(no, "treinador", onde, "o nome do treinador");
			Integer forca = forca(no, onde);
			Escudo escudo = null;
			JsonNode noEscudo = no.path("escudo");
			ModeloEscudo modelo = valor(ModeloEscudo.class, noEscudo.path("modelo").asString(""), onde + ": modelo do escudo");
			if (modelo != null) {
				try {
					escudo = new Escudo(modelo, noEscudo.path("cor1").asString(""), noEscudo.path("cor2").asString(""));
				}
				catch (IllegalArgumentException e) {
					problemas.add(onde + ": " + e.getMessage() + ".");
				}
			}
			JsonNode nosJogadores = no.path("jogadores");
			if (!nosJogadores.isArray() || nosJogadores.size() < MINIMO_JOGADORES || nosJogadores.size() > Clube.MAXIMO_JOGADORES) {
				problemas.add(onde + ": tem " + (nosJogadores.isArray() ? nosJogadores.size() : 0) + " jogadores. Precisa ter de "
						+ MINIMO_JOGADORES + " a " + Clube.MAXIMO_JOGADORES + ".");
				return Optional.empty();
			}
			List<Jogador> jogadores = new ArrayList<>();
			int k = 0;
			for (JsonNode j : nosJogadores) {
				k++;
				jogador(j, onde, k).ifPresent(jogadores::add);
			}
			if (jogadores.size() == nosJogadores.size() && jogadores.stream().noneMatch(j -> j.posicao() == Posicao.GOLEIRO)) {
				problemas.add(onde + ": precisa de ao menos um goleiro.");
			}
			if (problemas.size() > antes) {
				return Optional.empty();
			}
			return Optional.of(new ClubeEditavel(new Clube(0, nome, forca, escudo), treinador, jogadores));
		}

		private Optional<Jogador> jogador(JsonNode no, String clube, int ordem) {
			String onde = clube + ", jogador " + ordem + (no.path("nome").asString("").isBlank() ? "" : " (“" + no.path("nome").asString() + "”)");
			int antes = problemas.size();
			String nome = nome(no, "nome", onde, "o nome do jogador");
			Posicao posicao = valor(Posicao.class, no.path("posicao").asString(""), onde + ": posição");
			Integer forca = forca(no, onde);
			Temperamento temperamento = valor(Temperamento.class, no.path("temperamento").asString(""), onde + ": temperamento");
			JsonNode asterisco = no.path("asterisco");
			if (!asterisco.isBoolean()) {
				problemas.add(onde + ": “asterisco” precisa ser true ou false.");
			}
			String nacionalidade = no.path("nacionalidade").asString("");
			if (!nacionalidade.matches("[A-Z]{3}") || !paisFifa.apply(nacionalidade)) {
				problemas.add(onde + ": a nacionalidade “" + nacionalidade + "” não é de um país do catálogo.");
			}
			if (problemas.size() > antes) {
				return Optional.empty();
			}
			return Optional.of(new Jogador(0, nome, posicao, forca, temperamento, asterisco.booleanValue(), nacionalidade));
		}

		private Optional<Continental> continental(JsonNode no, Set<String> codigos) {
			String nome = no.path("nome").asString("");
			String onde = "Competição continental “" + nome + "”";
			int antes = problemas.size();
			Regiao regiao = valor(Regiao.class, no.path("regiao").asString(""), onde + ": região");
			nome(no, "nome", onde, "o nome da competição");
			Map<String, Integer> vagas = new LinkedHashMap<>();
			JsonNode nosVagas = no.path("vagas");
			if (!nosVagas.isObject() || nosVagas.isEmpty()) {
				problemas.add(onde + ": precisa das vagas por país.");
			}
			for (String codigo : nosVagas.isObject() ? nosVagas.propertyNames() : Set.<String>of()) {
				JsonNode vaga = nosVagas.get(codigo);
				if (!codigos.contains(codigo)) {
					problemas.add(onde + ": o país " + codigo + " não está no arquivo.");
				} else if (!vaga.isIntegralNumber() || vaga.asInt() < 1) {
					problemas.add(onde + ": as vagas de " + codigo + " precisam ser um número a partir de 1.");
				} else {
					vagas.put(codigo, vaga.asInt());
				}
			}
			if (problemas.size() > antes) {
				return Optional.empty();
			}
			return Optional.of(new Continental(regiao, nome, vagas));
		}

		private String nome(JsonNode no, String campo, String onde, String descricao) {
			JsonNode valor = no.path(campo);
			String texto = valor.isString() ? valor.asString().strip() : "";
			if (texto.isEmpty()) {
				problemas.add(onde + ": falta " + descricao + ".");
			} else if (texto.length() > TAMANHO_NOME) {
				problemas.add(onde + ": " + descricao + " passa de " + TAMANHO_NOME + " letras.");
			}
			return texto;
		}

		private Integer forca(JsonNode no, String onde) {
			Integer forca = inteiro(no, "forca", onde);
			if (forca != null && (forca < Jogador.FORCA_MINIMA || forca > Jogador.FORCA_MAXIMA)) {
				problemas.add(onde + ": força " + forca + ". A força vai de " + Jogador.FORCA_MINIMA + " a " + Jogador.FORCA_MAXIMA + ".");
				return null;
			}
			return forca;
		}

		private Integer inteiro(JsonNode no, String campo, String onde) {
			JsonNode valor = no.path(campo);
			if (!valor.isIntegralNumber() || !valor.canConvertToInt()) {
				problemas.add(onde + ": “" + campo + "” precisa ser um número inteiro.");
				return null;
			}
			return valor.asInt();
		}

		private <E extends Enum<E>> E valor(Class<E> tipo, String texto, String onde) {
			try {
				return Enum.valueOf(tipo, texto);
			}
			catch (IllegalArgumentException e) {
				List<String> aceitos = new ArrayList<>();
				for (E constante : tipo.getEnumConstants()) {
					aceitos.add(constante.name());
				}
				problemas.add(onde + " “" + texto + "” não existe. Use um destes: " + String.join(", ", aceitos) + ".");
				return null;
			}
		}
	}
}
