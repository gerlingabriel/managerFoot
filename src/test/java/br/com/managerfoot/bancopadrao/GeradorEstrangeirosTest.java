package br.com.managerfoot.bancopadrao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.bancopadrao.GeradorBancoPadrao.ClubeGerado;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao.EstrangeiroGerado;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.ParametrosMotor;

/** Países gerados só para a competição continental (SPEC §8.3.6, rev. 72 e 73). */
class GeradorEstrangeirosTest {

	static final ParametrosBancoPadrao PARAMETROS = ParametrosBancoPadrao.padrao();
	static final GeradorBancoPadrao GERADOR = new GeradorBancoPadrao(PARAMETROS, ParametrosMotor.padrao());
	static final List<EstrangeiroGerado> ESTRANGEIROS = GERADOR.gerarEstrangeiros(PaisEstrangeiroPadrao.TODOS,
			ContinentalPadrao.TODAS);

	static Set<String> codigos(List<String> lista) {
		return Set.copyOf(lista);
	}

	@Test
	void asVagasDasDuasRegioesSomam32() {
		for (ContinentalPadrao continental : ContinentalPadrao.TODAS) {
			assertThat(continental.vagas().values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(32);
		}
		assertThat(ContinentalPadrao.AMERICA_DO_SUL.vagas()).containsEntry("BRA", 7).containsEntry("ARG", 6)
			.containsEntry("VEN", 2);
	}

	@Test
	void vagasQueNaoSomam32SaoRecusadas() {
		org.assertj.core.api.Assertions
			.assertThatThrownBy(() -> new ContinentalPadrao(Regiao.EUROPA, "Teste", Map.of("ENG", 31)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("31");
	}

	@Test
	void todoPaisComVagaTemLigaNoBancoPadraoOuEGeradoENuncaOsDois() {
		Set<String> comLiga = PaisPadrao.TODOS.stream().map(PaisPadrao::codigo).collect(Collectors.toSet());
		Set<String> gerados = PaisEstrangeiroPadrao.TODOS.stream().map(PaisEstrangeiroPadrao::codigo)
			.collect(Collectors.toSet());
		Set<String> comVaga = ContinentalPadrao.TODAS.stream().flatMap(c -> c.vagas().keySet().stream())
			.collect(Collectors.toSet());
		assertThat(gerados).doesNotContainAnyElementsOf(comLiga);
		assertThat(comVaga).containsExactlyInAnyOrderElementsOf(
				java.util.stream.Stream.concat(comLiga.stream(), gerados.stream()).toList());
	}

	@Test
	void cadaPaisGeradoTemODobroDasSuasVagas() {
		Map<String, Integer> quantidade = ESTRANGEIROS.stream()
			.collect(Collectors.toMap(e -> e.pais().codigo(), e -> e.clubes().size()));
		assertThat(quantidade).containsEntry("GER", 8).containsEntry("FRA", 6).containsEntry("URU", 6)
			.containsEntry("VEN", 4).containsEntry("NOR", 2);
	}

	@Test
	void forcaDosClubesFicaNaFaixaDoNivelDoPais() {
		for (EstrangeiroGerado pais : ESTRANGEIROS) {
			ParametrosBancoPadrao.FaixaForca faixa = PARAMETROS.faixasEstrangeiros().get(pais.pais().nivel());
			assertThat(pais.clubes()).as(pais.pais().nome())
				.allSatisfy(c -> assertThat(c.clube().forca()).isBetween(faixa.minima(), faixa.maxima()))
				.allSatisfy(c -> assertThat(c.jogadores()).allSatisfy(j -> assertThat(j.forca()).isLessThanOrEqualTo(faixa.maxima())));
		}
	}

	@Test
	void argentinaDoBancoPadraoEMaisForteQueAVenezuelaGerada() {
		GeradorBancoPadrao.PaisGerado argentina = GERADOR.gerar(PaisPadrao.TODOS.get(1));
		assertThat(argentina.pais().codigo()).isEqualTo("ARG");
		double argentinos = argentina.clubes().subList(0, 20).stream().mapToInt(c -> c.clube().forca()).average().orElseThrow();
		double venezuelanos = media("VEN");
		assertThat(argentinos).isGreaterThan(venezuelanos + 5);
		assertThat(media("GER")).isGreaterThan(media("NED")).isGreaterThan(media("NOR"));
	}

	static double media(String codigo) {
		return ESTRANGEIROS.stream().filter(e -> e.pais().codigo().equals(codigo)).findFirst().orElseThrow().clubes()
			.stream().mapToInt(c -> c.clube().forca()).average().orElseThrow();
	}

	@Test
	void planteisTem20JogadoresDoPaisComAMediaDosOnzeNaForcaDoClube() {
		EscalacaoMaquina maquina = new EscalacaoMaquina(ParametrosMotor.padrao());
		for (EstrangeiroGerado pais : ESTRANGEIROS) {
			for (ClubeGerado clube : pais.clubes()) {
				assertThat(clube.jogadores()).hasSize(20)
					.allSatisfy(j -> assertThat(j.nacionalidade()).isEqualTo(pais.pais().codigo()));
				assertThat(clube.jogadores()).extracting(Jogador::nome).doesNotHaveDuplicates();
				double emCampo = GeradorBancoPadrao.mediaForca(maquina.escalar(clube.jogadores()).titulares());
				assertThat(emCampo).as(clube.clube().nome()).isBetween(clube.clube().forca() - 0.5,
						clube.clube().forca() + 0.5);
			}
		}
	}

	@Test
	void nomesDeClubeETreinadorNaoSeRepetemNoPaisENomesVemDoIdioma() {
		for (EstrangeiroGerado pais : ESTRANGEIROS) {
			assertThat(pais.clubes()).extracting(c -> c.clube().nome()).doesNotHaveDuplicates();
			assertThat(pais.clubes()).extracting(ClubeGerado::treinador).doesNotHaveDuplicates()
				.allMatch(t -> t != null && t.contains(" "));
			Idioma idioma = pais.pais().idioma();
			assertThat(pais.clubes().getFirst().jogadores()).extracting(Jogador::nome)
				.allMatch(n -> idioma.nomes().contains(n) || idioma.sobrenomes().contains(n) || idioma.nomes().stream()
					.anyMatch(nome -> n.startsWith(nome + " ") && idioma.sobrenomes().contains(n.substring(nome.length() + 1))));
		}
	}

	@Test
	void mesmaSementeGeraOsMesmosClubes() {
		assertThat(new GeradorBancoPadrao(PARAMETROS, ParametrosMotor.padrao())
			.gerarEstrangeiros(PaisEstrangeiroPadrao.TODOS, ContinentalPadrao.TODAS)).isEqualTo(ESTRANGEIROS);
	}
}
