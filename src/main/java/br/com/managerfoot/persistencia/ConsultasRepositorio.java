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
import br.com.managerfoot.temporada.Continental;
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
	private final ContinentalRepositorio continentais;
	private final ParametrosMoral parametrosMoral = ParametrosMoral.padrao();
	private final Premiacao premiacao = Premiacao.padrao();

	public ConsultasRepositorio(JdbcClient jdbc, JogoRepositorio jogos, RodadaRepositorio rodadas,
			TemporadaRepositorio temporadas, CopaRepositorio copas, ContinentalRepositorio continentais) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.rodadas = rodadas;
		this.temporadas = temporadas;
		this.copas = copas;
		this.continentais = continentais;
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
		int jogosPorClube = temporadas.carregarAtual(jogoId).divisoes().stream()
			.filter(d -> d.prioridade() == divisao.prioridade())
			.mapToInt(d -> d.calendario().jogos().size() * 2 / divisao.clubes().size())
			.findFirst().orElse(Integer.MAX_VALUE);
		// só a 1ª divisão dá vaga, e o jogo salvo antes da continental não tem vagas gravadas
		int vagas = divisao.prioridade() != 1 ? 0 : jdbc.sql("""
				SELECT coalesce((SELECT v.vagas FROM jogo_continental_vaga v JOIN jogo j ON j.id = v.jogo_id
				                 WHERE j.id = :jogo AND v.pais_codigo = j.pais_codigo), 0)""")
			.param("jogo", jogoId).query(Integer.class).single();
		return new VisaoClassificacao(abas(liga), divisao.prioridade(), divisao.nome(), rodadas.rodadasProcessadas(jogoId),
				classificacao.linhas(), divisao.rebaixados(), clube, vagas, classificacao.campeaoDefinido(jogosPorClube));
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
		String nacionalidade = jdbc.sql("SELECT nome FROM pais_fifa WHERE codigo = :codigo").param("codigo", jogador.nacionalidade())
			.query(String.class).single();
		// o contrato vale até o fim da temporada em que foi firmado (SPEC §9.5)
		Integer contratoAte = jdbc.sql("""
				SELECT g.ano_inicial + j.contrato_temporada - 1 FROM jogo_jogador j, jogo g
				WHERE j.id = :jogador AND g.id = :jogo
				  AND j.contrato_temporada = (SELECT max(t.numero) FROM jogo_temporada t WHERE t.jogo_id = :jogo)""")
			.param("jogador", jogadorId).param("jogo", jogoId).query(Integer.class).optional().orElse(null);
		return Optional.of(new Ficha(jogador, nomeClube, gols(jogoId, true).getOrDefault(jogadorId, 0),
				gols(jogoId, false).getOrDefault(jogadorId, 0), amarelos, clube.get() == doUsuario, Moeda.doPais(pais),
				PrecoJogador.padrao().preco(jogador, pais), nacionalidade, contratoAte));
	}

	/** Clube do jogo fora das divisões (modelo 14e, rev. 84); vazio se o clube não é deste jogo ou joga a liga. */
	@Transactional(readOnly = true)
	public Optional<ClubeDeFora> clubeDeFora(long jogoId, long usuarioId, long clubeId) {
		exigirClube(jogoId, usuarioId);
		return jdbc.sql("""
				SELECT c.id, c.nome, c.forca, c.escudo_modelo, c.escudo_cor_1, c.escudo_cor_2, c.pais_codigo, p.nome AS pais
				FROM jogo_clube c LEFT JOIN pais_fifa p ON p.codigo = c.pais_codigo
				WHERE c.id = :clube AND c.jogo_id = :jogo AND c.divisao_id IS NULL""")
			.param("clube", clubeId)
			.param("jogo", jogoId)
			.query((rs, n) -> new ClubeDeFora(TabelasLiga.clube(rs), rs.getString("pais_codigo"), rs.getString("pais")))
			.optional();
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
				WHERE t.jogo_id = :jogo AND t.numero = :numero AND p.gols_mandante IS NOT NULL AND p.divisao_id IS NOT NULL
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
		List<JogoNoCalendario> daContinental = jogosDaContinental(jogoId, temporadaId, clubeId, proximo);
		List<JogoNoCalendario> jogosDoClube = new ArrayList<>();
		for (Rodada rodada : divisao.get().calendario().rodadas()) {
			Optional<Jogo> jogo = rodada.jogos().stream()
				.filter(j -> j.mandante().id() == clubeId || j.visitante().id() == clubeId).findFirst();
			int[] placar = placares.get(rodada.numero());
			jogosDoClube.add(new JogoNoCalendario(rodada.numero(), jogo.map(j -> j.mandante().id() == clubeId).orElse(null),
					jogo.map(j -> j.mandante().id() == clubeId ? j.visitante() : j.mandante()).orElse(null),
					placar == null ? null : placar[0], placar == null ? null : placar[1],
					proximo instanceof RodadaRepositorio.Proximo.Liga(int r) && r == rodada.numero(), null, null, null));
			// os jogos da copa e da continental entram depois da rodada da liga em que são jogados (modelos 12d e 13d)
			daCopa.stream().filter(c -> c.rodada() == rodada.numero()).forEach(jogosDoClube::add);
			daContinental.stream().filter(c -> c.rodada() == rodada.numero()).forEach(jogosDoClube::add);
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

		List<FaseDaChave> chave = chave(copa, mostrada, clubeId);

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

	/** A chave das fases depois da mostrada (chave fixa, rev. 70): o vencedor do 1 pega o do 2. */
	static List<FaseDaChave> chave(Copa copa, FaseCopa mostrada, long clubeId) {
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
		return chave;
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

	static ConfrontoNaCopa confrontoNaCopa(ConfrontoCopa c, List<Copa.Etapa> quadro, long clubeId) {
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
					"Copa · " + j.etapa().nome(), j.detalhe(), null))
			.toList();
	}

	/** Os jogos da continental do clube, como {@link #jogosDaCopa} (modelo 13d, rev. 79). */
	private List<JogoNoCalendario> jogosDaContinental(long jogoId, long temporadaId, long clubeId,
			RodadaRepositorio.Proximo proximo) {
		Optional<Continental> continental = continentais.carregar(jogoId, temporadaId);
		if (continental.isEmpty()) {
			return List.of();
		}
		Optional<Clube> clube = continental.get().grupos().stream().flatMap(g -> g.clubes().stream())
			.filter(c -> c.id() == clubeId).findFirst();
		if (clube.isEmpty()) {
			return List.of();
		}
		Map<Long, String> deFora = continentais.paisesDeFora(jogoId);
		return continental.get().jogosDo(clube.get(), continentais.quadro(temporadaId)).stream()
			.map(j -> new JogoNoCalendario(j.etapa().depoisDaRodada(), j.emCasa(), j.adversario(), j.golsPro(),
					j.golsContra(), proximo instanceof RodadaRepositorio.Proximo.EtapaContinental e && e.indice() == j.indice(),
					PREFIXO_CONTINENTAL + j.etapa().nome(), j.detalhe(), deFora.get(j.adversario().id())))
			.toList();
	}

	private static final String PREFIXO_CONTINENTAL = "Continental · ";

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
	 * Histórico em quadros (SPEC §8.1.1, rev. 77 e 79; modelo 13a): a liga da 1ª divisão com campeão, vice
	 * e 3º, a copa e a continental com campeão e vice, e todos os títulos dos treinadores do país, em
	 * que empatados dividem a posição. A 2ª divisão não aparece nem conta.
	 */
	@Transactional(readOnly = true)
	public VisaoHistorico historico(long jogoId, long usuarioId) {
		exigirClube(jogoId, usuarioId);
		List<LinhaDaLiga> liga = jdbc.sql("""
				SELECT c.ano, c.clube_nome, c.usuario_id, coalesce(u.nome, m.nome) AS treinador, c.vice_nome, c.vice_treinador,
				       c.vice_usuario_id, c.terceiro_nome, c.terceiro_treinador, c.terceiro_usuario_id
				FROM jogo_campeao c
				LEFT JOIN usuario u ON u.id = c.usuario_id
				LEFT JOIN jogo_treinador_maquina m ON m.id = c.treinador_maquina_id
				WHERE c.jogo_id = :jogo AND c.divisao_prioridade = 1 ORDER BY c.ano DESC""")
			.param("jogo", jogoId)
			.query((rs, n) -> new LinhaDaLiga(rs.getInt("ano"),
					noQuadro(rs.getString("clube_nome"), rs.getString("treinador"), (Long) rs.getObject("usuario_id"), usuarioId, null, null),
					noQuadro(rs.getString("vice_nome"), rs.getString("vice_treinador"), (Long) rs.getObject("vice_usuario_id"), usuarioId, null, null),
					noQuadro(rs.getString("terceiro_nome"), rs.getString("terceiro_treinador"), (Long) rs.getObject("terceiro_usuario_id"), usuarioId, null, null)))
			.list();
		List<LinhaDaDecisao> copa = decisoes(jogoId, usuarioId, "jogo_campeao_copa", "COPA", "NULL", "NULL");
		List<LinhaDaDecisao> continental = decisoes(jogoId, usuarioId, "jogo_campeao_continental", "CONTINENTAL", "c.clube_pais",
				"c.vice_pais");
		String nomeContinental = jdbc.sql("SELECT nome FROM jogo_continental WHERE jogo_id = :jogo").param("jogo", jogoId)
			.query(String.class).optional().orElse(null);
		Map<String, Long> ids = new HashMap<>();
		jdbc.sql("SELECT id, nome FROM jogo_clube WHERE jogo_id = :jogo").param("jogo", jogoId)
			.query(rs -> {
				ids.put(rs.getString("nome"), rs.getLong("id"));
			});
		liga = liga.stream().map(l -> new LinhaDaLiga(l.ano(), comId(l.campeao(), ids), comId(l.vice(), ids), comId(l.terceiro(), ids))).toList();
		copa = comIds(copa, ids);
		continental = comIds(continental, ids);
		return new VisaoHistorico(jogos.carregarLiga(jogoId).divisoes().getFirst().nome(), liga, copa, nomeContinental, continental,
				titulos(jogoId, usuarioId));
	}

	private static NoQuadro comId(NoQuadro no, Map<String, Long> ids) {
		return no == null ? null : no.comId(ids);
	}

	private static List<LinhaDaDecisao> comIds(List<LinhaDaDecisao> linhas, Map<String, Long> ids) {
		return linhas.stream().map(l -> new LinhaDaDecisao(l.ano(), comId(l.campeao(), ids), comId(l.vice(), ids))).toList();
	}

	private static NoQuadro noQuadro(String clube, String treinador, Long usuario, long usuarioId, String pais, String detalhe) {
		return clube == null ? null : new NoQuadro(clube, treinador, usuario != null && usuario == usuarioId, pais, detalhe, null);
	}

	/**
	 * Campeão e vice de cada ano numa competição de final única, com o placar do ponto de vista do
	 * campeão. {@code tabela} e as colunas de país são constantes de quem chama, nunca entrada de usuário.
	 */
	private List<LinhaDaDecisao> decisoes(long jogoId, long usuarioId, String tabela, String competicao, String paisCampeao,
			String paisVice) {
		return jdbc.sql("""
				SELECT c.ano, c.clube_id, c.clube_nome, c.usuario_id, coalesce(u.nome, m.nome) AS treinador, c.vice_nome,
				       c.vice_treinador, c.vice_usuario_id, %s AS clube_pais, %s AS vice_pais,
				       p.mandante_id, p.gols_mandante, p.gols_visitante, p.penaltis_mandante, p.penaltis_visitante
				FROM %s c
				LEFT JOIN usuario u ON u.id = c.usuario_id
				LEFT JOIN jogo_treinador_maquina m ON m.id = c.treinador_maquina_id
				LEFT JOIN jogo_temporada t ON t.jogo_id = c.jogo_id AND t.numero = c.temporada
				LEFT JOIN jogo_copa_confronto k ON k.temporada_id = t.id AND k.competicao = :competicao AND k.fase = 'FINAL'
				LEFT JOIN jogo_partida p ON p.confronto_id = k.id
				WHERE c.jogo_id = :jogo ORDER BY c.ano DESC""".formatted(paisCampeao, paisVice, tabela))
			.param("competicao", competicao)
			.param("jogo", jogoId)
			.query((rs, n) -> {
				String fin = null;
				if (rs.getObject("gols_mandante") != null) {
					boolean emCasa = rs.getLong("mandante_id") == rs.getLong("clube_id");
					Integer penMandante = (Integer) rs.getObject("penaltis_mandante");
					String penaltis = penMandante == null ? ""
							: " · pên. " + rs.getInt(emCasa ? "penaltis_mandante" : "penaltis_visitante") + " x "
									+ rs.getInt(emCasa ? "penaltis_visitante" : "penaltis_mandante");
					fin = "final " + rs.getInt(emCasa ? "gols_mandante" : "gols_visitante") + " x "
							+ rs.getInt(emCasa ? "gols_visitante" : "gols_mandante") + penaltis;
				}
				return new LinhaDaDecisao(rs.getInt("ano"),
						noQuadro(rs.getString("clube_nome"), rs.getString("treinador"), (Long) rs.getObject("usuario_id"), usuarioId,
								rs.getString("clube_pais"), fin),
						noQuadro(rs.getString("vice_nome"), rs.getString("vice_treinador"), (Long) rs.getObject("vice_usuario_id"), usuarioId,
								rs.getString("vice_pais"), null));
			})
			.list();
	}

	/** Títulos da 1ª divisão, da copa e da continental; quem ganhou sem treinador no jogo (clube de fora) não entra. */
	private List<TreinadorNoRanking> titulos(long jogoId, long usuarioId) {
		List<TreinadorNoRanking> titulos = new ArrayList<>();
		jdbc.sql("""
				WITH conquista AS (
				    SELECT usuario_id, treinador_maquina_id, 'LIGA' AS competicao FROM jogo_campeao
				    WHERE jogo_id = :jogo AND divisao_prioridade = 1
				    UNION ALL SELECT usuario_id, treinador_maquina_id, 'COPA' FROM jogo_campeao_copa WHERE jogo_id = :jogo
				    UNION ALL SELECT usuario_id, treinador_maquina_id, 'CONTINENTAL' FROM jogo_campeao_continental WHERE jogo_id = :jogo
				)
				SELECT c.usuario_id, coalesce(u.nome, m.nome) AS nome, coalesce(cu.nome, cm.nome) AS clube,
				       count(*) FILTER (WHERE c.competicao = 'LIGA') AS liga,
				       count(*) FILTER (WHERE c.competicao = 'COPA') AS copa,
				       count(*) FILTER (WHERE c.competicao = 'CONTINENTAL') AS continental
				FROM conquista c
				LEFT JOIN usuario u ON u.id = c.usuario_id
				LEFT JOIN jogo_treinador t ON t.jogo_id = :jogo AND t.usuario_id = c.usuario_id
				LEFT JOIN jogo_clube cu ON cu.id = t.clube_id
				LEFT JOIN jogo_treinador_maquina m ON m.id = c.treinador_maquina_id
				LEFT JOIN jogo_clube cm ON cm.id = m.clube_id
				WHERE c.usuario_id IS NOT NULL OR c.treinador_maquina_id IS NOT NULL
				GROUP BY c.usuario_id, c.treinador_maquina_id, u.nome, m.nome, cu.nome, cm.nome
				ORDER BY count(*) DESC, nome""")
			.param("jogo", jogoId)
			.query(rs -> {
				int liga = rs.getInt("liga");
				int copa = rs.getInt("copa");
				int continental = rs.getInt("continental");
				int total = liga + copa + continental;
				// empatados dividem a posição
				int posicao = titulos.isEmpty() || titulos.getLast().total() != total ? titulos.size() + 1
						: titulos.getLast().posicao();
				Long usuario = (Long) rs.getObject("usuario_id");
				titulos.add(new TreinadorNoRanking(posicao, rs.getString("nome"), rs.getString("clube"), liga, copa, continental,
						usuario != null && usuario == usuarioId));
			});
		return titulos;
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
	 * Os quadros do Histórico, com o ano mais recente primeiro (modelo 13a, rev. 79).
	 *
	 * @param divisao nome da 1ª divisão
	 * @param nomeContinental nome da competição continental; nulo se o jogo não tem
	 * @param titulos todos os títulos dos treinadores do país
	 */
	public record VisaoHistorico(String divisao, List<LinhaDaLiga> liga, List<LinhaDaDecisao> copa, String nomeContinental,
			List<LinhaDaDecisao> continental, List<TreinadorNoRanking> titulos) {
	}

	/** @param vice nulo nas temporadas encerradas antes da rev. 78, como {@code terceiro} */
	public record LinhaDaLiga(int ano, NoQuadro campeao, NoQuadro vice, NoQuadro terceiro) {
	}

	/** Campeão e vice de uma competição com final. */
	public record LinhaDaDecisao(int ano, NoQuadro campeao, NoQuadro vice) {
	}

	/**
	 * Um clube num quadro do Histórico.
	 *
	 * @param treinador nulo no clube de fora e no vice de temporada antiga
	 * @param seu o treinador era o usuário que vê a tela
	 * @param pais sigla do clube de fora; nulo no clube do país jogado
	 * @param detalhe no campeão, o placar da final ("final 1 x 1 · pên. 4 x 3"); nulo nos outros
	 * @param clubeId para abrir a janela do clube; o histórico guarda só o nome e o id vem do clube com esse nome
	 */
	public record NoQuadro(String clube, String treinador, boolean seu, String pais, String detalhe, Long clubeId) {

		NoQuadro comId(Map<String, Long> clubes) {
			return new NoQuadro(clube, treinador, seu, pais, detalhe, clubes.get(clube));
		}
	}

	/** @param clube clube atual do treinador; nulo se ele está sem clube */
	public record TreinadorNoRanking(int posicao, String nome, String clube, int liga, int copa, int continental, boolean voce) {

		public int total() {
			return liga + copa + continental;
		}
	}

	/**
	 * @param rodadas rodadas já jogadas na temporada
	 * @param rebaixados quantos caem no fim, marcados na tabela
	 * @param vagasContinental quantos vão à continental (modelo 14c); 0 fora da 1ª divisão
	 * @param campeaoDefinido o líder já é campeão matematicamente
	 */
	public record VisaoClassificacao(List<AbaDivisao> divisoes, int prioridade, String divisao, int rodadas,
			List<LinhaClassificacao> linhas, int rebaixados, long clubeDoUsuario, int vagasContinental, boolean campeaoDefinido) {
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
	 * @param copa nome do jogo da copa ou da continental ("Copa · Oitavas de final · ida"); nulo na liga
	 * @param detalhe no mata-mata, o total ou a ida embaixo do placar ("total 3 x 2 · passa"); pode ser nulo
	 * @param paisAdversario sigla do adversário de fora, na continental; nulo nos outros casos
	 */
	public record JogoNoCalendario(int rodada, Boolean emCasa, Clube adversario, Integer golsPro, Integer golsContra,
			boolean proximo, String copa, String detalhe, String paisAdversario) {

		/** É jogo da continental, em azul-claro na tela (modelo 13d, rev. 79). */
		public boolean continental() {
			return copa != null && copa.startsWith(PREFIXO_CONTINENTAL);
		}

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
			return jogos.stream().filter(j -> j.golsPro() != null && j.copa() != null && !j.continental()).count();
		}

		/** Jogos da continental já disputados. */
		public long jogadosNaContinental() {
			return jogos.stream().filter(j -> j.golsPro() != null && j.continental()).count();
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
	 * Um clube sem jogos na liga: de fora do campeonato ou na fila de espera.
	 *
	 * @param sigla código do país do clube de fora; nula na fila de espera
	 * @param pais nome do país do clube de fora; nulo na fila de espera
	 */
	public record ClubeDeFora(Clube clube, String sigla, String pais) {
	}

	/**
	 * @param doUsuario o jogador é do clube do treinador: a ficha oferece a venda por leilão
	 * @param preco preço da compra direta pela tabela, em centavos de Real (referência para o lance mínimo)
	 * @param nacionalidade nome do país do jogador
	 * @param contratoAte ano até o fim do qual vale o contrato firmado; nulo se não há contrato nesta temporada
	 */
	public record Ficha(Jogador jogador, String clube, int golsTemporada, int golsCarreira, int amarelos,
			boolean doUsuario, Moeda moeda, long preco, String nacionalidade, Integer contratoAte) {
	}
}
