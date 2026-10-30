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
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.temporada.ClubeInicial;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.FormatoDivisao;
import br.com.managerfoot.temporada.VagasContinental;

/**
 * Jogo em andamento (docs/decisions/0006): nasce de uma cópia da liga e dos planteis de um país do
 * molde e evolui sem alterá-lo. Todo jogo tem um dono, que é também o seu treinador humano (SPEC §3.2).
 */
@Repository
public class JogoRepositorio {

	private final JdbcClient jdbc;
	private final ContasRepositorio contas;
	private final MoldeRepositorio moldes;
	private final EscalacaoRepositorio escalacoes;
	private final EscalacaoMaquina escalacaoMaquina = new EscalacaoMaquina(ParametrosMotor.padrao());
	private final int candidatosClubeInicial;
	private final int anoInicial;

	public JogoRepositorio(JdbcClient jdbc, ContasRepositorio contas, MoldeRepositorio moldes,
			EscalacaoRepositorio escalacoes,
			@Value("${managerfoot.jogo.candidatos-clube-inicial}") int candidatosClubeInicial,
			@Value("${managerfoot.jogo.ano-inicial}") int anoInicial) {
		if (candidatosClubeInicial < 1) {
			throw new IllegalArgumentException(
					"managerfoot.jogo.candidatos-clube-inicial deve ser ao menos 1: " + candidatosClubeInicial);
		}
		this.jdbc = jdbc;
		this.contas = contas;
		this.moldes = moldes;
		this.escalacoes = escalacoes;
		this.candidatosClubeInicial = candidatosClubeInicial;
		this.anoInicial = anoInicial;
	}

	/** Ano da temporada {@code numero} do jogo (SPEC §8.1.1, rev. 40). */
	public int ano(long jogoId, int numero) {
		return jdbc.sql("SELECT ano_inicial + :numero - 1 FROM jogo WHERE id = :jogo").param("numero", numero)
			.param("jogo", jogoId).query(Integer.class).single();
	}

	/**
	 * Cria um jogo com os clubes e planteis do país do molde, distribuídos pela força nas divisões do
	 * formato (SPEC §4.1, rev. 27), e dá ao usuário um clube sorteado entre os mais fracos da última
	 * divisão (SPEC §3.2). Serve a participantes e ao administrador que prefira o sorteio. A semente
	 * decide os empates da distribuição e o sorteio do clube. Devolve o id do jogo.
	 *
	 * @throws NoSuchElementException se o usuário ou o país não existem
	 * @throws IllegalStateException se o país ainda não tem liga
	 * @throws IllegalArgumentException com todos os problemas, se a liga não puder iniciar
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public long criarJogoComClubeSorteado(long paisMoldeId, long usuarioId, long semente) {
		exigirUsuario(usuarioId);
		Copia copia = copiar(paisMoldeId, usuarioId, semente);
		Clube clube = ClubeInicial.sortear(copia.liga().divisoes().getLast(), candidatosClubeInicial,
				new SplittableRandom(~semente));
		inserirTreinador(copia.jogoId(), usuarioId, clube.id());
		return copia.jogoId();
	}

	/**
	 * Clubes do país distribuídos nas divisões como {@link #criarJogoComClubeSorteado} faria com a
	 * mesma semente, ainda com os ids do molde. É o quadro em que o administrador escolhe o clube.
	 *
	 * @throws IllegalArgumentException com todos os problemas, se a liga não puder iniciar
	 */
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public ConfiguracaoLiga distribuicao(long paisMoldeId, long semente) {
		lerPais(paisMoldeId);
		return distribuir(paisMoldeId, semente);
	}

	/**
	 * Cria um jogo como {@link #criarJogoComClubeSorteado} com o clube escolhido pelo administrador
	 * no quadro de {@link #distribuicao} da mesma semente. O clube precisa estar numa divisão: a fila
	 * de espera não tem liga para jogar. Devolve o id do jogo.
	 *
	 * @param clubeMoldeId id do clube no molde; o jogo recebe a cópia dele
	 * @throws AcessoNegadoException se quem pede não é administrador
	 * @throws IllegalArgumentException se o clube não está numa divisão da liga do país
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public long criarJogoComClubeEscolhido(long paisMoldeId, long administradorId, long semente, long clubeMoldeId) {
		if (exigirUsuario(administradorId).papel() != Papel.ADMINISTRADOR) {
			throw new AcessoNegadoException("só o administrador escolhe o clube; os demais recebem um clube sorteado");
		}
		Copia copia = copiar(paisMoldeId, administradorId, semente);
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
	 * Distribui os clubes do país e copia a liga e os planteis para um jogo novo. A leitura do molde
	 * é consistente mesmo que ele seja editado ao mesmo tempo, pelo isolamento de quem chama.
	 */
	private Copia copiar(long paisMoldeId, long donoId, long semente) {
		PaisMolde pais = lerPais(paisMoldeId);
		ConfiguracaoLiga liga = distribuir(paisMoldeId, semente);

		long jogoId = TabelasLiga.inserir(jdbc, """
				INSERT INTO jogo (molde_id, pais_codigo, nome_liga, dono_id, semente_criacao, ano_inicial)
				VALUES (:molde, :codigo, :nome, :dono, :semente, :ano)""",
				Map.of("molde", pais.moldeId(), "codigo", pais.codigo(), "nome", liga.nome(), "dono", donoId, "semente",
						semente, "ano", anoInicial));
		ConfiguracaoLiga copia = TabelasLiga.JOGO.gravar(jdbc, jogoId, liga);
		List<Clube> origens = clubes(liga);
		List<Clube> destinos = clubes(copia);
		Map<Long, Long> clubesPorOrigem = new HashMap<>();
		Map<Long, String> treinadores = moldes.treinadores(paisMoldeId);
		for (int i = 0; i < origens.size(); i++) {
			long destino = destinos.get(i).id();
			clubesPorOrigem.put(origens.get(i).id(), destino);
			jdbc.sql("INSERT INTO jogo_treinador_maquina (jogo_id, nome, clube_id) VALUES (:jogo, :nome, :clube)")
				.param("jogo", jogoId)
				.param("nome", treinadores.get(origens.get(i).id()))
				.param("clube", destino)
				.update();
			TabelasLiga.MOLDE.carregarPlantel(jdbc, origens.get(i).id())
				.forEach(j -> TabelasLiga.JOGO.inserirJogador(jdbc, destino, j));
		}
		copiarContinental(jogoId, pais);
		return new Copia(jogoId, copia, clubesPorOrigem);
	}

	/**
	 * Copia a competição continental da região do país jogado e os clubes que podem ocupar as vagas
	 * dos outros países (SPEC §8.3.6, rev. 74): de um país com liga, os mais fortes no tamanho da 1ª
	 * divisão; de um país sem liga, todos. Sem competição da região no molde, o jogo fica sem ela.
	 */
	private void copiarContinental(long jogoId, PaisMolde pais) {
		Optional<MoldeRepositorio.ContinentalMolde> continental = moldes.continentais(pais.moldeId()).stream()
			.filter(c -> c.vagas().containsKey(pais.codigo()))
			.findFirst();
		if (continental.isEmpty()) {
			return;
		}
		jdbc.sql("INSERT INTO jogo_continental (jogo_id, regiao, nome) VALUES (:jogo, :regiao, :nome)")
			.param("jogo", jogoId)
			.param("regiao", continental.get().regiao())
			.param("nome", continental.get().nome())
			.update();
		int ordem = 0;
		for (Map.Entry<String, Integer> vaga : continental.get().vagas().entrySet()) {
			jdbc.sql("""
					INSERT INTO jogo_continental_vaga (jogo_id, pais_codigo, vagas, ordem)
					VALUES (:jogo, :pais, :vagas, :ordem)""")
				.param("jogo", jogoId)
				.param("pais", vaga.getKey())
				.param("vagas", vaga.getValue())
				.param("ordem", ordem++)
				.update();
			if (!vaga.getKey().equals(pais.codigo())) {
				copiarEstrangeiros(jogoId, pais.moldeId(), vaga.getKey());
			}
		}
	}

	private void copiarEstrangeiros(long jogoId, long moldeId, String codigo) {
		Optional<Long> paisId = jdbc.sql("SELECT id FROM molde_pais WHERE molde_id = :molde AND codigo = :codigo")
			.param("molde", moldeId)
			.param("codigo", codigo)
			.query(Long.class)
			.optional();
		if (paisId.isEmpty()) {
			return; // país sem clubes: as vagas ficam vazias (§8.3.2 item 3)
		}
		List<Clube> clubes = moldes.carregarClubes(paisId.get());
		List<FormatoDivisao> divisoes = moldes.carregarFormato(paisId.get()).divisoes();
		List<Clube> elegiveis = divisoes.isEmpty() ? clubes
				: VagasContinental.elegiveisDoPaisNaoJogado(clubes, divisoes.getFirst().quantidadeClubes());
		for (Clube origem : elegiveis) {
			Clube destino = TabelasLiga.JOGO.inserirEstrangeiro(jdbc, jogoId, codigo, origem);
			TabelasLiga.MOLDE.carregarPlantel(jdbc, origem.id())
				.forEach(j -> TabelasLiga.JOGO.inserirJogador(jdbc, destino.id(), j));
		}
	}

	private PaisMolde lerPais(long paisMoldeId) {
		PaisMolde pais = jdbc.sql("SELECT molde_id, codigo, nome_liga FROM molde_pais WHERE id = :pais")
			.param("pais", paisMoldeId)
			.query((rs, n) -> new PaisMolde(rs.getLong("molde_id"), rs.getString("codigo"), rs.getString("nome_liga")))
			.optional()
			.orElseThrow(() -> new NoSuchElementException("país " + paisMoldeId + " não existe no molde"));
		if (pais.nomeLiga() == null) {
			throw new IllegalStateException("o país " + pais.codigo() + " ainda não tem liga no molde");
		}
		return pais;
	}

	private ConfiguracaoLiga distribuir(long paisMoldeId, long semente) {
		return moldes.carregarFormato(paisMoldeId)
			.distribuir(moldes.carregarClubes(paisMoldeId), new SplittableRandom(semente));
	}

	private Usuario exigirUsuario(long usuarioId) {
		return contas.buscar(usuarioId)
			.orElseThrow(() -> new NoSuchElementException("usuário " + usuarioId + " não existe"));
	}

	/** Põe o treinador no clube, que começa com a escalação da máquina (SPEC §10.3, rev. 34). */
	private void inserirTreinador(long jogoId, long usuarioId, long clubeId) {
		jdbc.sql("INSERT INTO jogo_treinador (jogo_id, usuario_id, clube_id) VALUES (:jogo, :usuario, :clube)")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.param("clube", clubeId)
			.update();
		List<Jogador> plantel = TabelasLiga.JOGO.carregarPlantel(jdbc, clubeId);
		// elenco abaixo de 11 ainda está em aberto (SPEC §9.3): sem escalação gravada, como os da máquina
		if (plantel.size() >= Escalacao.MINIMO_TITULARES) {
			escalacoes.gravar(clubeId, EscalacaoDoTreinador.de(escalacaoMaquina.escalar(plantel)));
		}
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
