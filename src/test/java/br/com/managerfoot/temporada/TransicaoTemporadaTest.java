package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;

class TransicaoTemporadaTest {

	private long proximoId = 1;

	private List<Clube> clubes(int quantidade) {
		List<Clube> lista = new ArrayList<>();
		for (int i = 0; i < quantidade; i++) {
			long id = proximoId++;
			lista.add(new Clube(id, "C" + id, 30));
		}
		return lista;
	}

	/** Temporada completa em que o clube de menor id sempre vence: a ordem final é a ordem dos ids. */
	private static Classificacao temporadaPorId(Divisao divisao) {
		List<ResultadoJogo> resultados = Calendario.turnoEReturno(divisao.clubes()).jogos().stream()
			.map(j -> j.mandante().id() < j.visitante().id() ? new ResultadoJogo(j, 1, 0) : new ResultadoJogo(j, 0, 1))
			.toList();
		return Classificacao.calcular(divisao.clubes(), resultados, 1);
	}

	private static Map<Integer, Classificacao> finais(ConfiguracaoLiga liga) {
		Map<Integer, Classificacao> finais = new HashMap<>();
		liga.divisoes().forEach(d -> finais.put(d.prioridade(), temporadaPorId(d)));
		return finais;
	}

	@Test
	void duasDivisoesTrocamQuatroClubesEAFilaRepoeAUltima() {
		List<Clube> a = clubes(20);
		List<Clube> b = clubes(18);
		List<Clube> fila = clubes(4);
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga",
				List.of(new Divisao("A", 1, 50, 0, 4, a), new Divisao("B", 2, 30, 4, 4, b)), fila);

		TransicaoTemporada t = TransicaoTemporada.aplicar(liga, finais(liga), new SplittableRandom(1));
		Divisao novaA = t.proxima().divisoes().get(0);
		Divisao novaB = t.proxima().divisoes().get(1);

		// 4 últimos da A (ids 17-20) descem; 4 primeiros da B (ids 21-24) sobem
		assertThat(novaA.clubes()).hasSize(20).doesNotContainAnyElementsOf(a.subList(16, 20)).containsAll(b.subList(0, 4));
		assertThat(novaB.clubes()).hasSize(18).containsAll(a.subList(16, 20)).doesNotContainAnyElementsOf(b.subList(0, 4));
		// 4 últimos da B ficam sem liga; os 4 da fila entram na B
		assertThat(t.semLiga()).containsExactlyInAnyOrderElementsOf(b.subList(14, 18));
		assertThat(novaB.clubes()).containsAll(fila).doesNotContainAnyElementsOf(t.semLiga());
		assertThat(t.proxima().filaDeEspera()).containsExactlyInAnyOrderElementsOf(b.subList(14, 18));
		assertThat(t.promovidos()).hasSize(8);
		assertThat(t.rebaixados()).hasSize(8);
	}

	@Test
	void comMaisClubesNaFilaOsQueSobemSaoSorteadosEntreQuemJaEsperava() {
		List<Clube> unica = clubes(16);
		List<Clube> fila = clubes(6);
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(new Divisao("Única", 1, 50, 0, 2, unica)), fila);

		Set<Clube> sorteadosAlgumaVez = new HashSet<>();
		for (long semente = 0; semente < 40; semente++) {
			TransicaoTemporada t = TransicaoTemporada.aplicar(liga, finais(liga), new SplittableRandom(semente));
			List<Clube> novos = t.proxima().divisoes().getFirst().clubes().stream().filter(fila::contains).toList();
			assertThat(novos).hasSize(2);
			assertThat(t.proxima().divisoes().getFirst().clubes()).doesNotContainAnyElementsOf(t.semLiga());
			assertThat(t.proxima().filaDeEspera()).hasSize(6).containsAll(t.semLiga());
			sorteadosAlgumaVez.addAll(novos);
		}
		assertThat(sorteadosAlgumaVez).containsExactlyInAnyOrderElementsOf(fila);
	}

	@Test
	void sorteioDaFilaEReproduzivel() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(new Divisao("Única", 1, 50, 0, 2, clubes(16))),
				clubes(6));
		assertThat(TransicaoTemporada.aplicar(liga, finais(liga), new SplittableRandom(9)))
			.isEqualTo(TransicaoTemporada.aplicar(liga, finais(liga), new SplittableRandom(9)));
	}

	@Test
	void rebaixadoParaAFilaFicaAoMenosUmaTemporadaFora() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(new Divisao("Única", 1, 50, 0, 2, clubes(16))),
				clubes(2));
		TransicaoTemporada primeira = TransicaoTemporada.aplicar(liga, finais(liga), new SplittableRandom(1));
		List<Clube> foraNaPrimeira = primeira.semLiga();

		TransicaoTemporada segunda = TransicaoTemporada.aplicar(primeira.proxima(), finais(primeira.proxima()),
				new SplittableRandom(1));

		// na segunda transição, quem caiu na primeira volta (fila só tinha eles para sortear)
		assertThat(segunda.proxima().divisoes().getFirst().clubes()).containsAll(foraNaPrimeira);
	}

	@Test
	void semRebaixamentoNadaMuda() {
		List<Clube> unica = clubes(16);
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(new Divisao("Única", 1, 50, 0, 0, unica)), List.of());

		TransicaoTemporada t = TransicaoTemporada.aplicar(liga, finais(liga), new SplittableRandom(1));

		assertThat(t.proxima().divisoes().getFirst().clubes()).containsExactlyInAnyOrderElementsOf(unica);
		assertThat(t.semLiga()).isEmpty();
	}

	@Test
	void naoAvancaComTemporadaIncompleta() {
		Divisao divisao = new Divisao("Única", 1, 50, 0, 0, clubes(16));
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(divisao), List.of());
		List<ResultadoJogo> metade = Calendario.turnoEReturno(divisao.clubes()).rodadas().subList(0, 15).stream()
			.flatMap(r -> r.jogos().stream())
			.map(j -> new ResultadoJogo(j, 1, 0))
			.toList();
		Map<Integer, Classificacao> finais = Map.of(1, Classificacao.calcular(divisao.clubes(), metade, 1));

		assertThatIllegalArgumentException()
			.isThrownBy(() -> TransicaoTemporada.aplicar(liga, finais, new SplittableRandom(1)))
			.withMessageContaining("ainda não terminou")
			.withMessageContaining("30 jogos");
	}

	@Test
	void exigeClassificacaoDeTodasAsDivisoes() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga",
				List.of(new Divisao("A", 1, 50, 0, 2, clubes(16)), new Divisao("B", 2, 30, 2, 0, clubes(16))), List.of());
		Map<Integer, Classificacao> soA = Map.of(1, temporadaPorId(liga.divisoes().getFirst()));

		assertThatIllegalArgumentException()
			.isThrownBy(() -> TransicaoTemporada.aplicar(liga, soA, new SplittableRandom(1)))
			.withMessageContaining("falta a classificação final de \"B\"");
	}
}
