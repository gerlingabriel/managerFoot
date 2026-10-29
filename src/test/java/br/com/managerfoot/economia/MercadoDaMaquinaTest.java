package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

class MercadoDaMaquinaTest {

	private final MercadoDaMaquina mercado = MercadoDaMaquina.padrao();

	@Test
	void limiteEDezRodadasDaFolhaEAbaixoDeleOClubeVende() {
		// 23 jogadores de força 17: folha de R$ 43.700 por rodada (R$ 1.900 cada, faixa −5%)
		List<Jogador> plantel = plantel(23, 17);
		assertThat(mercado.limite(plantel)).isEqualTo(437_000_00L);
		assertThat(mercado.precisaVender(436_999_00L, plantel)).isTrue();
		assertThat(mercado.precisaVender(437_000_00L, plantel)).isFalse();
	}

	@Test
	void vendeOMaisCaroSemAsteriscoSemFicarAbaixoDoElencoMinimo() {
		List<Jogador> plantel = new ArrayList<>(plantel(23, 17));
		plantel.add(jogador(100, Posicao.ATACANTE, 40).comAsterisco(true));
		plantel.add(jogador(101, Posicao.GOLEIRO, 35));
		// o atacante de força 40 tem asterisco; o goleiro 101 é o mais caro que pode sair
		assertThat(mercado.jogadorParaVender(plantel, "BRA")).map(Jogador::id).contains(101L);

		// único goleiro não sai, mesmo sendo o mais caro
		List<Jogador> semReserva = new ArrayList<>(plantel(23, 17).stream().filter(j -> j.posicao() != Posicao.GOLEIRO).toList());
		semReserva.add(jogador(200, Posicao.GOLEIRO, 45));
		assertThat(mercado.jogadorParaVender(semReserva, "BRA").orElseThrow().id()).isNotEqualTo(200L);
	}

	@Test
	void ninguemSaiQuandoOElencoEstaNoMinimo() {
		List<Jogador> minimo = new ArrayList<>();
		minimo.add(jogador(1, Posicao.GOLEIRO, 20));
		for (int i = 0; i < 4; i++) {
			minimo.add(jogador(10 + i, Posicao.DEFENSOR, 20));
			minimo.add(jogador(20 + i, Posicao.MEIO_CAMPISTA, 20));
		}
		minimo.add(jogador(30, Posicao.ATACANTE, 20));
		minimo.add(jogador(31, Posicao.ATACANTE, 20));
		assertThat(mercado.jogadorParaVender(minimo, "BRA")).isEmpty();
		assertThat(minimo).noneMatch(j -> mercado.podeSair(minimo, j));
		minimo.add(jogador(32, Posicao.ATACANTE, 20));
		// 12 jogadores: abaixo do plantel mínimo de 14, ninguém sai (rev. 59)
		assertThat(mercado.podeSair(minimo, minimo.get(minimo.size() - 1))).isFalse();
		for (int i = 0; i < 3; i++) {
			minimo.add(jogador(40 + i, Posicao.MEIO_CAMPISTA, 20));
		}
		assertThat(mercado.podeSair(minimo, minimo.get(0))).isFalse();
		assertThat(mercado.podeSair(minimo, minimo.get(12))).isTrue();
	}

	@Test
	void lanceFicaAbaixoDoPrecoEOClubeMaisForteDaUmPoucoMais() {
		List<Jogador> plantel = plantel(23, 17);
		Jogador alvo = jogador(300, Posicao.MEIO_CAMPISTA, 17); // preço R$ 152.000
		long rico = 10_000_000_00L;
		long fraco = mercado.lance(new Clube(1, "Fraco", 10), rico, plantel, alvo, "BRA", 121_000_00L).orElseThrow();
		long forte = mercado.lance(new Clube(2, "Forte", 50), rico, plantel, alvo, "BRA", 121_000_00L).orElseThrow();
		assertThat(fraco).isEqualTo(138_000_00L); // 152.000 × (0,90 + 0,05 × 10/50) = 138.320
		assertThat(forte).isEqualTo(144_000_00L); // 152.000 × 0,95 = 144.400
		assertThat(fraco).isLessThan(forte).isGreaterThanOrEqualTo(121_000_00L);
	}

	@Test
	void naoDaLanceSemCaixaAcimaDoLimiteNemComPlantelCheio() {
		Jogador alvo = jogador(300, Posicao.MEIO_CAMPISTA, 17);
		Clube clube = new Clube(1, "Clube", 17);
		List<Jogador> plantel = plantel(23, 17);
		// com o novo jogador a folha é 24 × R$ 1.900 = R$ 45.600: limite de 10 rodadas mais folga de 5,
		// R$ 684.000; lance de R$ 139.000
		assertThat(mercado.lance(clube, 823_000_00L, plantel, alvo, "BRA", 0)).contains(139_000_00L);
		assertThat(mercado.lance(clube, 822_999_00L, plantel, alvo, "BRA", 0)).isEmpty();
		assertThat(mercado.lance(clube, 10_000_000_00L, plantel(Clube.MAXIMO_JOGADORES, 17), alvo, "BRA", 0)).isEmpty();
	}

	@Test
	void naoDaLanceEmJogadorMaisDeCincoPontosAbaixoDaForcaEmCampo() {
		Clube clube = new Clube(1, "Clube", 30);
		List<Jogador> plantel = plantel(23, 30);
		long rico = 100_000_000_00L;
		assertThat(mercado.lance(clube, rico, plantel, jogador(300, Posicao.ATACANTE, 25), "BRA", 0)).isPresent();
		assertThat(mercado.lance(clube, rico, plantel, jogador(301, Posicao.ATACANTE, 24), "BRA", 0)).isEmpty();
	}

	@Test
	void sorteiaCincoOuSeisAVendaDeFormaReproduzivel() {
		List<Jogador> plantel = plantel(23, 20);
		Set<Integer> quantidades = new HashSet<>();
		for (int semente = 0; semente < 40; semente++) {
			List<Jogador> novos = mercado.sortearAVenda(plantel, Set.of(), new SplittableRandom(semente));
			assertThat(novos).hasSizeBetween(5, 6).doesNotHaveDuplicates();
			assertThat(mercado.sortearAVenda(plantel, Set.of(), new SplittableRandom(semente))).isEqualTo(novos);
			quantidades.add(novos.size());
		}
		assertThat(quantidades).containsExactlyInAnyOrder(5, 6);
	}

	@Test
	void completaQuemTemPoucosAVendaENaoMexeEmQuemTemCinco() {
		List<Jogador> plantel = plantel(23, 20);
		List<Jogador> novos = mercado.sortearAVenda(plantel, Set.of(1L, 2L, 3L), new SplittableRandom(7));
		assertThat(novos).hasSizeBetween(2, 3).extracting(Jogador::id).doesNotContain(1L, 2L, 3L);
		assertThat(mercado.sortearAVenda(plantel, Set.of(1L, 2L, 3L, 4L, 5L), new SplittableRandom(7))).isEmpty();
	}

	@Test
	void anunciadosNaoDeixamPosicaoAbaixoDoElencoMinimo() {
		// 1 goleiro, 4 defensores, 4 meias, 2 atacantes e 6 meias a mais (17): só meias saem, e só 3,
		// para o plantel não ficar abaixo de 14
		List<Jogador> plantel = new ArrayList<>();
		plantel.add(jogador(1, Posicao.GOLEIRO, 20));
		for (int i = 0; i < 4; i++) {
			plantel.add(jogador(10 + i, Posicao.DEFENSOR, 20));
		}
		for (int i = 0; i < 10; i++) {
			plantel.add(jogador(20 + i, Posicao.MEIO_CAMPISTA, 20));
		}
		plantel.add(jogador(30, Posicao.ATACANTE, 20));
		plantel.add(jogador(31, Posicao.ATACANTE, 20));
		List<Jogador> novos = mercado.sortearAVenda(plantel, Set.of(), new SplittableRandom(3));
		assertThat(novos).hasSize(3).allMatch(j -> j.posicao() == Posicao.MEIO_CAMPISTA);
	}

	@Test
	void ricoComDoisLimitesGastaMaisSoComAAlavancaLigada() {
		List<Jogador> plantel = plantel(23, 17); // limite R$ 437.000
		MercadoDaMaquina ligado = new MercadoDaMaquina(ParametrosMercadoMaquina.padrao().comRico(2, 2, true),
				PrecoJogador.padrao(), CaixaDaRodada.padrao());
		assertThat(ligado.rico(874_000_00L, plantel)).isTrue();
		assertThat(ligado.rico(873_999_00L, plantel)).isFalse();
		assertThat(ligado.comprasPorRodada(874_000_00L, plantel)).isEqualTo(2);
		assertThat(ligado.comprasPorRodada(873_999_00L, plantel)).isEqualTo(1);
		// no padrão: sempre 1 compra e nenhuma compra direta
		assertThat(mercado.comprasPorRodada(10_000_000_00L, plantel)).isEqualTo(1);
		assertThat(mercado.compraDireta(10_000_000_00L, plantel, List.of(jogador(300, Posicao.ATACANTE, 30)), "BRA")).isEmpty();
	}

	@Test
	void compraDiretaLevaOMaisForteQueMelhoraOTimeECabeNoCaixa() {
		MercadoDaMaquina ligado = new MercadoDaMaquina(ParametrosMercadoMaquina.padrao().comRico(2, 1, true),
				PrecoJogador.padrao(), CaixaDaRodada.padrao());
		List<Jogador> plantel = plantel(23, 20);
		List<Jogador> anunciados = List.of(jogador(300, Posicao.ATACANTE, 20), jogador(301, Posicao.ATACANTE, 24),
				jogador(302, Posicao.MEIO_CAMPISTA, 26));
		long rico = 100_000_000_00L;
		assertThat(ligado.compraDireta(rico, plantel, anunciados, "BRA")).map(Jogador::id).contains(302L);
		// força igual à do time não melhora: não compra
		assertThat(ligado.compraDireta(rico, plantel, List.of(anunciados.get(0)), "BRA")).isEmpty();
		// rico, mas sem caixa para o preço cheio com a folga
		long limiteRico = ligado.limite(plantel) * 2;
		assertThat(ligado.compraDireta(limiteRico, plantel, List.of(anunciados.get(2)), "BRA")).isEmpty();
		assertThat(ligado.compraDireta(rico, plantel(Clube.MAXIMO_JOGADORES, 20), anunciados, "BRA")).isEmpty();
	}

	private static List<Jogador> plantel(int quantidade, int forca) {
		Posicao[] ordem = { Posicao.GOLEIRO, Posicao.DEFENSOR, Posicao.DEFENSOR, Posicao.MEIO_CAMPISTA,
				Posicao.MEIO_CAMPISTA, Posicao.ATACANTE };
		List<Jogador> plantel = new ArrayList<>();
		for (int i = 0; i < quantidade; i++) {
			plantel.add(jogador(i + 1, ordem[i % ordem.length], forca));
		}
		return plantel;
	}

	private static Jogador jogador(long id, Posicao posicao, int forca) {
		return new Jogador(id, "J" + id, posicao, forca, Temperamento.NORMAL, "BRA");
	}
}
