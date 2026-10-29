package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.economia.Premiacao;
import br.com.managerfoot.temporada.ClubeInicial;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.Temporada;
import br.com.managerfoot.temporada.TransicaoTemporada;

/** {@link FimDeTemporadaRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FimDeTemporadaRepositorioIT {

	private static final long SEMENTE_NOVA = 77;

	@Autowired
	FimDeTemporadaRepositorio fim;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	EscalacaoRepositorio escalacoes;

	@Autowired
	ConsultasRepositorio consultas;

	long jogo;

	@BeforeEach
	void criaJogoComTemporada() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
	}

	@Test
	void temporadaComRodadasAJogarNaoEncerra() {
		rodadas.avancar(jogo, 1);

		assertThatThrownBy(() -> fim.avancarTemporada(jogo, 1, SEMENTE_NOVA)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("29 rodada(s) a jogar");
		assertThat(temporadas.carregarAtual(jogo).numero()).isEqualTo(1);
	}

	@Test
	void caixaNegativoVendeUmJogadorNoInicioDaTemporadaMesmoComTodosRenovados() {
		for (int rodada = 1; rodada <= 30; rodada++) {
			rodadas.avancar(jogo, rodada);
		}
		long clube = jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = ?").param(jogo).query(Long.class).single();
		// renovados na temporada 1: na 2 ficam sem contrato e qualquer um pode ir a leilão
		jdbc.sql("UPDATE jogo_jogador SET contrato_temporada = 1 WHERE clube_id = ?").param(clube).update();
		jdbc.sql("UPDATE jogo_clube SET saldo = -1000000000 WHERE id = ?").param(clube).update();

		FimDeTemporadaRepositorio.NovaTemporada nova = fim.avancarTemporada(jogo, 1, SEMENTE_NOVA);

		assertThat(nova.vendas()).hasSize(1);
		LeilaoRepositorio.VendaPorCaixa venda = nova.vendas().get(0);
		assertThat(venda.vendedorId()).isEqualTo(clube);
		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_leilao l JOIN jogo_temporada t ON t.id = l.temporada_id
				WHERE t.jogo_id = ? AND t.numero = 2 AND l.rodada = 0 AND l.lance_minimo = 0 AND l.jogador_id = ?""")
			.param(jogo).param(venda.jogador().id()).query(Long.class).single()).isEqualTo(1);
		// o patrocínio vem depois, na mesma rodada 0
		assertThat(contar("SELECT count(*) FROM jogo_lancamento l JOIN jogo_temporada t ON t.id = l.temporada_id "
				+ "WHERE t.jogo_id = ? AND t.numero = 2 AND l.tipo = 'PATROCINIO'")).isPositive();
	}

	@Test
	void fimDaTemporadaPromoveRebaixaZeraAmarelosEIniciaASeguinte() {
		for (int rodada = 1; rodada <= 30; rodada++) {
			rodadas.avancar(jogo, rodada);
		}
		ConfiguracaoLiga liga = jogos.carregarLiga(jogo);
		List<String> lideres = rodadas.classificacao(jogo).values().stream()
			.map(c -> c.linhas().getFirst().clube().nome()).toList();
		TransicaoTemporada esperada = TransicaoTemporada.aplicar(liga, rodadas.classificacao(jogo),
				new SplittableRandom(~SEMENTE_NOVA));
		long suspensoesAntes = contar("SELECT coalesce(sum(j.suspensao), 0) FROM jogo_jogador j "
				+ "JOIN jogo_clube c ON c.id = j.clube_id WHERE c.jogo_id = ?");
		assertThat(contar("SELECT count(*) FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id "
				+ "WHERE c.jogo_id = ? AND j.amarelos > 0")).isPositive();

		FimDeTemporadaRepositorio.NovaTemporada nova = fim.avancarTemporada(jogo, 1, SEMENTE_NOVA);

		assertThat(nova.transicao()).isEqualTo(esperada);
		ConfiguracaoLiga proxima = jogos.carregarLiga(jogo);
		assertThat(nomes(proxima.divisoes().get(0))).containsExactlyInAnyOrderElementsOf(nomes(esperada.proxima().divisoes().get(0)));
		assertThat(nomes(proxima.divisoes().get(1))).containsExactlyInAnyOrderElementsOf(nomes(esperada.proxima().divisoes().get(1)));
		assertThat(proxima.filaDeEspera()).containsExactlyInAnyOrderElementsOf(esperada.semLiga());
		// os 2 da fila, que esperavam, subiram para a Série B
		assertThat(nomes(proxima.divisoes().get(1))).contains("Fila 1", "Fila 2");

		assertThat(contar("SELECT count(*) FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id "
				+ "WHERE c.jogo_id = ? AND j.amarelos > 0")).isZero();
		assertThat(contar("SELECT coalesce(sum(j.suspensao), 0) FROM jogo_jogador j "
				+ "JOIN jogo_clube c ON c.id = j.clube_id WHERE c.jogo_id = ?")).isEqualTo(suspensoesAntes);

		Temporada segunda = temporadas.carregarAtual(jogo);
		assertThat(segunda).isEqualTo(nova.temporada());
		assertThat(segunda.numero()).isEqualTo(2);
		assertThat(rodadas.rodadasProcessadas(jogo)).isZero();
		assertThat(segunda.divisoes().get(1).calendario().jogos())
			.anyMatch(j -> j.mandante().nome().equals("Fila 1"));

		// histórico: o líder final de cada divisão, com o ano da temporada (rev. 40)
		assertThat(jdbc.sql("SELECT clube_nome FROM jogo_campeao WHERE jogo_id = ? AND temporada = 1 AND ano = 2026")
			.param(jogo).query(String.class).list()).containsExactlyInAnyOrderElementsOf(lideres);
		assertThat(jogos.ano(jogo, 2)).isEqualTo(2027);
		// cada campeão tem treinador: o usuário ou o da máquina (rev. 54)
		assertThat(contar("SELECT count(*) FROM jogo_campeao WHERE jogo_id = ? "
				+ "AND (usuario_id IS NULL) = (treinador_maquina_id IS NULL)")).isZero();
		long usuario = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = ?").param(jogo).query(Long.class).single();
		ConsultasRepositorio.VisaoHistorico historico = consultas.historico(jogo, usuario);
		assertThat(historico.anos()).hasSize(1);
		assertThat(historico.anos().get(0).ano()).isEqualTo(2026);
		assertThat(historico.anos().get(0).campeoes()).extracting(ConsultasRepositorio.CampeaoNoAno::clube)
			.containsExactlyInAnyOrderElementsOf(lideres);
		assertThat(historico.ranking()).hasSize(1);
		assertThat(historico.ranking().get(0).posicao()).isEqualTo(1);
		assertThat(historico.ranking().get(0).titulos()).isEqualTo(1);

		// prêmio da liga na última rodada da temporada 1 e patrocínio na rodada 0 da 2 (rev. 45 e 46)
		Premiacao premiacao = Premiacao.padrao();
		int limiteA = liga.divisoes().get(0).limiteForca();
		int limiteB = liga.divisoes().get(1).limiteForca();
		assertThat(contar("SELECT count(*) FROM jogo_lancamento l JOIN jogo_temporada t ON t.id = l.temporada_id "
				+ "WHERE t.jogo_id = ? AND t.numero = 1 AND l.tipo = 'PREMIO' AND l.rodada = 30")).isEqualTo(3 + 3);
		assertThat(lancamento(lideres.get(0), 1, "PREMIO")).isEqualTo(premiacao.premio(1, limiteA));
		assertThat(lancamento(lideres.get(1), 1, "PREMIO")).isEqualTo(premiacao.premio(1, limiteB));
		assertThat(contar("SELECT count(*) FROM jogo_lancamento l JOIN jogo_temporada t ON t.id = l.temporada_id "
				+ "WHERE t.jogo_id = ? AND t.numero = 2 AND l.tipo = 'PATROCINIO' AND l.rodada = 0")).isEqualTo(16 + 15);
		assertThat(lancamento(lideres.get(0), 2, "PATROCINIO")).isEqualTo(premiacao.patrocinio(1, 16, limiteA));
		// quem veio da fila recebe como o último da divisão em que entra
		assertThat(lancamento("Fila 1", 2, "PATROCINIO")).isEqualTo(premiacao.patrocinio(15, 15, limiteB));

		// posição final da temporada passada para o público das primeiras rodadas (rev. 57): o campeão
		// da Série A é 1º de 16; quem veio da fila conta como o último da Série B; a fila fica sem
		assertThat(posicaoAnterior(lideres.get(0))).isEqualTo("1/16");
		assertThat(posicaoAnterior("Fila 1")).isEqualTo("15/15");
		assertThat(contar("SELECT count(*) FROM jogo_clube WHERE jogo_id = ? AND divisao_id IS NOT NULL "
				+ "AND posicao_anterior IS NULL")).isZero();
		assertThat(contar("SELECT count(*) FROM jogo_clube WHERE jogo_id = ? AND divisao_id IS NULL "
				+ "AND posicao_anterior IS NOT NULL")).isZero();

		assertThatThrownBy(() -> fim.avancarTemporada(jogo, 1, SEMENTE_NOVA)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("é a 2");
		assertThat(rodadas.avancar(jogo, 1).numero()).isEqualTo(1);
	}

	private long lancamento(String clube, int temporada, String tipo) {
		return jdbc.sql("""
				SELECT l.valor FROM jogo_lancamento l JOIN jogo_temporada t ON t.id = l.temporada_id
				JOIN jogo_clube c ON c.id = l.clube_id
				WHERE t.jogo_id = :jogo AND t.numero = :temporada AND c.nome = :clube AND l.tipo = :tipo""")
			.param("jogo", jogo)
			.param("temporada", temporada)
			.param("clube", clube)
			.param("tipo", tipo)
			.query(Long.class)
			.single();
	}

	private String posicaoAnterior(String clube) {
		return jdbc.sql("SELECT posicao_anterior || '/' || clubes_divisao_anterior FROM jogo_clube WHERE jogo_id = ? AND nome = ?")
			.param(jogo).param(clube).query(String.class).single();
	}

	private long contar(String sql) {
		return jdbc.sql(sql).param(jogo).query(Long.class).single();
	}

	private static List<String> nomes(Divisao divisao) {
		return divisao.clubes().stream().map(Clube::nome).toList();
	}

	@Test
	void treinadorComClubeNaFilaEhDemitidoEAssumeUmDosMaisFracosDaUltimaDivisao() {
		for (int rodada = 1; rodada <= 30; rodada++) {
			rodadas.avancar(jogo, rodada);
		}
		long usuario = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = :j").param("j", jogo)
			.query(Long.class).single();
		long antigo = jogos.clubeDoTreinador(jogo, usuario).orElseThrow();
		// o clube do treinador perde todos os jogos: termina em último e cai para a fila
		jdbc.sql("UPDATE jogo_partida SET gols_mandante = 0, gols_visitante = 9 WHERE mandante_id = :c").param("c", antigo)
			.update();
		jdbc.sql("UPDATE jogo_partida SET gols_mandante = 9, gols_visitante = 0 WHERE visitante_id = :c").param("c", antigo)
			.update();
		jdbc.sql("INSERT INTO jogo_escalacao (clube_id, formacao) VALUES (:c, 'F_4_4_2')").param("c", antigo).update();

		FimDeTemporadaRepositorio.NovaTemporada nova = fim.avancarTemporada(jogo, 1, SEMENTE_NOVA);

		assertThat(nova.transicao().proxima().filaDeEspera()).extracting(Clube::id).contains(antigo);
		Clube novo = nova.demitidos().get(usuario);
		assertThat(novo).isNotNull();
		assertThat(jogos.clubeDoTreinador(jogo, usuario)).hasValue(novo.id());
		assertThat(ClubeInicial.candidatos(nova.transicao().proxima().divisoes().getLast(), 4)).contains(novo);
		assertThat(escalacoes.carregar(novo.id())).isPresent();
		assertThat(escalacoes.carregar(antigo)).isEmpty();
		assertThat(nova.temporada().divisoes().getLast().calendario().jogos())
			.anyMatch(j -> j.mandante().id() == novo.id() || j.visitante().id() == novo.id());
	}
}
