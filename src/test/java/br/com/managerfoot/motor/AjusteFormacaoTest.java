package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Formacao;

/** Ajustes de equilíbrio das formações (SPEC §7.2, rev. 113). Sementes fixas: o resultado se repete. */
class AjusteFormacaoTest {

	private static final int PARTIDAS = 3_000;
	private static final ParametrosMotor SEM_AJUSTES = ParametrosMotor.padrao().comAjustes(ParametrosMotor.ajustesPadrao()
		.keySet().stream().collect(java.util.stream.Collectors.toMap(f -> f, f -> AjusteFormacao.NENHUM)));

	@Test
	void tresZagueirosGanhamMeioEO343Menos() {
		ParametrosMotor p = ParametrosMotor.padrao();
		assertThat(p.ajuste(Formacao.F_3_5_2).meio()).isGreaterThan(1);
		assertThat(p.ajuste(Formacao.F_3_6_1).meio()).isGreaterThan(1);
		assertThat(p.ajuste(Formacao.F_3_4_3).meio()).isGreaterThan(1)
			.isLessThan(p.ajuste(Formacao.F_3_5_2).meio())
			.isLessThan(p.ajuste(Formacao.F_3_6_1).meio());
	}

	@Test
	void quatroAtacantesGanhamAtaqueEUmAtacanteComDefesaForteGanhaDefesa() {
		ParametrosMotor p = ParametrosMotor.padrao();
		assertThat(p.ajuste(Formacao.F_4_2_4).ataque()).isGreaterThan(1);
		assertThat(p.ajuste(Formacao.F_4_5_1).defesa()).isGreaterThan(1);
		assertThat(p.ajuste(Formacao.F_5_4_1).defesa()).isGreaterThanOrEqualTo(1);
		assertThat(p.ajuste(Formacao.F_3_6_1).defesa()).isEqualTo(1);
		assertThat(p.ajuste(Formacao.F_4_4_2)).isEqualTo(AjusteFormacao.NENHUM);
		assertThat(p.ajuste(Formacao.F_4_3_3)).isEqualTo(AjusteFormacao.NENHUM);
	}

	@Test
	void bonusDeMeioDaMaisJogadasEMaisVitorias() {
		EstatisticasLote com = medir(ParametrosMotor.padrao(), Formacao.F_3_6_1);
		EstatisticasLote sem = medir(SEM_AJUSTES, Formacao.F_3_6_1);

		assertThat(com.golsCasa).isGreaterThan(sem.golsCasa);
		assertThat(com.golsFora).isLessThan(sem.golsFora);
		assertThat(com.vitoriasCasa).isGreaterThan(sem.vitoriasCasa);
	}

	@Test
	void bonusDeAtaqueFazOTimeMarcarMais() {
		EstatisticasLote com = medir(ParametrosMotor.padrao(), Formacao.F_4_2_4);
		EstatisticasLote sem = medir(SEM_AJUSTES, Formacao.F_4_2_4);

		assertThat(com.golsCasa).isGreaterThan(sem.golsCasa);
		assertThat(com.vitoriasCasa).isGreaterThan(sem.vitoriasCasa);
	}

	@Test
	void bonusDeDefesaFazOAdversarioMarcarMenos() {
		EstatisticasLote com = medir(ParametrosMotor.padrao(), Formacao.F_4_5_1);
		EstatisticasLote sem = medir(SEM_AJUSTES, Formacao.F_4_5_1);

		assertThat(com.golsFora).isLessThan(sem.golsFora);
		assertThat(com.vitoriasFora).isLessThan(sem.vitoriasFora);
	}

	@Test
	void formacaoSemAjusteContraOutraSemAjusteNaoMuda() {
		EstatisticasLote com = medir(ParametrosMotor.padrao(), Formacao.F_4_3_3);
		EstatisticasLote sem = medir(SEM_AJUSTES, Formacao.F_4_3_3);

		assertThat(com.golsCasa).isEqualTo(sem.golsCasa);
		assertThat(com.golsFora).isEqualTo(sem.golsFora);
	}

	@Test
	void recusaFatorNaoPositivoEFormacaoSemAjuste() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AjusteFormacao(0, 1, 1));
		assertThatIllegalArgumentException().isThrownBy(() -> new AjusteFormacao(1, -1, 1));
		assertThatIllegalArgumentException().isThrownBy(() -> new AjusteFormacao(1, 1, 0));
		assertThatIllegalArgumentException()
			.isThrownBy(() -> ParametrosMotor.padrao().comAjustes(Map.of(Formacao.F_4_4_2, AjusteFormacao.NENHUM)));
	}

	private static EstatisticasLote medir(ParametrosMotor parametros, Formacao formacao) {
		return EstatisticasLote.medir(new MotorPartida(parametros), escalacao(formacao, 25, 1),
				escalacao(Formacao.F_4_4_2, 25, 100), 0, PARTIDAS);
	}
}
