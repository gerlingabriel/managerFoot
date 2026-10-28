package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.economia.Leilao.Lance;
import br.com.managerfoot.economia.LeiloesDaRodada.ClubeNoMercado;
import br.com.managerfoot.economia.LeiloesDaRodada.Desfecho;
import br.com.managerfoot.economia.LeiloesDaRodada.Oferta;

class LeiloesDaRodadaTest {

	private static final long RICO = 10_000_000_00L;

	private final LeiloesDaRodada leiloes = LeiloesDaRodada.padrao();
	private final PrecoJogador preco = PrecoJogador.padrao();

	@Test
	void abreUmLeilaoPorClubeAbaixoDoLimiteESemMinimoComSaldoNegativo() {
		// 24 jogadores, folha de R$ 48.000 (força 17) mais o de força 30: limite acima de R$ 480.000
		ClubeNoMercado pobre = clube(1, 100, 100_000_00L);
		ClubeNoMercado negativo = clube(2, 200, -1_00L);
		ClubeNoMercado rico = clube(3, 300, RICO);

		List<Oferta> ofertas = leiloes.abrir(List.of(rico, negativo, pobre), "BRA");

		assertThat(ofertas).extracting(Oferta::vendedorId).containsExactly(1L, 2L);
		Oferta primeira = ofertas.get(0);
		assertThat(primeira.jogador().id()).isEqualTo(100L); // o mais caro sem asterisco
		assertThat(primeira.lanceMinimo()).isEqualTo(preco.lanceInicialLeilao(primeira.jogador(), "BRA"));
		assertThat(ofertas.get(1).lanceMinimo()).isZero();
	}

	@Test
	void levaOMaiorLanceEOHumanoPodeVencerAMaquina() {
		ClubeNoMercado vendedor = clube(1, 100, 100_000_00L);
		ClubeNoMercado maquina = clube(2, 200, RICO);
		ClubeNoMercado humano = clube(9, 900, RICO);
		Oferta oferta = leiloes.abrir(List.of(vendedor), "BRA").get(0);
		long precoCheio = preco.preco(oferta.jogador(), "BRA");

		Desfecho maquinaLeva = resolver(oferta, List.of(vendedor, maquina), humano, oferta.lanceMinimo()).get(0);
		assertThat(maquinaLeva.vencedor()).map(Lance::clubeId).contains(2L);
		assertThat(maquinaLeva.lances()).hasSize(2);

		Desfecho humanoLeva = resolver(oferta, List.of(vendedor, maquina), humano, precoCheio).get(0);
		assertThat(humanoLeva.vencedor()).contains(new Lance(9, precoCheio));
		assertThat(humanoLeva.empate()).isFalse();
	}

	@Test
	void empateESorteadoEMarcado() {
		ClubeNoMercado vendedor = clube(1, 100, 100_000_00L);
		ClubeNoMercado maquina = clube(2, 200, RICO);
		ClubeNoMercado humano = clube(9, 900, RICO);
		Oferta oferta = leiloes.abrir(List.of(vendedor), "BRA").get(0);
		long lanceDaMaquina = resolver(oferta, List.of(vendedor, maquina), humano, 0).get(0).vencedor().orElseThrow().valor();

		Desfecho empatado = resolver(oferta, List.of(vendedor, maquina), humano, lanceDaMaquina).get(0);

		assertThat(empatado.empate()).isTrue();
		assertThat(empatado.vencedor().orElseThrow().valor()).isEqualTo(lanceDaMaquina);
	}

	@Test
	void lanceDoHumanoAbaixoDoMinimoOuAcimaDoCaixaNaoVale() {
		ClubeNoMercado vendedor = clube(1, 100, 100_000_00L);
		Oferta oferta = leiloes.abrir(List.of(vendedor), "BRA").get(0);
		ClubeNoMercado humanoSemCaixa = clube(9, 900, oferta.lanceMinimo() - 1);

		assertThat(resolver(oferta, List.of(vendedor), clube(9, 900, RICO), oferta.lanceMinimo() - 1_000_00L).get(0).vencedor())
			.isEmpty();
		assertThat(resolver(oferta, List.of(vendedor), humanoSemCaixa, oferta.lanceMinimo()).get(0).vencedor()).isEmpty();
	}

	@Test
	void humanoNaoGastaOQueJaGastouNumLeilaoAnterior() {
		ClubeNoMercado a = clube(1, 100, 100_000_00L);
		ClubeNoMercado b = clube(2, 200, 100_000_00L);
		List<Oferta> ofertas = leiloes.abrir(List.of(a, b), "BRA");
		long minimo = ofertas.get(0).lanceMinimo();
		// caixa para um só dos dois lances
		ClubeNoMercado humano = clube(9, 900, minimo + minimo / 2);
		Map<Long, List<Lance>> lances = Map.of(
				ofertas.get(0).jogador().id(), List.of(new Lance(9, minimo)),
				ofertas.get(1).jogador().id(), List.of(new Lance(9, minimo)));

		List<Desfecho> desfechos = leiloes.resolver(ofertas, List.of(a, b), List.of(humano), lances, "BRA", new SplittableRandom(1));

		assertThat(desfechos.get(0).vencedor()).map(Lance::clubeId).contains(9L);
		assertThat(desfechos.get(1).vencedor()).isEmpty();
	}

	@Test
	void cadaClubeDaMaquinaCompraNoMaximoUmPorRodadaESemLanceOJogadorFica() {
		ClubeNoMercado a = clube(1, 100, 100_000_00L);
		ClubeNoMercado b = clube(2, 200, 100_000_00L);
		ClubeNoMercado comprador = clube(3, 300, RICO);
		List<Oferta> ofertas = leiloes.abrir(List.of(a, b), "BRA");

		List<Desfecho> desfechos = leiloes.resolver(ofertas, List.of(a, b, comprador), List.of(), Map.of(), "BRA",
				new SplittableRandom(1));

		assertThat(desfechos.get(0).vencedor()).map(Lance::clubeId).contains(3L);
		assertThat(desfechos.get(1).vencedor()).isEmpty();
		assertThat(desfechos.get(1).lances()).isEmpty();
	}

	@Test
	void clubeDaMaquinaQueJaComprouAntesDaRodadaNaoDaLance() {
		ClubeNoMercado a = clube(1, 100, 100_000_00L);
		ClubeNoMercado comprador = clube(3, 300, RICO);
		List<Oferta> ofertas = leiloes.abrir(List.of(a), "BRA");

		List<Desfecho> desfechos = leiloes.resolver(ofertas, List.of(a, comprador), List.of(), Map.of(), Set.of(3L), "BRA",
				new SplittableRandom(1));

		assertThat(desfechos.get(0).vencedor()).isEmpty();
	}

	@Test
	void caixaNegativoSorteiaUmSemContratoRenovadoSemLanceMinimo() {
		ClubeNoMercado negativo = clube(9, 900, -1);
		Set<Long> renovados = Set.of(900L, 901L);

		Oferta oferta = leiloes.abrirPorCaixaNegativo(negativo, renovados, new SplittableRandom(3)).orElseThrow();

		assertThat(oferta.lanceMinimo()).isZero();
		assertThat(oferta.vendedorId()).isEqualTo(9L);
		assertThat(renovados).doesNotContain(oferta.jogador().id());
		assertThat(leiloes.abrirPorCaixaNegativo(negativo, renovados, new SplittableRandom(3))).contains(oferta);
		// sorteio: sementes diferentes escolhem jogadores diferentes
		assertThat(new SplittableRandom(0).ints(0, 50).limit(20).mapToObj(s -> leiloes
			.abrirPorCaixaNegativo(negativo, renovados, new SplittableRandom(s)).orElseThrow().jogador().id()).distinct().count())
			.isGreaterThan(1);
	}

	@Test
	void semCaixaNegativoOuComTodosRenovadosNinguemVaiALeilao() {
		ClubeNoMercado zerado = clube(9, 900, 0);
		ClubeNoMercado negativo = clube(9, 900, -1);
		Set<Long> todos = new HashSet<>();
		negativo.plantel().forEach(j -> todos.add(j.id()));

		assertThat(leiloes.abrirPorCaixaNegativo(zerado, Set.of(), new SplittableRandom(1))).isEmpty();
		assertThat(leiloes.abrirPorCaixaNegativo(negativo, todos, new SplittableRandom(1))).isEmpty();
	}

	private List<Desfecho> resolver(Oferta oferta, List<ClubeNoMercado> daMaquina, ClubeNoMercado humano, long lance) {
		Map<Long, List<Lance>> lances = lance == 0 ? Map.of() : Map.of(oferta.jogador().id(), List.of(new Lance(humano.clube().id(), lance)));
		return leiloes.resolver(List.of(oferta), daMaquina, List.of(humano), lances, "BRA", new SplittableRandom(7));
	}

	/** 23 jogadores de força 17 e um meia de força 30 (id {@code primeiroId}), o mais caro. */
	private static ClubeNoMercado clube(long id, long primeiroId, long saldo) {
		Posicao[] ordem = { Posicao.GOLEIRO, Posicao.DEFENSOR, Posicao.DEFENSOR, Posicao.MEIO_CAMPISTA,
				Posicao.MEIO_CAMPISTA, Posicao.ATACANTE };
		List<Jogador> plantel = new ArrayList<>();
		plantel.add(new Jogador(primeiroId, "J" + primeiroId, Posicao.MEIO_CAMPISTA, 30, Temperamento.NORMAL, "BRA"));
		for (int i = 1; i <= 23; i++) {
			plantel.add(new Jogador(primeiroId + i, "J" + (primeiroId + i), ordem[i % ordem.length], 17, Temperamento.NORMAL, "BRA"));
		}
		return new ClubeNoMercado(new Clube(id, "Clube " + id, 17), saldo, plantel);
	}
}
