package br.com.managerfoot.persistencia;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.Partida;
import br.com.managerfoot.motor.RegrasPartida;
import br.com.managerfoot.motor.TipoEvento;
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
			TipoEvento.SEGUNDO_AMARELO, TipoEvento.CARTAO_VERMELHO);
	private static final Set<TipoEvento> EXPULSOES = Set.of(TipoEvento.SEGUNDO_AMARELO, TipoEvento.CARTAO_VERMELHO);

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final RodadaRepositorio rodadas;
	private final CaixaRepositorio caixas;
	private final int minutos = ParametrosMotor.padrao().minutos();

	public RodadaAoVivoRepositorio(JdbcClient jdbc, JogoRepositorio jogos, RodadaRepositorio rodadas,
			CaixaRepositorio caixas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.rodadas = rodadas;
		this.caixas = caixas;
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
	 */
	@Transactional
	public void iniciar(long jogoId, long usuarioId, int rodada, Instant agora) {
		exigirClube(jogoId, usuarioId);
		jogos.travar(jogoId);
		Optional<Linha> existente = linha(jogoId);
		if (existente.isPresent()) {
			if (existente.get().rodada() == rodada) {
				return;
			}
			throw new IllegalStateException("a rodada " + existente.get().rodada() + " já está em andamento");
		}
		int processadas = rodadas.rodadasProcessadas(jogoId);
		if (rodada != processadas + 1 || rodada > rodadas.totalDeRodadas(jogoId)) {
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
		RodadaAoVivo rodada = rodadas.preparar(jogoId, linha.get().rodada(), substituicoes);
		Map<Integer, List<PartidaDaRodada>> porDivisao = porDivisao(rodada);
		RelogioRodada relogio = relogio(porDivisao);
		double progresso = progresso(linha.get(), relogio, velocidade, agora);
		boolean terminada = relogio.terminado(progresso);
		if (terminada) {
			finalizar(jogoId, linha.get().rodada(), substituicoes);
		}
		return Optional.of(montar(jogoId, clube, linha.get(), rodada, porDivisao, relogio, progresso, terminada,
				velocidade));
	}

	/** Pausa o relógio, só durante a divisão do usuário (rev. 30). */
	@Transactional
	public void pausar(long jogoId, long usuarioId, Instant agora) {
		exigirClube(jogoId, usuarioId);
		Linha linha = linha(jogoId).orElseThrow(() -> new IllegalStateException("não há rodada em andamento"));
		if (linha.andandoDesde() == null) {
			return;
		}
		RelogioRodada relogio = relogio(porDivisao(rodadas.preparar(jogoId, linha.rodada(), substituicoes(jogoId))));
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
			RelogioRodada relogio = relogio(porDivisao(rodadas.preparar(jogoId, linha.get().rodada(), substituicoes(jogoId))));
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
		RodadaAoVivo rodada = rodadas.preparar(jogoId, linha.rodada(), substituicoes(jogoId));
		Map<Integer, List<PartidaDaRodada>> porDivisao = porDivisao(rodada);
		RelogioRodada relogio = relogio(porDivisao);
		PartidaDaRodada partida = rodada.partidas().stream()
			.filter(p -> p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Seu clube folga nesta rodada."));
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
	 * no clube do usuário, o banco e as substituições. Com {@code sai} e {@code entra}, também a
	 * formação que ficaria com a troca. Vazio se o jogo não é da divisão em andamento.
	 */
	@Transactional(readOnly = true)
	public Optional<JanelaJogo> janela(long jogoId, long usuarioId, long mandanteId, Long sai, Long entra, Instant agora) {
		long clube = exigirClube(jogoId, usuarioId);
		Optional<Linha> linha = linha(jogoId);
		if (linha.isEmpty()) {
			return Optional.empty();
		}
		RodadaAoVivo rodada = rodadas.preparar(jogoId, linha.get().rodada(), substituicoes(jogoId));
		Map<Integer, List<PartidaDaRodada>> porDivisao = porDivisao(rodada);
		RelogioRodada relogio = relogio(porDivisao);
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
		String previa = null;
		String erroPrevia = null;
		if (doUsuario != null && sai != null && entra != null) {
			try {
				previa = rodada.formacaoCom(p, minuto, doUsuario, sai, entra).rotulo();
			}
			catch (IllegalArgumentException e) {
				erroPrevia = e.getMessage();
			}
		}
		int maximo = RegrasPartida.padrao().substituicoes();
		List<TimeNaJanela> times = new ArrayList<>();
		for (Lado lado : Lado.values()) {
			Set<Long> amarelados = new HashSet<>();
			eventos.stream().filter(e -> e.lado() == lado && e.tipo() == TipoEvento.CARTAO_AMARELO)
				.forEach(e -> amarelados.add(e.jogador().id()));
			times.add(new TimeNaJanela(p.clube(lado), jogo.formacaoAtual(lado), jogo.emCampo(lado),
					lado == doUsuario ? jogo.reservasDisponiveis(lado) : List.of(), maximo - jogo.substituicoesRestantes(lado),
					maximo, eventos.stream().filter(e -> e.lado() == lado && e.tipo() == TipoEvento.SUBSTITUICAO).toList(),
					amarelados));
		}
		return Optional.of(new JanelaJogo(p.jogo().mandante().id(), minuto, linha.get().andandoDesde() == null,
				placar(rodada, p, minuto, clube, caixas.publicos(jogoId, List.of(p))), eventos.stream().filter(e -> LANCES.contains(e.tipo())).toList(), doUsuario,
				times.get(0), times.get(1), previa, erroPrevia));
	}

	/** Grava a rodada pelo caminho de sempre e encerra a rodada ao vivo; o segundo pedido não faz nada. */
	private void finalizar(long jogoId, int rodada, List<RodadaAoVivo.Substituicao> substituicoes) {
		int apagadas = jdbc.sql("DELETE FROM jogo_rodada_ao_vivo WHERE jogo_id = :jogo AND rodada = :rodada")
			.param("jogo", jogoId)
			.param("rodada", rodada)
			.update();
		if (apagadas == 1) {
			rodadas.avancar(jogoId, rodada, substituicoes);
		}
	}

	private EstadoRodada montar(long jogoId, long clube, Linha linha, RodadaAoVivo rodada,
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
		List<DivisaoNaRodada> divisoes = new ArrayList<>();
		for (int i = 0; i < prioridades.size(); i++) {
			Situacao situacao = terminada || i < atual ? Situacao.TERMINADA : i == atual ? Situacao.JOGANDO : Situacao.AGUARDANDO;
			divisoes.add(new DivisaoNaRodada(nomes.get(prioridades.get(i)), situacao, relogio.comUsuario().get(i)));
		}
		Map<Long, Integer> publicos = caixas.publicos(jogoId, rodada.partidas());
		List<PlacarAoVivo> placares = porDivisao.get(prioridades.get(atual)).stream()
			.map(p -> placar(rodada, p, minuto, clube, publicos))
			.toList();
		// o jogo do usuário no minuto em que ele está: o final, se a divisão dele já terminou
		PlacarAoVivo doUsuario = null;
		for (int i = 0; i < prioridades.size(); i++) {
			for (PartidaDaRodada p : porDivisao.get(prioridades.get(i))) {
				if (p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube) {
					doUsuario = placar(rodada, p, terminada || i < atual ? Integer.MAX_VALUE : i == atual ? minuto : 0, clube, publicos);
				}
			}
		}
		int temporada = jdbc.sql("""
				SELECT j.ano_inicial + max(t.numero) - 1 FROM jogo j JOIN jogo_temporada t ON t.jogo_id = j.id
				WHERE j.id = :jogo GROUP BY j.ano_inicial""").param("jogo", jogoId).query(Integer.class).single();
		boolean som = jdbc.sql("SELECT som_gols FROM jogo_treinador WHERE jogo_id = :jogo AND clube_id = :clube")
			.param("jogo", jogoId).param("clube", clube).query(Boolean.class).single();
		return new EstadoRodada(jogoId, temporada, linha.rodada(), divisoes, atual, minuto, linha.andandoDesde() == null,
				terminada, velocidade, relogio.comUsuario().get(atual), placares, doUsuario, som);
	}

	private static PlacarAoVivo placar(RodadaAoVivo rodada, PartidaDaRodada p, int minuto, long clube,
			Map<Long, Integer> publicos) {
		List<EventoPartida> eventos = rodada.eventosAte(p, minuto);
		int golsCasa = (int) eventos.stream().filter(e -> e.tipo() == TipoEvento.GOL && e.lado() == Lado.CASA).count();
		int golsFora = (int) eventos.stream().filter(e -> e.tipo() == TipoEvento.GOL && e.lado() == Lado.FORA).count();
		EventoPartida ultimo = eventos.stream().filter(e -> LANCES.contains(e.tipo())).reduce((a, b) -> b).orElse(null);
		return new PlacarAoVivo(p.jogo().mandante(), p.jogo().visitante(), publicos.get(p.jogo().mandante().id()),
				golsCasa, golsFora,
				p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube, ultimo,
				eventos.stream().anyMatch(e -> EXPULSOES.contains(e.tipo()) && e.lado() == Lado.CASA),
				eventos.stream().anyMatch(e -> EXPULSOES.contains(e.tipo()) && e.lado() == Lado.FORA));
	}

	/** Partidas por divisão, na ordem de exibição (da maior prioridade para a 1ª). */
	private static Map<Integer, List<PartidaDaRodada>> porDivisao(RodadaAoVivo rodada) {
		Map<Integer, List<PartidaDaRodada>> porDivisao = new LinkedHashMap<>();
		rodada.partidas().forEach(p -> porDivisao.computeIfAbsent(p.prioridade(), k -> new ArrayList<>()).add(p));
		return porDivisao;
	}

	private RelogioRodada relogio(Map<Integer, List<PartidaDaRodada>> porDivisao) {
		return new RelogioRodada(porDivisao.values().stream()
			.map(ps -> ps.stream().anyMatch(p -> !p.ladosDoUsuario().isEmpty()))
			.toList(), minutos);
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
		return jdbc.sql("SELECT rodada, progresso, andando_desde FROM jogo_rodada_ao_vivo WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query((rs, n) -> {
				OffsetDateTime desde = rs.getObject("andando_desde", OffsetDateTime.class);
				return new Linha(rs.getInt("rodada"), rs.getDouble("progresso"), desde == null ? null : desde.toInstant());
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

	private record Linha(int rodada, double progresso, Instant andandoDesde) {
	}

	/**
	 * Um lado na janela do jogo.
	 *
	 * @param banco reservas que ainda podem entrar; vazio no clube que não é do usuário
	 */
	public record TimeNaJanela(Clube clube, Formacao formacao, List<Jogador> emCampo, List<Jogador> banco,
			int substituicoesFeitas, int substituicoesMaximas, List<EventoPartida> substituicoes, Set<Long> amarelados) {
	}

	/**
	 * A janela de um jogo, pausado ou não.
	 *
	 * @param doUsuario o lado do clube do usuário; nulo se o jogo não é dele
	 * @param previa formação com a troca marcada; nula sem troca marcada ou se ela não vale
	 * @param erroPrevia por que a troca marcada não vale
	 */
	public record JanelaJogo(long mandanteId, int minuto, boolean pausada, PlacarAoVivo placar, List<EventoPartida> lances,
			Lado doUsuario, TimeNaJanela casa, TimeNaJanela fora, String previa, String erroPrevia) {

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
	 */
	public record PlacarAoVivo(Clube mandante, Clube visitante, int publico, int golsCasa, int golsFora, boolean doUsuario,
			EventoPartida ultimoLance, boolean expulsaoCasa, boolean expulsaoFora) {
	}

	/**
	 * O quadro da rodada num instante.
	 *
	 * @param temporada ano da temporada
	 * @param divisaoAtual índice em {@code divisoes} da divisão em jogo
	 * @param comUsuario se a divisão em jogo tem clube do usuário (só ela tem velocidade e pausa)
	 * @param jogoDoUsuario o jogo do clube do usuário até onde ele já foi jogado; nulo na folga
	 * @param somGols se a tela toca o som dos gols (SPEC §10.4)
	 */
	public record EstadoRodada(long jogoId, int temporada, int rodada, List<DivisaoNaRodada> divisoes, int divisaoAtual,
			int minuto, boolean pausada, boolean terminada, Velocidade velocidade, boolean comUsuario,
			List<PlacarAoVivo> partidas, PlacarAoVivo jogoDoUsuario, boolean somGols) {

		/** Gols da divisão em jogo até o minuto: a tela toca o som quando o número sobe. */
		public int golsNaDivisao() {
			return partidas.stream().mapToInt(p -> p.golsCasa() + p.golsFora()).sum();
		}
	}
}
