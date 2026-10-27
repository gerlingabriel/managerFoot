package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

class TemporadaTest {

	private static final ConfiguracaoLiga LIGA = new ConfiguracaoLiga("Nacional",
			List.of(new Divisao("A", 1, 50, 0, 2, clubes(1, 16)), new Divisao("B", 2, 30, 2, 2, clubes(101, 17))),
			clubes(201, 2));
	private static final Map<Long, List<Jogador>> PLANTEIS = planteis();

	@Test
	void mesmaSementeMesmosSorteios() {
		assertThat(Temporada.iniciar(1, LIGA, PLANTEIS, 42)).isEqualTo(Temporada.iniciar(1, LIGA, PLANTEIS, 42));
	}

	@Test
	void sementesDiferentesMudamOsSorteios() {
		Temporada a = Temporada.iniciar(1, LIGA, PLANTEIS, 1);
		Temporada b = Temporada.iniciar(1, LIGA, PLANTEIS, 2);

		assertThat(a.divisoes().getFirst().calendario()).isNotEqualTo(b.divisoes().getFirst().calendario());
	}

	@Test
	void cadaDivisaoSorteiaSoEntreOsSeusClubesEJogadores() {
		Temporada temporada = Temporada.iniciar(1, LIGA, PLANTEIS, 7);

		assertThat(temporada.divisoes()).extracting(Temporada.SorteiosDivisao::prioridade).containsExactly(1, 2);
		for (int i = 0; i < LIGA.divisoes().size(); i++) {
			Divisao divisao = LIGA.divisoes().get(i);
			Temporada.SorteiosDivisao sorteios = temporada.divisoes().get(i);
			int n = divisao.clubes().size();
			int rodadas = n % 2 == 0 ? 2 * (n - 1) : 2 * n;

			assertThat(sorteios.calendario().rodadas()).hasSize(rodadas);
			assertThat(sorteios.calendario().jogos()).hasSize(n * (n - 1))
				.allMatch(j -> divisao.clubes().contains(j.mandante()) && divisao.clubes().contains(j.visitante()));
			assertThat(divisao.clubes()).contains(sorteios.sorte().beneficiado(), sorteios.sorte().prejudicado());

			Set<Long> daDivisao = new HashSet<>();
			divisao.clubes().forEach(c -> PLANTEIS.get(c.id()).forEach(j -> daDivisao.add(j.id())));
			Set<Long> sorteados = new HashSet<>(sorteios.bonus().asteriscoTemporario());
			sorteados.addAll(sorteios.bonus().bonusExtra());
			assertThat(sorteados).hasSize(BonusArtilharia.QUANTIDADE_POR_DIVISAO).isSubsetOf(daDivisao);
		}
	}

	@Test
	void clubeSemPlantelNaoImpedeOSorteio() {
		Temporada temporada = Temporada.iniciar(1, LIGA, Map.of(), 3);

		assertThat(temporada.divisoes()).allMatch(d -> d.bonus().asteriscoTemporario().isEmpty()
				&& d.bonus().bonusExtra().isEmpty());
	}

	@Test
	void numeroComecaEmUm() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Temporada(0, 1, List.of()));
	}

	private static List<Clube> clubes(int primeiroId, int quantidade) {
		return IntStream.range(primeiroId, primeiroId + quantidade)
			.mapToObj(id -> new Clube(id, "Clube " + id, 25))
			.toList();
	}

	/** 3 jogadores por clube (goleiro, meia com asterisco, atacante), com ids únicos. */
	private static Map<Long, List<Jogador>> planteis() {
		Map<Long, List<Jogador>> planteis = new HashMap<>();
		List<Clube> todos = new java.util.ArrayList<>(LIGA.filaDeEspera());
		LIGA.divisoes().forEach(d -> todos.addAll(d.clubes()));
		for (Clube c : todos) {
			long base = c.id() * 10;
			planteis.put(c.id(), List.of(new Jogador(base, "G", Posicao.GOLEIRO, 20, Temperamento.NORMAL, "BRA"),
					new Jogador(base + 1, "M", Posicao.MEIO_CAMPISTA, 20, Temperamento.NORMAL, true, "BRA"),
					new Jogador(base + 2, "A", Posicao.ATACANTE, 20, Temperamento.NORMAL, "POR")));
		}
		return planteis;
	}
}
