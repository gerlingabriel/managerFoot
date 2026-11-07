package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;

/** Trava das goleadas com expulso (SPEC §7.4, rev. 114 e 131). Sementes fixas: o resultado se repete. */
class TravaGoleadaExpulsaoTest {

	private static final int PARTIDAS = 20_000;
	private static final ParametrosMotor COM_TRAVA = ParametrosMotor.padrao();
	private static final ParametrosMotor SEM_TRAVA = COM_TRAVA.comDisciplina(COM_TRAVA.disciplina().comTravaGoleada(1));
	private static final Escalacao CASA = escalacao(Formacao.F_4_4_2, 25, 1);
	private static final Escalacao FORA = escalacao(Formacao.F_4_4_2, 25, 100);

	@Test
	void padraoTravaAsGoleadasComExpulso() {
		assertThat(COM_TRAVA.disciplina().travaGoleadaComExpulsao()).isLessThan(1);
	}

	@Test
	void partidaSemExpulsaoSaiIgualComOuSemTrava() {
		MotorPartida com = new MotorPartida(COM_TRAVA);
		MotorPartida sem = new MotorPartida(SEM_TRAVA);
		int conferidas = 0;
		for (long semente = 0; semente < 2_000; semente++) {
			ResultadoPartida r = com.simular(CASA, FORA, semente);
			if (r.eventos().stream().noneMatch(e -> e.tipo().expulsao())) {
				assertThat(r.eventos()).isEqualTo(sem.simular(CASA, FORA, semente).eventos());
				conferidas++;
			}
		}
		assertThat(conferidas).isGreaterThan(1_000);
	}

	@Test
	void comExpulsoAsGoleadasCaemMasOResultadoNao() {
		Contagem com = contar(COM_TRAVA);
		Contagem sem = contar(SEM_TRAVA);

		// um gol travado vira chute perdido, que sorteia defesa/trave/fora: o resto da partida muda um pouco
		assertThat(com.comExpulso).isCloseTo(sem.comExpulso, org.assertj.core.data.Percentage.withPercentage(1));
		// com 1 expulso a trava começa em 3 gols de vantagem; com 2, em 5 (rev. 131)
		assertThat(com.adversarioCincoOuMais).isLessThan(sem.adversarioCincoOuMais / 2);
		// a trava só começa depois dos gols livres: quem perde continua perdendo
		assertThat((double) com.punidoPerde / com.comExpulso)
			.isCloseTo((double) sem.punidoPerde / sem.comExpulso, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void golsLivresCrescemComOsExpulsosAMais() {
		ParametrosDisciplina d = ParametrosDisciplina.padrao();
		assertThat(d.golsLivresPorExpulso()).isEqualTo(2);
		assertThatIllegalArgumentException().isThrownBy(() -> d.comGolsLivresPorExpulso(0));
	}

	@Test
	void vermelhoDiretoComecaEmZeroECresceAteOMinuto90() {
		ParametrosDisciplina d = ParametrosDisciplina.padrao();
		assertThat(d.proporcaoVermelhoDireto(0)).isZero();
		assertThat(d.proporcaoVermelhoDireto(45)).isCloseTo(0.01, org.assertj.core.data.Offset.offset(1e-9));
		assertThat(d.proporcaoVermelhoDireto(90)).isCloseTo(0.02, org.assertj.core.data.Offset.offset(1e-9));
		assertThat(d.proporcaoVermelhoDireto(120)).as("na prorrogação vale a do 90").isEqualTo(d.proporcaoVermelhoDireto(90));
		assertThat(d.comVermelhoDireto(0.05, 0).proporcaoVermelhoDireto(1)).as("expoente 0: igual no jogo todo").isEqualTo(0.05);
	}

	@Test
	void recusaTravaForaDoIntervalo() {
		ParametrosDisciplina d = ParametrosDisciplina.padrao();
		assertThatIllegalArgumentException().isThrownBy(() -> d.comTravaGoleada(0));
		assertThatIllegalArgumentException().isThrownBy(() -> d.comTravaGoleada(1.1));
	}

	private static Contagem contar(ParametrosMotor parametros) {
		MotorPartida motor = new MotorPartida(parametros);
		Contagem c = new Contagem();
		for (long semente = 0; semente < PARTIDAS; semente++) {
			ResultadoPartida r = motor.simular(CASA, FORA, semente);
			int diferenca = 0;
			for (EventoPartida e : r.eventos()) {
				if (e.tipo().expulsao()) {
					diferenca += e.lado() == Lado.CASA ? 1 : -1;
				}
			}
			if (diferenca == 0) {
				continue;
			}
			int punido = diferenca > 0 ? r.golsCasa() : r.golsFora();
			int adversario = diferenca > 0 ? r.golsFora() : r.golsCasa();
			c.comExpulso++;
			c.punidoPerde += punido < adversario ? 1 : 0;
			c.adversarioCincoOuMais += adversario >= 5 ? 1 : 0;
		}
		return c;
	}

	private static final class Contagem {
		int comExpulso;
		int punidoPerde;
		int adversarioCincoOuMais;
	}
}
