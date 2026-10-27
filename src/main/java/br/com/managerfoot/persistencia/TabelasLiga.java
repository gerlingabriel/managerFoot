package br.com.managerfoot.persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escudo;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.ModeloEscudo;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;

/**
 * SQL comum às tabelas de divisões, clubes e jogadores do molde ({@code molde_*}, donas por
 * {@code pais_id}) e do jogo ({@code jogo_*}, donas por {@code jogo_id}), que têm as mesmas colunas.
 * O prefixo e a coluna dona são constantes dos repositórios, nunca entrada de usuário. Não abre
 * transação: quem chama abre. No molde os clubes não têm divisão (rev. 27): {@link #gravar} e
 * {@link #carregar} são só do jogo.
 */
final class TabelasLiga {

	static final TabelasLiga MOLDE = new TabelasLiga("molde", "pais_id");
	static final TabelasLiga JOGO = new TabelasLiga("jogo", "jogo_id");

	private final String prefixo;
	private final String dono;

	private TabelasLiga(String prefixo, String dono) {
		this.prefixo = prefixo;
		this.dono = dono;
	}

	/** Grava divisões e clubes; devolve a liga com os ids gerados, na mesma ordem da recebida. */
	ConfiguracaoLiga gravar(JdbcClient jdbc, long donoId, ConfiguracaoLiga liga) {
		exigirJogo();
		List<Divisao> divisoes = new ArrayList<>();
		for (Divisao d : liga.divisoes()) {
			long divisaoId = inserir(jdbc, """
					INSERT INTO %s_divisao (%s, nome, prioridade, limite_forca, promovidos, rebaixados)
					VALUES (:dono, :nome, :prioridade, :limite, :promovidos, :rebaixados)""".formatted(prefixo, dono),
					Map.of("dono", donoId, "nome", d.nome(), "prioridade", d.prioridade(), "limite", d.limiteForca(),
							"promovidos", d.promovidos(), "rebaixados", d.rebaixados()));
			List<Clube> clubes = d.clubes().stream().map(c -> inserirClube(jdbc, donoId, divisaoId, c)).toList();
			divisoes.add(new Divisao(d.nome(), d.prioridade(), d.limiteForca(), d.promovidos(), d.rebaixados(), clubes));
		}
		List<Clube> fila = liga.filaDeEspera().stream().map(c -> inserirClube(jdbc, donoId, null, c)).toList();
		return new ConfiguracaoLiga(liga.nome(), divisoes, fila);
	}

	/** Divisões por prioridade; clubes em ordem de id dentro de cada divisão e na fila. */
	ConfiguracaoLiga carregar(JdbcClient jdbc, long donoId, String nomeLiga) {
		exigirJogo();
		List<LinhaClube> linhas = jdbc.sql("""
				SELECT id, divisao_id, nome, forca, escudo_modelo, escudo_cor_1, escudo_cor_2
				FROM jogo_clube WHERE jogo_id = :dono ORDER BY id""")
			.param("dono", donoId)
			.query((rs, n) -> new LinhaClube(rs.getObject("divisao_id", Long.class), clube(rs)))
			.list();
		Map<Long, List<Clube>> porDivisao = linhas.stream()
			.filter(l -> l.divisaoId() != null)
			.collect(Collectors.groupingBy(LinhaClube::divisaoId,
					Collectors.mapping(LinhaClube::clube, Collectors.toList())));
		List<Clube> fila = linhas.stream().filter(l -> l.divisaoId() == null).map(LinhaClube::clube).toList();

		List<Divisao> divisoes = jdbc.sql("""
				SELECT id, nome, prioridade, limite_forca, promovidos, rebaixados
				FROM %s_divisao WHERE %s = :dono ORDER BY prioridade""".formatted(prefixo, dono))
			.param("dono", donoId)
			.query((rs, n) -> new Divisao(rs.getString("nome"), rs.getInt("prioridade"), rs.getInt("limite_forca"),
					rs.getInt("promovidos"), rs.getInt("rebaixados"),
					porDivisao.getOrDefault(rs.getLong("id"), List.of())))
			.list();
		return new ConfiguracaoLiga(nomeLiga, divisoes, fila);
	}

	/** Clube de uma linha com id, nome, força e escudo. */
	static Clube clube(ResultSet rs) throws SQLException {
		return new Clube(rs.getLong("id"), rs.getString("nome"), rs.getInt("forca"),
				new Escudo(ModeloEscudo.valueOf(rs.getString("escudo_modelo")), rs.getString("escudo_cor_1"),
						rs.getString("escudo_cor_2")));
	}

	boolean clubeExiste(JdbcClient jdbc, long clubeId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM %s_clube WHERE id = :clube)".formatted(prefixo))
			.param("clube", clubeId)
			.query(Boolean.class)
			.single();
	}

	/** O asterisco gravado é o permanente. */
	Jogador inserirJogador(JdbcClient jdbc, long clubeId, Jogador j) {
		long id = inserir(jdbc, """
				INSERT INTO %s_jogador (clube_id, nome, nacionalidade, posicao, forca, temperamento, asterisco)
				VALUES (:clube, :nome, :nacionalidade, :posicao, :forca, :temperamento, :asterisco)""".formatted(prefixo),
				Map.of("clube", clubeId, "nome", j.nome(), "nacionalidade", j.nacionalidade(), "posicao",
						j.posicao().name(), "forca", j.forca(), "temperamento", j.temperamento().name(), "asterisco",
						j.asterisco()));
		return new Jogador(id, j.nome(), j.posicao(), j.forca(), j.temperamento(), j.asterisco(), j.nacionalidade());
	}

	/** Plantel do clube em ordem de id. */
	List<Jogador> carregarPlantel(JdbcClient jdbc, long clubeId) {
		return jdbc.sql("""
				SELECT id, nome, posicao, forca, temperamento, asterisco, nacionalidade
				FROM %s_jogador WHERE clube_id = :clube ORDER BY id""".formatted(prefixo))
			.param("clube", clubeId)
			.query((rs, n) -> new Jogador(rs.getLong("id"), rs.getString("nome"), Posicao.valueOf(rs.getString("posicao")),
					rs.getInt("forca"), Temperamento.valueOf(rs.getString("temperamento")), rs.getBoolean("asterisco"),
					rs.getString("nacionalidade")))
			.list();
	}

	static long inserir(JdbcClient jdbc, String sql, Map<String, ?> parametros) {
		KeyHolder chave = new GeneratedKeyHolder();
		jdbc.sql(sql).params(parametros).update(chave, "id");
		return chave.getKeyAs(Long.class);
	}

	private void exigirJogo() {
		if (this != JOGO) {
			throw new IllegalStateException("só o jogo tem clubes em divisões; o molde guarda o formato");
		}
	}

	private Clube inserirClube(JdbcClient jdbc, long donoId, Long divisaoId, Clube c) {
		Map<String, Object> p = new HashMap<>();
		p.put("dono", donoId);
		p.put("divisao", divisaoId);
		p.put("nome", c.nome());
		p.put("forca", c.forca());
		p.put("modelo", c.escudo().modelo().name());
		p.put("cor1", c.escudo().corPrimaria());
		p.put("cor2", c.escudo().corSecundaria());
		long id = inserir(jdbc, """
				INSERT INTO %s_clube (%s, divisao_id, nome, forca, escudo_modelo, escudo_cor_1, escudo_cor_2)
				VALUES (:dono, :divisao, :nome, :forca, :modelo, :cor1, :cor2)""".formatted(prefixo, dono), p);
		return new Clube(id, c.nome(), c.forca(), c.escudo());
	}

	private record LinhaClube(Long divisaoId, Clube clube) {
	}
}
