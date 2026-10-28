package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.temporada.ParametrosEvolucao.Desfecho;

class EvolucaoForcaTest {

	private static final Set<Long> JOGARAM = Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
	private static final Set<Long> BANCO = Set.of(12L, 13L, 14L, 15L, 16L);

	private final EvolucaoForca evolucao = EvolucaoForca.padrao();

	@Test
	void quemJogouPerdeMaisNaDerrotaEQuemNaoJogouCresceMaisNaVitoria() {
		assertThat(perdaMedia(Desfecho.VITORIA)).isBetween(0.8, 1.2);
		assertThat(perdaMedia(Desfecho.EMPATE)).isBetween(2.7, 3.3);
		assertThat(perdaMedia(Desfecho.DERROTA)).isBetween(4.6, 5.4);
		assertThat(crescimentoMedio(Desfecho.VITORIA)).isGreaterThan(crescimentoMedio(Desfecho.EMPATE));
		assertThat(crescimentoMedio(Desfecho.EMPATE)).isGreaterThan(crescimentoMedio(Desfecho.DERROTA));
	}

	@Test
	void mesmaSementeMesmoResultadoEOrdemDoPlantelMantida() {
		List<Jogador> plantel = plantel(30);
		List<Jogador> a = evolucao.aposPartida(plantel, JOGARAM, BANCO, Desfecho.DERROTA, 50, 30, new SplittableRandom(4));
		List<Jogador> b = evolucao.aposPartida(plantel, JOGARAM, BANCO, Desfecho.DERROTA, 50, 30, new SplittableRandom(4));
		assertThat(a).isEqualTo(b);
		assertThat(a).extracting(Jogador::id).containsExactlyElementsOf(plantel.stream().map(Jogador::id).toList());
	}

	@Test
	void desgasteParaNoPisoECrescimentoNoTeto() {
		// força do clube 30: ninguém que joga cai abaixo de 25
		List<Jogador> noPiso = plantel(25);
		List<Jogador> noTeto = plantel(30);
		for (int s = 0; s < 200; s++) {
			noPiso = evolucao.aposPartida(noPiso, JOGARAM, Set.of(), Desfecho.DERROTA, 50, 30, new SplittableRandom(s));
			noTeto = evolucao.aposPartida(noTeto, Set.of(), BANCO, Desfecho.VITORIA, 30, 30, new SplittableRandom(s));
		}
		assertThat(noPiso).allMatch(j -> j.forca() >= 25);
		assertThat(noTeto).allMatch(j -> j.forca() <= 30);
	}

	@Test
	void acimaDoTetoNaoCresceEPerdeAosPoucos() {
		List<Jogador> rebaixado = plantel(45);
		for (int s = 0; s < 60; s++) {
			rebaixado = evolucao.aposPartida(rebaixado, Set.of(), BANCO, Desfecho.VITORIA, 30, 45, new SplittableRandom(s));
		}
		double media = rebaixado.stream().mapToInt(Jogador::forca).average().orElseThrow();
		assertThat(media).isBetween(30.0, 38.0);
	}

	private double perdaMedia(Desfecho desfecho) {
		double total = 0;
		for (int s = 0; s < 2000; s++) {
			List<Jogador> depois = evolucao.aposPartida(plantel(30), JOGARAM, BANCO, desfecho, 50, 30, new SplittableRandom(s));
			total += depois.stream().filter(j -> JOGARAM.contains(j.id())).mapToInt(j -> 30 - j.forca()).sum();
		}
		return total / 2000;
	}

	private double crescimentoMedio(Desfecho desfecho) {
		double total = 0;
		for (int s = 0; s < 2000; s++) {
			List<Jogador> depois = evolucao.aposPartida(plantel(30), JOGARAM, BANCO, desfecho, 50, 30, new SplittableRandom(s));
			total += depois.stream().filter(j -> !JOGARAM.contains(j.id())).mapToInt(j -> j.forca() - 30).sum();
		}
		return total / 2000;
	}

	/** 23 jogadores com a mesma força, ids 1 a 23. */
	private static List<Jogador> plantel(int forca) {
		List<Jogador> p = new ArrayList<>();
		for (long id = 1; id <= 23; id++) {
			p.add(new Jogador(id, "J" + id, Posicao.MEIO_CAMPISTA, forca, Temperamento.NORMAL, "BRA"));
		}
		return p;
	}
}
