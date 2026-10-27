package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escudo;
import br.com.managerfoot.dominio.ModeloEscudo;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.ValidadorLiga;

/** Migração V2 e {@link MoldeRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MoldeRepositorioIT {

	@Autowired
	MoldeRepositorio repositorio;

	@Autowired
	JdbcClient jdbc;

	@Test
	void ligaGravadaVoltaIgualComIdsGerados() {
		long pais = repositorio.criarPais(repositorio.criarMolde("Padrão"), "BRA", "Brasil");
		ConfiguracaoLiga original = ligaDeDuasDivisoes();

		ConfiguracaoLiga gravada = repositorio.criarLiga(pais, original);
		ConfiguracaoLiga carregada = repositorio.carregarLiga(pais);

		assertThat(carregada).isEqualTo(gravada);
		assertThat(carregada.nome()).isEqualTo("Campeonato Nacional");
		assertThat(carregada.divisoes()).extracting(Divisao::prioridade).containsExactly(1, 2);
		assertThat(carregada.divisoes().get(1).clubes()).extracting(Clube::nome)
			.containsExactlyElementsOf(original.divisoes().get(1).clubes().stream().map(Clube::nome).toList());
		assertThat(carregada.filaDeEspera()).extracting(Clube::nome).containsExactly("Fila 1", "Fila 2");
		assertThat(carregada.divisoes().getFirst().clubes().getFirst().escudo())
			.isEqualTo(new Escudo(ModeloEscudo.LISTRAS_VERTICAIS, "#111827", "#F4F1E6"));
		assertThat(ValidadorLiga.problemas(carregada)).isEmpty();
	}

	@Test
	void paisesDoMesmoMoldeTemLigasIndependentes() {
		long molde = repositorio.criarMolde("Padrão");
		long brasil = repositorio.criarPais(molde, "BRA", "Brasil");
		long argentina = repositorio.criarPais(molde, "ARG", "Argentina");
		repositorio.criarLiga(brasil, ligaDeDuasDivisoes());
		repositorio.criarLiga(argentina, new ConfiguracaoLiga("Liga Argentina",
				List.of(new Divisao("Primera", 1, 50, 0, 2, clubes("ARG", 16, 30))), clubes("ARG fila", 2, 20)));

		assertThat(repositorio.carregarLiga(argentina).divisoes()).hasSize(1);
		assertThat(repositorio.carregarLiga(brasil).divisoes()).hasSize(2);
	}

	@Test
	void codigoDePaisRepetidoNoMesmoMoldeERecusado() {
		long molde = repositorio.criarMolde("Padrão");
		repositorio.criarPais(molde, "BRA", "Brasil");

		assertThatThrownBy(() -> repositorio.criarPais(molde, "BRA", "Brasil de novo"))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> repositorio.criarPais(molde, "br", "Minúsculas"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void paisNaoPodeGanharSegundaLiga() {
		long pais = repositorio.criarPais(repositorio.criarMolde("Padrão"), "BRA", "Brasil");
		repositorio.criarLiga(pais, ligaDeDuasDivisoes());

		assertThatThrownBy(() -> repositorio.criarLiga(pais, ligaDeDuasDivisoes()))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void falhaNoMeioDaGravacaoDesfazTudo() {
		long pais = repositorio.criarPais(repositorio.criarMolde("Padrão"), "BRA", "Brasil");
		// prioridade repetida: a segunda divisão viola a constraint depois de a primeira ser gravada
		ConfiguracaoLiga invalida = new ConfiguracaoLiga("Repetida", List.of(
				new Divisao("A", 1, 50, 0, 0, clubes("A", 2, 30)), new Divisao("B", 1, 30, 0, 0, clubes("B", 2, 20))),
				List.of());

		assertThatThrownBy(() -> repositorio.criarLiga(pais, invalida))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(contar("SELECT count(*) FROM molde_clube WHERE pais_id = ?", pais)).isZero();
		assertThat(repositorio.carregarLiga(pais).nome()).isNull();
	}

	@Test
	void constraintsDoClubeRecusamDadosInvalidos() {
		long pais = repositorio.criarPais(repositorio.criarMolde("Padrão"), "BRA", "Brasil");
		String insert = "INSERT INTO molde_clube (pais_id, nome, forca, escudo_modelo, escudo_cor_1, escudo_cor_2) "
				+ "VALUES (?, 'X', ?, ?, ?, ?)";

		assertThatThrownBy(() -> jdbc.sql(insert).params(pais, 51, "QUARTOS", "#000000", "#FFFFFF").update())
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.sql(insert).params(pais, 20, "ESTRELA", "#000000", "#FFFFFF").update())
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.sql(insert).params(pais, 20, "QUARTOS", "#abcdef", "#FFFFFF").update())
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.sql(insert).params(pais, 20, "QUARTOS", "#FFFFFF", "#FFFFFF").update())
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void clubeNaoPodeEstarEmDivisaoDeOutroPais() {
		long molde = repositorio.criarMolde("Padrão");
		long brasil = repositorio.criarPais(molde, "BRA", "Brasil");
		long argentina = repositorio.criarPais(molde, "ARG", "Argentina");
		repositorio.criarLiga(brasil, ligaDeDuasDivisoes());
		long divisaoBrasileira = jdbc.sql("SELECT id FROM molde_divisao WHERE pais_id = ? AND prioridade = 1")
			.param(brasil)
			.query(Long.class)
			.single();

		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO molde_clube (pais_id, divisao_id, nome, forca, escudo_modelo, escudo_cor_1, escudo_cor_2)
				VALUES (?, ?, 'Intruso', 20, 'QUARTOS', '#000000', '#FFFFFF')""").params(argentina, divisaoBrasileira).update())
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void apagarMoldeApagaPaisesDivisoesEClubes() {
		long molde = repositorio.criarMolde("Temporário");
		long pais = repositorio.criarPais(molde, "BRA", "Brasil");
		repositorio.criarLiga(pais, ligaDeDuasDivisoes());

		jdbc.sql("DELETE FROM molde WHERE id = ?").param(molde).update();

		assertThat(contar("SELECT count(*) FROM molde_pais WHERE id = ?", pais)).isZero();
		assertThat(contar("SELECT count(*) FROM molde_divisao WHERE pais_id = ?", pais)).isZero();
		assertThat(contar("SELECT count(*) FROM molde_clube WHERE pais_id = ?", pais)).isZero();
	}

	@Test
	void paisInexistenteERecusadoAoCarregar() {
		assertThatThrownBy(() -> repositorio.carregarLiga(-1)).isInstanceOf(NoSuchElementException.class);
	}

	private long contar(String sql, long id) {
		return jdbc.sql(sql).param(id).query(Long.class).single();
	}

	/** 1ª com 16 clubes e 2 rebaixados, 2ª com 16 clubes, 2 promovidos e 2 rebaixados, fila com 2. */
	private static ConfiguracaoLiga ligaDeDuasDivisoes() {
		List<Clube> primeira = new ArrayList<>(clubes("Primeira", 16, 40));
		primeira.set(0, new Clube(0, "Listrado", 40, new Escudo(ModeloEscudo.LISTRAS_VERTICAIS, "#111827", "#f4f1e6")));
		return new ConfiguracaoLiga("Campeonato Nacional",
				List.of(new Divisao("Série A", 1, 50, 0, 2, primeira),
						new Divisao("Série B", 2, 30, 2, 2, clubes("Segunda", 16, 25))),
				clubes("Fila", 2, 15));
	}

	private static List<Clube> clubes(String prefixo, int quantidade, int forca) {
		return IntStream.rangeClosed(1, quantidade).mapToObj(i -> new Clube(0, prefixo + " " + i, forca)).toList();
	}
}
