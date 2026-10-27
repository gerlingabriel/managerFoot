package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static br.com.managerfoot.motor.EscalacoesDeTeste.titulares;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Temperamento;

/** Regras de cartões, expulsões e pênaltis verificadas em um conjunto fixo de sementes. */
class PartidaDisciplinaTest {

	private static final int PARTIDAS = 2000;
	private final MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());

	@Test
	void segundoAmareloSoDepoisDoPrimeiroEExpulsoNaoApareceMais() {
		Escalacao casa = escalacao(Formacao.F_4_4_2, 25, 1, Temperamento.VIOLENTO);
		Escalacao fora = escalacao(Formacao.F_4_3_3, 25, 100, Temperamento.ANSIOSO);
		int expulsoesVerificadas = 0;

		for (long s = 0; s < PARTIDAS; s++) {
			Map<Jogador, Integer> amarelos = new HashMap<>();
			Set<Jogador> expulsos = new HashSet<>();
			for (EventoPartida e : motor.simular(casa, fora, s).eventos()) {
				assertThat(expulsos).as("semente %d: %s agiu depois de expulso", s, e.jogador().nome())
					.doesNotContain(e.jogador());
				switch (e.tipo()) {
					case CARTAO_AMARELO -> assertThat(amarelos.merge(e.jogador(), 1, Integer::sum)).isEqualTo(1);
					case SEGUNDO_AMARELO -> assertThat(amarelos.merge(e.jogador(), 1, Integer::sum)).isEqualTo(2);
					default -> {
					}
				}
				if (e.tipo().expulsao()) {
					expulsos.add(e.jogador());
					expulsoesVerificadas++;
				}
			}
		}
		assertThat(expulsoesVerificadas).as("as sementes devem cobrir expulsões").isPositive();
	}

	@Test
	void timeViolentoRecebeMaisCartoesQueTimeCalmo() {
		Escalacao violento = escalacao(Formacao.F_4_4_2, 25, 1, Temperamento.VIOLENTO);
		Escalacao calmo = escalacao(Formacao.F_4_4_2, 25, 100, Temperamento.CALMO);
		long cartoesViolento = 0;
		long cartoesCalmo = 0;
		for (long s = 0; s < PARTIDAS; s++) {
			for (EventoPartida e : motor.simular(violento, calmo, s).eventos()) {
				if (e.tipo() == TipoEvento.CARTAO_AMARELO || e.tipo().expulsao()) {
					if (e.lado() == Lado.CASA) {
						cartoesViolento++;
					} else {
						cartoesCalmo++;
					}
				}
			}
		}
		assertThat(cartoesViolento).isGreaterThan(cartoesCalmo);
	}

	@Test
	void dentroDoTimeOViolentoEhOMaisPunido() {
		List<Jogador> titulares = new java.util.ArrayList<>(titulares(Formacao.F_4_4_2, 25, 1, Temperamento.CALMO));
		Jogador violento = new Jogador(titulares.get(6).id(), "Violento", titulares.get(6).posicao(), 25,
				Temperamento.VIOLENTO, "BRA");
		titulares.set(6, violento);
		Escalacao casa = new Escalacao(Formacao.F_4_4_2, titulares);
		Escalacao fora = escalacao(Formacao.F_4_4_2, 25, 100);

		Map<Jogador, Long> cartoes = new HashMap<>();
		for (long s = 0; s < PARTIDAS; s++) {
			motor.simular(casa, fora, s).eventos().stream()
				.filter(e -> e.lado() == Lado.CASA && (e.tipo() == TipoEvento.CARTAO_AMARELO || e.tipo().expulsao()))
				.forEach(e -> cartoes.merge(e.jogador(), 1L, Long::sum));
		}
		long maximo = cartoes.values().stream().mapToLong(Long::longValue).max().orElseThrow();
		assertThat(cartoes.get(violento)).isEqualTo(maximo);
	}

	@Test
	void penaltiEhCobradoPeloBatedorEscolhido() {
		List<Jogador> titulares = titulares(Formacao.F_4_4_2, 25, 1, Temperamento.CALMO);
		Jogador escolhido = titulares.get(2); // defensor
		Escalacao casa = new Escalacao(Formacao.F_4_4_2, titulares, escolhido);
		Escalacao fora = escalacao(Formacao.F_4_4_2, 25, 100, Temperamento.CALMO);
		int penaltisDaCasa = 0;

		for (long s = 0; s < PARTIDAS; s++) {
			List<EventoPartida> eventos = motor.simular(casa, fora, s).eventos();
			boolean escolhidoExpulso = false;
			for (EventoPartida e : eventos) {
				if (e.tipo().expulsao() && e.jogador().equals(escolhido)) {
					escolhidoExpulso = true;
				}
				if (e.penalti() && e.lado() == Lado.CASA) {
					penaltisDaCasa++;
					if (!escolhidoExpulso) {
						assertThat(e.jogador()).isEqualTo(escolhido);
					}
				}
			}
		}
		assertThat(penaltisDaCasa).as("as sementes devem cobrir pênaltis").isPositive();
	}
}
