package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.motor.ParametrosMotor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Banco editável (SPEC §3.5, rev. 89) contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BancoEditavelIT {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	BancoEditavelRepositorio bancos;

	@Autowired
	BancoPadraoRepositorio bancoPadrao;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	JdbcClient jdbc;

	long usuario;

	@BeforeEach
	void carregarBancoPadrao() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());
		usuario = LigaDeTeste.participante(contas).id();
	}

	@Test
	void bancoPadraoBaixadoVoltaIgual() {
		byte[] arquivo = bancos.baixarBancoPadrao();

		BancoEditavel banco = bancos.ler(arquivo);

		assertThat(banco.paraArquivo()).isEqualTo(arquivo);
		assertThat(banco.comLiga()).extracting(BancoEditavel.Pais::codigo).contains("BRA", "ARG", "ENG", "ESP", "ITA", "POR");
		assertThat(banco.continentais()).hasSize(2);
	}

	@Test
	void jogoDoArquivoTemOsNomesDelEOMoldeTemporarioSome() throws Exception {
		ObjectNode raiz = padrao();
		ObjectNode brasil = pais(raiz, "BRA");
		brasil.put("liga", "Campeonato da Turma");
		ObjectNode clube = (ObjectNode) brasil.path("clubes").get(0);
		clube.put("nome", "Time do Zé");
		((ObjectNode) clube.path("jogadores").get(0)).put("nome", "Zé Goleiro");
		int moldes = jdbc.sql("SELECT count(*) FROM molde").query(Integer.class).single();

		long jogo = bancos.criarJogo(JSON.writeValueAsBytes(raiz), "BRA", usuario).jogoId();

		assertThat(jdbc.sql("SELECT nome_liga FROM jogo WHERE id = :j").param("j", jogo).query(String.class).single())
			.isEqualTo("Campeonato da Turma");
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_clube WHERE jogo_id = :j AND nome = 'Time do Zé'").param("j", jogo)
			.query(Integer.class).single()).isEqualTo(1);
		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id
				WHERE c.jogo_id = :j AND j.nome = 'Zé Goleiro'""").param("j", jogo).query(Integer.class).single()).isEqualTo(1);
		assertThat(jdbc.sql("SELECT count(*) FROM molde").query(Integer.class).single()).isEqualTo(moldes);
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_continental WHERE jogo_id = :j").param("j", jogo).query(Integer.class).single())
			.isEqualTo(1);
	}

	@Test
	void paisSemContinentalViraCampeonatoEJoga() throws Exception {
		ObjectNode raiz = padrao();
		ObjectNode mexico = pais(raiz, "MEX");
		ArrayNode clubes = (ArrayNode) mexico.path("clubes");
		ObjectNode modelo = (ObjectNode) pais(raiz, "BRA").path("clubes").get(0);
		while (clubes.size() < 18) {
			ObjectNode novo = modelo.deepCopy();
			novo.put("nome", "Clube Mexicano " + clubes.size());
			clubes.add(novo);
		}
		mexico.put("liga", "Liga do México");
		ArrayNode divisoes = mexico.putArray("divisoes");
		divisoes.addObject().put("nome", "Liga MX").put("clubes", 18).put("limiteForca", 50).put("rebaixados", 0);

		long jogo = bancos.criarJogo(JSON.writeValueAsBytes(raiz), "MEX", usuario).jogoId();

		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		RodadasDeTeste.jogarRodada(rodadas, jogo, 1);
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_continental WHERE jogo_id = :j").param("j", jogo).query(Integer.class).single())
			.isZero();
		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_partida p JOIN jogo_temporada t ON t.id = p.temporada_id
				WHERE t.jogo_id = :j AND p.gols_mandante IS NOT NULL""").param("j", jogo).query(Integer.class).single()).isEqualTo(9);
	}

	@Test
	void arquivoComErrosMostraTodosComOLugar() throws Exception {
		ObjectNode raiz = padrao();
		ObjectNode brasil = pais(raiz, "BRA");
		ArrayNode clubes = (ArrayNode) brasil.path("clubes");
		ObjectNode primeiro = (ObjectNode) clubes.get(0);
		String nome = primeiro.path("nome").asString();
		((ObjectNode) primeiro.path("jogadores").get(6)).put("nome", " ");
		((ObjectNode) primeiro.path("jogadores").get(1)).put("forca", 70);
		((ObjectNode) primeiro.path("jogadores").get(2)).put("nacionalidade", "XYZ");
		((ObjectNode) primeiro.path("escudo")).put("modelo", "ESTRELA");
		clubes.remove(clubes.size() - 1);
		int jogosAntes = jdbc.sql("SELECT count(*) FROM jogo").query(Integer.class).single();

		assertThatThrownBy(() -> bancos.criarJogo(JSON.writeValueAsBytes(raiz), "BRA", usuario))
			.isInstanceOfSatisfying(BancoRecusadoException.class, e -> assertThat(e.problemas())
				.anySatisfy(p -> assertThat(p).contains("Brasil, clube “" + nome + "”, jogador 7").contains("falta o nome"))
				.anySatisfy(p -> assertThat(p).contains("força 70"))
				.anySatisfy(p -> assertThat(p).contains("XYZ"))
				.anySatisfy(p -> assertThat(p).contains("ESTRELA").contains("LISTRAS_VERTICAIS"))
				.anySatisfy(p -> assertThat(p).startsWith("Brasil:").contains("Faltam 1")));
		assertThat(jdbc.sql("SELECT count(*) FROM jogo").query(Integer.class).single()).isEqualTo(jogosAntes);
	}

	@Test
	void jogoSalvoOuOutroArquivoNaoEhBanco() {
		assertThat(problemas("{\"aplicacao\":\"managerFoot\",\"formato\":1,\"tabelas\":{}}")).singleElement().asString().contains("jogo salvo");
		assertThat(problemas("isto não é json")).singleElement().asString().contains("não é um banco");
		assertThat(problemas("{\"aplicacao\":\"managerFoot\",\"tipo\":\"banco\",\"formato\":2,\"paises\":[]}")).singleElement().asString()
			.contains("versão mais nova");
	}

	private List<String> problemas(String arquivo) {
		try {
			bancos.ler(arquivo.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}
		catch (BancoRecusadoException e) {
			return e.problemas();
		}
		throw new AssertionError("o arquivo deveria ter sido recusado");
	}

	private ObjectNode padrao() throws Exception {
		return (ObjectNode) JSON.readTree(bancos.baixarBancoPadrao());
	}

	private static ObjectNode pais(ObjectNode raiz, String codigo) {
		for (JsonNode pais : raiz.path("paises")) {
			if (pais.path("codigo").asString().equals(codigo)) {
				return (ObjectNode) pais;
			}
		}
		throw new IllegalArgumentException(codigo);
	}
}
