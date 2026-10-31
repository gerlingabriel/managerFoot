package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;
import br.com.managerfoot.persistencia.TabelasDoSave.Tabela;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Arquivo de save (SPEC §3.5) contra PostgreSQL real: ida e volta, recusas e o apagar. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SaveRepositorioIT {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	SaveRepositorio saves;

	@Autowired
	BancoPadraoRepositorio bancoPadrao;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	RodadaAoVivoRepositorio aoVivo;

	@Autowired
	FimDeTemporadaRepositorio fins;

	@Autowired
	JdbcClient jdbc;

	long usuario;
	long outro;
	long jogo;

	@BeforeEach
	void criarJogo() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());
		CampeonatoDisponivel brasil = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow();
		usuario = LigaDeTeste.participante(contas).id();
		outro = LigaDeTeste.participante(contas).id();
		jogo = jogos.criarJogoComClubeSorteado(brasil.paisId(), usuario, 5);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
	}

	@Test
	void listaDoSaveCobreOEsquema() {
		List<String> doBanco = jdbc.sql("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name LIKE 'jogo%'""")
			.query(String.class).list();
		Set<String> listadas = new HashSet<>(TabelasDoSave.FORA_DO_SAVE);
		TabelasDoSave.TODAS.forEach(t -> listadas.add(t.nome()));
		assertThat(listadas).containsExactlyInAnyOrderElementsOf(doBanco);

		List<String[]> chaves = jdbc.sql("""
				SELECT c.conrelid::regclass::text, a.attname, c.confrelid::regclass::text
				FROM pg_constraint c CROSS JOIN LATERAL unnest(c.conkey) k
				JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k
				WHERE c.contype = 'f' AND c.conrelid::regclass::text LIKE 'jogo%'""")
			.query((rs, n) -> new String[] { rs.getString(1), rs.getString(2), rs.getString(3) }).list();
		List<String> gravadas = new ArrayList<>();
		for (Tabela tabela : TabelasDoSave.TODAS) {
			gravadas.add(tabela.nome());
			tabela.referencias().values().forEach(destino -> assertThat(gravadas)
				.as("%s só pode referenciar tabelas anteriores", tabela.nome()).contains(destino));
		}
		for (String[] chave : chaves) {
			Tabela tabela = TabelasDoSave.buscar(chave[0]);
			if (tabela == null || chave[2].equals("pais_fifa") || chave[2].equals("molde")) {
				continue;
			}
			if (chave[2].equals("usuario")) {
				assertThat(tabela.usuarios()).as("%s.%s", chave[0], chave[1]).contains(chave[1]);
			} else {
				assertThat(tabela.referencias()).as("%s.%s", chave[0], chave[1]).containsKey(chave[1]);
			}
		}
		List<String[]> ids = jdbc.sql("""
				SELECT table_name, column_name FROM information_schema.columns
				WHERE table_schema = current_schema() AND table_name LIKE 'jogo%' AND column_name LIKE '%\\_id' ESCAPE '\\'""")
			.query((rs, n) -> new String[] { rs.getString(1), rs.getString(2) }).list();
		for (String[] id : ids) {
			Tabela tabela = TabelasDoSave.buscar(id[0]);
			if (tabela != null) {
				assertThat(tabela.referencias().containsKey(id[1]) || tabela.usuarios().contains(id[1]) || tabela.omitidas().contains(id[1]))
					.as("%s.%s precisa ter o id trocado ao enviar", id[0], id[1]).isTrue();
			}
		}
	}

	@Test
	void temporadaInteiraVoltaIgualEContinuaIgual() throws Exception {
		jogarATemporada();
		byte[] arquivo = saves.exportar(jogo, usuario).orElseThrow().conteudo();
		for (String cheia : List.of("jogo_partida", "jogo_evento", "jogo_lancamento", "jogo_campeao", "jogo_campeao_copa",
				"jogo_campeao_continental", "jogo_campeao_mundial", "jogo_copa_confronto", "jogo_continental_grupo")) {
			assertThat(JSON.readTree(arquivo).path("tabelas").path(cheia).path("linhas").size()).as(cheia).isPositive();
		}

		long copia = saves.importar(new ByteArrayInputStream(arquivo), usuario);

		assertThat(copia).isNotEqualTo(jogo);
		assertThat(normalizado(saves.exportar(copia, usuario).orElseThrow().conteudo())).isEqualTo(normalizado(arquivo));

		assertThat(placaresDaRodada(copia)).isEqualTo(placaresDaRodada(jogo));
		assertThat(placaresDaRodada(copia)).isNotEmpty();
	}

	@Test
	void arquivoEnviadoPorOutroTreinadorViraJogoDeleEOriginalNaoMuda() {
		byte[] arquivo = saves.exportar(jogo, usuario).orElseThrow().conteudo();

		long copia = saves.importar(new ByteArrayInputStream(arquivo), outro);

		assertThat(jdbc.sql("SELECT dono_id FROM jogo WHERE id = :j").param("j", copia).query(Long.class).single()).isEqualTo(outro);
		assertThat(jdbc.sql("SELECT molde_id FROM jogo WHERE id = :j").param("j", copia).query(Long.class).optional()).isEmpty();
		assertThat(jogos.clubeDoTreinador(copia, outro)).isPresent();
		assertThat(jogos.clubeDoTreinador(jogo, usuario)).isPresent();
		assertThat(jogos.clubeDoTreinador(jogo, outro)).isEmpty();
	}

	@Test
	void soOTreinadorBaixaEApaga() {
		assertThat(saves.exportar(jogo, outro)).isEmpty();
		assertThat(saves.apagar(jogo, outro)).isFalse();
		assertThat(saves.exportar(-1, usuario)).isEmpty();
		assertThat(jogos.clubeDoTreinador(jogo, usuario)).isPresent();
	}

	@Test
	void rodadaAoVivoNaoBaixaEmAndamento() {
		RodadasDeTeste.jogarCopaPendente(rodadas, jogo);
		aoVivo.iniciar(jogo, usuario, 1, Instant.parse("2026-10-28T20:00:00Z"));

		assertThatThrownBy(() -> saves.exportar(jogo, usuario)).isInstanceOf(SaveRecusadoException.class)
			.hasMessageContaining("rodada");
	}

	@Test
	void apagarTiraOJogoInteiroENaoMexeNoOutro() {
		jogarATemporada();
		long copia = saves.importar(new ByteArrayInputStream(saves.exportar(jogo, usuario).orElseThrow().conteudo()), usuario);
		int partidas = contar(jogo, "jogo_partida");

		assertThat(saves.apagar(copia, usuario)).isTrue();

		assertThat(jogos.clubeDoTreinador(copia, usuario)).isEmpty();
		assertThat(saves.exportar(copia, usuario)).isEmpty();
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_clube WHERE jogo_id = :j").param("j", copia).query(Integer.class).single()).isZero();
		assertThat(contar(jogo, "jogo_partida")).isEqualTo(partidas).isPositive();
	}

	@Test
	void arquivoQueNaoEDoJogoEhRecusado() {
		assertThat(recusa("isto nao e json".getBytes(StandardCharsets.UTF_8))).contains("não é um jogo salvo");
		assertThat(recusa("{\"aplicacao\":\"outro\",\"formato\":1,\"tabelas\":{}}".getBytes(StandardCharsets.UTF_8)))
			.contains("não é um jogo salvo");
		assertThat(recusa("{\"aplicacao\":\"managerFoot\",\"formato\":1,\"tabelas\":{}}".getBytes(StandardCharsets.UTF_8)))
			.contains("exatamente um jogo");
	}

	@Test
	void arquivoAlteradoSemCoerenciaEhRecusadoESemGravarNada() throws Exception {
		int antes = jdbc.sql("SELECT count(*) FROM jogo").query(Integer.class).single();

		ObjectNode versao = original();
		versao.put("formato", SaveRepositorio.FORMATO + 1);
		assertThat(recusa(versao)).contains("versão mais nova");

		ObjectNode tabelaNova = original();
		((ObjectNode) tabelaNova.path("tabelas")).set("usuario", JSON.createObjectNode());
		assertThat(recusa(tabelaNova)).contains("usuario");

		ObjectNode aoVivoNoArquivo = original();
		((ObjectNode) aoVivoNoArquivo.path("tabelas")).set("jogo_rodada_ao_vivo", JSON.createObjectNode());
		assertThat(recusa(aoVivoNoArquivo)).contains("jogo_rodada_ao_vivo");

		ObjectNode colunaNova = original();
		((ArrayNode) colunaNova.path("tabelas").path("jogo_clube").path("colunas")).set(1, JSON.getNodeFactory().stringNode("senha; DROP TABLE jogo"));
		assertThat(recusa(colunaNova)).contains("coluna");

		ObjectNode semColuna = original();
		((ArrayNode) semColuna.path("tabelas").path("jogo_jogador").path("colunas")).remove(0);
		assertThat(recusa(semColuna)).contains("jogo_jogador");

		// jogador apontando para o clube de um jogo que já está no banco: o id precisa estar no próprio arquivo
		long clubeAlheio = jogos.clubeDoTreinador(jogo, usuario).orElseThrow() + 1_000_000;
		ObjectNode alheio = original();
		trocar(alheio, "jogo_jogador", "clube_id", JSON.getNodeFactory().numberNode(clubeAlheio));
		assertThat(recusa(alheio)).contains("não está no arquivo");

		ObjectNode forca = original();
		trocar(forca, "jogo_jogador", "forca", JSON.getNodeFactory().numberNode(99));
		assertThat(recusa(forca)).contains("não aceita");

		ObjectNode tipo = original();
		trocar(tipo, "jogo_jogador", "forca", JSON.getNodeFactory().stringNode("10; DROP TABLE jogo"));
		assertThat(recusa(tipo)).contains("forca");

		ObjectNode idRepetido = original();
		ArrayNode clubes = (ArrayNode) idRepetido.path("tabelas").path("jogo_clube").path("linhas");
		clubes.add(clubes.get(0).deepCopy());
		assertThat(recusa(idRepetido)).contains("repetido");

		ObjectNode doisTreinadores = original();
		ArrayNode treinadores = (ArrayNode) doisTreinadores.path("tabelas").path("jogo_treinador").path("linhas");
		treinadores.add(treinadores.get(0).deepCopy());
		assertThat(recusa(doisTreinadores)).contains("exatamente um treinador");

		assertThat(jdbc.sql("SELECT count(*) FROM jogo").query(Integer.class).single()).isEqualTo(antes);
	}

	@Test
	void arquivoAlteradoQuePassaNaValidacaoEhAceito() throws Exception {
		ObjectNode alterado = original();
		trocar(alterado, "jogo_clube", "nome", JSON.getNodeFactory().stringNode("Clube Renomeado"));

		long copia = saves.importar(new ByteArrayInputStream(JSON.writeValueAsBytes(alterado)), usuario);

		assertThat(jdbc.sql("SELECT count(*) FROM jogo_clube WHERE jogo_id = :j AND nome = 'Clube Renomeado'")
			.param("j", copia).query(Integer.class).single()).isEqualTo(1);
	}

	private ObjectNode original() throws Exception {
		return (ObjectNode) JSON.readTree(saves.exportar(jogo, usuario).orElseThrow().conteudo());
	}

	/** Troca o valor da coluna na primeira linha da parte. */
	private static void trocar(ObjectNode raiz, String tabela, String coluna, JsonNode valor) {
		JsonNode nomes = raiz.path("tabelas").path(tabela).path("colunas");
		for (int i = 0; i < nomes.size(); i++) {
			if (nomes.get(i).asString().equals(coluna)) {
				((ArrayNode) raiz.path("tabelas").path(tabela).path("linhas").get(0)).set(i, valor);
				return;
			}
		}
		throw new IllegalArgumentException(coluna);
	}

	private String recusa(ObjectNode raiz) {
		return recusa(JSON.writeValueAsBytes(raiz));
	}

	private String recusa(byte[] arquivo) {
		try {
			saves.importar(new ByteArrayInputStream(arquivo), usuario);
		}
		catch (SaveRecusadoException e) {
			return e.getMessage();
		}
		throw new AssertionError("o arquivo deveria ter sido recusado");
	}

	private int contar(long jogoId, String tabela) {
		return jdbc.sql("SELECT count(*) FROM " + tabela + " x WHERE x." + TabelasDoSave.buscar(tabela).filtro().replace(":jogo", String.valueOf(jogoId)))
			.query(Integer.class).single();
	}

	private void jogarATemporada() {
		while (true) {
			RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
			switch (rodadas.proximo(jogo)) {
				case RodadaRepositorio.Proximo.Liga(int r) -> rodadas.avancar(jogo, r);
				case RodadaRepositorio.Proximo.EtapaCopa e -> rodadas.avancarCopa(jogo, e.indice(), List.of());
				case RodadaRepositorio.Proximo.EtapaContinental e -> rodadas.avancarContinental(jogo, e.indice(), List.of());
				case RodadaRepositorio.Proximo.EtapaMundial e -> rodadas.avancarMundial(jogo, e.indice(), List.of());
				default -> {
					RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
					fins.avancarTemporada(jogo, 1, 99);
					RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
					return;
				}
			}
		}
	}

	/** Joga a próxima rodada da liga e devolve os placares pelos nomes, que não mudam com os ids. */
	private Map<String, String> placaresDaRodada(long jogoId) {
		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogoId);
		RodadasDeTeste.jogarCopaPendente(rodadas, jogoId);
		if (!(rodadas.proximo(jogoId) instanceof RodadaRepositorio.Proximo.Liga(int rodada))) {
			throw new IllegalStateException("esperava uma rodada da liga");
		}
		rodadas.avancar(jogoId, rodada);
		return new TreeMap<>(jdbc.sql("""
				SELECT m.nome || ' x ' || v.nome AS jogo, p.gols_mandante || '-' || p.gols_visitante AS placar
				FROM jogo_partida p
				JOIN jogo_clube m ON m.id = p.mandante_id
				JOIN jogo_clube v ON v.id = p.visitante_id
				JOIN jogo_temporada t ON t.id = p.temporada_id
				WHERE t.jogo_id = :jogo AND t.numero = 2 AND p.divisao_id IS NOT NULL AND p.gols_mandante IS NOT NULL""")
			.param("jogo", jogoId)
			.query((rs, n) -> Map.entry(rs.getString("jogo"), rs.getString("placar")))
			.list().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
	}

	/**
	 * O conteúdo do arquivo sem o que muda de um jogo para a cópia: cada id vira a posição dele na tabela de origem e o
	 * último acesso sai. Dois arquivos iguais aqui guardam o mesmo jogo.
	 */
	private static Map<String, List<List<String>>> normalizado(byte[] arquivo) throws Exception {
		JsonNode tabelas = JSON.readTree(arquivo).path("tabelas");
		Map<String, Map<Long, Integer>> posicoes = new HashMap<>();
		for (Tabela t : TabelasDoSave.TODAS) {
			if (t.chaveGerada() == null) {
				continue;
			}
			int indice = indice(tabelas.path(t.nome()), t.chaveGerada());
			Map<Long, Integer> posicao = new HashMap<>();
			for (JsonNode linha : tabelas.path(t.nome()).path("linhas")) {
				posicao.put(linha.get(indice).longValue(), posicao.size());
			}
			posicoes.put(t.nome(), posicao);
		}
		Map<String, List<List<String>>> resultado = new TreeMap<>();
		for (Tabela t : TabelasDoSave.TODAS) {
			JsonNode nomes = tabelas.path(t.nome()).path("colunas");
			List<List<String>> linhas = new ArrayList<>();
			for (JsonNode linha : tabelas.path(t.nome()).path("linhas")) {
				List<String> valores = new ArrayList<>();
				for (int i = 0; i < nomes.size(); i++) {
					String coluna = nomes.get(i).asString();
					JsonNode valor = linha.get(i);
					if (coluna.equals("ultimo_acesso_em") || coluna.equals("criado_em") || t.usuarios().contains(coluna)) {
						continue;
					}
					String destino = coluna.equals(t.chaveGerada()) ? t.nome() : t.referencias().get(coluna);
					valores.add(coluna + "=" + (destino != null && !valor.isNull() ? "#" + posicoes.get(destino).get(valor.longValue()) : valor.toString()));
				}
				linhas.add(valores);
			}
			resultado.put(t.nome(), linhas);
		}
		return resultado;
	}

	private static int indice(JsonNode tabela, String coluna) {
		for (int i = 0; i < tabela.path("colunas").size(); i++) {
			if (tabela.path("colunas").get(i).asString().equals(coluna)) {
				return i;
			}
		}
		throw new IllegalArgumentException(coluna);
	}
}
