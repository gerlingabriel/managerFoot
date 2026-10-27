package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escudo;
import br.com.managerfoot.dominio.ModeloEscudo;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;

/**
 * Molde editável (docs/decisions/0006): países e a liga nacional de cada um. Os ids dos clubes
 * recebidos são ignorados; o banco gera novos e eles voltam na liga retornada.
 */
@Repository
public class MoldeRepositorio {

	private final JdbcClient jdbc;

	public MoldeRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public long criarMolde(String nome) {
		return inserir("INSERT INTO molde (nome) VALUES (:nome)", Map.of("nome", nome));
	}

	public long criarPais(long moldeId, String codigo, String nome) {
		return inserir("INSERT INTO molde_pais (molde_id, codigo, nome) VALUES (:molde, :codigo, :nome)",
				Map.of("molde", moldeId, "codigo", codigo, "nome", nome));
	}

	/** Grava a liga de um país que ainda não tem liga e devolve a liga com os ids gerados. */
	@Transactional
	public ConfiguracaoLiga criarLiga(long paisId, ConfiguracaoLiga liga) {
		int atualizados = jdbc.sql("UPDATE molde_pais SET nome_liga = :nome WHERE id = :pais AND nome_liga IS NULL")
			.param("nome", liga.nome())
			.param("pais", paisId)
			.update();
		if (atualizados == 0) {
			throw new IllegalStateException("país " + paisId + " não existe ou já tem liga");
		}
		List<Divisao> divisoes = new ArrayList<>();
		for (Divisao d : liga.divisoes()) {
			long divisaoId = inserir("""
					INSERT INTO molde_divisao (pais_id, nome, prioridade, limite_forca, promovidos, rebaixados)
					VALUES (:pais, :nome, :prioridade, :limite, :promovidos, :rebaixados)""",
					Map.of("pais", paisId, "nome", d.nome(), "prioridade", d.prioridade(), "limite", d.limiteForca(),
							"promovidos", d.promovidos(), "rebaixados", d.rebaixados()));
			List<Clube> clubes = d.clubes().stream().map(c -> inserirClube(paisId, divisaoId, c)).toList();
			divisoes.add(new Divisao(d.nome(), d.prioridade(), d.limiteForca(), d.promovidos(), d.rebaixados(), clubes));
		}
		List<Clube> fila = liga.filaDeEspera().stream().map(c -> inserirClube(paisId, null, c)).toList();
		return new ConfiguracaoLiga(liga.nome(), divisoes, fila);
	}

	/** Liga do país; clubes em ordem de id dentro de cada divisão e na fila. */
	@Transactional(readOnly = true)
	public ConfiguracaoLiga carregarLiga(long paisId) {
		// nome_liga nulo = país ainda sem liga; o Optional externo distingue país inexistente
		String nomeLiga = jdbc.sql("SELECT nome_liga FROM molde_pais WHERE id = :pais")
			.param("pais", paisId)
			.query((rs, n) -> Optional.ofNullable(rs.getString("nome_liga")))
			.optional()
			.orElseThrow(() -> new NoSuchElementException("país " + paisId + " não existe no molde"))
			.orElse(null);

		List<LinhaClube> linhas = jdbc.sql("""
				SELECT id, divisao_id, nome, forca, escudo_modelo, escudo_cor_1, escudo_cor_2
				FROM molde_clube WHERE pais_id = :pais ORDER BY id""")
			.param("pais", paisId)
			.query((rs, n) -> new LinhaClube(rs.getObject("divisao_id", Long.class),
					new Clube(rs.getLong("id"), rs.getString("nome"), rs.getInt("forca"),
							new Escudo(ModeloEscudo.valueOf(rs.getString("escudo_modelo")), rs.getString("escudo_cor_1"),
									rs.getString("escudo_cor_2")))))
			.list();
		Map<Long, List<Clube>> porDivisao = linhas.stream()
			.filter(l -> l.divisaoId() != null)
			.collect(Collectors.groupingBy(LinhaClube::divisaoId,
					Collectors.mapping(LinhaClube::clube, Collectors.toList())));
		List<Clube> fila = linhas.stream().filter(l -> l.divisaoId() == null).map(LinhaClube::clube).toList();

		List<Divisao> divisoes = jdbc.sql("""
				SELECT id, nome, prioridade, limite_forca, promovidos, rebaixados
				FROM molde_divisao WHERE pais_id = :pais ORDER BY prioridade""")
			.param("pais", paisId)
			.query((rs, n) -> new Divisao(rs.getString("nome"), rs.getInt("prioridade"), rs.getInt("limite_forca"),
					rs.getInt("promovidos"), rs.getInt("rebaixados"),
					porDivisao.getOrDefault(rs.getLong("id"), List.of())))
			.list();
		return new ConfiguracaoLiga(nomeLiga, divisoes, fila);
	}

	private Clube inserirClube(long paisId, Long divisaoId, Clube c) {
		long id = inserir("""
				INSERT INTO molde_clube (pais_id, divisao_id, nome, forca, escudo_modelo, escudo_cor_1, escudo_cor_2)
				VALUES (:pais, :divisao, :nome, :forca, :modelo, :cor1, :cor2)""",
				parametrosClube(paisId, divisaoId, c));
		return new Clube(id, c.nome(), c.forca(), c.escudo());
	}

	private static Map<String, Object> parametrosClube(long paisId, Long divisaoId, Clube c) {
		Map<String, Object> p = new HashMap<>();
		p.put("pais", paisId);
		p.put("divisao", divisaoId);
		p.put("nome", c.nome());
		p.put("forca", c.forca());
		p.put("modelo", c.escudo().modelo().name());
		p.put("cor1", c.escudo().corPrimaria());
		p.put("cor2", c.escudo().corSecundaria());
		return p;
	}

	private long inserir(String sql, Map<String, ?> parametros) {
		KeyHolder chave = new GeneratedKeyHolder();
		jdbc.sql(sql).params(parametros).update(chave, "id");
		return chave.getKeyAs(Long.class);
	}

	private record LinhaClube(Long divisaoId, Clube clube) {
	}
}
