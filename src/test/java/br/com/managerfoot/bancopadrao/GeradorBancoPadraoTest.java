package br.com.managerfoot.bancopadrao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.bancopadrao.GeradorBancoPadrao.ClubeGerado;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao.PaisGerado;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.FormatoDivisao;
import br.com.managerfoot.temporada.ValidadorLiga;

class GeradorBancoPadraoTest {

	static final ParametrosBancoPadrao PARAMETROS = ParametrosBancoPadrao.padrao();
	static final List<PaisGerado> BANCO = new GeradorBancoPadrao(PARAMETROS, ParametrosMotor.padrao())
		.gerar(PaisPadrao.TODOS);

	static List<ClubeGerado> todosOsClubes() {
		return BANCO.stream().flatMap(p -> p.clubes().stream()).toList();
	}

	@Test
	void osSeisPaisesTemClubesSuficientesEFormatoValido() {
		assertThat(BANCO).extracting(p -> p.pais().codigo()).containsExactly("BRA", "ARG", "ENG", "ESP", "ITA", "POR");
		for (PaisGerado pais : BANCO) {
			assertThat(pais.clubes()).hasSize(pais.pais().clubesNecessarios());
			assertThat(ValidadorLiga.problemas(pais.pais().formato(), pais.clubes().size())).as(pais.pais().nome())
				.isEmpty();
			ConfiguracaoLiga liga = pais.pais().formato()
				.distribuir(pais.clubes().stream().map(ClubeGerado::clube).toList(), new SplittableRandom(7));
			assertThat(ValidadorLiga.problemas(liga)).as(pais.pais().nome()).isEmpty();
		}
	}

	@Test
	void forcaDosClubesFicaNaFaixaDaSuaDivisao() {
		for (PaisGerado pais : BANCO) {
			ConfiguracaoLiga liga = pais.pais().formato()
				.distribuir(pais.clubes().stream().map(ClubeGerado::clube).toList(), new SplittableRandom(7));
			for (int i = 0; i < liga.divisoes().size(); i++) {
				Divisao divisao = liga.divisoes().get(i);
				ParametrosBancoPadrao.FaixaForca faixa = PARAMETROS.faixas().get(i);
				assertThat(divisao.clubes()).as(pais.pais().nome() + " " + divisao.nome())
					.allSatisfy(c -> assertThat(c.forca()).isBetween(faixa.minima(), faixa.maxima()));
			}
		}
	}

	@Test
	void cadaClubeTem23JogadoresNacionaisNaComposicaoPadrao() {
		for (PaisGerado pais : BANCO) {
			for (ClubeGerado clube : pais.clubes()) {
				assertThat(clube.jogadores()).hasSize(23);
				Map<Posicao, Long> porPosicao = clube.jogadores().stream()
					.collect(Collectors.groupingBy(Jogador::posicao, () -> new EnumMap<>(Posicao.class), Collectors.counting()));
				assertThat(porPosicao).containsExactlyInAnyOrderEntriesOf(Map.of(Posicao.GOLEIRO, 3L, Posicao.DEFENSOR, 7L,
						Posicao.MEIO_CAMPISTA, 7L, Posicao.ATACANTE, 6L));
				assertThat(clube.jogadores()).allSatisfy(j -> assertThat(j.nacionalidade()).isEqualTo(pais.pais().codigo()));
				assertThat(clube.jogadores()).extracting(Jogador::nome).doesNotHaveDuplicates();
			}
		}
	}

	@Test
	void mediaDosOnzeEmCampoBateComAForcaDoClube() {
		EscalacaoMaquina maquina = new EscalacaoMaquina(ParametrosMotor.padrao());
		for (ClubeGerado clube : todosOsClubes()) {
			double media = GeradorBancoPadrao.mediaForca(maquina.escalar(clube.jogadores()).titulares());
			assertThat(media).as(clube.clube().nome()).isBetween(clube.clube().forca() - 0.5, clube.clube().forca() + 0.5);
		}
	}

	@Test
	void jogadoresNaoPassamDoTetoDaFaixaEmQueOClubeFoiGerado() {
		for (PaisGerado pais : BANCO) {
			List<FormatoDivisao> divisoes = pais.pais().formato().divisoes();
			int indice = 0;
			for (int i = 0; i < divisoes.size(); i++) {
				int quantidade = divisoes.get(i).quantidadeClubes() + (i == divisoes.size() - 1 ? divisoes.get(i).rebaixados() : 0);
				int teto = PARAMETROS.faixas().get(i).maxima();
				for (ClubeGerado clube : pais.clubes().subList(indice, indice + quantidade)) {
					assertThat(clube.jogadores()).as(clube.clube().nome())
						.allSatisfy(j -> assertThat(j.forca()).isBetween(1, teto));
				}
				indice += quantidade;
			}
		}
	}

	@Test
	void asteriscosVaoDeZeroATresEAparecemMaisNosClubesFortes() {
		double fortes = mediaAsteriscos(c -> c.forca() >= 40);
		double fracos = mediaAsteriscos(c -> c.forca() <= 22);
		for (ClubeGerado clube : todosOsClubes()) {
			assertThat(clube.jogadores().stream().filter(Jogador::asterisco).count()).isBetween(0L, 3L);
		}
		assertThat(fortes).isGreaterThan(fracos + 0.8);
	}

	static double mediaAsteriscos(java.util.function.Predicate<Clube> filtro) {
		return todosOsClubes().stream()
			.filter(c -> filtro.test(c.clube()))
			.mapToLong(c -> c.jogadores().stream().filter(Jogador::asterisco).count())
			.average()
			.orElseThrow();
	}

	@Test
	void temperamentoNormalEOMaisComumEViolentoOMaisRaro() {
		Map<Temperamento, Long> contagem = todosOsClubes().stream()
			.flatMap(c -> c.jogadores().stream())
			.collect(Collectors.groupingBy(Jogador::temperamento, Collectors.counting()));
		long normal = contagem.get(Temperamento.NORMAL);
		long violento = contagem.get(Temperamento.VIOLENTO);
		assertThat(contagem.values()).allSatisfy(n -> assertThat(n).isBetween(violento, normal));
	}

	@Test
	void nomesDeJogadoresAparecemNasTresFormas() {
		List<String> nomes = todosOsClubes().stream().flatMap(c -> c.jogadores().stream()).map(Jogador::nome).toList();
		assertThat(nomes).anyMatch(n -> n.contains(" ") && Idioma.PORTUGUES.nomes().contains(n.split(" ")[0]));
		assertThat(nomes).anyMatch(Idioma.PORTUGUES.nomes()::contains);
		assertThat(nomes).anyMatch(Idioma.PORTUGUES.sobrenomes()::contains);
	}

	@Test
	void nomesDosClubesNaoSeRepetemNoPais() {
		for (PaisGerado pais : BANCO) {
			assertThat(pais.clubes()).extracting(c -> c.clube().nome()).doesNotHaveDuplicates();
		}
	}

	@Test
	void mesmaSementeGeraOMesmoBanco() {
		List<PaisGerado> outro = new GeradorBancoPadrao(PARAMETROS, ParametrosMotor.padrao()).gerar(PaisPadrao.TODOS);
		assertThat(outro).isEqualTo(BANCO);
	}

	@Test
	void acrescentarUmPaisNaoMudaOsOutros() {
		GeradorBancoPadrao gerador = new GeradorBancoPadrao(PARAMETROS, ParametrosMotor.padrao());
		Function<PaisPadrao, PaisGerado> sozinho = gerador::gerar;
		assertThat(sozinho.apply(PaisPadrao.TODOS.get(3))).isEqualTo(BANCO.get(3));
	}
}
