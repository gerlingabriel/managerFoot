package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SplittableRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.contas.AcessoNegadoException;
import br.com.managerfoot.contas.Papel;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.temporada.ClubeInicial;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.ValidadorLiga;

/**
 * Jogo em andamento (docs/decisions/0006): nasce de uma cópia da liga e dos planteis de um país do
 * molde e evolui sem alterá-lo. Todo jogo tem um dono, que é também o seu treinador humano (SPEC §3.2).
 */
@Repository
public class JogoRepositorio {

	private final JdbcClient jdbc;
	private final ContasRepositorio contas;
	private final int candidatosClubeInicial;

	public JogoRepositorio(JdbcClient jdbc, ContasRepositorio contas,
			@Value("${managerfoot.jogo.candidatos-clube-inicial}") int candidatosClubeInicial) {
		if (candidatosClubeInicial < 1) {
			throw new IllegalArgumentException(
					"managerfoot.jogo.candidatos-clube-inicial deve ser ao menos 1: " + candidatosClubeInicial);
		}
		this.jdbc = jdbc;
		this.contas = contas;
		this.candidatosClubeInicial = candidatosClubeInicial;
	}

	/**
	 * Cria um jogo com a liga e os planteis do país do molde e dá ao usuário um clube sorteado entre
	 * os mais fracos da última divisão (SPEC §3.2). Serve a participantes e ao administrador que
	 * prefira o sorteio. Devolve o id do jogo.
	 *
	 * @throws NoSuchElementException se o usuário ou o país não existem
	 * @throws IllegalStateException se o país ainda não tem liga
	 * @throws IllegalArgumentException com todos os problemas, se a liga não puder iniciar
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public long criarJogoComClubeSorteado(long paisMoldeId, long usuarioId, long semente) {
		exigirUsuario(usuarioId);
		Copia copia = copiar(paisMoldeId, usuarioId);
		Clube clube = ClubeInicial.sortear(copia.liga().divisoes().getLast(), candidatosClubeInicial,
				new SplittableRandom(semente));
		jdbc.sql("UPDATE jogo SET semente_clube_inicial = :semente WHERE id = :jogo")
			.param("semente", semente)
			.param("jogo", copia.jogoId())
			.update();
		inserirTreinador(copia.jogoId(), usuarioId, clube.id());
		return copia.jogoId();
	}

	/**
	 * Cria um jogo como {@link #criarJogoComClubeSorteado} com o clube escolhido pelo administrador,
	 * de qualquer divisão (a fila de espera não serve: não tem liga para jogar). Devolve o id do jogo.
	 *
	 * @param clubeMoldeId id do clube no molde; o jogo recebe a cópia dele
	 * @throws AcessoNegadoException se quem pede não é administrador
	 * @throws IllegalArgumentException se o clube não está numa divisão da liga do país
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public long criarJogoComClubeEscolhido(long paisMoldeId, long administradorId, long clubeMoldeId) {
		if (exigirUsuario(administradorId).papel() != Papel.ADMINISTRADOR) {
			throw new AcessoNegadoException("só o administrador escolhe o clube; os demais recebem um clube sorteado");
		}
		Copia copia = copiar(paisMoldeId, administradorId);
		Long destino = copia.clubesPorOrigem().get(clubeMoldeId);
		if (destino == null || copia.liga().filaDeEspera().stream().anyMatch(c -> c.id() == destino)) {
			throw new IllegalArgumentException("O clube escolhido precisa estar numa divisão da liga deste país.");
		}
		inserirTreinador(copia.jogoId(), administradorId, destino);
		return copia.jogoId();
	}

	/** Clube do treinador humano no jogo, se ele tiver um. */
	@Transactional(readOnly = true)
	public Optional<Long> clubeDoTreinador(long jogoId, long usuarioId) {
		return jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo AND usuario_id = :usuario")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.query(Long.class)
			.optional();
	}

	/**
	 * Copia a liga e os planteis do país; a leitura do molde é consistente mesmo que ele seja editado
	 * ao mesmo tempo, pelo isolamento de quem chama.
	 */
	private Copia copiar(long paisMoldeId, long donoId) {
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

		long jogoId = TabelasLiga.inserir(jdbc, """
				INSERT INTO jogo (molde_id, pais_codigo, nome_liga, dono_id)
				VALUES (:molde, :codigo, :nome, :dono)""",
				Map.of("molde", pais.moldeId(), "codigo", pais.codigo(), "nome", liga.nome(), "dono", donoId));
		ConfiguracaoLiga copia = TabelasLiga.JOGO.gravar(jdbc, jogoId, liga);
		List<Clube> origens = clubes(liga);
		List<Clube> destinos = clubes(copia);
		Map<Long, Long> clubesPorOrigem = new HashMap<>();
		for (int i = 0; i < origens.size(); i++) {
			long destino = destinos.get(i).id();
			clubesPorOrigem.put(origens.get(i).id(), destino);
			TabelasLiga.MOLDE.carregarPlantel(jdbc, origens.get(i).id())
				.forEach(j -> TabelasLiga.JOGO.inserirJogador(jdbc, destino, j));
		}
		return new Copia(jogoId, copia, clubesPorOrigem);
	}

	private Usuario exigirUsuario(long usuarioId) {
		return contas.buscar(usuarioId)
			.orElseThrow(() -> new NoSuchElementException("usuário " + usuarioId + " não existe"));
	}

	private void inserirTreinador(long jogoId, long usuarioId, long clubeId) {
		jdbc.sql("INSERT INTO jogo_treinador (jogo_id, usuario_id, clube_id) VALUES (:jogo, :usuario, :clube)")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.param("clube", clubeId)
			.update();
	}

	/**
	 * Trava a linha do jogo até o fim da transação de quem chama, para serializar operações que
	 * mudam a temporada.
	 *
	 * @throws NoSuchElementException se o jogo não existe
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void travar(long jogoId) {
		jdbc.sql("SELECT id FROM jogo WHERE id = :jogo FOR UPDATE")
			.param("jogo", jogoId)
			.query(Long.class)
			.optional()
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não existe"));
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

	private record Copia(long jogoId, ConfiguracaoLiga liga, Map<Long, Long> clubesPorOrigem) {
	}
}
