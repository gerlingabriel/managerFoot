package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.GravidadeLesao;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.Lesao;
import br.com.managerfoot.motor.ResultadoPartida;
import br.com.managerfoot.motor.TipoEvento;
import br.com.managerfoot.temporada.ParametrosEvolucao.Desfecho;

class EvolucaoForcaTest {

	private static final Set<Long> JOGARAM = Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
	private static final Set<Long> BANCO = Set.of(12L, 13L, 14L, 15L, 16L);

	private final EvolucaoForca evolucao = EvolucaoForca.padrao();

	@Test
	void naDerrotaCincoDosQueJogaramPerdemUmPontoCadaEQuemNaoJogouQuaseNaoCresce() {
		List<Jogador> depois = evolucao.aposPartida(plantel(30), JOGARAM, BANCO, Desfecho.DERROTA, 50, 30, new SplittableRandom(1));

		assertThat(depois).filteredOn(j -> JOGARAM.contains(j.id())).filteredOn(j -> j.forca() == 29).hasSize(5);
		assertThat(depois).allMatch(j -> j.forca() >= 29 && j.forca() <= 31);
		assertThat(depois).filteredOn(j -> !JOGARAM.contains(j.id())).filteredOn(j -> j.forca() == 31).hasSizeLessThanOrEqualTo(1);
	}

	@Test
	void naVitoriaUmPerdeENoEmpateTres() {
		for (int s = 0; s < 50; s++) {
			assertThat(perderam(Desfecho.VITORIA, s)).isEqualTo(1);
			assertThat(perderam(Desfecho.EMPATE, s)).isEqualTo(3);
			assertThat(cresceram(Desfecho.VITORIA, s)).isEqualTo(4);
			assertThat(cresceram(Desfecho.EMPATE, s)).isEqualTo(2);
		}
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

	private long perderam(Desfecho desfecho, long semente) {
		return evolucao.aposPartida(plantel(30), JOGARAM, BANCO, desfecho, 50, 30, new SplittableRandom(semente)).stream()
			.filter(j -> j.forca() == 29).count();
	}

	private long cresceram(Desfecho desfecho, long semente) {
		return evolucao.aposPartida(plantel(30), JOGARAM, BANCO, desfecho, 50, 30, new SplittableRandom(semente)).stream()
			.filter(j -> j.forca() == 31).count();
	}

	/** 23 jogadores com a mesma força, ids 1 a 23. */
	@Test
	void lesionadoPerdeAForcaDaLesaoSemPassarDaMinima() {
		List<Jogador> titulares = new ArrayList<>();
		Posicao[] posicoes = { Posicao.GOLEIRO, Posicao.DEFENSOR, Posicao.DEFENSOR, Posicao.DEFENSOR, Posicao.DEFENSOR,
				Posicao.MEIO_CAMPISTA, Posicao.MEIO_CAMPISTA, Posicao.MEIO_CAMPISTA, Posicao.MEIO_CAMPISTA,
				Posicao.ATACANTE, Posicao.ATACANTE };
		for (int i = 0; i < posicoes.length; i++) {
			titulares.add(new Jogador(i + 1, "J" + (i + 1), posicoes[i], i == 10 ? 4 : 30, Temperamento.NORMAL, "BRA"));
		}
		Escalacao escalacao = new Escalacao(Formacao.F_4_4_2, titulares);
		ResultadoPartida resultado = new ResultadoPartida(0, 0, List.of(
				new EventoPartida(20, Lado.CASA, TipoEvento.LESAO, titulares.get(4), false, null,
						new Lesao(GravidadeLesao.GRAVE, 6, 9)),
				new EventoPartida(50, Lado.CASA, TipoEvento.LESAO, titulares.get(10), false, null,
						new Lesao(GravidadeLesao.MEDIA, 4, 6)),
				new EventoPartida(60, Lado.FORA, TipoEvento.LESAO, titulares.get(1), false, null,
						new Lesao(GravidadeLesao.LEVE, 1, 3))));

		List<Jogador> depois = evolucao.aposPartida(titulares, escalacao, resultado, Lado.CASA, 50, 30,
				new SplittableRandom(1));

		assertThat(depois.get(4).forca()).isBetween(20, 21);
		assertThat(depois.get(10).forca()).isEqualTo(Jogador.FORCA_MINIMA);
		// a lesão do outro lado não é deste clube
		assertThat(depois.get(1).forca()).isBetween(29, 30);
	}

	private static List<Jogador> plantel(int forca) {
		List<Jogador> p = new ArrayList<>();
		for (long id = 1; id <= 23; id++) {
			p.add(new Jogador(id, "J" + id, Posicao.MEIO_CAMPISTA, forca, Temperamento.NORMAL, "BRA"));
		}
		return p;
	}
}
