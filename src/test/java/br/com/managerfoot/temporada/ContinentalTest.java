package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.temporada.GrupoContinental.PartidaGrupo;
import br.com.managerfoot.temporada.GrupoContinental.Placar;

/** Fase de grupos e passagem às oitavas da competição continental (SPEC §8.3.6). */
class ContinentalTest {

	static final ParametrosContinental PARAMETROS = ParametrosContinental.padrao();
	/** Vagas da América do Sul (rev. 72). */
	static final List<String> PAISES_AMERICA_DO_SUL = vagas("BRA", 7, "ARG", 6, "CHI", 3, "COL", 3, "URU", 3, "BOL", 2,
			"ECU", 2, "PAR", 2, "PER", 2, "VEN", 2);

	static List<String> vagas(Object... codigoEVagas) {
		List<String> paises = new ArrayList<>();
		for (int i = 0; i < codigoEVagas.length; i += 2) {
			for (int k = 0; k < (Integer) codigoEVagas[i + 1]; k++) {
				paises.add((String) codigoEVagas[i]);
			}
		}
		return paises;
	}

	static List<Clube> clubes(int quantidade) {
		List<Clube> clubes = new ArrayList<>();
		for (int i = 1; i <= quantidade; i++) {
			clubes.add(new Clube(i, "Clube " + i, 30));
		}
		return clubes;
	}

	static Map<Long, String> paises(List<Clube> clubes, List<String> codigos) {
		Map<Long, String> paises = new HashMap<>();
		for (int i = 0; i < clubes.size(); i++) {
			paises.put(clubes.get(i).id(), codigos.get(i));
		}
		return paises;
	}

	static Map<Long, List<Jogador>> planteis(List<Clube> clubes, ToIntFunction<Clube> forca) {
		Map<Long, List<Jogador>> planteis = new HashMap<>();
		for (Clube c : clubes) {
			List<Jogador> jogadores = new ArrayList<>();
			for (int k = 0; k < 11; k++) {
				jogadores.add(new Jogador(c.id() * 100 + k, "J", Posicao.MEIO_CAMPISTA, forca.applyAsInt(c),
						Temperamento.NORMAL, "BRA"));
			}
			planteis.put(c.id(), jogadores);
		}
		return planteis;
	}

	static final List<Clube> CLUBES = clubes(32);
	/** O clube 1 é o mais forte e o 32 o mais fraco. */
	static final Map<Long, List<Jogador>> PLANTEIS = planteis(CLUBES, c -> 50 - (int) c.id());

	static Continental sortear(long semente) {
		return Continental.sortear(CLUBES, paises(CLUBES, PAISES_AMERICA_DO_SUL), PLANTEIS, PARAMETROS,
				new SplittableRandom(semente));
	}

	@Test
	void oitoGruposDeQuatroComUmClubeDeCadaPote() {
		Continental continental = sortear(1);
		assertThat(continental.grupos()).hasSize(8).extracting(GrupoContinental::nome).startsWith("Grupo A")
			.endsWith("Grupo H");
		assertThat(continental.grupos().stream().flatMap(g -> g.clubes().stream())).containsExactlyInAnyOrderElementsOf(CLUBES);
		for (GrupoContinental grupo : continental.grupos()) {
			for (int pote = 0; pote < 4; pote++) {
				// pote 1: clubes 1 a 8, pote 2: 9 a 16...
				assertThat(grupo.clubes().get(pote).id()).as(grupo.nome()).isBetween(pote * 8 + 1L, pote * 8 + 8L);
			}
		}
	}

	@RepeatedTest(20)
	void doisClubesDoMesmoPaisNaoCaemNoMesmoGrupoQuandoDaParaEvitar(org.junit.jupiter.api.RepetitionInfo repeticao) {
		Map<Long, String> paises = paises(CLUBES, PAISES_AMERICA_DO_SUL);
		Continental continental = Continental.sortear(CLUBES, paises, PLANTEIS, PARAMETROS,
				new SplittableRandom(repeticao.getCurrentRepetition()));
		for (GrupoContinental grupo : continental.grupos()) {
			assertThat(grupo.clubes()).extracting(c -> paises.get(c.id())).as(grupo.nome()).doesNotHaveDuplicates();
		}
	}

	@Test
	void comNoveClubesDeUmPaisAceitaSoARepeticaoInevitavel() {
		List<String> codigos = new ArrayList<>(vagas("BRA", 9, "ARG", 23));
		Map<Long, String> paises = paises(CLUBES, codigos);
		Continental continental = Continental.sortear(CLUBES, paises, PLANTEIS, PARAMETROS, new SplittableRandom(3));
		Map<String, Long> repetidos = continental.grupos().stream()
			.collect(Collectors.toMap(GrupoContinental::nome,
					g -> g.clubes().stream().filter(c -> paises.get(c.id()).equals("BRA")).count()));
		assertThat(repetidos.values()).allSatisfy(n -> assertThat(n).isBetween(1L, 2L));
		assertThat(repetidos.values().stream().filter(n -> n == 2).count()).isEqualTo(1);
	}

	@Test
	void recusaNumeroErradoDeInscritosOuClubeSemPais() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Continental.sortear(clubes(31), Map.of(), PLANTEIS, PARAMETROS, new SplittableRandom(1)))
			.withMessageContaining("31");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Continental.sortear(CLUBES, Map.of(), PLANTEIS, PARAMETROS, new SplittableRandom(1)))
			.withMessageContaining("sem país");
	}

	@Test
	void mesmaSementeMesmoSorteio() {
		assertThat(sortear(9)).isEqualTo(sortear(9));
		assertThat(sortear(9)).isNotEqualTo(sortear(10));
	}

	@Test
	void cadaClubeJogaSeisVezesTresEmCasaEEnfrentaCadaRivalNaIdaENaVolta() {
		GrupoContinental grupo = sortear(1).grupos().getFirst();
		assertThat(grupo.jogos()).isEqualTo(6);
		for (int jogo = 1; jogo <= 6; jogo++) {
			assertThat(grupo.partidasDoJogo(jogo).stream().flatMap(p -> java.util.stream.Stream.of(p.mandante(), p.visitante())))
				.as("jogo " + jogo).containsExactlyInAnyOrderElementsOf(grupo.clubes());
		}
		for (Clube clube : grupo.clubes()) {
			assertThat(grupo.partidas().stream().filter(p -> p.mandante().equals(clube))).hasSize(3);
			for (Clube rival : grupo.clubes()) {
				if (!rival.equals(clube)) {
					assertThat(grupo.partidas()).filteredOn(p -> p.mandante().equals(clube) && p.visitante().equals(rival))
						.hasSize(1);
				}
			}
		}
	}

	@Test
	void partidasDoJogoSaemNaRodadaDoQuadroESomemDepoisDoPlacar() {
		Continental continental = sortear(1);
		List<Continental.PartidaDeGrupo> primeiras = continental.partidasDosGrupos(1, PARAMETROS.rodadasDosGrupos().getFirst());
		assertThat(primeiras).hasSize(16).allSatisfy(p -> assertThat(p.jogo().rodada()).isEqualTo(2));
		Continental.PartidaDeGrupo p = primeiras.getFirst();
		Continental depois = continental.comPlacarDoGrupo(p.grupo().indice(), 1, p.jogo().mandante(), new Placar(2, 1));
		assertThat(depois.partidasDosGrupos(1, 2)).hasSize(15);
		assertThatIllegalStateException()
			.isThrownBy(() -> depois.comPlacarDoGrupo(p.grupo().indice(), 1, p.jogo().mandante(), new Placar(0, 0)));
	}

	/** Joga todos os jogos dos grupos: o clube de menor id vence por 1 x 0. */
	static Continental jogarGrupos(Continental continental) {
		for (int jogo = 1; jogo <= 6; jogo++) {
			for (Continental.PartidaDeGrupo p : continental.partidasDosGrupos(jogo, 1)) {
				boolean mandanteVence = p.jogo().mandante().id() < p.jogo().visitante().id();
				continental = continental.comPlacarDoGrupo(p.grupo().indice(), jogo, p.jogo().mandante(),
						mandanteVence ? new Placar(1, 0) : new Placar(0, 1));
			}
		}
		return continental;
	}

	@Test
	void desempateDoGrupoEPontosDepoisSaldoDepoisGolsPro() {
		List<Clube> c = clubes(4);
		GrupoContinental grupo = GrupoContinental.novo(0, c);
		// os dois primeiros empatam em pontos e o saldo decide
		grupo = jogar(grupo, Map.of(
				par(c.get(0), c.get(1)), new Placar(1, 1), par(c.get(1), c.get(0)), new Placar(0, 0),
				par(c.get(0), c.get(2)), new Placar(3, 0), par(c.get(2), c.get(0)), new Placar(0, 1),
				par(c.get(0), c.get(3)), new Placar(1, 0), par(c.get(3), c.get(0)), new Placar(0, 2),
				par(c.get(1), c.get(2)), new Placar(5, 0), par(c.get(2), c.get(1)), new Placar(0, 1),
				par(c.get(1), c.get(3)), new Placar(2, 0), par(c.get(3), c.get(1)), new Placar(0, 1)));
		grupo = jogar(grupo, Map.of(par(c.get(2), c.get(3)), new Placar(3, 3), par(c.get(3), c.get(2)), new Placar(1, 1)));
		List<LinhaClassificacao> tabela = grupo.classificacao(7);
		// c0: 4 vitórias e 2 empates = 14, saldo +7; c1: 14, saldo +9; c2 e c3: 2 pontos, saldos -10 e -6
		assertThat(tabela).extracting(LinhaClassificacao::clube).containsExactly(c.get(1), c.get(0), c.get(3), c.get(2));
		assertThat(tabela.get(0).pontos()).isEqualTo(tabela.get(1).pontos()).isEqualTo(14);
		assertThat(grupo.classificados(7)).containsExactly(c.get(1), c.get(0));
	}

	@Test
	void depoisDoSaldoDecidemOsGolsProEDepoisUmSorteioFixo() {
		List<Clube> c = clubes(4);
		GrupoContinental grupo = GrupoContinental.novo(0, c);
		// tudo empatado; c2 x c3 com gols: todos com 6 pontos e saldo 0, c2 e c3 com 4 gols pró
		grupo = jogar(grupo, Map.of(par(c.get(2), c.get(3)), new Placar(3, 3), par(c.get(3), c.get(2)), new Placar(1, 1)));
		for (PartidaGrupo p : grupo.partidas()) {
			if (p.placar().isEmpty()) {
				grupo = grupo.comPlacar(p.jogo(), p.mandante(), new Placar(0, 0));
			}
		}
		List<Clube> ordem = grupo.classificacao(7).stream().map(LinhaClassificacao::clube).toList();
		assertThat(ordem.subList(0, 2)).containsExactlyInAnyOrder(c.get(2), c.get(3));
		assertThat(ordem.subList(2, 4)).containsExactlyInAnyOrder(c.get(0), c.get(1));
		assertThat(grupo.classificacao(7).stream().map(LinhaClassificacao::clube).toList()).isEqualTo(ordem);
		boolean sorteioMuda = false;
		for (long semente = 0; semente < 20 && !sorteioMuda; semente++) {
			sorteioMuda = !grupo.classificacao(semente).getFirst().clube().equals(ordem.getFirst());
		}
		assertThat(sorteioMuda).as("outra semente pode mudar a ordem dos empatados em tudo").isTrue();
	}

	static List<Clube> par(Clube mandante, Clube visitante) {
		return List.of(mandante, visitante);
	}

	static GrupoContinental jogar(GrupoContinental grupo, Map<List<Clube>, Placar> placares) {
		for (PartidaGrupo p : grupo.partidas()) {
			Placar placar = placares.get(par(p.mandante(), p.visitante()));
			if (placar != null && p.placar().isEmpty()) {
				grupo = grupo.comPlacar(p.jogo(), p.mandante(), placar);
			}
		}
		return grupo;
	}

	@Test
	void oitavasSoDepoisDosGruposComPrimeiroContraSegundoDeOutroGrupoEVoltaNaCasaDoPrimeiro() {
		Continental continental = sortear(4);
		assertThatIllegalStateException().isThrownBy(() -> sortear(4).iniciarMataMata(1, new SplittableRandom(1)));
		continental = jogarGrupos(continental);
		assertThat(continental.gruposEncerrados()).isTrue();
		Continental comOitavas = continental.iniciarMataMata(1, new SplittableRandom(2));
		List<ConfrontoCopa> oitavas = comOitavas.mataMata().orElseThrow().confrontosDa(FaseCopa.OITAVAS);
		assertThat(oitavas).hasSize(8);
		for (int g = 0; g < 8; g++) {
			GrupoContinental grupo = continental.grupos().get(g);
			ConfrontoCopa confronto = oitavas.get(g);
			List<Clube> classificados = grupo.classificados(1);
			// o 1º joga a volta em casa: é o visitante da ida
			assertThat(confronto.visitante()).isEqualTo(classificados.get(0));
			assertThat(classificados).doesNotContain(confronto.mandante());
			assertThat(continental.grupos().stream().filter(x -> x.classificados(1).get(1).equals(confronto.mandante())))
				.hasSize(1);
		}
		assertThat(oitavas).extracting(ConfrontoCopa::mandante).doesNotHaveDuplicates();
		assertThatIllegalStateException().isThrownBy(() -> comOitavas.iniciarMataMata(1, new SplittableRandom(2)));
	}

	@Test
	void mataMataSegueAteOCampeaoComoACopa() {
		Continental continental = jogarGrupos(sortear(5)).iniciarMataMata(1, new SplittableRandom(3));
		Copa copa = continental.mataMata().orElseThrow();
		while (true) {
			FaseCopa fase = copa.faseAtual();
			for (ConfrontoCopa c : copa.confrontosDa(fase)) {
				if (fase.jogoUnico()) {
					copa = copa.comPlacar(fase, c.ordem(), Copa.Perna.JOGO_UNICO, PlacarCopa.de(2, 0));
				} else {
					copa = copa.comPlacar(fase, c.ordem(), Copa.Perna.IDA, PlacarCopa.de(1, 0));
					copa = copa.comPlacar(fase, c.ordem(), Copa.Perna.VOLTA, PlacarCopa.de(0, 0));
				}
			}
			if (!copa.podeAvancar()) {
				break;
			}
			copa = copa.avancar(new SplittableRandom(fase.ordinal()));
		}
		continental = continental.comMataMata(copa);
		assertThat(continental.campeao()).isPresent();
		assertThat(continental.vice()).isPresent().isNotEqualTo(continental.campeao());
		assertThat(continental.participa(continental.campeao().orElseThrow())).isTrue();
	}

	@Test
	void quadroComOsSeisJogosDosGruposOMataMataEAFinalDepoisDaUltimaRodada() {
		List<Continental.Etapa> quadro = Continental.quadro(38, PARAMETROS);
		assertThat(quadro).extracting(Continental.Etapa::depoisDaRodada)
			.containsExactly(2, 4, 7, 9, 14, 16, 19, 21, 22, 24, 25, 27, 38);
		assertThat(quadro).extracting(Continental.Etapa::nome).startsWith("Grupos · 1º jogo")
			.contains("Oitavas de final · ida", "Semifinal · volta").endsWith("Final");
		assertThat(quadro.getLast().mataMata().perna()).isEqualTo(Copa.Perna.JOGO_UNICO);
		assertThat(Continental.quadro(28, PARAMETROS)).hasSize(13);
		assertThatIllegalArgumentException().isThrownBy(() -> Continental.quadro(27, PARAMETROS));
	}

	@Test
	void parametrosRecusamQuadroQueSobrepoeFases() {
		java.util.Map<FaseCopa, ParametrosCopa.Pernas> mataMata = new java.util.EnumMap<>(FaseCopa.class);
		mataMata.put(FaseCopa.OITAVAS, new ParametrosCopa.Pernas(15, 17));
		mataMata.put(FaseCopa.QUARTAS, new ParametrosCopa.Pernas(22, 24));
		mataMata.put(FaseCopa.SEMIFINAL, new ParametrosCopa.Pernas(25, 27));
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ParametrosContinental(8, 4, List.of(2, 4, 7, 9, 14, 16), mataMata, 3))
			.withMessageContaining("Oitavas");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ParametrosContinental(8, 4, List.of(2, 4, 7), PARAMETROS.mataMata(), 3));
	}
}
