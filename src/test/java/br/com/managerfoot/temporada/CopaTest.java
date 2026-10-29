package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.motor.JogoDecisivo;

class CopaTest {

	private final ParametrosCopa parametros = ParametrosCopa.padrao();

	private static List<Clube> clubes(int quantidade) {
		List<Clube> clubes = new ArrayList<>();
		for (int i = 1; i <= quantidade; i++) {
			clubes.add(new Clube(i, "Clube " + i, 30));
		}
		return clubes;
	}

	/** Cada clube com 11 jogadores da força dada por {@code forca}. */
	private static Map<Long, List<Jogador>> planteis(List<Clube> clubes, java.util.function.ToIntFunction<Clube> forca) {
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

	@ParameterizedTest
	@CsvSource({ "2, 40, 32", "2, 32, 32", "1, 20, 16", "1, 16, 16", "1, 15, 8", "2, 30, 16", "3, 60, 32" })
	void tamanhoPeloNumeroDeDivisoesELimitadoAPotenciaDeDois(int divisoes, int quantidade, int esperado) {
		List<Clube> clubes = clubes(quantidade);
		List<Clube> inscritos = Copa.inscritos(clubes, planteis(clubes, c -> 20), divisoes, parametros,
				new SplittableRandom(1));
		assertThat(inscritos).hasSize(esperado).doesNotHaveDuplicates();
	}

	@Test
	void ficamDeForaOsDeMenorForcaEmCampo() {
		List<Clube> clubes = clubes(40);
		// força = id: os 8 primeiros são os mais fracos
		List<Clube> inscritos = Copa.inscritos(clubes, planteis(clubes, c -> (int) c.id()), 2, parametros,
				new SplittableRandom(1));
		assertThat(inscritos).extracting(Clube::id).allMatch(id -> id > 8);
	}

	@Test
	void empateNaLinhaDeCorteDecidePorSorteio() {
		List<Clube> clubes = clubes(40);
		// 30 clubes fortes e 10 iguais disputando as 2 últimas vagas
		Map<Long, List<Jogador>> planteis = planteis(clubes, c -> c.id() <= 30 ? 40 : 10);
		Set<Set<Long>> escolhidos = new HashSet<>();
		for (long s = 0; s < 30; s++) {
			List<Clube> inscritos = Copa.inscritos(clubes, planteis, 2, parametros, new SplittableRandom(s));
			assertThat(inscritos.stream().filter(c -> c.id() <= 30)).hasSize(30);
			Set<Long> fracos = new HashSet<>();
			inscritos.stream().filter(c -> c.id() > 30).forEach(c -> fracos.add(c.id()));
			escolhidos.add(fracos);
		}
		assertThat(escolhidos).hasSizeGreaterThan(5);
	}

	@Test
	void sorteioCriaAPrimeiraFaseComTodosOsInscritosUmaVez() {
		List<Clube> inscritos = clubes(32);
		Copa copa = Copa.sortear(inscritos, new SplittableRandom(3));
		assertThat(copa.faseAtual()).isEqualTo(FaseCopa.DEZESSEIS_AVOS);
		assertThat(copa.confrontos()).hasSize(16);
		Set<Clube> todos = new HashSet<>();
		copa.confrontos().forEach(c -> {
			todos.add(c.mandante());
			todos.add(c.visitante());
		});
		assertThat(todos).containsExactlyInAnyOrderElementsOf(inscritos);
		assertThat(Copa.sortear(inscritos, new SplittableRandom(3))).isEqualTo(copa);
		assertThat(Copa.sortear(inscritos, new SplittableRandom(4))).isNotEqualTo(copa);
	}

	@Test
	void sorteioRecusaQuantidadeQueNaoEPotenciaDeDois() {
		assertThatIllegalArgumentException().isThrownBy(() -> Copa.sortear(clubes(12), new SplittableRandom(1)));
	}

	@Test
	void quadroDeRodadasCom2DivisoesECom1() {
		assertThat(Copa.quadro(FaseCopa.DEZESSEIS_AVOS, 38, parametros)).extracting(Copa.Etapa::depoisDaRodada)
			.containsExactly(3, 5, 10, 12, 18, 20, 26, 28, 38);
		assertThat(Copa.quadro(FaseCopa.OITAVAS, 38, parametros)).extracting(Copa.Etapa::depoisDaRodada)
			.containsExactly(10, 12, 18, 20, 26, 28, 38);
		assertThat(Copa.quadro(FaseCopa.QUARTAS, 28, parametros)).extracting(Copa.Etapa::depoisDaRodada)
			.containsExactly(18, 20, 26, 28, 28);
		assertThat(Copa.quadro(FaseCopa.QUARTAS, 28, parametros).getLast())
			.isEqualTo(new Copa.Etapa(28, FaseCopa.FINAL, Copa.Perna.JOGO_UNICO));
		assertThatIllegalArgumentException().isThrownBy(() -> Copa.quadro(FaseCopa.OITAVAS, 27, parametros));
	}

	@Test
	void voltaInverteOMandoESomaAIda() {
		Copa copa = Copa.sortear(clubes(16), new SplittableRandom(5));
		Copa.Etapa ida = new Copa.Etapa(10, FaseCopa.OITAVAS, Copa.Perna.IDA);
		Copa.Etapa volta = new Copa.Etapa(12, FaseCopa.OITAVAS, Copa.Perna.VOLTA);
		assertThat(copa.partidas(volta)).isEmpty();
		List<Copa.PartidaCopa> idas = copa.partidas(ida);
		assertThat(idas).hasSize(8).allMatch(p -> p.decisivo().isEmpty());
		Copa.PartidaCopa primeira = idas.getFirst();
		copa = copa.comPlacar(FaseCopa.OITAVAS, 0, Copa.Perna.IDA, PlacarCopa.de(2, 1));
		assertThat(copa.partidas(ida)).hasSize(7);

		Copa.PartidaCopa daVolta = copa.partidas(volta).getFirst();
		assertThat(daVolta.jogo().mandante()).isEqualTo(primeira.jogo().visitante());
		assertThat(daVolta.jogo().visitante()).isEqualTo(primeira.jogo().mandante());
		assertThat(daVolta.decisivo()).contains(JogoDecisivo.volta(1, 2));
	}

	@Test
	void vencedorPeloPlacarSomadoSemGolForaDeCasa() {
		Clube a = new Clube(1, "A", 30);
		Clube b = new Clube(2, "B", 30);
		ConfrontoCopa c = ConfrontoCopa.novo(FaseCopa.OITAVAS, 0, a, b).comIda(PlacarCopa.de(2, 1));
		assertThat(c.vencedor()).isEmpty();
		// volta vista de B, em casa: 1 x 0 empata em 2 x 2; o gol de B fora não desempata
		assertThatIllegalArgumentException().isThrownBy(() -> c.comVolta(PlacarCopa.de(1, 0)));
		ConfrontoCopa nosPenaltis = c.comVolta(new PlacarCopa(1, 0, true, Optional.of(new PlacarCopa.Penaltis(4, 3))));
		assertThat(nosPenaltis.vencedor()).contains(b);
		assertThat(nosPenaltis.perdedor()).contains(a);
		ConfrontoCopa visitanteVence = c.comVolta(PlacarCopa.de(0, 0));
		assertThat(visitanteVence.vencedor()).contains(a);
		ConfrontoCopa bVira = c.comVolta(new PlacarCopa(3, 1, true, Optional.empty()));
		assertThat(bVira.vencedor()).contains(b);
	}

	@Test
	void finalUnicaEmCampoNeutroDecideNosPenaltis() {
		Clube a = new Clube(1, "A", 30);
		Clube b = new Clube(2, "B", 30);
		ConfrontoCopa f = ConfrontoCopa.novo(FaseCopa.FINAL, 0, a, b);
		assertThat(f.decisivo()).isEqualTo(JogoDecisivo.FINAL);
		assertThat(f.comIda(PlacarCopa.de(0, 1)).vencedor()).contains(b);
		assertThat(f.comIda(new PlacarCopa(1, 1, true, Optional.of(new PlacarCopa.Penaltis(5, 4)))).vencedor()).contains(a);
		assertThatIllegalArgumentException().isThrownBy(() -> f.comIda(PlacarCopa.de(1, 1)));
	}

	@Test
	void copaCompletaAteOCampeaoPelaChaveFixa() {
		List<Clube> inscritos = clubes(16);
		SplittableRandom aleatorio = new SplittableRandom(9);
		Copa copa = Copa.sortear(inscritos, aleatorio);
		List<Copa.Etapa> quadro = Copa.quadro(copa.primeiraFase(), 38, parametros);
		Clube esperadoNaQuartas0 = null;
		for (Copa.Etapa etapa : quadro) {
			if (copa.podeAvancar()) {
				copa = copa.avancar(aleatorio);
			}
			for (Copa.PartidaCopa p : copa.partidas(etapa)) {
				// ganha sempre o clube de menor id
				Clube casa = p.jogo().mandante();
				Clube fora = p.jogo().visitante();
				PlacarCopa placar = switch (etapa.perna()) {
					case IDA -> PlacarCopa.de(casa.id() < fora.id() ? 1 : 0, casa.id() < fora.id() ? 0 : 1);
					case VOLTA, JOGO_UNICO -> PlacarCopa.de(casa.id() < fora.id() ? 2 : 0, casa.id() < fora.id() ? 0 : 2);
				};
				copa = copa.comPlacar(etapa.fase(), p.confronto().ordem(), etapa.perna(), placar);
			}
			if (etapa.fase() == FaseCopa.OITAVAS && etapa.perna() == Copa.Perna.VOLTA) {
				List<ConfrontoCopa> oitavas = copa.confrontosDa(FaseCopa.OITAVAS);
				esperadoNaQuartas0 = oitavas.get(0).vencedor().orElseThrow();
				Clube outro = oitavas.get(1).vencedor().orElseThrow();
				copa = copa.avancar(aleatorio);
				assertThat(copa.confrontosDa(FaseCopa.QUARTAS).getFirst().envolve(esperadoNaQuartas0)).isTrue();
				assertThat(copa.confrontosDa(FaseCopa.QUARTAS).getFirst().envolve(outro)).isTrue();
			}
		}
		assertThat(copa.campeao()).contains(inscritos.getFirst());
		assertThat(copa.vice()).isPresent();
		assertThat(copa.naDisputa(inscritos.getFirst())).isTrue();
		assertThat(copa.naDisputa(copa.vice().orElseThrow())).isFalse();
		assertThat(copa.confrontos()).hasSize(8 + 4 + 2 + 1);
		Copa terminada = copa;
		assertThatIllegalStateException().isThrownBy(() -> terminada.avancar(aleatorio));
	}

	@Test
	void naoAvancaComConfrontoPendente() {
		Copa copa = Copa.sortear(clubes(4), new SplittableRandom(1));
		assertThat(copa.podeAvancar()).isFalse();
		Copa semUm = copa.comPlacar(FaseCopa.SEMIFINAL, 0, Copa.Perna.IDA, PlacarCopa.de(1, 0))
			.comPlacar(FaseCopa.SEMIFINAL, 0, Copa.Perna.VOLTA, PlacarCopa.de(0, 0));
		assertThat(semUm.podeAvancar()).isFalse();
		assertThatIllegalStateException().isThrownBy(() -> semUm.avancar(new SplittableRandom(1)));
		assertThatIllegalStateException()
			.isThrownBy(() -> semUm.comPlacar(FaseCopa.SEMIFINAL, 0, Copa.Perna.IDA, PlacarCopa.de(1, 0)));
	}
}
