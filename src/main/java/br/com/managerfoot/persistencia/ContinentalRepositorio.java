package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.TreeMap;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.ConfrontoCopa;
import br.com.managerfoot.temporada.Continental;
import br.com.managerfoot.temporada.Copa;
import br.com.managerfoot.temporada.FaseCopa;
import br.com.managerfoot.temporada.GrupoContinental;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.LinhaClassificacao;
import br.com.managerfoot.temporada.ParametrosContinental;
import br.com.managerfoot.temporada.PlacarCopa;
import br.com.managerfoot.temporada.ResultadoJogo;
import br.com.managerfoot.temporada.VagasContinental;

/**
 * Competição continental da temporada (SPEC §8.3.6; V33 e V34): quem ocupa as vagas, os grupos
 * sorteados no início da temporada, as partidas deles e os confrontos do mata-mata. Não abre
 * transação de escrita: quem chama abre e trava o jogo.
 */
@Repository
public class ContinentalRepositorio {

	/** Mistura a semente da temporada para os sorteios da continental, separados dos da liga e da copa. */
	private static final long MISTURA_CONTINENTAL = 0x9E3779B97F4A7C15L;

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final ParametrosContinental parametros = ParametrosContinental.padrao();

	public ContinentalRepositorio(JdbcClient jdbc, JogoRepositorio jogos) {
		this.jdbc = jdbc;
		this.jogos = jogos;
	}

	/**
	 * Escolhe os clubes, sorteia os grupos e grava as partidas da temporada recém-gravada. Fica sem
	 * continental o jogo sem competição da região (criado antes da V33), a liga que termina antes do
	 * quadro e a edição sem os 32 clubes (§8.3.2 item 5).
	 *
	 * @param numero o número da temporada; na 1ª, o país jogado escolhe por força com sorteio
	 * @return a competição criada, ou vazio
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public Optional<Continental> criar(long jogoId, long temporadaId, int numero, long semente) {
		Map<String, Integer> vagas = vagas(jogoId);
		if (vagas.isEmpty()) {
			return Optional.empty();
		}
		int ultimaRodada = ultimaRodada(temporadaId);
		try {
			Continental.quadro(ultimaRodada, parametros);
		}
		catch (IllegalArgumentException e) {
			return Optional.empty();
		}
		SplittableRandom aleatorio = new SplittableRandom(semente ^ MISTURA_CONTINENTAL);
		String paisJogado = jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = :jogo").param("jogo", jogoId)
			.query(String.class).single();
		Map<Long, List<Jogador>> planteis = new HashMap<>(jogos.carregarPlanteis(jogoId));
		Map<String, List<Clube>> estrangeiros = estrangeiros(jogoId, planteis);
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);

		List<Clube> inscritos = new ArrayList<>();
		Map<Long, String> paises = new HashMap<>();
		for (Map.Entry<String, Integer> vaga : vagas.entrySet()) {
			List<Clube> doPais;
			if (vaga.getKey().equals(paisJogado)) {
				doPais = numero == 1
						? VagasContinental.porForca(liga.divisoes().getFirst().clubes(), planteis, vaga.getValue(),
								parametros.expoenteForca(), aleatorio)
						: VagasContinental.peloDesempenho(primeiraDivisaoAnterior(jogoId, numero - 1, liga),
								campeaoDaCopa(jogoId, numero - 1, liga), vaga.getValue());
			} else {
				doPais = VagasContinental.porForca(estrangeiros.getOrDefault(vaga.getKey(), List.of()), planteis,
						vaga.getValue(), parametros.expoenteForca(), aleatorio);
			}
			doPais.forEach(c -> paises.put(c.id(), vaga.getKey()));
			inscritos.addAll(doPais);
		}
		if (inscritos.size() != parametros.clubes()) {
			return Optional.empty();
		}
		Continental continental = Continental.sortear(inscritos, paises, planteis, parametros, aleatorio);
		gravarGrupos(temporadaId, continental, Continental.quadro(ultimaRodada, parametros));
		return Optional.of(continental);
	}

	/** A competição da temporada como está gravada; vazio se a temporada não tem continental. */
	@Transactional(readOnly = true)
	public Optional<Continental> carregar(long jogoId, long temporadaId) {
		Map<Long, Clube> clubes = clubes(jogoId);
		Map<Integer, List<Clube>> membros = new TreeMap<>();
		jdbc.sql("SELECT grupo, clube_id FROM jogo_continental_grupo WHERE temporada_id = :temporada ORDER BY grupo, pote")
			.param("temporada", temporadaId)
			.query(rs -> {
				membros.computeIfAbsent(rs.getInt("grupo"), g -> new ArrayList<>()).add(clubes.get(rs.getLong("clube_id")));
			});
		if (membros.isEmpty()) {
			return Optional.empty();
		}
		Map<Integer, List<GrupoContinental.PartidaGrupo>> partidas = new TreeMap<>();
		jdbc.sql("""
				SELECT grupo, jogo_do_grupo, mandante_id, visitante_id, gols_mandante, gols_visitante FROM jogo_partida
				WHERE temporada_id = :temporada AND grupo IS NOT NULL ORDER BY grupo, jogo_do_grupo, id""")
			.param("temporada", temporadaId)
			.query(rs -> {
				Integer gm = rs.getObject("gols_mandante", Integer.class);
				partidas.computeIfAbsent(rs.getInt("grupo"), g -> new ArrayList<>())
					.add(new GrupoContinental.PartidaGrupo(rs.getInt("jogo_do_grupo"), clubes.get(rs.getLong("mandante_id")),
							clubes.get(rs.getLong("visitante_id")), gm == null ? Optional.empty()
									: Optional.of(new GrupoContinental.Placar(gm, rs.getInt("gols_visitante")))));
			});
		List<GrupoContinental> grupos = new ArrayList<>();
		membros.forEach((g, lista) -> grupos.add(new GrupoContinental(g, lista, partidas.getOrDefault(g, List.of()))));
		return Optional.of(new Continental(grupos, mataMata(temporadaId, clubes)));
	}

	/** Etapas do quadro da continental da temporada; vazio se ela não tem continental. */
	@Transactional(readOnly = true)
	public List<Continental.Etapa> quadro(long temporadaId) {
		boolean tem = jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_continental_grupo WHERE temporada_id = :temporada)")
			.param("temporada", temporadaId).query(Boolean.class).single();
		return tem ? Continental.quadro(ultimaRodada(temporadaId), parametros) : List.of();
	}

	/** Vagas do país jogado e dos outros, na ordem da tabela; vazio se o jogo não tem continental. */
	private Map<String, Integer> vagas(long jogoId) {
		Map<String, Integer> vagas = new LinkedHashMap<>();
		jdbc.sql("SELECT pais_codigo, vagas FROM jogo_continental_vaga WHERE jogo_id = :jogo ORDER BY ordem")
			.param("jogo", jogoId)
			.query(rs -> {
				vagas.put(rs.getString("pais_codigo"), rs.getInt("vagas"));
			});
		return vagas;
	}

	/** Clubes estrangeiros por país, com os planteis acrescentados a {@code planteis}. */
	private Map<String, List<Clube>> estrangeiros(long jogoId, Map<Long, List<Jogador>> planteis) {
		Map<String, List<Clube>> porPais = new LinkedHashMap<>();
		jdbc.sql("""
				SELECT id, nome, forca, escudo_modelo, escudo_cor_1, escudo_cor_2, pais_codigo FROM jogo_clube
				WHERE jogo_id = :jogo AND pais_codigo IS NOT NULL ORDER BY id""")
			.param("jogo", jogoId)
			.query(rs -> {
				Clube clube = TabelasLiga.clube(rs);
				porPais.computeIfAbsent(rs.getString("pais_codigo"), p -> new ArrayList<>()).add(clube);
			});
		porPais.values().forEach(l -> l.forEach(c -> planteis.put(c.id(), TabelasLiga.JOGO.carregarPlantel(jdbc, c.id()))));
		return porPais;
	}

	/** A 1ª divisão da temporada {@code numero} na ordem final, recalculada dos placares. */
	private List<Clube> primeiraDivisaoAnterior(long jogoId, int numero, ConfiguracaoLiga liga) {
		Map<Long, Clube> clubes = new HashMap<>();
		liga.divisoes().forEach(d -> d.clubes().forEach(c -> clubes.put(c.id(), c)));
		liga.filaDeEspera().forEach(c -> clubes.put(c.id(), c));
		long[] semente = { 0 };
		List<ResultadoJogo> resultados = new ArrayList<>();
		jdbc.sql("""
				SELECT t.semente, p.rodada, p.mandante_id, p.visitante_id, p.gols_mandante, p.gols_visitante
				FROM jogo_partida p JOIN jogo_temporada t ON t.id = p.temporada_id JOIN jogo_divisao d ON d.id = p.divisao_id
				WHERE t.jogo_id = :jogo AND t.numero = :numero AND d.prioridade = 1 AND p.gols_mandante IS NOT NULL""")
			.param("jogo", jogoId)
			.param("numero", numero)
			.query(rs -> {
				semente[0] = rs.getLong("semente");
				resultados.add(new ResultadoJogo(new Jogo(rs.getInt("rodada"), clubes.get(rs.getLong("mandante_id")),
						clubes.get(rs.getLong("visitante_id"))), rs.getInt("gols_mandante"), rs.getInt("gols_visitante")));
			});
		List<Clube> participantes = resultados.stream()
			.flatMap(r -> java.util.stream.Stream.of(r.jogo().mandante(), r.jogo().visitante()))
			.distinct()
			.sorted(Comparator.comparingLong(Clube::id))
			.toList();
		return Classificacao.calcular(participantes, resultados, semente[0]).linhas().stream()
			.map(LinhaClassificacao::clube)
			.toList();
	}

	private Optional<Clube> campeaoDaCopa(long jogoId, int numero, ConfiguracaoLiga liga) {
		return jdbc.sql("SELECT clube_id FROM jogo_campeao_copa WHERE jogo_id = :jogo AND temporada = :numero")
			.param("jogo", jogoId)
			.param("numero", numero)
			.query(Long.class)
			.optional()
			.flatMap(id -> java.util.stream.Stream.concat(liga.divisoes().stream().flatMap(d -> d.clubes().stream()),
					liga.filaDeEspera().stream()).filter(c -> c.id() == id).findFirst());
	}

	private void gravarGrupos(long temporadaId, Continental continental, List<Continental.Etapa> quadro) {
		for (GrupoContinental grupo : continental.grupos()) {
			for (int pote = 0; pote < grupo.clubes().size(); pote++) {
				jdbc.sql("""
						INSERT INTO jogo_continental_grupo (temporada_id, clube_id, grupo, pote)
						VALUES (:temporada, :clube, :grupo, :pote)""")
					.param("temporada", temporadaId)
					.param("clube", grupo.clubes().get(pote).id())
					.param("grupo", grupo.indice())
					.param("pote", pote)
					.update();
			}
			for (GrupoContinental.PartidaGrupo p : grupo.partidas()) {
				jdbc.sql("""
						INSERT INTO jogo_partida (temporada_id, grupo, jogo_do_grupo, rodada, mandante_id, visitante_id)
						VALUES (:temporada, :grupo, :jogo, :rodada, :mandante, :visitante)""")
					.param("temporada", temporadaId)
					.param("grupo", grupo.indice())
					.param("jogo", p.jogo())
					.param("rodada", quadro.get(p.jogo() - 1).depoisDaRodada())
					.param("mandante", p.mandante().id())
					.param("visitante", p.visitante().id())
					.update();
			}
		}
	}

	/** O mata-mata gravado, com os placares das partidas; vazio antes do fim dos grupos. */
	private Optional<Copa> mataMata(long temporadaId, Map<Long, Clube> clubes) {
		Map<Long, PlacarCopa[]> placares = new HashMap<>();
		jdbc.sql("""
				SELECT p.confronto_id, p.perna, p.gols_mandante, p.gols_visitante, p.prorrogacao, p.penaltis_mandante,
				       p.penaltis_visitante
				FROM jogo_partida p JOIN jogo_copa_confronto k ON k.id = p.confronto_id
				WHERE p.temporada_id = :temporada AND k.competicao = 'CONTINENTAL' AND p.gols_mandante IS NOT NULL""")
			.param("temporada", temporadaId)
			.query(rs -> {
				Integer pm = rs.getObject("penaltis_mandante", Integer.class);
				PlacarCopa placar = new PlacarCopa(rs.getInt("gols_mandante"), rs.getInt("gols_visitante"),
						rs.getBoolean("prorrogacao"), pm == null ? Optional.empty()
								: Optional.of(new PlacarCopa.Penaltis(pm, rs.getInt("penaltis_visitante"))));
				placares.computeIfAbsent(rs.getLong("confronto_id"), k -> new PlacarCopa[2])[
						Copa.Perna.valueOf(rs.getString("perna")) == Copa.Perna.VOLTA ? 1 : 0] = placar;
			});
		List<ConfrontoCopa> confrontos = jdbc.sql("""
				SELECT id, fase, ordem, mandante_id, visitante_id FROM jogo_copa_confronto
				WHERE temporada_id = :temporada AND competicao = 'CONTINENTAL'""")
			.param("temporada", temporadaId)
			.query((rs, n) -> {
				PlacarCopa[] p = placares.getOrDefault(rs.getLong("id"), new PlacarCopa[2]);
				return new ConfrontoCopa(FaseCopa.valueOf(rs.getString("fase")), rs.getInt("ordem"),
						clubes.get(rs.getLong("mandante_id")), clubes.get(rs.getLong("visitante_id")),
						Optional.ofNullable(p[0]), Optional.ofNullable(p[1]));
			})
			.list();
		return confrontos.isEmpty() ? Optional.empty() : Optional.of(new Copa(confrontos));
	}

	/** A última rodada da liga na temporada, depois da qual se joga a final. */
	private int ultimaRodada(long temporadaId) {
		return jdbc.sql("SELECT coalesce(max(rodada), 0) FROM jogo_partida WHERE temporada_id = :temporada AND divisao_id IS NOT NULL")
			.param("temporada", temporadaId)
			.query(Integer.class)
			.single();
	}

	/** Todos os clubes do jogo, do país jogado e de fora. */
	private Map<Long, Clube> clubes(long jogoId) {
		Map<Long, Clube> clubes = new HashMap<>();
		jdbc.sql("SELECT id, nome, forca, escudo_modelo, escudo_cor_1, escudo_cor_2 FROM jogo_clube WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query(rs -> {
				Clube c = TabelasLiga.clube(rs);
				clubes.put(c.id(), c);
			});
		return clubes;
	}
}
