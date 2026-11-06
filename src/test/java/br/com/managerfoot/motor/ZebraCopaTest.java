package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;

/** Zebra da copa nacional (SPEC §8.2, rev. 117). Sementes fixas: o resultado se repete. */
class ZebraCopaTest {

	private static final int PARTIDAS = 4_000;
	private static final MotorPartida MOTOR = new MotorPartida(ParametrosMotor.padrao());
	private static final CondicoesPartida PRIMEIRAS = CondicoesPartida.NEUTRAS.comCopa(new CopaNacional(true));
	private static final CondicoesPartida QUARTAS = CondicoesPartida.NEUTRAS.comCopa(new CopaNacional(false));
	private static final Escalacao FRACO = escalacao(Formacao.F_4_4_2, 20, 1);
	private static final Escalacao FORTE = escalacao(Formacao.F_4_4_2, 40, 100);

	@Test
	void foraDaCopaOuComDiferencaAbaixoDoLimiteNadaMuda() {
		Escalacao perto = escalacao(Formacao.F_4_4_2, 34, 100);
		for (long semente = 0; semente < 300; semente++) {
			assertThat(iniciar(FRACO, FORTE, CondicoesPartida.NEUTRAS, semente).ladoComBonusDeCopa()).isEmpty();
			// 14 de diferença: sem bônus e sem sorteio do super bônus, a partida sai igual à de fora da copa
			assertThat(MOTOR.simular(RegrasPartida.padrao(), FRACO, perto, PRIMEIRAS, semente).eventos())
				.isEqualTo(MOTOR.simular(RegrasPartida.padrao(), FRACO, perto, CondicoesPartida.NEUTRAS, semente).eventos());
		}
	}

	@Test
	void oMaisFracoGanhaOBonusEmCasaOuFora() {
		assertThat(iniciar(FRACO, FORTE, QUARTAS, 0).ladoComBonusDeCopa()).contains(Lado.CASA);
		assertThat(iniciar(FORTE, FRACO, QUARTAS, 0).ladoComBonusDeCopa()).contains(Lado.FORA);
	}

	@Test
	void superBonusSoNasPrimeirasFasesEmCercaDe10PorCento() {
		int nasPrimeiras = 0;
		int nasQuartas = 0;
		for (long semente = 0; semente < PARTIDAS; semente++) {
			nasPrimeiras += iniciar(FRACO, FORTE, PRIMEIRAS, semente).comSuperBonusDeCopa() ? 1 : 0;
			nasQuartas += iniciar(FRACO, FORTE, QUARTAS, semente).comSuperBonusDeCopa() ? 1 : 0;
		}
		assertThat(nasQuartas).isZero();
		assertThat((double) nasPrimeiras / PARTIDAS).isBetween(0.08, 0.12);
	}

	@Test
	void oBonusReduzADesigualdadeSemIgualarEOSuperBonusDaMaisSurpresas() {
		double sem = vitoriasDoFraco(CondicoesPartida.NEUTRAS);
		double comBonus = vitoriasDoFraco(QUARTAS);
		double comSuper = vitoriasDoFraco(PRIMEIRAS);

		// 20 de diferença vale como 16: o fraco vence uns 5 pontos a mais (11,6% → 16,7% com estas sementes)
		assertThat(comBonus).isGreaterThan(sem + 0.03);
		// o forte segue favorito
		assertThat(comBonus).isLessThan(0.35);
		assertThat(comSuper).isGreaterThan(comBonus);
	}

	@Test
	void recusaParametrosInvalidos() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosZebraCopa(-1, 0.8, 0.1, 50));
		// fração 1 deixaria o mais fraco igual ao forte
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosZebraCopa(15, 1, 0.1, 50));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosZebraCopa(15, 0.8, 1.1, 50));
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametrosZebraCopa(15, 0.8, 0.1, 0));
	}

	private static double vitoriasDoFraco(CondicoesPartida condicoes) {
		int vitorias = 0;
		for (long semente = 0; semente < PARTIDAS; semente++) {
			ResultadoPartida r = MOTOR.simular(RegrasPartida.padrao(), FRACO, FORTE, condicoes, semente);
			vitorias += r.golsCasa() > r.golsFora() ? 1 : 0;
		}
		return (double) vitorias / PARTIDAS;
	}

	private static Partida iniciar(Escalacao casa, Escalacao fora, CondicoesPartida condicoes, long semente) {
		return MOTOR.iniciar(RegrasPartida.padrao(), casa, fora, Set.of(), condicoes, semente);
	}
}
