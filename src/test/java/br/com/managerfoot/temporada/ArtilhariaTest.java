package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.ResultadoPartida;
import br.com.managerfoot.motor.TipoEvento;
import br.com.managerfoot.temporada.Artilharia.Artilheiro;

class ArtilhariaTest {

	private static Jogador jogador(long id) {
		return new Jogador(id, "J" + id, Posicao.ATACANTE, 25, Temperamento.NORMAL, "BRA");
	}

	private static EventoPartida gol(long id) {
		return new EventoPartida(10, Lado.CASA, TipoEvento.GOL, jogador(id));
	}

	private static ResultadoPartida partida(EventoPartida... eventos) {
		return new ResultadoPartida(0, 0, List.of(eventos));
	}

	@Test
	void todosComecamComZero() {
		Artilharia a = Artilharia.vazia();
		assertThat(a.golsNaTemporada(1)).isZero();
		assertThat(a.golsNaCarreira(1)).isZero();
		assertThat(a.rankingTemporada()).isEmpty();
		assertThat(a.artilheirosDaTemporada()).isEmpty();
	}

	@Test
	void contaGolsInclusivePenaltiENaoContaOutrosEventos() {
		Artilharia a = Artilharia.vazia().registrar(partida(
				gol(1),
				new EventoPartida(30, Lado.CASA, TipoEvento.GOL, jogador(1), true),
				new EventoPartida(40, Lado.FORA, TipoEvento.DEFESA, jogador(2)),
				new EventoPartida(50, Lado.FORA, TipoEvento.TRAVE, jogador(2), true),
				new EventoPartida(60, Lado.FORA, TipoEvento.CARTAO_AMARELO, jogador(2))));

		assertThat(a.golsNaTemporada(1)).isEqualTo(2);
		assertThat(a.golsNaCarreira(1)).isEqualTo(2);
		assertThat(a.golsNaTemporada(2)).isZero();
	}

	@Test
	void somaPartidasDeQualquerCompeticaoEZeraSoATemporadaNaVirada() {
		Artilharia a = Artilharia.vazia()
			.registrar(partida(gol(1), gol(1)))   // liga
			.registrar(partida(gol(1), gol(2)));  // copa
		assertThat(a.golsNaTemporada(1)).isEqualTo(3);

		Artilharia seguinte = a.novaTemporada().registrar(partida(gol(1)));
		assertThat(seguinte.golsNaTemporada(1)).isEqualTo(1);
		assertThat(seguinte.golsNaCarreira(1)).isEqualTo(4);
		assertThat(seguinte.golsNaCarreira(2)).isEqualTo(1);
	}

	@Test
	void registrarNaoAlteraAArtilhariaAnterior() {
		Artilharia antes = Artilharia.vazia();
		antes.registrar(partida(gol(1)));
		assertThat(antes.golsNaTemporada(1)).isZero();
	}

	@Test
	void empatadosDividemAPosicaoEOPremio() {
		Artilharia a = Artilharia.vazia().registrar(partida(gol(3), gol(3), gol(1), gol(1), gol(2)));

		assertThat(a.rankingTemporada()).containsExactly(
				new Artilheiro(1, 1, 2), new Artilheiro(1, 3, 2), new Artilheiro(3, 2, 1));
		assertThat(a.artilheirosDaTemporada()).containsExactlyInAnyOrder(1L, 3L);
	}

	@Test
	void rankingDeTodosOsTemposUsaACarreira() {
		Artilharia a = Artilharia.vazia()
			.registrar(partida(gol(1), gol(1), gol(1)))
			.novaTemporada()
			.registrar(partida(gol(2), gol(2)));

		assertThat(a.rankingTemporada()).containsExactly(new Artilheiro(1, 2, 2));
		assertThat(a.rankingCarreira()).containsExactly(new Artilheiro(1, 1, 3), new Artilheiro(2, 2, 2));
	}
}
