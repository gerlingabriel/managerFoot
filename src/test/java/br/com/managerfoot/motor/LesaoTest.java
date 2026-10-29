package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static br.com.managerfoot.motor.EscalacoesDeTeste.titulares;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

/** Lesões na partida (SPEC §7.5, rev. 69), com sementes fixas e frequência alta para ter casos. */
class LesaoTest {

	private static final ParametrosLesao PADRAO = ParametrosLesao.padrao();
	/** Uma lesão a cada ~20 minutos por time, com o limite de 2 da temporada. */
	private static final ParametrosMotor FREQUENTE = ParametrosMotor.padrao()
		.comLesao(new ParametrosLesao(0.05, PADRAO.pesosTemperamento(), PADRAO.gravidades(), 2));

	private static final List<Jogador> BANCO = List.of(reserva(901, Posicao.GOLEIRO), reserva(902, Posicao.DEFENSOR),
			reserva(903, Posicao.MEIO_CAMPISTA), reserva(904, Posicao.ATACANTE), reserva(905, Posicao.MEIO_CAMPISTA));

	private static Jogador reserva(long id, Posicao posicao) {
		return new Jogador(id, "R" + id, posicao, 20, Temperamento.NORMAL, "BRA");
	}

	private static List<EventoPartida> lesoes(ResultadoPartida resultado) {
		return resultado.eventos().stream().filter(e -> e.tipo() == TipoEvento.LESAO).toList();
	}

	@Test
	void respeitaOLimiteDaTemporadaEAsFaixasDeGravidade() {
		MotorPartida motor = new MotorPartida(FREQUENTE);
		int total = 0;
		for (long s = 0; s < 300; s++) {
			List<EventoPartida> lesoes = lesoes(motor.simular(escalacao(Formacao.F_4_4_2, 25, 1),
					escalacao(Formacao.F_4_4_2, 25, 100), s));
			total += lesoes.size();
			for (Lado lado : Lado.values()) {
				assertThat(lesoes.stream().filter(e -> e.lado() == lado)).hasSizeLessThanOrEqualTo(2);
			}
			for (EventoPartida e : lesoes) {
				ParametrosLesao.Faixa faixa = PADRAO.gravidades().get(e.lesao().gravidade());
				assertThat(e.lesao().jogos()).isBetween(faixa.jogosMinimo(), faixa.jogosMaximo());
				assertThat(e.lesao().perdaForca()).isBetween(faixa.perdaMinima(), faixa.perdaMaxima());
			}
		}
		assertThat(total).isPositive();
	}

	@Test
	void clubeQueJaAtingiuOLimiteNaoSeLesiona() {
		CondicoesPartida semLesoes = new CondicoesPartida(Map.of(), Set.of(), Map.of(Lado.CASA, 0, Lado.FORA, 0));
		for (long s = 0; s < 100; s++) {
			Partida partida = new Partida(FREQUENTE, RegrasPartida.padrao(), escalacao(Formacao.F_4_4_2, 25, 1),
					escalacao(Formacao.F_4_4_2, 25, 100), Set.of(), semLesoes, new SplittableRandom(s));
			while (!partida.terminada()) {
				partida.avancarMinuto();
			}
			assertThat(lesoes(partida.resultado())).isEmpty();
		}
	}

	@Test
	void calmoSeLesionaMaisQueViolento() {
		// metade do time calma e metade violenta, nas mesmas posições
		List<Jogador> misto = new ArrayList<>();
		for (Jogador j : titulares(Formacao.F_4_4_2, 25, 1, Temperamento.NORMAL)) {
			Temperamento t = j.id() % 2 == 0 ? Temperamento.CALMO : Temperamento.VIOLENTO;
			misto.add(new Jogador(j.id(), j.nome(), j.posicao(), j.forca(), t, "BRA"));
		}
		Escalacao casa = new Escalacao(Formacao.F_4_4_2, misto);
		MotorPartida motor = new MotorPartida(FREQUENTE);
		Map<Temperamento, Integer> porTemperamento = new EnumMap<>(Temperamento.class);
		for (long s = 0; s < 500; s++) {
			lesoes(motor.simular(casa, escalacao(Formacao.F_4_4_2, 25, 100), s)).stream()
				.filter(e -> e.lado() == Lado.CASA)
				.forEach(e -> porTemperamento.merge(e.jogador().temperamento(), 1, Integer::sum));
		}
		assertThat(porTemperamento.getOrDefault(Temperamento.CALMO, 0))
			.isGreaterThan(porTemperamento.getOrDefault(Temperamento.VIOLENTO, 0) * 3);
	}

	@Test
	void maquinaTrocaOLesionadoPeloReservaDaMesmaPosicao() {
		Escalacao casa = new Escalacao(Formacao.F_4_4_2, titulares(Formacao.F_4_4_2, 25, 1, Temperamento.NORMAL), BANCO);
		MotorPartida motor = new MotorPartida(FREQUENTE);
		int trocas = 0;
		for (long s = 0; s < 100; s++) {
			List<EventoPartida> eventos = motor.simular(casa, escalacao(Formacao.F_4_4_2, 25, 100), s).eventos();
			List<Jogador> noBanco = new ArrayList<>(BANCO);
			for (int i = 0; i < eventos.size(); i++) {
				EventoPartida e = eventos.get(i);
				if (e.tipo() == TipoEvento.SUBSTITUICAO && e.lado() == Lado.CASA) {
					noBanco.remove(e.jogador());
				}
				if (e.tipo() != TipoEvento.LESAO || e.lado() != Lado.CASA) {
					continue;
				}
				EventoPartida seguinte = eventos.get(i + 1);
				assertThat(seguinte.tipo()).isEqualTo(TipoEvento.SUBSTITUICAO);
				assertThat(seguinte.substituido()).isEqualTo(e.jogador());
				assertThat(seguinte.minuto()).isEqualTo(e.minuto());
				if (noBanco.stream().anyMatch(j -> j.posicao() == e.jogador().posicao())) {
					assertThat(seguinte.jogador().posicao()).isEqualTo(e.jogador().posicao());
				}
				trocas++;
			}
		}
		assertThat(trocas).isPositive();
	}

	@Test
	void semReservaOTimeFicaComUmAMenos() {
		for (long s = 0; s < 50; s++) {
			Partida partida = new Partida(FREQUENTE, escalacao(Formacao.F_4_4_2, 25, 1),
					escalacao(Formacao.F_4_4_2, 25, 100), new SplittableRandom(s));
			while (!partida.terminada()) {
				partida.avancarMinuto();
			}
			long saidas = partida.resultado().eventos().stream()
				.filter(e -> e.lado() == Lado.CASA && (e.tipo() == TipoEvento.LESAO || e.tipo().expulsao()))
				.count();
			assertThat(partida.emCampo(Lado.CASA)).hasSize(11 - (int) saidas);
		}
	}

	@Test
	void lesionadoDoUsuarioEsperaATrocaDele() {
		Escalacao casa = new Escalacao(Formacao.F_4_4_2, titulares(Formacao.F_4_4_2, 25, 1, Temperamento.NORMAL), BANCO);
		Partida partida = partidaAteALesao(casa);
		Jogador lesionado = partida.lesionadoAguardandoTroca(Lado.CASA).orElseThrow();
		assertThat(partida.emCampo(Lado.CASA)).contains(lesionado);

		Jogador entra = BANCO.stream()
			.filter(j -> lesionado.posicao() == Posicao.GOLEIRO ? j.posicao() == Posicao.GOLEIRO
					: j.posicao() == lesionado.posicao())
			.findFirst()
			.orElseThrow();
		partida.substituir(Lado.CASA, lesionado, entra);

		assertThat(partida.lesionadoAguardandoTroca(Lado.CASA)).isEmpty();
		partida.avancarMinuto();
		assertThat(partida.emCampo(Lado.CASA)).contains(entra).doesNotContain(lesionado);
	}

	@Test
	void semATrocaDoUsuarioAMaquinaDecideNoMinutoSeguinte() {
		Escalacao casa = new Escalacao(Formacao.F_4_4_2, titulares(Formacao.F_4_4_2, 25, 1, Temperamento.NORMAL), BANCO);
		Partida partida = partidaAteALesao(casa);
		Jogador lesionado = partida.lesionadoAguardandoTroca(Lado.CASA).orElseThrow();
		int minutoDaLesao = partida.minuto();

		partida.avancarMinuto();

		assertThat(partida.lesionadoAguardandoTroca(Lado.CASA)).isEmpty();
		assertThat(partida.emCampo(Lado.CASA)).doesNotContain(lesionado).hasSize(11);
		assertThat(partida.resultado().eventos())
			.anyMatch(e -> e.tipo() == TipoEvento.SUBSTITUICAO && e.substituido().equals(lesionado)
					&& e.minuto() == minutoDaLesao);
	}

	/** Primeira semente em que a casa (do usuário) tem uma lesão, parada no minuto dela. */
	private static Partida partidaAteALesao(Escalacao casa) {
		for (long s = 0; s < 100; s++) {
			Partida partida = new Partida(FREQUENTE, RegrasPartida.padrao(), casa, escalacao(Formacao.F_4_4_2, 25, 100),
					Set.of(Lado.CASA), new SplittableRandom(s));
			while (partida.minuto() < 80) {
				partida.avancarMinuto();
				if (partida.lesionadoAguardandoTroca(Lado.CASA).isPresent()) {
					return partida;
				}
			}
		}
		throw new AssertionError("nenhuma semente com lesão da casa");
	}
}
