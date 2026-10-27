package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;

class ClassificacaoTest {

	private final Clube a = new Clube(1, "A", 30);
	private final Clube b = new Clube(2, "B", 30);
	private final Clube c = new Clube(3, "C", 30);
	private final Clube d = new Clube(4, "D", 30);
	private final List<Clube> clubes = List.of(a, b, c, d);
	private final List<ResultadoJogo> resultados = new ArrayList<>();
	private int rodada = 1;

	private void jogo(Clube mandante, Clube visitante, int gm, int gv) {
		resultados.add(new ResultadoJogo(new Jogo(rodada++, mandante, visitante), gm, gv));
	}

	private Classificacao tabela() {
		return Classificacao.calcular(clubes, resultados, 42);
	}

	@Test
	void vitoriaValeTresEmpateUm() {
		jogo(a, c, 2, 0);
		jogo(b, d, 1, 1);

		Classificacao t = tabela();
		assertThat(t.linha(a).pontos()).isEqualTo(3);
		assertThat(t.linha(b).pontos()).isEqualTo(1);
		assertThat(t.linha(c).pontos()).isZero();
		assertThat(t.linha(a)).extracting(LinhaClassificacao::jogos, LinhaClassificacao::vitorias,
				LinhaClassificacao::golsPro, LinhaClassificacao::golsContra, LinhaClassificacao::saldo)
			.containsExactly(1, 1, 2, 0, 2);
		assertThat(t.linha(c).derrotas()).isEqualTo(1);
	}

	@Test
	void maisPontosFicaNaFrente() {
		jogo(b, c, 1, 0);
		jogo(a, d, 0, 0);
		assertThat(tabela().posicao(b)).isLessThan(tabela().posicao(a));
	}

	@Test
	void comMesmosPontosMaisVitoriasFicaNaFrente() {
		jogo(a, c, 1, 0); // A: 1V = 3 pts
		jogo(b, c, 0, 0); // B: 3E = 3 pts
		jogo(b, d, 0, 0);
		jogo(d, b, 0, 0);
		assertThat(tabela().linha(b).pontos()).isEqualTo(3);
		assertThat(tabela().posicao(a)).isLessThan(tabela().posicao(b));
	}

	@Test
	void comMesmosPontosEVitoriasMenosDerrotasFicaNaFrente() {
		// acontece quando o número de jogos difere (ex.: folga)
		jogo(a, c, 1, 0); // A: 1V
		jogo(b, d, 1, 0); // B: 1V 1D
		jogo(c, b, 1, 0);
		assertThat(tabela().linha(a).pontos()).isEqualTo(tabela().linha(b).pontos());
		assertThat(tabela().posicao(a)).isLessThan(tabela().posicao(b));
	}

	@Test
	void depoisMelhorSaldo() {
		jogo(a, c, 2, 0);
		jogo(b, d, 1, 0);
		assertThat(tabela().posicao(a)).isLessThan(tabela().posicao(b));
	}

	@Test
	void depoisMaisGolsMarcados() {
		jogo(b, d, 2, 0);
		jogo(a, c, 3, 1);
		assertThat(tabela().linha(a).saldo()).isEqualTo(tabela().linha(b).saldo());
		assertThat(tabela().posicao(a)).isLessThan(tabela().posicao(b));
	}

	@Test
	void empateEmTudoUsaSorteioFixoNaTemporada() {
		jogo(a, c, 1, 0);
		jogo(b, d, 1, 0);
		Classificacao primeira = tabela();

		// recalcular, inclusive com novos jogos de outros clubes, não troca A e B de lugar
		jogo(c, d, 2, 2);
		Classificacao depois = tabela();
		assertThat(depois.posicao(a) < depois.posicao(b)).isEqualTo(primeira.posicao(a) < primeira.posicao(b));
		assertThat(Classificacao.calcular(clubes, resultados, 42)).isEqualTo(depois);
	}

	@Test
	void sorteioVariaEntreTemporadas() {
		jogo(a, c, 1, 0);
		jogo(b, d, 1, 0);
		Set<Clube> lideres = new HashSet<>();
		for (long semente = 0; semente < 50; semente++) {
			lideres.add(Classificacao.calcular(clubes, resultados, semente).linhas().getFirst().clube());
		}
		assertThat(lideres).containsExactlyInAnyOrder(a, b);
	}

	@Test
	void semJogosTodosZerados() {
		Classificacao t = tabela();
		assertThat(t.linhas()).hasSize(4).allSatisfy(l -> assertThat(l.pontos()).isZero());
	}

	@Test
	void recusaJogoInformadoDuasVezes() {
		ResultadoJogo resultado = new ResultadoJogo(new Jogo(1, a, b), 1, 0);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Classificacao.calcular(clubes, List.of(resultado, resultado), 42))
			.withMessageContaining("informado mais de uma vez");
	}

	@Test
	void recusaClubeForaDaClassificacao() {
		Clube intruso = new Clube(99, "Intruso", 30);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Classificacao.calcular(clubes, List.of(new ResultadoJogo(new Jogo(1, a, intruso), 1, 0)), 42))
			.withMessageContaining("Intruso tem resultado");
	}

	@Test
	void recusaGolsNegativos() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ResultadoJogo(new Jogo(1, a, b), -1, 0));
	}

	@Test
	void temporadaCompletaComCalendarioSomaPontosCoerentes() {
		// todos os jogos do calendário terminam 1x0 para o mandante: cada clube vence em casa e perde fora
		List<Clube> vinte = new ArrayList<>();
		for (int i = 1; i <= 20; i++) {
			vinte.add(new Clube(i, "Clube " + i, 30));
		}
		List<ResultadoJogo> todos = Calendario.turnoEReturno(vinte).jogos().stream()
			.map(j -> new ResultadoJogo(j, 1, 0))
			.toList();

		Classificacao t = Classificacao.calcular(vinte, todos, 7);
		assertThat(t.linhas()).allSatisfy(l -> {
			assertThat(l.jogos()).isEqualTo(38);
			assertThat(l.pontos()).isEqualTo(19 * 3);
			assertThat(l.saldo()).isZero();
		});
	}
}
