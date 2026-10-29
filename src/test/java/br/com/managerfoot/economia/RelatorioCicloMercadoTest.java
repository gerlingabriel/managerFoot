package br.com.managerfoot.economia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.PaisPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.economia.ParametrosEconomia.FaixaSalario;
import br.com.managerfoot.economia.CaixaDaRodada.Local;
import br.com.managerfoot.economia.LeiloesDaRodada.ClubeNoMercado;
import br.com.managerfoot.economia.LeiloesDaRodada.Desfecho;
import br.com.managerfoot.economia.LeiloesDaRodada.Oferta;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Disciplina;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.EvolucaoForca;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.ProcessamentoRodada;
import br.com.managerfoot.temporada.ResultadoJogo;
import br.com.managerfoot.temporada.Rodada;
import br.com.managerfoot.temporada.RodadaAoVivo;
import br.com.managerfoot.temporada.Temporada;
import br.com.managerfoot.temporada.TransicaoTemporada;

/**
 * Gera {@code docs/simulacoes/ciclo-do-mercado.md} (rev. 59): várias temporadas das ligas do banco
 * padrão com a economia atual inteira (público pela posição, estádio e ampliação, evolução de força,
 * prêmio e patrocínio fixo) e os leilões da máquina antes de cada rodada, com as mesmas classes do
 * jogo ({@link LeiloesDaRodada}). Mede quem vende, quem compra e quem consegue dar lance, para ver se
 * os clubes ricos que caem vendem e se os de baixo têm dinheiro para comprar (objetivo do usuário,
 * rev. 58). Só máquina. Na rev. 65 compara cenários da inflação pela força do clube. Executar com
 * {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none -Dtest=RelatorioCicloMercadoTest}.
 */
@Tag("simulacao")
class RelatorioCicloMercadoTest {

	private static final int SEMENTES = 3;
	private static final int TEMPORADAS = 6;
	private static final Path SAIDA = Path.of("docs/simulacoes/ciclo-do-mercado.md");
	private static final String[] TERCOS = { "Topo", "Meio", "Fundo" };
	private static final String[] RIQUEZA = { "Rico", "Médio", "Pobre" };

	private static final List<FaixaSalario> FAIXAS_DO_USUARIO = ParametrosEconomia.padrao().faixasSalario();
	private static final List<FaixaSalario> FAIXAS_MAIORES = List.of(new FaixaSalario(0, 0.90), new FaixaSalario(15, 0.95),
			new FaixaSalario(25, 1.0), new FaixaSalario(35, 1.20), new FaixaSalario(45, 1.40));

	/** Cenários da inflação pela força (rev. 65): o primeiro é o jogo atual. */
	private static final List<Cenario> CENARIOS = List.of(
			new Cenario("Atual: faixa pelo jogador no salário e no preço", false, false, FAIXAS_DO_USUARIO),
			new Cenario("Preço pelo comprador; salário pelo jogador", true, false, FAIXAS_DO_USUARIO),
			new Cenario("Preço pelo comprador; salário pelo clube", true, true, FAIXAS_DO_USUARIO),
			new Cenario("Preço pelo comprador; salário pelo jogador; faixas maiores", true, false, FAIXAS_MAIORES),
			new Cenario("Preço pelo comprador; salário pelo clube; faixas maiores", true, true, FAIXAS_MAIORES));

	@Test
	void gerarRelatorio() throws IOException {
		GeradorBancoPadrao gerador = new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao());
		List<GeradorBancoPadrao.PaisGerado> gerados = PaisPadrao.TODOS.stream().map(gerador::gerar).toList();
		List<Simulacao> simulacoes = new ArrayList<>();
		for (Cenario cenario : CENARIOS) {
			Simulacao sim = new Simulacao(cenario);
			for (int p = 0; p < PaisPadrao.TODOS.size(); p++) {
				for (int semente = 0; semente < SEMENTES; semente++) {
					try {
						sim.simular(PaisPadrao.TODOS.get(p), gerados.get(p), semente);
					} catch (IllegalStateException e) {
						// um clube ficou sem 11 disponíveis: a rodada travaria no jogo
						sim.travadas++;
					}
				}
			}
			simulacoes.add(sim);
		}
		Files.createDirectories(SAIDA.getParent());
		String md = comparacao(simulacoes) + simulacoes.get(DETALHADO).relatorio();
		Files.writeString(SAIDA, md);
		System.out.println(md);
	}

	private static String comparacao(List<Simulacao> simulacoes) {
		StringBuilder md = new StringBuilder();
		md.append("# Ciclo do mercado: quem vende, quem compra\n\n")
			.append("Gerado por `RelatorioCicloMercadoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, ")
			.append(SEMENTES).append(" sementes, ").append(TEMPORADAS).append(" temporadas seguidas com promoção e rebaixamento. ")
			.append("Economia atual: público pela posição e pela força (§9.2), estádio com ampliação da máquina, evolução de ")
			.append("força (§6.1), salário da máquina pela força atual com as faixas da rev. 61, prêmio da liga e patrocínio fixo ")
			.append("pela posição. Antes de cada rodada, os leilões da máquina (`LeiloesDaRodada`, as regras da rev. 44): quem está ")
			.append("abaixo de 10 rodadas da folha põe o mais caro sem asterisco em leilão; dá lance quem fica acima do limite mais ")
			.append("5 rodadas da folha depois de pagar, tem menos de 25 jogadores e o jogador tem força de pelo menos a do time ")
			.append("menos 5. Só máquina.\n\n")
			.append("## Inflação pela força do clube (rev. 65)\n\n")
			.append("*Faixa*: o ajuste da rev. 61 (abaixo de 15 −10%, 15–24 −5%, 25–34 0, 35–44 +5%, 45–50 +10%; *faixas maiores*: ")
			.append("35–44 +20% e 45–50 +40%). *Preço pelo comprador*: o preço sai do salário sem faixa e leva a faixa da força ")
			.append("em campo do clube que compra; o lance inicial do leilão (preço −20%) fica sem faixa. *Salário pelo clube*: todos ")
			.append("os jogadores do clube recebem com a faixa da força em campo do clube. Prêmio e ampliação do estádio iguais em ")
			.append("todos. Saldo no fim da ").append(TEMPORADAS).append("ª temporada, em limites, por terço final da tabela.\n\n")
			.append("| Cenário | 1ª topo | 1ª fundo | 2ª topo | 2ª fundo | 1ª leilões sem lance | 2ª leilões sem lance ")
			.append("| Vendas do rico no fundo (por 100 clubes-rodada) | Mobilidade | Ligas travadas |\n|---|---|---|---|---|---|---|---|---|---|\n");
		for (int i = 0; i < CENARIOS.size(); i++) {
			Simulacao s = simulacoes.get(i);
			Medida d1 = s.temporadas.get(1).get(TEMPORADAS);
			Medida d2 = s.temporadas.get(2).get(TEMPORADAS);
			md.append("| ").append(CENARIOS.get(i).nome()).append(" | ").append(dec(d1.limites[0] / d1.n[0])).append(" | ")
				.append(dec(d1.limites[2] / d1.n[2])).append(" | ").append(dec(d2.limites[0] / d2.n[0])).append(" | ")
				.append(dec(d2.limites[2] / d2.n[2])).append(" | ").append(pct(s.semLance[0] / (double) Math.max(1, s.ofertas[0])))
				.append(" | ").append(pct(s.semLance[1] / (double) Math.max(1, s.ofertas[1])))
				.append(" | ").append(dec(100.0 * s.vendedor[0][2] / Math.max(1, s.clubesRodada[0][2]))).append(" | ")
				.append(pct(s.mudaramDeTerco / (double) Math.max(1, s.clubesNaMobilidade))).append(" | ").append(s.travadas)
				.append(" de ").append(PaisPadrao.TODOS.size() * SEMENTES).append(" |\n");
		}
		md.append("\n*Ligas travadas*: liga × semente em que um clube ficou sem 11 disponíveis (vendas até o plantel mínimo de 14 ")
			.append("mais suspensões); a simulação dessa liga para ali e as medidas contam só as temporadas jogadas.\n\n")
			.append("As tabelas abaixo são do cenário \"").append(CENARIOS.get(DETALHADO).nome()).append("\".\n\n");
		return md.toString();
	}

	/** Cenário com as tabelas completas no relatório. */
	private static final int DETALHADO = 2;

	/**
	 * @param precoPeloComprador a faixa entra no preço pela força em campo do comprador
	 * @param salarioPeloClube a faixa do salário segue a força em campo do clube
	 */
	private record Cenario(String nome, boolean precoPeloComprador, boolean salarioPeloClube, List<FaixaSalario> faixas) {
	}

	/** Acumuladores e regras de um cenário. */
	private static final class Simulacao {

		private final CaixaDaRodada caixa;
		private final ParametrosPublico parametros = ParametrosPublico.padrao();
		private final PublicoDoJogo publico = PublicoDoJogo.padrao();
		private final AmpliacaoEstadio ampliacao = AmpliacaoEstadio.padrao();
		private final Premiacao premiacao = Premiacao.padrao();
		private final EvolucaoForca evolucao = EvolucaoForca.padrao();
		private final PrecoJogador preco;
		private final MercadoDaMaquina mercado;
		private final LeiloesDaRodada leiloes;

		/** Vendas por divisão do vendedor (linha) e do comprador (coluna). */
		private final long[][] porDivisao = new long[3][3];
		/** Vendas dentro da mesma divisão, por terço da tabela do vendedor e do comprador. */
		private final long[][] porTerco = new long[3][3];
		/** Vendas por riqueza do vendedor no início da temporada e terço da tabela na hora da venda. */
		private final long[][] vendedor = new long[3][3];
		/** Clubes-rodada por riqueza e terço, para a taxa de venda. */
		private final long[][] clubesRodada = new long[3][3];
		/** Por divisão e terço da tabela: lances dados e clubes-oferta (quem podia dar lance). */
		private final long[][] lances = new long[3][3];
		private final long[][] candidatos = new long[3][3];
		private final long[] ofertas = new long[3];
		private final long[] semLance = new long[3];
		private final long[] vendidos = new long[3];
		private final double[] pagoSobrePreco = new double[3];
		/** Por divisão: soma da força do jogador vendido menos a força em campo do comprador. */
		private final double[] diferencaForca = new double[3];
		/** Por divisão e temporada: saldo no fim em limites por terço, negativos e trocas de jogador. */
		private final Map<Integer, Map<Integer, Medida>> temporadas = new java.util.TreeMap<>();
		private long clubesNaMobilidade;
		private long mudaramDeTerco;
		private int travadas;

		private Simulacao(Cenario cenario) {
			caixa = new CaixaDaRodada(ParametrosEconomia.padrao().comFaixas(cenario.faixas(), cenario.salarioPeloClube()),
					ParametrosMoral.padrao());
			preco = new PrecoJogador(ParametrosMercado.padrao().comPrecoPeloComprador(cenario.precoPeloComprador()), caixa);
			mercado = new MercadoDaMaquina(ParametrosMercadoMaquina.padrao(), preco, caixa);
			leiloes = new LeiloesDaRodada(mercado, preco);
		}

		private String relatorio() {
			StringBuilder md = new StringBuilder();
			md.append("*Riqueza*: terço do clube na divisão pelo saldo dividido pelo limite, no início da temporada. *Terço da ")
				.append("tabela*: posição na hora da venda.\n\n");

			md.append("## Quem vende: riqueza no início da temporada × posição na hora (vendas por 100 clubes-rodada)\n\n")
				.append("| Riqueza | Topo | Meio | Fundo |\n|---|---|---|---|\n");
			for (int r = 0; r < 3; r++) {
				md.append("| ").append(RIQUEZA[r]);
				for (int t = 0; t < 3; t++) {
					md.append(" | ").append(dec(100.0 * vendedor[r][t] / Math.max(1, clubesRodada[r][t])));
				}
				md.append(" |\n");
			}

			md.append("\n## Para onde vão os jogadores: divisão do vendedor × do comprador (% das vendas)\n\n")
				.append("| Vendedor \\ comprador | 1ª | 2ª | 3ª |\n|---|---|---|---|\n");
			long total = 0;
			for (long[] l : porDivisao) {
				for (long v : l) {
					total += v;
				}
			}
			for (int a = 0; a < 3; a++) {
				md.append("| ").append(a + 1).append("ª");
				for (int b = 0; b < 3; b++) {
					md.append(" | ").append(pct(porDivisao[a][b] / (double) Math.max(1, total)));
				}
				md.append(" |\n");
			}

			md.append("\n## Na mesma divisão: terço do vendedor × do comprador (% das vendas na mesma divisão)\n\n")
				.append("| Vendedor \\ comprador | Topo | Meio | Fundo |\n|---|---|---|---|\n");
			long mesma = 0;
			for (long[] l : porTerco) {
				for (long v : l) {
					mesma += v;
				}
			}
			for (int a = 0; a < 3; a++) {
				md.append("| ").append(TERCOS[a]);
				for (int b = 0; b < 3; b++) {
					md.append(" | ").append(pct(porTerco[a][b] / (double) Math.max(1, mesma)));
				}
				md.append(" |\n");
			}

			md.append("\n## Quem consegue comprar: clubes que dão lance por leilão aberto, por terço da tabela\n\n")
				.append("% dos clubes do terço que deram lance em cada leilão do país (todos os clubes da máquina podem dar lance em ")
				.append("qualquer leilão do país).\n\n")
				.append("| Divisão | Leilões | Sem lance | Topo | Meio | Fundo | Pago / preço | Força do vendido − força do comprador |\n")
				.append("|---|---|---|---|---|---|---|---|\n");
			for (int d = 0; d < 3; d++) {
				if (ofertas[d] == 0) {
					continue;
				}
				md.append("| ").append(d + 1).append("ª | ").append(ofertas[d]).append(" | ").append(pct(semLance[d] / (double) ofertas[d]));
				for (int t = 0; t < 3; t++) {
					md.append(" | ").append(pct(lances[d][t] / (double) Math.max(1, candidatos[d][t])));
				}
				md.append(" | ").append(pct(pagoSobrePreco[d] / Math.max(1, vendidos[d]))).append(" | ")
					.append(dec(diferencaForca[d] / Math.max(1, vendidos[d]))).append(" |\n");
			}
			md.append("\n*Leilões*: pela divisão do vendedor. *Topo/Meio/Fundo*: pela divisão de quem dá o lance.\n");

			md.append("\n## Caixa por temporada: saldo no fim em limites (1,0 = 10 rodadas da folha) por terço final\n\n")
				.append("| Divisão | Temporada | Topo | Meio | Fundo | Clubes negativos | Jogadores trocados por clube |\n")
				.append("|---|---|---|---|---|---|---|\n");
			temporadas.forEach((d, porTemporada) -> porTemporada.forEach((t, m) -> md.append("| ").append(d).append("ª | ")
				.append(t).append(" | ").append(dec(m.limites[0] / m.n[0])).append(" | ").append(dec(m.limites[1] / m.n[1]))
				.append(" | ").append(dec(m.limites[2] / m.n[2])).append(" | ")
				.append(pct(m.negativos / (m.n[0] + m.n[1] + m.n[2]))).append(" | ")
				.append(dec(m.trocas / (m.n[0] + m.n[1] + m.n[2]))).append(" |\n")));

			md.append("\n## Mobilidade\n\n")
				.append("Clubes que mudaram de terço de força em campo no país entre o início e o fim das ").append(TEMPORADAS)
				.append(" temporadas: ").append(pct(mudaramDeTerco / (double) Math.max(1, clubesNaMobilidade))).append(".\n");
			return md.toString();
		}

		private void simular(PaisPadrao pais, GeradorBancoPadrao.PaisGerado gerado, long semente) {
			String paisDaLiga = pais.codigo();
			Map<Long, List<Jogador>> planteis = new HashMap<>();
			List<Clube> clubes = new ArrayList<>();
			for (GeradorBancoPadrao.ClubeGerado c : gerado.clubes()) {
				clubes.add(c.clube());
				planteis.put(c.clube().id(), c.jogadores().stream()
					.map(j -> new Jogador(c.clube().id() * 100 + j.id(), j.nome(), j.posicao(), j.forca(), j.temperamento(),
							j.asterisco(), j.nacionalidade()))
					.toList());
			}
			Map<Long, Integer> tercoInicial = tercosDeForca(clubes, planteis);
			ConfiguracaoLiga liga = pais.formato().distribuir(clubes, new SplittableRandom(semente));
			Map<Long, Integer> estadio = new HashMap<>();
			Map<Long, Long> saldo = new HashMap<>();
			Map<Long, int[]> obra = new HashMap<>();
			clubes.forEach(c -> {
				estadio.put(c.id(), publico.estadioInicial(c.forca()));
				saldo.put(c.id(), caixa.saldoInicial(planteis.get(c.id())));
			});
			Map<Long, int[]> posicaoAnterior = new HashMap<>();
			ProcessamentoRodada processamento = ProcessamentoRodada.padrao();
			SplittableRandom aleatorio = new SplittableRandom(semente * 131 + 7);
			Disciplina disciplina = Disciplina.vazia();
			Map<Long, Moral> moral = new HashMap<>();
			for (int numero = 1; numero <= TEMPORADAS; numero++) {
				Temporada temporada = Temporada.iniciar(numero, liga, planteis, aleatorio.nextLong());
				Map<Long, Integer> teto = new HashMap<>();
				Map<Long, Divisao> divisaoDe = new HashMap<>();
				Map<Long, Clube> clubePorId = new HashMap<>();
				Map<Long, Integer> trocas = new HashMap<>();
				for (Divisao d : liga.divisoes()) {
					for (Clube c : d.clubes()) {
						teto.put(c.id(), d.limiteForca());
						divisaoDe.put(c.id(), d);
						clubePorId.put(c.id(), c);
						int[] anterior = posicaoAnterior.get(c.id());
						saldo.merge(c.id(), anterior == null ? premiacao.patrocinioInicial(d.clubes().size(), d.limiteForca())
								: premiacao.patrocinio(anterior[0], anterior[1], anterior[2]), Long::sum);
					}
				}
				Map<Long, Integer> riqueza = riqueza(liga, saldo, planteis);
				Map<Long, OptionalDouble> anterior = new HashMap<>();
				divisaoDe.keySet().forEach(id -> {
					int[] a = posicaoAnterior.get(id);
					anterior.put(id, a == null ? OptionalDouble.empty() : OptionalDouble.of(PublicoDoJogo.relativa(a[0], a[1])));
				});
				Map<Integer, List<ResultadoJogo>> resultados = new HashMap<>();
				int rodadas = temporada.divisoes().stream().mapToInt(d -> d.calendario().rodadas().size()).max().orElse(0);
				for (int r = 1; r <= rodadas; r++) {
					Map<Long, Integer> posicao = new HashMap<>();
					for (Divisao d : liga.divisoes()) {
						Classificacao c = Classificacao.calcular(d.clubes(), resultados.getOrDefault(d.prioridade(), List.of()),
								temporada.semente());
						d.clubes().forEach(clube -> posicao.put(clube.id(), c.posicao(clube)));
					}
					leiloes(paisDaLiga, liga, divisaoDe, clubePorId, posicao, riqueza, planteis, saldo, trocas, aleatorio);
					for (Temporada.SorteiosDivisao sd : temporada.divisoes()) {
						Divisao d = divisaoDe.get(sd.calendario().jogos().getFirst().mandante().id());
						int total = sd.calendario().rodadas().size();
						for (Rodada rodada : sd.calendario().rodadas()) {
							if (rodada.numero() != r) {
								continue;
							}
							for (Jogo jogo : rodada.jogos()) {
								long m = jogo.mandante().id();
								long v = jogo.visitante().id();
								int pagantes = publico.publico(estadio.get(m), emCampo(planteis.get(m)), posicao.get(m), posicao.get(v),
										d.clubes().size(), r, total, anterior.get(m), anterior.get(v), aleatorio);
								lancar(saldo, m, caixa.lancamentos(Local.CASA, d.prioridade(), pagantes, planteis.get(m)));
								lancar(saldo, v, caixa.lancamentos(Local.FORA, d.prioridade(), 0, planteis.get(v)));
							}
							rodada.folga().ifPresent(c -> lancar(saldo, c.id(),
									caixa.lancamentos(Local.FOLGA, d.prioridade(), 0, planteis.get(c.id()))));
						}
					}
					RodadaAoVivo aoVivo = processamento.preparar(temporada, r, planteis, disciplina, Map.of(), List.of());
					ProcessamentoRodada.Resultado res = aoVivo.finalizar(moral);
					for (int i = 0; i < res.partidas().size(); i++) {
						RodadaAoVivo.PartidaDaRodada partida = aoVivo.partidas().get(i);
						ProcessamentoRodada.PartidaJogada jogada = res.partidas().get(i);
						resultados.computeIfAbsent(jogada.prioridade(), k -> new ArrayList<>())
							.add(new ResultadoJogo(jogada.jogo(), jogada.resultado().golsCasa(), jogada.resultado().golsFora()));
						for (Lado lado : Lado.values()) {
							Clube c = partida.clube(lado);
							planteis.put(c.id(), evolucao.aposPartida(planteis.get(c.id()), lado == Lado.CASA ? partida.casa() : partida.fora(),
									jogada.resultado(), lado, teto.get(c.id()), c.forca(), aleatorio));
						}
					}
					disciplina = res.disciplina();
					moral = new HashMap<>(res.moral());
					for (Long id : divisaoDe.keySet()) {
						int[] o = obra.get(id);
						if (o != null && --o[0] <= 0) {
							estadio.merge(id, parametros.bloco(), Integer::sum);
							obra.remove(id);
						}
						if (obra.get(id) == null && ampliacao.maquinaAmplia(estadio.get(id), emCampo(planteis.get(id)), saldo.get(id),
								planteis.get(id))) {
							saldo.merge(id, -ampliacao.custo(), Long::sum);
							obra.put(id, new int[] { parametros.rodadasDeObra() });
						}
					}
				}
				Map<Integer, Classificacao> finais = new HashMap<>();
				for (Divisao d : liga.divisoes()) {
					Classificacao c = Classificacao.calcular(d.clubes(), resultados.get(d.prioridade()), temporada.semente());
					finais.put(d.prioridade(), c);
					Medida m = temporadas.computeIfAbsent(d.prioridade(), k -> new java.util.TreeMap<>())
						.computeIfAbsent(numero, k -> new Medida());
					for (Clube clube : d.clubes()) {
						int pos = c.posicao(clube);
						saldo.merge(clube.id(), premiacao.premio(pos, d.limiteForca()), Long::sum);
						posicaoAnterior.put(clube.id(), new int[] { pos, d.clubes().size(), d.limiteForca() });
						int t = Math.min(2, (pos - 1) * 3 / d.clubes().size());
						m.n[t]++;
						m.limites[t] += saldo.get(clube.id()) / (double) mercado.limite(planteis.get(clube.id()));
						m.negativos += saldo.get(clube.id()) < 0 ? 1 : 0;
						m.trocas += trocas.getOrDefault(clube.id(), 0);
					}
				}
				liga = TransicaoTemporada.aplicar(liga, finais, aleatorio).proxima();
				disciplina = disciplina.novaTemporada();
			}
			Map<Long, Integer> tercoFinal = tercosDeForca(clubes, planteis);
			clubesNaMobilidade += clubes.size();
			mudaramDeTerco += clubes.stream().filter(c -> !tercoInicial.get(c.id()).equals(tercoFinal.get(c.id()))).count();
		}

		/** Leilões da máquina antes da rodada, como no jogo; aplica as vendas e mede. */
		private void leiloes(String paisDaLiga, ConfiguracaoLiga liga, Map<Long, Divisao> divisaoDe, Map<Long, Clube> clubePorId,
				Map<Long, Integer> posicao, Map<Long, Integer> riqueza, Map<Long, List<Jogador>> planteis, Map<Long, Long> saldo,
				Map<Long, Integer> trocas, SplittableRandom aleatorio) {
			List<ClubeNoMercado> daMaquina = new ArrayList<>();
			for (Long id : divisaoDe.keySet().stream().sorted().toList()) {
				daMaquina.add(new ClubeNoMercado(clubePorId.get(id), saldo.get(id), planteis.get(id)));
				int t = terco(id, posicao, divisaoDe);
				clubesRodada[riqueza.get(id)][t]++;
			}
			List<Oferta> abertas = leiloes.abrir(daMaquina, paisDaLiga);
			List<Desfecho> desfechos = leiloes.resolver(abertas, daMaquina, List.of(), Map.of(), paisDaLiga, aleatorio);
			for (Desfecho d : desfechos) {
				long vend = d.oferta().vendedorId();
				int dv = divisaoDe.get(vend).prioridade() - 1;
				ofertas[dv]++;
				// quem podia dar lance: todos os outros clubes da máquina do país
				for (Long id : divisaoDe.keySet()) {
					if (id != vend) {
						candidatos[divisaoDe.get(id).prioridade() - 1][terco(id, posicao, divisaoDe)]++;
					}
				}
				d.lances().forEach(l -> lances[divisaoDe.get(l.clubeId()).prioridade() - 1][terco(l.clubeId(), posicao, divisaoDe)]++);
				if (d.vencedor().isEmpty()) {
					semLance[dv]++;
					continue;
				}
				long comp = d.vencedor().get().clubeId();
				vender(d.oferta().jogador(), vend, comp, d.vencedor().get().valor(), paisDaLiga, divisaoDe, posicao, riqueza,
						planteis, saldo, trocas);
			}
		}

		/** Aplica uma venda e mede. */
		private void vender(Jogador jogador, long vend, long comp, long valor, String paisDaLiga, Map<Long, Divisao> divisaoDe,
				Map<Long, Integer> posicao, Map<Long, Integer> riqueza, Map<Long, List<Jogador>> planteis, Map<Long, Long> saldo,
				Map<Long, Integer> trocas) {
			int dv = divisaoDe.get(vend).prioridade() - 1;
			int dc = divisaoDe.get(comp).prioridade() - 1;
			diferencaForca[dv] += jogador.forca() - emCampo(planteis.get(comp));
			List<Jogador> doVendedor = new ArrayList<>(planteis.get(vend));
			doVendedor.removeIf(j -> j.id() == jogador.id());
			planteis.put(vend, doVendedor);
			List<Jogador> doComprador = new ArrayList<>(planteis.get(comp));
			doComprador.add(jogador);
			planteis.put(comp, doComprador);
			saldo.merge(vend, valor, Long::sum);
			saldo.merge(comp, -valor, Long::sum);
			trocas.merge(vend, 1, Integer::sum);
			trocas.merge(comp, 1, Integer::sum);
			vendidos[dv]++;
			pagoSobrePreco[dv] += valor / (double) preco.preco(jogador, paisDaLiga);
			porDivisao[dv][dc]++;
			int tv = terco(vend, posicao, divisaoDe);
			if (dv == dc) {
				porTerco[tv][terco(comp, posicao, divisaoDe)]++;
			}
			vendedor[riqueza.get(vend)][tv]++;
		}

		private static int terco(long id, Map<Long, Integer> posicao, Map<Long, Divisao> divisaoDe) {
			return Math.min(2, (posicao.get(id) - 1) * 3 / divisaoDe.get(id).clubes().size());
		}

		/** Terço de riqueza na divisão pelo saldo em limites: 0 rico, 2 pobre. */
		private Map<Long, Integer> riqueza(ConfiguracaoLiga liga, Map<Long, Long> saldo, Map<Long, List<Jogador>> planteis) {
			Map<Long, Integer> riqueza = new HashMap<>();
			for (Divisao d : liga.divisoes()) {
				List<Clube> ordem = d.clubes().stream()
					.sorted(Comparator.comparingDouble(
							(Clube c) -> saldo.get(c.id()) / (double) mercado.limite(planteis.get(c.id()))).reversed())
					.toList();
				for (int i = 0; i < ordem.size(); i++) {
					riqueza.put(ordem.get(i).id(), Math.min(2, i * 3 / ordem.size()));
				}
			}
			return riqueza;
		}

		/** Terço de força em campo no país: 0 os mais fortes. */
		private static Map<Long, Integer> tercosDeForca(List<Clube> clubes, Map<Long, List<Jogador>> planteis) {
			List<Clube> ordem = clubes.stream()
				.sorted(Comparator.comparingDouble((Clube c) -> emCampo(planteis.get(c.id()))).reversed())
				.toList();
			Map<Long, Integer> tercos = new HashMap<>();
			for (int i = 0; i < ordem.size(); i++) {
				tercos.put(ordem.get(i).id(), Math.min(2, i * 3 / ordem.size()));
			}
			return tercos;
		}
	}

	private static void lancar(Map<Long, Long> saldo, long clube, List<Lancamento> lancamentos) {
		lancamentos.forEach(l -> saldo.merge(clube, l.valor(), Long::sum));
	}

	private static double emCampo(List<Jogador> plantel) {
		return MercadoDaMaquina.forcaEmCampo(plantel);
	}

	private static String pct(double v) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%.0f%%", v * 100);
	}

	private static String dec(double v) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", v);
	}

	private static final class Medida {
		final double[] n = new double[3];
		final double[] limites = new double[3];
		double negativos;
		double trocas;
	}
}
