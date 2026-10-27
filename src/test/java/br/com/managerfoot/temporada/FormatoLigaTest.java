package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;

class FormatoLigaTest {

	/** Itália do exemplo do usuário: 1ª de 19 com 2 rebaixados, 2ª de 20 com 4 rebaixados. */
	private static final FormatoLiga ITALIA = new FormatoLiga("Campeonato Italiano",
			List.of(new FormatoDivisao("Serie A", 1, 19, 50, 2), new FormatoDivisao("Serie B", 2, 20, 30, 4)));

	/** Clubes com as forças dadas, ids 1, 2, 3... na ordem. */
	private static List<Clube> clubes(int... forcas) {
		List<Clube> clubes = new ArrayList<>();
		for (int i = 0; i < forcas.length; i++) {
			clubes.add(new Clube(i + 1, "C" + (i + 1), forcas[i]));
		}
		return clubes;
	}

	private static int[] forcas(int quantidade, int forca) {
		int[] forcas = new int[quantidade];
		Arrays.fill(forcas, forca);
		return forcas;
	}

	private static int[] juntar(int[]... partes) {
		return Arrays.stream(partes).flatMapToInt(Arrays::stream).toArray();
	}

	@Test
	void promovidosVemDosRebaixadosDaDivisaoDeCima() {
		assertThat(ITALIA.promovidos(0)).isZero();
		assertThat(ITALIA.promovidos(1)).isEqualTo(2);
		assertThat(ITALIA.clubesNecessarios()).isEqualTo(43);
	}

	@Test
	void maisFortesFicamNaPrincipalESemMudarAForca() {
		// os 19 primeiros (força 50) vêm depois dos fracos na lista, para a ordem de cadastro não importar
		List<Clube> clubes = clubes(juntar(forcas(25, 20), forcas(19, 50)));

		ConfiguracaoLiga liga = ITALIA.distribuir(clubes, new SplittableRandom(1));

		assertThat(liga.divisoes().get(0).clubes()).hasSize(19).allMatch(c -> c.forca() == 50);
		assertThat(liga.divisoes().get(1).clubes()).hasSize(20).allMatch(c -> c.forca() == 20);
		assertThat(liga.filaDeEspera()).hasSize(5);
		assertThat(liga.divisoes().get(1).promovidos()).isEqualTo(2);
		assertThat(liga.divisoes().get(1).limiteForca()).isEqualTo(30);
		assertThat(ValidadorLiga.problemas(liga)).isEmpty();
	}

	@Test
	void clubeAcimaDoTetoComecaComAForcaCadastrada() {
		List<Clube> clubes = clubes(juntar(forcas(19, 50), forcas(24, 35)));

		ConfiguracaoLiga liga = ITALIA.distribuir(clubes, new SplittableRandom(1));

		assertThat(liga.divisoes().get(1).clubes()).allMatch(c -> c.forca() == 35);
	}

	@Test
	void empateNaFronteiraESorteadoEReproduzivelPelaSemente() {
		// 22 clubes de força 50 para 19 vagas da Serie A
		List<Clube> clubes = clubes(juntar(forcas(22, 50), forcas(21, 10)));
		Set<Set<Long>> serieAs = new HashSet<>();
		for (long semente = 0; semente < 20; semente++) {
			ConfiguracaoLiga liga = ITALIA.distribuir(clubes, new SplittableRandom(semente));
			List<Clube> serieA = liga.divisoes().get(0).clubes();
			assertThat(serieA).hasSize(19).allMatch(c -> c.forca() == 50);
			assertThat(liga.divisoes().get(1).clubes()).filteredOn(c -> c.forca() == 50).hasSize(3);
			serieAs.add(new HashSet<>(serieA.stream().map(Clube::id).toList()));
			assertThat(ITALIA.distribuir(clubes, new SplittableRandom(semente))).isEqualTo(liga);
		}
		assertThat(serieAs).hasSizeGreaterThan(1);
	}

	@Test
	void ordemDaListaRecebidaNaoMudaADistribuicao() {
		List<Clube> clubes = clubes(juntar(forcas(22, 50), forcas(21, 10)));
		List<Clube> invertidos = new ArrayList<>(clubes.reversed());
		assertThat(ITALIA.distribuir(invertidos, new SplittableRandom(3)))
			.isEqualTo(ITALIA.distribuir(clubes, new SplittableRandom(3)));
	}

	@Test
	void clubesInsuficientesSaoRecusadosDizendoQuantosFaltam() {
		List<Clube> clubes = clubes(forcas(30, 30));
		assertThatIllegalArgumentException().isThrownBy(() -> ITALIA.distribuir(clubes, new SplittableRandom(1)))
			.withMessageContaining("43 clubes").withMessageContaining("Faltam 13");
	}

	@Test
	void premierLeagueCom21IngleseFicaComTresNaFila() {
		FormatoLiga premier = new FormatoLiga("Premier League", List.of(new FormatoDivisao("Premier", 1, 18, 50, 3)));
		ConfiguracaoLiga liga = premier.distribuir(clubes(forcas(21, 40)), new SplittableRandom(1));
		assertThat(liga.divisoes().getFirst().clubes()).hasSize(18);
		assertThat(liga.filaDeEspera()).hasSize(3);
	}
}
