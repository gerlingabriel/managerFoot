package br.com.managerfoot.persistencia;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio.JogoGravado;
import br.com.managerfoot.persistencia.TabelasDoSave.Tabela;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Arquivo de save (SPEC §3.5): baixar o jogo em JSON, enviar de volta e apagar do servidor. Enviar sempre cria um jogo
 * novo, do treinador que enviou, com ids novos; o arquivo nunca aponta para dados que já estão no banco. Um arquivo
 * alterado à mão é aceito se passar na validação (Provisório, §3.5).
 */
@Repository
public class SaveRepositorio {

	/** Versão do formato do arquivo; ao mudar o formato, converter aqui os arquivos das versões anteriores. */
	static final int FORMATO = 1;

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final JdbcClient jdbc;
	private final JdbcTemplate template;
	private final NamedParameterJdbcTemplate nomeado;
	private final JogosDoTreinadorRepositorio jogosDoTreinador;
	private final JogoRepositorio jogos;
	private final RodadaRepositorio rodadas;

	public SaveRepositorio(JdbcClient jdbc, JdbcTemplate template, NamedParameterJdbcTemplate nomeado,
			JogosDoTreinadorRepositorio jogosDoTreinador, JogoRepositorio jogos, RodadaRepositorio rodadas) {
		this.jdbc = jdbc;
		this.template = template;
		this.nomeado = nomeado;
		this.jogosDoTreinador = jogosDoTreinador;
		this.jogos = jogos;
		this.rodadas = rodadas;
	}

	/**
	 * O arquivo do jogo, numa leitura só, para não misturar duas rodadas.
	 *
	 * @return vazio se o jogo não existe ou se o usuário não é treinador nele
	 * @throws SaveRecusadoException com a rodada ao vivo em andamento
	 */
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public Optional<ArquivoDeSave> exportar(long jogoId, long usuarioId) {
		Optional<JogoGravado> jogo = jogosDoTreinador.listar(usuarioId).stream().filter(j -> j.jogoId() == jogoId).findFirst();
		if (jogo.isEmpty()) {
			return Optional.empty();
		}
		boolean aoVivo = jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_rodada_ao_vivo WHERE jogo_id = :jogo)")
			.param("jogo", jogoId).query(Boolean.class).single();
		if (aoVivo) {
			throw new SaveRecusadoException("Termine a rodada que está em andamento antes de baixar o jogo.");
		}
		Map<String, Object> tabelas = new LinkedHashMap<>();
		for (Tabela tabela : TabelasDoSave.TODAS) {
			tabelas.put(tabela.nome(), linhas(tabela, jogoId));
		}
		JogoGravado j = jogo.get();
		Map<String, Object> resumo = new LinkedHashMap<>();
		resumo.put("clube", j.clube().nome());
		resumo.put("pais", j.pais());
		resumo.put("temporada", j.ano());
		resumo.put("rodada", j.rodada());
		Map<String, Object> raiz = new LinkedHashMap<>();
		raiz.put("aplicacao", "managerFoot");
		raiz.put("formato", FORMATO);
		raiz.put("baixadoEm", Instant.now().toString());
		raiz.put("resumo", resumo);
		raiz.put("tabelas", tabelas);
		return Optional.of(new ArquivoDeSave(j.clube().nome(), j.ano(), j.rodada(), JSON.writeValueAsBytes(raiz)));
	}

	private Map<String, Object> linhas(Tabela tabela, long jogoId) {
		List<Coluna> colunas = colunas(tabela);
		String sql = "SELECT " + colunas.stream().map(Coluna::nome).collect(Collectors.joining(", ")) + " FROM " + tabela.nome()
				+ " WHERE " + tabela.filtro() + " ORDER BY " + String.join(", ", tabela.ordem());
		List<List<Object>> linhas = nomeado.query(sql, Map.of("jogo", jogoId), (rs, n) -> {
			List<Object> linha = new ArrayList<>(colunas.size());
			for (int i = 0; i < colunas.size(); i++) {
				linha.add(valor(rs, i + 1, colunas.get(i)));
			}
			return linha;
		});
		Map<String, Object> conteudo = new LinkedHashMap<>();
		conteudo.put("colunas", colunas.stream().map(Coluna::nome).toList());
		conteudo.put("linhas", linhas);
		return conteudo;
	}

	private static Object valor(ResultSet rs, int indice, Coluna coluna) throws SQLException {
		if (coluna.tipo().startsWith("timestamp")) {
			OffsetDateTime momento = rs.getObject(indice, OffsetDateTime.class);
			return momento == null ? null : momento.toString();
		}
		return coluna.textual() ? rs.getString(indice) : rs.getObject(indice);
	}

	/** As colunas da tabela no banco, menos as que não vão para o arquivo. */
	private List<Coluna> colunas(Tabela tabela) {
		return jdbc.sql("""
				SELECT column_name, data_type, is_nullable = 'YES' AS opcional,
				       column_default IS NOT NULL OR is_identity = 'YES' AS tem_padrao
				FROM information_schema.columns
				WHERE table_schema = current_schema() AND table_name = :tabela
				ORDER BY ordinal_position""")
			.param("tabela", tabela.nome())
			.query((rs, n) -> new Coluna(rs.getString("column_name"), rs.getString("data_type"), rs.getBoolean("opcional"),
					rs.getBoolean("tem_padrao")))
			.list().stream().filter(c -> !tabela.omitidas().contains(c.nome())).toList();
	}

	/**
	 * Cria um jogo do usuário a partir do arquivo. Recusado, nada fica gravado.
	 *
	 * @return o id do jogo novo
	 * @throws SaveRecusadoException se o arquivo não passa na validação
	 */
	@Transactional
	public long importar(InputStream arquivo, long usuarioId) {
		JsonNode tabelas = validarCabecalho(ler(arquivo));
		long usuarioNoArquivo = usuarioNoArquivo(tabelas);
		tabelas.propertyNames().forEach(nome -> {
			if (TabelasDoSave.buscar(nome) == null) {
				throw new SaveRecusadoException("O arquivo tem dados que este jogo não conhece (" + nome + ").");
			}
		});
		Map<String, Map<Long, Long>> idsNovos = new HashMap<>();
		for (Tabela tabela : TabelasDoSave.TODAS) {
			if (tabelas.has(tabela.nome())) {
				gravar(tabela, tabelas.get(tabela.nome()), idsNovos, usuarioNoArquivo, usuarioId);
			}
		}
		long jogoId = idsNovos.get("jogo").values().iterator().next();
		jdbc.sql("UPDATE jogo SET dono_id = :usuario WHERE id = :jogo").param("usuario", usuarioId).param("jogo", jogoId).update();
		jdbc.sql("UPDATE jogo_treinador SET ultimo_acesso_em = now() WHERE jogo_id = :jogo").param("jogo", jogoId).update();
		conferirQueOJogoAbre(jogoId, usuarioId);
		return jogoId;
	}

	private static JsonNode ler(InputStream arquivo) {
		try (arquivo) {
			return JSON.readTree(arquivo);
		}
		catch (JacksonException | IOException e) {
			throw new SaveRecusadoException("Este arquivo não é um jogo salvo do managerFoot.");
		}
	}

	private static JsonNode validarCabecalho(JsonNode raiz) {
		if (raiz == null || !raiz.isObject() || !"managerFoot".equals(raiz.path("aplicacao").asString(""))
				|| !raiz.path("formato").isIntegralNumber() || !raiz.path("tabelas").isObject()) {
			throw new SaveRecusadoException("Este arquivo não é um jogo salvo do managerFoot.");
		}
		int formato = raiz.path("formato").asInt();
		if (formato > FORMATO) {
			throw new SaveRecusadoException("Este arquivo veio de uma versão mais nova do jogo (formato " + formato + ").");
		}
		if (formato < 1) {
			throw new SaveRecusadoException("Este arquivo tem um formato desconhecido (" + formato + ").");
		}
		return raiz.path("tabelas");
	}

	/** O treinador do arquivo: um só, porque o save existe só para o jogo individual (§3.5). */
	private long usuarioNoArquivo(JsonNode tabelas) {
		JsonNode treinador = tabelas.path("jogo_treinador");
		JsonNode linhas = treinador.path("linhas");
		if (tabelas.path("jogo").path("linhas").size() != 1) {
			throw new SaveRecusadoException("O arquivo precisa ter exatamente um jogo.");
		}
		if (linhas.size() != 1) {
			throw new SaveRecusadoException("O arquivo precisa ter exatamente um treinador.");
		}
		int indice = -1;
		for (int i = 0; i < treinador.path("colunas").size(); i++) {
			if ("usuario_id".equals(treinador.path("colunas").get(i).asString(""))) {
				indice = i;
			}
		}
		JsonNode usuario = indice < 0 ? null : linhas.get(0).get(indice);
		if (usuario == null || !usuario.isIntegralNumber() || !usuario.canConvertToLong()) {
			throw new SaveRecusadoException("O treinador do arquivo não está completo.");
		}
		return usuario.longValue();
	}

	private void gravar(Tabela tabela, JsonNode conteudo, Map<String, Map<Long, Long>> idsNovos, long usuarioNoArquivo, long usuarioId) {
		List<Coluna> colunas = colunasDoArquivo(tabela, conteudo.path("colunas"));
		JsonNode linhas = conteudo.path("linhas");
		if (!linhas.isArray()) {
			throw new SaveRecusadoException("A parte " + tabela.nome() + " do arquivo não está completa.");
		}
		Map<Long, Long> novos = reservarIds(tabela, colunas, linhas);
		idsNovos.put(tabela.nome(), novos);
		List<Object[]> valores = new ArrayList<>(linhas.size());
		for (JsonNode linha : linhas) {
			if (!linha.isArray() || linha.size() != colunas.size()) {
				throw new SaveRecusadoException("Há uma linha incompleta na parte " + tabela.nome() + " do arquivo.");
			}
			Object[] valor = new Object[colunas.size()];
			for (int i = 0; i < colunas.size(); i++) {
				valor[i] = converter(tabela, colunas.get(i), linha.get(i), idsNovos, usuarioNoArquivo, usuarioId);
			}
			valores.add(valor);
		}
		if (valores.isEmpty()) {
			return;
		}
		String sql = "INSERT INTO " + tabela.nome() + " (" + colunas.stream().map(Coluna::nome).collect(Collectors.joining(", ")) + ")"
				+ (tabela.chaveGerada() == null ? "" : " OVERRIDING SYSTEM VALUE")
				+ " VALUES (" + colunas.stream().map(c -> "?::" + c.tipo()).collect(Collectors.joining(", ")) + ")";
		try {
			template.batchUpdate(sql, valores);
		}
		catch (DataAccessException e) {
			throw new SaveRecusadoException("O arquivo tem valores que o jogo não aceita na parte " + tabela.nome() + ".");
		}
	}

	/** Só nomes de coluna que existem no banco chegam ao SQL; coluna obrigatória que falta recusa o arquivo. */
	private List<Coluna> colunasDoArquivo(Tabela tabela, JsonNode nomes) {
		Map<String, Coluna> doBanco = new LinkedHashMap<>();
		colunas(tabela).forEach(c -> doBanco.put(c.nome(), c));
		List<Coluna> colunas = new ArrayList<>();
		for (JsonNode nome : nomes) {
			Coluna coluna = doBanco.remove(nome.asString(""));
			if (coluna == null) {
				throw new SaveRecusadoException("O arquivo tem uma coluna que este jogo não conhece ou repetida na parte " + tabela.nome() + ".");
			}
			colunas.add(coluna);
		}
		doBanco.values().stream().filter(c -> !c.opcional() && !c.temPadrao()).findFirst().ifPresent(c -> {
			throw new SaveRecusadoException("Falta " + c.nome() + " na parte " + tabela.nome() + " do arquivo.");
		});
		if (tabela.chaveGerada() != null && colunas.stream().noneMatch(c -> c.nome().equals(tabela.chaveGerada()))) {
			throw new SaveRecusadoException("Falta " + tabela.chaveGerada() + " na parte " + tabela.nome() + " do arquivo.");
		}
		return colunas;
	}

	/** Tira da sequência um id novo para cada id do arquivo, na mesma ordem, e assim as linhas não mudam de lugar. */
	private Map<Long, Long> reservarIds(Tabela tabela, List<Coluna> colunas, JsonNode linhas) {
		Map<Long, Long> novos = new HashMap<>();
		if (tabela.chaveGerada() == null || linhas.isEmpty()) {
			return novos;
		}
		int indice = 0;
		while (!colunas.get(indice).nome().equals(tabela.chaveGerada())) {
			indice++;
		}
		List<Long> sequencia = jdbc.sql("""
				SELECT nextval(pg_get_serial_sequence(:tabela, :chave)) AS id FROM generate_series(1, :quantos) ORDER BY id""")
			.param("tabela", tabela.nome())
			.param("chave", tabela.chaveGerada())
			.param("quantos", linhas.size())
			.query(Long.class).list();
		int proximo = 0;
		for (JsonNode linha : linhas) {
			JsonNode id = linha.get(indice);
			if (id == null || !id.isIntegralNumber() || !id.canConvertToLong() || novos.putIfAbsent(id.longValue(), sequencia.get(proximo++)) != null) {
				throw new SaveRecusadoException("Há um id inválido ou repetido na parte " + tabela.nome() + " do arquivo.");
			}
		}
		return novos;
	}

	/** O valor como texto, que o SQL converte para o tipo da coluna; ids e contas saem trocados pelos novos. */
	private static String converter(Tabela tabela, Coluna coluna, JsonNode valor, Map<String, Map<Long, Long>> idsNovos,
			long usuarioNoArquivo, long usuarioId) {
		String nome = coluna.nome();
		if (valor == null || valor.isNull()) {
			return null;
		}
		if (!coluna.aceita(valor)) {
			throw new SaveRecusadoException("Valor inválido em " + nome + " na parte " + tabela.nome() + " do arquivo.");
		}
		if (nome.equals(tabela.chaveGerada())) {
			return String.valueOf(idsNovos.get(tabela.nome()).get(valor.longValue()));
		}
		if (tabela.referencias().containsKey(nome)) {
			String destino = tabela.referencias().get(nome);
			Long novo = valor.canConvertToLong() ? idsNovos.getOrDefault(destino, Map.of()).get(valor.longValue()) : null;
			if (novo == null) {
				throw new SaveRecusadoException(nome + " da parte " + tabela.nome() + " aponta para um registro que não está no arquivo.");
			}
			return novo.toString();
		}
		if (tabela.usuarios().contains(nome)) {
			if (valor.canConvertToLong() && valor.longValue() == usuarioNoArquivo) {
				return String.valueOf(usuarioId);
			}
			if (coluna.opcional()) {
				return null;
			}
			throw new SaveRecusadoException(nome + " da parte " + tabela.nome() + " aponta para outro treinador.");
		}
		return valor.asString();
	}

	private void conferirQueOJogoAbre(long jogoId, long usuarioId) {
		try {
			if (jogos.clubeDoTreinador(jogoId, usuarioId).isEmpty()) {
				throw new SaveRecusadoException("O treinador do arquivo não tem clube.");
			}
			jogos.carregarLiga(jogoId);
			rodadas.proximo(jogoId);
		}
		catch (SaveRecusadoException e) {
			throw e;
		}
		catch (IllegalArgumentException | IllegalStateException | NoSuchElementException | DataAccessException e) {
			throw new SaveRecusadoException("O jogo deste arquivo não pode ser aberto: faltam dados ou há dados incoerentes.");
		}
	}

	/**
	 * Apaga o jogo do servidor, com tudo o que é dele. Não dá para desfazer.
	 *
	 * @return falso se o jogo não existe ou se o usuário não é treinador nele
	 */
	@Transactional
	public boolean apagar(long jogoId, long usuarioId) {
		Optional<Long> dele = jdbc.sql("""
				SELECT j.id FROM jogo j JOIN jogo_treinador t ON t.jogo_id = j.id
				WHERE j.id = :jogo AND t.usuario_id = :usuario FOR UPDATE OF j""")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.query(Long.class).optional();
		if (dele.isEmpty()) {
			return false;
		}
		jdbc.sql("DELETE FROM jogo WHERE id = :jogo").param("jogo", jogoId).update();
		return true;
	}

	/** O arquivo pronto para baixar, com o que a tela usa para dar nome a ele. */
	public record ArquivoDeSave(String clube, int ano, int rodada, byte[] conteudo) {
	}

	private record Coluna(String nome, String tipo, boolean opcional, boolean temPadrao) {

		boolean textual() {
			return !(tipo.equals("bigint") || tipo.equals("integer") || tipo.equals("smallint") || tipo.equals("boolean")
					|| tipo.equals("double precision"));
		}

		boolean aceita(JsonNode valor) {
			return switch (tipo) {
				case "bigint", "integer", "smallint" -> valor.isIntegralNumber();
				case "double precision" -> valor.isNumber();
				case "boolean" -> valor.isBoolean();
				default -> valor.isString();
			};
		}
	}
}
