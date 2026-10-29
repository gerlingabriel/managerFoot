package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.economia.PrecoJogador;
import br.com.managerfoot.economia.Premiacao;
import br.com.managerfoot.temporada.Artilharia;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.ConfrontoCopa;
import br.com.managerfoot.temporada.Copa;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.FaseCopa;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.LinhaClassificacao;
import br.com.managerfoot.temporada.PlacarCopa;
import br.com.managerfoot.temporada.Rodada;
import br.com.managerfoot.temporada.Temporada;

/**
 * Consultas das abas do treinador (SPEC §10.3, rev. 16): classificação por divisão, artilheiros da
 * temporada e de todos os tempos, e a ficha do jogador. Só leitura; o usuário precisa ser treinador
 * do jogo.
 */
@Repository
public class ConsultasRepositorio {

	/** Linhas mostradas nos rankings de artilheiros. */
	private static final int LINHAS_ARTILHEIROS = 20;
	/** Resultados mostrados na janela do clube (modelo 11c). */
	private static final int ULTIMOS_RESULTADOS = 5;

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final RodadaRepositorio rodadas;
	private final TemporadaRepositorio temporadas;
	private final CopaRepositorio copas;
	private final ParametrosMoral parametrosMoral = ParametrosMoral.padrao();
	private final Premiacao premiacao = Premiacao.padrao();

	public ConsultasRepositorio(JdbcClient jdbc, JogoRepositorio jogos, RodadaRepositorio rodadas,
			TemporadaRepositorio temporadas, CopaRepositorio copas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.rodadas = rodadas;
		this.temporadas = temporadas;
		this.copas = copas;
	}

	/**
	 * Classificação da divisão (a do clube do usuário, se {@code prioridade} for nula).
	 *
	 * @throws NoSuchElementException se o usuário não é treinador do jogo
	 */
	@Transactional(readOnly = true)
	public VisaoClassificacao classificacao(long jogoId, long usuarioId, Integer prioridade) {
		long clube = exigirClube(jogoId, usuarioId);
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		Divisao divisao = escolher(liga, clube, prioridade);
		Classificacao classificacao = rodadas.classificacao(jogoId).get(divisao.prioridade());
		return new VisaoClassificacao(abas(liga), divisao.prioridade(), divisao.nome(), rodadas.rodadasProcessadas(jogoId),
				classificacao.linhas(), divisao.rebaixados(), clube);
	}

	/** Artilheiros da temporada atual entre os jogadores dos clubes da divisão (a do usuário, se nula). */
	@Transactional(readOnly = true)
	public VisaoArtilheiros artilheirosDaTemporada(long jogoId, long usuarioId, Integer prioridade) {
		long clube = exigirClube(jogoId, usuarioId);
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		Divisao divisao = escolher(liga, clube, prioridade);
		List<Long> clubes = divisao.clubes().stream().map(Clube::id).toList();
		Map<Long, Integer> gols = gols(jogoId, true);
		gols.keySet().removeIf(id -> !clubes.contains(clubeDoJogador(id)));
		return new VisaoArtilheiros(abas(liga), divisao.prioridade(), divisao.nome(), temporada(jogoId),
				linhas(new Artilharia(gols, Map.of()).rankingTemporada()), clube);
	}

	/** Artilheiros de todos os tempos do jogo salvo, pelos gols na carreira. */
	@Transactional(readOnly = true)
	public VisaoArtilheiros artilheirosDeTodosOsTempos(long jogoId, long usuarioId) {
		long clube = exigirClube(jogoId, usuarioId);
		return new VisaoArtilheiros(abas(jogos.carregarLiga(jogoId)), null, null, temporada(jogoId),
				linhas(new Artilharia(Map.of(), gols(jogoId, false)).rankingCarreira()), clube);
	}

	/** Ficha de um jogador do jogo (SPEC §10.3): gols na temporada e na carreira, força e amarelos. */
	@Transactional(readOnly = true)
	public Optional<Ficha> ficha(long jogoId, long usuarioId, long jogadorId) {
		long doUsuario = exigirClube(jogoId, usuarioId);
		Optional<Long> clube = jdbc.sql("""
				SELECT j.clube_id FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id
				WHERE j.id = :jogador AND c.jogo_id = :jogo""")
			.param("jogador", jogadorId).param("jogo", jogoId).query(Long.class).optional();
		if (clube.isEmpty()) {
			return Optional.empty();
		}
		Jogador jogador = jogos.carregarPlanteis(jogoId).get(clube.get()).stream().filter(j -> j.id() == jogadorId)
			.findFirst().orElseThrow();
		String nomeClube = jdbc.sql("SELECT nome FROM jogo_clube WHERE id = :c").param("c", clube.get())
			.query(String.class).single();
		int amarelos = jdbc.sql("SELECT amarelos FROM jogo_jogador WHERE id = :j").param("j", jogadorId)
			.query(Integer.class).single();
		String pais = jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = :jogo").param("jogo", jogoId).query(String.class).single();
		return Optional.of(new Ficha(jogador, nomeClube, gols(jogoId, true).getOrDefault(jogadorId, 0),
				gols(jogoId, false).getOrDefault(jogadorId, 0), amarelos, clube.get() == doUsuario, Moeda.doPais(pais),
				PrecoJogador.padrao().preco(jogador, pais)));
	}

	/**
	 * Calendário do clube na temporada atual (SPEC §10.3, rev. 17 e 18; modelo 3b, rev. 36): os jogos
	 * da liga e da copa (modelo 12d, rev. 71), com o resultado dos já jogados, sem detalhes dos gols. Vazio se o clube não joga a
	 * temporada (fila de espera) ou não é deste jogo.
	 */
	@Transactional(readOnly = true)
	public Optional<VisaoCalendario> calendario(long jogoId, long usuarioId, long clubeId) {
		exigirClube(jogoId, usuarioId);
		Temporada temporada = temporadas.carregarAtual(jogoId);
		Optional<Temporada.SorteiosDivisao> divisao = temporada.divisoes().stream()
			.filter(d -> d.calendario().jogos().stream()
				.anyMatch(j -> j.mandante().id() == clubeId || j.visitante().id() == clubeId))
			.findFirst();
		if (divisao.isEmpty()) {
			return Optional.empty();
		}
		Map<Integer, int[]> placares = new HashMap<>();
		jdbc.sql("""
				SELECT p.rodada, p.mandante_id, p.gols_mandante, p.gols_visitante FROM jogo_partida p
				JOIN jogo_temporada t ON t.id = p.temporada_id
				WHERE t.jogo_id = :jogo AND t.numero = :numero AND p.gols_mandante IS NOT NULL AND p.confronto_id IS NULL
				  AND (p.mandante_id = :clube OR p.visitante_id = :clube)""")
			.param("jogo", jogoId)
			.param("numero", temporada.numero())
			.param("clube", clubeId)
			.query(rs -> {
				boolean emCasa = rs.getLong("mandante_id") == clubeId;
				int mandante = rs.getInt("gols_mandante");
				int visitante = rs.getInt("gols_visitante");
				placares.put(rs.getInt("rodada"), emCasa ? new int[] { mandante, visitante } : new int[] { visitante, mandante });
			});
		RodadaRepositorio.Proximo proximo = rodadas.proximo(jogoId);
		long temporadaId = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :jogo AND numero = :numero")
			.param("jogo", jogoId).param("numero", temporada.numero()).query(Long.class).single();
		List<JogoNoCalendario> daCopa = jogosDaCopa(jogoId, temporadaId, clubeId, proximo);
		List<JogoNoCalendario> jogosDoClube = new ArrayList<>();
		for (Rodada rodada : divisao.get().calendario().rodadas()) {
			Optional<Jogo> jogo = rodada.jogos().stream()
				.filter(j -> j.mandante().id() == clubeId || j.visitante().id() == clubeId).findFirst();
			int[] placar = placares.get(rodada.numero());
			jogosDoClube.add(new JogoNoCalendario(rodada.numero(), jogo.map(j -> j.mandante().id() == clubeId).orElse(null),
					jogo.map(j -> j.mandante().id() == clubeId ? j.visitante() : j.mandante()).orElse(null),
					placar == null ? null : placar[0], placar == null ? null : placar[1],
					proximo instanceof RodadaRepositorio.Proximo.Liga(int r) && r == rodada.numero(), null, null));
			// os jogos da copa entram depois da rodada da liga em que são jogados (modelo 12d, rev. 71)
			daCopa.stream().filter(c -> c.rodada() == rodada.numero()).forEach(jogosDoClube::add);
		}
		Classificacao classificacao = rodadas.classificacao(jogoId).get(divisao.get().prioridade());
		LinhaClassificacao linha = classificacao.linhas().stream().filter(l -> l.clube().id() == clubeId).findFirst()
			.orElseThrow();
		String nomeDivisao = jdbc.sql("SELECT nome FROM jogo_divisao WHERE jogo_id = :jogo AND prioridade = :p")
			.param("jogo", jogoId).param("p", divisao.get().prioridade()).query(String.class).single();
		// faixa do topo da janela (modelo 11c, rev. 62): treinador, força em campo, moral e últimos 5
		record Treinador(String nome, Integer moral) {
		}
		Treinador treinador = jdbc.sql("""
				SELECT coalesce(u.nome, m.nome) AS nome, c.moral FROM jogo_clube c
				LEFT JOIN jogo_treinador t ON t.clube_id = c.id LEFT JOIN usuario u ON u.id = t.usuario_id
				LEFT JOIN jogo_treinador_maquina m ON m.clube_id = c.id WHERE c.id = :clube""")
			.param("clube", clubeId)
			.query((rs, n) -> new Treinador(rs.getString("nome"), (Integer) rs.getObject("moral")))
			.single();
		Moral moral = treinador.moral() == null ? Moral.inicial(parametrosMoral) : new Moral(treinador.moral());
		List<String> ultimos = jogosDoClube.stream().map(JogoNoCalendario::resultado).filter(r -> r != null).toList();
		ultimos = ultimos.subList(Math.max(0, ultimos.size() - ULTIMOS_RESULTADOS), ultimos.size());
		double forcaEmCampo = Clube.forcaEmCampo(jogos.carregarPlanteis(jogoId).getOrDefault(clubeId, List.of()));
		return Optional.of(new VisaoCalendario(linha.clube(), jogos.ano(jogoId, temporada.numero()), nomeDivisao,
				classificacao.linhas().indexOf(linha) + 1, linha, jogosDoClube, treinador.nome(), forcaEmCampo, moral.valor(),
				parametrosMoral.maximo(), moral.faixa(parametrosMoral), ultimos));
	}

	/**
	 * Aba Copa (modelo 12a, rev. 71): as fases com o momento de cada uma, os confrontos da fase
	 * escolhida, a chave até a final e os clubes fora da copa. Vazio se a temporada não tem copa.
	 *
	 * @param fase a fase mostrada; nula é a fase atual
	 * @throws NoSuchElementException se o usuário não é treinador do jogo
	 */
	@Transactional(readOnly = true)
	public Optional<VisaoCopa> copa(long jogoId, long usuarioId, FaseCopa fase) {
		long clubeId = exigirClube(jogoId, usuarioId);
		Temporada temporada = temporadas.carregarAtual(jogoId);
		long temporadaId = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :jogo AND numero = :numero")
			.param("jogo", jogoId).param("numero", temporada.numero()).query(Long.class).single();
		Optional<Copa> carregada = copas.carregar(jogoId, temporadaId);
		if (carregada.isEmpty()) {
			return Optional.empty();
		}
		Copa copa = carregada.get();
		List<Copa.Etapa> quadro = copas.quadro(jogoId, temporadaId);
		FaseCopa mostrada = fase == null || fase.ordinal() < copa.primeiraFase().ordinal() ? copa.faseAtual() : fase;
		RodadaRepositorio.Proximo proximo = rodadas.proximo(jogoId);

		List<FaseNaCopa> fases = new ArrayList<>();
		for (FaseCopa f = copa.primeiraFase(); f != null; f = f.seguinte().orElse(null)) {
			FaseCopa estaFase = f;
			List<Copa.Etapa> etapas = quadro.stream().filter(e -> e.fase() == estaFase).toList();
			List<ConfrontoCopa> daFase = copa.confrontosDa(f);
			boolean feita = !daFase.isEmpty() && daFase.stream().allMatch(ConfrontoCopa::decidido);
			boolean idaFeita = !daFase.isEmpty() && daFase.stream().allMatch(c -> c.ida().isPresent());
			String quando = f.jogoUnico() ? (feita ? "feita · jogo único" : "jogo único depois da liga")
					: feita ? "feita · rodadas " + etapas.get(0).depoisDaRodada() + " e " + etapas.get(1).depoisDaRodada()
					: idaFeita ? "ida feita · volta depois da " + etapas.get(1).depoisDaRodada()
					: "depois das rodadas " + etapas.get(0).depoisDaRodada() + " e " + etapas.get(1).depoisDaRodada();
			SituacaoDaFase situacao = feita ? SituacaoDaFase.TERMINADA
					: f == copa.faseAtual() ? SituacaoDaFase.JOGANDO : SituacaoDaFase.AGUARDANDO;
			fases.add(new FaseNaCopa(f, f.nome(), quando, situacao, f == mostrada));
		}

		List<ConfrontoNaCopa> confrontos = copa.confrontosDa(mostrada).stream()
			.map(c -> confrontoNaCopa(c, quadro, clubeId))
			.toList();

		// a chave das fases seguintes (chave fixa, rev. 70): o vencedor do 1 pega o do 2
		List<FaseDaChave> chave = new ArrayList<>();
		List<String> anteriores = copa.confrontosDa(mostrada).stream().map(c -> rotuloVencedor(c, clubeId)).toList();
		List<String> codigos = new ArrayList<>();
		for (int k = 0; k < anteriores.size(); k++) {
			codigos.add("Venc. " + (k + 1));
		}
		for (FaseCopa f = mostrada.seguinte().orElse(null); f != null; f = f.seguinte().orElse(null)) {
			List<ConfrontoCopa> criados = copa.confrontosDa(f);
			List<JogoDaChave> jogosDaChave = new ArrayList<>();
			List<String> proximosRotulos = new ArrayList<>();
			List<String> proximosCodigos = new ArrayList<>();
			String letra = f.nome().substring(0, 1);
			for (int k = 0; k < codigos.size() / 2; k++) {
				int ordem = k;
				Optional<ConfrontoCopa> criado = criados.stream().filter(c -> c.ordem() == ordem).findFirst();
				String texto = criado.map(c -> c.mandante().nome() + " × " + c.visitante().nome())
					.orElse(rotulo(anteriores.get(2 * k), codigos.get(2 * k)) + " × "
							+ rotulo(anteriores.get(2 * k + 1), codigos.get(2 * k + 1)));
				boolean seu = criado.map(c -> c.mandante().id() == clubeId || c.visitante().id() == clubeId)
					.orElse(anteriores.get(2 * k).contains(SEU) || anteriores.get(2 * k + 1).contains(SEU));
				jogosDaChave.add(new JogoDaChave(texto.replace(SEU, " (você)"), seu, criado.isEmpty()));
				proximosRotulos.add(criado.map(c -> rotuloVencedor(c, clubeId)).orElse(null));
				proximosCodigos.add("Venc. " + letra + (k + 1));
			}
			chave.add(new FaseDaChave(f.jogoUnico() ? f.nome() + " · campo neutro" : f.nome(), jogosDaChave));
			anteriores = proximosRotulos.stream().map(r -> r == null ? "" : r).toList();
			codigos = proximosCodigos;
		}

		List<Clube> inscritos = copa.confrontosDa(copa.primeiraFase()).stream()
			.flatMap(c -> Stream.of(c.mandante(), c.visitante())).toList();
		List<String> fora = temporada.divisoes().stream()
			.flatMap(d -> d.calendario().jogos().stream().flatMap(j -> Stream.of(j.mandante(), j.visitante())))
			.distinct().filter(c -> !inscritos.contains(c)).map(Clube::nome).toList();
		Optional<Clube> seuClube = inscritos.stream().filter(c -> c.id() == clubeId).findFirst();
		String situacaoDoClube = seuClube.map(c -> situacaoNaCopa(copa, c, quadro, proximo))
			.orElse("Fora da copa nesta temporada");
		int teto = jogos.carregarLiga(jogoId).divisoes().getFirst().limiteForca();
		return Optional.of(new VisaoCopa(jogos.ano(jogoId, temporada.numero()), inscritos.size(),
				jogos.carregarLiga(jogoId).divisoes().size(), premiacao.premioDaCopa(1, teto), premiacao.premioDaCopa(2, teto),
				Moeda.doPais(jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = :jogo").param("jogo", jogoId)
					.query(String.class).single()), situacaoDoClube, fases, mostrada.nome(), mostrada.jogoUnico(), confrontos, chave, fora,
				copa.campeao().map(Clube::nome).orElse(null)));
	}

	/** Marca, no rótulo da chave, o clube do usuário; trocada por " (você)" na tela. */
	private static final String SEU = "§";

	/** Nome do vencedor do confronto, ou "A ou B" enquanto ele não foi decidido. */
	private static String rotuloVencedor(ConfrontoCopa c, long clubeId) {
		return c.vencedor().map(v -> nomeNaChave(v, clubeId))
			.orElse(nomeNaChave(c.mandante(), clubeId) + " ou " + nomeNaChave(c.visitante(), clubeId));
	}

	private static String nomeNaChave(Clube c, long clubeId) {
		return c.id() == clubeId ? c.nome() + SEU : c.nome();
	}

	/**
	 * Na chave, o nome (decidido) ou "A ou B" só no caminho do usuário; nos outros, o código ("Venc. 3"),
	 * como no modelo 12a.
	 */
	private static String rotulo(String rotulo, String codigo) {
		if (rotulo.isEmpty()) {
			return codigo;
		}
		return !rotulo.contains(" ou ") || rotulo.contains(SEU) ? rotulo : codigo;
	}

	private static ConfrontoNaCopa confrontoNaCopa(ConfrontoCopa c, List<Copa.Etapa> quadro, long clubeId) {
		List<Copa.Etapa> etapas = quadro.stream().filter(e -> e.fase() == c.fase()).toList();
		String ida = c.ida().map(p -> p.golsMandante() + " x " + p.golsVisitante()).orElse("—");
		// a volta com os gols do primeiro clube primeiro
		String volta = c.fase().jogoUnico() ? null
				: c.volta().map(p -> p.golsVisitante() + " x " + p.golsMandante()).orElse("—");
		String total = c.ida().isEmpty() ? "—" : c.golsDoMandanteSomados() + " x " + c.golsDoVisitanteSomados();
		Optional<PlacarCopa> decisiva = c.fase().jogoUnico() ? c.ida() : c.volta();
		String extra;
		if (c.ida().isEmpty()) {
			extra = c.fase().jogoUnico() ? "depois da liga" : "ida depois da " + etapas.get(0).depoisDaRodada();
		} else if (!c.decidido()) {
			extra = "volta depois da " + etapas.get(1).depoisDaRodada();
		} else {
			// pênaltis com os gols do primeiro clube primeiro; na volta ele é o visitante
			extra = decisiva.flatMap(PlacarCopa::penaltis)
				.map(p -> c.fase().jogoUnico() ? "pên. " + p.mandante() + " x " + p.visitante()
						: "pên. " + p.visitante() + " x " + p.mandante())
				.orElse(decisiva.get().prorrogacao() ? "prorrogação" : null);
		}
		Optional<Clube> vencedor = c.vencedor();
		return new ConfrontoNaCopa(c.ordem() + 1, c.mandante(), c.visitante(), ida, volta, total, extra,
				vencedor.map(v -> v.equals(c.mandante())).orElse(null), c.mandante().id() == clubeId,
				c.visitante().id() == clubeId);
	}

	/** "Nas oitavas · volta depois da rodada 12", "Eliminado nas Quartas de final", "Campeão". */
	private static String situacaoNaCopa(Copa copa, Clube clube, List<Copa.Etapa> quadro, RodadaRepositorio.Proximo proximo) {
		if (copa.campeao().map(clube::equals).orElse(false)) {
			return "Campeão";
		}
		Optional<ConfrontoCopa> eliminacao = copa.confrontos().stream()
			.filter(c -> c.perdedor().map(clube::equals).orElse(false)).findFirst();
		if (eliminacao.isPresent()) {
			return eliminacao.get().fase().jogoUnico() ? "Vice-campeão" : "Eliminado: " + eliminacao.get().fase().nome();
		}
		String fase = copa.faseAtual().nome();
		if (proximo instanceof RodadaRepositorio.Proximo.EtapaCopa e && e.etapa().fase() == copa.faseAtual()) {
			return "Na disputa · " + e.etapa().nome() + " é o próximo jogo";
		}
		return quadro.stream().filter(e -> e.fase() == copa.faseAtual())
			.filter(e -> copa.jogosDo(clube, quadro).stream().noneMatch(j -> j.etapa().equals(e) && j.golsPro() != null))
			.findFirst()
			.map(e -> "Na disputa · " + e.nome() + " depois da rodada " + e.depoisDaRodada())
			.orElse("Na disputa · " + fase);
	}

	/**
	 * Os jogos da copa do clube na temporada, na ordem do quadro (modelo 12d, rev. 71). A rodada é a da
	 * liga depois da qual o jogo é disputado.
	 */
	private List<JogoNoCalendario> jogosDaCopa(long jogoId, long temporadaId, long clubeId,
			RodadaRepositorio.Proximo proximo) {
		Optional<Copa> copa = copas.carregar(jogoId, temporadaId);
		if (copa.isEmpty()) {
			return List.of();
		}
		Optional<Clube> clube = copa.get().confrontos().stream()
			.flatMap(c -> Stream.of(c.mandante(), c.visitante())).filter(c -> c.id() == clubeId)
			.findFirst();
		if (clube.isEmpty()) {
			return List.of();
		}
		return copa.get().jogosDo(clube.get(), copas.quadro(jogoId, temporadaId)).stream()
			.map(j -> new JogoNoCalendario(j.etapa().depoisDaRodada(), j.emCasa(), j.adversario(), j.golsPro(),
					j.golsContra(), proximo instanceof RodadaRepositorio.Proximo.EtapaCopa e && e.indice() == j.indice(),
					"Copa · " + j.etapa().nome(), j.detalhe()))
			.toList();
	}

	private List<LinhaArtilheiro> linhas(List<Artilharia.Artilheiro> ranking) {
		List<LinhaArtilheiro> linhas = new ArrayList<>();
		for (Artilharia.Artilheiro a : ranking.subList(0, Math.min(LINHAS_ARTILHEIROS, ranking.size()))) {
			jdbc.sql("""
					SELECT j.id, j.nome, j.posicao, c.id AS clube_id, c.nome AS clube
					FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id WHERE j.id = :j""")
				.param("j", a.jogadorId())
				.query(rs -> {
					linhas.add(new LinhaArtilheiro(a.posicao(), a.jogadorId(), rs.getString("nome"),
							Posicao.valueOf(rs.getString("posicao")), rs.getLong("clube_id"),
							rs.getString("clube"), a.gols()));
				});
		}
		return linhas;
	}

	/** Gols por jogador: na temporada atual, ou em todas (carreira, desde o início do jogo salvo). */
	private Map<Long, Integer> gols(long jogoId, boolean soTemporadaAtual) {
		Map<Long, Integer> gols = new HashMap<>();
		jdbc.sql("""
				SELECT e.jogador_id, count(*) AS gols FROM jogo_evento e
				JOIN jogo_partida p ON p.id = e.partida_id JOIN jogo_temporada t ON t.id = p.temporada_id
				WHERE t.jogo_id = :jogo AND e.tipo = 'GOL'"""
				+ (soTemporadaAtual ? " AND t.numero = (SELECT max(numero) FROM jogo_temporada WHERE jogo_id = :jogo)" : "")
				+ " GROUP BY e.jogador_id")
			.param("jogo", jogoId)
			.query(rs -> {
				gols.put(rs.getLong("jogador_id"), rs.getInt("gols"));
			});
		return gols;
	}

	private long clubeDoJogador(long jogadorId) {
		return jdbc.sql("SELECT clube_id FROM jogo_jogador WHERE id = :j").param("j", jogadorId).query(Long.class).single();
	}

	/** Ano da temporada atual (SPEC §8.1.1, rev. 40). */
	private int temporada(long jogoId) {
		return jdbc.sql("""
				SELECT j.ano_inicial + max(t.numero) - 1 FROM jogo j JOIN jogo_temporada t ON t.jogo_id = j.id
				WHERE j.id = :jogo GROUP BY j.ano_inicial""").param("jogo", jogoId)
			.query(Integer.class).single();
	}

	/**
	 * Histórico (SPEC §8.1.1, rev. 40 a 54; modelo 8a): os campeões de cada ano, com o treinador, e o
	 * ranking de treinadores pelos títulos da 1ª divisão, em que empatados dividem a posição.
	 */
	@Transactional(readOnly = true)
	public VisaoHistorico historico(long jogoId, long usuarioId) {
		exigirClube(jogoId, usuarioId);
		Map<Integer, Map<Integer, CampeaoNoAno>> porAno = new TreeMap<>(Comparator.reverseOrder());
		jdbc.sql("""
				SELECT c.ano, c.divisao_prioridade, c.clube_nome, c.usuario_id, coalesce(u.nome, m.nome) AS treinador
				FROM jogo_campeao c
				LEFT JOIN usuario u ON u.id = c.usuario_id
				LEFT JOIN jogo_treinador_maquina m ON m.id = c.treinador_maquina_id
				WHERE c.jogo_id = :jogo""")
			.param("jogo", jogoId)
			.query(rs -> {
				Long usuario = (Long) rs.getObject("usuario_id");
				porAno.computeIfAbsent(rs.getInt("ano"), a -> new HashMap<>()).put(rs.getInt("divisao_prioridade"),
						new CampeaoNoAno(rs.getString("clube_nome"), rs.getString("treinador"),
								usuario != null && usuario == usuarioId, null));
			});
		// a copa (modelo 12e, rev. 71): campeão, treinador e o placar da final do ponto de vista dele
		Map<Integer, CampeaoNoAno> copas = new HashMap<>();
		jdbc.sql("""
				SELECT c.ano, c.clube_id, c.clube_nome, c.vice_nome, c.usuario_id, coalesce(u.nome, m.nome) AS treinador,
				       p.mandante_id, p.gols_mandante, p.gols_visitante, p.penaltis_mandante, p.penaltis_visitante
				FROM jogo_campeao_copa c
				LEFT JOIN usuario u ON u.id = c.usuario_id
				LEFT JOIN jogo_treinador_maquina m ON m.id = c.treinador_maquina_id
				LEFT JOIN jogo_temporada t ON t.jogo_id = c.jogo_id AND t.numero = c.temporada
				LEFT JOIN jogo_partida p ON p.temporada_id = t.id AND p.perna = 'JOGO_UNICO'
				WHERE c.jogo_id = :jogo""")
			.param("jogo", jogoId)
			.query(rs -> {
				Long usuario = (Long) rs.getObject("usuario_id");
				String fin = null;
				if (rs.getObject("gols_mandante") != null) {
					boolean emCasa = rs.getLong("mandante_id") == rs.getLong("clube_id");
					int gols = rs.getInt(emCasa ? "gols_mandante" : "gols_visitante");
					int sofridos = rs.getInt(emCasa ? "gols_visitante" : "gols_mandante");
					Integer penMandante = (Integer) rs.getObject("penaltis_mandante");
					String penaltis = penMandante == null ? ""
							: ", pên. " + rs.getInt(emCasa ? "penaltis_mandante" : "penaltis_visitante") + " x "
									+ rs.getInt(emCasa ? "penaltis_visitante" : "penaltis_mandante");
					fin = "final " + gols + " x " + sofridos + penaltis + " sobre " + rs.getString("vice_nome");
				}
				copas.put(rs.getInt("ano"), new CampeaoNoAno(rs.getString("clube_nome"), rs.getString("treinador"),
						usuario != null && usuario == usuarioId, fin));
			});
		copas.keySet().forEach(ano -> porAno.computeIfAbsent(ano, a -> new HashMap<>()));
		List<AbaDivisao> divisoes = abas(jogos.carregarLiga(jogoId));
		List<AnoNoHistorico> anos = new ArrayList<>();
		porAno.forEach((ano, campeoes) -> anos.add(new AnoNoHistorico(ano,
				divisoes.stream().map(d -> campeoes.get(d.prioridade())).toList(), copas.get(ano))));
		List<TreinadorNoRanking> ranking = new ArrayList<>();
		jdbc.sql("""
				SELECT c.usuario_id, coalesce(u.nome, m.nome) AS nome, coalesce(cu.nome, cm.nome) AS clube, count(*) AS titulos
				FROM jogo_campeao c
				LEFT JOIN usuario u ON u.id = c.usuario_id
				LEFT JOIN jogo_treinador t ON t.jogo_id = c.jogo_id AND t.usuario_id = c.usuario_id
				LEFT JOIN jogo_clube cu ON cu.id = t.clube_id
				LEFT JOIN jogo_treinador_maquina m ON m.id = c.treinador_maquina_id
				LEFT JOIN jogo_clube cm ON cm.id = m.clube_id
				WHERE c.jogo_id = :jogo AND c.divisao_prioridade = 1
				  AND (c.usuario_id IS NOT NULL OR c.treinador_maquina_id IS NOT NULL)
				GROUP BY c.usuario_id, c.treinador_maquina_id, u.nome, m.nome, cu.nome, cm.nome
				ORDER BY titulos DESC, nome""")
			.param("jogo", jogoId)
			.query(rs -> {
				int titulos = rs.getInt("titulos");
				// empatados dividem a posição
				int posicao = ranking.isEmpty() || ranking.getLast().titulos() != titulos ? ranking.size() + 1
						: ranking.getLast().posicao();
				Long usuario = (Long) rs.getObject("usuario_id");
				ranking.add(new TreinadorNoRanking(posicao, rs.getString("nome"), rs.getString("clube"), titulos,
						usuario != null && usuario == usuarioId));
			});
		return new VisaoHistorico(divisoes, anos, ranking, !copas.isEmpty());
	}

	/** A divisão pedida, ou a do clube do usuário (a última, se ele está na fila). */
	private static Divisao escolher(ConfiguracaoLiga liga, long clube, Integer prioridade) {
		if (prioridade != null) {
			return liga.divisoes().stream().filter(d -> d.prioridade() == prioridade).findFirst()
				.orElseThrow(() -> new NoSuchElementException("divisão " + prioridade + " não existe"));
		}
		return liga.divisoes().stream().filter(d -> d.clubes().stream().anyMatch(c -> c.id() == clube)).findFirst()
			.orElse(liga.divisoes().getLast());
	}

	private static List<AbaDivisao> abas(ConfiguracaoLiga liga) {
		return liga.divisoes().stream().map(d -> new AbaDivisao(d.prioridade(), d.nome())).toList();
	}

	private long exigirClube(long jogoId, long usuarioId) {
		return jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
	}

	public record AbaDivisao(int prioridade, String nome) {
	}

	/**
	 * A aba Copa (modelo 12a, rev. 71), em centavos de Real nos prêmios.
	 *
	 * @param ano ano da temporada
	 * @param seuClube situação do clube do usuário na copa
	 * @param fase nome da fase mostrada
	 * @param jogoUnico a fase mostrada é a final: sem a coluna da volta
	 * @param fora clubes das divisões que ficaram fora da copa
	 * @param campeao nulo enquanto a final não foi jogada
	 */
	public record VisaoCopa(int ano, int clubes, int divisoes, long premioCampeao, long premioVice, Moeda moeda,
			String seuClube, List<FaseNaCopa> fases, String fase, boolean jogoUnico, List<ConfrontoNaCopa> confrontos,
			List<FaseDaChave> chave, List<String> fora, String campeao) {
	}

	/**
	 * @param quando "depois das rodadas 18 e 20", "ida feita · volta depois da 12"
	 * @param situacao terminada, a atual (jogando) ou futura (aguardando)
	 */
	public record FaseNaCopa(FaseCopa fase, String nome, String quando, SituacaoDaFase situacao, boolean mostrada) {
	}

	/** Situação de uma fase da copa. */
	public enum SituacaoDaFase {
		TERMINADA, JOGANDO, AGUARDANDO
	}

	/**
	 * Um confronto na aba Copa: o {@code primeiro} jogou a ida em casa; os placares têm os gols dele
	 * primeiro.
	 *
	 * @param volta nula na final
	 * @param extra pênaltis, prorrogação ou quando falta jogar; pode ser nulo
	 * @param primeiroPassa nulo enquanto o confronto não foi decidido
	 */
	public record ConfrontoNaCopa(int numero, Clube primeiro, Clube segundo, String ida, String volta, String total,
			String extra, Boolean primeiroPassa, boolean primeiroSeu, boolean segundoSeu) {

		public boolean doUsuario() {
			return primeiroSeu || segundoSeu;
		}
	}

	public record FaseDaChave(String nome, List<JogoDaChave> jogos) {
	}

	/** @param emAberto o confronto ainda não existe */
	public record JogoDaChave(String texto, boolean doUsuario, boolean emAberto) {
	}

	/**
	 * @param divisoes colunas dos campeões, na ordem das divisões
	 * @param anos do mais recente ao mais antigo
	 * @param temCopa alguma copa já terminou: mostra a coluna Copa
	 */
	public record VisaoHistorico(List<AbaDivisao> divisoes, List<AnoNoHistorico> anos, List<TreinadorNoRanking> ranking,
			boolean temCopa) {
	}

	/**
	 * @param campeoes um por divisão, na ordem de {@link VisaoHistorico#divisoes()}; nulo se não houve
	 * @param copa campeão da copa; nulo se não houve
	 */
	public record AnoNoHistorico(int ano, List<CampeaoNoAno> campeoes, CampeaoNoAno copa) {
	}

	/**
	 * @param seu o treinador era o usuário que vê a tela
	 * @param detalhe na copa, o placar da final ("final 1 x 1, pên. 4 x 3 sobre Vice"); nulo na liga
	 */
	public record CampeaoNoAno(String clube, String treinador, boolean seu, String detalhe) {
	}

	/** @param clube clube atual do treinador; nulo se ele está sem clube */
	public record TreinadorNoRanking(int posicao, String nome, String clube, int titulos, boolean voce) {
	}

	/**
	 * @param rodadas rodadas já jogadas na temporada
	 * @param rebaixados quantos caem no fim, marcados na tabela
	 */
	public record VisaoClassificacao(List<AbaDivisao> divisoes, int prioridade, String divisao, int rodadas,
			List<LinhaClassificacao> linhas, int rebaixados, long clubeDoUsuario) {
	}

	public record LinhaArtilheiro(int posicao, long jogadorId, String nome, Posicao posicaoEmCampo,
			long clubeId, String clube, int gols) {
	}

	/**
	 * @param prioridade nula no ranking de todos os tempos
	 * @param temporada ano da temporada atual
	 */
	public record VisaoArtilheiros(List<AbaDivisao> divisoes, Integer prioridade, String divisao, int temporada,
			List<LinhaArtilheiro> linhas, long clubeDoUsuario) {
	}

	/**
	 * Um jogo no calendário do clube.
	 *
	 * @param rodada na copa, a rodada da liga depois da qual o jogo é disputado
	 * @param emCasa nulo na folga e na final da copa (campo neutro)
	 * @param adversario nulo na folga
	 * @param golsPro nulo enquanto o jogo não foi jogado
	 * @param copa nome do jogo da copa ("Copa · Oitavas de final · ida"); nulo na liga
	 * @param detalhe na copa, o total ou a ida embaixo do placar ("total 3 x 2 · passa"); pode ser nulo
	 */
	public record JogoNoCalendario(int rodada, Boolean emCasa, Clube adversario, Integer golsPro, Integer golsContra,
			boolean proximo, String copa, String detalhe) {

		/** V, E ou D; nulo se ainda não foi jogado. */
		public String resultado() {
			if (golsPro == null) {
				return null;
			}
			return golsPro > golsContra ? "V" : golsPro.equals(golsContra) ? "E" : "D";
		}
	}

	/**
	 * @param temporada ano da temporada
	 * @param linha a linha do clube na classificação: jogos, vitórias, empates, derrotas e gols
	 */
	/**
	 * @param treinador nome do usuário ou do treinador da máquina
	 * @param forcaEmCampo média dos 11 mais fortes
	 * @param ultimos resultados dos últimos jogos (V, E, D), do mais antigo ao mais recente
	 */
	public record VisaoCalendario(Clube clube, int temporada, String divisao, int posicao, LinhaClassificacao linha,
			List<JogoNoCalendario> jogos, String treinador, double forcaEmCampo, int moral, int moralMaxima,
			Moral.Faixa faixa, List<String> ultimos) {

		/** Jogos já disputados, da liga e da copa. */
		public long jogados() {
			return jogos.stream().filter(j -> j.golsPro() != null).count();
		}

		/** Jogos da copa já disputados. */
		public long jogadosNaCopa() {
			return jogos.stream().filter(j -> j.golsPro() != null && j.copa() != null).count();
		}

		/** Quantos jogos terminaram com o resultado (V, E ou D), liga e copa. */
		public long contar(String resultado) {
			return jogos.stream().filter(j -> resultado.equals(j.resultado())).count();
		}

		public int golsPro() {
			return jogos.stream().filter(j -> j.golsPro() != null).mapToInt(JogoNoCalendario::golsPro).sum();
		}

		public int golsContra() {
			return jogos.stream().filter(j -> j.golsPro() != null).mapToInt(JogoNoCalendario::golsContra).sum();
		}
	}

	/**
	 * @param doUsuario o jogador é do clube do treinador: a ficha oferece a venda por leilão
	 * @param preco preço da compra direta pela tabela, em centavos de Real (referência para o lance mínimo)
	 */
	public record Ficha(Jogador jogador, String clube, int golsTemporada, int golsCarreira, int amarelos,
			boolean doUsuario, Moeda moeda, long preco) {
	}
}
