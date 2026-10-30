package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.bancopadrao.Regiao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.temporada.FaseCopa;
import br.com.managerfoot.temporada.Mundial;
import br.com.managerfoot.temporada.ParametrosContinental;
import br.com.managerfoot.temporada.PlacarCopa;

/**
 * Grava o Mundial da temporada (SPEC §8.3.4, rev. 88): sorteado ao fim da final da continental, com os jogos nos
 * confrontos da copa marcados pela competição. Jogo sem clubes das outras regiões fica sem Mundial.
 */
@Repository
public class MundialRepositorio {

	/** Mistura a semente da temporada para o sorteio, separado dos da liga, da copa e da continental. */
	private static final long MISTURA_MUNDIAL = 0xE7037ED1A0B428DBL;

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final double expoenteForca = ParametrosContinental.padrao().expoenteForca();

	public MundialRepositorio(JdbcClient jdbc, JogoRepositorio jogos) {
		this.jdbc = jdbc;
		this.jogos = jogos;
	}

	/**
	 * Sorteia e grava as semifinais.
	 *
	 * @return o Mundial, ou vazio se falta clube de alguma região
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public Optional<Mundial> criar(long jogoId, long temporadaId, long semente, Clube campeaoContinental) {
		Optional<Regiao> regiao = jdbc.sql("SELECT regiao FROM jogo_continental WHERE jogo_id = :jogo").param("jogo", jogoId)
			.query(String.class).optional().map(Regiao::valueOf);
		if (regiao.isEmpty()) {
			return Optional.empty();
		}
		Map<Long, Clube> clubes = clubes(jogoId);
		Map<Regiao, List<Clube>> candidatos = new EnumMap<>(Regiao.class);
		Map<Long, List<Jogador>> planteis = new HashMap<>();
		jdbc.sql("SELECT id, regiao_mundial FROM jogo_clube WHERE jogo_id = :jogo AND regiao_mundial IS NOT NULL ORDER BY id")
			.param("jogo", jogoId)
			.query(rs -> {
				long id = rs.getLong("id");
				candidatos.computeIfAbsent(Regiao.valueOf(rs.getString("regiao_mundial")), r -> new ArrayList<>()).add(clubes.get(id));
			});
		candidatos.values().forEach(l -> l.forEach(c -> planteis.put(c.id(), jogos.carregarPlantel(c.id()))));
		Optional<Mundial> mundial = Mundial.sortear(campeaoContinental, regiao.get(), candidatos, planteis, expoenteForca,
				new SplittableRandom(semente ^ MISTURA_MUNDIAL));
		mundial.ifPresent(m -> m.semifinais().forEach(c -> inserir(temporadaId, c, rodada(temporadaId, 0))));
		return mundial;
	}

	/** Cria a final depois das semifinais; o segundo pedido não faz nada. */
	@Transactional(propagation = Propagation.MANDATORY)
	public Mundial criarFinal(long temporadaId, Mundial mundial) {
		if (mundial.decisao().isPresent()) {
			return mundial;
		}
		Mundial comFinal = mundial.comFinal();
		inserir(temporadaId, comFinal.decisao().orElseThrow(), rodada(temporadaId, 1));
		return comFinal;
	}

	/** As etapas a jogar: nenhuma antes do sorteio ou sem Mundial. */
	@Transactional(readOnly = true)
	public List<FaseCopa> quadro(long temporadaId) {
		boolean existe = jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_copa_confronto WHERE temporada_id = :temporada AND competicao = 'MUNDIAL')")
			.param("temporada", temporadaId).query(Boolean.class).single();
		return existe ? Mundial.ETAPAS : List.of();
	}

	@Transactional(readOnly = true)
	public Optional<Mundial> carregar(long jogoId, long temporadaId) {
		Map<Long, Clube> clubes = clubes(jogoId);
		List<Mundial.Confronto> confrontos = jdbc.sql("""
				SELECT k.fase, k.ordem, k.mandante_id, k.visitante_id, p.gols_mandante, p.gols_visitante, p.prorrogacao,
				       p.penaltis_mandante, p.penaltis_visitante
				FROM jogo_copa_confronto k JOIN jogo_partida p ON p.confronto_id = k.id
				WHERE k.temporada_id = :temporada AND k.competicao = 'MUNDIAL'
				ORDER BY k.fase = 'FINAL', k.ordem""")
			.param("temporada", temporadaId)
			.query((rs, n) -> {
				Integer gols = rs.getObject("gols_mandante", Integer.class);
				Integer penaltis = rs.getObject("penaltis_mandante", Integer.class);
				Optional<PlacarCopa> placar = gols == null ? Optional.empty()
						: Optional.of(new PlacarCopa(gols, rs.getInt("gols_visitante"), rs.getBoolean("prorrogacao"),
								penaltis == null ? Optional.empty()
										: Optional.of(new PlacarCopa.Penaltis(penaltis, rs.getInt("penaltis_visitante")))));
				return new Mundial.Confronto(FaseCopa.valueOf(rs.getString("fase")), rs.getInt("ordem"),
						clubes.get(rs.getLong("mandante_id")), clubes.get(rs.getLong("visitante_id")), placar);
			})
			.list();
		if (confrontos.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new Mundial(confrontos.stream().filter(c -> c.fase() == FaseCopa.SEMIFINAL).toList(),
				confrontos.stream().filter(c -> c.fase() == FaseCopa.FINAL).findFirst()));
	}

	/** A rodada da etapa: depois da última da liga, para o extrato separar a semifinal da final. */
	public int rodada(long temporadaId, int indice) {
		return jdbc.sql("SELECT coalesce(max(rodada), 0) FROM jogo_partida WHERE temporada_id = :temporada AND divisao_id IS NOT NULL")
			.param("temporada", temporadaId).query(Integer.class).single() + indice + 1;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void gravarCampeao(long jogoId, int temporada, Clube campeao, Clube vice) {
		jdbc.sql("""
				INSERT INTO jogo_campeao_mundial (jogo_id, temporada, ano, clube_id, clube_nome, clube_pais, vice_id, vice_nome,
				                                  vice_pais, usuario_id, treinador_maquina_id, vice_treinador, vice_usuario_id)
				SELECT j.id, :temporada, j.ano_inicial + :temporada - 1, c.id, c.nome, c.pais_codigo, v.id, v.nome,
				       v.pais_codigo,
				       (SELECT t.usuario_id FROM jogo_treinador t WHERE t.clube_id = c.id),
				       CASE WHEN NOT EXISTS (SELECT 1 FROM jogo_treinador t WHERE t.clube_id = c.id)
				            THEN (SELECT m.id FROM jogo_treinador_maquina m WHERE m.clube_id = c.id) END,
				       coalesce((SELECT u.nome FROM jogo_treinador t JOIN usuario u ON u.id = t.usuario_id WHERE t.clube_id = v.id),
				            (SELECT m.nome FROM jogo_treinador_maquina m WHERE m.clube_id = v.id)),
				       (SELECT t.usuario_id FROM jogo_treinador t WHERE t.clube_id = v.id)
				FROM jogo j JOIN jogo_clube c ON c.id = :campeao JOIN jogo_clube v ON v.id = :vice
				WHERE j.id = :jogo""")
			.param("temporada", temporada)
			.param("campeao", campeao.id())
			.param("vice", vice.id())
			.param("jogo", jogoId)
			.update();
	}

	/**
	 * A aba Mundial (modelo 15a, rev. 88).
	 *
	 * @param ano ano da temporada
	 * @param participantes vazio até o sorteio, que vem depois da final da continental
	 */
	public record VisaoMundial(int ano, List<JogoNaTela> semifinais, JogoNaTela decisao, List<Participante> participantes, String campeao) {
	}

	/** @param placar nulo até o jogo; "1 x 1 · pên. 4 x 3" quando vai aos pênaltis */
	public record JogoNaTela(String primeiro, String paisPrimeiro, boolean primeiroSeu, boolean primeiroVenceu, String segundo,
			String paisSegundo, boolean segundoSeu, boolean segundoVenceu, String placar) {
	}

	public record Participante(String regiao, String clube, String pais, boolean seu, String comoChegou) {
	}

	@Transactional(readOnly = true)
	public VisaoMundial visao(long jogoId, long usuarioId) {
		record Temporada(long id, int ano) {
		}
		Temporada temporada = jdbc.sql("""
				SELECT t.id, j.ano_inicial + t.numero - 1 AS ano FROM jogo_temporada t JOIN jogo j ON j.id = t.jogo_id
				WHERE t.jogo_id = :jogo ORDER BY t.numero DESC LIMIT 1""")
			.param("jogo", jogoId).query((rs, n) -> new Temporada(rs.getLong("id"), rs.getInt("ano"))).single();
		long seu = jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo AND usuario_id = :usuario")
			.param("jogo", jogoId).param("usuario", usuarioId).query(Long.class).single();
		Optional<Mundial> mundial = carregar(jogoId, temporada.id());
		if (mundial.isEmpty()) {
			return new VisaoMundial(temporada.ano(), List.of(), null, List.of(), null);
		}
		Map<Long, String> paises = new HashMap<>();
		Map<Long, Regiao> regioes = new HashMap<>();
		jdbc.sql("SELECT id, pais_codigo, regiao_mundial FROM jogo_clube WHERE jogo_id = :jogo AND pais_codigo IS NOT NULL")
			.param("jogo", jogoId)
			.query(rs -> {
				paises.put(rs.getLong("id"), rs.getString("pais_codigo"));
				if (rs.getString("regiao_mundial") != null) {
					regioes.put(rs.getLong("id"), Regiao.valueOf(rs.getString("regiao_mundial")));
				}
			});
		Regiao regiaoJogada = Regiao.valueOf(jdbc.sql("SELECT regiao FROM jogo_continental WHERE jogo_id = :jogo")
			.param("jogo", jogoId).query(String.class).single());
		Mundial m = mundial.get();
		List<Participante> participantes = new ArrayList<>();
		m.semifinais().forEach(c -> List.of(c.primeiro(), c.segundo()).forEach(clube -> {
			Regiao regiao = regioes.get(clube.id());
			participantes.add(new Participante(nomeDaRegiao(regiao == null ? regiaoJogada : regiao), clube.nome(), paises.get(clube.id()),
					clube.id() == seu, regiao == null ? "Campeão da continental" : "Por força com sorteio"));
		}));
		return new VisaoMundial(temporada.ano(), m.semifinais().stream().map(c -> naTela(c, paises, seu)).toList(),
				m.decisao().map(c -> naTela(c, paises, seu)).orElse(null), participantes,
				m.campeao().map(Clube::nome).orElse(null));
	}

	private static JogoNaTela naTela(Mundial.Confronto c, Map<Long, String> paises, long seu) {
		Optional<Clube> vencedor = c.vencedor();
		String placar = c.placar().map(p -> p.golsMandante() + " x " + p.golsVisitante()
				+ p.penaltis().map(q -> " · pên. " + q.mandante() + " x " + q.visitante()).orElse("")).orElse(null);
		return new JogoNaTela(c.primeiro().nome(), paises.get(c.primeiro().id()), c.primeiro().id() == seu,
				vencedor.filter(c.primeiro()::equals).isPresent(), c.segundo().nome(), paises.get(c.segundo().id()),
				c.segundo().id() == seu, vencedor.filter(c.segundo()::equals).isPresent(), placar);
	}

	private static String nomeDaRegiao(Regiao regiao) {
		return switch (regiao) {
			case AMERICA_DO_SUL -> "América do Sul";
			case EUROPA -> "Europa";
			case AMERICA_DO_NORTE -> "América do Norte e Central";
			case ASIA -> "Ásia";
			case AFRICA -> "África";
			case OCEANIA -> "Oceania";
		};
	}

	private void inserir(long temporadaId, Mundial.Confronto c, int rodada) {
		long id = TabelasLiga.inserir(jdbc, """
				INSERT INTO jogo_copa_confronto (temporada_id, competicao, fase, ordem, mandante_id, visitante_id)
				VALUES (:temporada, 'MUNDIAL', :fase, :ordem, :mandante, :visitante)""",
				Map.of("temporada", temporadaId, "fase", c.fase().name(), "ordem", c.ordem(), "mandante", c.primeiro().id(),
						"visitante", c.segundo().id()));
		jdbc.sql("""
				INSERT INTO jogo_partida (temporada_id, confronto_id, perna, rodada, mandante_id, visitante_id)
				VALUES (:temporada, :confronto, 'JOGO_UNICO', :rodada, :mandante, :visitante)""")
			.param("temporada", temporadaId)
			.param("confronto", id)
			.param("rodada", rodada)
			.param("mandante", c.primeiro().id())
			.param("visitante", c.segundo().id())
			.update();
	}

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
