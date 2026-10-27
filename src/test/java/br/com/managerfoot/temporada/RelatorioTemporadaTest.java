package br.com.managerfoot.temporada;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;

/**
 * Gera {@code docs/simulacoes/temporada.md}: distribuição de campeões, pontos, rebaixados e moral em
 * temporadas completas. Executar com {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none}.
 */
@Tag("simulacao")
class RelatorioTemporadaTest {

	private static final int TEMPORADAS = 200;
	private static final int CLUBES = 20;
	private static final int REBAIXADOS = 4;
	private static final Path SAIDA = Path.of("docs/simulacoes/temporada.md");

	@Test
	void gerarRelatorio() throws IOException {
		StringBuilder md = new StringBuilder();
		md.append("# Simulação de temporadas completas\n\n");
		md.append("Gerado por `RelatorioTemporadaTest` (ferramenta de desenvolvimento). ")
			.append(TEMPORADAS).append(" temporadas por cenário, divisão com ").append(CLUBES)
			.append(" clubes, turno e returno, calendário sorteado; sementes 0 a ").append(TEMPORADAS - 1).append(".\n\n");
		md.append("**Simplificação:** ainda não há elencos. Cada clube joga em 4-4-2 com os 11 titulares na força ")
			.append("do clube. Moral com os valores provisórios da SPEC §6.2.\n\n");
		cenario(md, "Cenário A: forças de 50 a 12, de 2 em 2 (exemplo de hierarquia do usuário)", 2);
		cenario(md, "Cenário B: forças de 50 a 31, de 1 em 1 (hierarquia apertada)", 1);
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private void cenario(StringBuilder md, String titulo, int passo) {
		List<Clube> clubes = new ArrayList<>();
		for (int i = 0; i < CLUBES; i++) {
			clubes.add(new Clube(i + 1, "F" + (50 - passo * i), 50 - passo * i));
		}
		Divisao divisao = new Divisao("Liga", 1, 50, 0, REBAIXADOS, clubes);
		ParametrosMoral pm = ParametrosMoral.padrao();

		int[] vezesNaPosicao1 = new int[CLUBES];
		int[] rebaixamentos = new int[CLUBES];
		double[] posicaoMedia = new double[CLUBES];
		double[] pontosMedios = new double[CLUBES];
		double[] moralMedia = new double[CLUBES];
		int[] moralVermelha = new int[CLUBES];
		double pontosCampeao = 0;
		double pontosLanterna = 0;
		double gols = 0;
		double jogos = 0;
		int campeaoForaDoTop3 = 0;

		for (long t = 0; t < TEMPORADAS; t++) {
			TemporadaSimulada temporada = TemporadaSimulada.jogar(divisao, t);
			Classificacao tabela = temporada.classificacao;
			pontosCampeao += tabela.linhas().getFirst().pontos();
			pontosLanterna += tabela.linhas().getLast().pontos();
			gols += temporada.gols;
			jogos += temporada.jogos;
			for (int i = 0; i < CLUBES; i++) {
				Clube clube = clubes.get(i);
				int posicao = tabela.posicao(clube);
				posicaoMedia[i] += posicao;
				pontosMedios[i] += tabela.linha(clube).pontos();
				if (posicao == 1) {
					vezesNaPosicao1[i]++;
					if (i >= 3) {
						campeaoForaDoTop3++;
					}
				}
				if (posicao > CLUBES - REBAIXADOS) {
					rebaixamentos[i]++;
				}
				Moral moral = temporada.moral.get(clube);
				moralMedia[i] += moral.valor();
				if (moral.faixa(pm) == Moral.Faixa.VERMELHO) {
					moralVermelha[i]++;
				}
			}
		}

		md.append("## ").append(titulo).append("\n\n| Métrica | Valor |\n|---|---|\n");
		md.append("| Gols por jogo | ").append(f(gols / jogos, 2)).append(" |\n");
		md.append("| Pontos do campeão (média) | ").append(f(pontosCampeao / TEMPORADAS, 1)).append(" |\n");
		md.append("| Pontos do lanterna (média) | ").append(f(pontosLanterna / TEMPORADAS, 1)).append(" |\n");
		md.append("| Campeão fora dos 3 mais fortes | ").append(f(100.0 * campeaoForaDoTop3 / TEMPORADAS, 1)).append("% |\n\n");
		md.append("Por clube:\n\n");
		md.append("| Força | Posição média | Pontos médios | Títulos | Rebaixamentos | Moral final média | Moral vermelha no fim |\n");
		md.append("|---|---|---|---|---|---|---|\n");
		for (int i = 0; i < CLUBES; i++) {
			md.append("| ").append(clubes.get(i).forca())
				.append(" | ").append(f(posicaoMedia[i] / TEMPORADAS, 1))
				.append(" | ").append(f(pontosMedios[i] / TEMPORADAS, 1))
				.append(" | ").append(f(100.0 * vezesNaPosicao1[i] / TEMPORADAS, 1)).append("%")
				.append(" | ").append(f(100.0 * rebaixamentos[i] / TEMPORADAS, 1)).append("%")
				.append(" | ").append(f(moralMedia[i] / TEMPORADAS, 1))
				.append(" | ").append(f(100.0 * moralVermelha[i] / TEMPORADAS, 1)).append("%")
				.append(" |\n");
		}
		md.append("\n");
	}

	private static String f(double valor, int casas) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%." + casas + "f", valor);
	}
}
