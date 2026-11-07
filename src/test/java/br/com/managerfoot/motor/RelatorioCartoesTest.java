package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Temperamento;

/**
 * Gera {@code docs/simulacoes/cartoes-e-lesoes.md} (SPEC §7.4 e §7.5, rev. 131): vermelho direto mais raro e crescendo
 * com o minuto, trava das goleadas por expulso a mais e frequência das lesões. Lento: fica fora da suíte padrão.
 * Executar com: {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none -Dtest=RelatorioCartoesTest}
 */
@Tag("simulacao")
class RelatorioCartoesTest {

	private static final int PARTIDAS = 40_000;
	private static final int PARTIDAS_TRAVA = 300_000;
	private static final Path SAIDA = Path.of("docs/simulacoes/cartoes-e-lesoes.md");
	/** {fração do vermelho direto no minuto 90, expoente do minuto}; a primeira é a regra até a rev. 130. */
	private static final double[][] VERMELHOS = { { 0.05, 0 }, { 0.04, 1 }, { 0.03, 1 }, { 0.02, 1 }, { 0.04, 2 } };
	private static final double[] LESOES_POR_TEMPORADA = { 1.5, 2.5, 3.0, 4.0 };
	private static final int RODADAS = 38;

	private final StringBuilder md = new StringBuilder();

	@Test
	void gerarRelatorio() throws IOException {
		linha("# Cartões, trava das goleadas e lesões");
		linha("");
		linha("Gerado por `RelatorioCartoesTest` (SPEC §7.4 e §7.5, rev. 131). Sementes 0 a N−1 (reproduzível). Os times têm");
		linha("temperamentos misturados (2 calmos, 2 educados, 3 normais, 2 ansiosos e 2 violentos em cada lado), força 25 × 25,");
		linha("4-4-2 × 4-4-2.");
		linha("");
		vermelhos();
		trava();
		lesoes();
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private void vermelhos() {
		linha("## Vermelho direto");
		linha("");
		linha("A fração dos cartões que é vermelho direto no minuto m é *fração no 90 × (m / 90)^expoente*. Expoente 0 é a");
		linha("regra até a rev. 130 (a mesma em todo o jogo); 1 cresce em linha reta do zero. " + PARTIDAS + " partidas por linha.");
		linha("");
		linha("| Fração no 90 | Expoente | Expulsões por jogo | Vermelho direto | Dois amarelos | Expulsões por dois amarelos | Até o minuto 15 | No 1º tempo | Amarelos por jogo | Gols por jogo |");
		linha("|---|---|---|---|---|---|---|---|---|---|");
		Escalacao casa = misturada(25, 1);
		Escalacao fora = misturada(25, 100);
		for (double[] v : VERMELHOS) {
			ParametrosMotor p = ParametrosMotor.padrao();
			MotorPartida motor = new MotorPartida(p.comDisciplina(p.disciplina().comVermelhoDireto(v[0], v[1])));
			long diretos = 0, segundos = 0, cedo = 0, primeiro = 0, amarelos = 0, gols = 0;
			for (long semente = 0; semente < PARTIDAS; semente++) {
				ResultadoPartida r = motor.simular(casa, fora, semente);
				gols += r.golsCasa() + r.golsFora();
				for (EventoPartida e : r.eventos()) {
					if (e.tipo() == TipoEvento.CARTAO_AMARELO || e.tipo() == TipoEvento.SEGUNDO_AMARELO) {
						amarelos++;
					}
					if (!e.tipo().expulsao()) {
						continue;
					}
					if (e.tipo() == TipoEvento.CARTAO_VERMELHO) {
						diretos++;
					} else {
						segundos++;
					}
					cedo += e.minuto() <= 15 ? 1 : 0;
					primeiro += e.minuto() <= 45 ? 1 : 0;
				}
			}
			long expulsoes = diretos + segundos;
			linha("| " + f(v[0] * 100, 0) + "% | " + f(v[1], 0) + " | " + f((double) expulsoes / PARTIDAS, 3) + " | "
					+ f((double) diretos / PARTIDAS, 3) + " | " + f((double) segundos / PARTIDAS, 3) + " | "
					+ pct(segundos, expulsoes) + " | " + pct(cedo, expulsoes) + " | " + pct(primeiro, expulsoes) + " | "
					+ f((double) amarelos / PARTIDAS, 2) + " | " + f((double) gols / PARTIDAS, 2) + " |");
		}
		linha("");
	}

	private void trava() {
		linha("## Trava das goleadas por expulso a mais");
		linha("");
		linha("A trava (fator " + f(ParametrosDisciplina.padrao().travaGoleadaComExpulsao(), 1) + " por gol) começa quando a vantagem de quem tem jogador a mais passa de");
		linha("*gols livres × expulsos a mais*. 1 é a regra até a rev. 130 (a partir de 2 gols, qualquer que seja o número de");
		linha("expulsos); 2 faz 1 expulso começar em 3 gols e 2 expulsos em 5. " + PARTIDAS_TRAVA + " partidas por linha, com o vermelho");
		linha("da regra atual; o resultado é visto por quem tem o expulso, pelo número de expulsos a mais no fim.");
		linha("");
		linha("| Cenário | Gols livres | Expulsos a mais | Partidas | Gols do adversário | Perde por 3+ | Perde por 5+ | Adversário faz 5+ |");
		linha("|---|---|---|---|---|---|---|---|");
		int[][] forcas = { { 25, 25 }, { 30, 20 } };
		for (int[] forca : forcas) {
			Escalacao casa = misturada(forca[0], 1);
			Escalacao fora = misturada(forca[1], 100);
			for (int livres : new int[] { 1, 2 }) {
				ParametrosMotor p = ParametrosMotor.padrao();
				MotorPartida motor = new MotorPartida(p.comDisciplina(p.disciplina().comGolsLivresPorExpulso(livres)));
				int[] partidas = new int[3], perde3 = new int[3], perde5 = new int[3], faz5 = new int[3];
				long[] golsOutro = new long[3];
				for (long semente = 0; semente < PARTIDAS_TRAVA; semente++) {
					ResultadoPartida r = motor.simular(casa, fora, semente);
					int diferenca = 0;
					for (EventoPartida e : r.eventos()) {
						if (e.tipo().expulsao()) {
							diferenca += e.lado() == Lado.CASA ? 1 : -1;
						}
					}
					int a = Math.min(2, Math.abs(diferenca));
					if (a == 0) {
						continue;
					}
					int dele = diferenca > 0 ? r.golsCasa() : r.golsFora();
					int outro = diferenca > 0 ? r.golsFora() : r.golsCasa();
					partidas[a]++;
					golsOutro[a] += outro;
					perde3[a] += outro - dele >= 3 ? 1 : 0;
					perde5[a] += outro - dele >= 5 ? 1 : 0;
					faz5[a] += outro >= 5 ? 1 : 0;
				}
				for (int a = 1; a <= 2; a++) {
					linha("| " + forca[0] + " × " + forca[1] + " | " + livres + " | " + (a == 2 ? "2 ou mais" : "1") + " | " + partidas[a]
							+ " | " + f((double) golsOutro[a] / Math.max(1, partidas[a]), 2) + " | " + pct(perde3[a], partidas[a])
							+ " | " + pct(perde5[a], partidas[a]) + " | " + pct(faz5[a], partidas[a]) + " |");
				}
			}
		}
		linha("");
	}

	private void lesoes() {
		linha("## Lesões");
		linha("");
		linha("Frequência em lesões por clube em " + RODADAS + " jogos, antes do limite da temporada. *Por clube na temporada*: a");
		linha("conta esperada com o limite, para " + RODADAS + " jogos (a copa e a continental somam alguns jogos a mais). "
				+ PARTIDAS + " partidas por linha.");
		linha("");
		linha("| Frequência | Lesões por jogo (os dois times) | Por clube na temporada, limite 2 | Limite 3 | Limite 4 | Clubes que batem o limite 2 | Limite 3 | Limite 4 |");
		linha("|---|---|---|---|---|---|---|---|");
		Escalacao casa = misturada(25, 1);
		Escalacao fora = misturada(25, 100);
		for (double porTemporada : LESOES_POR_TEMPORADA) {
			ParametrosMotor p = ParametrosMotor.padrao();
			ParametrosLesao l = p.lesao();
			ParametrosLesao outra = new ParametrosLesao(porTemporada / (RODADAS * 90), l.pesosTemperamento(), l.gravidades(), 99);
			MotorPartida motor = new MotorPartida(p.comLesao(outra));
			long lesoes = 0;
			for (long semente = 0; semente < PARTIDAS; semente++) {
				lesoes += motor.simular(casa, fora, semente).eventos().stream().filter(e -> e.tipo() == TipoEvento.LESAO).count();
			}
			double porTime = (double) lesoes / PARTIDAS / 2;
			StringBuilder esperada = new StringBuilder();
			StringBuilder batem = new StringBuilder();
			for (int limite = 2; limite <= 4; limite++) {
				esperada.append(" | ").append(f(esperadaComLimite(porTime, limite), 2));
				batem.append(" | ").append(f(100 * chanceDeAoMenos(porTime, limite), 0)).append("%");
			}
			linha("| " + f(porTemporada, 1) + " | " + f((double) lesoes / PARTIDAS, 3) + esperada + batem + " |");
		}
		linha("");
	}

	/** E[min(X, limite)] com X binomial de {@link #RODADAS} jogos. */
	private static double esperadaComLimite(double p, int limite) {
		double soma = 0;
		for (int k = 0; k <= RODADAS; k++) {
			soma += Math.min(k, limite) * binomial(k, p);
		}
		return soma;
	}

	private static double chanceDeAoMenos(double p, int limite) {
		double soma = 0;
		for (int k = limite; k <= RODADAS; k++) {
			soma += binomial(k, p);
		}
		return soma;
	}

	private static double binomial(int k, double p) {
		double c = 1;
		for (int i = 0; i < k; i++) {
			c = c * (RODADAS - i) / (i + 1);
		}
		return c * Math.pow(p, k) * Math.pow(1 - p, RODADAS - k);
	}

	/** 4-4-2 com temperamentos misturados, como num plantel de verdade. */
	private static Escalacao misturada(int forca, long primeiroId) {
		Temperamento[] mistura = { Temperamento.CALMO, Temperamento.EDUCADO, Temperamento.NORMAL, Temperamento.ANSIOSO,
				Temperamento.VIOLENTO, Temperamento.NORMAL, Temperamento.CALMO, Temperamento.EDUCADO, Temperamento.ANSIOSO,
				Temperamento.VIOLENTO, Temperamento.NORMAL };
		Escalacao base = escalacao(Formacao.F_4_4_2, forca, primeiroId);
		java.util.List<br.com.managerfoot.dominio.Jogador> titulares = new java.util.ArrayList<>();
		for (int i = 0; i < base.titulares().size(); i++) {
			var j = base.titulares().get(i);
			titulares.add(new br.com.managerfoot.dominio.Jogador(j.id(), j.nome(), j.posicao(), j.forca(), mistura[i],
					j.nacionalidade()));
		}
		return new Escalacao(Formacao.F_4_4_2, titulares);
	}

	private void linha(String texto) {
		md.append(texto).append('\n');
	}

	private static String f(double valor, int casas) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%." + casas + "f", valor);
	}

	private static String pct(long parte, long total) {
		return f(100.0 * parte / Math.max(1, total), 1) + "%";
	}
}
