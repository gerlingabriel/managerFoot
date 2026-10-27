package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.ValidadorLiga;

/**
 * Jogo em andamento (docs/decisions/0006): nasce de uma cópia da liga e dos planteis de um país do
 * molde e evolui sem alterá-lo.
 */
@Repository
public class JogoRepositorio {

	private final JdbcClient jdbc;

	public JogoRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Cria um jogo com a liga e os planteis do país do molde e devolve o id do jogo. A leitura do
	 * molde é consistente mesmo que ele seja editado ao mesmo tempo.
	 *
	 * @throws NoSuchElementException se o país não existe no molde
	 * @throws IllegalStateException se o país ainda não tem liga
	 * @throws IllegalArgumentException com todos os problemas, se a liga não puder iniciar
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public long criarJogo(long paisMoldeId) {
		PaisMolde pais = jdbc.sql("SELECT molde_id, codigo, nome_liga FROM molde_pais WHERE id = :pais")
			.param("pais", paisMoldeId)
			.query((rs, n) -> new PaisMolde(rs.getLong("molde_id"), rs.getString("codigo"), rs.getString("nome_liga")))
			.optional()
			.orElseThrow(() -> new NoSuchElementException("país " + paisMoldeId + " não existe no molde"));
		if (pais.nomeLiga() == null) {
			throw new IllegalStateException("o país " + pais.codigo() + " ainda não tem liga no molde");
		}
		ConfiguracaoLiga liga = TabelasLiga.MOLDE.carregar(jdbc, paisMoldeId, pais.nomeLiga());
		ValidadorLiga.exigirValida(liga);

		long jogoId = TabelasLiga.inserir(jdbc,
				"INSERT INTO jogo (molde_id, pais_codigo, nome_liga) VALUES (:molde, :codigo, :nome)",
				Map.of("molde", pais.moldeId(), "codigo", pais.codigo(), "nome", liga.nome()));
		ConfiguracaoLiga copia = TabelasLiga.JOGO.gravar(jdbc, jogoId, liga);
		List<Clube> origens = clubes(liga);
		List<Clube> destinos = clubes(copia);
		for (int i = 0; i < origens.size(); i++) {
			long destino = destinos.get(i).id();
			TabelasLiga.MOLDE.carregarPlantel(jdbc, origens.get(i).id())
				.forEach(j -> TabelasLiga.JOGO.inserirJogador(jdbc, destino, j));
		}
		return jogoId;
	}

	/** Liga do jogo; clubes em ordem de id dentro de cada divisão e na fila. */
	@Transactional(readOnly = true)
	public ConfiguracaoLiga carregarLiga(long jogoId) {
		String nomeLiga = jdbc.sql("SELECT nome_liga FROM jogo WHERE id = :jogo")
			.param("jogo", jogoId)
			.query(String.class)
			.optional()
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não existe"));
		return TabelasLiga.JOGO.carregar(jdbc, jogoId, nomeLiga);
	}

	/** Planteis de todos os clubes do jogo, por id do clube, cada um em ordem de id. */
	@Transactional(readOnly = true)
	public Map<Long, List<Jogador>> carregarPlanteis(long jogoId) {
		Map<Long, List<Jogador>> planteis = new HashMap<>();
		for (Clube clube : clubes(carregarLiga(jogoId))) {
			planteis.put(clube.id(), TabelasLiga.JOGO.carregarPlantel(jdbc, clube.id()));
		}
		return planteis;
	}

	/** Clubes das divisões, na ordem de prioridade, seguidos dos da fila de espera. */
	private static List<Clube> clubes(ConfiguracaoLiga liga) {
		List<Clube> clubes = new ArrayList<>();
		liga.divisoes().stream().map(Divisao::clubes).forEach(clubes::addAll);
		clubes.addAll(liga.filaDeEspera());
		return clubes;
	}

	private record PaisMolde(long moldeId, String codigo, String nomeLiga) {
	}
}
