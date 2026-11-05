package br.com.managerfoot.persistencia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;

/**
 * Balanceamento por simulação de temporadas completas (SPEC §15, Etapa 6). Ferramenta de desenvolvimento:
 * joga o jogo inteiro pelo caminho do servidor (leilões da máquina antes de cada rodada, liga, copa,
 * continental, Mundial, caixa, evolução de força, trocas de treinador e virada de temporada), só com clubes da
 * máquina, e grava {@code docs/simulacoes/jogo-completo.md}.
 *
 * <p>Só roda com {@code RELATORIO_JOGO=1}. Opcionais: {@code RELATORIO_PAISES} (códigos separados por
 * vírgula), {@code RELATORIO_SEMENTES} (padrão 2), {@code RELATORIO_TEMPORADAS} (padrão 5) e {@code RELATORIO_SAIDA}
 * (arquivo do relatório; padrão {@code docs/simulacoes/jogo-completo.md}). Exemplo:
 * {@code RELATORIO_JOGO=1 ./mvnw verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=RelatorioJogoCompletoIT}
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RELATORIO_JOGO", matches = ".+")
class RelatorioJogoCompletoIT {

	@Autowired
	BancoPadraoRepositorio bancoPadrao;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	LeilaoRepositorio leiloes;

	@Autowired
	FimDeTemporadaRepositorio fins;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	CaixaRepositorio caixas;

	@Autowired
	TransactionTemplate transacao;

	/** O que se mede de uma temporada de um jogo. */
	record Medida(String pais, int semente, int temporada, int partidas, int gols, int vitoriasMandante, int empates,
			int goleadas, int expulsoes, int lesoes, Map<Integer, double[]> campeaoLanterna, boolean campeaoEntreOs3,
			Long campeao1, Integer copaDivisao, Boolean continentalNacional, Boolean mundialNacional, int vendas, int semLance,
			int trocas, Map<Integer, double[]> forcaDivisao, Map<Integer, double[]> saldoDivisao, Map<Integer, Double> estadio, double[] promovidos, Map<String, Integer> golsPorPosicao, int artilheiro,
			Map<Integer, Map<String, Double>> fluxo, long milissegundos) {
	}

	@Test
	void gerarRelatorio() throws IOException {
		int sementes = inteiro("RELATORIO_SEMENTES", 2);
		int quantasTemporadas = inteiro("RELATORIO_TEMPORADAS", 5);
		List<String> paises = Optional.ofNullable(System.getenv("RELATORIO_PAISES")).filter(s -> !s.isBlank())
			.map(s -> Arrays.asList(s.split(","))).orElse(List.of());
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());
		List<CampeonatoDisponivel> campeonatos = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> paises.isEmpty() || paises.contains(c.codigoPais()))
			.sorted(Comparator.comparing(CampeonatoDisponivel::codigoPais))
			.toList();
		List<Medida> medidas = new ArrayList<>();
		for (CampeonatoDisponivel c : campeonatos) {
			for (int s = 0; s < sementes; s++) {
				medidas.addAll(jogar(c, s, quantasTemporadas));
			}
		}
		Path arquivo = Path.of(Optional.ofNullable(System.getenv("RELATORIO_SAIDA")).filter(x -> !x.isBlank())
			.orElse("docs/simulacoes/jogo-completo.md"));
		Files.writeString(arquivo, relatorio(medidas, sementes, quantasTemporadas));
		System.out.println("Relatório gravado em " + arquivo.toAbsolutePath());
	}

	private List<Medida> jogar(CampeonatoDisponivel campeonato, int semente, int quantasTemporadas) {
		long usuario = LigaDeTeste.participante(contas).id();
		long jogo = jogos.criarJogoComClubeSorteado(campeonato.paisId(), usuario, semente);
		// só a máquina: o clube sorteado volta para ela antes da primeira temporada
		jdbc.sql("DELETE FROM jogo_treinador WHERE jogo_id = :j").param("j", jogo).update();
		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		temporadas.iniciarPrimeiraTemporada(jogo, 1_000L + semente);
		// como o jogo individual (JogosDoTreinadorRepositorio.iniciar): patrocínio da 1ª temporada, aqui todo da máquina
		transacao.executeWithoutResult(st -> caixas.pagarPatrocinio(jogo, Map.of()));
		List<Medida> medidas = new ArrayList<>();
		Long campeaoAnterior = null;
		Map<Long, Integer> divisaoAnterior = Map.of();
		for (int t = 1; t <= quantasTemporadas; t++) {
			long inicio = System.currentTimeMillis();
			Map<Long, Double> forcaInicio = forcaEmCampo(jogo);
			Map<Long, Integer> divisaoInicio = divisoes(jogo);
			jogarTemporada(jogo);
			Medida m = medir(campeonato.codigoPais(), semente, t, jogo, forcaInicio, divisaoInicio, divisaoAnterior,
					campeaoAnterior, System.currentTimeMillis() - inicio);
			divisaoAnterior = divisaoInicio;
			medidas.add(m);
			System.out.printf(Locale.ROOT, "%s semente %d temporada %d: %d ms%n", campeonato.codigoPais(), semente, t,
					m.milissegundos());
			campeaoAnterior = m.campeao1();
			if (t < quantasTemporadas) {
				fins.avancarTemporada(jogo, t, 5_000L + 100L * semente + t);
			}
		}
		return medidas;
	}

	private void jogarTemporada(long jogo) {
		while (true) {
			switch (rodadas.proximo(jogo)) {
				case RodadaRepositorio.Proximo.Liga(int r) -> {
					// os leilões da máquina antes da rodada; sem treinador, saem resolvidos
					leiloes.abrirEmRede(jogo, r);
					rodadas.avancar(jogo, r);
				}
				case RodadaRepositorio.Proximo.EtapaCopa e -> rodadas.avancarCopa(jogo, e.indice(), List.of());
				case RodadaRepositorio.Proximo.EtapaContinental e -> rodadas.avancarContinental(jogo, e.indice(), List.of());
				case RodadaRepositorio.Proximo.EtapaMundial e -> rodadas.avancarMundial(jogo, e.indice(), List.of());
				case RodadaRepositorio.Proximo.Fim f -> {
					return;
				}
			}
		}
	}

	private Medida medir(String pais, int semente, int numero, long jogo, Map<Long, Double> forcaInicio,
			Map<Long, Integer> divisaoInicio, Map<Long, Integer> divisaoAnterior, Long campeaoAnterior, long ms) {
		long temporada = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :j AND numero = :n").param("j", jogo)
			.param("n", numero).query(Long.class).single();
		// partidas da liga: pontos, gols e mando
		Map<Integer, Map<Long, int[]>> tabela = new HashMap<>();
		int[] contagem = new int[5];
		jdbc.sql("""
				SELECT d.prioridade, p.mandante_id, p.visitante_id, p.gols_mandante, p.gols_visitante FROM jogo_partida p
				JOIN jogo_divisao d ON d.id = p.divisao_id WHERE p.temporada_id = :t AND p.gols_mandante IS NOT NULL""")
			.param("t", temporada)
			.query(rs -> {
				int gm = rs.getInt("gols_mandante");
				int gv = rs.getInt("gols_visitante");
				Map<Long, int[]> div = tabela.computeIfAbsent(rs.getInt("prioridade"), k -> new HashMap<>());
				int[] casa = div.computeIfAbsent(rs.getLong("mandante_id"), k -> new int[3]);
				int[] fora = div.computeIfAbsent(rs.getLong("visitante_id"), k -> new int[3]);
				casa[1] += gm - gv;
				fora[1] += gv - gm;
				casa[2] += gm;
				fora[2] += gv;
				casa[0] += gm > gv ? 3 : gm == gv ? 1 : 0;
				fora[0] += gv > gm ? 3 : gm == gv ? 1 : 0;
				contagem[0]++;
				contagem[1] += gm + gv;
				contagem[2] += gm > gv ? 1 : 0;
				contagem[3] += gm == gv ? 1 : 0;
				contagem[4] += gm >= 5 || gv >= 5 ? 1 : 0;
			});
		Map<String, Integer> eventos = new HashMap<>();
		jdbc.sql("""
				SELECT e.tipo, count(*) AS n FROM jogo_evento e JOIN jogo_partida p ON p.id = e.partida_id
				WHERE p.temporada_id = :t AND p.divisao_id IS NOT NULL GROUP BY e.tipo""")
			.param("t", temporada)
			.query(rs -> {
				eventos.put(rs.getString("tipo"), rs.getInt("n"));
			});
		// gols de jogada da liga pela posição do autor (sem pênalti) e os gols do artilheiro da 1ª
		Map<String, Integer> golsPorPosicao = new HashMap<>();
		jdbc.sql("""
				SELECT CASE WHEN e.penalti THEN 'PENALTI' ELSE j.posicao END AS chave, count(*) AS n
				FROM jogo_evento e JOIN jogo_partida p ON p.id = e.partida_id JOIN jogo_jogador j ON j.id = e.jogador_id
				WHERE p.temporada_id = :t AND p.divisao_id IS NOT NULL AND e.tipo = 'GOL' GROUP BY 1""")
			.param("t", temporada)
			.query(rs -> {
				golsPorPosicao.put(rs.getString("chave"), rs.getInt("n"));
			});
		int artilheiro = jdbc.sql("""
				SELECT coalesce(max(n), 0) FROM (SELECT count(*) AS n FROM jogo_evento e JOIN jogo_partida p ON p.id = e.partida_id
				  JOIN jogo_divisao d ON d.id = p.divisao_id WHERE p.temporada_id = :t AND d.prioridade = 1 AND e.tipo = 'GOL'
				  GROUP BY e.jogador_id) x""")
			.param("t", temporada).query(Integer.class).single();
		Map<Integer, double[]> campeaoLanterna = new HashMap<>();
		// quem subiu da 2ª no fim da temporada anterior: quantos, soma das posições na 1ª, quantos terminaram entre os
		// últimos (tantos quantos subiram), soma da força em campo menos a média da 1ª
		double[] promovidos = new double[4];
		Long campeao1 = null;
		boolean entreOs3 = false;
		for (var e : tabela.entrySet()) {
			List<Map.Entry<Long, int[]>> ordem = new ArrayList<>(e.getValue().entrySet());
			ordem.sort(Comparator.<Map.Entry<Long, int[]>>comparingInt(x -> -x.getValue()[0])
				.thenComparingInt(x -> -x.getValue()[1]).thenComparingInt(x -> -x.getValue()[2]));
			int jogos = 2 * (ordem.size() - 1);
			campeaoLanterna.put(e.getKey(), new double[] { ordem.getFirst().getValue()[0], ordem.getLast().getValue()[0],
					3.0 * jogos });
			if (e.getKey() == 1) {
				List<Long> subiram = ordem.stream().map(Map.Entry::getKey)
					.filter(id -> Integer.valueOf(2).equals(divisaoAnterior.get(id))).toList();
				double mediaDaPrimeira = ordem.stream().mapToDouble(x -> forcaInicio.getOrDefault(x.getKey(), 0.0)).average()
					.orElse(0);
				for (Long id : subiram) {
					int posicao = 1 + ordem.stream().map(Map.Entry::getKey).toList().indexOf(id);
					promovidos[0]++;
					promovidos[1] += posicao;
					promovidos[2] += posicao > ordem.size() - subiram.size() ? 1 : 0;
					promovidos[3] += forcaInicio.getOrDefault(id, 0.0) - mediaDaPrimeira;
				}
				campeao1 = ordem.getFirst().getKey();
				List<Long> fortes = e.getValue().keySet().stream()
					.sorted(Comparator.comparingDouble((Long id) -> -forcaInicio.getOrDefault(id, 0.0))).limit(3).toList();
				entreOs3 = fortes.contains(campeao1);
			}
		}
		Integer copaDivisao = jdbc.sql("SELECT clube_id FROM jogo_campeao_copa WHERE jogo_id = :j AND temporada = :n")
			.param("j", jogo).param("n", numero).query(Long.class).optional().map(divisaoInicio::get).orElse(null);
		Boolean continental = nacional(jogo, "jogo_campeao_continental", numero);
		Boolean mundial = nacional(jogo, "jogo_campeao_mundial", numero);
		Map<String, Integer> leilao = new HashMap<>();
		jdbc.sql("SELECT situacao, count(*) AS n FROM jogo_leilao WHERE temporada_id = :t GROUP BY situacao")
			.param("t", temporada)
			.query(rs -> {
				leilao.put(rs.getString("situacao"), rs.getInt("n"));
			});
		int trocas = jdbc.sql("SELECT count(*) FROM jogo_troca_treinador WHERE temporada_id = :t AND situacao = 'TROCA'")
			.param("t", temporada).query(Integer.class).single();
		Map<Integer, double[]> forcaDivisao = new HashMap<>();
		divisaoInicio.forEach((clube, div) -> {
			double f = forcaInicio.getOrDefault(clube, 0.0);
			double[] a = forcaDivisao.computeIfAbsent(div, k -> new double[] { 0, 0, Double.MAX_VALUE, 0 });
			a[0] += f;
			a[1]++;
			a[2] = Math.min(a[2], f);
			a[3] = Math.max(a[3], f);
		});
		// saldo no fim da temporada em rodadas da folha (a folha é a da última rodada da liga)
		Map<Integer, List<Double>> rodadasDeFolha = new HashMap<>();
		Map<Integer, double[]> estadio = new HashMap<>();
		jdbc.sql("""
				SELECT d.prioridade, c.saldo, c.estadio_capacidade,
				       (SELECT -l.valor FROM jogo_lancamento l WHERE l.clube_id = c.id AND l.temporada_id = :t
				          AND l.tipo = 'SALARIOS' ORDER BY l.rodada DESC LIMIT 1) AS folha
				FROM jogo_clube c JOIN jogo_divisao d ON d.id = c.divisao_id WHERE c.jogo_id = :j""")
			.param("t", temporada).param("j", jogo)
			.query(rs -> {
				long folha = rs.getLong("folha");
				if (folha > 0) {
					rodadasDeFolha.computeIfAbsent(rs.getInt("prioridade"), k -> new ArrayList<>())
						.add(rs.getLong("saldo") / (double) folha);
				}
				double[] e = estadio.computeIfAbsent(rs.getInt("prioridade"), k -> new double[2]);
				e[0] += rs.getInt("estadio_capacidade");
				e[1]++;
			});
		Map<Integer, double[]> saldoDivisao = new HashMap<>();
		rodadasDeFolha.forEach((div, lista) -> {
			double[] ordenada = lista.stream().mapToDouble(Double::doubleValue).sorted().toArray();
			double negativos = lista.stream().filter(x -> x < 0).count() / (double) lista.size();
			saldoDivisao.put(div, new double[] { ordenada[ordenada.length / 2], negativos, ordenada[0],
					ordenada[ordenada.length - 1] });
		});
		Map<Integer, Double> estadioMedio = new HashMap<>();
		estadio.forEach((div, e) -> estadioMedio.put(div, e[0] / e[1]));
		// o que entrou e saiu na temporada, por tipo, em rodadas de folha (média dos clubes da divisão no fim)
		Map<Integer, Map<String, Double>> fluxo = new HashMap<>();
		Map<Integer, Integer> clubesPorDivisao = new HashMap<>();
		jdbc.sql("""
				SELECT d.prioridade, l.tipo, sum(l.valor)::float8 / nullif(max(f.folha), 0) AS rodadas
				FROM jogo_lancamento l JOIN jogo_clube c ON c.id = l.clube_id JOIN jogo_divisao d ON d.id = c.divisao_id
				JOIN LATERAL (SELECT -x.valor AS folha FROM jogo_lancamento x WHERE x.clube_id = c.id AND x.temporada_id = :t
				              AND x.tipo = 'SALARIOS' ORDER BY x.rodada DESC LIMIT 1) f ON true
				WHERE l.temporada_id = :t GROUP BY d.prioridade, l.tipo, c.id""")
			.param("t", temporada)
			.query(rs -> {
				double r = rs.getDouble("rodadas");
				if (!rs.wasNull()) {
					fluxo.computeIfAbsent(rs.getInt("prioridade"), k -> new HashMap<>()).merge(rs.getString("tipo"), r, Double::sum);
				}
			});
		divisaoFim(jogo).values().forEach(div -> clubesPorDivisao.merge(div, 1, Integer::sum));
		fluxo.forEach((div, tipos) -> tipos.replaceAll((tipo, total) -> total / clubesPorDivisao.getOrDefault(div, 1)));
		return new Medida(pais, semente, numero, contagem[0], contagem[1], contagem[2], contagem[3], contagem[4],
				eventos.getOrDefault("CARTAO_VERMELHO", 0) + eventos.getOrDefault("SEGUNDO_AMARELO", 0),
				eventos.getOrDefault("LESAO", 0), campeaoLanterna, entreOs3, campeao1, copaDivisao, continental, mundial,
				leilao.getOrDefault("VENDIDO", 0), leilao.getOrDefault("SEM_LANCE", 0), trocas, forcaDivisao, saldoDivisao,
				estadioMedio, promovidos, golsPorPosicao, artilheiro, fluxo, ms);
	}

	private static String linhaPromovidos(String rotulo, double[] p) {
		if (p[0] == 0) {
			return "| " + rotulo + " | 0 | – | – | – |\n";
		}
		return "| " + rotulo + " | " + (int) p[0] + " | " + num(p[1] / p[0]) + "ª | " + pct(p[2] / p[0]) + " | "
				+ String.format(Locale.forLanguageTag("pt-BR"), "%+.1f", p[3] / p[0]) + " |\n";
	}

	/** O campeão da competição é um clube do país do jogo; nulo se ela não terminou na temporada. */
	private Boolean nacional(long jogo, String tabela, int numero) {
		return jdbc.sql("SELECT c.divisao_id IS NOT NULL FROM " + tabela
				+ " x JOIN jogo_clube c ON c.id = x.clube_id WHERE x.jogo_id = :j AND x.temporada = :n")
			.param("j", jogo).param("n", numero).query(Boolean.class).optional().orElse(null);
	}

	/** Força em campo de cada clube nas divisões: a média dos 11 mais fortes do plantel. */
	private Map<Long, Double> forcaEmCampo(long jogo) {
		Map<Long, Double> forca = new HashMap<>();
		jdbc.sql("""
				SELECT clube_id, avg(forca) AS media FROM (
				  SELECT j.clube_id, j.forca, row_number() OVER (PARTITION BY j.clube_id ORDER BY j.forca DESC) AS n
				  FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id
				  WHERE c.jogo_id = :j AND c.divisao_id IS NOT NULL) x
				WHERE n <= 11 GROUP BY clube_id""")
			.param("j", jogo)
			.query(rs -> {
				forca.put(rs.getLong("clube_id"), rs.getDouble("media"));
			});
		return forca;
	}

	private Map<Long, Integer> divisaoFim(long jogo) {
		return divisoes(jogo);
	}

	private Map<Long, Integer> divisoes(long jogo) {
		Map<Long, Integer> div = new HashMap<>();
		jdbc.sql("SELECT c.id, d.prioridade FROM jogo_clube c JOIN jogo_divisao d ON d.id = c.divisao_id WHERE c.jogo_id = :j")
			.param("j", jogo)
			.query(rs -> {
				div.put(rs.getLong("id"), rs.getInt("prioridade"));
			});
		return div;
	}

	// ---------- relatório ----------

	private static String relatorio(List<Medida> medidas, int sementes, int temporadas) {
		StringBuilder md = new StringBuilder();
		md.append("# Simulação do jogo completo\n\n")
			.append("Gerado por `RelatorioJogoCompletoIT` (ferramenta de desenvolvimento; SPEC §15, Etapa 6). ")
			.append("Cada jogo é criado do banco padrão e jogado pelo caminho do servidor, só com clubes da máquina: leilões ")
			.append("da máquina antes de cada rodada, liga, copa, continental, Mundial, caixa, evolução de força, trocas de ")
			.append("treinador e virada de temporada com promoção e rebaixamento. ")
			.append(sementes).append(sementes == 1 ? " semente" : " sementes").append(" por país (0 a ").append(sementes - 1)
			.append("), ").append(temporadas).append(" temporadas seguidas cada; a semente da temporada é fixa, então o ")
			.append("relatório se refaz igual.\n\n")
			.append("*Força em campo*: média dos 11 mais fortes do plantel no início da temporada. *Rodadas de folha*: saldo ")
			.append("no fim da temporada dividido pelos salários da última rodada. *Goleada*: um time com 5 gols ou mais. ")
			.append("*Entre os 3 mais fortes*: o campeão da 1ª estava entre as 3 maiores forças em campo no início.\n\n");

		md.append("## Por país\n\n")
			.append("| País | Gols por jogo | Mandante vence | Empate | Visitante vence | Goleadas | Expulsões por jogo | ")
			.append("Lesões por jogo | Pontos do campeão (1ª) | Pontos do lanterna (1ª) | Campeão entre os 3 mais fortes | ")
			.append("Mesmo campeão do ano anterior | Copa: campeão da 1ª | Continental: campeão do país | Vendas por temporada | ")
			.append("Trocas de treinador por temporada |\n")
			.append("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|\n");
		Map<String, List<Medida>> porPais = new LinkedHashMap<>();
		medidas.forEach(m -> porPais.computeIfAbsent(m.pais(), k -> new ArrayList<>()).add(m));
		porPais.put("Todos", medidas);
		porPais.forEach((pais, ms) -> md.append(linhaPais(pais, ms)));

		md.append("\n## Por temporada (todos os países)\n\n")
			.append("| Temporada | Força em campo 1ª (média; menor a maior) | Força em campo 2ª | Saldo 1ª em rodadas de folha ")
			.append("(mediana) | Clubes negativos 1ª | Saldo 2ª (mediana) | Clubes negativos 2ª | Estádio médio 1ª / 2ª | ")
			.append("Gols por jogo | Vendas | Sem lance |\n")
			.append("|---|---|---|---|---|---|---|---|---|---|---|\n");
		for (int t = 1; t <= temporadas; t++) {
			int numero = t;
			List<Medida> ms = medidas.stream().filter(m -> m.temporada() == numero).toList();
			if (ms.isEmpty()) {
				continue;
			}
			md.append("| ").append(t).append(" | ").append(forca(ms, 1)).append(" | ").append(forca(ms, 2)).append(" | ")
				.append(num(media(ms, m -> saldo(m, 1, 0)))).append(" | ").append(pct(media(ms, m -> saldo(m, 1, 1))))
				.append(" | ").append(num(media(ms, m -> saldo(m, 2, 0)))).append(" | ")
				.append(pct(media(ms, m -> saldo(m, 2, 1)))).append(" | ")
				.append(String.format(Locale.ROOT, "%,.0f / %,.0f", media(ms, m -> m.estadio().getOrDefault(1, Double.NaN)),
						media(ms, m -> m.estadio().getOrDefault(2, Double.NaN))).replace(',', '.'))
				.append(" | ").append(num(ms.stream().mapToInt(Medida::gols).sum() / (double) ms.stream().mapToInt(Medida::partidas).sum()))
				.append(" | ").append(num(media(ms, Medida::vendas))).append(" | ").append(num(media(ms, Medida::semLance)))
				.append(" |\n");
		}
		md.append("\n## Quem marca (liga, todos os países)\n\n")
			.append("Gols da liga pela posição do autor; o pênalti fica à parte. *Artilheiro*: gols do maior artilheiro da 1ª ")
			.append("na liga, média das temporadas. A máquina escolhe a formação a cada jogo (§7.2 dá os pesos por formação).\n\n")
			.append("| País | Atacante | Meia | Defensor | Goleiro | Pênalti | Artilheiro da 1ª |\n|---|---|---|---|---|---|---|\n");
		porPais.forEach((pais, ms) -> {
			double total = ms.stream().mapToDouble(m -> m.golsPorPosicao().values().stream().mapToInt(Integer::intValue).sum()).sum();
			md.append("| ").append(pais);
			for (String chave : List.of("ATACANTE", "MEIO_CAMPISTA", "DEFENSOR", "GOLEIRO", "PENALTI")) {
				double n = ms.stream().mapToDouble(m -> m.golsPorPosicao().getOrDefault(chave, 0)).sum();
				md.append(" | ").append(chave.equals("GOLEIRO") ? String.valueOf((long) n) : pct(n / Math.max(1, total)));
			}
			md.append(" | ").append(num(media(ms, Medida::artilheiro))).append(" |\n");
		});
		md.append("\n## Quem sobe da 2ª (todos os países)\n\n")
			.append("Clubes que subiram no fim da temporada anterior, na 1ª. *Caem de volta*: terminam entre os últimos, tantos ")
			.append("quantos subiram (a zona de rebaixamento). *Força*: força em campo no início menos a média da 1ª.\n\n")
			.append("| Temporada | Promovidos | Posição média | Caem de volta | Força em relação à média da 1ª |\n")
			.append("|---|---|---|---|---|\n");
		double[] todos = new double[4];
		for (int t = 2; t <= temporadas; t++) {
			int numero = t;
			double[] p = new double[4];
			medidas.stream().filter(m -> m.temporada() == numero).forEach(m -> {
				for (int i = 0; i < 4; i++) {
					p[i] += m.promovidos()[i];
					todos[i] += m.promovidos()[i];
				}
			});
			md.append(linhaPromovidos(String.valueOf(t), p));
		}
		md.append(linhaPromovidos("Todas", todos));
		md.append("\n## Caixa por temporada, por tipo (todos os países)\n\n")
			.append("Média dos clubes da divisão no fim da temporada, em rodadas da folha do próprio clube (positivo entra, ")
			.append("negativo sai). Uma temporada de liga com 20 clubes tem 38 rodadas.\n\n");
		List<String> tipos = medidas.stream().flatMap(m -> m.fluxo().values().stream()).flatMap(x -> x.keySet().stream())
			.distinct().sorted().toList();
		md.append("| Temporada | Divisão | ").append(String.join(" | ", tipos)).append(" | Saldo da temporada |\n|---|---|")
			.append("---|".repeat(tipos.size() + 1)).append("\n");
		for (int t = 1; t <= temporadas; t++) {
			int numero = t;
			List<Medida> daTemporada = medidas.stream().filter(m -> m.temporada() == numero).toList();
			for (int div = 1; div <= 2; div++) {
				int d = div;
				md.append("| ").append(t).append(" | ").append(div).append("ª");
				double total = 0;
				for (String tipo : tipos) {
					double v = media(daTemporada, m -> m.fluxo().getOrDefault(d, Map.of()).getOrDefault(tipo, 0.0));
					total += Double.isNaN(v) ? 0 : v;
					md.append(" | ").append(num(v));
				}
				md.append(" | ").append(num(total)).append(" |\n");
			}
		}
		long ms = medidas.stream().mapToLong(Medida::milissegundos).sum();
		md.append("\nTempo de simulação: ").append(String.format(Locale.ROOT, "%.0f", ms / 1000.0)).append(" s para ")
			.append(medidas.size()).append(" temporadas.\n");
		return md.toString();
	}

	private static String linhaPais(String pais, List<Medida> ms) {
		int partidas = ms.stream().mapToInt(Medida::partidas).sum();
		List<Medida> comAnterior = ms.stream().filter(m -> m.temporada() > 1).toList();
		long repetidos = 0;
		for (Medida m : comAnterior) {
			Medida anterior = ms.stream().filter(x -> x.pais().equals(m.pais()) && x.semente() == m.semente()
					&& x.temporada() == m.temporada() - 1).findFirst().orElse(null);
			if (anterior != null && anterior.campeao1() != null && anterior.campeao1().equals(m.campeao1())) {
				repetidos++;
			}
		}
		List<Medida> comCopa = ms.stream().filter(m -> m.copaDivisao() != null).toList();
		List<Medida> comContinental = ms.stream().filter(m -> m.continentalNacional() != null).toList();
		return "| " + pais + " | " + num(ms.stream().mapToInt(Medida::gols).sum() / (double) partidas) + " | "
				+ pct(ms.stream().mapToInt(Medida::vitoriasMandante).sum() / (double) partidas) + " | "
				+ pct(ms.stream().mapToInt(Medida::empates).sum() / (double) partidas) + " | "
				+ pct(1 - (ms.stream().mapToInt(Medida::vitoriasMandante).sum() + ms.stream().mapToInt(Medida::empates).sum())
						/ (double) partidas)
				+ " | " + pct(ms.stream().mapToInt(Medida::goleadas).sum() / (double) partidas) + " | "
				+ num2(ms.stream().mapToInt(Medida::expulsoes).sum() / (double) partidas) + " | "
				+ num2(ms.stream().mapToInt(Medida::lesoes).sum() / (double) partidas) + " | "
				+ pontos(ms, 0) + " | " + pontos(ms, 1) + " | "
				+ pct(ms.stream().filter(Medida::campeaoEntreOs3).count() / (double) ms.size()) + " | "
				+ (comAnterior.isEmpty() ? "–" : pct(repetidos / (double) comAnterior.size())) + " | "
				+ (comCopa.isEmpty() ? "–" : pct(comCopa.stream().filter(m -> m.copaDivisao() == 1).count() / (double) comCopa.size()))
				+ " | "
				+ (comContinental.isEmpty() ? "–"
						: pct(comContinental.stream().filter(Medida::continentalNacional).count() / (double) comContinental.size()))
				+ " | " + num(media(ms, Medida::vendas)) + " | " + num(media(ms, Medida::trocas)) + " |\n";
	}

	/** Pontos médios do campeão (0) ou do lanterna (1) da 1ª, com a porcentagem do máximo. */
	private static String pontos(List<Medida> ms, int qual) {
		double pts = media(ms, m -> m.campeaoLanterna().containsKey(1) ? m.campeaoLanterna().get(1)[qual] : Double.NaN);
		double max = media(ms, m -> m.campeaoLanterna().containsKey(1) ? m.campeaoLanterna().get(1)[2] : Double.NaN);
		return num(pts) + " (" + pct(pts / max) + ")";
	}

	private static String forca(List<Medida> ms, int div) {
		double media = media(ms, m -> m.forcaDivisao().containsKey(div)
				? m.forcaDivisao().get(div)[0] / m.forcaDivisao().get(div)[1] : Double.NaN);
		double menor = media(ms, m -> m.forcaDivisao().containsKey(div) ? m.forcaDivisao().get(div)[2] : Double.NaN);
		double maior = media(ms, m -> m.forcaDivisao().containsKey(div) ? m.forcaDivisao().get(div)[3] : Double.NaN);
		return Double.isNaN(media) ? "–" : num(media) + " (" + num(menor) + " a " + num(maior) + ")";
	}

	private static double saldo(Medida m, int div, int i) {
		double[] s = m.saldoDivisao().get(div);
		return s == null ? Double.NaN : s[i];
	}

	private static double media(List<Medida> ms, java.util.function.ToDoubleFunction<Medida> f) {
		return ms.stream().mapToDouble(f).filter(x -> !Double.isNaN(x)).average().orElse(Double.NaN);
	}

	private static String num(double x) {
		return Double.isNaN(x) ? "–" : String.format(Locale.ROOT, "%.1f", x).replace('.', ',');
	}

	private static String num2(double x) {
		return Double.isNaN(x) ? "–" : String.format(Locale.ROOT, "%.2f", x).replace('.', ',');
	}

	private static String pct(double x) {
		return Double.isNaN(x) ? "–" : String.format(Locale.ROOT, "%.0f%%", 100 * x);
	}

	private static int inteiro(String nome, int padrao) {
		String v = System.getenv(nome);
		return v == null || v.isBlank() ? padrao : Integer.parseInt(v.trim());
	}
}
