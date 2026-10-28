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
	void salarioCresceComAForca() {
		assertThat(caixa.salario(jogador(17))).isEqualTo(2_000_00L);
		assertThat(caixa.salario(jogador(18))).isEqualTo(2_144_00L);
		assertThat(caixa.folha(List.of(jogador(17), jogador(17)))).isEqualTo(4_000_00L);
		assertThat(caixa.saldoInicial(List.of(jogador(17)))).isEqualTo(20_000_00L);
	}

	@Test
	void publicoSobeComAMoralEComAdversarioForteENuncaPassaDaCapacidade() {
		int baixa = media(new Moral(0), 30, 30);
		int inicial = media(new Moral(18), 30, 30);
		int alta = media(new Moral(30), 30, 30);
		int adversarioForte = media(new Moral(18), 30, 50);

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
				new Lancamento(TipoLancamento.SALARIOS, -2_000_00L));
		assertThat(caixa.lancamentos(Local.FORA, 2, 0, plantel)).containsExactly(
				new Lancamento(TipoLancamento.DESLOCAMENTO, -30_000_00L), new Lancamento(TipoLancamento.SALARIOS, -2_000_00L));
		assertThat(caixa.lancamentos(Local.FOLGA, 3, 0, plantel))
			.containsExactly(new Lancamento(TipoLancamento.SALARIOS, -2_000_00L));
	}

	@Test
	void rodadaLancaMandanteVisitanteEFolgaComPublicoReproduzivel() {
		Clube a = new Clube(1, "A", 30);
		Clube b = new Clube(2, "B", 30);
		Clube c = new Clube(3, "C", 30);
		Rodada rodada = new Rodada(1, List.of(new Jogo(1, a, b)), Optional.of(c));
		List<Jogador> plantel = List.of(jogador(17));
		Map<Long, List<Jogador>> planteis = Map.of(1L, plantel, 2L, plantel, 3L, plantel);
		Map<Long, Integer> capacidades = Map.of(1L, 10_000, 2L, 10_000, 3L, 10_000);

		CaixaDaRodada.Movimento movimento = caixa.rodada(rodada, 1, Map.of(1L, 99L), capacidades, Map.of(), planteis);

		int publico = movimento.publicoPorMandante().get(1L);
		assertThat(publico).isEqualTo(caixa.publico(10_000, new Moral(18), 30, 30, new SplittableRandom(~99L)));
		assertThat(movimento.lancamentos().get(1L)).isEqualTo(caixa.lancamentos(Local.CASA, 1, publico, plantel));
		assertThat(movimento.lancamentos().get(2L)).isEqualTo(caixa.lancamentos(Local.FORA, 1, 0, plantel));
		assertThat(movimento.lancamentos().get(3L)).isEqualTo(caixa.lancamentos(Local.FOLGA, 1, 0, plantel));
		assertThat(caixa.rodada(rodada, 1, Map.of(1L, 99L), capacidades, Map.of(), planteis)).isEqualTo(movimento);
	}

	private int media(Moral moral, int forcaCasa, int forcaFora) {
		SplittableRandom aleatorio = new SplittableRandom(7);
		return (int) IntStream.range(0, 2_000).map(i -> caixa.publico(10_000, moral, forcaCasa, forcaFora, aleatorio))
			.average().orElseThrow();
	}

	private static Jogador jogador(int forca) {
		return new Jogador(1, "J", Posicao.ATACANTE, forca, Temperamento.NORMAL, "BRA");
	}
}
