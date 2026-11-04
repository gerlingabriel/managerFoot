package br.com.managerfoot.persistencia;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.multiplayer.Turno;
import br.com.managerfoot.multiplayer.Turno.Passo;
import br.com.managerfoot.multiplayer.Turno.Treinador;
import br.com.managerfoot.persistencia.LeilaoRepositorio.Etapa;
import br.com.managerfoot.persistencia.RodadaRepositorio.Proximo;

/**
 * O OK de todos no jogo em rede depois que a temporada começou (SPEC §3.3 itens 4 a 6, rev. 90 e 92;
 * parte 5b, docs/decisions/0021). Cada OK vale para um momento do jogo; quem dá o último OK faz o jogo
 * seguir, na mesma transação, com a linha do jogo travada: dois pedidos não avançam o mesmo passo.
 *
 * <p>Momentos, por temporada: a tática de cada rodada ou etapa de copa, continental e Mundial; os lances
 * dos leilões antes da rodada; o resultado dos leilões ("Ir para a rodada"); e o fim da temporada.
 */
@Repository
public class TurnoRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final RodadaRepositorio rodadas;
	private final LeilaoRepositorio leiloes;
	private final RodadaAoVivoRepositorio aoVivo;
	private final FimDeTemporadaRepositorio fimDeTemporada;

	public TurnoRepositorio(JdbcClient jdbc, JogoRepositorio jogos, RodadaRepositorio rodadas, LeilaoRepositorio leiloes,
			RodadaAoVivoRepositorio aoVivo, FimDeTemporadaRepositorio fimDeTemporada) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.rodadas = rodadas;
		this.leiloes = leiloes;
		this.aoVivo = aoVivo;
		this.fimDeTemporada = fimDeTemporada;
	}

	/** O que aconteceu com o OK. */
	public enum Desfecho {
		/** Falta o OK de alguém. */
		AGUARDANDO,
		/** Todos deram OK e os leilões abriram: cada um dá os lances. */
		LANCES,
		/** Os leilões foram resolvidos: cada um vê o resultado. */
		RESULTADO,
		/** A rodada ou a etapa começou ao vivo. */
		AO_VIVO,
		/** Começou a temporada seguinte. */
		TEMPORADA
	}

	/** Jogo em rede com a temporada já começada (fora da sala). */
	public boolean emRede(long jogoId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_rede WHERE jogo_id = :jogo AND comecou_em IS NOT NULL)")
			.param("jogo", jogoId)
			.query(Boolean.class)
			.single();
	}

	/** Os treinadores e o OK de cada um no momento atual; vazio fora do jogo em rede ou com a rodada ao vivo. */
	@Transactional(readOnly = true)
	public Optional<Turno> turno(long jogoId) {
		return momento(jogoId).map(m -> new Turno(m.chave(), m.passo(), treinadores(jogoId, m.chave())));
	}

	/** Se o treinador deu OK e ainda espera os outros: a escalação dele fica travada. */
	public boolean okDado(long jogoId, long usuarioId) {
		return jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM jogo_rede_participante
				               WHERE jogo_id = :jogo AND usuario_id = :usuario AND ok_momento IS NOT NULL)""")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.query(Boolean.class)
			.single();
	}

	/**
	 * O OK do treinador no passo em que o jogo está. Se for o último, o jogo segue: abre os leilões ou
	 * começa a rodada ao vivo, resolve os leilões ou começa a temporada seguinte. Repetir o OK não muda nada.
	 *
	 * @param passo o que a tela de quem pede confirma; se o jogo já está em outro passo, é recusado
	 * @param semente sorteio da temporada seguinte, se o OK for o do fim da temporada
	 * @throws IllegalStateException se o jogo não está nesse passo (outro já fez o jogo seguir)
	 * @throws NoSuchElementException se o usuário não treina no jogo em rede
	 */
	@Transactional
	public Desfecho darOk(long jogoId, long usuarioId, Passo passo, long semente, Instant agora) {
		jogos.travar(jogoId);
		Momento m = momento(jogoId).filter(x -> x.passo() == passo)
			.orElseThrow(() -> new IllegalStateException("o jogo " + jogoId + " não espera o OK de " + passo));
		int gravados = jdbc.sql("""
				UPDATE jogo_rede_participante SET ok_momento = :momento, ok_em = coalesce(ok_em, :agora)
				WHERE jogo_id = :jogo AND usuario_id = :usuario""")
			.param("momento", m.chave())
			.param("agora", hora(agora))
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.update();
		if (gravados == 0) {
			throw new NoSuchElementException("o usuário " + usuarioId + " não está no jogo " + jogoId);
		}
		return todosDeramOk(jogoId, m.chave()) ? seguir(jogoId, usuarioId, m, semente, agora) : Desfecho.AGUARDANDO;
	}

	/**
	 * Os lances secretos do treinador e o OK dele nos leilões (SPEC §3.3 item 6). Antes do OK, os lances
	 * podem ser trocados ("Mudar meus lances" tira o OK).
	 *
	 * @throws IllegalArgumentException se um lance não vale; nada é gravado
	 * @throws IllegalStateException se o jogo não está nos lances desta rodada
	 */
	@Transactional
	public Desfecho darLances(long jogoId, long usuarioId, int rodada, java.util.Map<Long, Long> lances, Instant agora) {
		jogos.travar(jogoId);
		Momento m = momento(jogoId).filter(x -> x.passo() == Passo.LANCES && x.proximo() instanceof Proximo.Liga(int r) && r == rodada)
			.orElseThrow(() -> new IllegalStateException("o jogo " + jogoId + " não espera lances antes da rodada " + rodada));
		if (okDado(jogoId, usuarioId)) {
			throw new IllegalArgumentException("Seus lances já foram dados. Para mudar, clique em \"Mudar meus lances\".");
		}
		leiloes.guardarLances(jogoId, usuarioId, rodada, lances);
		return darOk(jogoId, usuarioId, m.passo(), 0, agora);
	}

	/** "Tirar meu OK e mexer" (SPEC §3.3 item 5, rev. 92): só enquanto falta alguém, porque depois o jogo já seguiu. */
	@Transactional
	public void tirarOk(long jogoId, long usuarioId) {
		jogos.travar(jogoId);
		jdbc.sql("UPDATE jogo_rede_participante SET ok_momento = NULL, ok_em = NULL WHERE jogo_id = :jogo AND usuario_id = :usuario")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.update();
	}

	/** Todos deram OK: o jogo segue e os OK são apagados para o próximo momento. */
	private Desfecho seguir(long jogoId, long usuarioId, Momento m, long semente, Instant agora) {
		jdbc.sql("UPDATE jogo_rede_participante SET ok_momento = NULL, ok_em = NULL WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.update();
		return switch (m.passo()) {
			case PLANTEL -> switch (m.proximo()) {
				case Proximo.Liga(int rodada) -> abrirLeiloes(jogoId, usuarioId, m.temporada(), rodada, agora);
				case Proximo.EtapaCopa(int indice, var etapa) -> {
					aoVivo.iniciarCopa(jogoId, usuarioId, indice, agora);
					yield Desfecho.AO_VIVO;
				}
				case Proximo.EtapaContinental(int indice, var etapa) -> {
					aoVivo.iniciarContinental(jogoId, usuarioId, indice, agora);
					yield Desfecho.AO_VIVO;
				}
				case Proximo.EtapaMundial(int indice, var fase) -> {
					aoVivo.iniciarMundial(jogoId, usuarioId, indice, agora);
					yield Desfecho.AO_VIVO;
				}
				case Proximo.Fim() -> {
					fimDeTemporada.avancarTemporada(jogoId, m.temporada(), semente);
					yield Desfecho.TEMPORADA;
				}
			};
			case LANCES -> {
				int rodada = ((Proximo.Liga) m.proximo()).rodada();
				leiloes.resolverEmRede(jogoId, rodada);
				marcarLeiloes(jogoId, chave(m.temporada(), "r" + rodada + "/leiloes"));
				yield Desfecho.RESULTADO;
			}
			case RESULTADO -> {
				aoVivo.iniciar(jogoId, usuarioId, ((Proximo.Liga) m.proximo()).rodada(), agora);
				yield Desfecho.AO_VIVO;
			}
		};
	}

	/**
	 * Depois do OK da tática: abre os leilões. Quem não pode dar lance em nenhum já fica com o OK dos
	 * lances; se ninguém pode, os leilões já saem resolvidos.
	 */
	private Desfecho abrirLeiloes(long jogoId, long usuarioId, int temporada, int rodada, Instant agora) {
		Etapa etapa = leiloes.abrirEmRede(jogoId, rodada);
		if (etapa == Etapa.RODADA) {
			aoVivo.iniciar(jogoId, usuarioId, rodada, agora);
			return Desfecho.AO_VIVO;
		}
		if (etapa == Etapa.RESULTADO) {
			marcarLeiloes(jogoId, chave(temporada, "r" + rodada + "/leiloes"));
			return Desfecho.RESULTADO;
		}
		String lances = chave(temporada, "r" + rodada + "/lances");
		marcarLeiloes(jogoId, lances);
		for (long participante : participantes(jogoId)) {
			if (!leiloes.podeDarLance(jogoId, participante, rodada)) {
				jdbc.sql("UPDATE jogo_rede_participante SET ok_momento = :momento, ok_em = :agora WHERE jogo_id = :jogo AND usuario_id = :usuario")
					.param("momento", lances)
					.param("agora", hora(agora))
					.param("jogo", jogoId)
					.param("usuario", participante)
					.update();
			}
		}
		return Desfecho.LANCES;
	}

	private void marcarLeiloes(long jogoId, String momento) {
		jdbc.sql("UPDATE jogo_rede SET momento_leiloes = :momento WHERE jogo_id = :jogo")
			.param("momento", momento)
			.param("jogo", jogoId)
			.update();
	}

	/**
	 * O momento atual do jogo em rede, do que vem a seguir no calendário e do passo dos leilões. Vazio
	 * fora do jogo em rede começado ou com a rodada ao vivo.
	 */
	private Optional<Momento> momento(long jogoId) {
		Optional<String> leiloesDoJogo = jdbc.sql("SELECT coalesce(momento_leiloes, '') FROM jogo_rede WHERE jogo_id = :jogo AND comecou_em IS NOT NULL")
			.param("jogo", jogoId)
			.query(String.class)
			.optional();
		if (leiloesDoJogo.isEmpty() || aoVivo.emAndamento(jogoId)) {
			return Optional.empty();
		}
		int temporada = jdbc.sql("SELECT max(numero) FROM jogo_temporada WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query(Integer.class)
			.single();
		Proximo proximo = rodadas.proximo(jogoId);
		return Optional.of(switch (proximo) {
			case Proximo.Liga(int rodada) -> {
				String lances = chave(temporada, "r" + rodada + "/lances");
				String resultado = chave(temporada, "r" + rodada + "/leiloes");
				String atual = leiloesDoJogo.get();
				yield atual.equals(lances) ? new Momento(lances, Passo.LANCES, proximo, temporada)
						: atual.equals(resultado) ? new Momento(resultado, Passo.RESULTADO, proximo, temporada)
						: new Momento(chave(temporada, "r" + rodada + "/tatica"), Passo.PLANTEL, proximo, temporada);
			}
			case Proximo.EtapaCopa(int indice, var etapa) -> new Momento(chave(temporada, "copa" + indice), Passo.PLANTEL, proximo, temporada);
			case Proximo.EtapaContinental(int indice, var etapa) -> new Momento(chave(temporada, "continental" + indice), Passo.PLANTEL, proximo, temporada);
			case Proximo.EtapaMundial(int indice, var fase) -> new Momento(chave(temporada, "mundial" + indice), Passo.PLANTEL, proximo, temporada);
			case Proximo.Fim() -> new Momento(chave(temporada, "fim"), Passo.PLANTEL, proximo, temporada);
		});
	}

	private boolean todosDeramOk(long jogoId, String momento) {
		return jdbc.sql("""
				SELECT NOT EXISTS (SELECT 1 FROM jogo_rede_participante
				                   WHERE jogo_id = :jogo AND ok_momento IS DISTINCT FROM :momento)""")
			.param("jogo", jogoId)
			.param("momento", momento)
			.query(Boolean.class)
			.single();
	}

	private List<Long> participantes(long jogoId) {
		return jdbc.sql("SELECT usuario_id FROM jogo_rede_participante WHERE jogo_id = :jogo ORDER BY usuario_id")
			.param("jogo", jogoId)
			.query(Long.class)
			.list();
	}

	private List<Treinador> treinadores(long jogoId, String momento) {
		return jdbc.sql("""
				SELECT p.usuario_id, u.nome, c.nome AS clube, p.ok_momento IS NOT DISTINCT FROM :momento AS ok, p.visto_em
				FROM jogo_rede_participante p
				JOIN jogo j ON j.id = p.jogo_id
				JOIN usuario u ON u.id = p.usuario_id
				LEFT JOIN jogo_treinador t ON t.jogo_id = p.jogo_id AND t.usuario_id = p.usuario_id
				LEFT JOIN jogo_clube c ON c.id = t.clube_id
				WHERE p.jogo_id = :jogo
				ORDER BY p.usuario_id <> j.dono_id, p.entrou_em, p.usuario_id""")
			.param("jogo", jogoId)
			.param("momento", momento)
			.query((rs, n) -> new Treinador(rs.getLong("usuario_id"), rs.getString("nome"), rs.getString("clube"),
					rs.getBoolean("ok"), rs.getObject("visto_em", OffsetDateTime.class).toInstant()))
			.list();
	}

	private static String chave(int temporada, String passo) {
		return "t" + temporada + "/" + passo;
	}

	private static OffsetDateTime hora(Instant instante) {
		return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
	}

	/** Um momento do jogo em rede: a chave dos OK, o passo e o que vem no calendário. */
	private record Momento(String chave, Passo passo, Proximo proximo, int temporada) {
	}
}
