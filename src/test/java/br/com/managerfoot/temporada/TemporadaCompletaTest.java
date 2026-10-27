package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;

/** Fluxo de ponta a ponta: duas temporadas seguidas com motor, classificação, moral e transição. */
class TemporadaCompletaTest {

	private static List<Clube> clubes(long primeiro, int quantidade, int forca) {
		List<Clube> lista = new ArrayList<>();
		for (int i = 0; i < quantidade; i++) {
			lista.add(new Clube(primeiro + i, "C" + (primeiro + i), forca - i));
		}
		return lista;
	}

	@Test
	void duasTemporadasSeguidasMantemALigaValida() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga",
				List.of(new Divisao("A", 1, 50, 0, 3, clubes(1, 16, 45)), new Divisao("B", 2, 30, 3, 2, clubes(101, 16, 30))),
				clubes(201, 3, 20));

		for (long temporada = 1; temporada <= 2; temporada++) {
			TemporadaSimulada a = TemporadaSimulada.jogar(liga.divisoes().get(0), temporada);
			TemporadaSimulada b = TemporadaSimulada.jogar(liga.divisoes().get(1), temporada + 100);

			assertThat(a.classificacao.linhas()).allSatisfy(l -> assertThat(l.jogos()).isEqualTo(30));
			assertThat(a.moral.values()).allSatisfy(m -> assertThat(m.valor()).isBetween(0, 30));

			TransicaoTemporada t = TransicaoTemporada.aplicar(liga,
					Map.of(1, a.classificacao, 2, b.classificacao), new SplittableRandom(temporada));
			assertThat(t.promovidos()).hasSize(5);
			assertThat(t.semLiga()).hasSize(2);
			liga = t.proxima();
		}
		assertThat(ValidadorLiga.problemas(liga)).isEmpty();
	}

	@Test
	void temporadaEReproduzivelPelaSemente() {
		Divisao divisao = new Divisao("A", 1, 50, 0, 0, clubes(1, 16, 40));
		assertThat(TemporadaSimulada.jogar(divisao, 5).classificacao)
			.isEqualTo(TemporadaSimulada.jogar(divisao, 5).classificacao);
	}

	@Test
	void artilhariaDaTemporadaSomaTodosOsGolsDosPlacares() {
		Divisao divisao = new Divisao("A", 1, 50, 0, 0, clubes(1, 20, 40));
		TemporadaSimulada t = TemporadaSimulada.jogar(divisao, 11);

		int golsDosArtilheiros = t.artilharia.golsTemporada().values().stream().mapToInt(Integer::intValue).sum();
		assertThat(golsDosArtilheiros).isEqualTo(t.gols);
		assertThat(t.artilharia.golsCarreira()).isEqualTo(t.artilharia.golsTemporada());
		assertThat(t.artilharia.artilheirosDaTemporada()).isNotEmpty();
	}
}
