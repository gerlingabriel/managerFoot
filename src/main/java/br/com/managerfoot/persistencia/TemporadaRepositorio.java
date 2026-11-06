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
import java.util.SplittableRandom;
import java.util.TreeMap;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.temporada.BonusArtilharia;
import br.com.managerfoot.temporada.BonusTreinador;
import br.com.managerfoot.temporada.Calendario;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.ParametrosBonusTreinador;
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
		return temporada.comBonusTreinador(bonusTreinadorAtual(jogoId));
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
		garantirBonusTreinador(jogoId);
	}

	/**
	 * Sorteia o bônus do treinador (SPEC §5.3.2, rev. 121) de cada clube de usuário que ainda não sorteou na
	 * temporada atual: no início da temporada, quando o usuário assume outro clube e, nos jogos salvos antes da rev.
	 * 121, na primeira rodada. Quem já sorteou não sorteia de novo; o clube deixado mantém os seus sorteados.
	 * Chamar antes de preparar qualquer partida, para a rodada ao vivo e o resultado gravado usarem o mesmo bônus.
	 * Também tira a penalização do prejudicado que o usuário assumiu (§5.3.1, rev. 122).
	 */
	@Transactional
	public void garantirBonusTreinador(long jogoId) {
		Optional<LinhaTemporada> atual = jdbc.sql("""
				SELECT id, numero, semente FROM jogo_temporada WHERE jogo_id = :jogo ORDER BY numero DESC LIMIT 1""")
			.param("jogo", jogoId)
			.query((rs, n) -> new LinhaTemporada(rs.getLong("id"), rs.getInt("numero"), rs.getLong("semente")))
			.optional();
		if (atual.isEmpty()) {
			return;
		}
		long temporadaId = atual.get().id();
		retirarPrejudicadoDeUsuario(jogoId, temporadaId);
		List<Long> semSorteio = jdbc.sql("""
				SELECT t.clube_id FROM jogo_treinador t
				WHERE t.jogo_id = :jogo AND t.clube_id IS NOT NULL
				  AND NOT EXISTS (SELECT 1 FROM jogo_bonus_treinador_sorteio s
				                  WHERE s.temporada_id = :temporada AND s.clube_id = t.clube_id)
				ORDER BY t.clube_id""")
			.param("jogo", jogoId)
			.param("temporada", temporadaId)
			.query(Long.class)
			.list();
		if (semSorteio.isEmpty()) {
			return;
		}
		Map<Long, List<Jogador>> planteis = jogos.carregarPlanteis(jogoId);
		Set<Long> jaTem = new HashSet<>(jdbc.sql("SELECT jogador_id FROM jogo_bonus_treinador WHERE temporada_id = :temporada")
			.param("temporada", temporadaId)
			.query(Long.class)
			.list());
		for (long clube : new LinkedHashSet<>(semSorteio)) {
			int inseridos = jdbc.sql("""
					INSERT INTO jogo_bonus_treinador_sorteio (temporada_id, clube_id) VALUES (:temporada, :clube)
					ON CONFLICT DO NOTHING""")
				.param("temporada", temporadaId)
				.param("clube", clube)
				.update();
			if (inseridos == 0) {
				continue; // outra transação sorteou antes
			}
			List<BonusTreinador.Sorteado> sorteados = BonusTreinador.sortear(planteis.getOrDefault(clube, List.of()), jaTem,
					ParametrosBonusTreinador.padrao(), new SplittableRandom(atual.get().semente() * 59 + clube));
			for (BonusTreinador.Sorteado sorteado : sorteados) {
				jdbc.sql("""
						INSERT INTO jogo_bonus_treinador (temporada_id, jogador_id, clube_id, forca_extra, extra)
						VALUES (:temporada, :jogador, :clube, :forca, :extra)""")
					.param("temporada", temporadaId)
					.param("jogador", sorteado.jogadorId())
					.param("clube", clube)
					.param("forca", sorteado.forcaExtra())
					.param("extra", sorteado.extra())
					.update();
				jaTem.add(sorteado.jogadorId());
			}
		}
	}

	/**
	 * Clube de usuário nunca é prejudicado (SPEC §5.3.1, rev. 122): o usuário que assume no meio da temporada um clube
	 * sorteado prejudicado tira a penalização de vez; ela não volta se ele sair, e a divisão não sorteia outro.
	 */
	private void retirarPrejudicadoDeUsuario(long jogoId, long temporadaId) {
		jdbc.sql("""
				DELETE FROM jogo_temporada_beneficiado b
				WHERE b.temporada_id = :temporada AND b.prejudicado
				  AND b.clube_id IN (SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo AND clube_id IS NOT NULL)""")
			.param("temporada", temporadaId)
			.param("jogo", jogoId)
			.update();
		// temporada começada antes da rev. 108, com o sorteio antigo
		jdbc.sql("""
				UPDATE jogo_temporada_divisao d SET prejudicado_id = NULL
				WHERE d.temporada_id = :temporada
				  AND d.prejudicado_id IN (SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo AND clube_id IS NOT NULL)""")
			.param("temporada", temporadaId)
			.param("jogo", jogoId)
			.update();
	}

	/** Bônus do treinador da temporada atual (SPEC §5.3.2, rev. 121); vazio sem temporada. */
	@Transactional(readOnly = true)
	public BonusTreinador bonusTreinadorAtual(long jogoId) {
		return BonusTreinador.de(jdbc.sql("""
				SELECT b.jogador_id, b.forca_extra, b.extra FROM jogo_bonus_treinador b
				WHERE b.temporada_id = (SELECT id FROM jogo_temporada WHERE jogo_id = :jogo ORDER BY numero DESC LIMIT 1)""")
			.param("jogo", jogoId)
			.query((rs, n) -> new BonusTreinador.Sorteado(rs.getLong("jogador_id"), rs.getInt("forca_extra"), rs.getBoolean("extra")))
			.list());
	}

	/**
	 * Jogadores com o asterisco temporário do bônus de artilharia (SPEC §5.2) que valem para o clube na
	 * temporada atual: os sorteados na divisão dele, como no motor. Sem o bônus extra (de quem já tem
	 * asterisco). Vazio se o clube está fora das divisões.
	 */
	public Set<Long> asteriscosDaArtilharia(long jogoId, long clubeId) {
		return asteriscosDaArtilharia(jdbc, jogoId, clubeId);
	}

	/** Como {@link #asteriscosDaArtilharia(long, long)}, para outros repositórios. */
	static Set<Long> asteriscosDaArtilharia(JdbcClient jdbc, long jogoId, long clubeId) {
		return Set.copyOf(jdbc.sql("""
				SELECT b.jogador_id FROM jogo_bonus_artilharia b JOIN jogo_clube c ON c.divisao_id = b.divisao_id
				WHERE c.id = :clube AND NOT b.extra
				  AND b.temporada_id = (SELECT id FROM jogo_temporada WHERE jogo_id = :jogo ORDER BY numero DESC LIMIT 1)""")
			.param("clube", clubeId).param("jogo", jogoId)
			.query(Long.class).list());
	}

	private void gravarBeneficiados(long temporadaId, long divisaoId, SorteTemporada sorte) {
		for (Clube clube : sorte.beneficiados()) {
			gravarSorte(temporadaId, divisaoId, clube, false);
		}
		for (Clube clube : sorte.prejudicados()) {
			gravarSorte(temporadaId, divisaoId, clube, true);
		}
	}

	private void gravarSorte(long temporadaId, long divisaoId, Clube clube, boolean prejudicado) {
		jdbc.sql("""
				INSERT INTO jogo_temporada_beneficiado (temporada_id, divisao_id, clube_id, prejudicado)
				VALUES (:temporada, :divisao, :clube, :prejudicado)""")
			.param("temporada", temporadaId)
			.param("divisao", divisaoId)
			.param("clube", clube.id())
			.param("prejudicado", prejudicado)
			.update();
	}

	private record LinhaSorte(int prioridade, Clube clube, boolean prejudicado) {
	}

	/** Clubes com treinador usuário: nunca prejudicados e, fracos, beneficiados (SPEC §5.3.1, rev. 108 e 109). */
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
		// com todos nos clubes, o bônus de cada treinador (rev. 121)
		garantirBonusTreinador(jogoId);
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

		// beneficiados e prejudicados (rev. 108 e 109); temporada começada antes da rev. 108 só tem o sorteio antigo
		Map<Integer, Set<Clube>> beneficiados = new HashMap<>();
		Map<Integer, Set<Clube>> prejudicados = new HashMap<>();
		jdbc.sql("""
				SELECT divisao_id, clube_id, prejudicado FROM jogo_temporada_beneficiado WHERE temporada_id = :temporada
				UNION ALL
				SELECT d.divisao_id, x.clube_id, x.prejudicado FROM jogo_temporada_divisao d
				CROSS JOIN LATERAL (VALUES (d.beneficiado_id, false), (d.prejudicado_id, true)) AS x (clube_id, prejudicado)
				WHERE d.temporada_id = :temporada AND x.clube_id IS NOT NULL
				  AND NOT EXISTS (SELECT 1 FROM jogo_temporada_beneficiado b
				                  WHERE b.temporada_id = d.temporada_id AND b.divisao_id = d.divisao_id)""")
			.param("temporada", linha.id())
			.query((rs, n) -> new LinhaSorte(prioridades.get(rs.getLong("divisao_id")), clubes.get(rs.getLong("clube_id")),
					rs.getBoolean("prejudicado")))
			.list()
			.forEach(l -> (l.prejudicado() ? prejudicados : beneficiados).computeIfAbsent(l.prioridade(), p -> new HashSet<>())
				.add(l.clube()));
		Map<Integer, SorteTemporada> sortes = new HashMap<>();
		prioridades.values().forEach(p -> sortes.put(p,
				new SorteTemporada(beneficiados.getOrDefault(p, Set.of()), prejudicados.getOrDefault(p, Set.of()))));

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
		return new Temporada(linha.numero(), linha.semente(), divisoes, bonusTreinadorAtual(jogoId));
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
