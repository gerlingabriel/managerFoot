package br.com.managerfoot.temporada;

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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.PaisPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.motor.CondicoesPartida;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.MotorPartida;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.RegrasPartida;
import br.com.managerfoot.motor.ResultadoPartida;

/**
 * Gera {@code docs/simulacoes/copa.md}: prorrogações, pênaltis e zebras da copa nacional (SPEC §8.2)
 * com o banco padrão e o motor atual, sem a evolução de força nem os cartões da liga entre as fases.
 * Executar com {@code ./mvnw test -Dgroups=simulacao -Dsurefire.excludedGroups=none -Dtest=RelatorioCopaTest}.
 */
@Tag("simulacao")
class RelatorioCopaTest {

	private static final int SEMENTES = 200;
	private static final Path SAIDA = Path.of("docs/simulacoes/copa.md");

	@Test
	void gerarRelatorio() throws IOException {
		GeradorBancoPadrao gerador = new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao());
		MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());
		EscalacaoMaquina escalacao = new EscalacaoMaquina(ParametrosMotor.padrao());
		Resultado r = new Resultado();
		for (PaisPadrao pais : PaisPadrao.TODOS) {
			GeradorBancoPadrao.PaisGerado gerado = gerador.gerar(pais);
			List<Clube> clubes = new ArrayList<>();
			Map<Long, List<Jogador>> planteis = new HashMap<>();
			for (GeradorBancoPadrao.ClubeGerado c : gerado.clubes()) {
				clubes.add(c.clube());
				planteis.put(c.clube().id(), c.jogadores().stream()
					.map(j -> new Jogador(c.clube().id() * 100 + j.id(), j.nome(), j.posicao(), j.forca(),
							j.temperamento(), j.asterisco(), j.nacionalidade()))
					.toList());
			}
			for (long semente = 0; semente < SEMENTES; semente++) {
				SplittableRandom aleatorio = new SplittableRandom(semente * 31 + 7);
				ConfiguracaoLiga liga = pais.formato().distribuir(clubes, aleatorio);
				Map<Long, Integer> divisao = new HashMap<>();
				List<Clube> nasDivisoes = new ArrayList<>();
				liga.divisoes().forEach(d -> d.clubes().forEach(c -> {
					divisao.put(c.id(), d.prioridade());
					nasDivisoes.add(c);
				}));
				List<Clube> inscritos = Copa.inscritos(nasDivisoes, planteis, liga.divisoes().size(),
						ParametrosCopa.padrao(), aleatorio);
				Map<Long, Double> forca = new HashMap<>();
				inscritos.forEach(c -> forca.put(c.id(), Clube.forcaEmCampo(planteis.get(c.id()))));
				List<Clube> porForca = inscritos.stream()
					.sorted(Comparator.comparingDouble((Clube c) -> forca.get(c.id())).reversed())
					.toList();
				Copa copa = Copa.sortear(inscritos, aleatorio);
				for (Copa.Etapa etapa : Copa.quadro(copa.primeiraFase(), 38, ParametrosCopa.padrao())) {
					if (copa.podeAvancar()) {
						copa = copa.avancar(aleatorio);
					}
					for (Copa.PartidaCopa p : copa.partidas(etapa)) {
						CondicoesPartida condicoes = p.decisivo().map(CondicoesPartida.NEUTRAS::comDecisivo)
							.orElse(CondicoesPartida.NEUTRAS);
						ResultadoPartida res = motor.simular(RegrasPartida.padrao(),
								escalacao.escalar(planteis.get(p.jogo().mandante().id())),
								escalacao.escalar(planteis.get(p.jogo().visitante().id())), condicoes, aleatorio.nextLong());
						PlacarCopa placar = new PlacarCopa(res.golsCasa(), res.golsFora(), res.prorrogacao(),
								res.penaltis().map(d -> new PlacarCopa.Penaltis(d.gols(br.com.managerfoot.motor.Lado.CASA),
										d.gols(br.com.managerfoot.motor.Lado.FORA))));
						copa = copa.comPlacar(etapa.fase(), p.confronto().ordem(), etapa.perna(), placar);
						if (p.decisivo().isPresent()) {
							r.decisivas++;
							r.prorrogacoes += res.prorrogacao() ? 1 : 0;
							r.penaltis += res.penaltis().isPresent() ? 1 : 0;
							ConfrontoCopa decidido = copa.confrontosDa(etapa.fase()).get(p.confronto().ordem());
							Clube vencedor = decidido.vencedor().orElseThrow();
							Clube perdedor = decidido.perdedor().orElseThrow();
							double diferenca = forca.get(perdedor.id()) - forca.get(vencedor.id());
							int faixa = Math.abs(forca.get(vencedor.id()) - forca.get(perdedor.id())) < 3 ? 0
									: Math.abs(forca.get(vencedor.id()) - forca.get(perdedor.id())) < 8 ? 1 : 2;
							r.porFaixa[faixa]++;
							if (diferenca > 0) {
								r.zebrasPorFaixa[faixa]++;
							}
						}
					}
				}
				Clube campeao = copa.campeao().orElseThrow();
				r.copas++;
				int posicao = porForca.indexOf(campeao);
				r.campeaoPorPosicao[posicao < 1 ? 0 : posicao < 4 ? 1 : posicao < 8 ? 2 : 3]++;
				if (divisao.get(campeao.id()) > 1) {
					r.campeaoDaSegunda++;
				}
			}
		}
		String md = r.relatorio();
		Files.createDirectories(SAIDA.getParent());
		Files.writeString(SAIDA, md);
		System.out.println(md);
	}

	private static final class Resultado {
		int copas;
		int decisivas;
		int prorrogacoes;
		int penaltis;
		int[] porFaixa = new int[3];
		int[] zebrasPorFaixa = new int[3];
		int[] campeaoPorPosicao = new int[4];
		int campeaoDaSegunda;

		String relatorio() {
			StringBuilder md = new StringBuilder();
			md.append("# Simulação da copa nacional\n\n")
				.append("Gerado por `RelatorioCopaTest` (ferramenta de desenvolvimento). ").append(SEMENTES)
				.append(" copas de cada país do banco padrão (").append(copas).append(" no total), com a distribuição ")
				.append("das divisões sorteada, os inscritos pela força em campo e a chave sorteada (SPEC §8.2). ")
				.append("Todos com a escalação da máquina; sem a evolução de força, os cartões e as lesões da liga ")
				.append("entre as fases.\n\n")
				.append("## Desempate\n\n")
				.append("| Partidas decisivas (voltas e finais) | Com prorrogação | Nos pênaltis |\n|---|---|---|\n")
				.append(String.format(Locale.ROOT, "| %d | %.1f%% | %.1f%% |%n%n", decisivas, pct(prorrogacoes, decisivas),
						pct(penaltis, decisivas)))
				.append("## Zebras\n\n")
				.append("Confrontos ganhos pelo clube de menor força em campo, pela diferença entre os dois.\n\n")
				.append("| Diferença de força em campo | Confrontos | Zebras |\n|---|---|---|\n");
			String[] faixas = { "menos de 3", "de 3 a 8", "8 ou mais" };
			for (int i = 0; i < 3; i++) {
				md.append(String.format(Locale.ROOT, "| %s | %d | %.1f%% |%n", faixas[i], porFaixa[i],
						pct(zebrasPorFaixa[i], porFaixa[i])));
			}
			md.append("\n## Campeão\n\n")
				.append("Posição do campeão entre os inscritos, pela força em campo.\n\n")
				.append("| O mais forte | 2º a 4º | 5º a 8º | 9º em diante | Da 2ª divisão |\n|---|---|---|---|---|\n")
				.append(String.format(Locale.ROOT, "| %.1f%% | %.1f%% | %.1f%% | %.1f%% | %.1f%% |%n",
						pct(campeaoPorPosicao[0], copas), pct(campeaoPorPosicao[1], copas),
						pct(campeaoPorPosicao[2], copas), pct(campeaoPorPosicao[3], copas),
						pct(campeaoDaSegunda, copas)));
			return md.toString();
		}

		private static double pct(int parte, int total) {
			return total == 0 ? 0 : 100.0 * parte / total;
		}
	}
}
