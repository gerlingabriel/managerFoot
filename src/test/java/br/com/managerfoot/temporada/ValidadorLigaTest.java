package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;

class ValidadorLigaTest {

	private long proximoId = 1;

	private List<Clube> clubes(int quantidade) {
		List<Clube> clubes = new ArrayList<>();
		for (int i = 0; i < quantidade; i++) {
			long id = proximoId++;
			clubes.add(new Clube(id, "Clube " + id, 30));
		}
		return clubes;
	}

	private Divisao divisao(String nome, int prioridade, int limite, int promovidos, int rebaixados, int clubes) {
		return new Divisao(nome, prioridade, limite, promovidos, rebaixados, clubes(clubes));
	}

	@Test
	void exemploDaSpecUmaDivisaoCom20ERebaixamentoDe4Precisa24Clubes() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(divisao("Primeira", 1, 50, 0, 4, 20)), clubes(4));

		assertThat(liga.clubesNecessarios()).isEqualTo(24);
		assertThat(ValidadorLiga.problemas(liga)).isEmpty();
		assertThatNoException().isThrownBy(() -> ValidadorLiga.exigirValida(liga));
	}

	@Test
	void exemploDaSpecDuasDivisoes20E18Precisa42Clubes() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga",
				List.of(divisao("Série A", 1, 50, 0, 4, 20), divisao("Série B", 2, 30, 4, 4, 18)), clubes(4));

		assertThat(liga.clubesNecessarios()).isEqualTo(42);
		assertThat(ValidadorLiga.problemas(liga)).isEmpty();
	}

	@Test
	void faltaDeClubesNaFilaExplicaQuantosFaltam() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga",
				List.of(divisao("Série A", 1, 50, 0, 4, 20), divisao("Série B", 2, 30, 4, 4, 18)), clubes(1));

		assertThat(ValidadorLiga.problemas(liga)).singleElement()
			.asString()
			.contains("precisa de 42 clubes")
			.contains("mas há 39")
			.contains("Faltam 3 clube(s) na fila de espera");
	}

	@Test
	void ultimaDivisaoSemRebaixamentoNaoPrecisaDeFila() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(divisao("Única", 1, 50, 0, 0, 16)), List.of());
		assertThat(ValidadorLiga.problemas(liga)).isEmpty();
	}

	@Test
	void divisaoForaDaFaixaDe15A20() {
		ConfiguracaoLiga pequena = new ConfiguracaoLiga("Liga", List.of(divisao("A", 1, 50, 0, 0, 14)), List.of());
		ConfiguracaoLiga grande = new ConfiguracaoLiga("Liga", List.of(divisao("A", 1, 50, 0, 0, 21)), List.of());

		assertThat(ValidadorLiga.problemas(pequena)).anyMatch(p -> p.contains("tem 14 clubes") && p.contains("de 15 a 20"));
		assertThat(ValidadorLiga.problemas(grande)).anyMatch(p -> p.contains("tem 21 clubes"));
	}

	@Test
	void ligaPrincipalTemLimite50ENaoTemPromovidos() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(divisao("A", 1, 30, 2, 0, 16)), List.of());

		assertThat(ValidadorLiga.problemas(liga))
			.anyMatch(p -> p.contains("deve ter limite de força 50, mas tem 30"))
			.anyMatch(p -> p.contains("não pode ter promovidos"));
	}

	@Test
	void rebaixadosDeUmaDivisaoDevemIgualarPromovidosDaDeBaixo() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga",
				List.of(divisao("Série A", 1, 50, 0, 4, 20), divisao("Série B", 2, 30, 3, 0, 18)), List.of());

		assertThat(ValidadorLiga.problemas(liga))
			.anyMatch(p -> p.contains("rebaixa 4") && p.contains("promove 3") && p.contains("devem ser iguais"));
	}

	@Test
	void prioridadesDevemSerSequenciais() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga",
				List.of(divisao("Série A", 1, 50, 0, 0, 16), divisao("Série C", 3, 30, 0, 0, 16)), List.of());

		assertThat(ValidadorLiga.problemas(liga)).anyMatch(p -> p.contains("deveria ter prioridade 2"));
	}

	@Test
	void divisoesSaoOrdenadasPelaPrioridade() {
		Divisao b = divisao("Série B", 2, 30, 4, 4, 16);
		Divisao a = divisao("Série A", 1, 50, 0, 4, 16);
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(b, a), clubes(4));

		assertThat(liga.divisoes()).containsExactly(a, b);
		assertThat(ValidadorLiga.problemas(liga)).isEmpty();
	}

	@Test
	void promovidosMaisRebaixadosNaoPassamDoTotal() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga",
				List.of(divisao("A", 1, 50, 0, 9, 16), divisao("B", 2, 30, 9, 8, 16)), clubes(8));

		assertThat(ValidadorLiga.problemas(liga)).anyMatch(p -> p.contains("promove 9 e rebaixa 8"));
	}

	@Test
	void clubeRepetidoEntreDivisoesOuFila() {
		List<Clube> primeira = clubes(16);
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(new Divisao("A", 1, 50, 0, 1, primeira)),
				List.of(primeira.get(3)));

		assertThat(ValidadorLiga.problemas(liga)).anyMatch(p -> p.contains(primeira.get(3).nome() + " aparece mais de uma vez"));
	}

	@Test
	void ligaSemDivisoesOuSemNome() {
		assertThat(ValidadorLiga.problemas(new ConfiguracaoLiga(" ", List.of(), List.of())))
			.contains("A liga precisa de um nome.", "A liga precisa de ao menos uma divisão.");
	}

	@Test
	void exigirValidaListaTodosOsProblemas() {
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(divisao("A", 1, 30, 0, 2, 14)), List.of());

		assertThatIllegalArgumentException().isThrownBy(() -> ValidadorLiga.exigirValida(liga))
			.withMessageStartingWith("A liga não pode iniciar:")
			.withMessageContaining("tem 14 clubes")
			.withMessageContaining("limite de força 50")
			.withMessageContaining("Faltam 2 clube(s)");
	}

	@Test
	void formatoDoMoldeConfereDivisoesEClubesDoPais() {
		FormatoLiga formato = new FormatoLiga("Italiano", List.of(new FormatoDivisao("Serie A", 1, 19, 50, 2),
				new FormatoDivisao("Serie B", 2, 20, 30, 4)));

		assertThat(ValidadorLiga.problemas(formato, 43)).isEmpty();
		assertThat(ValidadorLiga.problemas(formato, 42)).containsExactly(
				"A liga precisa de 43 clubes do país (soma das divisões + 4 rebaixado(s) da última divisão), mas há 42. "
						+ "Faltam 1 clube(s).");
	}

	@Test
	void formatoComDivisaoForaDaFaixaOuPrincipalSemLimite50() {
		FormatoLiga formato = new FormatoLiga("Liga", List.of(new FormatoDivisao("A", 1, 12, 40, 2)));

		assertThat(ValidadorLiga.problemas(formato, 100)).hasSize(2)
			.anyMatch(p -> p.contains("tem 12 clubes"))
			.anyMatch(p -> p.contains("limite de força 50"));
		assertThat(ValidadorLiga.problemas(new FormatoLiga(null, List.of()), 0))
			.contains("A liga precisa de um nome.", "A liga precisa de ao menos uma divisão.");
	}

	@Test
	void formatoComPrioridadeRepetidaOuPulada() {
		FormatoLiga formato = new FormatoLiga("Liga",
				List.of(new FormatoDivisao("A", 1, 20, 50, 2), new FormatoDivisao("C", 3, 20, 20, 2)));
		assertThat(ValidadorLiga.problemas(formato, 100)).singleElement().asString().contains("deveria ter prioridade 2");
	}

	@Test
	void ligaComTresDivisoesPassaDoMaximoDeDuas() {
		FormatoLiga formato = new FormatoLiga("Liga", List.of(new FormatoDivisao("A", 1, 20, 50, 4),
				new FormatoDivisao("B", 2, 20, 30, 4), new FormatoDivisao("C", 3, 20, 22, 4)));
		ConfiguracaoLiga liga = new ConfiguracaoLiga("Liga", List.of(divisao("A", 1, 50, 0, 4, 20),
				divisao("B", 2, 30, 4, 4, 20), divisao("C", 3, 22, 4, 4, 20)), clubes(4));

		assertThat(ValidadorLiga.problemas(formato, 100)).containsExactly("A liga tem 3 divisões; o máximo é 2.");
		assertThat(ValidadorLiga.problemas(liga)).contains("A liga tem 3 divisões; o máximo é 2.");
	}
}
