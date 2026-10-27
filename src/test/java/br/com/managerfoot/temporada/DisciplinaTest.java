package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.ResultadoPartida;
import br.com.managerfoot.motor.TipoEvento;

class DisciplinaTest {

	private static final ParametrosSuspensao PADRAO = ParametrosSuspensao.padrao();
	private static final Jogador ANA = new Jogador(1, "Ana", Posicao.DEFENSOR, 20, Temperamento.VIOLENTO, "BRA");
	private static final Jogador BIA = new Jogador(2, "Bia", Posicao.ATACANTE, 20, Temperamento.NORMAL, "BRA");
	private static final List<Long> PLANTEIS = List.of(1L, 2L, 3L);

	@Test
	void quintoAmareloSuspendeUmJogoEZeraOContador() {
		Disciplina d = Disciplina.vazia();
		for (int i = 0; i < 4; i++) {
			d = d.aposPartida(PLANTEIS, partida(TipoEvento.CARTAO_AMARELO, ANA), PADRAO, new SplittableRandom(1));
		}
		assertThat(d.amarelosDe(1)).isEqualTo(4);
		assertThat(d.suspenso(1)).isFalse();

		d = d.aposPartida(PLANTEIS, partida(TipoEvento.CARTAO_AMARELO, ANA), PADRAO, new SplittableRandom(1));

		assertThat(d.amarelosDe(1)).isZero();
		assertThat(d.suspensaoDe(1)).isEqualTo(1);
	}

	@Test
	void suspensaoECumpridaNaProximaPartidaDoClube() {
		Disciplina d = Disciplina.vazia()
			.aposPartida(PLANTEIS, partida(TipoEvento.SEGUNDO_AMARELO, ANA), PADRAO, new SplittableRandom(1));
		assertThat(d.suspensaoDe(1)).isEqualTo(1);

		// partida de outros clubes não conta
		Disciplina outra = d.aposPartida(List.of(50L, 51L), partida(), PADRAO, new SplittableRandom(1));
		assertThat(outra.suspensaoDe(1)).isEqualTo(1);

		Disciplina cumprida = outra.aposPartida(PLANTEIS, partida(), PADRAO, new SplittableRandom(1));
		assertThat(cumprida.suspenso(1)).isFalse();
		assertThat(cumprida.suspensoes()).isEmpty();
	}

	@Test
	void segundoAmareloSomaAoContadorESuspendePelaExpulsao() {
		Disciplina d = Disciplina.vazia()
			.aposPartida(PLANTEIS, partida(TipoEvento.CARTAO_AMARELO, ANA, TipoEvento.SEGUNDO_AMARELO, ANA), PADRAO,
					new SplittableRandom(1));

		assertThat(d.amarelosDe(1)).isEqualTo(2);
		assertThat(d.suspensaoDe(1)).isEqualTo(1);
	}

	@Test
	void comQuatroAmarelosDoisNaPartidaDaoDuasSuspensoesEContadorEmUm() {
		Disciplina d = new Disciplina(Map.of(1L, 4), Map.of())
			.aposPartida(PLANTEIS, partida(TipoEvento.CARTAO_AMARELO, ANA, TipoEvento.SEGUNDO_AMARELO, ANA), PADRAO,
					new SplittableRandom(1));

		assertThat(d.amarelosDe(1)).isEqualTo(1);
		assertThat(d.suspensaoDe(1)).isEqualTo(2);
	}

	@Test
	void vermelhoDiretoSorteiaDeUmATresJogosComUmMaisComum() {
		SplittableRandom aleatorio = new SplittableRandom(2026);
		int[] contagem = new int[4];
		for (int i = 0; i < 10_000; i++) {
			Disciplina d = Disciplina.vazia()
				.aposPartida(PLANTEIS, partida(TipoEvento.CARTAO_VERMELHO, BIA), PADRAO, aleatorio);
			contagem[d.suspensaoDe(2)]++;
		}

		assertThat(contagem[0]).isZero();
		assertThat(contagem[1]).isBetween(6_800, 7_200);
		assertThat(contagem[2]).isBetween(2_300, 2_700);
		assertThat(contagem[3]).isBetween(400, 600);
		assertThat(Arrays.stream(contagem).sum()).isEqualTo(10_000);
	}

	@Test
	void mesmaSementeMesmaSuspensao() {
		ResultadoPartida vermelho = partida(TipoEvento.CARTAO_VERMELHO, BIA);

		assertThat(IntStream.range(0, 20).map(s -> Disciplina.vazia()
			.aposPartida(PLANTEIS, vermelho, PADRAO, new SplittableRandom(s)).suspensaoDe(2)).toArray())
			.containsExactly(IntStream.range(0, 20).map(s -> Disciplina.vazia()
				.aposPartida(PLANTEIS, vermelho, PADRAO, new SplittableRandom(s)).suspensaoDe(2)).toArray());
	}

	@Test
	void novaTemporadaZeraAmarelosEMantemSuspensoes() {
		Disciplina d = new Disciplina(Map.of(1L, 3, 2L, 4), Map.of(2L, 2));

		Disciplina nova = d.novaTemporada();

		assertThat(nova.amarelos()).isEmpty();
		assertThat(nova.suspensaoDe(2)).isEqualTo(2);
	}

	@Test
	void recusaValoresNegativos() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Disciplina(Map.of(1L, -1), Map.of()));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosSuspensao(5, List.of(0, 0)));
	}

	/** Partida 0 a 0 com os eventos dados em pares (tipo, jogador). */
	private static ResultadoPartida partida(Object... tiposEJogadores) {
		List<EventoPartida> eventos = IntStream.iterate(0, i -> i < tiposEJogadores.length, i -> i + 2)
			.mapToObj(i -> new EventoPartida(10 + i, Lado.CASA, (TipoEvento) tiposEJogadores[i],
					(Jogador) tiposEJogadores[i + 1]))
			.toList();
		return new ResultadoPartida(0, 0, eventos);
	}
}
