package br.com.managerfoot.persistencia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.function.LongPredicate;
import java.util.function.UnaryOperator;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.RegraEstrangeiros;
import br.com.managerfoot.dominio.Lugar;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.Continental;
import br.com.managerfoot.temporada.FaseCopa;
import br.com.managerfoot.temporada.Copa;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.LinhaClassificacao;
import br.com.managerfoot.temporada.Temporada;

/**
 * Plantel e tática do treinador humano (SPEC §10.3, rev. 16, 31 e 34): o que a tela mostra e as
 * mudanças na escalação. Toda operação confere que o usuário é o
 * treinador do jogo; caso contrário, o jogo não existe para ele.
 */
@Repository
public class PlantelRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final TemporadaRepositorio temporadas;
	private final RodadaRepositorio rodadas;
	private final EscalacaoRepositorio escalacoes;
	private final CaixaRepositorio caixas;
	private final CopaRepositorio copas;
	private final ContinentalRepositorio continentais;
	private final ParametrosMoral parametrosMoral = ParametrosMoral.padrao();

	public PlantelRepositorio(JdbcClient jdbc, JogoRepositorio jogos, TemporadaRepositorio temporadas,
			RodadaRepositorio rodadas, EscalacaoRepositorio escalacoes, CaixaRepositorio caixas, CopaRepositorio copas,
			ContinentalRepositorio continentais) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.temporadas = temporadas;
		this.rodadas = rodadas;
		this.escalacoes = escalacoes;
		this.caixas = caixas;
		this.copas = copas;
		this.continentais = continentais;
	}

	/**
	 * O plantel do clube do usuário no jogo; vazio se ele não é treinador nesse jogo.
	 *
	 * @throws ClubeNaFilaException se o clube dele está na fila de espera
	 */
	@Transactional
	public Optional<VisaoPlantel> visao(long jogoId, long usuarioId) {
		Optional<Long> clubeId = jogos.clubeDoTreinador(jogoId, usuarioId);
		if (clubeId.isEmpty()) {
			return Optional.empty();
		}
		long clube = clubeId.get();
		if (jdbc.sql("SELECT divisao_id IS NULL FROM jogo_clube WHERE id = :clube").param("clube", clube)
			.query(Boolean.class).single()) {
			throw new ClubeNaFilaException("o clube " + clube + " está na fila de espera");
		}
		List<Jogador> plantel = jogos.carregarPlanteis(jogoId).getOrDefault(clube, List.of());
		Map<Long, Situacao> situacoes = situacoes(clube);
		EscalacaoDoTreinador escalacao = escalacao(clube, plantel);
		String paisDoClube = jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = :jogo").param("jogo", jogoId)
			.query(String.class).single();

		List<LinhaJogador> linhas = plantel.stream().map(j -> {
			Situacao s = situacoes.getOrDefault(j.id(), Situacao.NENHUMA);
			return new LinhaJogador(j, escalacao.lugarDe(j.id()), s.gols(), s.amarelos(), s.suspensao() > 0, s.lesao(),
					RegraEstrangeiros.deOutroPais(j.nacionalidade(), paisDoClube) ? j.nacionalidade() : null,
					RegraEstrangeiros.equiparado(j.nacionalidade(), paisDoClube), s.comContrato());
		}).toList();
		LongPredicate suspenso = id -> situacoes.getOrDefault(id, Situacao.NENHUMA).suspensao() > 0;
		LongPredicate lesionado = id -> situacoes.getOrDefault(id, Situacao.NENHUMA).lesao() > 0;
		List<String> problemas = escalacao.problemas(plantel, suspenso, lesionado);
		String goleiroImprovisado = escalacao.goleiroImprovisado(plantel, suspenso.or(lesionado)).map(Jogador::nome)
			.orElse(null);

		int processadas = rodadas.rodadasProcessadas(jogoId);
		int total = rodadas.totalDeRodadas(jogoId);
		Temporada temporada = temporadas.carregarAtual(jogoId);
		Temporada.SorteiosDivisao divisao = temporada.divisoes().stream()
			.filter(d -> d.calendario().jogos().stream().anyMatch(j -> envolve(j, clube)))
			.findFirst()
			.orElseThrow(() -> new ClubeNaFilaException("o clube " + clube + " está na fila de espera"));
		Classificacao classificacao = rodadas.classificacao(jogoId).get(divisao.prioridade());
		LinhaClassificacao linha = classificacao.linhas().stream().filter(l -> l.clube().id() == clube).findFirst()
			.orElseThrow();
		Clube oClube = linha.clube();
		String nomeDivisao = jdbc.sql("SELECT nome FROM jogo_divisao WHERE jogo_id = :jogo AND prioridade = :p")
			.param("jogo", jogoId).param("p", divisao.prioridade()).query(String.class).single();
		Moral moral = jdbc.sql("SELECT moral FROM jogo_clube WHERE id = :clube").param("clube", clube)
			.query((rs, n) -> rs.getObject("moral") == null ? Moral.inicial(parametrosMoral) : new Moral(rs.getInt("moral")))
			.single();

		RodadaRepositorio.Proximo seguinte = rodadas.proximo(jogoId);
		Optional<Jogo> proximo;
		String etapaCopa = null;
		Integer indiceCopa = null;
		String detalheCopa = null;
		boolean finalCopa = false;
		boolean continental = false;
		boolean mundial = false;
		if (seguinte instanceof RodadaRepositorio.Proximo.EtapaCopa(int indice, Copa.Etapa etapa)) {
			// a etapa da copa vem antes da próxima rodada da liga (SPEC §8.2, rev. 70)
			etapaCopa = "Copa · " + etapa.nome();
			indiceCopa = indice;
			finalCopa = etapa.perna() == Copa.Perna.JOGO_UNICO;
			proximo = rodadas.partidasDaEtapa(jogoId, etapa).stream().map(Copa.PartidaCopa::jogo).filter(j -> envolve(j, clube))
				.findFirst();
			// linha de detalhe do modelo 12c (rev. 71): a ida, o campo neutro ou por que o clube saiu
			long temporadaId = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :jogo AND numero = :numero")
				.param("jogo", jogoId).param("numero", temporada.numero()).query(Long.class).single();
			detalheCopa = copas.carregar(jogoId, temporadaId)
				.flatMap(c -> c.detalheDoProximo(oClube, copas.quadro(jogoId, temporadaId), indice))
				.orElse(null);
		} else if (seguinte instanceof RodadaRepositorio.Proximo.EtapaContinental(int indice, Continental.Etapa etapa)) {
			// a etapa da continental, como a da copa (modelo 13d, rev. 79)
			continental = true;
			etapaCopa = "Continental · " + etapa.nome();
			indiceCopa = indice;
			finalCopa = !etapa.grupos() && etapa.mataMata().perna() == Copa.Perna.JOGO_UNICO;
			proximo = rodadas.partidasDaEtapa(jogoId, etapa).stream().filter(j -> envolve(j, clube)).findFirst();
			long temporadaId = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :jogo AND numero = :numero")
				.param("jogo", jogoId).param("numero", temporada.numero()).query(Long.class).single();
			detalheCopa = continentais.carregar(jogoId, temporadaId)
				.flatMap(c -> c.detalheDoProximo(oClube, continentais.quadro(temporadaId), indice, temporada.semente()))
				.orElse(null);
		} else if (seguinte instanceof RodadaRepositorio.Proximo.EtapaMundial(int indice, FaseCopa fase)) {
			// a etapa do Mundial, como a da continental (modelo 15b, rev. 88)
			mundial = true;
			etapaCopa = "Mundial · " + fase.nome();
			indiceCopa = indice;
			finalCopa = fase == FaseCopa.FINAL;
			proximo = rodadas.partidasDoMundial(jogoId, indice, fase).stream().filter(j -> envolve(j, clube)).findFirst();
			detalheCopa = proximo.isPresent() ? "Jogo único, com prorrogação e pênaltis" : null;
		} else {
			proximo = processadas >= total ? Optional.empty()
					: divisao.calendario().jogos().stream().filter(j -> j.rodada() == processadas + 1 && envolve(j, clube))
						.findFirst();
		}

		Clube adversario = proximo.map(j -> j.mandante().id() == clube ? j.visitante() : j.mandante()).orElse(null);
		return Optional.of(new VisaoPlantel(jogoId, temporada.numero(), jogos.ano(jogoId, temporada.numero()), oClube, nomeDivisao, classificacao.linhas().indexOf(linha) + 1,
				linha.pontos(), linha.jogos(), moral.valor(), parametrosMoral.maximo(), moral.faixa(parametrosMoral),
				processadas + 1, seguinte instanceof RodadaRepositorio.Proximo.Fim, etapaCopa, indiceCopa, detalheCopa, finalCopa,
				continental, mundial, proximo.map(j -> j.mandante().id() == clube).orElse(null), adversario,
				adversario == null || !(continental || mundial) ? null : continentais.paisesDeFora(jogoId).get(adversario.id()), escalacao,
				linhas, problemas, goleiroImprovisado, caixas.saldo(clube, plantel), caixas.moeda(jogoId)));
	}

	/** Move o jogador para titular, banco ou fora (SPEC §10.3, rev. 34). */
	@Transactional
	public void mover(long jogoId, long usuarioId, long jogadorId, Lugar lugar) {
		alterar(jogoId, usuarioId, e -> e.com(jogadorId, lugar), jogadorId, lugar);
	}

	/**
	 * Escolhe a tática e, se faltar titular nela, completa o time com os mais fortes (SPEC §10.3, rev. 86).
	 *
	 * @param semente desempate entre jogadores iguais
	 */
	@Transactional
	public void definirFormacao(long jogoId, long usuarioId, Formacao formacao, long semente) {
		long clube = exigirClube(jogoId, usuarioId);
		Map<Long, Situacao> situacoes = situacoes(clube);
		List<Jogador> plantel = jogos.carregarPlanteis(jogoId).getOrDefault(clube, List.of());
		alterar(jogoId, usuarioId, e -> e.com(formacao).completada(plantel,
				id -> !situacoes.getOrDefault(id, Situacao.NENHUMA).disponivel(),
				ParametrosMotor.padrao().asterisco().limiteSemPenalidade(), new Random(semente)), null, null);
	}

	/** Tira todos os jogadores do time e do banco, para a tática escalar de novo (SPEC §10.3, rev. 86). */
	@Transactional
	public void retirarTodos(long jogoId, long usuarioId) {
		alterar(jogoId, usuarioId, EscalacaoDoTreinador::vazia, null, null);
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

	private void alterar(long jogoId, long usuarioId, UnaryOperator<EscalacaoDoTreinador> mudanca, Long jogadorId,
			Lugar lugar) {
		long clube = exigirClube(jogoId, usuarioId);
		if (jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_rodada_ao_vivo WHERE jogo_id = :jogo)").param("jogo", jogoId)
			.query(Boolean.class).single()) {
			throw new IllegalArgumentException("A rodada está em andamento: as trocas agora são pela substituição.");
		}
		List<Jogador> plantel = jogos.carregarPlanteis(jogoId).getOrDefault(clube, List.of());
		if (jogadorId != null) {
			Jogador jogador = plantel.stream().filter(j -> j.id() == jogadorId).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Este jogador não é do seu clube."));
			Situacao situacao = situacoes(clube).getOrDefault(jogadorId, Situacao.NENHUMA);
			if (lugar != Lugar.FORA && situacao.suspensao() > 0) {
				throw new IllegalArgumentException(jogador.nome() + " está suspenso e não pode ser escalado.");
			}
			if (lugar != Lugar.FORA && situacao.lesao() > 0) {
				throw new IllegalArgumentException(jogador.nome() + " está lesionado e não pode ser escalado.");
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
				.escalar(plantel.stream().filter(j -> situacoes(clube).getOrDefault(j.id(), Situacao.NENHUMA)
					.disponivel()).toList()));
			escalacoes.gravar(clube, daMaquina);
			return daMaquina;
		});
	}

	/** Gols na temporada atual, amarelos, suspensão e lesão de cada jogador do clube. */
	private Map<Long, Situacao> situacoes(long clube) {
		Map<Long, Situacao> situacoes = new HashMap<>();
		jdbc.sql("""
				SELECT j.id, j.amarelos, j.suspensao, j.lesao,
				       coalesce(j.contrato_temporada = (SELECT max(t.numero) FROM jogo_temporada t JOIN jogo_clube c ON c.jogo_id = t.jogo_id
				                                        WHERE c.id = j.clube_id), false) AS com_contrato,
				       (SELECT count(*) FROM jogo_evento e JOIN jogo_partida p ON p.id = e.partida_id
				        WHERE e.jogador_id = j.id AND e.tipo = 'GOL'
				          AND p.temporada_id = (SELECT max(t.id) FROM jogo_temporada t JOIN jogo_clube c ON c.jogo_id = t.jogo_id
				                                WHERE c.id = j.clube_id)) AS gols
				FROM jogo_jogador j WHERE j.clube_id = :clube""")
			.param("clube", clube)
			.query(rs -> {
				situacoes.put(rs.getLong("id"), new Situacao(rs.getInt("gols"), rs.getInt("amarelos"), rs.getInt("suspensao"),
						rs.getInt("lesao"), rs.getBoolean("com_contrato")));
			});
		return situacoes;
	}

	private static boolean envolve(Jogo jogo, long clube) {
		return jogo.mandante().id() == clube || jogo.visitante().id() == clube;
	}

	private record Situacao(int gols, int amarelos, int suspensao, int lesao, boolean comContrato) {

		static final Situacao NENHUMA = new Situacao(0, 0, 0, 0, false);

		boolean disponivel() {
			return suspensao == 0 && lesao == 0;
		}
	}

	/**
	 * Um jogador na tela do plantel.
	 *
	 * @param estrangeiro sigla do país, se não é o país do clube
	 * @param equiparado é de outro país, mas não conta como estrangeiro (SPEC §8.4)
	 */
	/** @param lesao jogos fora por lesão a cumprir (SPEC §7.5); zero se não está lesionado */
	public record LinhaJogador(Jogador jogador, Lugar lugar, int gols, int amarelos, boolean suspenso, int lesao,
			String estrangeiro, boolean equiparado, boolean comContrato) {

		/** Conta entre os estrangeiros em campo. */
		public boolean contaComoEstrangeiro() {
			return estrangeiro != null && !equiparado;
		}
	}

	/**
	 * Tudo o que a tela do plantel mostra.
	 *
	 * @param temporada número da temporada, enviado pela tela no fim dela
	 * @param ano ano da temporada, mostrado na tela
	 * @param etapaCopa nome da etapa da copa que vem antes da próxima rodada; nulo se a próxima é da liga
	 * @param indiceCopa índice da etapa no quadro da copa, para o pedido de jogá-la; nulo se a próxima é da liga
	 * @param detalheCopa linha embaixo do próximo jogo da copa (modelo 12c); pode ser nulo
	 * @param finalCopa a próxima é a final da copa, em campo neutro
	 * @param emCasa nulo quando não há próximo jogo (folga, fora da etapa da copa ou temporada encerrada)
	 * @param adversario nulo quando não há próximo jogo
	 * @param continental a etapa do próximo jogo é da continental, com o nome em {@code etapaCopa}
	 * @param mundial a etapa do próximo jogo é do Mundial, com o nome em {@code etapaCopa}
	 * @param paisAdversario sigla do adversário de fora, na continental; nulo nos outros casos
	 * @param saldo caixa do clube em centavos de Real, para saber se pode contratar (rev. 39)
	 */
	public record VisaoPlantel(long jogoId, int temporada, int ano, Clube clube, String divisao, int posicao, int pontos, int jogos,
			int moral, int moralMaxima, Moral.Faixa faixaMoral, int rodada, boolean temporadaEncerrada, String etapaCopa,
			Integer indiceCopa, String detalheCopa, boolean finalCopa, boolean continental, boolean mundial, Boolean emCasa,
			Clube adversario, String paisAdversario, EscalacaoDoTreinador escalacao, List<LinhaJogador> jogadores, List<String> problemas,
			String goleiroImprovisado, long saldo, Moeda moeda) {
	}
}
