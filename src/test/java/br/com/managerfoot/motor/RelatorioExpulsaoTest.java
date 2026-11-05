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

/**
 * Gera {@code docs/simulacoes/expulsao.md}: o resultado de quem tem um expulso a mais, com a regra
 * antiga (só o buraco no setor), com a da rev. 98 (adversário com 50% a mais da chance de ficar com a
 * jogada, teto 99/1) e com lances a mais para quem tem um a mais (rev. 99). Lento: fica fora da suíte padrão. Executar com:
 * {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none -Dtest=RelatorioExpulsaoTest}
 */
@Tag("simulacao")
class RelatorioExpulsaoTest {

	private static final int PARTIDAS = 40_000;
	private static final Path SAIDA = Path.of("docs/simulacoes/expulsao.md");
	/** {bônus de domínio, lances extras}: a regra anterior, a da rev. 98 e as propostas da rev. 99. */
	private static final double[][] REGRAS = { { 0, 0 }, { 0.5, 0 }, { 0.5, 0.5 }, { 0.5, 1 }, { 0.5, 2 }, { 0.5, 3 } };

	private final StringBuilder md = new StringBuilder();

	@Test
	void gerarRelatorio() throws IOException {
		linha("# Expulsão e domínio do meio");
		linha("");
		linha("Gerado por `RelatorioExpulsaoTest` (SPEC §7.4, rev. 98). Cada cenário: " + PARTIDAS
				+ " partidas, sementes 0 a " + (PARTIDAS - 1) + " (reproduzível), 4-4-2 × 4-4-2, temperamento normal.");
		linha("As expulsões acontecem naturalmente (cerca de 0,2 por partida). Contam só as partidas em que um time");
		linha("terminou com mais expulsos que o outro; o resultado é o desse time. As tabelas de domínio e lances extras");
		linha("usam a trava das goleadas padrão (rev. 114).");
		linha("");
		linha("- **Domínio:** quanto a chance do adversário de ficar com a jogada cresce (0 = regra anterior, só o buraco");
		linha("  no setor; 0,5 = rev. 98, 50/50 vira 75/25).");
		linha("- **Lances extras:** lance a mais por minuto para quem tem um a mais, em fração da chance normal de lance");
		linha("  (1 = o dobro de lances desse time; proposta da rev. 99).");
		linha("");
		cenario("Times iguais (25 × 25)", 25, 25);
		cenario("Mandante mais forte (30 × 20)", 30, 20);
		cenario("Visitante mais forte (20 × 30)", 20, 30);
		travaGoleadas();
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private void cenario(String titulo, int forcaCasa, int forcaFora) {
		linha("## " + titulo);
		linha("");
		linha("| Domínio | Lances extras | Partidas com expulso a mais | Quem tem o expulso perde | Empata | Vence | Vence ou empata, expulsão no 1º tempo | Idem, no 2º tempo | Gols por partida (todas) | Gols do adversário depois da expulsão |");
		linha("|---|---|---|---|---|---|---|---|---|---|");
		Escalacao casa = escalacao(Formacao.F_4_4_2, forcaCasa, 1);
		Escalacao fora = escalacao(Formacao.F_4_4_2, forcaFora, 100);
		for (double[] regra : REGRAS) {
			MotorPartida motor = new MotorPartida(comRegra(regra[0], regra[1]));
			int comExpulso = 0;
			int perde = 0;
			int empata = 0;
			int primeiroTempo = 0;
			int naoPerdePrimeiro = 0;
			int segundoTempo = 0;
			int naoPerdeSegundo = 0;
			long gols = 0;
			long golsDepois = 0;
			for (long semente = 0; semente < PARTIDAS; semente++) {
				ResultadoPartida r = motor.simular(casa, fora, semente);
				gols += r.golsCasa() + r.golsFora();
				int diferenca = 0;
				int minutoDecisivo = 0;
				for (EventoPartida e : r.eventos()) {
					if (e.tipo().expulsao()) {
						diferenca += e.lado() == Lado.CASA ? 1 : -1;
						if (diferenca != 0 && minutoDecisivo == 0) {
							minutoDecisivo = e.minuto();
						}
					}
				}
				if (diferenca == 0) {
					continue;
				}
				Lado punido = diferenca > 0 ? Lado.CASA : Lado.FORA;
				int dele = punido == Lado.CASA ? r.golsCasa() : r.golsFora();
				int outro = punido == Lado.CASA ? r.golsFora() : r.golsCasa();
				int minuto = minutoDecisivo;
				golsDepois += r.eventos().stream()
					.filter(e -> e.tipo() == TipoEvento.GOL && e.lado() != punido && e.minuto() >= minuto).count();
				comExpulso++;
				boolean naoPerdeu = dele >= outro;
				if (dele < outro) {
					perde++;
				} else if (dele == outro) {
					empata++;
				}
				if (minuto <= 45) {
					primeiroTempo++;
					naoPerdePrimeiro += naoPerdeu ? 1 : 0;
				} else {
					segundoTempo++;
					naoPerdeSegundo += naoPerdeu ? 1 : 0;
				}
			}
			int vence = comExpulso - perde - empata;
			linha("| " + f(regra[0], 1) + " | " + f(regra[1], 1) + " | " + comExpulso + " | " + pct(perde, comExpulso) + " | " + pct(empata, comExpulso) + " | "
					+ pct(vence, comExpulso) + " | " + pct(naoPerdePrimeiro, primeiroTempo) + " | " + pct(naoPerdeSegundo, segundoTempo)
					+ " | " + f((double) gols / PARTIDAS, 2) + " | " + f((double) golsDepois / Math.max(1, comExpulso), 2) + " |");
		}
		linha("");
	}

	/** Trava das goleadas com expulso (rev. 114): tamanho do placar com e sem expulso a mais, por valor da trava. */
	private void travaGoleadas() {
		linha("## Trava das goleadas com expulso (rev. 114)");
		linha("");
		linha("Com jogador a mais e 2 gols ou mais de vantagem, a conversão de quem tem um a mais é multiplicada pela trava");
		linha("a cada gol de vantagem além do primeiro (1 = sem trava; padrão " + f(ParametrosDisciplina.padrao().travaGoleadaComExpulsao(), 2)
				+ "). *Sem expulso*: partidas que terminaram sem expulso a mais. *Perde por 4+* e *Adversário faz 5+*: do ponto");
		linha("de vista de quem tem o expulso.");
		linha("");
		linha("| Cenário | Trava | Gols, sem expulso | Diferença 4+, sem expulso | 5+ de um time, sem expulso | Gols, com expulso | Perde | Empata | Perde por 4+ | Adversário faz 5+ |");
		linha("|---|---|---|---|---|---|---|---|---|---|");
		int[][] forcas = { { 25, 25 }, { 30, 20 }, { 20, 30 } };
		for (int[] forca : forcas) {
			Escalacao casa = escalacao(Formacao.F_4_4_2, forca[0], 1);
			Escalacao fora = escalacao(Formacao.F_4_4_2, forca[1], 100);
			for (double trava : new double[] { 1, 0.65, 0.5, 0.4, 0.3 }) {
				ParametrosMotor p = ParametrosMotor.padrao();
				MotorPartida motor = new MotorPartida(p.comDisciplina(p.disciplina().comTravaGoleada(trava)));
				int sem = 0, semQuatro = 0, semCinco = 0, com = 0, perde = 0, empata = 0, comQuatro = 0, comCinco = 0;
				long golsSem = 0, golsCom = 0;
				for (long semente = 0; semente < PARTIDAS; semente++) {
					ResultadoPartida r = motor.simular(casa, fora, semente);
					int diferenca = 0;
					for (EventoPartida e : r.eventos()) {
						if (e.tipo().expulsao()) {
							diferenca += e.lado() == Lado.CASA ? 1 : -1;
						}
					}
					if (diferenca == 0) {
						sem++;
						golsSem += r.golsCasa() + r.golsFora();
						semQuatro += Math.abs(r.golsCasa() - r.golsFora()) >= 4 ? 1 : 0;
						semCinco += Math.max(r.golsCasa(), r.golsFora()) >= 5 ? 1 : 0;
						continue;
					}
					int dele = diferenca > 0 ? r.golsCasa() : r.golsFora();
					int outro = diferenca > 0 ? r.golsFora() : r.golsCasa();
					com++;
					golsCom += dele + outro;
					perde += dele < outro ? 1 : 0;
					empata += dele == outro ? 1 : 0;
					comQuatro += outro - dele >= 4 ? 1 : 0;
					comCinco += outro >= 5 ? 1 : 0;
				}
				linha("| " + forca[0] + " × " + forca[1] + " | " + f(trava, 2) + " | " + f((double) golsSem / sem, 2) + " | "
						+ pct(semQuatro, sem) + " | " + pct(semCinco, sem) + " | " + f((double) golsCom / com, 2) + " | "
						+ pct(perde, com) + " | " + pct(empata, com) + " | " + pct(comQuatro, com) + " | " + pct(comCinco, com) + " |");
			}
		}
		linha("");
	}

	/** Os parâmetros padrão com outra regra da expulsão. */
	private static ParametrosMotor comRegra(double bonus, double extras) {
		ParametrosMotor p = ParametrosMotor.padrao();
		ParametrosDisciplina d = p.disciplina();
		ParametrosDisciplina outra = new ParametrosDisciplina(d.cartoesPorMinuto(), d.proporcaoVermelhoDireto(),
				d.sensibilidadeTemperamento(), d.pesosTemperamento(), bonus, d.dominioMaximo(), extras,
				d.travaGoleadaComExpulsao());
		return new ParametrosMotor(p.minutos(), p.chancePorMinuto(), p.fatorMando(), p.conversaoBase(), p.sensibilidadeForca(),
				p.expoenteMeias(), p.expoenteAtacantes(), p.expoenteDefensores(), p.fatorPorGolMarcado(), p.conversaoMinima(),
				p.conversaoMaxima(), p.proporcaoDefesa(), p.proporcaoTrave(), p.pesosFinalizador(), outra, p.penalti(), p.sorte(),
				p.asterisco(), p.lesao(), p.aberturaPorAtacantes(), p.ajustesFormacao());
	}

	private void linha(String texto) {
		md.append(texto).append('\n');
	}

	private static String f(double valor, int casas) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%." + casas + "f", valor);
	}

	private static String pct(int parte, int total) {
		return f(100.0 * parte / Math.max(1, total), 1) + "%";
	}
}
