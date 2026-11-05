package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Formacao;

/** Jogo aberto pelos atacantes em campo (SPEC §7.2, rev. 107). Sementes fixas: o resultado se repete. */
class JogoAbertoTest {

	private static final int PARTIDAS = 3_000;
	private static final List<Double> SEM_ABERTURA = List.of(1.0, 1.0, 1.0, 1.0, 1.0);

	@Test
	void ateDoisAtacantesNadaMudaETresEQuatroAbremOJogo() {
		ParametrosMotor p = ParametrosMotor.padrao();
		assertThat(p.abertura(1)).isEqualTo(1.0);
		assertThat(p.abertura(2)).isEqualTo(1.0);
		assertThat(p.abertura(3)).isGreaterThan(1.0);
		assertThat(p.abertura(4)).isGreaterThan(p.abertura(3));
	}

	@Test
	void quatroAtacantesFazemESofremMaisGolsQueSemOJogoAberto() {
		EstatisticasLote com = medir(ParametrosMotor.padrao(), Formacao.F_4_2_4);
		EstatisticasLote sem = medir(ParametrosMotor.padrao().comAbertura(SEM_ABERTURA), Formacao.F_4_2_4);

		assertThat(com.porPartida(com.golsCasa)).isGreaterThan(sem.porPartida(sem.golsCasa) * 1.1);
		assertThat(com.porPartida(com.golsFora)).isGreaterThan(sem.porPartida(sem.golsFora) * 1.1);
	}

	@Test
	void doisAtacantesContraDoisNaoMudam() {
		EstatisticasLote com = medir(ParametrosMotor.padrao(), Formacao.F_4_4_2);
		EstatisticasLote sem = medir(ParametrosMotor.padrao().comAbertura(SEM_ABERTURA), Formacao.F_4_4_2);

		assertThat(com.golsCasa).isEqualTo(sem.golsCasa);
		assertThat(com.golsFora).isEqualTo(sem.golsFora);
	}

	@Test
	void recusaQuantidadeErradaOuValorNaoPositivo() {
		ParametrosMotor p = ParametrosMotor.padrao();
		assertThatIllegalArgumentException().isThrownBy(() -> p.comAbertura(List.of(1.0, 1.0, 1.0)));
		assertThatIllegalArgumentException().isThrownBy(() -> p.comAbertura(List.of(1.0, 1.0, 1.0, 0.0, 1.0)));
	}

	private static EstatisticasLote medir(ParametrosMotor parametros, Formacao formacao) {
		return EstatisticasLote.medir(new MotorPartida(parametros), escalacao(formacao, 25, 1),
				escalacao(Formacao.F_4_4_2, 25, 100), 0, PARTIDAS);
	}
}
