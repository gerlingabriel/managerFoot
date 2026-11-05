package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.temporada.BonusArtilharia;
import br.com.managerfoot.temporada.Calendario;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.Rodada;
import br.com.managerfoot.temporada.SorteTemporada;
import br.com.managerfoot.temporada.Temporada;

/** Temporadas do jogo em andamento, com calendário, sorte e azar e bônus de artilharia. */
@Repository
public class TemporadaRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final MercadoRepositorio mercado;
	private final CopaRepositorio copas;
	private final ContinentalRepositorio continentais;

	public TemporadaRepositorio(JdbcClient jdbc, JogoRepositorio jogos, MercadoRepositorio mercado,
			CopaRepositorio copas, ContinentalRepositorio continentais) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.mercado = mercado;
		this.copas = copas;
		this.continentais = continentais;
	}

	/**
	 * Inicia a 1ª temporada do jogo com os sorteios da semente e devolve-a. As seguintes nascem do
	 * fim de temporada ({@link FimDeTemporadaRepositorio}).
	 *
	 * @throws NoSuchElementException se o jogo não existe
	 * @throws IllegalStateException se o jogo já tem temporada
	 */
	@Transactional
	public Temporada iniciarPrimeiraTemporada(long jogoId, long semente) {
		// trava o jogo: duas chamadas simultâneas não sorteiam duas temporadas
		jogos.travar(jogoId);
		if (numeroAtual(jogoId).isPresent()) {
			throw new IllegalStateException("o jogo " + jogoId + " já tem temporada iniciada");
		}
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		Temporada temporada = Temporada.iniciar(1, liga, jogos.carregarPlanteis(jogoId), semente, clubesDeUsuario(jogoId));
		gravar(jogoId, temporada);
		return temporada;
	}

	/**
	 * Grava a temporada e seus sorteios, sorteia a copa (SPEC §8.2) e a continental (§8.3.6) e completa os jogadores à venda da máquina; quem chama abre a
	 * transação, trava o jogo e já pôs os treinadores nos clubes da nova temporada.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void gravar(long jogoId, Temporada temporada) {
		long semente = temporada.semente();
		long temporadaId = TabelasLiga.inserir(jdbc,
				"INSERT INTO jogo_temporada (jogo_id, numero, semente) VALUES (:jogo, :numero, :semente)",
				Map.of("jogo", jogoId, "numero", temporada.numero(), "semente", semente));
		Map<Integer, Long> divisaoPorPrioridade = divisaoPorPrioridade(jogoId);
		for (Temporada.SorteiosDivisao sorteios : temporada.divisoes()) {
			long divisaoId = divisaoPorPrioridade.get(sorteios.prioridade());
			gravarBeneficiados(temporadaId, divisaoId, sorteios.sorte());
			for (Jogo jogo : sorteios.calendario().jogos()) {
				jdbc.sql("""
						INSERT INTO jogo_partida (temporada_id, divisao_id, rodada, mandante_id, visitante_id)
						VALUES (:temporada, :divisao, :rodada, :mandante, :visitante)""")
					.param("temporada", temporadaId)
					.param("divisao", divisaoId)
					.param("rodada", jogo.rodada())
					.param("mandante", jogo.mandante().id())
					.param("visitante", jogo.visitante().id())
					.update();
			}
			inserirBonus(temporadaId, divisaoId, sorteios.bonus().asteriscoTemporario(), false);
			inserirBonus(temporadaId, divisaoId, sorteios.bonus().bonusExtra(), true);
		}
		copas.criar(jogoId, temporadaId, semente);
		continentais.criar(jogoId, temporadaId, temporada.numero(), semente);
		mercado.reporAVenda(jogoId, MercadoRepositorio.semente(semente, 0));
		mercado.renovarClubesDeFora(jogoId, MercadoRepositorio.semente(semente, 0));
	}

	private void gravarBeneficiados(long temporadaId, long divisaoId, SorteTemporada sorte) {
		for (Clube clube : sorte.beneficiados()) {
			jdbc.sql("""
					INSERT INTO jogo_temporada_beneficiado (temporada_id, divisao_id, clube_id)
					VALUES (:temporada, :divisao, :clube)""")
				.param("temporada", temporadaId)
				.param("divisao", divisaoId)
				.param("clube", clube.id())
				.update();
		}
	}

	/** Clubes com treinador usuário: ficam fora do sorteio da sorte e, fracos, têm o benefício (SPEC §5.3.1, rev. 108). */
	@Transactional(propagation = Propagation.MANDATORY)
	public Set<Long> clubesDeUsuario(long jogoId) {
		return Set.copyOf(jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query(Long.class)
			.list());
	}

	/**
	 * Jogo em rede (SPEC §5.3.1, rev. 108): a 1ª temporada foi sorteada com a sala aberta, só com o administrador. Ao
	 * começar, com todos nos clubes, refaz os beneficiados pela mesma semente; calendário e bônus não mudam.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void refazerSorteDaPrimeiraTemporada(long jogoId) {
		LinhaTemporada linha = jdbc.sql("SELECT id, numero, semente FROM jogo_temporada WHERE jogo_id = :jogo AND numero = 1")
			.param("jogo", jogoId)
			.query((rs, n) -> new LinhaTemporada(rs.getLong("id"), rs.getInt("numero"), rs.getLong("semente")))
			.single();
		Temporada refeita = Temporada.iniciar(1, jogos.carregarLiga(jogoId), jogos.carregarPlanteis(jogoId), linha.semente(),
				clubesDeUsuario(jogoId));
		jdbc.sql("DELETE FROM jogo_temporada_beneficiado WHERE temporada_id = :temporada").param("temporada", linha.id()).update();
		Map<Integer, Long> divisaoPorPrioridade = divisaoPorPrioridade(jogoId);
		for (Temporada.SorteiosDivisao sorteios : refeita.divisoes()) {
			gravarBeneficiados(linha.id(), divisaoPorPrioridade.get(sorteios.prioridade()), sorteios.sorte());
		}
	}

	/**
	 * Temporada mais recente do jogo, como foi gravada.
	 *
	 * @throws NoSuchElementException se o jogo não existe ou ainda não tem temporada
	 */
	@Transactional(readOnly = true)
	public Temporada carregarAtual(long jogoId) {
		LinhaTemporada linha = jdbc.sql("""
				SELECT id, numero, semente FROM jogo_temporada WHERE jogo_id = :jogo
				ORDER BY numero DESC LIMIT 1""")
			.param("jogo", jogoId)
			.query((rs, n) -> new LinhaTemporada(rs.getLong("id"), rs.getInt("numero"), rs.getLong("semente")))
			.optional()
			.orElseThrow(() -> new NoSuchElementException("o jogo " + jogoId + " não existe ou não tem temporada"));

		Map<Long, Clube> clubes = new HashMap<>();
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		liga.divisoes().stream().flatMap(d -> d.clubes().stream()).forEach(c -> clubes.put(c.id(), c));
		liga.filaDeEspera().forEach(c -> clubes.put(c.id(), c));

		Map<Long, Integer> prioridades = new HashMap<>();
		divisaoPorPrioridade(jogoId).forEach((prioridade, id) -> prioridades.put(id, prioridade));

		// divisões em ordem de prioridade; partidas na ordem gravada dentro de cada rodada
		Map<Integer, List<Jogo>> jogosPorPrioridade = new TreeMap<>();
		jdbc.sql("""
				SELECT divisao_id, rodada, mandante_id, visitante_id FROM jogo_partida
				WHERE temporada_id = :temporada AND divisao_id IS NOT NULL ORDER BY divisao_id, rodada, id""")
			.param("temporada", linha.id())
			.query((rs, n) -> Map.entry(prioridades.get(rs.getLong("divisao_id")), new Jogo(rs.getInt("rodada"),
					clubes.get(rs.getLong("mandante_id")), clubes.get(rs.getLong("visitante_id")))))
			.list()
			.forEach(e -> jogosPorPrioridade.computeIfAbsent(e.getKey(), p -> new ArrayList<>()).add(e.getValue()));

		// beneficiados (rev. 108); temporada começada antes dela só tem o beneficiado antigo, que vale até a próxima
		Map<Integer, Set<Clube>> beneficiados = new HashMap<>();
		jdbc.sql("""
				SELECT divisao_id, clube_id FROM jogo_temporada_beneficiado WHERE temporada_id = :temporada
				UNION ALL
				SELECT d.divisao_id, d.beneficiado_id FROM jogo_temporada_divisao d
				WHERE d.temporada_id = :temporada AND d.beneficiado_id IS NOT NULL
				  AND NOT EXISTS (SELECT 1 FROM jogo_temporada_beneficiado b
				                  WHERE b.temporada_id = d.temporada_id AND b.divisao_id = d.divisao_id)""")
			.param("temporada", linha.id())
			.query((rs, n) -> Map.entry(prioridades.get(rs.getLong("divisao_id")), clubes.get(rs.getLong("clube_id"))))
			.list()
			.forEach(e -> beneficiados.computeIfAbsent(e.getKey(), p -> new HashSet<>()).add(e.getValue()));
		Map<Integer, SorteTemporada> sortes = new HashMap<>();
		prioridades.values().forEach(p -> sortes.put(p, new SorteTemporada(beneficiados.getOrDefault(p, Set.of()))));

		Map<Integer, Set<Long>> temporarios = new HashMap<>();
		Map<Integer, Set<Long>> extras = new HashMap<>();
		jdbc.sql("SELECT divisao_id, jogador_id, extra FROM jogo_bonus_artilharia WHERE temporada_id = :temporada")
			.param("temporada", linha.id())
			.query((rs, n) -> new LinhaBonus(prioridades.get(rs.getLong("divisao_id")), rs.getLong("jogador_id"),
					rs.getBoolean("extra")))
			.list()
			.forEach(b -> (b.extra() ? extras : temporarios).computeIfAbsent(b.prioridade(), p -> new HashSet<>())
				.add(b.jogadorId()));

		List<Temporada.SorteiosDivisao> divisoes = new ArrayList<>();
		jogosPorPrioridade.forEach((prioridade, lista) -> divisoes.add(new Temporada.SorteiosDivisao(prioridade,
				calendario(lista), sortes.get(prioridade), new BonusArtilharia(
						temporarios.getOrDefault(prioridade, Set.of()), extras.getOrDefault(prioridade, Set.of())))));
		return new Temporada(linha.numero(), linha.semente(), divisoes);
	}

	/** Remonta as rodadas; a folga é o clube da divisão que não joga na rodada. */
	private static Calendario calendario(List<Jogo> jogos) {
		Set<Clube> participantes = new LinkedHashSet<>();
		Map<Integer, List<Jogo>> porRodada = new TreeMap<>();
		for (Jogo jogo : jogos) {
			participantes.add(jogo.mandante());
			participantes.add(jogo.visitante());
			porRodada.computeIfAbsent(jogo.rodada(), r -> new ArrayList<>()).add(jogo);
		}
		List<Rodada> rodadas = new ArrayList<>();
		porRodada.forEach((numero, daRodada) -> {
			Set<Clube> folgam = new LinkedHashSet<>(participantes);
			daRodada.forEach(j -> {
				folgam.remove(j.mandante());
				folgam.remove(j.visitante());
			});
			rodadas.add(new Rodada(numero, daRodada, folgam.stream().findFirst()));
		});
		return new Calendario(rodadas);
	}

	private Optional<Integer> numeroAtual(long jogoId) {
		return jdbc.sql("SELECT max(numero) FROM jogo_temporada WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query((rs, n) -> rs.getObject(1, Integer.class))
			.optional();
	}

	private Map<Integer, Long> divisaoPorPrioridade(long jogoId) {
		Map<Integer, Long> ids = new HashMap<>();
		jdbc.sql("SELECT id, prioridade FROM jogo_divisao WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query((rs, n) -> ids.put(rs.getInt("prioridade"), rs.getLong("id")))
			.list();
		return ids;
	}

	private void inserirBonus(long temporadaId, long divisaoId, Set<Long> jogadores, boolean extra) {
		for (long jogador : jogadores) {
			jdbc.sql("""
					INSERT INTO jogo_bonus_artilharia (temporada_id, divisao_id, jogador_id, extra)
					VALUES (:temporada, :divisao, :jogador, :extra)""")
				.param("temporada", temporadaId)
				.param("divisao", divisaoId)
				.param("jogador", jogador)
				.param("extra", extra)
				.update();
		}
	}

	private record LinhaTemporada(long id, int numero, long semente) {
	}

	private record LinhaBonus(int prioridade, long jogadorId, boolean extra) {
	}
}
