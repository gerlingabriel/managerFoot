package br.com.managerfoot.temporada;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.PaisPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.GravidadeLesao;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.ParametrosLesao;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.RegrasPartida;
import br.com.managerfoot.motor.TipoEvento;

/**
 * Gera {@code docs/simulacoes/lesoes.md}: frequência, gravidade e efeito das lesões (SPEC §7.5) numa
 * temporada inteira de cada país do banco padrão, com a evolução de força e o limite de 2 por clube.
 * Executar com {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none -Dtest=RelatorioLesoesTest}.
 */
@Tag("simulacao")
class RelatorioLesoesTest {

	private static final int SEMENTES = 10;
	private static final Path SAIDA = Path.of("docs/simulacoes/lesoes.md");
	/** Lesões por clube numa temporada de 38 rodadas, antes do limite. */
	private static final double[] FREQUENCIAS = { 1.0, 1.5, 2.0, 3.0 };

	@Test
	void gerarRelatorio() throws IOException {
		GeradorBancoPadrao gerador = new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao());
		List<GeradorBancoPadrao.PaisGerado> gerados = PaisPadrao.TODOS.stream().map(gerador::gerar).toList();
		StringBuilder md = new StringBuilder();
		md.append("# Simulação de lesões\n\n")
			.append("Gerado por `RelatorioLesoesTest` (ferramenta de desenvolvimento). Uma temporada de cada país do ")
			.append("banco padrão (6 países, 2 divisões) por semente, sementes 0 a ").append(SEMENTES - 1)
			.append("; escalação da máquina para todos, evolução de força ligada, limite de ")
			.append(ParametrosLesao.padrao().limitePorTemporada()).append(" lesões por clube na temporada (SPEC §7.5).\n\n")
			.append("*Frequência* é a taxa por minuto expressa em lesões por clube numa temporada de 38 rodadas, ")
			.append("sem o limite; a 2ª divisão tem menos rodadas.\n\n")
			.append("| Frequência | Lesões por clube | Clubes com 0 / 1 / 2 | Leve / média / grave | Jogos fora por lesão ")
			.append("| Jogos sem o lesionado por clube | Força perdida por clube | Jogos com time incompleto |\n")
			.append("|---|---|---|---|---|---|---|---|\n");
		Resultado detalhado = null;
		for (double frequencia : FREQUENCIAS) {
			Resultado r = new Resultado();
			for (int p = 0; p < gerados.size(); p++) {
				for (long semente = 0; semente < SEMENTES; semente++) {
					simular(PaisPadrao.TODOS.get(p), gerados.get(p), semente, frequencia, r);
				}
			}
			md.append(r.linha(frequencia));
			if (frequencia == 1.5) {
				detalhado = r;
			}
		}
		md.append("\n*Jogos com time incompleto*: partidas em que um clube tinha menos de 11 disponíveis ")
			.append("(lesionados e suspensos) e jogou com um a menos (SPEC §9.3).\n\n")
			.append(detalhado.temperamentos());
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private static void simular(PaisPadrao pais, GeradorBancoPadrao.PaisGerado gerado, long semente, double frequencia,
			Resultado r) {
		ParametrosLesao padrao = ParametrosLesao.padrao();
		ParametrosMotor motor = ParametrosMotor.padrao()
			.comLesao(new ParametrosLesao(frequencia / (38 * 90), padrao.pesosTemperamento(), padrao.gravidades(),
					padrao.limitePorTemporada()));
		ProcessamentoRodada processamento = new ProcessamentoRodada(motor, RegrasPartida.padrao(),
				ParametrosSuspensao.padrao(), ParametrosMoral.padrao());
		EvolucaoForca evolucao = EvolucaoForca.padrao();
		Map<Long, List<Jogador>> planteis = new HashMap<>();
		List<Clube> clubes = new ArrayList<>();
		for (GeradorBancoPadrao.ClubeGerado c : gerado.clubes()) {
			clubes.add(c.clube());
			planteis.put(c.clube().id(), c.jogadores().stream()
				.map(j -> new Jogador(c.clube().id() * 100 + j.id(), j.nome(), j.posicao(), j.forca(), j.temperamento(),
						j.asterisco(), j.nacionalidade()))
				.toList());
		}
		planteis.values().forEach(l -> l.forEach(j -> r.noPlantel.merge(j.temperamento(), 1, Integer::sum)));
		ConfiguracaoLiga liga = pais.formato().distribuir(clubes, new SplittableRandom(semente));
		Map<Long, Integer> teto = new HashMap<>();
		liga.divisoes().forEach(d -> d.clubes().forEach(c -> teto.put(c.id(), d.limiteForca())));
		SplittableRandom aleatorio = new SplittableRandom(semente * 977 + 3);
		Temporada temporada = Temporada.iniciar(1, liga, planteis, aleatorio.nextLong());
		Disciplina disciplina = Disciplina.vazia();
		Map<Long, Moral> moral = new HashMap<>();
		Map<Long, Integer> forcaInicial = new HashMap<>();
		planteis.values().forEach(l -> l.forEach(j -> forcaInicial.put(j.id(), j.forca())));
		Map<Long, Integer> perdaPorLesao = new HashMap<>();
		int rodadas = temporada.divisoes().stream().mapToInt(d -> d.calendario().rodadas().size()).max().orElse(0);
		for (int rodada = 1; rodada <= rodadas; rodada++) {
			RodadaAoVivo aoVivo = processamento.preparar(temporada, rodada, planteis, disciplina, Map.of(), List.of());
			ProcessamentoRodada.Resultado res = aoVivo.finalizar(moral);
			for (int i = 0; i < res.partidas().size(); i++) {
				RodadaAoVivo.PartidaDaRodada partida = aoVivo.partidas().get(i);
				for (Lado lado : Lado.values()) {
					Clube c = partida.clube(lado);
					if ((lado == Lado.CASA ? partida.casa() : partida.fora()).incompleta()) {
						r.incompletos++;
					}
					Disciplina antes = disciplina;
					long lesionados = planteis.get(c.id()).stream().filter(j -> antes.lesionado(j.id())).count();
					r.jogosSemLesionado += lesionados;
					planteis.put(c.id(), evolucao.aposPartida(planteis.get(c.id()),
							lado == Lado.CASA ? partida.casa() : partida.fora(), res.partidas().get(i).resultado(), lado,
							teto.get(c.id()), c.forca(), aleatorio));
				}
				for (EventoPartida e : res.partidas().get(i).resultado().eventos()) {
					if (e.tipo() == TipoEvento.LESAO) {
						r.porGravidade.merge(e.lesao().gravidade(), 1, Integer::sum);
						r.jogosFora += e.lesao().jogos();
						r.porTemperamento.merge(e.jogador().temperamento(), 1, Integer::sum);
						perdaPorLesao.merge(partida.clube(e.lado()).id(), e.lesao().perdaForca(), Integer::sum);
					}
				}
			}
			disciplina = res.disciplina();
			moral = new HashMap<>(res.moral());
		}
		for (Clube c : clubes) {
			int lesoes = disciplina.lesoesDoClube(c.id());
			r.clubes++;
			r.lesoes += lesoes;
			r.porQuantidade[Math.min(lesoes, 2)]++;
			r.forcaPerdida += perdaPorLesao.getOrDefault(c.id(), 0);
		}
	}

	private static final class Resultado {
		int clubes;
		int lesoes;
		int[] porQuantidade = new int[3];
		Map<GravidadeLesao, Integer> porGravidade = new EnumMap<>(GravidadeLesao.class);
		Map<Temperamento, Integer> porTemperamento = new EnumMap<>(Temperamento.class);
		Map<Temperamento, Integer> noPlantel = new EnumMap<>(Temperamento.class);
		long jogosFora;
		long jogosSemLesionado;
		long forcaPerdida;
		int incompletos;

		String linha(double frequencia) {
			int total = Math.max(1, lesoes);
			return String.format(Locale.forLanguageTag("pt-BR"),
					"| %.1f | %.2f | %s / %s / %s | %s / %s / %s | %.1f | %.1f | %.1f | %d |%n", frequencia,
					lesoes / (double) clubes, pct(porQuantidade[0], clubes), pct(porQuantidade[1], clubes),
					pct(porQuantidade[2], clubes), pct(porGravidade.getOrDefault(GravidadeLesao.LEVE, 0), total),
					pct(porGravidade.getOrDefault(GravidadeLesao.MEDIA, 0), total),
					pct(porGravidade.getOrDefault(GravidadeLesao.GRAVE, 0), total), jogosFora / (double) total,
					jogosSemLesionado / (double) clubes, forcaPerdida / (double) clubes, incompletos);
		}

		String temperamentos() {
			StringBuilder md = new StringBuilder("## Quem se lesiona (frequência 1,5)\n\n")
				.append("| Temperamento | No plantel | Nas lesões |\n|---|---|---|\n");
			int plantel = noPlantel.values().stream().mapToInt(Integer::intValue).sum();
			int lesoesTotal = Math.max(1, porTemperamento.values().stream().mapToInt(Integer::intValue).sum());
			for (Temperamento t : Temperamento.values()) {
				md.append("| ").append(t.name().charAt(0)).append(t.name().substring(1).toLowerCase(Locale.ROOT))
					.append(" | ").append(pct(noPlantel.getOrDefault(t, 0), plantel)).append(" | ")
					.append(pct(porTemperamento.getOrDefault(t, 0), lesoesTotal)).append(" |\n");
			}
			return md.toString();
		}

		private static String pct(int parte, int total) {
			return Math.round(100.0 * parte / Math.max(1, total)) + "%";
		}
	}
}
