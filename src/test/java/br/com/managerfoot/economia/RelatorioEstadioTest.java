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
import br.com.managerfoot.economia.CaixaDaRodada.Local;
import br.com.managerfoot.economia.PropostasPatrocinio.Proposta;
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
 * Gera {@code docs/simulacoes/estadio-publico-patrocinio.md}: várias temporadas das ligas do banco
 * padrão com a evolução de força (§6.1), o caixa da rodada com o público pela posição na tabela e pela
 * força (§9.2, rev. 56), a ampliação do estádio pela máquina, o prêmio da liga e o patrocínio fixo.
 * As três propostas de patrocínio (modelo 9a) são comparadas pelo que cada clube teria recebido.
 * Sem mercado. Executar com
 * {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none -Dtest=RelatorioEstadioTest}.
 */
@Tag("simulacao")
class RelatorioEstadioTest {

	private static final int SEMENTES = 3;
	private static final int TEMPORADAS = 6;
	private static final Path SAIDA = Path.of("docs/simulacoes/estadio-publico-patrocinio.md");
	private static final String[] TERCOS = { "Topo", "Meio", "Fundo" };

	private final CaixaDaRodada caixa = CaixaDaRodada.padrao();
	private final ParametrosPublico parametros = ParametrosPublico.padrao();
	private final PublicoDoJogo publico = PublicoDoJogo.padrao();
	private final AmpliacaoEstadio ampliacao = AmpliacaoEstadio.padrao();
	private final Premiacao premiacao = Premiacao.padrao();
	private final PropostasPatrocinio propostas = PropostasPatrocinio.padrao();
	private final EvolucaoForca evolucao = EvolucaoForca.padrao();

	private final Map<Integer, Divisoes> porDivisao = new TreeMap<>();

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
		md.append("# Simulação de estádio, público e patrocínio\n\n")
			.append("Gerado por `RelatorioEstadioTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, ")
			.append(SEMENTES).append(" sementes, ").append(TEMPORADAS).append(" temporadas seguidas com promoção e ")
			.append("rebaixamento, evolução de força (§6.1, rev. 56), caixa da rodada, prêmio da liga e patrocínio fixo. Sem ")
			.append("mercado. Só máquina.\n\n")
			.append("## Regras simuladas (proposta do agente, `ParametrosPublico.padrao()`)\n\n")
			.append("- **Torcida** do clube: a curva de 5.000 a 80.000 pela força em campo atual (média dos 11 mais fortes).\n")
			.append("- **Estádio inicial**: ").append(pct(parametros.estadioInicialSobreTorcida()))
			.append(" da torcida pela força cadastrada; máximo de ").append(mil(parametros.capacidadeMaxima())).append(" para todos.\n")
			.append("- **Ocupação** (fração da torcida): ").append(pct(parametros.ocupacaoLider())).append(" no jogo do líder, caindo ")
			.append("em linha reta até ").append(pct(parametros.ocupacaoUltimo())).append(" no do último; receber o líder soma até ")
			.append(pct(parametros.bonusAdversario())).append(", menos quanto pior o mandante (o último não ganha nada); nas ")
			.append(parametros.rodadasDecisivas()).append(" últimas rodadas, 1º contra 2º soma mais ")
			.append(pct(parametros.bonusDecisivo())).append(". Antes da rodada ").append(parametros.rodadaTabelaInteira())
			.append(" a tabela vale menos (mistura com ").append(pct(parametros.ocupacaoInicio())).append("). Sorteio de ±")
			.append(pct(parametros.variacao())).append(". Nunca passa do estádio.\n")
			.append("- **Ampliação**: ").append(mil(parametros.bloco())).append(" lugares por ").append(real(ampliacao.custo()))
			.append(" (preço de um jogador de força 50), valem depois de ").append(parametros.rodadasDeObra())
			.append(" rodadas; uma obra por vez. A máquina amplia quando o jogo grande (líder contra o 2º) já passa do estádio ")
			.append("com o bloco novo e sobram 1,5 limite de caixa, ou com 3 limites de caixa sobrando, até 105% da torcida.\n\n");

		md.append("## Ocupação do estádio por posição na tabela antes do jogo (fração do estádio)\n\n")
			.append("| Divisão | Topo | Meio | Fundo | Líder × 2º | Jogos acima de 90% | Ocupação máxima com estádio acima da torcida |\n")
			.append("|---|---|---|---|---|---|---|\n");
		porDivisao.forEach((p, d) -> md.append("| ").append(p).append("ª | ").append(pct(d.ocupacao[0] / d.jogos[0])).append(" | ")
			.append(pct(d.ocupacao[1] / d.jogos[1])).append(" | ").append(pct(d.ocupacao[2] / d.jogos[2])).append(" | ")
			.append(pct(d.liderContraSegundo / Math.max(1, d.jogosLiderSegundo))).append(" | ")
			.append(pct(d.cheios / (double) (d.jogos[0] + d.jogos[1] + d.jogos[2]))).append(" | ")
			.append(pct(d.maiorOcupacaoEstadioGrande)).append(" |\n"));

		md.append("\n## Estádio e caixa por temporada (média por clube)\n\n")
			.append("| Divisão | Temporada | Estádio | Ampliações na temporada | Bilheteria | Resultado da temporada | Saldo no fim | Clubes negativos |\n")
			.append("|---|---|---|---|---|---|---|---|\n");
		porDivisao.forEach((p, d) -> d.temporadas.forEach((t, m) -> md.append("| ").append(p).append("ª | ").append(t).append(" | ")
			.append(mil((int) Math.round(m.estadio / m.n))).append(" | ").append(dec(m.ampliacoes / m.n)).append(" | ")
			.append(real(m.bilheteria / m.n)).append(" | ").append(real(m.resultado / m.n)).append(" | ")
			.append(real(m.saldo / m.n)).append(" | ").append(pct(m.negativos / m.n)).append(" |\n")));

		md.append("\n## Patrocínio: quanto cada proposta teria pago (média por clube e temporada)\n\n")
			.append("Proposta por vitória: ").append(pct(propostas.porVitoriaAgora())).append(" da tabela à vista e o resto ")
			.append("dividido pelas vitórias de quem vence ").append(pct(propostas.vitoriasEsperadas())).append(" dos jogos. ")
			.append("Proposta por acesso: ").append(pct(propostas.seSubirAgora())).append(" à vista e ")
			.append(dec(propostas.bonusSeSubir())).append(" vezes a tabela se subir (na 1ª, se ficar entre os 3 primeiros). ")
			.append("Terço pela posição final.\n\n")
			.append("| Divisão | Terço final | Fixa | Por vitória | Por acesso | Melhor: fixa / vitória / acesso |\n|---|---|---|---|---|---|\n");
		porDivisao.forEach((p, d) -> {
			for (int t = 0; t < 3; t++) {
				double n = d.patrocinios[t];
				md.append("| ").append(p).append("ª | ").append(TERCOS[t]).append(" | ").append(real(d.fixa[t] / n)).append(" | ")
					.append(real(d.porVitoria[t] / n)).append(" | ").append(real(d.porAcesso[t] / n)).append(" | ")
					.append(pct(d.melhor[t][0] / n)).append(" / ").append(pct(d.melhor[t][1] / n)).append(" / ")
					.append(pct(d.melhor[t][2] / n)).append(" |\n");
			}
		});
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private void simular(PaisPadrao pais, GeradorBancoPadrao.PaisGerado gerado, long semente) {
		Map<Long, List<Jogador>> planteis = new HashMap<>();
		List<Clube> clubes = new ArrayList<>();
		for (GeradorBancoPadrao.ClubeGerado c : gerado.clubes()) {
			clubes.add(c.clube());
			planteis.put(c.clube().id(), c.jogadores().stream()
				.map(j -> new Jogador(c.clube().id() * 100 + j.id(), j.nome(), j.posicao(), j.forca(), j.temperamento(),
						j.asterisco(), j.nacionalidade()))
				.toList());
		}
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
		SplittableRandom aleatorio = new SplittableRandom(semente * 97 + 3);
		Disciplina disciplina = Disciplina.vazia();
		Map<Long, Moral> moral = new HashMap<>();
		for (int numero = 1; numero <= TEMPORADAS; numero++) {
			Temporada temporada = Temporada.iniciar(numero, liga, planteis, aleatorio.nextLong());
			Map<Long, Integer> teto = new HashMap<>();
			Map<Long, Divisao> divisaoDe = new HashMap<>();
			Map<Long, Long> tabelaPatrocinio = new HashMap<>();
			Map<Long, Long> saldoInicio = new HashMap<>();
			Map<Long, Long> bilheteria = new HashMap<>();
			Map<Long, Integer> ampliacoes = new HashMap<>();
			for (Divisao d : liga.divisoes()) {
				for (Clube c : d.clubes()) {
					teto.put(c.id(), d.limiteForca());
					divisaoDe.put(c.id(), d);
					int[] anterior = posicaoAnterior.get(c.id());
					long valor = anterior == null ? premiacao.patrocinioInicial(d.clubes().size(), d.limiteForca())
							: premiacao.patrocinio(anterior[0], anterior[1], anterior[2]);
					tabelaPatrocinio.put(c.id(), valor);
					saldo.merge(c.id(), valor, Long::sum);
					saldoInicio.put(c.id(), saldo.get(c.id()));
				}
			}
			Map<Integer, List<ResultadoJogo>> resultados = new HashMap<>();
			int rodadas = temporada.divisoes().stream().mapToInt(d -> d.calendario().rodadas().size()).max().orElse(0);
			for (int r = 1; r <= rodadas; r++) {
				Map<Long, Integer> posicao = new HashMap<>();
				for (Divisao d : liga.divisoes()) {
					Classificacao c = Classificacao.calcular(d.clubes(), resultados.getOrDefault(d.prioridade(), List.of()),
							temporada.semente());
					d.clubes().forEach(clube -> posicao.put(clube.id(), c.posicao(clube)));
				}
				// público e caixa antes da evolução, com os planteis que jogaram
				for (Temporada.SorteiosDivisao sd : temporada.divisoes()) {
					Divisao d = liga.divisoes().stream().filter(x -> x.prioridade() == sd.prioridade()).findFirst().orElseThrow();
					int total = sd.calendario().rodadas().size();
					for (Rodada rodada : sd.calendario().rodadas()) {
						if (rodada.numero() != r) {
							continue;
						}
						for (Jogo jogo : rodada.jogos()) {
							long m = jogo.mandante().id();
							int pm = posicao.get(m);
							int pv = posicao.get(jogo.visitante().id());
							int pagantes = publico.publico(estadio.get(m), emCampo(planteis.get(m)), pm, pv, d.clubes().size(), r,
									total, aleatorio);
							medirPublico(d, r, pm, pv, pagantes, estadio.get(m), emCampo(planteis.get(m)));
							lancar(saldo, bilheteria, m, caixa.lancamentos(Local.CASA, d.prioridade(), pagantes, planteis.get(m)));
							long v = jogo.visitante().id();
							lancar(saldo, bilheteria, v, caixa.lancamentos(Local.FORA, d.prioridade(), 0, planteis.get(v)));
						}
						rodada.folga().ifPresent(c -> lancar(saldo, bilheteria, c.id(),
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
				// obras que terminam e decisões da máquina
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
						ampliacoes.merge(id, 1, Integer::sum);
					}
				}
			}
			Map<Integer, Classificacao> finais = new HashMap<>();
			for (Divisao d : liga.divisoes()) {
				Classificacao c = Classificacao.calcular(d.clubes(), resultados.get(d.prioridade()), temporada.semente());
				finais.put(d.prioridade(), c);
				Divisoes acumulado = porDivisao.computeIfAbsent(d.prioridade(), k -> new Divisoes());
				for (Clube clube : d.clubes()) {
					int pos = c.posicao(clube);
					long premio = premiacao.premio(pos, d.limiteForca());
					saldo.merge(clube.id(), premio, Long::sum);
					posicaoAnterior.put(clube.id(), new int[] { pos, d.clubes().size(), d.limiteForca() });
					int terco = Math.min(2, (pos - 1) * 3 / d.clubes().size());
					int vitorias = c.linhas().stream().filter(l -> l.clube().id() == clube.id()).findFirst().orElseThrow().vitorias();
					boolean subiu = d.prioridade() == 1 ? pos <= 3 : pos <= d.promovidos();
					List<Proposta> lista = propostas.propostas(tabelaPatrocinio.get(clube.id()), 2 * (d.clubes().size() - 1));
					acumulado.patrocinio(terco, lista, vitorias, subiu);
					Medida m = acumulado.temporadas.computeIfAbsent(numero, k -> new Medida());
					m.n++;
					m.estadio += estadio.get(clube.id());
					m.ampliacoes += ampliacoes.getOrDefault(clube.id(), 0);
					m.bilheteria += bilheteria.getOrDefault(clube.id(), 0L);
					m.resultado += saldo.get(clube.id()) - saldoInicio.get(clube.id());
					m.saldo += saldo.get(clube.id());
					m.negativos += saldo.get(clube.id()) < 0 ? 1 : 0;
				}
			}
			liga = TransicaoTemporada.aplicar(liga, finais, aleatorio).proxima();
			disciplina = disciplina.novaTemporada();
		}
	}

	private void medirPublico(Divisao d, int rodada, int pm, int pv, int pagantes, int capacidade, double forca) {
		Divisoes a = porDivisao.computeIfAbsent(d.prioridade(), k -> new Divisoes());
		double ocupacao = pagantes / (double) capacidade;
		if (rodada >= parametros.rodadaTabelaInteira()) {
			int terco = Math.min(2, (pm - 1) * 3 / d.clubes().size());
			a.ocupacao[terco] += ocupacao;
			a.jogos[terco]++;
			if ((pm == 1 && pv == 2) || (pm == 2 && pv == 1)) {
				a.liderContraSegundo += ocupacao;
				a.jogosLiderSegundo++;
			}
			a.cheios += ocupacao >= 0.9 ? 1 : 0;
		}
		if (capacidade > publico.torcida(forca)) {
			a.maiorOcupacaoEstadioGrande = Math.max(a.maiorOcupacaoEstadioGrande, ocupacao);
		}
	}

	private static void lancar(Map<Long, Long> saldo, Map<Long, Long> bilheteria, long clube, List<Lancamento> lancamentos) {
		for (Lancamento l : lancamentos) {
			saldo.merge(clube, l.valor(), Long::sum);
			if (l.tipo() == TipoLancamento.BILHETERIA) {
				bilheteria.merge(clube, l.valor(), Long::sum);
			}
		}
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

	private static String mil(int v) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%,d", v);
	}

	private static String real(double centavos) {
		return Moeda.REAL.formatar(Math.round(centavos));
	}

	private static final class Divisoes {
		final double[] ocupacao = new double[3];
		final int[] jogos = new int[3];
		double liderContraSegundo;
		int jogosLiderSegundo;
		int cheios;
		double maiorOcupacaoEstadioGrande;
		final Map<Integer, Medida> temporadas = new TreeMap<>();
		final double[] fixa = new double[3];
		final double[] porVitoria = new double[3];
		final double[] porAcesso = new double[3];
		final double[][] melhor = new double[3][3];
		final double[] patrocinios = new double[3];

		void patrocinio(int terco, List<Proposta> lista, int vitorias, boolean subiu) {
			long[] totais = lista.stream().mapToLong(p -> p.total(vitorias, subiu)).toArray();
			fixa[terco] += totais[0];
			porVitoria[terco] += totais[1];
			porAcesso[terco] += totais[2];
			patrocinios[terco]++;
			long maior = Math.max(totais[0], Math.max(totais[1], totais[2]));
			for (int i = 0; i < 3; i++) {
				if (totais[i] == maior) {
					melhor[terco][i]++;
					break;
				}
			}
		}
	}

	private static final class Medida {
		double n;
		double estadio;
		double ampliacoes;
		double bilheteria;
		double resultado;
		double saldo;
		double negativos;
	}
}
