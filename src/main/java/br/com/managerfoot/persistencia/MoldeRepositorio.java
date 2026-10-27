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
import br.com.managerfoot.temporada.FormatoDivisao;
import br.com.managerfoot.temporada.FormatoLiga;
import br.com.managerfoot.temporada.ValidadorLiga;

/**
 * Molde editável (docs/decisions/0006): países, o formato da liga nacional de cada um, os clubes do
 * país e os planteis. Os ids
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

	/**
	 * Dá nome à liga de um país que ainda não tem liga. As divisões entram depois, uma a uma.
	 *
	 * @throws IllegalStateException se o país não existe ou já tem liga
	 */
	@Transactional
	public void criarLiga(long paisId, String nome) {
		int atualizados = jdbc.sql("UPDATE molde_pais SET nome_liga = :nome WHERE id = :pais AND nome_liga IS NULL")
			.param("nome", nome)
			.param("pais", paisId)
			.update();
		if (atualizados == 0) {
			throw new IllegalStateException("país " + paisId + " não existe ou já tem liga");
		}
	}

	/**
	 * Acrescenta uma divisão abaixo das que já existem e devolve a prioridade dada a ela (SPEC
	 * §4.2, rev. 27): a próxima livre, sem repetir nem pular. A faixa de clubes e os limites são
	 * conferidos pelo {@link ValidadorLiga}, para o editor poder guardar uma liga incompleta.
	 *
	 * @throws IllegalStateException se o país não existe ou ainda não tem liga
	 */
	@Transactional
	public int adicionarDivisao(long paisId, String nome, int quantidadeClubes, int limiteForca, int rebaixados) {
		travarPaisComLiga(paisId);
		int prioridade = jdbc.sql("SELECT coalesce(max(prioridade), 0) + 1 FROM molde_divisao WHERE pais_id = :pais")
			.param("pais", paisId)
			.query(Integer.class)
			.single();
		jdbc.sql("""
				INSERT INTO molde_divisao (pais_id, nome, prioridade, quantidade_clubes, limite_forca, rebaixados)
				VALUES (:pais, :nome, :prioridade, :quantidade, :limite, :rebaixados)""")
			.param("pais", paisId)
			.param("nome", nome)
			.param("prioridade", prioridade)
			.param("quantidade", quantidadeClubes)
			.param("limite", limiteForca)
			.param("rebaixados", rebaixados)
			.update();
		return prioridade;
	}

	/**
	 * Exclui a divisão; as de baixo sobem um número (rev. 27, opção A), mantendo nome e limite.
	 *
	 * @throws NoSuchElementException se o país não tem divisão com essa prioridade
	 */
	@Transactional
	public void excluirDivisao(long paisId, int prioridade) {
		travarPaisComLiga(paisId);
		int excluidas = jdbc.sql("DELETE FROM molde_divisao WHERE pais_id = :pais AND prioridade = :prioridade")
			.param("pais", paisId)
			.param("prioridade", prioridade)
			.update();
		if (excluidas == 0) {
			throw new NoSuchElementException("o país " + paisId + " não tem divisão de prioridade " + prioridade);
		}
		jdbc.sql("UPDATE molde_divisao SET prioridade = prioridade - 1 WHERE pais_id = :pais AND prioridade > :prioridade")
			.param("pais", paisId)
			.param("prioridade", prioridade)
			.update();
	}

	/** Formato da liga do país, divisões por prioridade; nome nulo se o país ainda não tem liga. */
	@Transactional(readOnly = true)
	public FormatoLiga carregarFormato(long paisId) {
		// nome_liga nulo = país ainda sem liga; o Optional externo distingue país inexistente
		String nomeLiga = jdbc.sql("SELECT nome_liga FROM molde_pais WHERE id = :pais")
			.param("pais", paisId)
			.query((rs, n) -> Optional.ofNullable(rs.getString("nome_liga")))
			.optional()
			.orElseThrow(() -> new NoSuchElementException("país " + paisId + " não existe no molde"))
			.orElse(null);
		List<FormatoDivisao> divisoes = jdbc.sql("""
				SELECT nome, prioridade, quantidade_clubes, limite_forca, rebaixados
				FROM molde_divisao WHERE pais_id = :pais ORDER BY prioridade""")
			.param("pais", paisId)
			.query((rs, n) -> new FormatoDivisao(rs.getString("nome"), rs.getInt("prioridade"),
					rs.getInt("quantidade_clubes"), rs.getInt("limite_forca"), rs.getInt("rebaixados")))
			.list();
		return new FormatoLiga(nomeLiga, divisoes);
	}

	/** Cadastra um clube do país e devolve-o com o id gerado. */
	@Transactional
	public Clube adicionarClube(long paisId, Clube clube) {
		long id = TabelasLiga.inserir(jdbc, """
				INSERT INTO molde_clube (pais_id, nome, forca, escudo_modelo, escudo_cor_1, escudo_cor_2)
				VALUES (:pais, :nome, :forca, :modelo, :cor1, :cor2)""",
				Map.of("pais", paisId, "nome", clube.nome(), "forca", clube.forca(), "modelo",
						clube.escudo().modelo().name(), "cor1", clube.escudo().corPrimaria(), "cor2",
						clube.escudo().corSecundaria()));
		return new Clube(id, clube.nome(), clube.forca(), clube.escudo());
	}

	/** Clubes do país em ordem de id. */
	@Transactional(readOnly = true)
	public List<Clube> carregarClubes(long paisId) {
		return jdbc.sql("""
				SELECT id, nome, forca, escudo_modelo, escudo_cor_1, escudo_cor_2
				FROM molde_clube WHERE pais_id = :pais ORDER BY id""")
			.param("pais", paisId)
			.query((rs, n) -> TabelasLiga.clube(rs))
			.list();
	}

	/**
	 * Países do molde cuja liga pode iniciar: com formato válido e clubes suficientes (SPEC §4.1).
	 * É a lista de campeonatos que a tela oferece para escolher.
	 */
	@Transactional(readOnly = true)
	public List<CampeonatoDisponivel> campeonatosDisponiveis(long moldeId) {
		List<CampeonatoDisponivel> paises = jdbc.sql("""
				SELECT id, codigo, nome, nome_liga FROM molde_pais
				WHERE molde_id = :molde AND nome_liga IS NOT NULL ORDER BY nome""")
			.param("molde", moldeId)
			.query((rs, n) -> new CampeonatoDisponivel(rs.getLong("id"), rs.getString("codigo"), rs.getString("nome"),
					rs.getString("nome_liga")))
			.list();
		return paises.stream()
			.filter(p -> ValidadorLiga.problemas(carregarFormato(p.paisId()), contarClubes(p.paisId())).isEmpty())
			.toList();
	}

	/** Campeonato que pode ser escolhido: a liga nacional de um país. */
	public record CampeonatoDisponivel(long paisId, String codigoPais, String nomePais, String nomeLiga) {
	}

	private int contarClubes(long paisId) {
		return jdbc.sql("SELECT count(*) FROM molde_clube WHERE pais_id = :pais")
			.param("pais", paisId)
			.query(Integer.class)
			.single();
	}

	/** Serializa as edições de divisões do país: duas inclusões simultâneas não pegam a mesma prioridade. */
	private void travarPaisComLiga(long paisId) {
		jdbc.sql("SELECT id FROM molde_pais WHERE id = :pais AND nome_liga IS NOT NULL FOR UPDATE")
			.param("pais", paisId)
			.query(Long.class)
			.optional()
			.orElseThrow(() -> new IllegalStateException("país " + paisId + " não existe ou ainda não tem liga"));
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
