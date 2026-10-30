package br.com.managerfoot.persistencia;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SplittableRandom;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.TipoEvento;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Copa;
import br.com.managerfoot.temporada.FaseCopa;
import br.com.managerfoot.temporada.PlacarCopa;
import br.com.managerfoot.temporada.Disciplina;
import br.com.managerfoot.temporada.EvolucaoForca;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.ProcessamentoRodada;
import br.com.managerfoot.temporada.RodadaAoVivo;
import br.com.managerfoot.temporada.Temporada;

/**
 * Avanço de rodada da temporada atual (SPEC §15, 3c.3) e classificação recalculada dos placares.
 * Cada rodada é processada uma só vez: quem pede o avanço diz qual rodada quer jogar, e o avanço
 * trava a temporada e só aceita a seguinte à última processada. Um pedido repetido (clique duplo,
 * dois clientes) é recusado em vez de jogar mais uma rodada.
 */
@Repository
public class RodadaRepositorio {

	/** Eventos gravados; os demais (finalizações sem gol, substituições) ficam só na partida. */
	private static final Set<TipoEvento> EVENTOS_GRAVADOS = Set.of(TipoEvento.GOL, TipoEvento.CARTAO_AMARELO,
			TipoEvento.SEGUNDO_AMARELO, TipoEvento.CARTAO_VERMELHO, TipoEvento.LESAO);

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final TemporadaRepositorio temporadas;
	private final EscalacaoRepositorio escalacoes;
	private final CaixaRepositorio caixas;
	private final MercadoRepositorio mercado;
	private final ClassificacaoRepositorio classificacoes;
	private final EstadioRepositorio estadios;
	private final PatrocinioRepositorio patrocinios;
	private final TrocaRepositorio trocas;
	private final CopaRepositorio copas;
	private final ProcessamentoRodada processamento = ProcessamentoRodada.padrao();
	private final EvolucaoForca evolucao = EvolucaoForca.padrao();

	public RodadaRepositorio(JdbcClient jdbc, JogoRepositorio jogos, TemporadaRepositorio temporadas,
			EscalacaoRepositorio escalacoes, CaixaRepositorio caixas, MercadoRepositorio mercado,
			ClassificacaoRepositorio classificacoes, EstadioRepositorio estadios, PatrocinioRepositorio patrocinios,
			TrocaRepositorio trocas, CopaRepositorio copas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.temporadas = temporadas;
		this.escalacoes = escalacoes;
		this.caixas = caixas;
		this.mercado = mercado;
		this.classificacoes = classificacoes;
		this.estadios = estadios;
		this.patrocinios = patrocinios;
		this.trocas = trocas;
		this.copas = copas;
	}

	/** O que a temporada atual joga a seguir (SPEC §8.2): uma rodada da liga, uma etapa da copa ou nada. */
	public sealed interface Proximo {

		/** A rodada da liga seguinte à última processada. */
		record Liga(int rodada) implements Proximo {
		}

		/**
		 * Uma etapa da copa, jogada depois da rodada da liga {@code etapa.depoisDaRodada()}.
		 *
		 * @param indice posição da etapa no quadro da copa
		 */
		record EtapaCopa(int indice, Copa.Etapa etapa) implements Proximo {
		}

		/** Liga e copa terminadas: falta o fim de temporada. */
		record Fim() implements Proximo {
		}
	}

	/** O que a temporada atual joga a seguir. */
	@Transactional(readOnly = true)
	public Proximo proximo(long jogoId) {
		return proximo(jogoId, temporadaAtual(jogoId, false));
	}

	/** A etapa da copa depois da rodada R vem antes da rodada R + 1; a final, depois da última. */
	private Proximo proximo(long jogoId, LinhaTemporada linha) {
		List<Copa.Etapa> quadro = copas.quadro(jogoId, linha.id());
		if (linha.etapasCopa() < quadro.size() && quadro.get(linha.etapasCopa()).depoisDaRodada() <= linha.rodadasProcessadas()) {
			return new Proximo.EtapaCopa(linha.etapasCopa(), quadro.get(linha.etapasCopa()));
		}
		if (linha.rodadasProcessadas() < totalDeRodadas(linha)) {
			return new Proximo.Liga(linha.rodadasProcessadas() + 1);
		}
		return new Proximo.Fim();
	}

	/**
	 * Joga a rodada da temporada atual e grava placares, gols, cartões, suspensões, moral, o caixa
	 * dos clubes (SPEC §9.1) e as obras do estádio (§9.2), e completa os jogadores à venda da máquina (SPEC §9.4). Os clubes de treinador humano jogam com a escalação gravada, e quem
	 * ficou suspenso sai dela.
	 *
	 * @param rodada a rodada que quem pede espera jogar: a seguinte à última processada
	 * @return o resultado do processamento
	 * @throws NoSuchElementException se o jogo não existe ou não tem temporada
	 * @throws IllegalStateException se a rodada já foi jogada, está fora de ordem ou não existe, ou se
	 *         a escalação gravada de um treinador não pode jogar
	 */
	@Transactional
	public RodadaJogada avancar(long jogoId, int rodada) {
		return avancar(jogoId, rodada, List.of());
	}

	/**
	 * Como {@link #avancar(long, int)}, com as substituições feitas na rodada ao vivo
	 * (docs/decisions/0010).
	 */
	@Transactional
	public RodadaJogada avancar(long jogoId, int rodada, List<RodadaAoVivo.Substituicao> substituicoes) {
		LinhaTemporada linha = temporadaAtual(jogoId, true);
		if (rodada != linha.rodadasProcessadas() + 1) {
			throw new IllegalStateException("o jogo " + jogoId + " já processou " + linha.rodadasProcessadas()
					+ " rodada(s); a próxima é a " + (linha.rodadasProcessadas() + 1) + ", não a " + rodada);
		}
		int total = totalDeRodadas(linha);
		if (rodada > total) {
			throw new IllegalStateException("a temporada do jogo " + jogoId + " já teve as " + total + " rodadas jogadas");
		}
		if (proximo(jogoId, linha) instanceof Proximo.EtapaCopa(int indice, Copa.Etapa etapa)) {
			throw new IllegalStateException("o jogo " + jogoId + " joga antes a etapa " + indice + " da copa ("
					+ etapa.fase().nome() + ")");
		}

		Map<Long, List<Jogador>> planteis = jogos.carregarPlanteis(jogoId);
		Disciplina antes = carregarDisciplina(jogoId);
		Map<Long, Moral> moralAntes = carregarMoral(jogoId);
		Temporada temporada = temporadas.carregarAtual(jogoId);
		RodadaAoVivo aoVivo = preparar(temporada, rodada, jogoId, planteis, antes, substituicoes);
		ProcessamentoRodada.Resultado resultado = aoVivo.finalizar(moralAntes);

		for (ProcessamentoRodada.PartidaJogada partida : resultado.partidas()) {
			gravarPartida(linha.id(), partida);
		}
		gravarDisciplina(planteis, antes, resultado.disciplina());
		escalacoes.retirarSuspensos(jogoId);
		caixas.lancarRodada(jogoId, linha.id(), temporada, rodada, resultado.partidas(), planteis);
		patrocinios.pagarVitorias(linha.id(), rodada, resultado.partidas());
		evoluir(jogoId, new SplittableRandom(temporada.semente() * 41 + rodada), aoVivo, resultado, planteis);
		estadios.avancarObras(jogoId, linha.id(), rodada, planteis);
		mercado.reporAVenda(jogoId, MercadoRepositorio.semente(temporada.semente(), rodada));
		gravarMoralEDemitir(jogoId, linha.id(), rodada, moralAntes, resultado);

		int atualizadas = jdbc.sql("""
				UPDATE jogo_temporada SET rodadas_processadas = :rodada
				WHERE id = :temporada AND rodadas_processadas = :anterior""")
			.param("rodada", rodada)
			.param("temporada", linha.id())
			.param("anterior", rodada - 1)
			.update();
		if (atualizadas != 1) {
			throw new IllegalStateException("a rodada " + rodada + " do jogo " + jogoId + " já foi processada");
		}
		return new RodadaJogada(rodada, resultado);
	}

	/**
	 * Avança a etapa da copa (SPEC §8.2, rev. 70) e grava placares com prorrogação e pênaltis, gols,
	 * cartões, suspensões, lesões, moral, o caixa sem salário, o patrocínio por vitória, a evolução de
	 * força e as demissões, como numa rodada da liga. Terminada a fase, cria a seguinte. Cada etapa é
	 * processada uma só vez.
	 *
	 * @param indice a etapa que quem pede espera jogar: a {@link Proximo.EtapaCopa} da temporada
	 * @throws IllegalStateException se a etapa não é a próxima ou se a escalação de um treinador não
	 *         pode jogar
	 */
	@Transactional
	public EtapaJogada avancarCopa(long jogoId, int indice, List<RodadaAoVivo.Substituicao> substituicoes) {
		LinhaTemporada linha = temporadaAtual(jogoId, true);
		if (!(proximo(jogoId, linha) instanceof Proximo.EtapaCopa(int esperado, Copa.Etapa etapa)) || esperado != indice) {
			throw new IllegalStateException("a etapa " + indice + " da copa não é a próxima do jogo " + jogoId);
		}
		Map<Long, List<Jogador>> planteis = jogos.carregarPlanteis(jogoId);
		Disciplina antes = carregarDisciplina(jogoId);
		Map<Long, Moral> moralAntes = carregarMoral(jogoId);
		Temporada temporada = temporadas.carregarAtual(jogoId);
		Copa copa = copas.carregar(jogoId, linha.id()).orElseThrow();
		List<Copa.PartidaCopa> jogosDaEtapa = copa.partidas(etapa);
		RodadaAoVivo aoVivo = prepararCopa(temporada, indice, jogosDaEtapa, jogoId, planteis, antes, substituicoes);
		ProcessamentoRodada.Resultado resultado = aoVivo.finalizar(moralAntes);

		for (int i = 0; i < jogosDaEtapa.size(); i++) {
			Copa.PartidaCopa partida = jogosDaEtapa.get(i);
			ProcessamentoRodada.PartidaJogada jogada = resultado.partidas().get(i);
			PlacarCopa placar = new PlacarCopa(jogada.resultado().golsCasa(), jogada.resultado().golsFora(),
					jogada.resultado().prorrogacao(), jogada.resultado().penaltis()
						.map(d -> new PlacarCopa.Penaltis(d.gols(Lado.CASA), d.gols(Lado.FORA))));
			copa = copa.comPlacar(etapa.fase(), partida.confronto().ordem(), partida.perna(), placar);
			gravarPartida(linha.id(), jogada, placar);
		}
		int rodada = etapa.depoisDaRodada();
		gravarDisciplina(planteis, antes, resultado.disciplina());
		escalacoes.retirarSuspensos(jogoId);
		caixas.lancarCopa(jogoId, linha.id(), temporada, resultado.partidas(), etapa.fase() == FaseCopa.FINAL, planteis);
		patrocinios.pagarVitorias(linha.id(), rodada, resultado.partidas(), Competicao.COPA);
		evoluir(jogoId, new SplittableRandom(temporada.semente() * 43 + indice), aoVivo, resultado, planteis);
		gravarMoralEDemitir(jogoId, linha.id(), rodada, moralAntes, resultado);

		int atualizadas = jdbc.sql("""
				UPDATE jogo_temporada SET etapas_copa_processadas = :etapa
				WHERE id = :temporada AND etapas_copa_processadas = :anterior""")
			.param("etapa", indice + 1)
			.param("temporada", linha.id())
			.param("anterior", indice)
			.update();
		if (atualizadas != 1) {
			throw new IllegalStateException("a etapa " + indice + " da copa do jogo " + jogoId + " já foi processada");
		}
		copa = copas.avancarFase(linha.id(), copa, temporada.semente());
		if (copa.campeao().isPresent()) {
			caixas.pagarPremiosDaCopa(jogoId, linha.id(), rodada, copa.campeao().get(), copa.vice().orElseThrow());
			copas.gravarCampeao(jogoId, temporada.numero(), copa.campeao().get(), copa.vice().orElseThrow());
		}
		return new EtapaJogada(indice, etapa, resultado, copa);
	}

	/**
	 * Grava a moral de quem mudou e então as demissões pela moral e as trocas de treinador (SPEC §5.4,
	 * rev. 62).
	 */
	private void gravarMoralEDemitir(long jogoId, long temporadaId, int rodada, Map<Long, Moral> moralAntes,
			ProcessamentoRodada.Resultado resultado) {
		resultado.moral().forEach((clubeId, moral) -> {
			if (!moral.equals(moralAntes.get(clubeId))) {
				jdbc.sql("UPDATE jogo_clube SET moral = :moral WHERE id = :clube")
					.param("moral", moral.valor())
					.param("clube", clubeId)
					.update();
			}
		});
		Set<Long> jogaram = new HashSet<>();
		resultado.partidas().forEach(p -> {
			jogaram.add(p.jogo().mandante().id());
			jogaram.add(p.jogo().visitante().id());
		});
		trocas.demitirDepoisDaRodada(jogoId, temporadaId, rodada, moralAntes, jogaram);
	}

	/**
	 * Evolução de força depois das partidas (SPEC §6.1, rev. 55): quem jogou se desgasta, quem não
	 * jogou pode crescer. Grava só quem mudou. O teto é o da divisão de cada clube.
	 */
	private void evoluir(long jogoId, SplittableRandom aleatorio, RodadaAoVivo aoVivo,
			ProcessamentoRodada.Resultado resultado, Map<Long, List<Jogador>> planteis) {
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		Map<Long, Integer> tetos = new HashMap<>();
		liga.divisoes().forEach(d -> d.clubes().forEach(c -> tetos.put(c.id(), d.limiteForca())));
		for (int i = 0; i < aoVivo.partidas().size(); i++) {
			RodadaAoVivo.PartidaDaRodada partida = aoVivo.partidas().get(i);
			ProcessamentoRodada.PartidaJogada jogada = resultado.partidas().get(i);
			if (!partida.jogo().equals(jogada.jogo())) {
				throw new IllegalStateException("partidas fora de ordem na rodada " + jogada.jogo().rodada());
			}
			for (Lado lado : Lado.values()) {
				Clube clube = partida.clube(lado);
				int teto = tetos.get(clube.id());
				List<Jogador> plantel = planteis.getOrDefault(clube.id(), List.of());
				List<Jogador> depois = evolucao.aposPartida(plantel, lado == Lado.CASA ? partida.casa() : partida.fora(),
						jogada.resultado(), lado, teto, clube.forca(), aleatorio);
				for (int k = 0; k < plantel.size(); k++) {
					if (depois.get(k).forca() != plantel.get(k).forca()) {
						jdbc.sql("UPDATE jogo_jogador SET forca = :forca WHERE id = :jogador")
							.param("forca", depois.get(k).forca())
							.param("jogador", depois.get(k).id())
							.update();
					}
				}
			}
		}
	}

	/**
	 * A rodada pronta para jogar ao vivo, com as escalações e as substituições já feitas. Não grava
	 * nada.
	 *
	 * @throws IllegalStateException se a escalação de um treinador não pode jogar
	 */
	@Transactional(readOnly = true)
	public RodadaAoVivo preparar(long jogoId, int rodada, List<RodadaAoVivo.Substituicao> substituicoes) {
		return preparar(temporadas.carregarAtual(jogoId), rodada, jogoId, jogos.carregarPlanteis(jogoId),
				carregarDisciplina(jogoId), substituicoes);
	}

	private RodadaAoVivo preparar(Temporada temporada, int rodada, long jogoId, Map<Long, List<Jogador>> planteis, Disciplina antes,
			List<RodadaAoVivo.Substituicao> substituicoes) {
		return processamento.preparar(temporada, rodada, planteis, antes,
				escalacoesDosTreinadores(jogoId, "a rodada " + rodada, planteis, antes), substituicoes);
	}

	/**
	 * A etapa da copa pronta para jogar ao vivo, como {@link #preparar(long, int, List)}. Não grava
	 * nada.
	 *
	 * @throws IllegalStateException se a etapa não é a próxima ou a escalação de um treinador não pode jogar
	 */
	@Transactional(readOnly = true)
	public RodadaAoVivo prepararCopa(long jogoId, int indice, List<RodadaAoVivo.Substituicao> substituicoes) {
		LinhaTemporada linha = temporadaAtual(jogoId, false);
		if (!(proximo(jogoId, linha) instanceof Proximo.EtapaCopa(int esperado, Copa.Etapa etapa)) || esperado != indice) {
			throw new IllegalStateException("a etapa " + indice + " da copa não é a próxima do jogo " + jogoId);
		}
		Copa copa = copas.carregar(jogoId, linha.id()).orElseThrow();
		return prepararCopa(temporadas.carregarAtual(jogoId), indice, copa.partidas(etapa), jogoId,
				jogos.carregarPlanteis(jogoId), carregarDisciplina(jogoId), substituicoes);
	}

	private RodadaAoVivo prepararCopa(Temporada temporada, int indice, List<Copa.PartidaCopa> jogosDaEtapa, long jogoId,
			Map<Long, List<Jogador>> planteis, Disciplina antes, List<RodadaAoVivo.Substituicao> substituicoes) {
		return processamento.prepararCopa(temporada, indice, jogosDaEtapa, planteis, antes,
				escalacoesDosTreinadores(jogoId, "a copa", planteis, antes), substituicoes);
	}

	/** A escalação gravada de cada treinador, sem suspensos e lesionados. */
	private Map<Long, Escalacao> escalacoesDosTreinadores(long jogoId, String oQue, Map<Long, List<Jogador>> planteis,
			Disciplina antes) {
		Map<Long, Escalacao> doTreinador = new HashMap<>();
		escalacoes.carregarDoJogo(jogoId).forEach((clubeId, escalacao) -> {
			try {
				doTreinador.put(clubeId, escalacao.escalacao(planteis.getOrDefault(clubeId, List.of()), antes::suspenso,
						antes::lesionado, ParametrosMotor.padrao().penalti().fatorGoleiroImprovisado()));
			}
			catch (IllegalStateException e) {
				throw new IllegalStateException("o clube " + clubeId + " não pode jogar " + oQue + ": " + e.getMessage(), e);
			}
		});
		return doTreinador;
	}

	/** As partidas da etapa da copa na temporada atual; vazia se não há copa ou a etapa já foi jogada. */
	@Transactional(readOnly = true)
	public List<Copa.PartidaCopa> partidasDaEtapa(long jogoId, Copa.Etapa etapa) {
		return copas.carregar(jogoId, temporadaAtual(jogoId, false).id()).map(c -> c.partidas(etapa)).orElse(List.of());
	}

	/** Rodadas já jogadas na temporada atual. */
	@Transactional(readOnly = true)
	public int rodadasProcessadas(long jogoId) {
		return temporadaAtual(jogoId, false).rodadasProcessadas();
	}

	/** Rodadas da temporada atual: as da divisão com mais rodadas. */
	@Transactional(readOnly = true)
	public int totalDeRodadas(long jogoId) {
		return totalDeRodadas(temporadaAtual(jogoId, false));
	}

	/** Classificação de cada divisão da temporada atual, por prioridade, com os placares gravados. */
	@Transactional(readOnly = true)
	public Map<Integer, Classificacao> classificacao(long jogoId) {
		return classificacoes.calcular(temporadas.carregarAtual(jogoId), temporadaAtual(jogoId, false).id(), Integer.MAX_VALUE);
	}

	private int totalDeRodadas(LinhaTemporada linha) {
		return jdbc.sql("SELECT coalesce(max(rodada), 0) FROM jogo_partida WHERE temporada_id = :temporada AND divisao_id IS NOT NULL")
			.param("temporada", linha.id())
			.query(Integer.class)
			.single();
	}

	private void gravarPartida(long temporadaId, ProcessamentoRodada.PartidaJogada partida) {
		gravarPartida(temporadaId, partida, null);
	}

	/** @param copa o placar da partida da copa, com prorrogação e pênaltis; nulo na liga */
	private void gravarPartida(long temporadaId, ProcessamentoRodada.PartidaJogada partida, PlacarCopa copa) {
		Jogo jogo = partida.jogo();
		long partidaId = jdbc.sql("""
				UPDATE jogo_partida SET gols_mandante = :gm, gols_visitante = :gv, semente = :semente,
				       prorrogacao = :prorrogacao, penaltis_mandante = :pm, penaltis_visitante = :pv
				WHERE temporada_id = :temporada AND rodada = :rodada AND mandante_id = :mandante
				  AND visitante_id = :visitante AND gols_mandante IS NULL
				  AND coalesce((SELECT k.competicao FROM jogo_copa_confronto k WHERE k.id = confronto_id),
				               CASE WHEN divisao_id IS NOT NULL THEN 'LIGA' ELSE 'CONTINENTAL' END) = :competicao
				RETURNING id""")
			.param("prorrogacao", copa != null && copa.prorrogacao())
			.param("pm", copa == null ? null : copa.penaltis().map(PlacarCopa.Penaltis::mandante).orElse(null))
			.param("pv", copa == null ? null : copa.penaltis().map(PlacarCopa.Penaltis::visitante).orElse(null))
			.param("competicao", (copa != null ? Competicao.COPA : Competicao.LIGA).name())
			.param("gm", partida.resultado().golsCasa())
			.param("gv", partida.resultado().golsFora())
			.param("semente", partida.semente())
			.param("temporada", temporadaId)
			.param("rodada", jogo.rodada())
			.param("mandante", jogo.mandante().id())
			.param("visitante", jogo.visitante().id())
			.query(Long.class)
			.optional()
			.orElseThrow(() -> new IllegalStateException("partida " + jogo.mandante().nome() + " x "
					+ jogo.visitante().nome() + " não existe ou já foi jogada"));
		for (EventoPartida evento : partida.resultado().eventos()) {
			if (!EVENTOS_GRAVADOS.contains(evento.tipo())) {
				continue;
			}
			Clube clube = evento.lado() == Lado.CASA ? jogo.mandante() : jogo.visitante();
			jdbc.sql("""
					INSERT INTO jogo_evento (partida_id, minuto, clube_id, jogador_id, tipo, penalti)
					VALUES (:partida, :minuto, :clube, :jogador, :tipo, :penalti)""")
				.param("partida", partidaId)
				.param("minuto", evento.minuto())
				.param("clube", clube.id())
				.param("jogador", evento.jogador().id())
				.param("tipo", evento.tipo().name())
				.param("penalti", evento.penalti())
				.update();
		}
	}

	private void gravarDisciplina(Map<Long, List<Jogador>> planteis, Disciplina antes, Disciplina depois) {
		planteis.values().stream().flatMap(List::stream).mapToLong(Jogador::id).forEach(id -> {
			if (antes.amarelosDe(id) != depois.amarelosDe(id) || antes.suspensaoDe(id) != depois.suspensaoDe(id)
					|| antes.lesaoDe(id) != depois.lesaoDe(id)) {
				jdbc.sql("UPDATE jogo_jogador SET amarelos = :amarelos, suspensao = :suspensao, lesao = :lesao WHERE id = :jogador")
					.param("amarelos", depois.amarelosDe(id))
					.param("suspensao", depois.suspensaoDe(id))
					.param("lesao", depois.lesaoDe(id))
					.param("jogador", id)
					.update();
			}
		});
	}

	/** Amarelos, suspensões e lesões dos jogadores; as lesões dos clubes contam os eventos da temporada atual. */
	private Disciplina carregarDisciplina(long jogoId) {
		Map<Long, Integer> amarelos = new HashMap<>();
		Map<Long, Integer> suspensoes = new HashMap<>();
		Map<Long, Integer> lesoes = new HashMap<>();
		jdbc.sql("""
				SELECT j.id, j.amarelos, j.suspensao, j.lesao FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id
				WHERE c.jogo_id = :jogo AND (j.amarelos > 0 OR j.suspensao > 0 OR j.lesao > 0)""")
			.param("jogo", jogoId)
			.query((rs, n) -> new LinhaDisciplina(rs.getLong("id"), rs.getInt("amarelos"), rs.getInt("suspensao"),
					rs.getInt("lesao")))
			.list()
			.forEach(l -> {
				amarelos.put(l.jogadorId(), l.amarelos());
				suspensoes.put(l.jogadorId(), l.suspensao());
				lesoes.put(l.jogadorId(), l.lesao());
			});
		Map<Long, Integer> lesoesDosClubes = new HashMap<>();
		jdbc.sql("""
				SELECT e.clube_id, count(*) AS lesoes FROM jogo_evento e JOIN jogo_partida p ON p.id = e.partida_id
				WHERE p.temporada_id = :temporada AND e.tipo = 'LESAO'
				GROUP BY e.clube_id""")
			.param("temporada", temporadaAtual(jogoId, false).id())
			.query((rs, n) -> Map.entry(rs.getLong("clube_id"), rs.getInt("lesoes")))
			.list()
			.forEach(l -> lesoesDosClubes.put(l.getKey(), l.getValue()));
		return new Disciplina(amarelos, suspensoes, lesoes, lesoesDosClubes);
	}

	/** Só os clubes que já jogaram; os demais têm a moral inicial. */
	private Map<Long, Moral> carregarMoral(long jogoId) {
		Map<Long, Moral> moral = new HashMap<>();
		jdbc.sql("SELECT id, moral FROM jogo_clube WHERE jogo_id = :jogo AND moral IS NOT NULL")
			.param("jogo", jogoId)
			.query((rs, n) -> Map.entry(rs.getLong("id"), new Moral(rs.getInt("moral"))))
			.list()
			.forEach(e -> moral.put(e.getKey(), e.getValue()));
		return moral;
	}

	private LinhaTemporada temporadaAtual(long jogoId, boolean travar) {
		return jdbc.sql("""
				SELECT id, rodadas_processadas, etapas_copa_processadas FROM jogo_temporada WHERE jogo_id = :jogo
				ORDER BY numero DESC LIMIT 1""" + (travar ? " FOR UPDATE" : ""))
			.param("jogo", jogoId)
			.query((rs, n) -> new LinhaTemporada(rs.getLong("id"), rs.getInt("rodadas_processadas"),
					rs.getInt("etapas_copa_processadas")))
			.optional()
			.orElseThrow(() -> new NoSuchElementException("o jogo " + jogoId + " não existe ou não tem temporada"));
	}

	/** Rodada jogada pelo avanço. */
	public record RodadaJogada(int numero, ProcessamentoRodada.Resultado resultado) {
	}

	/** Etapa da copa jogada pelo avanço, com a copa depois dela (e da fase seguinte, se criada). */
	public record EtapaJogada(int indice, Copa.Etapa etapa, ProcessamentoRodada.Resultado resultado, Copa copa) {
	}

	private record LinhaTemporada(long id, int rodadasProcessadas, int etapasCopa) {
	}

	private record LinhaDisciplina(long jogadorId, int amarelos, int suspensao, int lesao) {
	}
}
