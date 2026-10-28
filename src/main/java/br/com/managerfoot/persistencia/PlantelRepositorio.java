package br.com.managerfoot.persistencia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Lugar;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.LinhaClassificacao;
import br.com.managerfoot.temporada.ProcessamentoRodada;
import br.com.managerfoot.temporada.Temporada;

/**
 * Plantel e tática do treinador humano (SPEC §10.3, rev. 16, 31 e 34): o que a tela mostra, as
 * mudanças na escalação e o "Confirmar e jogar rodada". Toda operação confere que o usuário é o
 * treinador do jogo; caso contrário, o jogo não existe para ele.
 */
@Repository
public class PlantelRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final TemporadaRepositorio temporadas;
	private final RodadaRepositorio rodadas;
	private final EscalacaoRepositorio escalacoes;
	private final ParametrosMoral parametrosMoral = ParametrosMoral.padrao();

	public PlantelRepositorio(JdbcClient jdbc, JogoRepositorio jogos, TemporadaRepositorio temporadas,
			RodadaRepositorio rodadas, EscalacaoRepositorio escalacoes) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.temporadas = temporadas;
		this.rodadas = rodadas;
		this.escalacoes = escalacoes;
	}

	/** O plantel do clube do usuário no jogo; vazio se ele não é treinador nesse jogo. */
	@Transactional
	public Optional<VisaoPlantel> visao(long jogoId, long usuarioId) {
		Optional<Long> clubeId = jogos.clubeDoTreinador(jogoId, usuarioId);
		if (clubeId.isEmpty()) {
			return Optional.empty();
		}
		long clube = clubeId.get();
		List<Jogador> plantel = jogos.carregarPlanteis(jogoId).getOrDefault(clube, List.of());
		Map<Long, Situacao> situacoes = situacoes(clube);
		EscalacaoDoTreinador escalacao = escalacao(clube, plantel);
		String paisDoClube = jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = :jogo").param("jogo", jogoId)
			.query(String.class).single();

		List<LinhaJogador> linhas = plantel.stream().map(j -> {
			Situacao s = situacoes.getOrDefault(j.id(), new Situacao(0, 0, 0));
			return new LinhaJogador(j, escalacao.lugarDe(j.id()), s.gols(), s.amarelos(), s.suspensao() > 0,
					j.nacionalidade().equals(paisDoClube) ? null : j.nacionalidade());
		}).toList();
		List<String> problemas = escalacao.problemas(plantel, id -> situacoes.containsKey(id) && situacoes.get(id).suspensao() > 0);

		int processadas = rodadas.rodadasProcessadas(jogoId);
		int total = rodadas.totalDeRodadas(jogoId);
		Temporada temporada = temporadas.carregarAtual(jogoId);
		Temporada.SorteiosDivisao divisao = temporada.divisoes().stream()
			.filter(d -> d.calendario().jogos().stream().anyMatch(j -> envolve(j, clube)))
			.findFirst()
			.orElseThrow(() -> new NoSuchElementException("o clube " + clube + " não joga nesta temporada"));
		Classificacao classificacao = rodadas.classificacao(jogoId).get(divisao.prioridade());
		LinhaClassificacao linha = classificacao.linhas().stream().filter(l -> l.clube().id() == clube).findFirst()
			.orElseThrow();
		Clube oClube = linha.clube();
		String nomeDivisao = jdbc.sql("SELECT nome FROM jogo_divisao WHERE jogo_id = :jogo AND prioridade = :p")
			.param("jogo", jogoId).param("p", divisao.prioridade()).query(String.class).single();
		Moral moral = jdbc.sql("SELECT moral FROM jogo_clube WHERE id = :clube").param("clube", clube)
			.query((rs, n) -> rs.getObject("moral") == null ? Moral.inicial(parametrosMoral) : new Moral(rs.getInt("moral")))
			.single();

		Optional<Jogo> proximo = processadas >= total ? Optional.empty()
				: divisao.calendario().jogos().stream().filter(j -> j.rodada() == processadas + 1 && envolve(j, clube))
					.findFirst();

		return Optional.of(new VisaoPlantel(jogoId, oClube, nomeDivisao, classificacao.linhas().indexOf(linha) + 1,
				linha.pontos(), linha.jogos(), moral.valor(), parametrosMoral.maximo(), moral.faixa(parametrosMoral),
				processadas + 1, processadas >= total, proximo.map(j -> j.mandante().id() == clube).orElse(null),
				proximo.map(j -> j.mandante().id() == clube ? j.visitante() : j.mandante()).orElse(null), escalacao,
				linhas, problemas));
	}

	/** Move o jogador para titular, banco ou fora (SPEC §10.3, rev. 34). */
	@Transactional
	public void mover(long jogoId, long usuarioId, long jogadorId, Lugar lugar) {
		alterar(jogoId, usuarioId, e -> e.com(jogadorId, lugar), jogadorId, lugar);
	}

	@Transactional
	public void definirFormacao(long jogoId, long usuarioId, Formacao formacao) {
		alterar(jogoId, usuarioId, e -> e.com(formacao), null, null);
	}

	/**
	 * @param jogadorId nulo é o automático
	 * @throws IllegalArgumentException se o batedor não está entre os titulares ou no banco
	 */
	@Transactional
	public void definirBatedor(long jogoId, long usuarioId, Long jogadorId) {
		alterar(jogoId, usuarioId, e -> {
			if (jogadorId != null && e.lugarDe(jogadorId) == Lugar.FORA) {
				throw new IllegalArgumentException("O batedor precisa estar entre os titulares ou no banco.");
			}
			return e.comBatedor(jogadorId);
		}, null, null);
	}

	/**
	 * Joga a rodada esperada com a escalação gravada e devolve o jogo do treinador (vazio na folga).
	 *
	 * @throws IllegalStateException se a escalação não pode jogar, ou se a rodada já foi jogada
	 */
	@Transactional
	public Optional<ProcessamentoRodada.PartidaJogada> jogarRodada(long jogoId, long usuarioId, int rodada) {
		long clube = exigirClube(jogoId, usuarioId);
		RodadaRepositorio.RodadaJogada jogada = rodadas.avancar(jogoId, rodada);
		return jogada.resultado().partidas().stream().filter(p -> envolve(p.jogo(), clube)).findFirst();
	}

	private void alterar(long jogoId, long usuarioId, UnaryOperator<EscalacaoDoTreinador> mudanca, Long jogadorId,
			Lugar lugar) {
		long clube = exigirClube(jogoId, usuarioId);
		List<Jogador> plantel = jogos.carregarPlanteis(jogoId).getOrDefault(clube, List.of());
		if (jogadorId != null) {
			Jogador jogador = plantel.stream().filter(j -> j.id() == jogadorId).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Este jogador não é do seu clube."));
			if (lugar != Lugar.FORA && situacoes(clube).getOrDefault(jogadorId, new Situacao(0, 0, 0)).suspensao() > 0) {
				throw new IllegalArgumentException(jogador.nome() + " está suspenso e não pode ser escalado.");
			}
		}
		escalacoes.gravar(clube, mudanca.apply(escalacao(clube, plantel)));
	}

	private long exigirClube(long jogoId, long usuarioId) {
		return jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
	}

	/** A escalação gravada; se não houver (jogo antigo), a da máquina passa a ser a gravada. */
	private EscalacaoDoTreinador escalacao(long clube, List<Jogador> plantel) {
		return escalacoes.carregar(clube).orElseGet(() -> {
			EscalacaoDoTreinador daMaquina = EscalacaoDoTreinador.de(new EscalacaoMaquina(ParametrosMotor.padrao())
				.escalar(plantel.stream().filter(j -> situacoes(clube).getOrDefault(j.id(), new Situacao(0, 0, 0))
					.suspensao() == 0).toList()));
			escalacoes.gravar(clube, daMaquina);
			return daMaquina;
		});
	}

	/** Gols na temporada atual, amarelos e suspensão de cada jogador do clube. */
	private Map<Long, Situacao> situacoes(long clube) {
		Map<Long, Situacao> situacoes = new HashMap<>();
		jdbc.sql("""
				SELECT j.id, j.amarelos, j.suspensao,
				       (SELECT count(*) FROM jogo_evento e JOIN jogo_partida p ON p.id = e.partida_id
				        WHERE e.jogador_id = j.id AND e.tipo = 'GOL'
				          AND p.temporada_id = (SELECT max(t.id) FROM jogo_temporada t JOIN jogo_clube c ON c.jogo_id = t.jogo_id
				                                WHERE c.id = j.clube_id)) AS gols
				FROM jogo_jogador j WHERE j.clube_id = :clube""")
			.param("clube", clube)
			.query(rs -> {
				situacoes.put(rs.getLong("id"), new Situacao(rs.getInt("gols"), rs.getInt("amarelos"), rs.getInt("suspensao")));
			});
		return situacoes;
	}

	private static boolean envolve(Jogo jogo, long clube) {
		return jogo.mandante().id() == clube || jogo.visitante().id() == clube;
	}

	private record Situacao(int gols, int amarelos, int suspensao) {
	}

	/**
	 * Um jogador na tela do plantel.
	 *
	 * @param estrangeiro sigla do país, se não é o país do clube
	 */
	public record LinhaJogador(Jogador jogador, Lugar lugar, int gols, int amarelos, boolean suspenso,
			String estrangeiro) {
	}

	/**
	 * Tudo o que a tela do plantel mostra.
	 *
	 * @param emCasa nulo quando não há próximo jogo (folga ou temporada encerrada)
	 * @param adversario nulo quando não há próximo jogo
	 */
	public record VisaoPlantel(long jogoId, Clube clube, String divisao, int posicao, int pontos, int jogos,
			int moral, int moralMaxima, Moral.Faixa faixaMoral, int rodada, boolean temporadaEncerrada, Boolean emCasa,
			Clube adversario, EscalacaoDoTreinador escalacao, List<LinhaJogador> jogadores, List<String> problemas) {
	}
}
