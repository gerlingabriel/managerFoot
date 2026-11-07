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
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
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
import br.com.managerfoot.temporada.FaseCopa;
import br.com.managerfoot.temporada.Mundial;
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

	/** O que aparece entre os lances: gol, cartão, lesão e pênalti defendido. */
	private static boolean lance(EventoPartida evento) {
		return LANCES.contains(evento.tipo()) || evento.penaltiDefendido();
	}

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final RodadaRepositorio rodadas;
	private final CaixaRepositorio caixas;
	private final AumentoRepositorio aumentos;
	private final TrocaRepositorio trocas;
	private final CopaRepositorio copas;
	private final ContinentalRepositorio continentais;
	private final int minutos = ParametrosMotor.padrao().minutos();
	private final Duration pausaEmRede;
	private final Duration fimNaTela;

	public RodadaAoVivoRepositorio(JdbcClient jdbc, JogoRepositorio jogos, RodadaRepositorio rodadas,
			CaixaRepositorio caixas, AumentoRepositorio aumentos, TrocaRepositorio trocas, CopaRepositorio copas,
			ContinentalRepositorio continentais,
			@Value("${managerfoot.rede.pausa-da-substituicao}") Duration pausaEmRede,
			@Value("${managerfoot.rodada.fim-na-tela}") Duration fimNaTela) {
		this.pausaEmRede = pausaEmRede;
		this.fimNaTela = fimNaTela;
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
					&& existente.get().etapaContinental() == null && existente.get().etapaMundial() == null) {
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
		rodadas.garantirBonusTreinador(jogoId);
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
		rodadas.garantirBonusTreinador(jogoId);
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
		rodadas.garantirBonusTreinador(jogoId);
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

	/**
	 * Começa ao vivo uma etapa do Mundial (SPEC §8.3.4, rev. 88), como a da continental. Repetir não começa outra.
	 *
	 * @throws DecisaoPendenteException como em {@link #iniciar}
	 */
	@Transactional
	public void iniciarMundial(long jogoId, long usuarioId, int indice, Instant agora) {
		exigirClube(jogoId, usuarioId);
		jogos.travar(jogoId);
		Optional<Linha> existente = linha(jogoId);
		if (existente.isPresent()) {
			if (Integer.valueOf(indice).equals(existente.get().etapaMundial())) {
				return;
			}
			throw new IllegalStateException("a rodada " + existente.get().rodada() + " já está em andamento");
		}
		if (!(rodadas.proximo(jogoId) instanceof RodadaRepositorio.Proximo.EtapaMundial(int proxima, FaseCopa fase)) || proxima != indice) {
			throw new IllegalStateException("a etapa " + indice + " do Mundial não é a próxima do jogo " + jogoId);
		}
		exigirSemDecisaoPendente(jogoId);
		rodadas.garantirBonusTreinador(jogoId);
		RodadaAoVivo aoVivo = rodadas.prepararMundial(jogoId, indice, List.of());
		jdbc.sql("""
				INSERT INTO jogo_rodada_ao_vivo (jogo_id, temporada_id, rodada, etapa_mundial, progresso, andando_desde)
				SELECT :jogo, id, :rodada, :etapa, 0, :agora FROM jogo_temporada WHERE jogo_id = :jogo
				ORDER BY numero DESC LIMIT 1""")
			.param("jogo", jogoId)
			.param("rodada", aoVivo.partidas().getFirst().jogo().rodada())
			.param("etapa", indice)
			.param("agora", OffsetDateTime.ofInstant(agora, ZoneOffset.UTC))
			.update();
	}

	/** Sala do jogo em rede, patrocínio, convite e pedido de aumento esperam antes de qualquer jogo. */
	private void exigirSemDecisaoPendente(long jogoId) {
		boolean naSala = jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_rede WHERE jogo_id = :jogo AND comecou_em IS NULL)")
			.param("jogo", jogoId)
			.query(Boolean.class)
			.single();
		if (naSala) {
			// jogo em rede: nada é jogado antes de o administrador começar a temporada (SPEC §3.3, rev. 92)
			throw new DecisaoPendenteException("A temporada ainda não começou: aguarde o administrador na sala.");
		}
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
	 * O quadro da rodada no instante. Quando o relógio chega ao fim, o quadro fica no último minuto com os placares
	 * finais por {@code managerfoot.rodada.fim-na-tela}; passado esse tempo, grava a rodada (uma vez só) e devolve o
	 * estado gravado. Vazio se não há rodada ao vivo.
	 */
	@Transactional
	public Optional<EstadoRodada> estado(long jogoId, long usuarioId, Instant agora) {
		long clube = exigirClube(jogoId, usuarioId);
		// serializa os pedidos do quadro: no fim, só um grava a rodada e os outros a veem gravada
		jogos.travar(jogoId);
		Optional<Linha> gravada = linha(jogoId);
		if (gravada.isEmpty()) {
			return Optional.empty();
		}
		Linha atual = gravada.get().noInstante(agora);
		if (atual != gravada.get()) {
			// os 60 s da substituição acabaram: o relógio volta a andar de onde parou (SPEC §3.3 item 8)
			gravarRelogio(jogoId, atual.progresso(), atual.andandoDesde());
		}
		Velocidade velocidade = velocidade(jogoId, usuarioId);
		List<RodadaAoVivo.Substituicao> substituicoes = substituicoes(jogoId);
		RodadaAoVivo rodada = preparar(jogoId, atual, substituicoes);
		Map<Integer, List<PartidaDaRodada>> porDivisao = porDivisao(rodada);
		RelogioRodada relogio = relogio(rodada, porDivisao);
		double progresso = progresso(atual, relogio, velocidade, agora);
		if (atual.andandoDesde() != null) {
			Optional<Lesao> lesao = paradaDeTreinador(rodada, porDivisao, relogio, atual.lesaoAte(), progresso,
					paradasDosTreinadores(jogoId));
			if (lesao.isPresent()) {
				// lesão ou expulsão num clube de treinador que marcou a caixa: o relógio para no minuto dela e a janela
				// abre (SPEC §7.5, rev. 69; §10.5, rev. 132); no jogo em rede, para todos por até 60 s, como a
				// substituição (§3.3 item 3)
				progresso = lesao.get().progresso();
				gravarRelogio(jogoId, progresso, null);
				jdbc.sql("UPDATE jogo_rodada_ao_vivo SET lesao_ate = :ate WHERE jogo_id = :jogo")
					.param("ate", progresso)
					.param("jogo", jogoId)
					.update();
				Long pausadaPor = emRede(jogoId) ? treinadorDoClube(jogoId, lesao.get().clube()).orElse(null) : null;
				Instant pausaAte = pausadaPor == null ? null : agora.plus(pausaEmRede);
				if (pausadaPor != null) {
					marcarPausa(jogoId, pausadaPor, pausaAte, agora);
				}
				atual = new Linha(atual.rodada(), atual.etapaCopa(), atual.etapaContinental(), atual.etapaMundial(), atual.temporadaId(), progresso,
						null, progresso, pausadaPor, pausaAte);
			}
		}
		boolean terminada = relogio.terminado(progresso);
		// o último minuto com os placares finais fica na tela antes de ir ao plantel: o relógio já estava no fim
		// fimNaTela atrás (parado no fim, não há o que esperar)
		boolean encerrada = terminada && (atual.andandoDesde() == null
				|| relogio.terminado(progresso(atual, relogio, velocidade, agora.minus(fimNaTela))));
		if (encerrada) {
			finalizar(jogoId, atual, substituicoes);
		}
		return Optional.of(montar(jogoId, usuarioId, clube, atual, rodada, porDivisao, relogio, progresso, terminada,
				encerrada, velocidade, agora));
	}

	/** Lesão ou expulsão que para a rodada: em que ponto do relógio e de qual clube. */
	private record Lesao(double progresso, long clube) {
	}

	/** As caixas da janela do jogo de um treinador (SPEC §10.5, rev. 132; pênaltis a favor na rev. 133). */
	public record Paradas(boolean naLesao, boolean noVermelho, boolean naPenalti) {

		/** As caixas como começam: lesões e pênaltis marcados, vermelhos desmarcado. */
		public static final Paradas INICIAIS = new Paradas(true, false, true);

		/** O lance para o jogo: a cobrança de pênalti tem o minuto em que ele foi marcado. */
		boolean para(EventoPartida e) {
			if (e.penalti()) {
				return naPenalti;
			}
			return e.tipo() == TipoEvento.LESAO ? naLesao : e.tipo().expulsao() && noVermelho;
		}
	}

	/** As caixas de cada clube com treinador, pelo id do clube. */
	private Map<Long, Paradas> paradasDosTreinadores(long jogoId) {
		Map<Long, Paradas> paradas = new HashMap<>();
		jdbc.sql("SELECT clube_id, parar_na_lesao, parar_no_vermelho, parar_no_penalti FROM jogo_treinador WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query(rs -> {
				paradas.put(rs.getLong("clube_id"), new Paradas(rs.getBoolean("parar_na_lesao"), rs.getBoolean("parar_no_vermelho"),
						rs.getBoolean("parar_no_penalti")));
			});
		return paradas;
	}

	/**
	 * A primeira lesão ou expulsão de um clube com treinador, entre a última que já pausou e o progresso dado, que
	 * a caixa dele manda parar (no jogo individual, só o clube do usuário); no último minuto não pausa (não há mais
	 * substituição).
	 */
	private static Optional<Lesao> paradaDeTreinador(RodadaAoVivo rodada, Map<Integer, List<PartidaDaRodada>> porDivisao,
			RelogioRodada relogio, double lesaoAte, double progresso, Map<Long, Paradas> paradas) {
		List<Integer> prioridades = new ArrayList<>(porDivisao.keySet());
		Lesao primeira = null;
		for (PartidaDaRodada p : rodada.partidas()) {
			if (p.ladosDoUsuario().isEmpty()) {
				continue;
			}
			double inicio = prioridades.indexOf(p.prioridade()) * (double) relogio.minutos();
			Partida inteira = rodada.noMinuto(p, Integer.MAX_VALUE);
			for (EventoPartida e : inteira.resultado().eventos()) {
				double g = inicio + e.minuto();
				if (p.ladosDoUsuario().contains(e.lado())
						&& paradas.getOrDefault(p.clube(e.lado()).id(), Paradas.INICIAIS).para(e)
						&& e.minuto() < inteira.minutoFinal()
						&& g > lesaoAte && g <= progresso && (primeira == null || g < primeira.progresso())) {
					primeira = new Lesao(g, p.clube(e.lado()).id());
				}
			}
		}
		return Optional.ofNullable(primeira);
	}

	/**
	 * Clicar num clube pausa o relógio, só durante a divisão com treinador (rev. 30). No jogo em rede
	 * (SPEC §3.3 itens 3 e 8, rev. 90 e 92), só o clique no próprio clube, com a partida dele em andamento,
	 * para o jogo, e para todos por até 60 s; ver o clube de outro não para. Com o jogo já parado, qualquer
	 * clique entra na parada: o jogo só volta quando todos fecham ou o tempo acaba (rev. 102).
	 *
	 * @param mandanteId mandante do jogo clicado
	 */
	@Transactional
	public void pausar(long jogoId, long usuarioId, long mandanteId, Instant agora) {
		long clube = exigirClube(jogoId, usuarioId);
		jogos.travar(jogoId);
		Linha linha = linha(jogoId, agora).orElseThrow(() -> new IllegalStateException("não há rodada em andamento"));
		if (linha.andandoDesde() == null) {
			if (linha.pausadaPor() != null) {
				entrarNaParada(jogoId, usuarioId, agora);
			}
			return;
		}
		RodadaAoVivo rodada = preparar(jogoId, linha, substituicoes(jogoId));
		Map<Integer, List<PartidaDaRodada>> porDivisao = porDivisao(rodada);
		RelogioRodada relogio = relogio(rodada, porDivisao);
		double progresso = progresso(linha, relogio, velocidade(jogoId, usuarioId), agora);
		// no fim de uma divisão, a partida já acabou: não há o que pausar
		if (relogio.terminado(progresso) || relogio.noFimDeDivisao(progresso)
				|| !relogio.comUsuario().get(relogio.divisao(progresso))) {
			return;
		}
		if (!emRede(jogoId)) {
			gravarRelogio(jogoId, progresso, null);
			return;
		}
		int prioridade = new ArrayList<>(porDivisao.keySet()).get(relogio.divisao(progresso));
		int minuto = relogio.minuto(progresso);
		boolean propriaEmAndamento = porDivisao.get(prioridade).stream()
			.filter(p -> p.jogo().mandante().id() == mandanteId)
			.anyMatch(p -> (p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube)
					&& !rodada.noMinuto(p, minuto).terminada());
		if (propriaEmAndamento) {
			gravarRelogio(jogoId, progresso, null);
			marcarPausa(jogoId, usuarioId, agora.plus(pausaEmRede), agora);
		}
	}

	/**
	 * "Continuar" ou fechar a janela: o relógio volta a andar. No jogo em rede, quem fecha sai da parada, e
	 * o jogo só volta quando ninguém mais está nela (rev. 102); quem não está na parada não muda nada.
	 */
	@Transactional
	public void continuar(long jogoId, long usuarioId, Instant agora) {
		exigirClube(jogoId, usuarioId);
		jogos.travar(jogoId);
		Optional<Linha> linha = linha(jogoId, agora);
		if (linha.isEmpty() || linha.get().andandoDesde() != null) {
			return;
		}
		if (linha.get().pausadaPor() != null) {
			int saiu = jdbc.sql("DELETE FROM jogo_pausa_participante WHERE jogo_id = :jogo AND usuario_id = :usuario")
				.param("jogo", jogoId)
				.param("usuario", usuarioId)
				.update();
			if (saiu == 0 || !naParada(jogoId).isEmpty()) {
				return;
			}
		}
		gravarRelogio(jogoId, linha.get().progresso(), agora);
	}

	/**
	 * A pausa da substituição no jogo em rede (modelo 18f), para a contagem dos segundos; vazia se o
	 * relógio está andando ou se o jogo não é em rede.
	 */
	@Transactional(readOnly = true)
	public Optional<PausaEmRede> pausaEmRede(long jogoId, long usuarioId, Instant agora) {
		exigirClube(jogoId, usuarioId);
		return linha(jogoId, agora).flatMap(l -> pausa(jogoId, usuarioId, l, agora));
	}

	private Optional<PausaEmRede> pausa(long jogoId, long usuarioId, Linha linha, Instant agora) {
		if (linha.pausadaPor() == null || linha.andandoDesde() != null) {
			return Optional.empty();
		}
		long segundos = Math.max(0, (Duration.between(agora, linha.pausaAte()).toMillis() + 999) / 1000);
		List<NaParada> naParada = naParada(jogoId);
		List<String> nomes = naParada.stream().map(t -> t.nome() + " (" + t.clube() + ")").toList();
		boolean soQuemParou = naParada.size() == 1 && naParada.getFirst().usuarioId() == linha.pausadaPor();
		return Optional.of(new PausaEmRede(nomes, soQuemParou, naParada.stream().anyMatch(t -> t.usuarioId() == usuarioId),
				linha.pausadaPor() == usuarioId, segundos));
	}

	/** Um treinador com a janela aberta durante a parada. */
	private record NaParada(long usuarioId, String nome, String clube) {
	}

	/** Quem está na parada, na ordem em que entrou. */
	private List<NaParada> naParada(long jogoId) {
		return jdbc.sql("""
				SELECT p.usuario_id, u.nome, c.nome AS clube FROM jogo_pausa_participante p
				JOIN usuario u ON u.id = p.usuario_id
				JOIN jogo_treinador t ON t.jogo_id = p.jogo_id AND t.usuario_id = p.usuario_id
				JOIN jogo_clube c ON c.id = t.clube_id
				WHERE p.jogo_id = :jogo ORDER BY p.desde, p.usuario_id""")
			.param("jogo", jogoId)
			.query((rs, n) -> new NaParada(rs.getLong("usuario_id"), rs.getString("nome"), rs.getString("clube")))
			.list();
	}

	private void marcarPausa(long jogoId, long usuarioId, Instant ate, Instant agora) {
		jdbc.sql("UPDATE jogo_rodada_ao_vivo SET pausada_por = :usuario, pausa_ate = :ate WHERE jogo_id = :jogo")
			.param("usuario", usuarioId)
			.param("ate", OffsetDateTime.ofInstant(ate, ZoneOffset.UTC))
			.param("jogo", jogoId)
			.update();
		entrarNaParada(jogoId, usuarioId, agora);
	}

	private void entrarNaParada(long jogoId, long usuarioId, Instant agora) {
		jdbc.sql("""
				INSERT INTO jogo_pausa_participante (jogo_id, usuario_id, desde) VALUES (:jogo, :usuario, :desde)
				ON CONFLICT DO NOTHING""")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.param("desde", OffsetDateTime.ofInstant(agora, ZoneOffset.UTC))
			.update();
	}

	private Optional<Long> treinadorDoClube(long jogoId, long clubeId) {
		return jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = :jogo AND clube_id = :clube")
			.param("jogo", jogoId)
			.param("clube", clubeId)
			.query(Long.class)
			.optional();
	}

	/** Jogo em rede com a temporada já começada. */
	private boolean emRede(long jogoId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_rede WHERE jogo_id = :jogo AND comecou_em IS NOT NULL)")
			.param("jogo", jogoId)
			.query(Boolean.class)
			.single();
	}

	/** Troca a velocidade, guardando o progresso até agora; vale também para as próximas rodadas. */
	@Transactional
	public void mudarVelocidade(long jogoId, long usuarioId, Velocidade nova, Instant agora) {
		exigirClube(jogoId, usuarioId);
		if (quemEscolheAVelocidade(jogoId, usuarioId).isPresent()) {
			// jogo em rede: só o administrador muda a velocidade, que vale para todos (modelo 18f)
			throw new IllegalArgumentException("Só o administrador muda a velocidade da rodada.");
		}
		Optional<Linha> linha = linha(jogoId, agora);
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
		Linha linha = linha(jogoId, agora).orElseThrow(() -> new IllegalArgumentException("Não há rodada em andamento."));
		if (linha.andandoDesde() != null) {
			throw new IllegalArgumentException(emRede(jogoId)
					? "O jogo voltou a andar. Clique no seu clube para substituir de novo."
					: "Pause a partida para substituir.");
		}
		RodadaAoVivo rodada = preparar(jogoId, linha, substituicoes(jogoId));
		Map<Integer, List<PartidaDaRodada>> porDivisao = porDivisao(rodada);
		RelogioRodada relogio = relogio(rodada, porDivisao);
		PartidaDaRodada partida = rodada.partidas().stream()
			.filter(p -> p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(linha.etapaMundial() != null ? "Seu clube não joga esta etapa do Mundial."
					: linha.etapaContinental() != null
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

	/**
	 * Quem bate o pênalti a favor que espera a cobrança, com a partida pausada (SPEC §12, rev. 133). A escolha é
	 * gravada como as substituições, com o mesmo jogador saindo e entrando, e só vale para esta cobrança.
	 *
	 * @throws IllegalArgumentException se não há pênalti esperando ou o jogador não está em campo, com o motivo
	 */
	@Transactional
	public void escolherBatedor(long jogoId, long usuarioId, long jogadorId, Instant agora) {
		long clube = exigirClube(jogoId, usuarioId);
		jogos.travar(jogoId);
		Linha linha = linha(jogoId, agora).orElseThrow(() -> new IllegalArgumentException("Não há rodada em andamento."));
		if (linha.andandoDesde() != null) {
			throw new IllegalArgumentException("O jogo voltou a andar e o pênalti já foi cobrado.");
		}
		RodadaAoVivo rodada = preparar(jogoId, linha, substituicoes(jogoId));
		Map<Integer, List<PartidaDaRodada>> porDivisao = porDivisao(rodada);
		RelogioRodada relogio = relogio(rodada, porDivisao);
		PartidaDaRodada partida = rodada.partidas().stream()
			.filter(p -> p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Seu clube não joga agora."));
		Lado lado = partida.jogo().mandante().id() == clube ? Lado.CASA : Lado.FORA;
		RodadaAoVivo.Substituicao escolha = rodada.escolherBatedor(partida, relogio.minuto(linha.progresso()), lado, jogadorId);
		jdbc.sql("""
				INSERT INTO jogo_substituicao_ao_vivo (jogo_id, mandante_id, minuto, lado, sai_id, entra_id)
				VALUES (:jogo, :mandante, :minuto, :lado, :jogador, :jogador)""")
			.param("jogo", jogoId)
			.param("mandante", escolha.mandanteId())
			.param("minuto", escolha.minuto())
			.param("lado", escolha.lado().name())
			.param("jogador", jogadorId)
			.update();
	}

	/** As caixas "Lesões" e "Cartões vermelhos" da janela do jogo; valem também para as próximas rodadas (rev. 132). */
	@Transactional
	public void definirParadas(long jogoId, long usuarioId, Paradas paradas) {
		exigirClube(jogoId, usuarioId);
		jdbc.sql("""
				UPDATE jogo_treinador SET parar_na_lesao = :lesao, parar_no_vermelho = :vermelho, parar_no_penalti = :penalti
				WHERE jogo_id = :jogo AND usuario_id = :usuario""")
			.param("lesao", paradas.naLesao())
			.param("vermelho", paradas.noVermelho())
			.param("penalti", paradas.naPenalti())
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.update();
	}

	/** As caixas do treinador (rev. 132). */
	@Transactional(readOnly = true)
	public Paradas paradas(long jogoId, long usuarioId) {
		return jdbc.sql("""
				SELECT parar_na_lesao, parar_no_vermelho, parar_no_penalti FROM jogo_treinador
				WHERE jogo_id = :jogo AND usuario_id = :usuario""")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.query((rs, n) -> new Paradas(rs.getBoolean("parar_na_lesao"), rs.getBoolean("parar_no_vermelho"),
					rs.getBoolean("parar_no_penalti")))
			.optional()
			.orElse(Paradas.INICIAIS);
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
		Optional<Linha> linha = linha(jogoId, agora);
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
					amarelados, jogo.lesionadoAguardandoTroca(lado).orElse(null),
					// a expulsão deste minuto, que pode ter parado o jogo (rev. 132)
					eventos.stream().filter(e -> e.lado() == lado && e.tipo().expulsao() && e.minuto() == minuto).reduce((a, b) -> b)
						.orElse(null),
					lado == doUsuario && jogo.penaltiAguardandoCobranca(lado), jogo.batedorDoPlantelEmCampo(lado).orElse(null)));
		}
		return Optional.of(new JanelaJogo(p.jogo().mandante().id(), minuto, linha.get().andandoDesde() == null,
				placar(rodada, p, minuto, clube, caixas.publicos(jogoId, List.of(p)), gruposDosMandantes(jogoId, linha.get())), eventos.stream().filter(RodadaAoVivoRepositorio::lance).toList(), doUsuario,
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
				  AND etapa_continental IS NOT DISTINCT FROM :continental
				  AND etapa_mundial IS NOT DISTINCT FROM :mundial""")
			.param("jogo", jogoId)
			.param("rodada", linha.rodada())
			.param("etapa", linha.etapaCopa())
			.param("continental", linha.etapaContinental())
			.param("mundial", linha.etapaMundial())
			.update();
		if (apagadas == 1) {
			if (linha.etapaMundial() != null) {
				rodadas.avancarMundial(jogoId, linha.etapaMundial(), substituicoes);
			} else if (linha.etapaContinental() != null) {
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
		if (linha.etapaMundial() != null) {
			return rodadas.prepararMundial(jogoId, linha.etapaMundial(), substituicoes);
		}
		if (linha.etapaContinental() != null) {
			return rodadas.prepararContinental(jogoId, linha.etapaContinental(), substituicoes);
		}
		return linha.etapaCopa() == null ? rodadas.preparar(jogoId, linha.rodada(), substituicoes)
				: rodadas.prepararCopa(jogoId, linha.etapaCopa(), substituicoes);
	}

	private EstadoRodada montar(long jogoId, long usuarioId, long clube, Linha linha, RodadaAoVivo rodada,
			Map<Integer, List<PartidaDaRodada>> porDivisao, RelogioRodada relogio, double progresso, boolean terminada,
			boolean gravada, Velocidade velocidade, Instant agora) {
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
		if (linha.etapaMundial() != null) {
			nomes.put(ProcessamentoRodada.PRIORIDADE_COPA, "Mundial de Clubes");
			copa = "Mundial · " + Mundial.ETAPAS.get(linha.etapaMundial()).nome();
		}
		Map<Long, String> grupos = gruposDosMandantes(jogoId, linha);
		List<DivisaoNaRodada> divisoes = new ArrayList<>();
		// no fim de uma divisão ou da rodada, o último minuto com os placares finais fica na tela (rev. 110 e 111)
		boolean noFim = terminada || relogio.noFimDeDivisao(progresso);
		for (int i = 0; i < prioridades.size(); i++) {
			Situacao situacao = terminada || i < atual || (noFim && i == atual) ? Situacao.TERMINADA : i == atual ? Situacao.JOGANDO : Situacao.AGUARDANDO;
			divisoes.add(new DivisaoNaRodada(nomes.get(prioridades.get(i)), situacao, relogio.comUsuario().get(i)));
		}
		Map<Long, Integer> publicos = caixas.publicos(jogoId, rodada.partidas());
		List<PlacarAoVivo> placares = porDivisao.get(prioridades.get(atual)).stream()
			.map(p -> placar(rodada, p, minuto, clube, publicos, grupos))
			.toList();
		// o jogo do usuário no minuto em que ele está: o final, se a divisão dele já terminou
		PlacarAoVivo doUsuario = null;
		boolean doUsuarioJogando = false;
		for (int i = 0; i < prioridades.size(); i++) {
			for (PartidaDaRodada p : porDivisao.get(prioridades.get(i))) {
				if (p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube) {
					doUsuarioJogando = i == atual;
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
		Optional<PausaEmRede> pausa = terminada ? Optional.empty() : pausa(jogoId, usuarioId, linha, agora);
		// a janela abre sozinha na lesão do jogo individual e, em rede, para quem parou o jogo (modelo 18f)
		boolean abre = pausa.isPresent() ? pausa.get().parouEu() && pausa.get().minha() : linha.pausadaNaLesao();
		Long janelaQueAbre = !terminada && abre && doUsuarioJogando ? doUsuario.mandante().id() : null;
		return new EstadoRodada(jogoId, temporada, linha.rodada(), copa, divisoes, atual, minuto, relogio.minutos(),
				linha.andandoDesde() == null, noFim, gravada, velocidade, relogio.comUsuario().get(atual), placares, doUsuario,
				doUsuario != null && doUsuario.mandante().id() == clube, som,
				janelaQueAbre, quemEscolheAVelocidade(jogoId, usuarioId).orElse(null), emRede(jogoId), pausa.orElse(null));
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
		EventoPartida ultimo = eventos.stream().filter(RodadaAoVivoRepositorio::lance).reduce((a, b) -> b).orElse(null);
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
				(int) eventos.stream().filter(e -> EXPULSOES.contains(e.tipo()) && e.lado() == Lado.CASA).count(),
				(int) eventos.stream().filter(e -> EXPULSOES.contains(e.tipo()) && e.lado() == Lado.FORA).count(), partida.prorrogacao(),
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
			.toList(), duracao, fimNaTela.toMillis());
	}

	private static double progresso(Linha linha, RelogioRodada relogio, Velocidade velocidade, Instant agora) {
		if (linha.andandoDesde() == null) {
			return linha.progresso();
		}
		long ms = Math.max(0, Duration.between(linha.andandoDesde(), agora).toMillis());
		return relogio.avancar(linha.progresso(), ms, velocidade);
	}

	private void gravarRelogio(long jogoId, double progresso, Instant andandoDesde) {
		jdbc.sql("DELETE FROM jogo_pausa_participante WHERE jogo_id = :jogo").param("jogo", jogoId).update();
		// parar ou voltar a andar apaga a pausa em rede; quem para pela substituição a marca depois
		jdbc.sql("""
				UPDATE jogo_rodada_ao_vivo SET progresso = :progresso, andando_desde = :desde, pausada_por = NULL, pausa_ate = NULL
				WHERE jogo_id = :jogo""")
			.param("progresso", progresso)
			.param("desde", andandoDesde == null ? null : OffsetDateTime.ofInstant(andandoDesde, ZoneOffset.UTC))
			.param("jogo", jogoId)
			.update();
	}

	private Optional<Linha> linha(long jogoId) {
		return jdbc.sql("""
				SELECT rodada, etapa_copa, etapa_continental, etapa_mundial, temporada_id, progresso, andando_desde, lesao_ate,
				       pausada_por, pausa_ate
				FROM jogo_rodada_ao_vivo
				WHERE jogo_id = :jogo""")
			.param("jogo", jogoId)
			.query((rs, n) -> {
				OffsetDateTime desde = rs.getObject("andando_desde", OffsetDateTime.class);
				OffsetDateTime ate = rs.getObject("pausa_ate", OffsetDateTime.class);
				return new Linha(rs.getInt("rodada"), rs.getObject("etapa_copa", Integer.class),
						rs.getObject("etapa_continental", Integer.class), rs.getObject("etapa_mundial", Integer.class), rs.getLong("temporada_id"),
						rs.getDouble("progresso"), desde == null ? null : desde.toInstant(), rs.getDouble("lesao_ate"),
						rs.getObject("pausada_por", Long.class), ate == null ? null : ate.toInstant());
			})
			.optional();
	}

	/** A linha como está no instante: com os 60 s da pausa em rede acabados, o relógio já anda. */
	private Optional<Linha> linha(long jogoId, Instant agora) {
		return linha(jogoId).map(l -> l.noInstante(agora));
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

	/**
	 * A velocidade que vale para o relógio. No jogo em rede, o relógio é um só e a velocidade é a do
	 * administrador para todos (SPEC §3.3 item 8, rev. 92; modelo 18f).
	 */
	private Velocidade velocidade(long jogoId, long usuarioId) {
		return jdbc.sql("""
				SELECT t.velocidade FROM jogo_treinador t JOIN jogo j ON j.id = t.jogo_id
				WHERE t.jogo_id = :jogo
				  AND t.usuario_id = CASE WHEN EXISTS (SELECT 1 FROM jogo_rede r WHERE r.jogo_id = :jogo)
				                          AND EXISTS (SELECT 1 FROM jogo_treinador d WHERE d.jogo_id = :jogo AND d.usuario_id = j.dono_id)
				                          THEN j.dono_id ELSE :usuario END""")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.query((rs, n) -> Velocidade.valueOf(rs.getString("velocidade")))
			.single();
	}

	/** No jogo em rede, o nome do administrador para quem não é ele; vazio se quem vê escolhe a velocidade. */
	private Optional<String> quemEscolheAVelocidade(long jogoId, long usuarioId) {
		return jdbc.sql("""
				SELECT u.nome FROM jogo_rede r JOIN jogo j ON j.id = r.jogo_id JOIN usuario u ON u.id = j.dono_id
				WHERE r.jogo_id = :jogo AND j.dono_id <> :usuario""")
			.param("jogo", jogoId)
			.param("usuario", usuarioId)
			.query(String.class)
			.optional();
	}

	private long exigirClube(long jogoId, long usuarioId) {
		return jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
	}

	/**
	 * @param etapaCopa índice da etapa da copa no quadro; nulo fora dela
	 * @param etapaContinental índice da etapa da continental no quadro; nulo fora dela
	 * @param etapaMundial índice da etapa do Mundial; nulo fora dela
	 * @param lesaoAte progresso da última lesão do usuário que já pausou a rodada
	 * @param pausadaPor no jogo em rede, quem parou o jogo para substituir; nulo nos outros casos
	 * @param pausaAte até quando o jogo em rede fica parado; nulo sem {@code pausadaPor}
	 */
	private record Linha(int rodada, Integer etapaCopa, Integer etapaContinental, Integer etapaMundial, long temporadaId, double progresso, Instant andandoDesde,
			double lesaoAte, Long pausadaPor, Instant pausaAte) {
		/** Parada na lesão, esperando a troca do usuário. */
		boolean pausadaNaLesao() {
			return andandoDesde == null && lesaoAte > 0 && progresso == lesaoAte;
		}

		/** Esta linha ou, se a pausa em rede já acabou, a mesma andando desde o fim da pausa. */
		Linha noInstante(Instant agora) {
			if (andandoDesde != null || pausaAte == null || agora.isBefore(pausaAte)) {
				return this;
			}
			return new Linha(rodada, etapaCopa, etapaContinental, etapaMundial, temporadaId, progresso, pausaAte, lesaoAte, null,
					null);
		}
	}

	/**
	 * O jogo em rede parado pela substituição ou lesão de um treinador (modelo 18f; rev. 102).
	 *
	 * @param naParada "Lúcia (Real Guará)" de cada um com a janela aberta, na ordem em que entrou
	 * @param soQuemParou só quem parou está na parada
	 * @param minha quem vê está na parada (com a janela aberta)
	 * @param parouEu quem vê foi quem parou
	 * @param segundos o que falta para o jogo voltar sozinho
	 */
	public record PausaEmRede(List<String> naParada, boolean soQuemParou, boolean minha, boolean parouEu, long segundos) {

		/** Quem vê é o único ainda na parada: os outros esperam por ele. */
		public boolean soEu() {
			return minha && naParada.size() == 1;
		}

		/** "Lúcia (Real Guará) está fazendo substituição", ou quem ainda está mexendo. */
		public String descricao() {
			if (soQuemParou) {
				return naParada.getFirst() + " está fazendo substituição";
			}
			if (naParada.isEmpty()) {
				return "aguardando";
			}
			String nomes = naParada.size() == 1 ? naParada.getFirst()
					: String.join(", ", naParada.subList(0, naParada.size() - 1)) + " e " + naParada.getLast();
			return nomes + (naParada.size() == 1 ? " está mexendo nos times" : " estão mexendo nos times");
		}
	}

	/**
	 * Um lado na janela do jogo.
	 *
	 * @param banco reservas que ainda podem entrar; vazio no clube que não é do usuário
	 * @param lesionado lesionado em campo esperando a troca do usuário (SPEC §7.5); nulo se não há
	 * @param expulsao expulsão do lado neste minuto, a que para o jogo com a caixa marcada (rev. 132); nula se não há
	 * @param penaltiAguardando pênalti a favor esperando a cobrança, para o usuário escolher quem bate (rev. 133)
	 * @param batedorDoPlantel o batedor escolhido no plantel, se está em campo; nulo é o automático
	 */
	public record TimeNaJanela(Clube clube, Formacao formacao, List<Jogador> emCampo, List<Jogador> banco,
			int substituicoesFeitas, int substituicoesMaximas, List<EventoPartida> substituicoes, Set<Long> amarelados,
			Jogador lesionado, EventoPartida expulsao, boolean penaltiAguardando, Jogador batedorDoPlantel) {
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
	 * @param ultimoLance gol, cartão, lesão ou pênalti defendido mais recente; nulo se não houve (SPEC rev. 33)
	 * @param expulsosCasa expulsos do mandante; a tela mostra um cartão vermelho por expulso (rev. 131)
	 * @param prorrogacao a partida da copa foi para a prorrogação
	 * @param penaltisCasa gols do mandante na disputa de pênaltis, depois do fim; nulo sem disputa
	 * @param primeiroJogo a ida de um confronto da copa
	 * @param idaCasa na volta da copa, os gols que o mandante desta partida marcou na ida; nulo fora da volta
	 * @param passa no fim da volta ou da final, quem segue na copa; nulo antes do fim e fora delas
	 * @param grupo nos grupos da continental, o nome do grupo ("Grupo C"); nulo fora deles
	 */
	public record PlacarAoVivo(Clube mandante, Clube visitante, int publico, int golsCasa, int golsFora, boolean doUsuario,
			EventoPartida ultimoLance, int expulsosCasa, int expulsosFora, boolean prorrogacao, Integer penaltisCasa,
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
	 * @param terminada o relógio está no fim da divisão em jogo (ou da rodada): o quadro mostra o último minuto com
	 *        os placares finais antes da próxima divisão ou do plantel
	 * @param gravada a rodada terminou e foi gravada: a tela vai ao plantel
	 * @param divisaoAtual índice em {@code divisoes} da divisão em jogo
	 * @param comUsuario se a divisão em jogo tem clube do usuário (só ela tem velocidade e pausa)
	 * @param janelaQueAbre mandante do jogo do usuário quando a janela dele abre sozinha: a rodada parou
	 *        numa lesão dele ou, em rede, ele parou o jogo para substituir; nulo nos outros casos
	 * @param jogoDoUsuario o jogo do clube do usuário até onde ele já foi jogado; nulo na folga
	 * @param somGols se a tela toca o som dos gols (SPEC §10.4)
	 * @param emRede jogo em rede: só o próprio clube para o jogo, e para todos (§3.3 item 3)
	 * @param pausa no jogo em rede parado pela substituição, quem parou e o tempo que falta; nulo nos outros casos
	 */
	public record EstadoRodada(long jogoId, int temporada, int rodada, String copa, List<DivisaoNaRodada> divisoes,
			int divisaoAtual, int minuto, int minutos, boolean pausada, boolean terminada, boolean gravada, Velocidade velocidade, boolean comUsuario,
			List<PlacarAoVivo> partidas, PlacarAoVivo jogoDoUsuario, boolean usuarioEmCasa, boolean somGols, Long janelaQueAbre,
			String velocidadeDe, boolean emRede, PausaEmRede pausa) {

		/** É uma etapa da continental (modelo 13c, rev. 79). */
		public boolean continental() {
			return copa != null && copa.startsWith(PREFIXO_CONTINENTAL);
		}

		/** É uma etapa do Mundial (rev. 88). */
		public boolean mundial() {
			return copa != null && copa.startsWith(PREFIXO_MUNDIAL);
		}

		/** Título do bloco em jogo: o nome dele e, na continental e no Mundial, também a etapa. */
		public String tituloDoBloco() {
			String nome = divisoes.get(divisaoAtual).nome();
			return continental() ? nome + " · " + copa.substring(PREFIXO_CONTINENTAL.length())
					: mundial() ? nome + " · " + copa.substring(PREFIXO_MUNDIAL.length()) : nome;
		}

		private static final String PREFIXO_MUNDIAL = "Mundial · ";

		private static final String PREFIXO_CONTINENTAL = "Continental · ";

		/** Gols do clube do usuário até o minuto: a tela toca o som quando o número sobe; nulo na folga. */
		public Integer golsPro() {
			return jogoDoUsuario == null ? null : usuarioEmCasa ? jogoDoUsuario.golsCasa() : jogoDoUsuario.golsFora();
		}

		/** Gols sofridos pelo clube do usuário até o minuto; nulo na folga. */
		public Integer golsContra() {
			return jogoDoUsuario == null ? null : usuarioEmCasa ? jogoDoUsuario.golsFora() : jogoDoUsuario.golsCasa();
		}
	}
}
