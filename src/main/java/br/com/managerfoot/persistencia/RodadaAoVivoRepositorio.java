package br.com.managerfoot.persistencia;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.motor.DisputaPenaltis;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.JogoDecisivo;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.Partida;
import br.com.managerfoot.motor.RegrasPartida;
import br.com.managerfoot.motor.TipoEvento;
import br.com.managerfoot.temporada.Continental;
import br.com.managerfoot.temporada.Copa;
import br.com.managerfoot.temporada.ProcessamentoRodada;
import br.com.managerfoot.temporada.RelogioRodada;
import br.com.managerfoot.temporada.RodadaAoVivo;
import br.com.managerfoot.temporada.RodadaAoVivo.PartidaDaRodada;
import br.com.managerfoot.temporada.Velocidade;

/**
 * Rodada ao vivo do jogo individual (migração V13; docs/decisions/0010; SPEC rev. 35). O relógio é
 * calculado do tempo passado; as partidas são refeitas pela semente e pelas substituições gravadas.
 * Quem chama informa o instante atual. Toda operação confere que o usuário é treinador do jogo.
 */
@Repository
public class RodadaAoVivoRepositorio {

	private static final Set<TipoEvento> LANCES = Set.of(TipoEvento.GOL, TipoEvento.CARTAO_AMARELO,
			TipoEvento.SEGUNDO_AMARELO, TipoEvento.CARTAO_VERMELHO, TipoEvento.LESAO);
	private static final Set<TipoEvento> EXPULSOES = Set.of(TipoEvento.SEGUNDO_AMARELO, TipoEvento.CARTAO_VERMELHO);

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final RodadaRepositorio rodadas;
	private final CaixaRepositorio caixas;
	private final AumentoRepositorio aumentos;
	private final TrocaRepositorio trocas;
	private final CopaRepositorio copas;
	private final ContinentalRepositorio continentais;
	private final int minutos = ParametrosMotor.padrao().minutos();

	public RodadaAoVivoRepositorio(JdbcClient jdbc, JogoRepositorio jogos, RodadaRepositorio rodadas,
			CaixaRepositorio caixas, AumentoRepositorio aumentos, TrocaRepositorio trocas, CopaRepositorio copas,
			ContinentalRepositorio continentais) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.rodadas = rodadas;
		this.caixas = caixas;
		this.aumentos = aumentos;
		this.trocas = trocas;
		this.copas = copas;
		this.continentais = continentais;
	}

	/** Se o jogo tem rodada ao vivo. */
	@Transactional(readOnly = true)
	public boolean emAndamento(long jogoId) {
		return linha(jogoId).isPresent();
	}

	/**
	 * Começa a rodada ao vivo. Pedido repetido da mesma rodada não faz nada.
	 *
	 * @throws IllegalStateException se a rodada não é a próxima, se há leilão aberto antes dela ou se a
	 *         escalação não pode jogar
	 * @throws DecisaoPendenteException se um treinador ainda não respondeu ao convite de outro clube, não
	 *         escolheu o patrocínio da temporada ou não respondeu a um pedido de aumento
	 */
	@Transactional
	public void iniciar(long jogoId, long usuarioId, int rodada, Instant agora) {
		exigirClube(jogoId, usuarioId);
		jogos.travar(jogoId);
		Optional<Linha> existente = linha(jogoId);
		if (existente.isPresent()) {
			if (existente.get().rodada() == rodada && existente.get().etapaCopa() == null
					&& existente.get().etapaContinental() == null) {
				return;
			}
			throw new IllegalStateException("a rodada " + existente.get().rodada() + " já está em andamento");
		}
		if (!(rodadas.proximo(jogoId) instanceof RodadaRepositorio.Proximo.Liga(int proxima)) || proxima != rodada) {
			throw new IllegalStateException("a rodada " + rodada + " não é a próxima do jogo " + jogoId);
		}
		boolean leilaoAberto = jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM jogo_leilao l JOIN jogo_temporada t ON t.id = l.temporada_id
				               WHERE t.jogo_id = :jogo AND l.rodada = :rodada AND l.situacao = 'ABERTO')""")
			.param("jogo", jogoId)
			.param("rodada", rodada)
			.query(Boolean.class)
			.single();
		if (leilaoAberto) {
			// os leilões da máquina são resolvidos antes do minuto a minuto (SPEC §9.4, rev. 41)
			throw new IllegalStateException("há leilões abertos antes da rodada " + rodada);
		}
		exigirSemDecisaoPendente(jogoId);
		rodadas.preparar(jogoId, rodada, List.of());
		jdbc.sql("""
				INSERT INTO jogo_rodada_ao_vivo (jogo_id, temporada_id, rodada, progresso, andando_desde)
				SELECT :jogo, id, :rodada, 0, :agora FROM jogo_temporada WHERE jogo_id = :jogo ORDER BY numero DESC LIMIT 1""")
			.param("jogo", jogoId)
			.param("rodada", rodada)
			.param("agora", OffsetDateTime.ofInstant(agora, ZoneOffset.UTC))
			.update();
	}

	/**
	 * Começa a etapa da copa ao vivo (SPEC §8.2, rev. 70), na mesma tela da rodada. Pedido repetido
	 * da mesma etapa não faz nada.
	 *
	 * @param indice a etapa que quem pede espera jogar
	 * @throws IllegalStateException se a etapa não é a próxima ou se a escalação não pode jogar
	 * @throws DecisaoPendenteException como em {@link #iniciar}
	 */
	@Transactional
	public void iniciarCopa(long jogoId, long usuarioId, int indice, Instant agora) {
		exigirClube(jogoId, usuarioId);
		jogos.travar(jogoId);
		Optional<Linha> existente = linha(jogoId);
		if (existente.isPresent()) {
			if (Integer.valueOf(indice).equals(existente.get().etapaCopa())) {
				return;
			}
			throw new IllegalStateException("a rodada " + existente.get().rodada() + " já está em andamento");
		}
		if (!(rodadas.proximo(jogoId) instanceof RodadaRepositorio.Proximo.EtapaCopa(int proxima, Copa.Etapa etapa))
				|| proxima != indice) {
			throw new IllegalStateException("a etapa " + indice + " da copa não é a próxima do jogo " + jogoId);
		}
		exigirSemDecisaoPendente(jogoId);
		rodadas.prepararCopa(jogoId, indice, List.of());
		jdbc.sql("""
				INSERT INTO jogo_rodada_ao_vivo (jogo_id, temporada_id, rodada, etapa_copa, progresso, andando_desde)
				SELECT :jogo, id, :rodada, :etapa, 0, :agora FROM jogo_temporada WHERE jogo_id = :jogo
				ORDER BY numero DESC LIMIT 1""")
			.param("jogo", jogoId)
			.param("rodada", etapa.depoisDaRodada())
			.param("etapa", indice)
			.param("agora", OffsetDateTime.ofInstant(agora, ZoneOffset.UTC))
			.update();
	}

	/**
	 * Começa a etapa da continental ao vivo (SPEC §8.3.6, rev. 79), na mesma tela da rodada e com todos
	 * os grupos num bloco só. Pedido repetido da mesma etapa não faz nada.
	 *
	 * @param indice a etapa que quem pede espera jogar
	 * @throws IllegalStateException se a etapa não é a próxima ou se a escalação não pode jogar
	 * @throws DecisaoPendenteException como em {@link #iniciar}
	 */
	@Transactional
	public void iniciarContinental(long jogoId, long usuarioId, int indice, Instant agora) {
		exigirClube(jogoId, usuarioId);
		jogos.travar(jogoId);
		Optional<Linha> existente = linha(jogoId);
		if (existente.isPresent()) {
			if (Integer.valueOf(indice).equals(existente.get().etapaContinental())) {
				return;
			}
			throw new IllegalStateException("a rodada " + existente.get().rodada() + " já está em andamento");
		}
		if (!(rodadas.proximo(jogoId) instanceof RodadaRepositorio.Proximo.EtapaContinental(int proxima, Continental.Etapa etapa))
				|| proxima != indice) {
			throw new IllegalStateException("a etapa " + indice + " da continental não é a próxima do jogo " + jogoId);
		}
		exigirSemDecisaoPendente(jogoId);
		rodadas.prepararContinental(jogoId, indice, List.of());
		jdbc.sql("""
				INSERT INTO jogo_rodada_ao_vivo (jogo_id, temporada_id, rodada, etapa_continental, progresso, andando_desde)
				SELECT :jogo, id, :rodada, :etapa, 0, :agora FROM jogo_temporada WHERE jogo_id = :jogo
				ORDER BY numero DESC LIMIT 1""")
			.param("jogo", jogoId)
			.param("rodada", etapa.depoisDaRodada())
			.param("etapa", indice)
			.param("agora", OffsetDateTime.ofInstant(agora, ZoneOffset.UTC))
			.update();
	}

	/** Patrocínio, convite e pedido de aumento esperam a resposta antes de qualquer jogo. */
	private void exigirSemDecisaoPendente(long jogoId) {
		boolean semPatrocinio = jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM jogo_patrocinio p JOIN jogo_temporada t ON t.id = p.temporada_id
				               JOIN jogo_clube c ON c.id = p.clube_id
				               WHERE t.jogo_id = :jogo AND p.escolha IS NULL AND c.divisao_id IS NOT NULL
				                 AND t.numero = (SELECT max(numero) FROM jogo_temporada WHERE jogo_id = :jogo))""")
			.param("jogo", jogoId)
			.query(Boolean.class)
			.single();
		if (semPatrocinio) {
			// a rodada 1 só começa depois da escolha do patrocínio (modelo 9a, rev. 55)
			throw new DecisaoPendenteException("Escolha o patrocínio da temporada antes de jogar a rodada.");
		}
		if (trocas.algumConvitePendente(jogoId)) {
			// o convite para assumir outro clube espera a resposta (modelo 11a, rev. 62)
			throw new DecisaoPendenteException("Responda ao convite para assumir outro clube antes de jogar a rodada.");
		}
		if (aumentos.algumPendente(jogoId)) {
			// o pedido de aumento espera a resposta (modelo 10a, rev. 58)
			throw new DecisaoPendenteException("Responda ao pedido de aumento antes de jogar a rodada.");
		}
	}

	/**
	 * O quadro da rodada no instante. Quando o tempo passa do fim, grava a rodada (uma vez só) e
	 * devolve o estado terminado. Vazio se não há rodada ao vivo.
	 */
	@Transactional
	public Optional<EstadoRodada> estado(long jogoId, long usuarioId, Instant agora) {
		long clube = exigirClube(jogoId, usuarioId);
		// serializa os pedidos do quadro: no fim, só um grava a rodada e os outros a veem gravada
		jogos.travar(jogoId);
		Optional<Linha> linha = linha(jogoId);
		if (linha.isEmpty()) {
			return Optional.empty();
		}
		Velocidade velocidade = velocidade(jogoId, usuarioId);
		List<RodadaAoVivo.Substituicao> substituicoes = substituicoes(jogoId);
		RodadaAoVivo rodada = preparar(jogoId, linha.get(), substituicoes);
		Map<Integer, List<PartidaDaRodada>> porDivisao = porDivisao(rodada);
		RelogioRodada relogio = relogio(rodada, porDivisao);
		Linha atual = linha.get();
		double progresso = progresso(atual, relogio, velocidade, agora);
		if (atual.andandoDesde() != null) {
			OptionalDouble lesao = lesaoDoUsuario(rodada, porDivisao, relogio, clube, atual.lesaoAte(), progresso);
			if (lesao.isPresent()) {
				// lesão no clube do usuário: o relógio para no minuto dela e a janela abre (SPEC §7.5, rev. 69)
				progresso = lesao.getAsDouble();
				gravarRelogio(jogoId, progresso, null);
				jdbc.sql("UPDATE jogo_rodada_ao_vivo SET lesao_ate = :ate WHERE jogo_id = :jogo")
					.param("ate", progresso)
					.param("jogo", jogoId)
					.update();
				atual = new Linha(atual.rodada(), atual.etapaCopa(), atual.etapaContinental(), atual.temporadaId(), progresso,
						null, progresso);
			}
		}
		boolean terminada = relogio.terminado(progresso);
		if (terminada) {
			finalizar(jogoId, atual, substituicoes);
		}
		return Optional.of(montar(jogoId, usuarioId, clube, atual, rodada, porDivisao, relogio, progresso, terminada,
				velocidade));
	}

	/**
	 * Progresso da primeira lesão do clube do usuário entre a última que já pausou e o progresso dado;
	 * lesão no último minuto não pausa (não há mais substituição).
	 */
	private OptionalDouble lesaoDoUsuario(RodadaAoVivo rodada, Map<Integer, List<PartidaDaRodada>> porDivisao,
			RelogioRodada relogio, long clube, double lesaoAte, double progresso) {
		List<Integer> prioridades = new ArrayList<>(porDivisao.keySet());
		for (PartidaDaRodada p : rodada.partidas()) {
			Lado lado = p.jogo().mandante().id() == clube ? Lado.CASA : p.jogo().visitante().id() == clube ? Lado.FORA : null;
			if (lado == null) {
				continue;
			}
			double inicio = prioridades.indexOf(p.prioridade()) * (double) relogio.minutos();
			Partida inteira = rodada.noMinuto(p, Integer.MAX_VALUE);
			return inteira.resultado().eventos().stream()
				.filter(e -> e.tipo() == TipoEvento.LESAO && e.lado() == lado && e.minuto() < inteira.minutoFinal())
				.mapToDouble(e -> inicio + e.minuto())
				.filter(g -> g > lesaoAte && g <= progresso)
				.min();
		}
		return OptionalDouble.empty();
	}

	/** Pausa o relógio, só durante a divisão do usuário (rev. 30). */
	@Transactional
	public void pausar(long jogoId, long usuarioId, Instant agora) {
		exigirClube(jogoId, usuarioId);
		Linha linha = linha(jogoId).orElseThrow(() -> new IllegalStateException("não há rodada em andamento"));
		if (linha.andandoDesde() == null) {
			return;
		}
		RodadaAoVivo rodada = preparar(jogoId, linha, substituicoes(jogoId));
		RelogioRodada relogio = relogio(rodada, porDivisao(rodada));
		double progresso = progresso(linha, relogio, velocidade(jogoId, usuarioId), agora);
		if (!relogio.terminado(progresso) && relogio.comUsuario().get(relogio.divisao(progresso))) {
			gravarRelogio(jogoId, progresso, null);
		}
	}

	@Transactional
	public void continuar(long jogoId, long usuarioId, Instant agora) {
		exigirClube(jogoId, usuarioId);
		jdbc.sql("UPDATE jogo_rodada_ao_vivo SET andando_desde = :agora WHERE jogo_id = :jogo AND andando_desde IS NULL")
			.param("agora", OffsetDateTime.ofInstant(agora, ZoneOffset.UTC))
			.param("jogo", jogoId)
			.update();
	}

	/** Troca a velocidade, guardando o progresso até agora; vale também para as próximas rodadas. */
	@Transactional
	public void mudarVelocidade(long jogoId, long usuarioId, Velocidade nova, Instant agora) {
		exigirClube(jogoId, usuarioId);
		Optional<Linha> linha = linha(jogoId);
		if (linha.isPresent() && linha.get().andandoDesde() != null) {
			RodadaAoVivo rodada = preparar(jogoId, linha.get(), substituicoes(jogoId));
			RelogioRodada relogio = relogio(rodada, porDivisao(rodada));
			gravarRelogio(jogoId, progresso(linha.get(), relogio, velocidade(jogoId, usuarioId), agora), agora);
		}
		jdbc.sql("UPDATE jogo_treinador SET velocidade = :v WHERE jogo_id = :jogo AND usuario_id = :usuario")
			.param("v", nova.name())
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.update();
	}

	/**
	 * Substituição do treinador, com a partida pausada, no minuto em que ela está (SPEC §7.4.1).
	 *
	 * @throws IllegalArgumentException se a troca não é permitida, com o motivo para a tela
	 */
	@Transactional
	public void substituir(long jogoId, long usuarioId, long saiId, long entraId, Instant agora) {
		long clube = exigirClube(jogoId, usuarioId);
		jogos.travar(jogoId);
		Linha linha = linha(jogoId).orElseThrow(() -> new IllegalArgumentException("Não há rodada em andamento."));
		if (linha.andandoDesde() != null) {
			throw new IllegalArgumentException("Pause a partida para substituir.");
		}
		RodadaAoVivo rodada = preparar(jogoId, linha, substituicoes(jogoId));
		Map<Integer, List<PartidaDaRodada>> porDivisao = porDivisao(rodada);
		RelogioRodada relogio = relogio(rodada, porDivisao);
		PartidaDaRodada partida = rodada.partidas().stream()
			.filter(p -> p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(linha.etapaContinental() != null
					? "Seu clube não joga esta etapa da continental."
					: linha.etapaCopa() == null ? "Seu clube folga nesta rodada." : "Seu clube não joga esta etapa da copa."));
		int divisaoAtual = new ArrayList<>(porDivisao.keySet()).get(relogio.divisao(linha.progresso()));
		if (divisaoAtual != partida.prioridade()) {
			throw new IllegalArgumentException("A partida do seu clube não está em andamento.");
		}
		Lado lado = partida.jogo().mandante().id() == clube ? Lado.CASA : Lado.FORA;
		RodadaAoVivo.Substituicao troca = rodada.substituir(partida, relogio.minuto(linha.progresso()), lado, saiId,
				entraId);
		jdbc.sql("""
				INSERT INTO jogo_substituicao_ao_vivo (jogo_id, mandante_id, minuto, lado, sai_id, entra_id)
				VALUES (:jogo, :mandante, :minuto, :lado, :sai, :entra)""")
			.param("jogo", jogoId)
			.param("mandante", troca.mandanteId())
			.param("minuto", troca.minuto())
			.param("lado", troca.lado().name())
			.param("sai", troca.saiId())
			.param("entra", troca.entraId())
			.update();
	}

	/** Liga ou desliga o som dos gols; vale também para as próximas rodadas. */
	@Transactional
	public void definirSom(long jogoId, long usuarioId, boolean ligado) {
		exigirClube(jogoId, usuarioId);
		jdbc.sql("UPDATE jogo_treinador SET som_gols = :som WHERE jogo_id = :jogo AND usuario_id = :usuario")
			.param("som", ligado)
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.update();
	}

	/**
	 * A janela de um jogo da divisão em andamento (SPEC §10.5, rev. 33): lances, os 11 de cada lado e,
	 * no clube do usuário, o banco e as substituições. Vazio se o jogo não é da divisão em andamento.
	 */
	@Transactional(readOnly = true)
	public Optional<JanelaJogo> janela(long jogoId, long usuarioId, long mandanteId, Instant agora) {
		long clube = exigirClube(jogoId, usuarioId);
		Optional<Linha> linha = linha(jogoId);
		if (linha.isEmpty()) {
			return Optional.empty();
		}
		RodadaAoVivo rodada = preparar(jogoId, linha.get(), substituicoes(jogoId));
		Map<Integer, List<PartidaDaRodada>> porDivisao = porDivisao(rodada);
		RelogioRodada relogio = relogio(rodada, porDivisao);
		double progresso = progresso(linha.get(), relogio, velocidade(jogoId, usuarioId), agora);
		if (relogio.terminado(progresso)) {
			return Optional.empty();
		}
		int minuto = relogio.minuto(progresso);
		int prioridade = new ArrayList<>(porDivisao.keySet()).get(relogio.divisao(progresso));
		Optional<PartidaDaRodada> partida = porDivisao.get(prioridade).stream()
			.filter(p -> p.jogo().mandante().id() == mandanteId).findFirst();
		if (partida.isEmpty()) {
			return Optional.empty();
		}
		PartidaDaRodada p = partida.get();
		Partida jogo = rodada.noMinuto(p, minuto);
		List<EventoPartida> eventos = jogo.resultado().eventos();
		Lado doUsuario = p.jogo().mandante().id() == clube ? Lado.CASA : p.jogo().visitante().id() == clube ? Lado.FORA : null;
		int maximo = RegrasPartida.padrao().substituicoes();
		List<TimeNaJanela> times = new ArrayList<>();
		for (Lado lado : Lado.values()) {
			Set<Long> amarelados = new HashSet<>();
			eventos.stream().filter(e -> e.lado() == lado && e.tipo() == TipoEvento.CARTAO_AMARELO)
				.forEach(e -> amarelados.add(e.jogador().id()));
			times.add(new TimeNaJanela(p.clube(lado), jogo.formacaoAtual(lado),
					jogo.emCampo(lado).stream().sorted(Comparator.comparing(Jogador::posicao)).toList(),
					lado == doUsuario ? jogo.reservasDisponiveis(lado) : List.of(), maximo - jogo.substituicoesRestantes(lado),
					maximo, eventos.stream().filter(e -> e.lado() == lado && e.tipo() == TipoEvento.SUBSTITUICAO).toList(),
					amarelados, jogo.lesionadoAguardandoTroca(lado).orElse(null)));
		}
		return Optional.of(new JanelaJogo(p.jogo().mandante().id(), minuto, linha.get().andandoDesde() == null,
				placar(rodada, p, minuto, clube, caixas.publicos(jogoId, List.of(p)), gruposDosMandantes(jogoId, linha.get())), eventos.stream().filter(e -> LANCES.contains(e.tipo())).toList(), doUsuario,
				times.get(0), times.get(1)));
	}

	/**
	 * Grava a rodada ou a etapa da copa ou da continental pelo caminho de sempre e encerra a rodada ao vivo; o segundo
	 * pedido não faz nada.
	 */
	private void finalizar(long jogoId, Linha linha, List<RodadaAoVivo.Substituicao> substituicoes) {
		int apagadas = jdbc.sql("""
				DELETE FROM jogo_rodada_ao_vivo
				WHERE jogo_id = :jogo AND rodada = :rodada AND etapa_copa IS NOT DISTINCT FROM :etapa
				  AND etapa_continental IS NOT DISTINCT FROM :continental""")
			.param("jogo", jogoId)
			.param("rodada", linha.rodada())
			.param("etapa", linha.etapaCopa())
			.param("continental", linha.etapaContinental())
			.update();
		if (apagadas == 1) {
			if (linha.etapaContinental() != null) {
				rodadas.avancarContinental(jogoId, linha.etapaContinental(), substituicoes);
			} else if (linha.etapaCopa() == null) {
				rodadas.avancar(jogoId, linha.rodada(), substituicoes);
			} else {
				rodadas.avancarCopa(jogoId, linha.etapaCopa(), substituicoes);
			}
		}
	}

	/** A rodada da liga ou a etapa da copa ou da continental da linha, pronta para refazer as partidas. */
	private RodadaAoVivo preparar(long jogoId, Linha linha, List<RodadaAoVivo.Substituicao> substituicoes) {
		if (linha.etapaContinental() != null) {
			return rodadas.prepararContinental(jogoId, linha.etapaContinental(), substituicoes);
		}
		return linha.etapaCopa() == null ? rodadas.preparar(jogoId, linha.rodada(), substituicoes)
				: rodadas.prepararCopa(jogoId, linha.etapaCopa(), substituicoes);
	}

	private EstadoRodada montar(long jogoId, long usuarioId, long clube, Linha linha, RodadaAoVivo rodada,
			Map<Integer, List<PartidaDaRodada>> porDivisao, RelogioRodada relogio, double progresso, boolean terminada,
			Velocidade velocidade) {
		List<Integer> prioridades = new ArrayList<>(porDivisao.keySet());
		int atual = relogio.divisao(progresso);
		int minuto = relogio.minuto(progresso);
		Map<Integer, String> nomes = new LinkedHashMap<>();
		jdbc.sql("SELECT prioridade, nome FROM jogo_divisao WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query(rs -> {
				nomes.put(rs.getInt("prioridade"), rs.getString("nome"));
			});
		String copa = linha.etapaCopa() == null ? null
				: "Copa · " + copas.quadro(jogoId, linha.temporadaId()).get(linha.etapaCopa()).nome();
		nomes.put(ProcessamentoRodada.PRIORIDADE_COPA, copa);
		if (linha.etapaContinental() != null) {
			// o bloco tem o nome da competição; o título, a etapa (modelo 13c, rev. 79)
			nomes.put(ProcessamentoRodada.PRIORIDADE_COPA, continentais.nome(jogoId));
			copa = "Continental · " + continentais.quadro(linha.temporadaId()).get(linha.etapaContinental()).nome();
		}
		Map<Long, String> grupos = gruposDosMandantes(jogoId, linha);
		List<DivisaoNaRodada> divisoes = new ArrayList<>();
		for (int i = 0; i < prioridades.size(); i++) {
			Situacao situacao = terminada || i < atual ? Situacao.TERMINADA : i == atual ? Situacao.JOGANDO : Situacao.AGUARDANDO;
			divisoes.add(new DivisaoNaRodada(nomes.get(prioridades.get(i)), situacao, relogio.comUsuario().get(i)));
		}
		Map<Long, Integer> publicos = caixas.publicos(jogoId, rodada.partidas());
		List<PlacarAoVivo> placares = porDivisao.get(prioridades.get(atual)).stream()
			.map(p -> placar(rodada, p, minuto, clube, publicos, grupos))
			.toList();
		// o jogo do usuário no minuto em que ele está: o final, se a divisão dele já terminou
		PlacarAoVivo doUsuario = null;
		for (int i = 0; i < prioridades.size(); i++) {
			for (PartidaDaRodada p : porDivisao.get(prioridades.get(i))) {
				if (p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube) {
					doUsuario = placar(rodada, p, terminada || i < atual ? Integer.MAX_VALUE : i == atual ? minuto : 0, clube, publicos,
							grupos);
				}
			}
		}
		int temporada = jdbc.sql("""
				SELECT j.ano_inicial + max(t.numero) - 1 FROM jogo j JOIN jogo_temporada t ON t.jogo_id = j.id
				WHERE j.id = :jogo GROUP BY j.ano_inicial""").param("jogo", jogoId).query(Integer.class).single();
		// pelo usuário: demitido no fim da rodada, ele já está em outro clube e "clube" é o da rodada
		boolean som = jdbc.sql("SELECT som_gols FROM jogo_treinador WHERE jogo_id = :jogo AND usuario_id = :usuario")
			.param("jogo", jogoId).param("usuario", usuarioId).query(Boolean.class).single();
		Long janelaLesao = !terminada && linha.pausadaNaLesao() && doUsuario != null ? doUsuario.mandante().id() : null;
		return new EstadoRodada(jogoId, temporada, linha.rodada(), copa, divisoes, atual, minuto, relogio.minutos(),
				linha.andandoDesde() == null, terminada, velocidade, relogio.comUsuario().get(atual), placares, doUsuario, som,
				janelaLesao);
	}

	/** Nome do grupo de cada mandante, numa etapa dos grupos da continental; vazio nos outros casos. */
	private Map<Long, String> gruposDosMandantes(long jogoId, Linha linha) {
		if (linha.etapaContinental() == null
				|| !continentais.quadro(linha.temporadaId()).get(linha.etapaContinental()).grupos()) {
			return Map.of();
		}
		Map<Long, String> grupos = new HashMap<>();
		continentais.carregar(jogoId, linha.temporadaId()).orElseThrow().grupos()
			.forEach(g -> g.clubes().forEach(c -> grupos.put(c.id(), g.nome())));
		return grupos;
	}

	private static PlacarAoVivo placar(RodadaAoVivo rodada, PartidaDaRodada p, int minuto, long clube,
			Map<Long, Integer> publicos, Map<Long, String> grupos) {
		Partida partida = rodada.noMinuto(p, minuto);
		List<EventoPartida> eventos = partida.resultado().eventos();
		Optional<DisputaPenaltis> penaltis = partida.terminada() ? partida.resultado().penaltis() : Optional.<DisputaPenaltis>empty();
		int golsCasa = (int) eventos.stream().filter(e -> e.tipo() == TipoEvento.GOL && e.lado() == Lado.CASA).count();
		int golsFora = (int) eventos.stream().filter(e -> e.tipo() == TipoEvento.GOL && e.lado() == Lado.FORA).count();
		EventoPartida ultimo = eventos.stream().filter(e -> LANCES.contains(e.tipo())).reduce((a, b) -> b).orElse(null);
		// etapa da copa (modelo 12b, rev. 71): a ida na volta e, no fim da partida decisiva, quem passa
		Optional<JogoDecisivo> decisivo = p.condicoes().decisivo();
		Optional<JogoDecisivo> volta = decisivo.filter(d -> !d.campoNeutro());
		Clube passa = null;
		if (decisivo.isPresent() && partida.terminada()) {
			int totalCasa = golsCasa + decisivo.get().golsCasaAntes();
			int totalFora = golsFora + decisivo.get().golsForaAntes();
			boolean casaPassa = totalCasa != totalFora ? totalCasa > totalFora
					: penaltis.orElseThrow().gols(Lado.CASA) > penaltis.get().gols(Lado.FORA);
			passa = casaPassa ? p.jogo().mandante() : p.jogo().visitante();
		}
		String grupo = grupos.get(p.jogo().mandante().id());
		return new PlacarAoVivo(p.jogo().mandante(), p.jogo().visitante(), publicos.get(p.jogo().mandante().id()),
				golsCasa, golsFora,
				p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube, ultimo,
				eventos.stream().anyMatch(e -> EXPULSOES.contains(e.tipo()) && e.lado() == Lado.CASA),
				eventos.stream().anyMatch(e -> EXPULSOES.contains(e.tipo()) && e.lado() == Lado.FORA), partida.prorrogacao(),
				penaltis.map(d -> d.gols(Lado.CASA)).orElse(null), penaltis.map(d -> d.gols(Lado.FORA)).orElse(null),
				p.prioridade() == ProcessamentoRodada.PRIORIDADE_COPA && decisivo.isEmpty() && grupo == null,
				volta.map(JogoDecisivo::golsCasaAntes).orElse(null), volta.map(JogoDecisivo::golsForaAntes).orElse(null), passa,
				grupo);
	}

	/** Partidas por divisão, na ordem de exibição (da maior prioridade para a 1ª). */
	private static Map<Integer, List<PartidaDaRodada>> porDivisao(RodadaAoVivo rodada) {
		Map<Integer, List<PartidaDaRodada>> porDivisao = new LinkedHashMap<>();
		rodada.partidas().forEach(p -> porDivisao.computeIfAbsent(p.prioridade(), k -> new ArrayList<>()).add(p));
		return porDivisao;
	}

	/**
	 * Uma divisão depois da outra, com 90 minutos; na etapa da copa, até 120 quando uma partida vai à
	 * prorrogação (rev. 70).
	 */
	private RelogioRodada relogio(RodadaAoVivo rodada, Map<Integer, List<PartidaDaRodada>> porDivisao) {
		int duracao = minutos;
		if (porDivisao.containsKey(ProcessamentoRodada.PRIORIDADE_COPA)) {
			duracao = rodada.partidas().stream().mapToInt(p -> rodada.noMinuto(p, Integer.MAX_VALUE).minutoFinal()).max()
				.orElse(minutos);
		}
		return new RelogioRodada(porDivisao.values().stream()
			.map(ps -> ps.stream().anyMatch(p -> !p.ladosDoUsuario().isEmpty()))
			.toList(), duracao);
	}

	private static double progresso(Linha linha, RelogioRodada relogio, Velocidade velocidade, Instant agora) {
		if (linha.andandoDesde() == null) {
			return linha.progresso();
		}
		long ms = Math.max(0, Duration.between(linha.andandoDesde(), agora).toMillis());
		return relogio.avancar(linha.progresso(), ms, velocidade);
	}

	private void gravarRelogio(long jogoId, double progresso, Instant andandoDesde) {
		jdbc.sql("UPDATE jogo_rodada_ao_vivo SET progresso = :progresso, andando_desde = :desde WHERE jogo_id = :jogo")
			.param("progresso", progresso)
			.param("desde", andandoDesde == null ? null : OffsetDateTime.ofInstant(andandoDesde, ZoneOffset.UTC))
			.param("jogo", jogoId)
			.update();
	}

	private Optional<Linha> linha(long jogoId) {
		return jdbc.sql("""
				SELECT rodada, etapa_copa, etapa_continental, temporada_id, progresso, andando_desde, lesao_ate
				FROM jogo_rodada_ao_vivo
				WHERE jogo_id = :jogo""")
			.param("jogo", jogoId)
			.query((rs, n) -> {
				OffsetDateTime desde = rs.getObject("andando_desde", OffsetDateTime.class);
				return new Linha(rs.getInt("rodada"), rs.getObject("etapa_copa", Integer.class),
						rs.getObject("etapa_continental", Integer.class), rs.getLong("temporada_id"),
						rs.getDouble("progresso"), desde == null ? null : desde.toInstant(), rs.getDouble("lesao_ate"));
			})
			.optional();
	}

	private List<RodadaAoVivo.Substituicao> substituicoes(long jogoId) {
		return jdbc.sql("""
				SELECT mandante_id, minuto, lado, sai_id, entra_id FROM jogo_substituicao_ao_vivo
				WHERE jogo_id = :jogo ORDER BY id""")
			.param("jogo", jogoId)
			.query((rs, n) -> new RodadaAoVivo.Substituicao(rs.getLong("mandante_id"), rs.getInt("minuto"),
					Lado.valueOf(rs.getString("lado")), rs.getLong("sai_id"), rs.getLong("entra_id")))
			.list();
	}

	private Velocidade velocidade(long jogoId, long usuarioId) {
		return jdbc.sql("SELECT velocidade FROM jogo_treinador WHERE jogo_id = :jogo AND usuario_id = :usuario")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.query((rs, n) -> Velocidade.valueOf(rs.getString("velocidade")))
			.single();
	}

	private long exigirClube(long jogoId, long usuarioId) {
		return jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
	}

	/**
	 * @param etapaCopa índice da etapa da copa no quadro; nulo fora dela
	 * @param etapaContinental índice da etapa da continental no quadro; nulo fora dela
	 * @param lesaoAte progresso da última lesão do usuário que já pausou a rodada
	 */
	private record Linha(int rodada, Integer etapaCopa, Integer etapaContinental, long temporadaId, double progresso, Instant andandoDesde,
			double lesaoAte) {

		/** Parada na lesão, esperando a troca do usuário. */
		boolean pausadaNaLesao() {
			return andandoDesde == null && lesaoAte > 0 && progresso == lesaoAte;
		}
	}

	/**
	 * Um lado na janela do jogo.
	 *
	 * @param banco reservas que ainda podem entrar; vazio no clube que não é do usuário
	 * @param lesionado lesionado em campo esperando a troca do usuário (SPEC §7.5); nulo se não há
	 */
	public record TimeNaJanela(Clube clube, Formacao formacao, List<Jogador> emCampo, List<Jogador> banco,
			int substituicoesFeitas, int substituicoesMaximas, List<EventoPartida> substituicoes, Set<Long> amarelados,
			Jogador lesionado) {
	}

	/**
	 * A janela de um jogo, pausado ou não.
	 *
	 * @param doUsuario o lado do clube do usuário; nulo se o jogo não é dele
	 */
	public record JanelaJogo(long mandanteId, int minuto, boolean pausada, PlacarAoVivo placar, List<EventoPartida> lances,
			Lado doUsuario, TimeNaJanela casa, TimeNaJanela fora) {

		public List<TimeNaJanela> times() {
			return List.of(casa, fora);
		}
	}

	public enum Situacao {
		TERMINADA,
		JOGANDO,
		AGUARDANDO
	}

	public record DivisaoNaRodada(String nome, Situacao situacao, boolean comUsuario) {
	}

	/**
	 * Um jogo no quadro da rodada.
	 *
	 * @param publico pagantes, o mesmo que o avanço grava (modelo 1a)
	 * @param ultimoLance gol, amarelo ou vermelho mais recente; nulo se não houve (SPEC rev. 33)
	 * @param prorrogacao a partida da copa foi para a prorrogação
	 * @param penaltisCasa gols do mandante na disputa de pênaltis, depois do fim; nulo sem disputa
	 * @param primeiroJogo a ida de um confronto da copa
	 * @param idaCasa na volta da copa, os gols que o mandante desta partida marcou na ida; nulo fora da volta
	 * @param passa no fim da volta ou da final, quem segue na copa; nulo antes do fim e fora delas
	 * @param grupo nos grupos da continental, o nome do grupo ("Grupo C"); nulo fora deles
	 */
	public record PlacarAoVivo(Clube mandante, Clube visitante, int publico, int golsCasa, int golsFora, boolean doUsuario,
			EventoPartida ultimoLance, boolean expulsaoCasa, boolean expulsaoFora, boolean prorrogacao, Integer penaltisCasa,
			Integer penaltisFora, boolean primeiroJogo, Integer idaCasa, Integer idaFora, Clube passa, String grupo) {

		/** É a volta da copa: a tela mostra a ida e o total (modelo 12b). */
		public boolean volta() {
			return idaCasa != null;
		}

		public int totalCasa() {
			return golsCasa + (idaCasa == null ? 0 : idaCasa);
		}

		public int totalFora() {
			return golsFora + (idaFora == null ? 0 : idaFora);
		}
	}

	/**
	 * O quadro da rodada num instante.
	 *
	 * @param temporada ano da temporada
	 * @param copa nome da etapa ("Copa · Oitavas de final · ida", "Continental · Grupos · 5º jogo"); nulo
	 *        numa rodada da liga
	 * @param minutos duração do relógio de cada divisão: 90, ou 120 na etapa da copa com prorrogação
	 * @param divisaoAtual índice em {@code divisoes} da divisão em jogo
	 * @param comUsuario se a divisão em jogo tem clube do usuário (só ela tem velocidade e pausa)
	 * @param janelaLesao mandante do jogo do usuário quando a rodada parou numa lesão dele (a janela abre
	 *        sozinha); nulo nos outros casos
	 * @param jogoDoUsuario o jogo do clube do usuário até onde ele já foi jogado; nulo na folga
	 * @param somGols se a tela toca o som dos gols (SPEC §10.4)
	 */
	public record EstadoRodada(long jogoId, int temporada, int rodada, String copa, List<DivisaoNaRodada> divisoes,
			int divisaoAtual, int minuto, int minutos, boolean pausada, boolean terminada, Velocidade velocidade, boolean comUsuario,
			List<PlacarAoVivo> partidas, PlacarAoVivo jogoDoUsuario, boolean somGols, Long janelaLesao) {

		/** É uma etapa da continental (modelo 13c, rev. 79). */
		public boolean continental() {
			return copa != null && copa.startsWith(PREFIXO_CONTINENTAL);
		}

		/** Título do bloco em jogo: o nome dele e, na continental, também a etapa. */
		public String tituloDoBloco() {
			String nome = divisoes.get(divisaoAtual).nome();
			return continental() ? nome + " · " + copa.substring(PREFIXO_CONTINENTAL.length()) : nome;
		}

		private static final String PREFIXO_CONTINENTAL = "Continental · ";

		/** Gols da divisão em jogo até o minuto: a tela toca o som quando o número sobe. */
		public int golsNaDivisao() {
			return partidas.stream().mapToInt(p -> p.golsCasa() + p.golsFora()).sum();
		}
	}
}
