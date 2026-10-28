package br.com.managerfoot.temporada;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.PaisPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.TipoEvento;
import br.com.managerfoot.temporada.ParametrosEvolucao.Desfecho;

/**
 * Gera {@code docs/simulacoes/evolucao-forca.md}: várias temporadas seguidas das ligas do banco
 * padrão, com promoção e rebaixamento, aplicando a evolução de força depois de cada partida (SPEC
 * §6.1, rev. 54). Compara variantes dos parâmetros. Só clubes da máquina (escalam os mais fortes).
 * Executar com {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none -Dtest=RelatorioEvolucaoTest}.
 */
@Tag("simulacao")
class RelatorioEvolucaoTest {

	private static final int SEMENTES = 3;
	private static final int TEMPORADAS = 6;
	private static final Path SAIDA = Path.of("docs/simulacoes/evolucao-forca.md");

	@Test
	void gerarRelatorio() throws IOException {
		Map<String, ParametrosEvolucao> variantes = new LinkedHashMap<>();
		variantes.put("A · literal (perde 1/3/5; cresce 15%/8%/1,5%)", ParametrosEvolucao.literal());
		variantes.put("B · triplo do crescimento (45%/24%/4,5%), sem piso", variante(0.45, 0.24, 0.045, ParametrosEvolucao.SEM_PISO));
		variantes.put("C · dobro do crescimento (30%/16%/3%), piso força do clube − 5", variante(0.30, 0.16, 0.03, 5));
		variantes.put("D · triplo do crescimento, piso força do clube − 5", variante(0.45, 0.24, 0.045, 5));
		variantes.put("E · proposta: 2,5× o crescimento (37,5%/20%/3,75%), piso força do clube − 5", ParametrosEvolucao.padrao());

		GeradorBancoPadrao gerador = new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao());
		Map<PaisPadrao, GeradorBancoPadrao.PaisGerado> paises = new LinkedHashMap<>();
		PaisPadrao.TODOS.forEach(p -> paises.put(p, gerador.gerar(p)));

		StringBuilder md = new StringBuilder();
		md.append("# Simulação da evolução de força\n\n")
			.append("Gerado por `RelatorioEvolucaoTest` (ferramenta de desenvolvimento). As 6 ligas do banco padrão, ")
			.append(SEMENTES).append(" sementes cada, ").append(TEMPORADAS).append(" temporadas seguidas com promoção, ")
			.append("rebaixamento e fila. Só clubes da máquina, que escalam sempre os mais fortes. Depois de cada partida:\n\n")
			.append("- cada um dos que jogaram (titulares e quem entrou) perde 1 com chance = perda média ÷ 11;\n")
			.append("- cada reserva que não entrou e cada jogador fora da escalação ganha 1 com a chance do resultado, ")
			.append("se estiver abaixo do teto da divisão;\n")
			.append("- quem está acima do teto não cresce e perde 1 com 25% de chance por partida, além do resto;\n")
			.append("- com piso, o desgaste de quem jogou não leva abaixo da força cadastrada do clube − 5 (como o ")
			.append("\"nível base do clube\" do Elifoot 98);\n")
			.append("- a força fica entre 1 e 50. Sem mercado e sem caixa.\n\n")
			.append("*Em campo*: média dos 11 mais fortes de cada clube. *Plantel*: média dos 23. *Faixa*: o clube mais fraco ")
			.append("e o mais forte da divisão (média entre as sementes). Temporada 0 = início do jogo.\n\n");

		for (var variante : variantes.entrySet()) {
			Map<Integer, Map<Integer, Medida>> medidas = new TreeMap<>();
				for (var pais : paises.entrySet()) {
				for (int semente = 0; semente < SEMENTES; semente++) {
					simular(pais.getKey(), pais.getValue(), semente, new EvolucaoForca(variante.getValue()), medidas);
				}
			}
			md.append("## ").append(variante.getKey()).append("\n\n");
			md.append("| Divisão (teto) | Temporada | Em campo | Plantel | Faixa em campo | No teto |\n|---|---|---|---|---|---|\n");
			medidas.forEach((prioridade, porTemporada) -> porTemporada.forEach((t, m) -> md.append("| ").append(prioridade)
				.append("ª (").append(m.teto / m.n).append(") | ").append(t).append(" | ").append(dec(m.emCampo / m.n))
				.append(" | ").append(dec(m.plantel / m.n)).append(" | ").append(dec(m.menor / m.n)).append(" – ")
				.append(dec(m.maior / m.n)).append(" | ").append(pct(m.noTeto / m.n)).append(" |\n")));
			md.append("\n");
		}
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private static ParametrosEvolucao variante(double vitoria, double empate, double derrota, int piso) {
		return new ParametrosEvolucao(ParametrosEvolucao.porDesfecho(1, 3, 5), ParametrosEvolucao.porDesfecho(vitoria, empate, derrota),
				ParametrosEvolucao.porDesfecho(vitoria, empate, derrota), 0.25, piso);
	}

	private void simular(PaisPadrao pais, GeradorBancoPadrao.PaisGerado gerado, long semente, EvolucaoForca evolucao,
			Map<Integer, Map<Integer, Medida>> medidas) {
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
		medir(liga, planteis, 0, medidas);
		ProcessamentoRodada processamento = ProcessamentoRodada.padrao();
		SplittableRandom aleatorio = new SplittableRandom(semente * 31 + 7);
		Disciplina disciplina = Disciplina.vazia();
		Map<Long, Moral> moral = new HashMap<>();
		for (int numero = 1; numero <= TEMPORADAS; numero++) {
			Temporada temporada = Temporada.iniciar(numero, liga, planteis, aleatorio.nextLong());
			Map<Long, Integer> teto = new HashMap<>();
			liga.divisoes().forEach(d -> d.clubes().forEach(c -> teto.put(c.id(), d.limiteForca())));
			Map<Integer, List<ResultadoJogo>> resultados = new HashMap<>();
			int rodadas = temporada.divisoes().stream().mapToInt(d -> d.calendario().rodadas().size()).max().orElse(0);
			for (int r = 1; r <= rodadas; r++) {
				RodadaAoVivo aoVivo = processamento.preparar(temporada, r, planteis, disciplina, Map.of(), List.of());
				ProcessamentoRodada.Resultado res = aoVivo.finalizar(moral);
				for (int i = 0; i < res.partidas().size(); i++) {
					RodadaAoVivo.PartidaDaRodada partida = aoVivo.partidas().get(i);
					ProcessamentoRodada.PartidaJogada jogada = res.partidas().get(i);
					int gc = jogada.resultado().golsCasa();
					int gf = jogada.resultado().golsFora();
					resultados.computeIfAbsent(jogada.prioridade(), k -> new ArrayList<>())
						.add(new ResultadoJogo(jogada.jogo(), gc, gf));
					evoluir(evolucao, planteis, partida.jogo().mandante(), partida.casa(), Lado.CASA, jogada, Desfecho.de(gc, gf),
							teto, aleatorio);
					evoluir(evolucao, planteis, partida.jogo().visitante(), partida.fora(), Lado.FORA, jogada,
							Desfecho.de(gf, gc), teto, aleatorio);
				}
				disciplina = res.disciplina();
				moral = new HashMap<>(res.moral());
			}
			Map<Integer, Classificacao> finais = new HashMap<>();
			for (Divisao d : liga.divisoes()) {
				finais.put(d.prioridade(), Classificacao.calcular(d.clubes(), resultados.get(d.prioridade()), temporada.semente()));
			}
			medir(liga, planteis, numero, medidas);
			liga = TransicaoTemporada.aplicar(liga, finais, aleatorio).proxima();
			disciplina = disciplina.novaTemporada();
		}
	}

	private static void evoluir(EvolucaoForca evolucao, Map<Long, List<Jogador>> planteis, Clube clube, Escalacao escalacao,
			Lado lado, ProcessamentoRodada.PartidaJogada jogada, Desfecho desfecho, Map<Long, Integer> teto,
			SplittableRandom aleatorio) {
		Set<Long> jogaram = new HashSet<>();
		escalacao.titulares().forEach(j -> jogaram.add(j.id()));
		for (EventoPartida e : jogada.resultado().eventos()) {
			if (e.tipo() == TipoEvento.SUBSTITUICAO && e.lado() == lado) {
				jogaram.add(e.jogador().id());
			}
		}
		Set<Long> banco = new HashSet<>();
		escalacao.reservas().forEach(j -> banco.add(j.id()));
		banco.removeAll(jogaram);
		planteis.put(clube.id(), evolucao.aposPartida(planteis.get(clube.id()), jogaram, banco, desfecho, teto.get(clube.id()),
				clube.forca(), aleatorio));
	}

	private static void medir(ConfiguracaoLiga liga, Map<Long, List<Jogador>> planteis, int temporada,
			Map<Integer, Map<Integer, Medida>> medidas) {
		for (Divisao d : liga.divisoes()) {
			Medida m = medidas.computeIfAbsent(d.prioridade(), k -> new TreeMap<>()).computeIfAbsent(temporada, k -> new Medida());
			double soma = 0;
			double somaPlantel = 0;
			double menor = Double.MAX_VALUE;
			double maior = 0;
			double noTeto = 0;
			double jogadores = 0;
			for (Clube c : d.clubes()) {
				List<Jogador> p = planteis.get(c.id());
				double emCampo = p.stream().map(Jogador::forca).sorted(Comparator.reverseOrder()).limit(11)
					.mapToInt(Integer::intValue).average().orElse(0);
				soma += emCampo;
				somaPlantel += p.stream().mapToInt(Jogador::forca).average().orElse(0);
				menor = Math.min(menor, emCampo);
				maior = Math.max(maior, emCampo);
				noTeto += p.stream().filter(j -> j.forca() >= d.limiteForca()).count();
				jogadores += p.size();
			}
			int n = d.clubes().size();
			m.n++;
			m.teto += d.limiteForca();
			m.emCampo += soma / n;
			m.plantel += somaPlantel / n;
			m.menor += menor;
			m.maior += maior;
			m.noTeto += noTeto / jogadores;
		}
	}

	private static String dec(double v) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", v);
	}

	private static String pct(double v) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%.0f%%", v * 100);
	}

	private static final class Medida {
		int n;
		int teto;
		double emCampo;
		double plantel;
		double menor;
		double maior;
		double noTeto;
	}
}
