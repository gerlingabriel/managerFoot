package br.com.managerfoot.economia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.PaisPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.economia.CaixaDaRodada.Local;
import br.com.managerfoot.economia.Leilao.Lance;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Disciplina;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.ProcessamentoRodada;
import br.com.managerfoot.temporada.Rodada;
import br.com.managerfoot.temporada.Temporada;

/**
 * Gera {@code docs/simulacoes/mercado-maquina.md}: uma temporada das ligas do banco padrão com o caixa
 * da rodada e o mercado da máquina (SPEC §9.3 e §9.4, rev. 43) depois de cada rodada. Mede se quem
 * perde vende, quem tem caixa compra e se há compra e venda em sequência. Executar com
 * {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none}.
 */
@Tag("simulacao")
class RelatorioMercadoTest {

	private static final int SEMENTES = 10;
	/** Compra desfeita: o clube vende alguém até este número de rodadas depois de comprar. */
	private static final int RODADAS_VAIVEM = 3;
	private static final Path SAIDA = Path.of("docs/simulacoes/mercado-maquina.md");

	private final CaixaDaRodada caixa = CaixaDaRodada.padrao();
	private final PrecoJogador preco = PrecoJogador.padrao();
	private final MercadoDaMaquina mercado = MercadoDaMaquina.padrao();
	private final ParametrosMercadoMaquina parametros = ParametrosMercadoMaquina.padrao();
	private final ParametrosMoral parametrosMoral = ParametrosMoral.padrao();
	private final Premiacao premiacao = Premiacao.padrao();

	private final Map<Integer, Acumulado> porDivisao = new TreeMap<>();
	private final Geral geral = new Geral();
	/** Bilheteria da rodada por clube, somada ao acumulado da divisão. */
	private final Map<Long, Long> receita = new HashMap<>();

	@Test
	void gerarRelatorio() throws IOException {
		GeradorBancoPadrao gerador = new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao());
		for (PaisPadrao pais : PaisPadrao.TODOS) {
			GeradorBancoPadrao.PaisGerado gerado = gerador.gerar(pais);
			for (int semente = 0; semente < SEMENTES; semente++) {
				simular(pais, gerado, semente);
			}
		}

		StringBuilder md = new StringBuilder();
		md.append("# Simulação do mercado da máquina\n\n");
		md.append("Gerado por `RelatorioMercadoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, ")
			.append(SEMENTES).append(" temporadas cada, começando do zero, só com clubes da máquina. Depois de cada ")
			.append("rodada entra o caixa (bilheteria, despesas, salários) e então o mercado:\n\n")
			.append("- clube com saldo abaixo de **").append(parametros.rodadasDeFolhaNoLimite())
			.append(" rodadas da folha** põe em leilão o jogador mais caro sem asterisco que não deixa a posição abaixo ")
			.append("do elenco mínimo (1 por rodada); com saldo negativo, o leilão é **sem lance mínimo**;\n")
			.append("- todos os outros clubes do país dão lance se, pago o lance, o saldo continuar acima do limite ")
			.append("mais ").append(parametros.folgaNaCompra()).append(" rodadas da folha (já com o novo salário), o plantel tiver menos de ").append(Clube.MAXIMO_JOGADORES).append(" (1 compra por ")
			.append("rodada) e o jogador tiver força de pelo menos a força em campo do clube menos ")
			.append(parametros.interesseAbaixoDoTime()).append("; o lance é ").append(pct(parametros.descontoLance())).append(" abaixo do preço, mais até ")
			.append(pct(parametros.acrescimoPelaForca())).append(" pela força do clube;\n")
			.append("- leva o maior lance; empate por sorteio; sem lance, o jogador fica.\n\n")
			.append("O patrocínio da primeira temporada (igual para todos da divisão) entra no saldo inicial. Sem ")
			.append("premiação (paga no fim) nem compra direta de usuário. Preço: `ParametrosMercado.padrao()`.\n\n");
		md.append("## Premiação da liga e patrocínio (rev. 45)\n\n");
		md.append("Prêmio do campeão = preço de 2 jogadores com asterisco do **teto de força da divisão**; 2º = 70%; ")
			.append("3º = 10%; patrocínio do 1º colocado = 10% do prêmio do campeão, o 2º 10% menos e cada posição ")
			.append("seguinte 8% menos que a anterior; na primeira temporada, a média para todos.\n\n");
		md.append("| Divisão | Teto de força | Campeão | 2º | 3º | Patrocínio no 1º ano | Bilheteria média do clube na temporada | Campeão / bilheteria |\n");
		md.append("|---|---|---|---|---|---|---|---|\n");
		porDivisao.forEach((prioridade, a) -> {
			double clubes = a.clubes[0] + a.clubes[1] + a.clubes[2];
			double bilheteria = a.receitaPorClube / clubes;
			md.append("| ").append(prioridade).append("ª | ").append(decimal(a.referencia / a.temporadas)).append(" | ")
				.append(real(a.campeao / a.temporadas)).append(" | ").append(real(a.segundo / a.temporadas)).append(" | ")
				.append(real(a.terceiro / a.temporadas)).append(" | ").append(real(a.patrocinio / a.temporadas))
				.append(" | ").append(real(bilheteria)).append(" | ").append(pct(a.campeao / a.temporadas / bilheteria))
				.append(" |\n");
		});
		md.append("\n");
		md.append("## Por posição final na tabela (média por clube e temporada)\n\n");
		md.append("| Divisão | Terço da tabela | Vendas | Compras | Força em campo: início → fim | Saldo final (em limites) |\n");
		md.append("|---|---|---|---|---|---|\n");
		String[] nomes = { "Topo", "Meio", "Fundo" };
		porDivisao.forEach((prioridade, a) -> {
			for (int t = 0; t < 3; t++) {
				double n = a.clubes[t];
				md.append("| ").append(prioridade).append("ª | ").append(nomes[t]).append(" | ").append(decimal(a.vendas[t] / n))
					.append(" | ").append(decimal(a.compras[t] / n)).append(" | ").append(decimal(a.forcaInicio[t] / n))
					.append(" → ").append(decimal(a.forcaFim[t] / n)).append(" | ").append(decimal(a.saldoEmLimites[t] / n))
					.append(" |\n");
			}
		});
		md.append("\n*Força em campo*: média dos 11 jogadores mais fortes. *Saldo final em limites*: saldo dividido pelo ")
			.append("limite (1,0 = exatamente ").append(parametros.rodadasDeFolhaNoLimite()).append(" rodadas da folha).\n\n");
		md.append("## Leilões\n\n");
		md.append("| Medida | Valor |\n|---|---|\n");
		md.append("| Leilões por clube e temporada | ").append(decimal(geral.leiloes / (double) geral.clubes)).append(" |\n");
		md.append("| Sem lance (jogador fica) | ").append(pct(geral.semLance / (double) geral.leiloes)).append(" |\n");
		md.append("| Sem lance mínimo (saldo negativo) | ").append(pct(geral.semMinimo / (double) geral.leiloes)).append(" |\n");
		md.append("| Valor pago / preço da tabela | ").append(pct(geral.pago / geral.tabela)).append(" |\n");
		md.append("| Clubes que ficaram negativos em algum momento | ").append(pct(geral.negativos / (double) geral.clubes))
			.append(" |\n");
		md.append("| Compras seguidas de venda em até ").append(RODADAS_VAIVEM).append(" rodadas | ")
			.append(pct(geral.vaivem / (double) Math.max(1, geral.vendidos))).append(" |\n");
		md.append("| Vendas nas ").append(RODADAS_VAIVEM).append(" primeiras rodadas | ")
			.append(pct(geral.vendasNoInicio / (double) Math.max(1, geral.vendidos))).append(" |\n");
		md.append("| Clubes que nunca venderam | ").append(pct(geral.nuncaVenderam / (double) geral.clubes)).append(" |\n");
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private void simular(PaisPadrao pais, GeradorBancoPadrao.PaisGerado gerado, long semente) {
		String paisDaLiga = pais.codigo();
		Map<Long, List<Jogador>> planteis = new HashMap<>();
		Map<Long, Clube> clubesPorId = new HashMap<>();
		List<Clube> clubes = new ArrayList<>();
		for (GeradorBancoPadrao.ClubeGerado c : gerado.clubes()) {
			clubes.add(c.clube());
			clubesPorId.put(c.clube().id(), c.clube());
			planteis.put(c.clube().id(), new ArrayList<>(c.jogadores().stream()
				.map(j -> new Jogador(c.clube().id() * 100 + j.id(), j.nome(), j.posicao(), j.forca(), j.temperamento(),
						j.asterisco(), j.nacionalidade()))
				.toList()));
		}
		ConfiguracaoLiga liga = pais.formato().distribuir(clubes, new SplittableRandom(semente));
		Temporada temporada = Temporada.iniciar(1, liga, planteis, semente);
		ProcessamentoRodada processamento = ProcessamentoRodada.padrao();
		SplittableRandom aleatorio = new SplittableRandom(semente * 7 + 1);
		Map<Long, Integer> capacidade = new HashMap<>();
		Map<Long, Long> saldo = new HashMap<>();
		Map<Long, Double> forcaInicio = new HashMap<>();
		Map<Long, Integer> pontos = new HashMap<>();
		Map<Long, Integer> vendas = new HashMap<>();
		Map<Long, Integer> compras = new HashMap<>();
		Map<Long, Integer> ultimaCompra = new HashMap<>();
		Set<Long> negativos = new HashSet<>();
		Set<Long> naLiga = new HashSet<>();
		liga.divisoes().forEach(d -> d.clubes().forEach(c -> naLiga.add(c.id())));
		Map<Long, Integer> referencia = new HashMap<>();
		for (Divisao divisao : liga.divisoes()) {
			int forca = divisao.limiteForca();
			Acumulado a = porDivisao.computeIfAbsent(divisao.prioridade(), k -> new Acumulado());
			a.temporadas++;
			a.referencia += forca;
			a.campeao += premiacao.premio(1, forca);
			a.segundo += premiacao.premio(2, forca);
			a.terceiro += premiacao.premio(3, forca);
			a.patrocinio += premiacao.patrocinioInicial(divisao.clubes().size(), forca);
			divisao.clubes().forEach(c -> referencia.put(c.id(), forca));
			for (Clube c : divisao.clubes()) {
				// patrocínio da primeira temporada, igual para todos da divisão, no início
				capacidade.put(c.id(), caixa.capacidadeInicial(c.forca()));
				saldo.put(c.id(), caixa.saldoInicial(planteis.get(c.id()))
						+ premiacao.patrocinioInicial(divisao.clubes().size(), forca));
				forcaInicio.put(c.id(), emCampo(planteis.get(c.id())));
			}
		}
		Disciplina disciplina = Disciplina.vazia();
		Map<Long, Moral> moral = new HashMap<>();
		receita.clear();
		int rodadas = temporada.divisoes().stream().mapToInt(d -> d.calendario().rodadas().size()).max().orElse(0);
		for (int numero = 1; numero <= rodadas; numero++) {
			ProcessamentoRodada.Resultado r = processamento.jogar(temporada, numero, Map.copyOf(planteis), disciplina, moral);
			for (ProcessamentoRodada.PartidaJogada p : r.partidas()) {
				int gc = p.resultado().golsCasa();
				int gf = p.resultado().golsFora();
				pontos.merge(p.jogo().mandante().id(), gc > gf ? 3 : gc == gf ? 1 : 0, Integer::sum);
				pontos.merge(p.jogo().visitante().id(), gf > gc ? 3 : gc == gf ? 1 : 0, Integer::sum);
			}
			for (Temporada.SorteiosDivisao d : temporada.divisoes()) {
				for (Rodada rodada : d.calendario().rodadas()) {
					if (rodada.numero() != numero) {
						continue;
					}
					for (Jogo jogo : rodada.jogos()) {
						Clube casa = jogo.mandante();
						int publico = caixa.publico(capacidade.get(casa.id()), moral.getOrDefault(casa.id(),
								Moral.inicial(parametrosMoral)), casa.forca(), jogo.visitante().forca(), aleatorio);
						lancar(casa, Local.CASA, d.prioridade(), publico, planteis, saldo);
						lancar(jogo.visitante(), Local.FORA, d.prioridade(), 0, planteis, saldo);
					}
					rodada.folga().ifPresent(c -> lancar(c, Local.FOLGA, d.prioridade(), 0, planteis, saldo));
					Acumulado a = porDivisao.get(d.prioridade());
					for (Jogo jogo : rodada.jogos()) {
						a.receitaPorClube += receita.getOrDefault(jogo.mandante().id(), 0L);
					}
					receita.clear();
				}
			}
			disciplina = r.disciplina();
			moral = r.moral();
			saldo.forEach((id, s) -> {
				if (s < 0) {
					negativos.add(id);
				}
			});
			mercado(numero, paisDaLiga, naLiga, clubesPorId, planteis, saldo, vendas, compras, ultimaCompra, aleatorio);
		}
		for (Divisao divisao : liga.divisoes()) {
			Acumulado a = porDivisao.computeIfAbsent(divisao.prioridade(), k -> new Acumulado());
			List<Clube> tabela = divisao.clubes().stream()
				.sorted(Comparator.comparingInt((Clube c) -> pontos.getOrDefault(c.id(), 0)).reversed()).toList();
			for (int i = 0; i < tabela.size(); i++) {
				long id = tabela.get(i).id();
				int t = Math.min(2, i * 3 / tabela.size());
				a.clubes[t]++;
				a.vendas[t] += vendas.getOrDefault(id, 0);
				a.compras[t] += compras.getOrDefault(id, 0);
				a.forcaInicio[t] += forcaInicio.get(id);
				a.forcaFim[t] += emCampo(planteis.get(id));
				a.saldoEmLimites[t] += saldo.get(id) / (double) mercado.limite(planteis.get(id));
				geral.clubes++;
				geral.negativos += negativos.contains(id) ? 1 : 0;
				geral.nuncaVenderam += vendas.getOrDefault(id, 0) == 0 ? 1 : 0;
			}
		}
	}

	/** Mercado depois da rodada: cada clube abaixo do limite põe um jogador em leilão. */
	private void mercado(int numero, String paisDaLiga, Set<Long> naLiga, Map<Long, Clube> clubes,
			Map<Long, List<Jogador>> planteis, Map<Long, Long> saldo, Map<Long, Integer> vendas,
			Map<Long, Integer> compras, Map<Long, Integer> ultimaCompra, SplittableRandom aleatorio) {
		Set<Long> compraram = new HashSet<>();
		for (long vendedor : naLiga.stream().sorted().toList()) {
			List<Jogador> plantel = planteis.get(vendedor);
			if (!mercado.precisaVender(saldo.get(vendedor), plantel)) {
				continue;
			}
			Optional<Jogador> aVenda = mercado.jogadorParaVender(plantel, paisDaLiga);
			if (aVenda.isEmpty()) {
				continue;
			}
			Jogador jogador = aVenda.get();
			long minimo = saldo.get(vendedor) < 0 ? 0 : preco.lanceInicialLeilao(jogador, paisDaLiga);
			List<Lance> lances = new ArrayList<>();
			for (long comprador : naLiga.stream().sorted().toList()) {
				if (comprador == vendedor || compraram.contains(comprador)) {
					continue;
				}
				mercado.lance(clubes.get(comprador), saldo.get(comprador), planteis.get(comprador), jogador, paisDaLiga,
						minimo).ifPresent(v -> lances.add(new Lance(comprador, v)));
			}
			geral.leiloes++;
			geral.semMinimo += minimo == 0 ? 1 : 0;
			Optional<Lance> vencedor = Leilao.vencedor(lances, minimo, aleatorio);
			if (vencedor.isEmpty()) {
				geral.semLance++;
				continue;
			}
			Lance v = vencedor.get();
			plantel.remove(jogador);
			planteis.get(v.clubeId()).add(jogador);
			saldo.merge(vendedor, v.valor(), Long::sum);
			saldo.merge(v.clubeId(), -v.valor(), Long::sum);
			compraram.add(v.clubeId());
			vendas.merge(vendedor, 1, Integer::sum);
			compras.merge(v.clubeId(), 1, Integer::sum);
			geral.vendidos++;
			geral.vendasNoInicio += numero <= RODADAS_VAIVEM ? 1 : 0;
			geral.pago += v.valor();
			geral.tabela += preco.preco(jogador, paisDaLiga);
			Integer comprou = ultimaCompra.get(vendedor);
			if (comprou != null && numero - comprou <= RODADAS_VAIVEM) {
				geral.vaivem++;
			}
			ultimaCompra.put(v.clubeId(), numero);
		}
	}

	private void lancar(Clube clube, Local local, int prioridade, int publico, Map<Long, List<Jogador>> planteis,
			Map<Long, Long> saldo) {
		for (Lancamento l : caixa.lancamentos(local, prioridade, publico, planteis.get(clube.id()))) {
			saldo.merge(clube.id(), l.valor(), Long::sum);
			if (l.tipo() == TipoLancamento.BILHETERIA) {
				receita.merge(clube.id(), l.valor(), Long::sum);
			}
		}
	}

	private static double emCampo(List<Jogador> plantel) {
		return MercadoDaMaquina.forcaEmCampo(plantel);
	}

	private static String real(double centavos) {
		return Moeda.REAL.formatar(Math.round(centavos));
	}

	private static String decimal(double valor) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", valor);
	}

	private static String pct(double fracao) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%.0f%%", fracao * 100);
	}

	private static final class Acumulado {
		double temporadas;
		double referencia;
		double campeao;
		double segundo;
		double terceiro;
		double patrocinio;
		/** Bilheteria somada de todos os clubes; dividida pelos clubes dá a média por clube. */
		double receitaPorClube;
		final double[] clubes = new double[3];
		final double[] vendas = new double[3];
		final double[] compras = new double[3];
		final double[] forcaInicio = new double[3];
		final double[] forcaFim = new double[3];
		final double[] saldoEmLimites = new double[3];
	}

	private static final class Geral {
		long clubes;
		long negativos;
		long nuncaVenderam;
		long leiloes;
		long semLance;
		long semMinimo;
		long vendidos;
		long vaivem;
		long vendasNoInicio;
		double pago;
		double tabela;
	}
}
