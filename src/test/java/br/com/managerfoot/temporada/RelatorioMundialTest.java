package br.com.managerfoot.temporada;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.bancopadrao.Regiao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.motor.CondicoesPartida;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.JogoDecisivo;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.MotorPartida;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.RegrasPartida;
import br.com.managerfoot.motor.ResultadoPartida;

/**
 * Gera {@code docs/simulacoes/mundial.md}: quem ganha o Mundial (SPEC §8.3.4) com os níveis de força do banco padrão.
 * O campeão continental é aproximado pela escolha por força entre os mais fortes da região dele, sem jogar a
 * continental, e os planteis são os iniciais, sem a evolução de força.
 * Executar com {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none -Dtest=RelatorioMundialTest}.
 */
@Tag("simulacao")
class RelatorioMundialTest {

	private static final int MUNDIAIS = 5000;
	private static final int CLUBES_POR_REGIAO = 8;
	private static final Path SAIDA = Path.of("docs/simulacoes/mundial.md");

	private final MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());
	private final EscalacaoMaquina escalacao = new EscalacaoMaquina(ParametrosMotor.padrao());
	private final Map<Long, Regiao> regiaoDoClube = new HashMap<>();
	private final Map<Long, List<Jogador>> planteis = new HashMap<>();
	private final Map<Regiao, List<Clube>> maisFortes = new EnumMap<>(Regiao.class);

	@Test
	void gerarRelatorio() throws IOException {
		carregarBanco();
		StringBuilder md = new StringBuilder("# Simulação do Mundial de Clubes\n\nGerado por `RelatorioMundialTest` (ferramenta de ")
			.append("desenvolvimento), com semente fixa. ").append(MUNDIAIS).append(" Mundiais em cada cenário, com os ")
			.append(CLUBES_POR_REGIAO).append(" clubes mais fortes de cada região e a escalação da máquina. O campeão continental é ")
			.append("escolhido por força entre os mais fortes da região dele, sem jogar a continental, e os planteis são os ")
			.append("iniciais, sem a evolução de força. Não mede, portanto, o efeito de uma temporada inteira.\n\n")
			.append("## Força em campo dos clubes que podem disputar\n\n| Região | Mais forte | Média | Mais fraco |\n|---|---|---|---|\n");
		for (Regiao regiao : Regiao.values()) {
			List<Double> forcas = maisFortes.get(regiao).stream().map(c -> Clube.forcaEmCampo(planteis.get(c.id()))).toList();
			md.append(String.format(Locale.ROOT, "| %s | %.1f | %.1f | %.1f |%n", regiao,
					forcas.stream().mapToDouble(d -> d).max().orElseThrow(), forcas.stream().mapToDouble(d -> d).average().orElseThrow(),
					forcas.stream().mapToDouble(d -> d).min().orElseThrow()));
		}
		md.append('\n');
		md.append(cenario(Regiao.AMERICA_DO_SUL, 11));
		md.append(cenario(Regiao.EUROPA, 23));
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md.toString());
		System.out.println(md);
	}

	private void carregarBanco() {
		GeradorBancoPadrao.BancoGerado banco = new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco();
		Map<Regiao, List<Clube>> porRegiao = new EnumMap<>(Regiao.class);
		long[] base = { 0 };
		banco.paises().forEach(p -> registrar(p.clubes(), p.pais().codigo(), base, porRegiao));
		banco.estrangeiros().forEach(e -> registrar(e.clubes(), e.pais().codigo(), base, porRegiao));
		porRegiao.forEach((regiao, clubes) -> maisFortes.put(regiao, clubes.stream()
			.sorted(Comparator.comparingInt(Clube::forca).reversed().thenComparingLong(Clube::id))
			.limit(CLUBES_POR_REGIAO).toList()));
	}

	private void registrar(List<GeradorBancoPadrao.ClubeGerado> clubes, String codigo, long[] base, Map<Regiao, List<Clube>> porRegiao) {
		Regiao regiao = GeradorBancoPadrao.regiaoDe(codigo);
		for (GeradorBancoPadrao.ClubeGerado gerado : clubes) {
			long id = ++base[0];
			Clube clube = new Clube(id, gerado.clube().nome(), gerado.clube().forca());
			regiaoDoClube.put(id, regiao);
			planteis.put(id, gerado.jogadores().stream()
				.map(j -> new Jogador(id * 1000 + j.id(), j.nome(), j.posicao(), j.forca(), j.temperamento(), j.asterisco(), j.nacionalidade()))
				.toList());
			porRegiao.computeIfAbsent(regiao, r -> new ArrayList<>()).add(clube);
		}
	}

	/** O país jogado fica nesta região: o representante dela é o campeão continental. */
	private String cenario(Regiao regiaoJogada, long semente) {
		double expoente = ParametrosContinental.padrao().expoenteForca();
		SplittableRandom aleatorio = new SplittableRandom(semente);
		Map<Regiao, Integer> titulos = new EnumMap<>(Regiao.class);
		Map<Regiao, Integer> finais = new EnumMap<>(Regiao.class);
		Map<Regiao, Integer> participacoes = new EnumMap<>(Regiao.class);
		int jogos = 0;
		int gols = 0;
		int prorrogacoes = 0;
		int penaltis = 0;
		int zebras = 0;
		for (int m = 0; m < MUNDIAIS; m++) {
			Clube campeaoContinental = VagasContinental.porForca(maisFortes.get(regiaoJogada), planteis, 1, expoente, aleatorio).getFirst();
			Mundial mundial = Mundial.sortear(campeaoContinental, regiaoJogada, maisFortes, planteis, expoente, aleatorio).orElseThrow();
			for (FaseCopa fase : Mundial.ETAPAS) {
				if (fase == FaseCopa.FINAL) {
					mundial = mundial.comFinal();
				}
				for (Mundial.Confronto confronto : mundial.confrontos(fase)) {
					ResultadoPartida r = motor.simular(RegrasPartida.padrao(), escalacao.escalar(planteis.get(confronto.primeiro().id())),
							escalacao.escalar(planteis.get(confronto.segundo().id())), CondicoesPartida.NEUTRAS.comDecisivo(JogoDecisivo.FINAL),
							aleatorio.nextLong());
					PlacarCopa placar = new PlacarCopa(r.golsCasa(), r.golsFora(), r.prorrogacao(), r.penaltis()
						.map(d -> new PlacarCopa.Penaltis(d.gols(Lado.CASA), d.gols(Lado.FORA))));
					mundial = mundial.comPlacar(fase, confronto.ordem(), placar);
					jogos++;
					gols += r.golsCasa() + r.golsFora();
					prorrogacoes += r.prorrogacao() ? 1 : 0;
					penaltis += r.penaltis().isPresent() ? 1 : 0;
					Mundial.Confronto jogado = mundial.confrontos(fase).get(fase == FaseCopa.FINAL ? 0 : confronto.ordem());
					double vencedor = Clube.forcaEmCampo(planteis.get(jogado.vencedor().orElseThrow().id()));
					double perdedor = Clube.forcaEmCampo(planteis.get(jogado.perdedor().orElseThrow().id()));
					zebras += vencedor < perdedor ? 1 : 0;
					if (fase == FaseCopa.SEMIFINAL) {
						participacoes.merge(regiaoDoClube.get(confronto.primeiro().id()), 1, Integer::sum);
						participacoes.merge(regiaoDoClube.get(confronto.segundo().id()), 1, Integer::sum);
					}
				}
			}
			finais.merge(regiaoDoClube.get(mundial.campeao().orElseThrow().id()), 1, Integer::sum);
			finais.merge(regiaoDoClube.get(mundial.vice().orElseThrow().id()), 1, Integer::sum);
			titulos.merge(regiaoDoClube.get(mundial.campeao().orElseThrow().id()), 1, Integer::sum);
		}
		StringBuilder md = new StringBuilder("## País jogado em ").append(regiaoJogada).append("\n\n")
			.append("| Região | Participou | Chegou à final, das vezes que participou | Títulos | Título, das vezes que participou |\n|---|---|---|---|---|\n");
		for (Regiao regiao : Regiao.values()) {
			int participou = participacoes.getOrDefault(regiao, 0);
			md.append(String.format(Locale.ROOT, "| %s | %.1f%% | %.1f%% | %.1f%% | %.1f%% |%n", regiao, pct(participou, MUNDIAIS),
					pct(finais.getOrDefault(regiao, 0), participou), pct(titulos.getOrDefault(regiao, 0), MUNDIAIS),
					pct(titulos.getOrDefault(regiao, 0), participou)));
		}
		md.append(String.format(Locale.ROOT, "%n| Jogos | Gols por jogo | Com prorrogação | Nos pênaltis | Venceu o mais fraco em campo |%n|---|---|---|---|---|%n| %d | %.2f | %.1f%% | %.1f%% | %.1f%% |%n%n",
				jogos, (double) gols / jogos, pct(prorrogacoes, jogos), pct(penaltis, jogos), pct(zebras, jogos)));
		return md.toString();
	}

	private static double pct(int parte, int total) {
		return total == 0 ? 0 : 100.0 * parte / total;
	}
}
