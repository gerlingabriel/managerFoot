package br.com.managerfoot.bancopadrao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.SplittableRandom;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escudo;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.ModeloEscudo;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.temporada.FormatoDivisao;

/**
 * Gera os clubes e jogadores do banco padrão (SPEC §4.1, §4.3, §5.2 e §5.3, rev. 30 e 31), sem
 * banco de dados. A mesma semente gera o mesmo banco; cada país tem a sua sequência, para que
 * acrescentar um país não mude os outros.
 *
 * <ul>
 * <li>Os clubes de cada divisão espalham-se pela faixa de força dela; os da fila entram na faixa da
 * última e, por serem os mais fracos, são os que a distribuição manda para a fila.</li>
 * <li>A força do clube é a média dos 11 que a máquina escalaria (rev. 31).</li>
 * <li>Todo clube tem jogadores de outros países (rev. 87), sorteados depois de pronto o plantel: mudam só o nome e
 * a nacionalidade, de modo que força, posição e asterisco continuam os mesmos.</li>
 * <li>Os países sem liga da continental (rev. 73) só têm clubes, espalhados pela faixa do nível do
 * país.</li>
 * </ul>
 */
public final class GeradorBancoPadrao {

	private final ParametrosBancoPadrao parametros;
	private final EscalacaoMaquina escalacao;

	public GeradorBancoPadrao(ParametrosBancoPadrao parametros, ParametrosMotor parametrosMotor) {
		this.parametros = parametros;
		this.escalacao = new EscalacaoMaquina(parametrosMotor);
	}

	/** Clube gerado com o treinador e o plantel; os ids são provisórios, o banco dá os definitivos. */
	public record ClubeGerado(Clube clube, String treinador, List<Jogador> jogadores) {
	}

	/** País com todos os clubes necessários, do mais forte ao mais fraco. */
	public record PaisGerado(PaisPadrao pais, List<ClubeGerado> clubes) {
	}

	/** País gerado só para a competição continental, do clube mais forte ao mais fraco (rev. 73). */
	public record EstrangeiroGerado(PaisEstrangeiroPadrao pais, List<ClubeGerado> clubes) {
	}

	/** Banco padrão inteiro: os países com liga, os gerados só para a continental e as vagas. */
	public record BancoGerado(List<PaisGerado> paises, List<EstrangeiroGerado> estrangeiros,
			List<ContinentalPadrao> continentais) {

		public BancoGerado {
			paises = List.copyOf(paises);
			estrangeiros = List.copyOf(estrangeiros);
			continentais = List.copyOf(continentais);
		}
	}

	/** O banco padrão da rev. 73: {@link PaisPadrao#TODOS}, {@link PaisEstrangeiroPadrao#TODOS} e as vagas. */
	public BancoGerado gerarBanco() {
		return new BancoGerado(gerar(PaisPadrao.TODOS),
				gerarEstrangeiros(PaisEstrangeiroPadrao.TODOS, ContinentalPadrao.TODAS), ContinentalPadrao.TODAS);
	}

	public List<PaisGerado> gerar(List<PaisPadrao> paises) {
		return paises.stream().map(this::gerar).toList();
	}

	public PaisGerado gerar(PaisPadrao pais) {
		SplittableRandom aleatorio = new SplittableRandom(parametros.semente() * 31 + pais.codigo().hashCode());
		List<FormatoDivisao> divisoes = pais.formato().divisoes();
		if (divisoes.size() > parametros.faixas().size()) {
			throw new IllegalArgumentException(pais.nome() + " tem " + divisoes.size() + " divisões, mas só há faixas de força para "
					+ parametros.faixas().size());
		}
		Set<String> nomesUsados = new HashSet<>();
		List<ClubeGerado> clubes = new ArrayList<>();
		for (int i = 0; i < divisoes.size(); i++) {
			FormatoDivisao divisao = divisoes.get(i);
			int quantidade = divisao.quantidadeClubes() + (i == divisoes.size() - 1 ? divisao.rebaixados() : 0);
			ParametrosBancoPadrao.FaixaForca faixa = parametros.faixas().get(i);
			for (int k = 0; k < quantidade; k++) {
				int forca = forcaNaFaixa(faixa, k, quantidade, aleatorio);
				Clube clube = new Clube(clubes.size() + 1, nomeDoClube(pais.nome(), pais.modelosClube(), pais.lugares(),
						pais.clubesNecessarios(), nomesUsados, aleatorio), forca,
						escudo(aleatorio));
				clubes.add(new ClubeGerado(clube, null,
						plantel(pais.idioma(), pais.codigo(), forca, faixa.maxima(), aleatorio)));
			}
		}
		return new PaisGerado(pais, comTreinadores(pais.idioma(), comEstrangeiros(pais.codigo(), clubes, aleatorio), aleatorio));
	}

	/** Os clubes do país sem liga, na faixa do nível dele, com a sequência própria do país. */
	public EstrangeiroGerado gerar(PaisEstrangeiroPadrao pais, int quantidade) {
		SplittableRandom aleatorio = new SplittableRandom(parametros.semente() * 31 + pais.codigo().hashCode());
		ParametrosBancoPadrao.FaixaForca faixa = parametros.faixasEstrangeiros().get(pais.nivel());
		Set<String> nomesUsados = new HashSet<>();
		List<ClubeGerado> clubes = new ArrayList<>();
		for (int k = 0; k < quantidade; k++) {
			int forca = forcaNaFaixa(faixa, k, quantidade, aleatorio);
			Clube clube = new Clube(clubes.size() + 1,
					nomeDoClube(pais.nome(), pais.modelosClube(), pais.lugares(), quantidade, nomesUsados, aleatorio), forca,
					escudo(aleatorio));
			clubes.add(new ClubeGerado(clube, null, plantel(pais.idioma(), pais.codigo(), forca, faixa.maxima(), aleatorio)));
		}
		return new EstrangeiroGerado(pais, comTreinadores(pais.idioma(), comEstrangeiros(pais.codigo(), clubes, aleatorio), aleatorio));
	}

	/** Os países sem liga das competições, cada um com os clubes que {@code continentais} pedem. */
	public List<EstrangeiroGerado> gerarEstrangeiros(List<PaisEstrangeiroPadrao> paises,
			List<ContinentalPadrao> continentais) {
		return paises.stream()
			.map(p -> gerar(p, continentais.stream().mapToInt(c -> c.clubesGerados(p.codigo())).sum()))
			.toList();
	}

	/** Gerador próprio: quantos estrangeiros sair não muda os treinadores sorteados depois (rev. 87). */
	private List<ClubeGerado> comEstrangeiros(String codigoPais, List<ClubeGerado> clubes, SplittableRandom aleatorio) {
		SplittableRandom paraEstrangeiros = new SplittableRandom(aleatorio.nextLong());
		Regiao regiao = regiaoDe(codigoPais);
		ParametrosBancoPadrao.FaixaQuantidade faixa = parametros.estrangeirosNoPlantel().get(regiao);
		List<OrigemEstrangeiro> origens = origens(codigoPais, regiao);
		return clubes.stream().map(c -> {
			int quantidade = paraEstrangeiros.nextInt(faixa.minima(), faixa.maxima() + 1);
			return new ClubeGerado(c.clube(), c.treinador(), comEstrangeiros(c.jogadores(), quantidade, origens, paraEstrangeiros));
		}).toList();
	}

	private List<Jogador> comEstrangeiros(List<Jogador> plantel, int quantidade, List<OrigemEstrangeiro> origens,
			SplittableRandom aleatorio) {
		List<Jogador> resultado = new ArrayList<>(plantel);
		Set<String> nomes = new HashSet<>(plantel.stream().map(Jogador::nome).toList());
		List<Integer> posicoes = new ArrayList<>(IntStream.range(0, plantel.size()).boxed().toList());
		for (int i = 0; i < quantidade; i++) {
			int posicao = posicoes.remove(aleatorio.nextInt(posicoes.size()));
			Jogador j = resultado.get(posicao);
			OrigemEstrangeiro origem = sortearOrigem(origens, aleatorio);
			resultado.set(posicao, new Jogador(j.id(), nomeDoJogador(origem.idioma(), nomes, aleatorio), j.posicao(), j.forca(),
					j.temperamento(), j.asterisco(), origem.codigo()));
		}
		return List.copyOf(resultado);
	}

	/** País de onde pode vir um estrangeiro, com o peso no sorteio. */
	private record OrigemEstrangeiro(String codigo, Idioma idioma, int peso) {
	}

	/** Os países do banco, menos o do clube; os da região dele pesam mais. A ordem é fixa, para a semente valer. */
	private List<OrigemEstrangeiro> origens(String codigoPais, Regiao regiao) {
		return Stream.concat(PaisPadrao.TODOS.stream().map(p -> Map.entry(p.codigo(), p.idioma())),
				PaisEstrangeiroPadrao.TODOS.stream().map(p -> Map.entry(p.codigo(), p.idioma())))
			.filter(p -> !p.getKey().equals(codigoPais))
			.map(p -> new OrigemEstrangeiro(p.getKey(), p.getValue(), regiaoDe(p.getKey()) == regiao ? parametros.pesoMesmaRegiao() : 1))
			.toList();
	}

	private static OrigemEstrangeiro sortearOrigem(List<OrigemEstrangeiro> origens, SplittableRandom aleatorio) {
		int sorteio = aleatorio.nextInt(origens.stream().mapToInt(OrigemEstrangeiro::peso).sum());
		for (OrigemEstrangeiro origem : origens) {
			sorteio -= origem.peso();
			if (sorteio < 0) {
				return origem;
			}
		}
		throw new IllegalStateException("pesos de origem inválidos");
	}

	private static Regiao regiaoDe(String codigoPais) {
		return ContinentalPadrao.TODAS.stream().filter(c -> c.vagas().containsKey(codigoPais)).map(ContinentalPadrao::regiao)
			.findFirst().orElseThrow(() -> new IllegalArgumentException("país sem região: " + codigoPais));
	}

	/** Gerador próprio: os treinadores (rev. 54) não mudam os clubes e planteis já gerados. */
	private static List<ClubeGerado> comTreinadores(Idioma idioma, List<ClubeGerado> clubes, SplittableRandom aleatorio) {
		SplittableRandom paraTreinadores = new SplittableRandom(~aleatorio.nextLong());
		Set<String> treinadores = new HashSet<>();
		return clubes.stream()
			.map(c -> new ClubeGerado(c.clube(), nomeDoTreinador(idioma, treinadores, paraTreinadores), c.jogadores()))
			.toList();
	}

	/** Do topo ao pé da faixa, em passos iguais, com ±1 de sorteio para não ficar uma escada perfeita. */
	private static int forcaNaFaixa(ParametrosBancoPadrao.FaixaForca faixa, int posicao, int quantidade,
			SplittableRandom aleatorio) {
		double passo = quantidade == 1 ? 0 : (double) (faixa.maxima() - faixa.minima()) / (quantidade - 1);
		int base = (int) Math.round(faixa.maxima() - passo * posicao);
		return limitar(base + aleatorio.nextInt(-1, 2), faixa.minima(), faixa.maxima());
	}

	private static String nomeDoClube(String pais, List<String> modelos, List<String> lugares, int necessarios,
			Set<String> usados, SplittableRandom aleatorio) {
		int combinacoes = modelos.size() * lugares.size();
		for (int tentativa = 0; tentativa < combinacoes * 4; tentativa++) {
			String nome = sortear(modelos, aleatorio).formatted(sortear(lugares, aleatorio));
			if (usados.add(nome)) {
				return nome;
			}
		}
		throw new IllegalStateException("faltam nomes de clube para " + pais + ": " + combinacoes + " combinações para "
				+ necessarios + " clubes");
	}

	private Escudo escudo(SplittableRandom aleatorio) {
		ModeloEscudo modelo = ModeloEscudo.values()[aleatorio.nextInt(ModeloEscudo.values().length)];
		String primaria = sortear(parametros.cores(), aleatorio);
		String secundaria;
		do {
			secundaria = sortear(parametros.cores(), aleatorio);
		} while (secundaria.equals(primaria));
		return new Escudo(modelo, primaria, secundaria);
	}

	/**
	 * Os 4-4-2 mais fortes nascem perto da força do clube e os demais abaixo; depois a força dos
	 * titulares que a máquina escolhe é ajustada, de 1 em 1, até a média bater com a do clube.
	 */
	/**
	 * Jogador criado na virada para o clube de fora do campeonato (rev. 84), com nome do país e sem asterisco.
	 *
	 * @param usados nomes já no plantel; recebe o novo
	 */
	public Jogador jogadorDeReposicao(String codigoPais, Posicao posicao, int forca, Set<String> usados, SplittableRandom aleatorio) {
		Idioma idioma = Stream.concat(PaisPadrao.TODOS.stream().filter(p -> p.codigo().equals(codigoPais)).map(PaisPadrao::idioma),
				PaisEstrangeiroPadrao.TODOS.stream().filter(p -> p.codigo().equals(codigoPais)).map(PaisEstrangeiroPadrao::idioma))
			.findFirst().orElseThrow(() -> new IllegalArgumentException("país sem idioma: " + codigoPais));
		return new Jogador(1, nomeDoJogador(idioma, usados, aleatorio), posicao, forca, temperamento(aleatorio), codigoPais);
	}

	private List<Jogador> plantel(Idioma idioma, String nacionalidade, int forcaClube, int teto,
			SplittableRandom aleatorio) {
		Map<Posicao, Integer> titulares = Map.of(Posicao.GOLEIRO, 1, Posicao.DEFENSOR, 4, Posicao.MEIO_CAMPISTA, 4,
				Posicao.ATACANTE, 2);
		Set<String> nomes = new HashSet<>();
		List<Jogador> jogadores = new ArrayList<>();
		for (Posicao posicao : Posicao.values()) {
			int total = parametros.composicao().getOrDefault(posicao, 0);
			for (int k = 0; k < total; k++) {
				int desvio = k < titulares.get(posicao)
						? aleatorio.nextInt(-parametros.variacaoTitulares(), parametros.variacaoTitulares() + 1)
						: -aleatorio.nextInt(parametros.quedaReservasMinima(), parametros.quedaReservasMaxima() + 1);
				jogadores.add(new Jogador(jogadores.size() + 1, nomeDoJogador(idioma, nomes, aleatorio), posicao,
						limitar(forcaClube + desvio, Jogador.FORCA_MINIMA, teto), temperamento(aleatorio), nacionalidade));
			}
		}
		jogadores = asteriscos(jogadores, forcaClube, aleatorio);
		return ajustarMedia(jogadores, forcaClube, teto);
	}

	private List<Jogador> asteriscos(List<Jogador> jogadores, int forcaClube, SplittableRandom aleatorio) {
		ParametrosBancoPadrao.FaixaForca ultima = parametros.faixas().getLast();
		double fracao = (double) (forcaClube - ultima.minima()) / (Jogador.FORCA_MAXIMA - ultima.minima());
		double chance = parametros.chanceAsteriscoMaisFraco()
				+ (parametros.chanceAsteriscoMaisForte() - parametros.chanceAsteriscoMaisFraco())
						* Math.clamp(fracao, 0.0, 1.0);
		List<Jogador> candidatos = new ArrayList<>(escalacao.escalar(jogadores).titulares());
		List<Jogador> resultado = new ArrayList<>(jogadores);
		for (int i = 0; i < parametros.maximoAsteriscos() && !candidatos.isEmpty(); i++) {
			if (aleatorio.nextDouble() < chance) {
				Jogador escolhido = candidatos.remove(aleatorio.nextInt(candidatos.size()));
				resultado.set(resultado.indexOf(escolhido), escolhido.comAsterisco(true));
			}
		}
		return resultado;
	}

	private List<Jogador> ajustarMedia(List<Jogador> jogadores, int forcaClube, int teto) {
		Map<Long, Jogador> porId = new LinkedHashMap<>();
		jogadores.forEach(j -> porId.put(j.id(), j));
		for (int passo = 0; passo < 200; passo++) {
			List<Jogador> emCampo = escalacao.escalar(List.copyOf(porId.values())).titulares();
			double diferenca = forcaClube - mediaForca(emCampo);
			if (Math.abs(diferenca) <= 0.5) {
				break;
			}
			Jogador alvo = diferenca > 0
					? emCampo.stream().filter(j -> j.forca() < teto).min(Comparator.comparingInt(Jogador::forca)).orElse(null)
					: emCampo.stream().filter(j -> j.forca() > Jogador.FORCA_MINIMA)
						.max(Comparator.comparingInt(Jogador::forca)).orElse(null);
			if (alvo == null) {
				break;
			}
			porId.put(alvo.id(), comForca(alvo, alvo.forca() + (diferenca > 0 ? 1 : -1)));
		}
		return List.copyOf(porId.values());
	}

	/** Média de força dos jogadores em campo: a força do clube na rev. 31. */
	public static double mediaForca(List<Jogador> emCampo) {
		return emCampo.stream().mapToInt(Jogador::forca).average().orElse(0);
	}

	private static Jogador comForca(Jogador j, int forca) {
		return new Jogador(j.id(), j.nome(), j.posicao(), forca, j.temperamento(), j.asterisco(), j.nacionalidade());
	}

	private String nomeDoJogador(Idioma idioma, Set<String> usados, SplittableRandom aleatorio) {
		for (int tentativa = 0; tentativa < 1000; tentativa++) {
			String nome = nomeQualquer(idioma, aleatorio);
			if (usados.add(nome)) {
				return nome;
			}
		}
		throw new IllegalStateException("não foi possível sortear nomes diferentes para o plantel em " + idioma);
	}

	/** Treinador com nome e sobrenome, diferente dos outros do país. */
	private static String nomeDoTreinador(Idioma idioma, Set<String> usados, SplittableRandom aleatorio) {
		for (int tentativa = 0; tentativa < 1000; tentativa++) {
			String nome = sortear(idioma.nomes(), aleatorio) + " " + sortear(idioma.sobrenomes(), aleatorio);
			if (usados.add(nome)) {
				return nome;
			}
		}
		throw new IllegalStateException("não foi possível sortear nomes diferentes de treinador em " + idioma);
	}

	private String nomeQualquer(Idioma idioma, SplittableRandom aleatorio) {
		int total = parametros.pesoNomeComposto() + parametros.pesoSoNome() + parametros.pesoSoSobrenome();
		int sorteio = aleatorio.nextInt(total);
		if (sorteio < parametros.pesoNomeComposto()) {
			return sortear(idioma.nomes(), aleatorio) + " " + sortear(idioma.sobrenomes(), aleatorio);
		}
		if (sorteio < parametros.pesoNomeComposto() + parametros.pesoSoNome()) {
			return sortear(idioma.nomes(), aleatorio);
		}
		return sortear(idioma.sobrenomes(), aleatorio);
	}

	private Temperamento temperamento(SplittableRandom aleatorio) {
		int total = parametros.pesosTemperamento().values().stream().mapToInt(Integer::intValue).sum();
		int sorteio = aleatorio.nextInt(total);
		// ordem fixa do enum, para o mapa imutável não mudar o resultado
		for (Temperamento t : Temperamento.values()) {
			sorteio -= parametros.pesosTemperamento().get(t);
			if (sorteio < 0) {
				return t;
			}
		}
		throw new IllegalStateException("pesos de temperamento inválidos");
	}

	private static <T> T sortear(List<T> lista, SplittableRandom aleatorio) {
		return lista.get(aleatorio.nextInt(lista.size()));
	}

	private static int limitar(int valor, int minimo, int maximo) {
		return Math.max(minimo, Math.min(maximo, valor));
	}
}
