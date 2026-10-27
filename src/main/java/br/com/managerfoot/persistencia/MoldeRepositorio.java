package br.com.managerfoot.persistencia;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.temporada.ConfiguracaoLiga;

/**
 * Molde editável (docs/decisions/0006): países, a liga nacional de cada um e os planteis. Os ids
 * dos clubes e jogadores recebidos são ignorados; o banco gera novos e eles voltam no retorno.
 */
@Repository
public class MoldeRepositorio {

	private final JdbcClient jdbc;

	public MoldeRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public long criarMolde(String nome) {
		return TabelasLiga.inserir(jdbc, "INSERT INTO molde (nome) VALUES (:nome)", Map.of("nome", nome));
	}

	public long criarPais(long moldeId, String codigo, String nome) {
		return TabelasLiga.inserir(jdbc,
				"INSERT INTO molde_pais (molde_id, codigo, nome) VALUES (:molde, :codigo, :nome)",
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
		return TabelasLiga.MOLDE.gravar(jdbc, paisId, liga);
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
		return TabelasLiga.MOLDE.carregar(jdbc, paisId, nomeLiga);
	}

	/**
	 * Acrescenta jogadores ao plantel do clube e devolve-os com os ids gerados. Recusa tudo se o
	 * plantel passar de {@link Clube#MAXIMO_JOGADORES}. O asterisco gravado é o permanente.
	 */
	@Transactional
	public List<Jogador> adicionarJogadores(long clubeId, List<Jogador> jogadores) {
		// trava o clube para que duas gravações simultâneas não passem juntas do limite
		jdbc.sql("SELECT id FROM molde_clube WHERE id = :clube FOR UPDATE")
			.param("clube", clubeId)
			.query(Long.class)
			.optional()
			.orElseThrow(() -> new NoSuchElementException("clube " + clubeId + " não existe no molde"));
		long atuais = jdbc.sql("SELECT count(*) FROM molde_jogador WHERE clube_id = :clube")
			.param("clube", clubeId)
			.query(Long.class)
			.single();
		if (atuais + jogadores.size() > Clube.MAXIMO_JOGADORES) {
			throw new IllegalStateException("o plantel do clube " + clubeId + " tem " + atuais + " jogadores; com mais "
					+ jogadores.size() + " passaria do máximo de " + Clube.MAXIMO_JOGADORES);
		}
		return jogadores.stream().map(j -> TabelasLiga.MOLDE.inserirJogador(jdbc, clubeId, j)).toList();
	}

	/** Plantel do clube em ordem de id. */
	@Transactional(readOnly = true)
	public List<Jogador> carregarPlantel(long clubeId) {
		if (!TabelasLiga.MOLDE.clubeExiste(jdbc, clubeId)) {
			throw new NoSuchElementException("clube " + clubeId + " não existe no molde");
		}
		return TabelasLiga.MOLDE.carregarPlantel(jdbc, clubeId);
	}
}
