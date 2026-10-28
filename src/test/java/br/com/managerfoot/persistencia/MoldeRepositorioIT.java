package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import br.com.managerfoot.temporada.FormatoDivisao;
import br.com.managerfoot.temporada.FormatoLiga;
import br.com.managerfoot.temporada.ValidadorLiga;

/** Migrações V2 e V10 e {@link MoldeRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MoldeRepositorioIT {

	@Autowired
	MoldeRepositorio repositorio;

	@Autowired
	JdbcClient jdbc;

	@Test
	void formatoTemPrioridadesAutomaticasEPromovidosDaDivisaoDeCima() {
		long pais = repositorio.criarPais(repositorio.criarMolde("Padrão"), "ITA", "Itália");
		repositorio.criarLiga(pais, "Campeonato Italiano");

		assertThat(repositorio.adicionarDivisao(pais, "Serie A", 19, 50, 2)).isEqualTo(1);
		assertThat(repositorio.adicionarDivisao(pais, "Serie B", 20, 30, 4)).isEqualTo(2);

		FormatoLiga formato = repositorio.carregarFormato(pais);
		assertThat(formato.nome()).isEqualTo("Campeonato Italiano");
		assertThat(formato.divisoes()).containsExactly(new FormatoDivisao("Serie A", 1, 19, 50, 2),
				new FormatoDivisao("Serie B", 2, 20, 30, 4));
		assertThat(formato.promovidos(1)).isEqualTo(2);
		assertThat(formato.clubesNecessarios()).isEqualTo(43);
	}

	@Test
	void clubeSemTreinadorERecusado() {
		long pais = repositorio.criarPais(repositorio.criarMolde("Padrão"), "BRA", "Brasil");

		assertThatThrownBy(() -> repositorio.adicionarClube(pais, new Clube(0, "Sem", 20), " "))
			.isInstanceOf(IllegalArgumentException.class).hasMessage("O clube Sem precisa de um treinador.");
		Clube clube = repositorio.adicionarClube(pais, new Clube(0, "Com", 20), "Rui");
		assertThatThrownBy(() -> repositorio.trocarTreinador(clube.id(), null))
			.isInstanceOf(IllegalArgumentException.class).hasMessage("O clube Com precisa de um treinador.");
		assertThatThrownBy(() -> repositorio.trocarTreinador(clube.id(), "x".repeat(61)))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("60 caracteres");
	}

	@Test
	void clubesDoPaisVoltamEmOrdemComIdsEEscudo() {
		long pais = repositorio.criarPais(repositorio.criarMolde("Padrão"), "BRA", "Brasil");
		Clube listrado = repositorio.adicionarClube(pais,
				new Clube(0, "Listrado", 40, new Escudo(ModeloEscudo.LISTRAS_VERTICAIS, "#111827", "#f4f1e6")), "Ana Ribeiro");
		Clube liso = repositorio.adicionarClube(pais, new Clube(0, "Liso", 12), "Treinador Liso");

		assertThat(repositorio.carregarClubes(pais)).containsExactly(listrado, liso);
		assertThat(repositorio.treinadores(pais)).containsExactly(entry(listrado.id(), "Ana Ribeiro"),
				entry(liso.id(), "Treinador Liso"));
		repositorio.trocarTreinador(liso.id(), "  Caio Nunes ");
		assertThat(repositorio.treinadores(pais)).containsEntry(liso.id(), "Caio Nunes");
		assertThat(listrado.escudo().corSecundaria()).isEqualTo("#F4F1E6");
	}

	@Test
	void excluirDivisaoDoMeioSobeAsDeBaixo() {
		long pais = repositorio.criarPais(repositorio.criarMolde("Padrão"), "BRA", "Brasil");
		repositorio.criarLiga(pais, "Brasileiro");
		repositorio.adicionarDivisao(pais, "Série A", 20, 50, 4);
		repositorio.adicionarDivisao(pais, "Série B", 20, 30, 4);
		repositorio.adicionarDivisao(pais, "Série C", 20, 20, 4);

		repositorio.excluirDivisao(pais, 2);

		assertThat(repositorio.carregarFormato(pais).divisoes()).containsExactly(
				new FormatoDivisao("Série A", 1, 20, 50, 4), new FormatoDivisao("Série C", 2, 20, 20, 4));
		assertThat(repositorio.adicionarDivisao(pais, "Série D", 20, 15, 4)).isEqualTo(3);
		assertThatThrownBy(() -> repositorio.excluirDivisao(pais, 9)).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void campeonatoSoFicaDisponivelComLigaValidaEClubesSuficientes() {
		long molde = repositorio.criarMolde("Padrão");
		long inglaterra = repositorio.criarPais(molde, "ENG", "Inglaterra");
		repositorio.criarLiga(inglaterra, "Premier League");
		repositorio.adicionarDivisao(inglaterra, "Premier", 18, 50, 3);
		clubes(inglaterra, 21);
		long italia = repositorio.criarPais(molde, "ITA", "Itália");
		clubes(italia, 30);
		long espanha = repositorio.criarPais(molde, "ESP", "Espanha");
		repositorio.criarLiga(espanha, "La Liga");
		repositorio.adicionarDivisao(espanha, "Primera", 20, 50, 3);
		clubes(espanha, 22);
		long franca = repositorio.criarPais(molde, "FRA", "França");
		repositorio.criarLiga(franca, "Ligue 1");
		repositorio.adicionarDivisao(franca, "Ligue 1", 12, 50, 2);
		clubes(franca, 30);

		assertThat(repositorio.campeonatosDisponiveis(molde))
			.containsExactly(new MoldeRepositorio.CampeonatoDisponivel(inglaterra, "ENG", "Inglaterra", "Premier League"));
		assertThat(ValidadorLiga.problemas(repositorio.carregarFormato(espanha), 22)).singleElement()
			.asString().contains("Faltam 1 clube(s)");
	}

	@Test
	void divisaoSoEntraEmPaisComLiga() {
		long pais = repositorio.criarPais(repositorio.criarMolde("Padrão"), "BRA", "Brasil");
		assertThatThrownBy(() -> repositorio.adicionarDivisao(pais, "Série A", 20, 50, 4))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void paisNaoPodeGanharSegundaLiga() {
		long pais = repositorio.criarPais(repositorio.criarMolde("Padrão"), "BRA", "Brasil");
		repositorio.criarLiga(pais, "Brasileiro");

		assertThatThrownBy(() -> repositorio.criarLiga(pais, "Outro")).isInstanceOf(IllegalStateException.class);
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
	void apagarMoldeApagaPaisesDivisoesEClubes() {
		long molde = repositorio.criarMolde("Temporário");
		long pais = repositorio.criarPais(molde, "BRA", "Brasil");
		repositorio.criarLiga(pais, "Brasileiro");
		repositorio.adicionarDivisao(pais, "Série A", 20, 50, 4);
		clubes(pais, 3);

		jdbc.sql("DELETE FROM molde WHERE id = ?").param(molde).update();

		assertThat(contar("SELECT count(*) FROM molde_pais WHERE id = ?", pais)).isZero();
		assertThat(contar("SELECT count(*) FROM molde_divisao WHERE pais_id = ?", pais)).isZero();
		assertThat(contar("SELECT count(*) FROM molde_clube WHERE pais_id = ?", pais)).isZero();
	}

	@Test
	void paisInexistenteERecusadoAoCarregar() {
		assertThatThrownBy(() -> repositorio.carregarFormato(-1)).isInstanceOf(NoSuchElementException.class);
	}

	private long contar(String sql, long id) {
		return jdbc.sql(sql).param(id).query(Long.class).single();
	}

	private void clubes(long pais, int quantidade) {
		IntStream.rangeClosed(1, quantidade).forEach(i -> repositorio.adicionarClube(pais, new Clube(0, "Clube " + i, 20), "Treinador " + i));
	}
}
