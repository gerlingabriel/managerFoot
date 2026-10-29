package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.ConfrontoCopa;
import br.com.managerfoot.temporada.Copa;
import br.com.managerfoot.temporada.FaseCopa;
import br.com.managerfoot.temporada.ParametrosCopa;
import br.com.managerfoot.temporada.PlacarCopa;

/**
 * Copa nacional da temporada (SPEC §8.2; migração V30): a chave sorteada no início da temporada, os
 * confrontos de cada fase e as partidas deles no calendário. Não abre transação de escrita: quem
 * chama abre e trava o jogo.
 */
@Repository
public class CopaRepositorio {

	/** Mistura a semente da temporada para o sorteio da copa, separado dos da liga. */
	private static final long MISTURA_COPA = 0xC2B2AE3D27D4EB4FL;

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final ParametrosCopa parametros = ParametrosCopa.padrao();

	public CopaRepositorio(JdbcClient jdbc, JogoRepositorio jogos) {
		this.jdbc = jdbc;
		this.jogos = jogos;
	}

	/**
	 * Sorteia a copa da temporada recém-gravada, com os clubes das divisões (sem a fila) e a força em
	 * campo de agora. Uma liga que termina antes do quadro da copa fica sem copa.
	 *
	 * @return a copa criada, ou vazio se a liga não comporta uma
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public Optional<Copa> criar(long jogoId, long temporadaId, long semente) {
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		List<Clube> clubes = liga.divisoes().stream().flatMap(d -> d.clubes().stream()).toList();
		SplittableRandom aleatorio = new SplittableRandom(semente ^ MISTURA_COPA);
		List<Clube> inscritos = Copa.inscritos(clubes, jogos.carregarPlanteis(jogoId), liga.divisoes().size(),
				parametros, aleatorio);
		Copa copa = Copa.sortear(inscritos, aleatorio);
		int ultimaRodada = ultimaRodada(temporadaId);
		try {
			Copa.quadro(copa.primeiraFase(), ultimaRodada, parametros);
		}
		catch (IllegalArgumentException e) {
			return Optional.empty();
		}
		inserirConfrontos(temporadaId, copa.confrontosDa(copa.faseAtual()), ultimaRodada);
		return Optional.of(copa);
	}

	/** A copa da temporada como está gravada; vazio se a temporada não tem copa. */
	@Transactional(readOnly = true)
	public Optional<Copa> carregar(long jogoId, long temporadaId) {
		Map<Long, Clube> clubes = clubes(jogoId);
		Map<Long, Placares> placares = new HashMap<>();
		jdbc.sql("""
				SELECT confronto_id, perna, gols_mandante, gols_visitante, prorrogacao, penaltis_mandante, penaltis_visitante
				FROM jogo_partida WHERE temporada_id = :temporada AND confronto_id IS NOT NULL AND gols_mandante IS NOT NULL""")
			.param("temporada", temporadaId)
			.query(rs -> {
				Integer penMandante = rs.getObject("penaltis_mandante", Integer.class);
				PlacarCopa placar = new PlacarCopa(rs.getInt("gols_mandante"), rs.getInt("gols_visitante"),
						rs.getBoolean("prorrogacao"), penMandante == null ? Optional.empty()
								: Optional.of(new PlacarCopa.Penaltis(penMandante, rs.getInt("penaltis_visitante"))));
				Placares p = placares.computeIfAbsent(rs.getLong("confronto_id"), k -> new Placares());
				if (Copa.Perna.valueOf(rs.getString("perna")) == Copa.Perna.VOLTA) {
					p.volta = placar;
				} else {
					p.ida = placar;
				}
			});
		List<ConfrontoCopa> confrontos = jdbc.sql("""
				SELECT id, fase, ordem, mandante_id, visitante_id FROM jogo_copa_confronto
				WHERE temporada_id = :temporada""")
			.param("temporada", temporadaId)
			.query((rs, n) -> {
				Placares p = placares.getOrDefault(rs.getLong("id"), new Placares());
				return new ConfrontoCopa(FaseCopa.valueOf(rs.getString("fase")), rs.getInt("ordem"),
						clubes.get(rs.getLong("mandante_id")), clubes.get(rs.getLong("visitante_id")),
						Optional.ofNullable(p.ida), Optional.ofNullable(p.volta));
			})
			.list();
		return confrontos.isEmpty() ? Optional.empty() : Optional.of(new Copa(confrontos));
	}

	/** Etapas do quadro da copa da temporada; vazio se ela não tem copa. */
	@Transactional(readOnly = true)
	public List<Copa.Etapa> quadro(long jogoId, long temporadaId) {
		return carregar(jogoId, temporadaId)
			.map(c -> Copa.quadro(c.primeiraFase(), ultimaRodada(temporadaId), parametros))
			.orElse(List.of());
	}

	/**
	 * Se a fase atual terminou, cria os confrontos da seguinte e as partidas deles, com o mando da ida
	 * sorteado pela semente da temporada e pela fase.
	 *
	 * @return a copa depois do avanço (a mesma, se a fase não terminou)
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public Copa avancarFase(long temporadaId, Copa copa, long semente) {
		if (!copa.podeAvancar()) {
			return copa;
		}
		FaseCopa seguinte = copa.faseAtual().seguinte().orElseThrow();
		Copa nova = copa.avancar(new SplittableRandom((semente ^ MISTURA_COPA) * 31 + seguinte.ordinal()));
		inserirConfrontos(temporadaId, nova.confrontosDa(seguinte), ultimaRodada(temporadaId));
		return nova;
	}

	private void inserirConfrontos(long temporadaId, List<ConfrontoCopa> confrontos, int ultimaRodada) {
		Map<FaseCopa, Map<Copa.Perna, Integer>> rodadas = new HashMap<>();
		Copa.quadro(FaseCopa.DEZESSEIS_AVOS, ultimaRodada, parametros)
			.forEach(e -> rodadas.computeIfAbsent(e.fase(), f -> new HashMap<>()).put(e.perna(), e.depoisDaRodada()));
		for (ConfrontoCopa c : confrontos) {
			long id = TabelasLiga.inserir(jdbc, """
					INSERT INTO jogo_copa_confronto (temporada_id, fase, ordem, mandante_id, visitante_id)
					VALUES (:temporada, :fase, :ordem, :mandante, :visitante)""",
					Map.of("temporada", temporadaId, "fase", c.fase().name(), "ordem", c.ordem(), "mandante",
							c.mandante().id(), "visitante", c.visitante().id()));
			Map<Copa.Perna, Integer> daFase = rodadas.get(c.fase());
			if (c.fase().jogoUnico()) {
				inserirPartida(temporadaId, id, Copa.Perna.JOGO_UNICO, daFase.get(Copa.Perna.JOGO_UNICO), c.mandante(),
						c.visitante());
			} else {
				inserirPartida(temporadaId, id, Copa.Perna.IDA, daFase.get(Copa.Perna.IDA), c.mandante(), c.visitante());
				inserirPartida(temporadaId, id, Copa.Perna.VOLTA, daFase.get(Copa.Perna.VOLTA), c.visitante(),
						c.mandante());
			}
		}
	}

	private void inserirPartida(long temporadaId, long confrontoId, Copa.Perna perna, int rodada, Clube mandante,
			Clube visitante) {
		jdbc.sql("""
				INSERT INTO jogo_partida (temporada_id, confronto_id, perna, rodada, mandante_id, visitante_id)
				VALUES (:temporada, :confronto, :perna, :rodada, :mandante, :visitante)""")
			.param("temporada", temporadaId)
			.param("confronto", confrontoId)
			.param("perna", perna.name())
			.param("rodada", rodada)
			.param("mandante", mandante.id())
			.param("visitante", visitante.id())
			.update();
	}

	/** A última rodada da liga na temporada, depois da qual se joga a final. */
	private int ultimaRodada(long temporadaId) {
		return jdbc.sql("SELECT coalesce(max(rodada), 0) FROM jogo_partida WHERE temporada_id = :temporada AND confronto_id IS NULL")
			.param("temporada", temporadaId)
			.query(Integer.class)
			.single();
	}

	/** Todos os clubes do jogo, inclusive os da fila: um clube da copa pode ter caído para ela depois. */
	private Map<Long, Clube> clubes(long jogoId) {
		Map<Long, Clube> clubes = new HashMap<>();
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		List<Clube> todos = new ArrayList<>(liga.filaDeEspera());
		liga.divisoes().forEach(d -> todos.addAll(d.clubes()));
		todos.forEach(c -> clubes.put(c.id(), c));
		return clubes;
	}

	private static final class Placares {
		PlacarCopa ida;
		PlacarCopa volta;
	}
}
