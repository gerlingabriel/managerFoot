package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;

class JogoDecisivoTest {

	private final MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());
	private final Escalacao casa = escalacao(Formacao.F_4_4_2, 25, 1);
	private final Escalacao fora = escalacao(Formacao.F_4_4_2, 25, 100);
	private final CondicoesPartida volta00 = CondicoesPartida.NEUTRAS.comDecisivo(JogoDecisivo.volta(0, 0));

	private ResultadoPartida simular(CondicoesPartida condicoes, long semente) {
		return motor.simular(RegrasPartida.padrao(), casa, fora, condicoes, semente);
	}

	@Test
	void semJogoDecisivoAPartidaEmpatadaTerminaAos90() {
		LongStream.range(0, 300).forEach(s -> {
			ResultadoPartida r = simular(CondicoesPartida.NEUTRAS, s);
			assertThat(r.prorrogacao()).isFalse();
			assertThat(r.penaltis()).isEmpty();
		});
	}

	@Test
	void placarSomadoEmpatadoVaiAProrrogacaoDe30Minutos() {
		long semente = LongStream.range(0, 300)
			.filter(s -> {
				ResultadoPartida r = simular(CondicoesPartida.NEUTRAS, s);
				return r.golsCasa() == r.golsFora();
			})
			.findFirst()
			.orElseThrow();
		Partida partida = motor.iniciar(RegrasPartida.padrao(), casa, fora, Set.of(), volta00, semente);
		while (!partida.terminada()) {
			partida.avancarMinuto();
		}
		assertThat(partida.prorrogacao()).isTrue();
		assertThat(partida.minuto()).isEqualTo(120);
		assertThat(partida.minutoFinal()).isEqualTo(120);
	}

	@Test
	void osPrimeiros90MinutosSaoIguaisAosDaPartidaComum() {
		LongStream.range(0, 100).forEach(s -> {
			ResultadoPartida comum = simular(CondicoesPartida.NEUTRAS, s);
			ResultadoPartida decisiva = simular(volta00, s);
			List<EventoPartida> ate90 = decisiva.eventos().stream().filter(e -> e.minuto() <= 90).toList();
			assertThat(ate90).isEqualTo(comum.eventos());
		});
	}

	@Test
	void vencedorNoPlacarSomadoNaoTemProrrogacao() {
		// o mandante da volta perdeu a ida por 3 a 0: só empata o somado se fizer 3 gols a mais
		CondicoesPartida condicoes = CondicoesPartida.NEUTRAS.comDecisivo(JogoDecisivo.volta(0, 3));
		LongStream.range(0, 300).forEach(s -> {
			ResultadoPartida r = simular(condicoes, s);
			ResultadoPartida ate90 = simular(CondicoesPartida.NEUTRAS, s);
			assertThat(r.prorrogacao()).isEqualTo(ate90.golsCasa() == ate90.golsFora() + 3);
		});
	}

	@Test
	void prorrogacaoEmpatadaTerminaNosPenaltisComVencedor() {
		int disputas = 0;
		for (long s = 0; s < 1000; s++) {
			ResultadoPartida r = simular(volta00, s);
			if (r.prorrogacao() && r.golsCasa() == r.golsFora()) {
				assertThat(r.penaltis()).isPresent();
				disputas++;
			} else {
				assertThat(r.penaltis()).isEmpty();
			}
		}
		assertThat(disputas).isGreaterThan(50);
	}

	@Test
	void disputaSegueAsRegrasDeCobranca() {
		for (long s = 0; s < 2000; s++) {
			ResultadoPartida r = simular(volta00, s);
			if (r.penaltis().isEmpty()) {
				continue;
			}
			DisputaPenaltis disputa = r.penaltis().get();
			List<DisputaPenaltis.Cobranca> cobrancas = disputa.cobrancas();
			Lado vencedor = disputa.vencedor();
			assertThat(disputa.gols(vencedor)).isGreaterThan(disputa.gols(vencedor.adversario()));
			// os lados alternam
			for (int i = 1; i < cobrancas.size(); i++) {
				assertThat(cobrancas.get(i).lado()).isNotEqualTo(cobrancas.get(i - 1).lado());
			}
			long casa = cobrancas.stream().filter(c -> c.lado() == Lado.CASA).count();
			long foraCobradas = cobrancas.size() - casa;
			if (cobrancas.size() > 10) {
				assertThat(casa).isEqualTo(foraCobradas);
			} else {
				assertThat(cobrancas.size()).isGreaterThanOrEqualTo(6);
			}
			// batedores são de quem está do lado da cobrança
			cobrancas.forEach(c -> assertThat(c.batedor().id() < 100).isEqualTo(c.lado() == Lado.CASA));
		}
	}

	@Test
	void golsDaDisputaNaoEntramNoPlacarNemNosEventos() {
		for (long s = 0; s < 1000; s++) {
			ResultadoPartida r = simular(volta00, s);
			long gols = r.eventos().stream().filter(e -> e.tipo() == TipoEvento.GOL).count();
			assertThat(gols).isEqualTo(r.golsCasa() + r.golsFora());
		}
	}

	@Test
	void batedorEscolhidoAbreADisputaDoSeuLado() {
		Jogador escolhido = casa.titulares().stream().filter(j -> j.id() == 3).findFirst().orElseThrow();
		Escalacao comBatedor = new Escalacao(casa.formacao(), casa.titulares(), escolhido);
		for (long s = 0; s < 1000; s++) {
			ResultadoPartida r = motor.simular(RegrasPartida.padrao(), comBatedor, fora, volta00, s);
			if (casaPerdeuJogador(r)) {
				continue;
			}
			r.penaltis().ifPresent(d -> assertThat(
					d.cobrancas().stream().filter(c -> c.lado() == Lado.CASA).findFirst().orElseThrow().batedor())
				.isEqualTo(escolhido));
		}
	}

	@Test
	void goleiroBateDepoisDosJogadoresDeLinha() {
		for (long s = 0; s < 2000; s++) {
			ResultadoPartida r = simular(volta00, s);
			if (casaPerdeuJogador(r)) {
				continue;
			}
			r.penaltis().ifPresent(d -> {
				List<Jogador> daCasa = d.cobrancas().stream().filter(c -> c.lado() == Lado.CASA).map(c -> c.batedor()).toList();
				for (int i = 0; i < Math.min(daCasa.size(), 10); i++) {
					assertThat(daCasa.get(i).posicao()).isNotEqualTo(br.com.managerfoot.dominio.Posicao.GOLEIRO);
				}
			});
		}
	}

	/** O mandante teve expulso ou lesionado: a ordem da disputa muda com quem saiu. */
	private static boolean casaPerdeuJogador(ResultadoPartida r) {
		return r.eventos().stream().anyMatch(e -> e.lado() == Lado.CASA && (e.tipo().expulsao() || e.tipo() == TipoEvento.LESAO));
	}

	@Test
	void campoNeutroTiraAVantagemDoMando() {
		CondicoesPartida neutro = CondicoesPartida.NEUTRAS.comDecisivo(JogoDecisivo.FINAL);
		int[] comMando = vitoriasAos90(CondicoesPartida.NEUTRAS);
		int[] semMando = vitoriasAos90(neutro);
		assertThat(comMando[0]).isGreaterThan(comMando[1] * 12 / 10);
		assertThat(Math.abs(semMando[0] - semMando[1])).isLessThan((semMando[0] + semMando[1]) / 10);
	}

	/** Vitórias do mandante e do visitante nos 90 minutos, entre times iguais. */
	private int[] vitoriasAos90(CondicoesPartida condicoes) {
		int[] vitorias = new int[2];
		for (long s = 0; s < 4000; s++) {
			Partida partida = motor.iniciar(RegrasPartida.padrao(), casa, fora, Set.of(), condicoes, s);
			while (partida.minuto() < 90) {
				partida.avancarMinuto();
			}
			ResultadoPartida r = partida.resultado();
			if (r.golsCasa() > r.golsFora()) {
				vitorias[0]++;
			} else if (r.golsFora() > r.golsCasa()) {
				vitorias[1]++;
			}
		}
		return vitorias;
	}

	@Test
	void mesmaSementeReproduzProrrogacaoEPenaltis() {
		LongStream.range(0, 200).forEach(s -> assertThat(simular(volta00, s)).isEqualTo(simular(volta00, s)));
	}

	@Test
	void condicoesComDecisivoMantemOResto() {
		CondicoesPartida c = new CondicoesPartida(Map.of(), Set.of(7L), Map.of(Lado.CASA, 1));
		CondicoesPartida d = c.comDecisivo(JogoDecisivo.FINAL);
		assertThat(d.bonusExtra()).isEqualTo(c.bonusExtra());
		assertThat(d.lesoesPermitidas()).isEqualTo(c.lesoesPermitidas());
		assertThat(d.decisivo()).contains(JogoDecisivo.FINAL);
	}
}
