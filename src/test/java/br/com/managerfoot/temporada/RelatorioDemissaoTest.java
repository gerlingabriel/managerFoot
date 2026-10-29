package br.com.managerfoot.temporada;

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
import java.util.Set;
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
 * Gera {@code docs/simulacoes/demissao.md} (rev. 61): demissões pela moral ({@link Moral#demite}) e
 * trocas de treinador ({@link TrocaDeTreinador}) num país de 2 divisões, com a regra valendo para todos
 * os clubes, e o efeito do ajuste da moral pela força do adversário
 * ({@link ParametrosMoral#diferencaPorPonto()}). Depois de cada rodada, do clube mais forte ao mais
 * fraco, quem é demitido chama o treinador de um clube do país; os dois clubes voltam à moral inicial e
 * não entram em outra troca na mesma rodada. Sem chamado, o treinador fica. Simplificação: cada clube
 * joga em 4-4-2 com os 11 na força do clube, sem evolução de força, mercado nem acesso. Executar com
 * {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none -Dtest=RelatorioDemissaoTest}.
 */
@Tag("simulacao")
class RelatorioDemissaoTest {

	private static final int TEMPORADAS = 100;
	private static final int CLUBES = 20;
	private static final Path SAIDA = Path.of("docs/simulacoes/demissao.md");

	private final MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());

	/** Resultado de uma variante, por temporada. */
	private record Medidas(double trocas1, double trocas2, double semChamado, double daSegunda, double vermelhoFim1,
			double vermelhoFim2, double diferenca, int[] trocasPorClube, int[] cedeuPorClube, int[] semChamadoPorClube) {
	}

	@Test
	void gerarRelatorio() throws IOException {
		List<Clube> primeira = divisao(1, 50, 35);
		List<Clube> segunda = divisao(101, 30, 22);
		ParametrosMoral padrao = ParametrosMoral.padrao();

		StringBuilder md = new StringBuilder();
		md.append("# Demissão pela moral e troca de treinador\n\n");
		md.append("Gerado por `RelatorioDemissaoTest` (ferramenta de desenvolvimento). País com 2 divisões de ")
			.append(CLUBES).append(" clubes: 1ª com forças de 50 a 35 e 2ª de 30 a 22 (como o banco padrão), turno e ")
			.append("returno, ").append(TEMPORADAS).append(" temporadas (sementes 0 a ").append(TEMPORADAS - 1)
			.append("). Cada clube joga em 4-4-2 com os 11 na força do clube, sem evolução de força, mercado nem acesso; ")
			.append("toda temporada começa com moral ").append(padrao.inicial()).append(".\n\n");
		md.append("**Regras (SPEC §5.4 e §6.2, rev. 61):** verde de ").append(padrao.limiteVerde()).append(" a ")
			.append(padrao.maximo()).append(", amarelo de ").append(padrao.limiteAmarelo()).append(" a ")
			.append(padrao.limiteVerde() - 1).append(", vermelho de ").append(padrao.minimo()).append(" a ")
			.append(padrao.limiteAmarelo() - 1).append(". Ao entrar no vermelho, o jogo seguinte é decisivo; se a moral ")
			.append("continua vermelha, o treinador é demitido — máquina e usuário (aqui todos são máquina). Depois da ")
			.append("rodada, do clube mais forte ao mais fraco, quem foi demitido chama o treinador de um clube do país ")
			.append("de força inferior, no máximo 12 abaixo, com moral verde (o mais forte deles). Os dois trocam de clube ")
			.append("e os dois clubes voltam a ").append(padrao.inicial())
			.append("; nenhum dos dois entra em outra troca na mesma rodada. Sem chamado, o treinador fica e o jogo ")
			.append("seguinte no vermelho é decisivo de novo.\n\n");
		md.append("**Ajuste pela força do adversário (proposta da rev. 61):** a surpresa vale mais. A cada *D* pontos ")
			.append("de diferença de força, 1 ponto, no máximo *M*: o favorito que empata ou perde perde esse tanto a ")
			.append("mais; o azarão que empata ou vence ganha esse tanto a mais; o resultado esperado vale o normal. ")
			.append("Ex. com D = 3, M = 3: o clube de força 50 que perde em casa para um de 41 perde 3 + 3 = 6; o de 41 que ")
			.append("vence fora ganha 3 + 3 = 6; se o de 50 vence, os dois ficam com o normal (+2 e −2).\n\n");
		md.append("## Comparação\n\n");
		md.append("Por temporada. *Trocas*: demissões que acharam quem chamar. *Decisivos sem chamado*: jogos ")
			.append("decisivos perdidos sem ninguém para chamar (o treinador fica). *Chamado da 2ª*: % das trocas da 1ª ")
			.append("em que o treinador veio da 2ª. *Vermelho no fim*: % dos clubes no vermelho na última rodada.\n\n");
		md.append("| D | M | Trocas 1ª | Trocas 2ª | Decisivos sem chamado | Chamado da 2ª | Diferença de força na troca | Vermelho no fim 1ª | Vermelho no fim 2ª |\n");
		md.append("|---|---|---|---|---|---|---|---|---|\n");
		double[][] variantes = { { 0, 0 }, { 5, 2 }, { 4, 2 }, { 4, 3 }, { 3, 3 }, { 3, 4 }, { 2, 4 } };
		for (double[] v : variantes) {
			ParametrosMoral p = comAjuste(padrao, v[0], (int) v[1]);
			Medidas m = simular(primeira, segunda, p);
			boolean escolhida = p.equals(padrao);
			md.append("| ").append(escolhida ? "**" : "").append(v[0] == 0 ? "sem ajuste" : f(v[0], 0))
				.append(escolhida ? "**" : "").append(" | ").append((int) v[1])
				.append(" | ").append(f(m.trocas1(), 1)).append(" | ").append(f(m.trocas2(), 1))
				.append(" | ").append(f(m.semChamado(), 1)).append(" | ").append(f(m.daSegunda(), 0)).append("%")
				.append(" | ").append(f(m.diferenca(), 1))
				.append(" | ").append(f(m.vermelhoFim1(), 0)).append("% | ").append(f(m.vermelhoFim2(), 0)).append("% |\n");
		}

		Medidas escolhida = simular(primeira, segunda, padrao);
		md.append("\n## Por clube, com os valores do jogo (D = ").append(f(padrao.diferencaPorPonto(), 0)).append(", M = ")
			.append(padrao.ajusteMaximo()).append(")\n\n");
		md.append("| Divisão | Força | Demitido e trocado por temporada | Decisivos sem chamado | Cedeu o treinador (subiu) |\n");
		md.append("|---|---|---|---|---|\n");
		List<Clube> todos = new ArrayList<>(primeira);
		todos.addAll(segunda);
		for (int i = 0; i < todos.size(); i++) {
			md.append("| ").append(i < CLUBES ? "1ª" : "2ª").append(" | ").append(todos.get(i).forca())
				.append(" | ").append(f((double) escolhida.trocasPorClube()[i] / TEMPORADAS, 2))
				.append(" | ").append(f((double) escolhida.semChamadoPorClube()[i] / TEMPORADAS, 2))
				.append(" | ").append(f((double) escolhida.cedeuPorClube()[i] / TEMPORADAS, 2)).append(" |\n");
		}

		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private Medidas simular(List<Clube> primeira, List<Clube> segunda, ParametrosMoral parametros) {
		TrocaDeTreinador troca = new TrocaDeTreinador(parametros, 3, 10, 12);
		List<Clube> todos = new ArrayList<>(primeira);
		todos.addAll(segunda);
		Map<Clube, Escalacao> escalacoes = new HashMap<>();
		todos.forEach(c -> escalacoes.put(c, escalacao(c)));
		int[] trocasPorClube = new int[todos.size()];
		int[] cedeu = new int[todos.size()];
		int[] semChamadoPorClube = new int[todos.size()];
		long trocas1 = 0;
		long trocas2 = 0;
		long semChamado = 0;
		long daSegunda = 0;
		long vermelho1 = 0;
		long vermelho2 = 0;
		double diferenca = 0;

		for (long t = 0; t < TEMPORADAS; t++) {
			SplittableRandom aleatorio = new SplittableRandom(t);
			List<Rodada> rodadas1 = Calendario.sorteado(primeira, aleatorio).rodadas();
			List<Rodada> rodadas2 = Calendario.sorteado(segunda, aleatorio).rodadas();
			Map<Clube, Moral> moral = new HashMap<>();
			todos.forEach(c -> moral.put(c, Moral.inicial(parametros)));
			for (int r = 0; r < rodadas1.size(); r++) {
				Map<Clube, Moral> antes = new HashMap<>(moral);
				List<Jogo> jogos = new ArrayList<>(rodadas1.get(r).jogos());
				jogos.addAll(rodadas2.get(r).jogos());
				for (Jogo jogo : jogos) {
					ResultadoPartida res = motor.simular(escalacoes.get(jogo.mandante()), escalacoes.get(jogo.visitante()),
							aleatorio.nextLong());
					Clube casa = jogo.mandante();
					Clube fora = jogo.visitante();
					moral.put(casa, moral.get(casa).aposJogo(true, res.golsCasa(), res.golsFora(), casa.forca(), fora.forca(),
							parametros));
					moral.put(fora, moral.get(fora).aposJogo(false, res.golsFora(), res.golsCasa(), fora.forca(), casa.forca(),
							parametros));
				}
				Set<Clube> trocados = new HashSet<>();
				List<Clube> ordem = todos.stream().sorted(Comparator.comparingInt(Clube::forca).reversed()).toList();
				for (Clube clube : ordem) {
					if (trocados.contains(clube) || !antes.get(clube).demite(moral.get(clube), parametros)) {
						continue;
					}
					List<TrocaDeTreinador.Candidato> candidatos = todos.stream()
						.filter(c -> c != clube && !trocados.contains(c))
						.map(c -> new TrocaDeTreinador.Candidato(c.id(), c.forca(), moral.get(c), false))
						.toList();
					List<TrocaDeTreinador.Candidato> chamados = troca.chamados(clube.forca(), candidatos);
					int i = todos.indexOf(clube);
					if (chamados.isEmpty()) {
						semChamado++;
						semChamadoPorClube[i]++;
						continue;
					}
					Clube chamado = todos.stream().filter(c -> c.id() == chamados.get(0).clubeId()).findFirst().orElseThrow();
					int k = todos.indexOf(chamado);
					trocasPorClube[i]++;
					cedeu[k]++;
					diferenca += clube.forca() - chamado.forca();
					if (i < CLUBES) {
						trocas1++;
						if (k >= CLUBES) {
							daSegunda++;
						}
					} else {
						trocas2++;
					}
					moral.put(clube, troca.moralDepoisDaTroca());
					moral.put(chamado, troca.moralDepoisDaTroca());
					trocados.add(clube);
					trocados.add(chamado);
				}
			}
			for (int i = 0; i < todos.size(); i++) {
				if (moral.get(todos.get(i)).faixa(parametros) == Moral.Faixa.VERMELHO) {
					if (i < CLUBES) {
						vermelho1++;
					} else {
						vermelho2++;
					}
				}
			}
		}
		long trocas = trocas1 + trocas2;
		return new Medidas((double) trocas1 / TEMPORADAS, (double) trocas2 / TEMPORADAS, (double) semChamado / TEMPORADAS,
				trocas1 == 0 ? 0 : 100.0 * daSegunda / trocas1, 100.0 * vermelho1 / (TEMPORADAS * CLUBES),
				100.0 * vermelho2 / (TEMPORADAS * CLUBES), trocas == 0 ? 0 : diferenca / trocas, trocasPorClube, cedeu,
				semChamadoPorClube);
	}

	/** 20 clubes com forças espalhadas de {@code maior} a {@code menor}. */
	private static List<Clube> divisao(long primeiroId, int maior, int menor) {
		List<Clube> clubes = new ArrayList<>();
		for (int i = 0; i < CLUBES; i++) {
			int forca = (int) Math.round(maior - (double) (maior - menor) * i / (CLUBES - 1));
			clubes.add(new Clube(primeiroId + i, "F" + forca + "-" + i, forca));
		}
		return clubes;
	}

	private static ParametrosMoral comAjuste(ParametrosMoral p, double diferencaPorPonto, int ajusteMaximo) {
		return new ParametrosMoral(p.minimo(), p.maximo(), p.inicial(), p.vitoriaEmCasa(), p.vitoriaFora(),
				p.empateEmCasa(), p.empateFora(), p.derrotaEmCasa(), p.derrotaFora(), p.limiteVerde(), p.limiteAmarelo(),
				p.divisorRetorno(), diferencaPorPonto, ajusteMaximo);
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
