package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;

/** Pênalti a favor do time do usuário: marcado num minuto e cobrado no seguinte, por quem ele escolher (SPEC §12, rev. 133). */
class PenaltiNaHoraTest {

	private static final Escalacao CASA = escalacao(Formacao.F_4_4_2, 25, 1);
	private static final Escalacao FORA = escalacao(Formacao.F_4_4_2, 25, 100);
	private final MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());

	@Test
	void oPenaltiDoUsuarioEsperaOMinutoSeguinteEOEscolhidoBate() {
		long semente = sementeComPenaltiDoUsuario();
		Partida partida = motor.iniciar(RegrasPartida.padrao(), CASA, FORA, Set.of(Lado.CASA), CondicoesPartida.NEUTRAS, semente);
		while (!partida.penaltiAguardandoCobranca(Lado.CASA)) {
			partida.avancarMinuto();
		}
		int marcado = partida.minuto();
		assertThat(partida.resultado().eventos()).as("ainda não cobrado").noneMatch(e -> e.penalti() && e.minuto() == marcado);
		Jogador escolhido = partida.emCampo(Lado.CASA).stream().filter(j -> j.posicao() == br.com.managerfoot.dominio.Posicao.DEFENSOR)
			.findFirst().orElseThrow();
		assertThatIllegalArgumentException().isThrownBy(() -> partida.escolherBatedor(Lado.FORA, escolhido));

		partida.escolherBatedor(Lado.CASA, escolhido);
		partida.avancarMinuto();

		assertThat(partida.penaltiAguardandoCobranca(Lado.CASA)).isFalse();
		EventoPartida cobranca = partida.resultado().eventos().stream().filter(e -> e.penalti() && e.lado() == Lado.CASA)
			.findFirst().orElseThrow();
		assertThat(cobranca.minuto()).as("a cobrança leva o minuto em que foi marcado").isEqualTo(marcado);
		assertThat(cobranca.jogador()).isEqualTo(escolhido);
	}

	@Test
	void naPartidaSoDaMaquinaOPenaltiECobradoNaHora() {
		long semente = sementeComPenaltiDoUsuario();
		Partida partida = motor.iniciar(RegrasPartida.padrao(), CASA, FORA, Set.of(), CondicoesPartida.NEUTRAS, semente);
		while (!partida.terminada()) {
			partida.avancarMinuto();
			assertThat(partida.penaltiAguardandoCobranca(Lado.CASA)).isFalse();
			assertThat(partida.penaltiAguardandoCobranca(Lado.FORA)).isFalse();
		}
	}

	/** A primeira semente em que a casa, do usuário, tem um pênalti marcado antes do último minuto. */
	private long sementeComPenaltiDoUsuario() {
		for (long semente = 0; semente < 2_000; semente++) {
			Partida p = motor.iniciar(RegrasPartida.padrao(), CASA, FORA, Set.of(Lado.CASA), CondicoesPartida.NEUTRAS, semente);
			while (!p.terminada()) {
				p.avancarMinuto();
				if (p.penaltiAguardandoCobranca(Lado.CASA)) {
					return semente;
				}
			}
		}
		throw new AssertionError("nenhuma semente com pênalti da casa");
	}
}
