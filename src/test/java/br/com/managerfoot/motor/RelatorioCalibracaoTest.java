package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Sorte;
import br.com.managerfoot.dominio.Temperamento;

/**
 * Gera {@code docs/simulacoes/calibracao-motor.md} com a distribuição de resultados do motor
 * (SPEC §7.6). Lento: fica fora da suíte padrão. Executar com:
 * {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none}
 */
@Tag("simulacao")
class RelatorioCalibracaoTest {

	private static final int PARTIDAS = 10_000;
	private static final Path SAIDA = Path.of("docs/simulacoes/calibracao-motor.md");

	private final ParametrosMotor parametros = ParametrosMotor.padrao();
	private final MotorPartida motor = new MotorPartida(parametros);
	private final StringBuilder md = new StringBuilder();

	@Test
	void gerarRelatorio() throws IOException {
		titulo();
		equilibrados();
		zebras();
		formacoes();
		todasContraTodas();
		temperamento();
		sorte();
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private void titulo() {
		linha("# Calibração do motor de partidas");
		linha("");
		linha("Gerado por `RelatorioCalibracaoTest` (ferramenta de desenvolvimento; SPEC §7.6).");
		linha("Cada cenário: " + PARTIDAS + " partidas, sementes 0 a " + (PARTIDAS - 1) + " (reproduzível).");
		linha("Todos os titulares de um time têm a mesma força; temperamento normal salvo indicação.");
		linha("");
		linha("## Parâmetros usados");
		linha("");
		linha("| Parâmetro | Valor |");
		linha("|---|---|");
		linha("| chancePorMinuto | " + f(parametros.chancePorMinuto(), 3) + " |");
		linha("| fatorMando | " + f(parametros.fatorMando(), 2) + " |");
		linha("| conversaoBase | " + f(parametros.conversaoBase(), 2) + " |");
		linha("| sensibilidadeForca | " + f(parametros.sensibilidadeForca(), 2) + " |");
		linha("| expoentes meias / atacantes / defensores | " + f(parametros.expoenteMeias(), 2) + " / "
				+ f(parametros.expoenteAtacantes(), 2) + " / " + f(parametros.expoenteDefensores(), 2) + " |");
		linha("| fatorPorGolMarcado | " + f(parametros.fatorPorGolMarcado(), 2) + " |");
		linha("| conversaoMinima / Maxima | " + f(parametros.conversaoMinima(), 2) + " / "
				+ f(parametros.conversaoMaxima(), 2) + " |");
		linha("| cartoesPorMinuto (por time) | " + f(parametros.disciplina().cartoesPorMinuto(), 4) + " |");
		linha("| proporcaoVermelhoDireto | " + f(parametros.disciplina().proporcaoVermelhoDireto(), 2) + " |");
		linha("| penaltisPorMinuto | " + f(parametros.penalti().penaltisPorMinuto(), 4) + " |");
		linha("| conversão do pênalti | " + f(parametros.penalti().conversao(), 2) + " |");
		linha("| sorte: chancesExtras / fatorDominioPrejudicado | " + f(parametros.sorte().chancesExtras(), 3) + " / "
				+ f(parametros.sorte().fatorDominioPrejudicado(), 2) + " |");
		linha("");
	}

	private void equilibrados() {
		EstatisticasLote e = medir(escalacao(Formacao.F_4_4_2, 25, 1), escalacao(Formacao.F_4_4_2, 25, 100));
		linha("## Times equilibrados (25 × 25, 4-4-2 × 4-4-2)");
		linha("");
		linha("| Métrica | Medido | Meta (§7.6) |");
		linha("|---|---|---|");
		linha("| Gols por partida | " + f(e.mediaGols(), 2) + " | 1,75 a 2,5 |");
		linha("| Vitória mandante | " + pct(e.pctCasa()) + " | ~45% |");
		linha("| Empate | " + pct(e.pctEmpate()) + " | ~27% |");
		linha("| Vitória visitante | " + pct(e.pctFora()) + " | ~28% |");
		linha("| Amarelos por partida | " + f(e.porPartida(e.amarelos), 2) + " | ~2 |");
		linha("| Expulsões por partida | " + f(e.porPartida(e.expulsoes), 2) + " | 0,1 a 0,2 |");
		linha("| Pênaltis por partida | " + f(e.porPartida(e.penaltis), 2) + " | ~0,3 |");
		linha("| Conversão de pênaltis | " + pct(100.0 * e.penaltisConvertidos / Math.max(1, e.penaltis)) + " | 75% |");
		linha("| Partidas com 5+ gols de um time | " + pct(e.pctGoleadas()) + " | raras |");
		linha("");
		linha("Placares mais comuns (mandante × visitante):");
		linha("");
		linha("| Placar | Frequência |");
		linha("|---|---|");
		e.placaresMaisComuns(12).forEach((placar, n) -> linha("| " + placar + " | " + pct(100.0 * n / PARTIDAS) + " |"));
		linha("");
	}

	private void zebras() {
		linha("## Zebras (time mais fraco em casa, 4-4-2 × 4-4-2)");
		linha("");
		linha("| Força casa × fora | Diferença | Fraco vence | Empate | Forte vence | Gols/partida | Meta fraco vence |");
		linha("|---|---|---|---|---|---|---|");
		int[][] casos = { { 20, 30, 15, 20 }, { 20, 40, 5, 10 }, { 10, 45, 1, 3 }, { 1, 50, 1, -1 } };
		for (int[] c : casos) {
			EstatisticasLote e = medir(escalacao(Formacao.F_4_4_2, c[0], 1), escalacao(Formacao.F_4_4_2, c[1], 100));
			String meta = c[3] < 0 ? "≥ ~1% (piso)" : c[2] + "% a " + c[3] + "%";
			linha("| " + c[0] + " × " + c[1] + " | " + (c[1] - c[0]) + " | " + pct(e.pctCasa()) + " | "
					+ pct(e.pctEmpate()) + " | " + pct(e.pctFora()) + " | " + f(e.mediaGols(), 2) + " | " + meta + " |");
		}
		linha("");
	}

	private void formacoes() {
		linha("## Formações contra 4-4-2 (força 25 × 25, formação testada em casa)");
		linha("");
		linha("| Formação | Estilo | Gols pró | Gols contra | Vitória | Empate | Derrota |");
		linha("|---|---|---|---|---|---|---|");
		for (Formacao formacao : Formacao.values()) {
			EstatisticasLote e = medir(escalacao(formacao, 25, 1), escalacao(Formacao.F_4_4_2, 25, 100));
			linha("| " + formacao.rotulo() + " | " + formacao.estilo() + " | " + f(e.porPartida(e.golsCasa), 2) + " | "
					+ f(e.porPartida(e.golsFora), 2) + " | " + pct(e.pctCasa()) + " | " + pct(e.pctEmpate()) + " | "
					+ pct(e.pctFora()) + " |");
		}
		linha("");
	}

	/** Cada formação joga em casa e fora contra todas (inclusive ela mesma), força 25 × 25. */
	private void todasContraTodas() {
		int porConfronto = 2_000;
		Formacao[] fs = Formacao.values();
		double[] pontos = new double[fs.length];
		int[][] saldo = new int[fs.length][fs.length];
		for (int i = 0; i < fs.length; i++) {
			for (int j = 0; j < fs.length; j++) {
				EstatisticasLote e = EstatisticasLote.medir(motor, escalacao(fs[i], 25, 1), escalacao(fs[j], 25, 100), 0,
						porConfronto);
				pontos[i] += (3.0 * e.vitoriasCasa + e.empates) / porConfronto;
				pontos[j] += (3.0 * e.vitoriasFora + e.empates) / porConfronto;
				saldo[i][j] += e.vitoriasCasa - e.vitoriasFora;
				saldo[j][i] += e.vitoriasFora - e.vitoriasCasa;
			}
		}
		linha("## Todas as formações contra todas (força 25 × 25, ida e volta, " + porConfronto + " partidas por jogo)");
		linha("");
		linha("Pontos por jogo (vitória 3, empate 1) e contra quantas das outras 7 formações tem saldo positivo de vitórias.");
		linha("");
		linha("| Formação | Estilo | Pontos por jogo | Supera |");
		linha("|---|---|---|---|");
		for (int i = 0; i < fs.length; i++) {
			int supera = 0;
			for (int j = 0; j < fs.length; j++) {
				if (j != i && saldo[i][j] > 0) {
					supera++;
				}
			}
			linha("| " + fs[i].rotulo() + " | " + fs[i].estilo() + " | " + f(pontos[i] / (2 * fs.length), 2) + " | "
					+ supera + " de 7 |");
		}
		linha("");
	}

	private void temperamento() {
		linha("## Temperamento e cartões (25 × 25, 4-4-2; todos os titulares com o temperamento indicado)");
		linha("");
		linha("| Temperamento do mandante | Amarelos/partida (mandante) | Expulsões/partida (mandante) |");
		linha("|---|---|---|");
		for (Temperamento t : Temperamento.values()) {
			Escalacao casa = escalacao(Formacao.F_4_4_2, 25, 1, t);
			Escalacao fora = escalacao(Formacao.F_4_4_2, 25, 100);
			long amarelos = 0;
			long expulsoes = 0;
			for (long s = 0; s < PARTIDAS; s++) {
				for (EventoPartida ev : motor.simular(casa, fora, s).eventos()) {
					if (ev.lado() != Lado.CASA) {
						continue;
					}
					if (ev.tipo() == TipoEvento.CARTAO_AMARELO || ev.tipo() == TipoEvento.SEGUNDO_AMARELO) {
						amarelos++;
					}
					if (ev.tipo().expulsao()) {
						expulsoes++;
					}
				}
			}
			linha("| " + t + " | " + f((double) amarelos / PARTIDAS, 2) + " | " + f((double) expulsoes / PARTIDAS, 3) + " |");
		}
		linha("");
	}

	private void sorte() {
		Escalacao a = escalacao(Formacao.F_4_4_2, 25, 1);
		Escalacao b = escalacao(Formacao.F_4_4_2, 25, 100);
		EstatisticasLote base = medir(a, b);
		linha("## Sorte e azar da temporada (25 × 25, 4-4-2; SPEC §5.3.1)");
		linha("");
		linha("Meta: cerca de 5 pontos percentuais a mais de vitórias (beneficiado) e de derrotas (prejudicado).");
		linha("");
		linha("| Cenário | Vitórias do clube sorteado | Empates | Derrotas do clube sorteado | Diferença vs. neutro (V / D) | Gols/partida |");
		linha("|---|---|---|---|---|---|");
		for (Sorte sorte : new Sorte[] { Sorte.BENEFICIADO, Sorte.PREJUDICADO }) {
			for (Lado lado : Lado.values()) {
				EstatisticasLote e = EstatisticasLote.medir(motor, a, b, Map.of(lado, sorte), 0, PARTIDAS);
				boolean casa = lado == Lado.CASA;
				double v = casa ? e.pctCasa() : e.pctFora();
				double d = casa ? e.pctFora() : e.pctCasa();
				double v0 = casa ? base.pctCasa() : base.pctFora();
				double d0 = casa ? base.pctFora() : base.pctCasa();
				linha("| " + sorte + " " + (casa ? "em casa" : "fora") + " | " + pct(v) + " | " + pct(e.pctEmpate()) + " | "
						+ pct(d) + " | " + sinal(v - v0) + " / " + sinal(d - d0) + " | " + f(e.mediaGols(), 2) + " |");
			}
		}
		linha("");
	}

	private static String sinal(double pontos) {
		return (pontos >= 0 ? "+" : "") + f(pontos, 1) + " p.p.";
	}

	private EstatisticasLote medir(Escalacao casa, Escalacao fora) {
		return EstatisticasLote.medir(motor, casa, fora, 0, PARTIDAS);
	}

	private void linha(String texto) {
		md.append(texto).append('\n');
	}

	private static String f(double valor, int casas) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%." + casas + "f", valor);
	}

	private static String pct(double valor) {
		return f(valor, 1) + "%";
	}
}
