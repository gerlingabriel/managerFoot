package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;

/** Migração V17 e jogadores à venda da máquina ({@link MercadoRepositorio}) contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MercadoRepositorioIT {

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JdbcClient jdbc;

	long jogo;
	long clubeDoUsuario;

	@BeforeEach
	void criaJogoComTemporada() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		clubeDoUsuario = jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo")
			.param("jogo", jogo)
			.query(Long.class)
			.single();
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
	}

	@Test
	void temporadaComecaComCincoOuSeisAVendaEmCadaClubeDaMaquinaDasDivisoes() {
		Map<Long, Integer> porClube = aVendaPorClube();
		// 16 + 15 clubes nas divisões, menos o do usuário; a fila não anuncia
		assertThat(porClube).hasSize(16 + 15 - 1).doesNotContainKey(clubeDoUsuario);
		assertThat(porClube.values()).allMatch(n -> n == 5 || n == 6);
		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_jogador jj JOIN jogo_clube c ON c.id = jj.clube_id
				WHERE c.jogo_id = :jogo AND jj.a_venda AND c.divisao_id IS NULL""")
			.param("jogo", jogo)
			.query(Integer.class)
			.single()).isZero();
	}

	@Test
	void rodadaCompletaOsAnunciosERetiraOsDoClubeDoUsuario() {
		long clubeDaMaquina = aVendaPorClube().keySet().iterator().next();
		List<Long> anunciados = jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :clube AND a_venda ORDER BY id")
			.param("clube", clubeDaMaquina)
			.query(Long.class)
			.list();
		// três saem do anúncio (como se tivessem sido vendidos) e um anúncio indevido no clube do usuário
		jdbc.sql("UPDATE jogo_jogador SET a_venda = false WHERE id IN (:ids)")
			.param("ids", anunciados.subList(0, 3))
			.update();
		jdbc.sql("UPDATE jogo_jogador SET a_venda = true WHERE clube_id = :clube")
			.param("clube", clubeDoUsuario)
			.update();

		rodadas.avancar(jogo, 1);

		Map<Long, Integer> porClube = aVendaPorClube();
		assertThat(porClube.get(clubeDaMaquina)).isBetween(5, 6);
		assertThat(porClube).doesNotContainKey(clubeDoUsuario);
		// os anúncios que continuavam não foram trocados
		assertThat(jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :clube AND a_venda")
			.param("clube", clubeDaMaquina)
			.query(Long.class)
			.list()).containsAll(anunciados.subList(3, anunciados.size()));
	}

	private Map<Long, Integer> aVendaPorClube() {
		Map<Long, Integer> porClube = new HashMap<>();
		jdbc.sql("""
				SELECT jj.clube_id, count(*) AS n FROM jogo_jogador jj JOIN jogo_clube c ON c.id = jj.clube_id
				WHERE c.jogo_id = :jogo AND jj.a_venda GROUP BY jj.clube_id""")
			.param("jogo", jogo)
			.query((rs, i) -> Map.entry(rs.getLong("clube_id"), rs.getInt("n")))
			.list()
			.forEach(e -> porClube.put(e.getKey(), e.getValue()));
		return porClube;
	}
}
