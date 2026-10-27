package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static br.com.managerfoot.motor.EscalacoesDeTeste.titulares;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

/** Substituições (SPEC §7.4.1), com a casa controlada pelo usuário. */
class SubstituicaoTest {

	private static final Jogador GOLEIRO_RESERVA = reserva(901, Posicao.GOLEIRO, 22);
	private static final Jogador ZAGUEIRO_RESERVA = reserva(902, Posicao.DEFENSOR, 20);
	private static final Jogador MEIA_RESERVA = reserva(903, Posicao.MEIO_CAMPISTA, 20);
	private static final Jogador ATACANTE_RESERVA = reserva(904, Posicao.ATACANTE, 20);
	private static final Jogador ATACANTE_RESERVA_2 = reserva(905, Posicao.ATACANTE, 20);

	private final List<Jogador> titulares = titulares(Formacao.F_4_4_2, 25, 1, Temperamento.NORMAL);
	private final Escalacao casa = new Escalacao(Formacao.F_4_4_2, titulares,
			List.of(GOLEIRO_RESERVA, ZAGUEIRO_RESERVA, MEIA_RESERVA, ATACANTE_RESERVA, ATACANTE_RESERVA_2));

	private static Jogador reserva(long id, Posicao posicao, int forca) {
		return new Jogador(id, "R" + id, posicao, forca, Temperamento.NORMAL);
	}

	private Partida partida(int limite) {
		return new Partida(ParametrosMotor.padrao(), new RegrasPartida(limite), casa,
				escalacao(Formacao.F_4_4_2, 25, 100), Set.of(Lado.CASA), new SplittableRandom(1));
	}

	private Jogador titular(Posicao posicao) {
		return titulares.stream().filter(j -> j.posicao() == posicao).findFirst().orElseThrow();
	}

	@Test
	void trocarZagueiroPorAtacanteMudaA442Para343() {
		Partida partida = partida(5);
		Jogador zagueiro = titular(Posicao.DEFENSOR);

		EventoPartida evento = partida.substituir(Lado.CASA, zagueiro, ATACANTE_RESERVA);

		assertThat(partida.formacaoAtual(Lado.CASA)).isEqualTo(Formacao.F_3_4_3);
		assertThat(partida.emCampo(Lado.CASA)).contains(ATACANTE_RESERVA).doesNotContain(zagueiro).hasSize(11);
		assertThat(evento.tipo()).isEqualTo(TipoEvento.SUBSTITUICAO);
		assertThat(evento.jogador()).isEqualTo(ATACANTE_RESERVA);
		assertThat(evento.substituido()).isEqualTo(zagueiro);
	}

	@Test
	void quemEntrouSaiDoBancoEQuemSaiuNaoVolta() {
		Partida partida = partida(5);
		Jogador meia = titular(Posicao.MEIO_CAMPISTA);
		partida.substituir(Lado.CASA, meia, MEIA_RESERVA);

		assertThat(partida.reservasDisponiveis(Lado.CASA)).doesNotContain(MEIA_RESERVA, meia);
		assertThat(partida.substituicoesRestantes(Lado.CASA)).isEqualTo(4);
		assertThatIllegalArgumentException().isThrownBy(() -> partida.substituir(Lado.CASA, MEIA_RESERVA, meia))
			.withMessageContaining("não está disponível no banco");
	}

	@Test
	void respeitaOLimiteDaCompeticao() {
		Partida partida = partida(1);
		partida.substituir(Lado.CASA, titular(Posicao.MEIO_CAMPISTA), MEIA_RESERVA);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> partida.substituir(Lado.CASA, titular(Posicao.ATACANTE), ATACANTE_RESERVA))
			.withMessageContaining("sem substituições restantes");
	}

	@Test
	void recusaTrocaQueGeraFormacaoForaDaLista() {
		// 4-4-2 -> 3-4-3 -> (zagueiro por atacante) 2-4-4, que não existe
		Partida partida = partida(5);
		List<Jogador> zagueiros = titulares.stream().filter(j -> j.posicao() == Posicao.DEFENSOR).toList();
		partida.substituir(Lado.CASA, zagueiros.get(0), ATACANTE_RESERVA);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> partida.substituir(Lado.CASA, zagueiros.get(1), ATACANTE_RESERVA_2))
			.withMessageContaining("fora das permitidas")
			.withMessageContaining("3-4-3");
		assertThat(partida.formacaoAtual(Lado.CASA)).isEqualTo(Formacao.F_3_4_3);
		assertThat(partida.substituicoesRestantes(Lado.CASA)).isEqualTo(4);
	}

	@Test
	void naoSubstituiQuemNaoEstaEmCampo() {
		Partida partida = partida(5);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> partida.substituir(Lado.CASA, ATACANTE_RESERVA_2, ATACANTE_RESERVA))
			.withMessageContaining("não está em campo");
	}

	@Test
	void goleiroReservaSoEntraNoLugarDoGoleiro() {
		Partida partida = partida(5);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> partida.substituir(Lado.CASA, titular(Posicao.ATACANTE), GOLEIRO_RESERVA))
			.withMessageContaining("só pode entrar no lugar do goleiro");

		partida.substituir(Lado.CASA, titular(Posicao.GOLEIRO), GOLEIRO_RESERVA);
		assertThat(partida.goleiro(Lado.CASA)).isEqualTo(GOLEIRO_RESERVA);
		assertThat(partida.formacaoAtual(Lado.CASA)).isEqualTo(Formacao.F_4_4_2);
	}

	@Test
	void jogadorDeLinhaNaoEntraNoGol() {
		Partida partida = partida(5);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> partida.substituir(Lado.CASA, titular(Posicao.GOLEIRO), ZAGUEIRO_RESERVA))
			.withMessageContaining("só um goleiro pode entrar");
	}

	@Test
	void naoSubstituiDepoisDoFim() {
		Partida partida = partida(5);
		while (!partida.terminada()) {
			partida.avancarMinuto();
		}
		assertThatIllegalStateException()
			.isThrownBy(() -> partida.substituir(Lado.CASA, titular(Posicao.ATACANTE), ATACANTE_RESERVA));
	}

	@Test
	void comGoleiroImprovisadoOReservaEntraEOImprovisadoVoltaADefesa() {
		TimeEmCampo time = new TimeEmCampo(casa, 0.5, 5);
		time.expulsar(time.goleiro());
		Jogador improvisado = time.goleiro();
		Jogador atacante = time.jogadoresNa(Posicao.ATACANTE).getFirst();

		time.substituir(atacante, GOLEIRO_RESERVA);

		assertThat(time.goleiro()).isEqualTo(GOLEIRO_RESERVA);
		assertThat(time.goleiroImprovisado()).isFalse();
		assertThat(time.jogadoresNa(Posicao.DEFENSOR)).contains(improvisado).hasSize(4);
		assertThat(time.jogadoresNa(Posicao.ATACANTE)).hasSize(1);
		assertThat(time.emCampo()).hasSize(10);
		assertThat(time.formacao()).isEqualTo(Formacao.F_4_4_2);
	}

	@Test
	void maquinaPoeGoleiroReservaNoLugarDoAtacanteMaisFraco() {
		List<Jogador> lista = new java.util.ArrayList<>(titulares);
		Jogador atacanteFraco = new Jogador(lista.get(10).id(), "Fraco", Posicao.ATACANTE, 10, Temperamento.NORMAL);
		lista.set(10, atacanteFraco);
		TimeEmCampo time = new TimeEmCampo(new Escalacao(Formacao.F_4_4_2, lista, List.of(GOLEIRO_RESERVA)), 0.5, 5);
		time.expulsar(time.goleiro());

		TreinadorMaquina.Troca troca = TreinadorMaquina.aposGoleiroExpulso(time).orElseThrow();

		assertThat(troca.sai()).isEqualTo(atacanteFraco);
		assertThat(troca.entra()).isEqualTo(GOLEIRO_RESERVA);
	}

	@Test
	void maquinaNaoTrocaSemGoleiroNoBancoOuSemSubstituicoes() {
		TimeEmCampo semGoleiro = new TimeEmCampo(new Escalacao(Formacao.F_4_4_2, titulares, List.of(MEIA_RESERVA)), 0.5, 5);
		semGoleiro.expulsar(semGoleiro.goleiro());
		assertThat(TreinadorMaquina.aposGoleiroExpulso(semGoleiro)).isEmpty();

		TimeEmCampo semTrocas = new TimeEmCampo(new Escalacao(Formacao.F_4_4_2, titulares, List.of(GOLEIRO_RESERVA)), 0.5, 0);
		semTrocas.expulsar(semTrocas.goleiro());
		assertThat(TreinadorMaquina.aposGoleiroExpulso(semTrocas)).isEmpty();
	}

	@Test
	void maquinaReageAoGoleiroExpulsoDuranteASimulacao() {
		// goleiro violento e o resto calmo: expulsões de goleiro aparecem nas sementes
		List<Jogador> lista = new java.util.ArrayList<>(titulares(Formacao.F_4_4_2, 25, 1, Temperamento.CALMO));
		Jogador goleiro = lista.getFirst();
		lista.set(0, new Jogador(goleiro.id(), goleiro.nome(), Posicao.GOLEIRO, 25, Temperamento.VIOLENTO));
		Escalacao maquina = new Escalacao(Formacao.F_4_4_2, lista, List.of(GOLEIRO_RESERVA));
		MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());
		int verificadas = 0;

		for (long s = 0; s < 3000; s++) {
			List<EventoPartida> eventos = motor.simular(maquina, escalacao(Formacao.F_4_4_2, 25, 100), s).eventos();
			for (int i = 0; i < eventos.size(); i++) {
				EventoPartida e = eventos.get(i);
				// só o titular: se o reserva também for expulso, não há outro goleiro no banco
				if (e.lado() == Lado.CASA && e.tipo().expulsao() && e.jogador().id() == goleiro.id()) {
					EventoPartida seguinte = eventos.get(i + 1);
					assertThat(seguinte.tipo()).isEqualTo(TipoEvento.SUBSTITUICAO);
					assertThat(seguinte.jogador()).isEqualTo(GOLEIRO_RESERVA);
					assertThat(seguinte.minuto()).isEqualTo(e.minuto());
					verificadas++;
				}
			}
		}
		assertThat(verificadas).as("as sementes devem cobrir goleiros expulsos").isPositive();
	}
}
