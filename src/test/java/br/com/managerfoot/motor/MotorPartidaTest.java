package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;

class MotorPartidaTest {

	private final MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());
	private final Escalacao casa = escalacao(Formacao.F_4_4_2, 25, 1);
	private final Escalacao fora = escalacao(Formacao.F_4_3_3, 25, 100);

	@Test
	void mesmaSementeReproduzAMesmaPartida() {
		assertThat(motor.simular(casa, fora, 42)).isEqualTo(motor.simular(casa, fora, 42));
	}

	@Test
	void sementesDiferentesProduzemPlacaresVariados() {
		Set<String> placares = new HashSet<>();
		LongStream.range(0, 100).forEach(s -> {
			ResultadoPartida r = motor.simular(casa, fora, s);
			placares.add(r.golsCasa() + "x" + r.golsFora());
		});
		assertThat(placares).hasSizeGreaterThan(5);
	}

	@Test
	void simulacaoRapidaEMinutoAMinutoDaoOMesmoResultado() {
		Partida partida = motor.iniciar(casa, fora, 7);
		while (!partida.terminada()) {
			partida.avancarMinuto();
		}
		assertThat(partida.resultado()).isEqualTo(motor.simular(casa, fora, 7));
	}

	@Test
	void partidaTem90MinutosENaoAvancaDepoisDoFim() {
		Partida partida = motor.iniciar(casa, fora, 1);
		for (int i = 0; i < 90; i++) {
			partida.avancarMinuto();
		}
		assertThat(partida.terminada()).isTrue();
		assertThat(partida.minuto()).isEqualTo(90);
		assertThatIllegalStateException().isThrownBy(partida::avancarMinuto);
	}

	@Test
	void placarConfereComEventosDeGolEFinalizadorPertenceAoTime() {
		LongStream.range(0, 200).forEach(s -> {
			ResultadoPartida r = motor.simular(casa, fora, s);
			List<EventoPartida> eventos = r.eventos();

			assertThat(eventos.stream().filter(e -> e.tipo() == TipoEvento.GOL && e.lado() == Lado.CASA).count())
				.isEqualTo(r.golsCasa());
			assertThat(eventos.stream().filter(e -> e.tipo() == TipoEvento.GOL && e.lado() == Lado.FORA).count())
				.isEqualTo(r.golsFora());
			assertThat(eventos).allSatisfy(e -> {
				assertThat(e.minuto()).isBetween(1, 90);
				Escalacao time = e.lado() == Lado.CASA ? casa : fora;
				assertThat(time.titulares()).contains(e.jogador());
			});
			assertThat(eventos).isSortedAccordingTo((a, b) -> Integer.compare(a.minuto(), b.minuto()));
		});
	}

	@Test
	void timeMaisForteVenceMaisVezesMasNaoSempre() {
		// conjunto fixo de sementes: resultado determinístico, sem teste estatístico frágil.
		// Diferenças extremas (ex.: 10 x 45) são tratadas na calibração (Etapa 1d).
		Escalacao forte = escalacao(Formacao.F_4_4_2, 30, 1);
		Escalacao fraco = escalacao(Formacao.F_4_4_2, 20, 100);
		int vitoriasForte = 0;
		int vitoriasFraco = 0;
		for (long s = 0; s < 1000; s++) {
			ResultadoPartida r = motor.simular(fraco, forte, s);
			if (r.golsFora() > r.golsCasa()) {
				vitoriasForte++;
			} else if (r.golsCasa() > r.golsFora()) {
				vitoriasFraco++;
			}
		}
		// razão de 2,8 a 3,8 entre lotes de 1000 sementes: o limite fica abaixo da variação
		assertThat(vitoriasForte).isGreaterThan(vitoriasFraco * 2);
		assertThat(vitoriasFraco).isPositive();
	}

	@Test
	void timeQueComecaComUmAMenosPerdeMaisSemPerderSempre() {
		// SPEC §9.3: com menos de 11 disponíveis, o time joga como se começasse com um expulso
		Escalacao completo = escalacao(Formacao.F_4_4_2, 25, 1);
		Escalacao incompleto = new Escalacao(Formacao.F_4_4_2, completo.titulares().subList(0, 10));
		Escalacao adversario = escalacao(Formacao.F_4_4_2, 25, 100);
		int golsCompleto = 0;
		int golsIncompleto = 0;
		int vitoriasIncompleto = 0;
		for (long s = 0; s < 1000; s++) {
			golsCompleto += motor.simular(completo, adversario, s).golsCasa();
			ResultadoPartida r = motor.simular(incompleto, adversario, s);
			golsIncompleto += r.golsCasa();
			if (r.golsCasa() > r.golsFora()) {
				vitoriasIncompleto++;
			}
		}
		assertThat(incompleto.incompleta()).isTrue();
		assertThat(golsIncompleto).isLessThan(golsCompleto);
		assertThat(vitoriasIncompleto).isPositive();
	}
}
