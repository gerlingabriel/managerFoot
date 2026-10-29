package br.com.managerfoot.temporada;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.motor.MotorPartida;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.ResultadoPartida;

/**
 * Gera {@code docs/simulacoes/demissao.md} (rev. 60): com que frequência a regra de demissão pela
 * moral ({@link Moral#demite}) demitiria o treinador de cada clube, se valesse para todos, e como
 * fica a troca (rev. 61, {@link TrocaDeTreinador}): o chamado vem de um clube inferior de moral verde,
 * os dois trocam de clube e recomeçam com a moral inicial; sem chamado, o treinador fica. Mesma simplificação de
 * {@link RelatorioTemporadaTest}: 4-4-2 com os 11 na força do clube. Executar com
 * {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none -Dtest=RelatorioDemissaoTest}.
 */
@Tag("simulacao")
class RelatorioDemissaoTest {

	private static final int TEMPORADAS = 200;
	private static final int CLUBES = 20;
	private static final Path SAIDA = Path.of("docs/simulacoes/demissao.md");

	private final ParametrosMoral parametros = ParametrosMoral.padrao();
	private final MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());
	private final TrocaDeTreinador troca = TrocaDeTreinador.padrao();

	@Test
	void gerarRelatorio() throws IOException {
		StringBuilder md = new StringBuilder();
		md.append("# Demissão pela moral\n\n");
		md.append("Gerado por `RelatorioDemissaoTest` (ferramenta de desenvolvimento). ").append(TEMPORADAS)
			.append(" temporadas por cenário, divisão de ").append(CLUBES)
			.append(" clubes em turno e returno, calendário sorteado; sementes 0 a ").append(TEMPORADAS - 1).append(".\n\n");
		md.append("**Regra (SPEC §5.4, rev. 60):** moral de 0 a 30, começa em ").append(parametros.inicial())
			.append("; verde de ").append(parametros.limiteVerde()).append(" a ").append(parametros.maximo())
			.append(", amarelo de ").append(parametros.limiteAmarelo()).append(" a ").append(parametros.limiteVerde() - 1)
			.append(", vermelho de ").append(parametros.minimo()).append(" a ").append(parametros.limiteAmarelo() - 1)
			.append(". Ao entrar no vermelho, o jogo seguinte é decisivo: se depois dele a moral continua vermelha, ")
			.append("o treinador é demitido. Aqui a regra vale para **todos** os clubes, para medir a frequência.\n\n");
		md.append("**Troca (rev. 61):** é chamado o treinador de um clube de força inferior com moral verde (entre os ")
			.append("3 mais fortes, o de maior força; aqui não há usuário). Os dois trocam de clube e os dois clubes ")
			.append("voltam à moral inicial. Sem chamado (nenhum inferior de moral verde), o treinador fica e a moral ")
			.append("segue: o jogo seguinte no vermelho é decisivo de novo.\n\n");
		md.append("**Simplificação:** cada clube joga em 4-4-2 com os 11 na força do clube, sem evolução de força nem ")
			.append("mercado, como em `temporada.md`.\n\n");
		cenario(md, "Cenário A: forças de 50 a 12, de 2 em 2", 2);
		cenario(md, "Cenário B: forças de 50 a 31, de 1 em 1 (hierarquia apertada)", 1);
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private void cenario(StringBuilder md, String titulo, int passo) {
		List<Clube> clubes = new ArrayList<>();
		Map<Clube, Escalacao> escalacoes = new HashMap<>();
		for (int i = 0; i < CLUBES; i++) {
			Clube clube = new Clube(i + 1, "F" + (50 - passo * i), 50 - passo * i);
			clubes.add(clube);
			escalacoes.put(clube, escalacao(clube));
		}
		int[] demissoes = new int[CLUBES];
		int[] semChamado = new int[CLUBES];
		int[] cedeu = new int[CLUBES];
		long totalSemChamado = 0;
		double somaDiferenca = 0;
		int[] temporadasComDemissao = new int[CLUBES];
		long[] rodadaPrimeira = new long[CLUBES];
		int[] entradasNoVermelho = new int[CLUBES];
		int[] salvos = new int[CLUBES];
		long totalDemissoes = 0;
		int[] demissoesPorRodada = new int[(CLUBES - 1) * 2 + 1];

		for (long t = 0; t < TEMPORADAS; t++) {
			SplittableRandom aleatorio = new SplittableRandom(t);
			Map<Clube, Moral> moral = new HashMap<>();
			clubes.forEach(c -> moral.put(c, Moral.inicial(parametros)));
			int[] naTemporada = new int[CLUBES];
			for (Rodada rodada : Calendario.sorteado(clubes, aleatorio).rodadas()) {
				for (Jogo jogo : rodada.jogos()) {
					ResultadoPartida r = motor.simular(escalacoes.get(jogo.mandante()), escalacoes.get(jogo.visitante()),
							aleatorio.nextLong());
					for (boolean emCasa : new boolean[] { true, false }) {
						Clube clube = emCasa ? jogo.mandante() : jogo.visitante();
						int i = clubes.indexOf(clube);
						Moral antes = moral.get(clube);
						Moral depois = antes.aposJogo(emCasa, emCasa ? r.golsCasa() : r.golsFora(),
								emCasa ? r.golsFora() : r.golsCasa(), parametros);
						boolean vermelhoAntes = antes.faixa(parametros) == Moral.Faixa.VERMELHO;
						boolean vermelhoDepois = depois.faixa(parametros) == Moral.Faixa.VERMELHO;
						if (!vermelhoAntes && vermelhoDepois) {
							entradasNoVermelho[i]++;
						}
						if (vermelhoAntes && !vermelhoDepois) {
							salvos[i]++;
						}
						if (antes.demite(depois, parametros)) {
							List<TrocaDeTreinador.Candidato> candidatos = new ArrayList<>();
							for (Clube outro : clubes) {
								if (outro != clube) {
									candidatos.add(new TrocaDeTreinador.Candidato(outro.id(), outro.forca(), moral.get(outro), false));
								}
							}
							List<TrocaDeTreinador.Candidato> chamados = troca.chamados(clube.forca(), candidatos);
							if (chamados.isEmpty()) {
								semChamado[i]++;
								totalSemChamado++;
								moral.put(clube, depois);
								continue;
							}
							Clube chamado = clubes.get((int) chamados.get(0).clubeId() - 1);
							cedeu[clubes.indexOf(chamado)]++;
							somaDiferenca += clube.forca() - chamado.forca();
							moral.put(chamado, troca.moralDepoisDaTroca());
							demissoes[i]++;
							totalDemissoes++;
							demissoesPorRodada[rodada.numero()]++;
							if (naTemporada[i]++ == 0) {
								rodadaPrimeira[i] += rodada.numero();
							}
							depois = troca.moralDepoisDaTroca();
						}
						moral.put(clube, depois);
					}
				}
			}
			for (int i = 0; i < CLUBES; i++) {
				if (naTemporada[i] > 0) {
					temporadasComDemissao[i]++;
				}
			}
		}

		md.append("## ").append(titulo).append("\n\n");
		md.append("Trocas por temporada na divisão (média): **").append(f((double) totalDemissoes / TEMPORADAS, 1))
			.append("**; diferença média de força entre o clube que demitiu e o que cedeu o treinador: **")
			.append(f(totalDemissoes == 0 ? 0 : somaDiferenca / totalDemissoes, 1))
			.append("**. Jogos decisivos perdidos sem ninguém para chamar (o treinador fica): **")
			.append(f((double) totalSemChamado / TEMPORADAS, 1)).append("** por temporada.\n\n");
		md.append("| Força | Trocas por temporada | Temporadas com troca | Rodada da 1ª troca (média) ")
			.append("| Entradas no vermelho por temporada | Saídas do vermelho no jogo decisivo ")
			.append("| Decisivos perdidos sem chamado | Vezes que cedeu o treinador |\n");
		md.append("|---|---|---|---|---|---|---|---|\n");
		for (int i = 0; i < CLUBES; i++) {
			md.append("| ").append(clubes.get(i).forca())
				.append(" | ").append(f((double) demissoes[i] / TEMPORADAS, 2))
				.append(" | ").append(f(100.0 * temporadasComDemissao[i] / TEMPORADAS, 1)).append("%")
				.append(" | ").append(temporadasComDemissao[i] == 0 ? "–" : f((double) rodadaPrimeira[i] / temporadasComDemissao[i], 1))
				.append(" | ").append(f((double) entradasNoVermelho[i] / TEMPORADAS, 2))
				.append(" | ").append(entradasNoVermelho[i] == 0 ? "–" : f(100.0 * salvos[i] / entradasNoVermelho[i], 0) + "%")
				.append(" | ").append(f((double) semChamado[i] / TEMPORADAS, 2))
				.append(" | ").append(f((double) cedeu[i] / TEMPORADAS, 2))
				.append(" |\n");
		}
		md.append("\nTrocas por trecho da temporada (% do total): ");
		int rodadas = demissoesPorRodada.length - 1;
		List<String> trechos = new ArrayList<>();
		for (int inicio = 1; inicio <= rodadas; inicio += 10) {
			int fim = Math.min(rodadas, inicio + 9);
			long soma = 0;
			for (int k = inicio; k <= fim; k++) {
				soma += demissoesPorRodada[k];
			}
			trechos.add("rodadas " + inicio + "–" + fim + " " + f(totalDemissoes == 0 ? 0 : 100.0 * soma / totalDemissoes, 0) + "%");
		}
		md.append(String.join("; ", trechos)).append(".\n\n");
	}

	private static Escalacao escalacao(Clube clube) {
		List<Jogador> titulares = new ArrayList<>();
		long id = clube.id() * 100;
		for (Posicao posicao : Posicao.values()) {
			for (int i = 0; i < Formacao.F_4_4_2.quantidade(posicao); i++) {
				titulares.add(new Jogador(id, clube.nome() + "-" + id, posicao, clube.forca(), Temperamento.NORMAL, "BRA"));
				id++;
			}
		}
		return new Escalacao(Formacao.F_4_4_2, titulares);
	}

	private static String f(double valor, int casas) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%." + casas + "f", valor);
	}
}
