package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;

/** Migração V3 e plantel do {@link MoldeRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MoldeJogadoresIT {

	@Autowired
	MoldeRepositorio repositorio;

	@Autowired
	JdbcClient jdbc;

	long molde;
	long clube;

	@BeforeEach
	void criaClube() {
		molde = repositorio.criarMolde("Padrão");
		long pais = repositorio.criarPais(molde, "BRA", "Brasil");
		ConfiguracaoLiga liga = repositorio.criarLiga(pais, new ConfiguracaoLiga("Nacional",
				List.of(new Divisao("Série A", 1, 50, 0, 0, List.of(new Clube(0, "Alvinegro", 35)))), List.of()));
		clube = liga.divisoes().getFirst().clubes().getFirst().id();
	}

	@Test
	void plantelGravadoVoltaIgualComIdsGerados() {
		List<Jogador> original = List.of(
				new Jogador(0, "Goleiro", Posicao.GOLEIRO, 30, Temperamento.CALMO, true, "BRA"),
				new Jogador(0, "Zagueiro", Posicao.DEFENSOR, 28, Temperamento.VIOLENTO, "POR"),
				new Jogador(0, "Meia", Posicao.MEIO_CAMPISTA, 33, Temperamento.EDUCADO, "ARG"),
				new Jogador(0, "Atacante", Posicao.ATACANTE, 40, Temperamento.ANSIOSO, true, "COL"));

		List<Jogador> gravados = repositorio.adicionarJogadores(clube, original);

		assertThat(repositorio.carregarPlantel(clube)).isEqualTo(gravados);
		assertThat(gravados).extracting(Jogador::id).doesNotContain(0L).doesNotHaveDuplicates();
		assertThat(gravados).usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
			.containsExactlyElementsOf(original);
	}

	@Test
	void plantelAceitaAte25EDepoisRecusa() {
		repositorio.adicionarJogadores(clube, jogadores(23));
		repositorio.adicionarJogadores(clube, jogadores(2));

		assertThatThrownBy(() -> repositorio.adicionarJogadores(clube, jogadores(1)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("tem 25 jogadores")
			.hasMessageContaining("máximo de 25");
		assertThat(repositorio.carregarPlantel(clube)).hasSize(Clube.MAXIMO_JOGADORES);
	}

	@Test
	void loteQuePassariaDoMaximoERecusadoInteiro() {
		repositorio.adicionarJogadores(clube, jogadores(20));

		assertThatThrownBy(() -> repositorio.adicionarJogadores(clube, jogadores(6)))
			.isInstanceOf(IllegalStateException.class);
		assertThat(repositorio.carregarPlantel(clube)).hasSize(20);
	}

	@Test
	void falhaNoMeioDoLoteDesfazTudo() {
		repositorio.adicionarJogadores(clube, jogadores(1));
		// o domínio aceita o caractere nulo, mas o PostgreSQL não o guarda em TEXT: falha no 2º insert
		List<Jogador> lote = List.of(new Jogador(0, "Bom", Posicao.DEFENSOR, 20, Temperamento.NORMAL, "BRA"),
				new Jogador(0, "Nulo\u0000", Posicao.DEFENSOR, 20, Temperamento.NORMAL, "BRA"));

		assertThatThrownBy(() -> repositorio.adicionarJogadores(clube, lote)).isInstanceOf(DataAccessException.class);
		assertThat(repositorio.carregarPlantel(clube)).extracting(Jogador::nome).containsExactly("Jogador 1");
	}

	@Test
	void constraintsDoJogadorRecusamDadosInvalidos() {
		String insert = "INSERT INTO molde_jogador (clube_id, nome, nacionalidade, posicao, forca, temperamento) "
				+ "VALUES (?, 'X', ?, ?, ?, ?)";

		assertThatThrownBy(() -> jdbc.sql(insert).params(clube, "bra", "GOLEIRO", 20, "NORMAL").update())
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.sql(insert).params(clube, "BRA", "LIBERO", 20, "NORMAL").update())
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.sql(insert).params(clube, "BRA", "GOLEIRO", 51, "NORMAL").update())
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.sql(insert).params(clube, "BRA", "GOLEIRO", 20, "NERVOSO").update())
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.sql(insert).params(-1L, "BRA", "GOLEIRO", 20, "NORMAL").update())
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void nacionalidadeForaDoCatalogoFifaERecusada() {
		assertThatThrownBy(() -> repositorio.adicionarJogadores(clube,
				List.of(new Jogador(0, "Sem país", Posicao.ATACANTE, 20, Temperamento.NORMAL, "XYZ"))))
			.isInstanceOf(DataIntegrityViolationException.class);
		// ISO 3166-1 (PRT), e não FIFA (POR)
		assertThatThrownBy(() -> repositorio.adicionarJogadores(clube,
				List.of(new Jogador(0, "Português", Posicao.ATACANTE, 20, Temperamento.NORMAL, "PRT"))))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(repositorio.carregarPlantel(clube)).isEmpty();
	}

	@Test
	void catalogoTemOs211MembrosDaFifa() {
		assertThat(jdbc.sql("SELECT count(*) FROM pais_fifa").query(Long.class).single()).isEqualTo(211);
		assertThat(jdbc.sql("SELECT nome FROM pais_fifa WHERE codigo IN ('ENG', 'SCO', 'POR', 'GER') ORDER BY codigo")
			.query(String.class)
			.list()).containsExactly("Inglaterra", "Alemanha", "Portugal", "Escócia");
	}

	@Test
	void paisDoMoldeForaDoCatalogoERecusado() {
		assertThatThrownBy(() -> repositorio.criarPais(molde, "XYZ", "Inventado"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void clubeInexistenteERecusado() {
		assertThatThrownBy(() -> repositorio.adicionarJogadores(-1, jogadores(1)))
			.isInstanceOf(NoSuchElementException.class);
		assertThatThrownBy(() -> repositorio.carregarPlantel(-1)).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void apagarMoldeApagaJogadores() {
		repositorio.adicionarJogadores(clube, jogadores(3));

		jdbc.sql("DELETE FROM molde WHERE id = ?").param(molde).update();

		assertThat(jdbc.sql("SELECT count(*) FROM molde_jogador WHERE clube_id = ?").param(clube).query(Long.class).single())
			.isZero();
	}

	private static List<Jogador> jogadores(int quantidade) {
		return IntStream.rangeClosed(1, quantidade)
			.mapToObj(i -> new Jogador(0, "Jogador " + i, Posicao.MEIO_CAMPISTA, 25, Temperamento.NORMAL, "BRA"))
			.toList();
	}
}
