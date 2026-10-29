package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.economia.CaixaDaRodada.Local;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.Rodada;

class CaixaDaRodadaTest {

	private final CaixaDaRodada caixa = CaixaDaRodada.padrao();

	@Test
	void estadioVaiDoMinimoAoMaximoEmMultiplosDe500() {
		assertThat(caixa.capacidadeInicial(5)).isEqualTo(5_000);
		assertThat(caixa.capacidadeInicial(12)).isEqualTo(5_000);
		assertThat(caixa.capacidadeInicial(50)).isEqualTo(80_000);
		assertThat(IntStream.rangeClosed(12, 50).map(caixa::capacidadeInicial))
			.allMatch(c -> c % 500 == 0)
			.isSorted();
	}

	@Test
	void salarioCresceComAForcaEComAFaixa() {
		// referência R$ 2.000 na força 17, × 1,072 por ponto; faixa de 15 a 24: −15% (rev. 66)
		assertThat(caixa.salario(jogador(17))).isEqualTo(1_700_00L);
		assertThat(caixa.salario(jogador(18))).isEqualTo(1_822_40L);
		assertThat(caixa.folha(List.of(jogador(17), jogador(17)))).isEqualTo(3_400_00L);
		assertThat(caixa.saldoInicial(List.of(jogador(17)))).isEqualTo(17_000_00L);
		// faixas: abaixo de 15 −30%, 25 a 34 sem ajuste, 35 a 44 +10%, 45 a 50 +20%
		assertThat(caixa.salario(14)).isEqualTo(Math.round(2_000_00L * Math.pow(1.072, -3) * 0.70));
		assertThat(caixa.salario(25)).isEqualTo(Math.round(2_000_00L * Math.pow(1.072, 8)));
		assertThat(caixa.salario(44)).isEqualTo(Math.round(2_000_00L * Math.pow(1.072, 27) * 1.10));
		assertThat(caixa.salario(45)).isEqualTo(Math.round(2_000_00L * Math.pow(1.072, 28) * 1.20));
		assertThat(IntStream.rangeClosed(Jogador.FORCA_MINIMA, Jogador.FORCA_MAXIMA).mapToLong(caixa::salario)).isSorted();
	}

	@Test
	void publicoSobeComAMoralEComAdversarioForteENuncaPassaDaCapacidade() {
		int baixa = media(new Moral(0), 30, 30);
		int inicial = media(new Moral(22), 30, 30);
		int alta = media(new Moral(30), 30, 30);
		int adversarioForte = media(new Moral(22), 30, 50);

		assertThat(baixa).isLessThan(inicial);
		assertThat(alta).isGreaterThan(inicial);
		assertThat(adversarioForte).isGreaterThan(inicial);
		assertThat(inicial).isBetween(4_000, 5_000);
		SplittableRandom aleatorio = new SplittableRandom(1);
		assertThat(IntStream.range(0, 1000).map(i -> caixa.publico(10_000, new Moral(30), 10, 50, aleatorio)))
			.allMatch(p -> p <= 10_000 && p >= 1_000);
	}

	@Test
	void lancamentosEmCasaForaENaFolga() {
		List<Jogador> plantel = List.of(jogador(17));

		assertThat(caixa.lancamentos(Local.CASA, 1, 1_000, plantel)).containsExactly(
				new Lancamento(TipoLancamento.BILHETERIA, 40_000_00L), new Lancamento(TipoLancamento.CUSTO_DO_JOGO, -6_000_00L),
				new Lancamento(TipoLancamento.SALARIOS, -1_700_00L));
		assertThat(caixa.lancamentos(Local.FORA, 2, 0, plantel)).containsExactly(
				new Lancamento(TipoLancamento.DESLOCAMENTO, -30_000_00L), new Lancamento(TipoLancamento.SALARIOS, -1_700_00L));
		assertThat(caixa.lancamentos(Local.FOLGA, 3, 0, plantel))
			.containsExactly(new Lancamento(TipoLancamento.SALARIOS, -1_700_00L));
	}

	@Test
	void rodadaLancaMandanteVisitanteEFolgaComOPublicoRecebido() {
		Clube a = new Clube(1, "A", 30);
		Clube b = new Clube(2, "B", 30);
		Clube c = new Clube(3, "C", 30);
		Rodada rodada = new Rodada(1, List.of(new Jogo(1, a, b)), Optional.of(c));
		List<Jogador> plantel = List.of(jogador(17));

		CaixaDaRodada.Movimento movimento = caixa.rodada(rodada, 1, Map.of(1L, 7_500),
				Map.of(1L, caixa.folha(plantel), 2L, caixa.folha(plantel), 3L, caixa.folha(plantel)));

		int publico = movimento.publicoPorMandante().get(1L);
		assertThat(publico).isEqualTo(7_500);
		assertThat(movimento.lancamentos().get(1L)).isEqualTo(caixa.lancamentos(Local.CASA, 1, publico, plantel));
		assertThat(movimento.lancamentos().get(2L)).isEqualTo(caixa.lancamentos(Local.FORA, 1, 0, plantel));
		assertThat(movimento.lancamentos().get(3L)).isEqualTo(caixa.lancamentos(Local.FOLGA, 1, 0, plantel));
	}

	private int media(Moral moral, int forcaCasa, int forcaFora) {
		SplittableRandom aleatorio = new SplittableRandom(7);
		return (int) IntStream.range(0, 2_000).map(i -> caixa.publico(10_000, moral, forcaCasa, forcaFora, aleatorio))
			.average().orElseThrow();
	}

	@Test
	void todosRecebemComAFaixaDaForcaEmCampoDoClube() {
		List<Jogador> plantel = new java.util.ArrayList<>(java.util.Collections.nCopies(11, jogador(40)));
		plantel.add(jogador(17));
		// força em campo 40: faixa +10% para todos, também para o reserva de força 17 (R$ 2.000 → R$ 2.200)
		assertThat(caixa.folha(plantel)).isEqualTo(11 * Math.round(caixa.salarioSemFaixa(40) * 1.10) + 2_200_00L);
		assertThat(caixa.salario(17, 40)).isEqualTo(2_200_00L);
		// com a faixa pelo jogador (rev. 61), o de força 17 recebe R$ 1.700
		CaixaDaRodada peloJogador = new CaixaDaRodada(
				ParametrosEconomia.padrao().comFaixas(ParametrosEconomia.padrao().faixasSalario(), false), ParametrosMoral.padrao());
		assertThat(peloJogador.folha(plantel)).isEqualTo(11 * peloJogador.salario(40) + 1_700_00L);
	}

	private static Jogador jogador(int forca) {
		return new Jogador(1, "J", Posicao.ATACANTE, forca, Temperamento.NORMAL, "BRA");
	}
}
