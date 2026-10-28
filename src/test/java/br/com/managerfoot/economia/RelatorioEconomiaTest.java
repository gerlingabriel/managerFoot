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
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.economia.CaixaDaRodada.Local;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Disciplina;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.ProcessamentoRodada;
import br.com.managerfoot.temporada.Rodada;
import br.com.managerfoot.temporada.Temporada;

/**
 * Gera {@code docs/simulacoes/economia-caixa.md}: o caixa da rodada (SPEC §9, rev. 37) aplicado a
 * temporadas inteiras das ligas do banco padrão. Serve para calibrar os {@link ParametrosEconomia}
 * antes da aprovação. Executar com {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none}.
 */
@Tag("simulacao")
class RelatorioEconomiaTest {

	private static final int SEMENTES = 30;
	private static final Path SAIDA = Path.of("docs/simulacoes/economia-caixa.md");

	private final CaixaDaRodada caixa = CaixaDaRodada.padrao();
	private final ParametrosEconomia parametros = ParametrosEconomia.padrao();
	private final ParametrosMoral parametrosMoral = ParametrosMoral.padrao();

	/** Acumulado por divisão (prioridade) de todas as ligas e sementes. */
	private final Map<Integer, Acumulado> porDivisao = new TreeMap<>();

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
		md.append("# Simulação do caixa da rodada\n\n");
		md.append("Gerado por `RelatorioEconomiaTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, ")
			.append(SEMENTES).append(" temporadas cada (sementes 0 a ").append(SEMENTES - 1)
			.append("), cada uma começando do zero: saldo inicial, moral inicial, sem promoção. ")
			.append("Valores em Real; parâmetros de `ParametrosEconomia.padrao()` (proposta da rev. 37).\n\n");
		md.append("Só o caixa da rodada: bilheteria, custo do jogo, deslocamento e salários. Ainda não há patrocínio, ")
			.append("premiação, mercado nem empréstimos.\n\n");
		tabelaDeReferencia(md);
		md.append("## Por divisão (média por clube e temporada)\n\n");
		md.append("| Divisão | Forças | Público médio | Ocupação | Bilheteria por jogo | Receita líquida | Folha | Resultado | Fecham no vermelho |\n");
		md.append("|---|---|---|---|---|---|---|---|---|\n");
		porDivisao.forEach((prioridade, a) -> md.append("| ").append(prioridade).append("ª | ").append(a.forcaMin).append("–")
			.append(a.forcaMax).append(" | ").append(inteiro(a.publico / a.jogosEmCasa)).append(" | ")
			.append(pct(a.ocupacao / a.jogosEmCasa)).append(" | ").append(real(a.bilheteria / a.jogosEmCasa)).append(" | ")
			.append(real(a.receitaLiquida / a.clubes)).append(" | ").append(real(a.folha / a.clubes)).append(" | ")
			.append(real(a.resultado / a.clubes)).append(" | ").append(pct((double) a.negativos / a.clubes)).append(" |\n"));
		md.append("\n## Resultado da temporada por terço de força dentro da divisão\n\n");
		md.append("| Divisão | Mais fortes | Medianos | Mais fracos | Saldo final mínimo |\n|---|---|---|---|---|\n");
		porDivisao.forEach((prioridade, a) -> md.append("| ").append(prioridade).append("ª | ")
			.append(real(a.tercos[0] / a.clubesPorTerco[0])).append(" | ").append(real(a.tercos[1] / a.clubesPorTerco[1]))
			.append(" | ").append(real(a.tercos[2] / a.clubesPorTerco[2])).append(" | ").append(real(a.saldoMinimo))
			.append(" |\n"));
		md.append("\n*Receita líquida* é bilheteria menos custo do jogo e deslocamento. *Resultado* é a receita líquida menos ")
			.append("a folha. *Fecham no vermelho*: clubes cujo resultado da temporada é negativo (o saldo inicial cobre ")
			.append(parametros.rodadasDeFolhaNoInicio()).append(" rodadas de folha).\n");
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private void simular(PaisPadrao pais, GeradorBancoPadrao.PaisGerado gerado, long semente) {
		// ids dos jogadores únicos no país (o gerador numera de 1 a 23 em cada clube)
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
		Temporada temporada = Temporada.iniciar(1, liga, planteis, semente);
		ProcessamentoRodada processamento = ProcessamentoRodada.padrao();
		SplittableRandom publicoAleatorio = new SplittableRandom(semente * 7 + 1);
		Map<Long, Integer> capacidade = new HashMap<>();
		Map<Long, Long> saldo = new HashMap<>();
		Map<Long, Long> resultado = new HashMap<>();
		Map<Long, Long> saldoMinimo = new HashMap<>();
		clubes.forEach(c -> {
			capacidade.put(c.id(), caixa.capacidadeInicial(c.forca()));
			saldo.put(c.id(), caixa.saldoInicial(planteis.get(c.id())));
			saldoMinimo.put(c.id(), saldo.get(c.id()));
		});
		Disciplina disciplina = Disciplina.vazia();
		Map<Long, Moral> moral = new HashMap<>();
		int rodadas = temporada.divisoes().stream().mapToInt(d -> d.calendario().rodadas().size()).max().orElse(0);
		for (int numero = 1; numero <= rodadas; numero++) {
			ProcessamentoRodada.Resultado r = processamento.jogar(temporada, numero, planteis, disciplina, moral);
			for (Temporada.SorteiosDivisao d : temporada.divisoes()) {
				Acumulado a = porDivisao.computeIfAbsent(d.prioridade(), k -> new Acumulado());
				for (Rodada rodada : d.calendario().rodadas()) {
					if (rodada.numero() != numero) {
						continue;
					}
					for (Jogo jogo : rodada.jogos()) {
						Clube casa = jogo.mandante();
						int publico = caixa.publico(capacidade.get(casa.id()), moral.getOrDefault(casa.id(),
								Moral.inicial(parametrosMoral)), casa.forca(), jogo.visitante().forca(), publicoAleatorio);
						a.publico += publico;
						a.ocupacao += publico / (double) capacidade.get(casa.id());
						a.jogosEmCasa++;
						lancar(a, casa, Local.CASA, d.prioridade(), publico, planteis, saldo, resultado, saldoMinimo);
						lancar(a, jogo.visitante(), Local.FORA, d.prioridade(), 0, planteis, saldo, resultado, saldoMinimo);
					}
					rodada.folga().ifPresent(c -> lancar(a, c, Local.FOLGA, d.prioridade(), 0, planteis, saldo, resultado,
							saldoMinimo));
				}
			}
			disciplina = r.disciplina();
			moral = r.moral();
		}
		for (Divisao divisao : liga.divisoes()) {
			Acumulado a = porDivisao.get(divisao.prioridade());
			List<Clube> ordem = divisao.clubes().stream().sorted(Comparator.comparingInt(Clube::forca).reversed()).toList();
			for (int i = 0; i < ordem.size(); i++) {
				Clube c = ordem.get(i);
				long r = resultado.getOrDefault(c.id(), 0L);
				a.clubes++;
				a.resultado += r;
				a.negativos += r < 0 ? 1 : 0;
				a.saldoMinimo = Math.min(a.saldoMinimo, saldoMinimo.get(c.id()));
				a.forcaMin = Math.min(a.forcaMin, c.forca());
				a.forcaMax = Math.max(a.forcaMax, c.forca());
				int terco = Math.min(2, i * 3 / ordem.size());
				a.tercos[terco] += r;
				a.clubesPorTerco[terco]++;
			}
		}
	}

	private void lancar(Acumulado a, Clube clube, Local local, int prioridade, int publico, Map<Long, List<Jogador>> planteis,
			Map<Long, Long> saldo, Map<Long, Long> resultado, Map<Long, Long> saldoMinimo) {
		for (Lancamento l : caixa.lancamentos(local, prioridade, publico, planteis.get(clube.id()))) {
			switch (l.tipo()) {
				case BILHETERIA -> {
					a.bilheteria += l.valor();
					a.receitaLiquida += l.valor();
				}
				case SALARIOS -> a.folha -= l.valor();
				default -> a.receitaLiquida += l.valor();
			}
			saldo.merge(clube.id(), l.valor(), Long::sum);
			resultado.merge(clube.id(), l.valor(), Long::sum);
		}
		saldoMinimo.merge(clube.id(), saldo.get(clube.id()), Math::min);
	}

	private void tabelaDeReferencia(StringBuilder md) {
		md.append("## Tabelas propostas\n\n");
		md.append("| Força | Estádio | Salário por rodada | Folha de 23 por rodada (todos nessa força) |\n|---|---|---|---|\n");
		for (int forca : new int[] { 12, 17, 22, 26, 30, 35, 40, 45, 50 }) {
			Jogador j = new Jogador(1, "x", Posicao.ATACANTE, forca,
					Temperamento.NORMAL, "BRA");
			md.append("| ").append(forca).append(" | ").append(inteiro(caixa.capacidadeInicial(forca))).append(" | ")
				.append(real(caixa.salario(j))).append(" | ").append(real(caixa.salario(j) * 23)).append(" |\n");
		}
		md.append("\n| Divisão | Ingresso | Deslocamento por jogo fora |\n|---|---|---|\n");
		for (int p = 1; p <= 3; p++) {
			md.append("| ").append(p).append("ª | ").append(real(parametros.ingresso(p))).append(" | ")
				.append(real(parametros.deslocamento(p))).append(" |\n");
		}
		md.append("\nOcupação: ").append(pct(parametros.ocupacaoBase())).append(" com moral inicial e adversário de mesma força; ")
			.append("moral máxima soma ").append(pct(parametros.pesoMoral() * 12 / 30)).append(" e moral zero tira ")
			.append(pct(parametros.pesoMoral() * 18 / 30)).append("; adversário 20 pontos mais forte soma ")
			.append(pct(parametros.pesoAdversario())).append("; sorteio de ±").append(pct(parametros.variacaoPublico()))
			.append(". Custo do jogo em casa: ").append(pct(parametros.custoDoJogo())).append(" da bilheteria.\n\n");
	}

	private static String real(double centavos) {
		return Moeda.REAL.formatar(Math.round(centavos));
	}

	private static String inteiro(double valor) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%,d", Math.round(valor));
	}

	private static String pct(double fracao) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%.0f%%", fracao * 100);
	}

	private static final class Acumulado {
		double publico;
		double ocupacao;
		long jogosEmCasa;
		double bilheteria;
		double receitaLiquida;
		double folha;
		double resultado;
		int clubes;
		int negativos;
		long saldoMinimo = Long.MAX_VALUE;
		int forcaMin = Integer.MAX_VALUE;
		int forcaMax;
		final double[] tercos = new double[3];
		final int[] clubesPorTerco = new int[3];
	}
}
