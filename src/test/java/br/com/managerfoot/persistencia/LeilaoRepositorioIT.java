package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.economia.PrecoJogador;
import br.com.managerfoot.persistencia.LeilaoRepositorio.Etapa;
import br.com.managerfoot.persistencia.LeilaoRepositorio.LeilaoNaTela;
import br.com.managerfoot.persistencia.LeilaoRepositorio.Situacao;

/** Migração V20 e leilões da máquina antes da rodada ({@link LeilaoRepositorio}) contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LeilaoRepositorioIT {

	private static final long RICO = 1_000_000_000_00L;

	@Autowired
	LeilaoRepositorio leiloes;

	@Autowired
	CompraRepositorio compras;

	@Autowired
	RodadaAoVivoRepositorio aoVivo;

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

	long jogo;
	long usuario;
	long clubeDoUsuario;
	long vendedor;

	@BeforeEach
	void criaJogoComTemporada() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		usuario = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = :jogo").param("jogo", jogo)
			.query(Long.class).single();
		clubeDoUsuario = jogos.clubeDoTreinador(jogo, usuario).orElseThrow();
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		vendedor = jdbc.sql("""
				SELECT id FROM jogo_clube WHERE jogo_id = :jogo AND divisao_id IS NOT NULL AND id <> :usuario
				ORDER BY id LIMIT 1""")
			.param("jogo", jogo).param("usuario", clubeDoUsuario).query(Long.class).single();
		// todos com caixa de sobra, menos o vendedor
		jdbc.sql("UPDATE jogo_clube SET estadio_capacidade = 10000, saldo = :rico WHERE jogo_id = :jogo")
			.param("rico", RICO).param("jogo", jogo).update();
		saldo(vendedor, 0);
	}

	@Test
	void semClubeAbaixoDoLimiteARodadaComecaDireto() {
		saldo(vendedor, RICO);

		assertThat(leiloes.preparar(jogo, usuario, 1)).isEqualTo(Etapa.RODADA);
		assertThat(leiloes.visao(jogo, usuario)).isEmpty();
	}

	@Test
	void treinadorDaLanceLevaOJogadorEOsLeiloesNaoReabrem() {
		assertThat(leiloes.preparar(jogo, usuario, 1)).isEqualTo(Etapa.LANCES);
		LeilaoNaTela leilao = unico();
		assertThat(leilao.situacao()).isEqualTo(Situacao.ABERTO);
		assertThat(leilao.impedimento()).isNull();
		assertThat(leilao.vendedor().id()).isEqualTo(vendedor);
		long preco = PrecoJogador.padrao().preco(leilao.jogador(), "BRA");
		assertThat(leilao.lanceMinimo()).isEqualTo(PrecoJogador.padrao().lanceInicialLeilao(leilao.jogador(), "BRA"));

		// rodada ao vivo não começa com leilão aberto
		assertThatThrownBy(() -> aoVivo.iniciar(jogo, usuario, 1, Instant.now())).isInstanceOf(IllegalStateException.class);

		// o preço cheio passa os lances da máquina (no máximo 95% do preço)
		leiloes.darLances(jogo, usuario, 1, Map.of(leilao.id(), preco / 100));

		LeilaoNaTela resolvido = unico();
		assertThat(resolvido.situacao()).isEqualTo(Situacao.VENDIDO);
		assertThat(resolvido.seu()).isTrue();
		assertThat(resolvido.valor()).isEqualTo(preco);
		assertThat(resolvido.seuLance()).isEqualTo(preco);
		assertThat(clubeDe(leilao.jogador().id())).isEqualTo(clubeDoUsuario);
		assertThat(saldo(clubeDoUsuario)).isEqualTo(RICO - preco);
		assertThat(saldo(vendedor)).isEqualTo(preco);
		assertThat(lancamento(clubeDoUsuario, "COMPRA", leilao.jogador().id())).isEqualTo(-preco);

		// confirmar de novo não reabre nem resolve outra vez
		assertThat(leiloes.preparar(jogo, usuario, 1)).isEqualTo(Etapa.RODADA);
		assertThatThrownBy(() -> leiloes.darLances(jogo, usuario, 1, Map.of())).isInstanceOf(IllegalStateException.class);
		assertThat(leiloes.visao(jogo, usuario).orElseThrow().leiloes()).hasSize(1);
	}

	@Test
	void semCaixaParaOLanceInicialOsLeiloesSaoResolvidosNaHora() {
		saldo(clubeDoUsuario, 0);

		assertThat(leiloes.preparar(jogo, usuario, 1)).isEqualTo(Etapa.RESULTADO);

		LeilaoNaTela leilao = unico();
		assertThat(leilao.situacao()).isIn(Situacao.VENDIDO, Situacao.SEM_LANCE);
		assertThat(leilao.seu()).isFalse();
		assertThat(leiloes.visao(jogo, usuario).orElseThrow().aguardandoLances()).isFalse();
	}

	@Test
	void lanceAbaixoDoInicialOuAcimaDoCaixaERecusadoSemGravarNada() {
		leiloes.preparar(jogo, usuario, 1);
		LeilaoNaTela leilao = unico();
		long minimo = leilao.lanceMinimo() / 100;

		assertThatThrownBy(() -> leiloes.darLances(jogo, usuario, 1, Map.of(leilao.id(), minimo - 1)))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("abaixo do lance inicial");
		assertThatThrownBy(() -> leiloes.darLances(jogo, usuario, 1, Map.of(leilao.id(), RICO)))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("acima do seu caixa");
		assertThat(unico().situacao()).isEqualTo(Situacao.ABERTO);
	}

	@Test
	void rodadaQueNaoEAProximaERecusada() {
		assertThatThrownBy(() -> leiloes.preparar(jogo, usuario, 2)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void jogadorCompradoDiretoDuranteOLeilaoCancelaOLeilao() {
		leiloes.preparar(jogo, usuario, 1);
		Jogador jogador = unico().jogador();
		jdbc.sql("UPDATE jogo_jogador SET a_venda = true WHERE id = :j").param("j", jogador.id()).update();
		compras.comprar(jogo, usuario, jogador.id());

		leiloes.darLances(jogo, usuario, 1, Map.of());

		assertThat(unico().situacao()).isEqualTo(Situacao.CANCELADO);
	}

	private LeilaoNaTela unico() {
		List<LeilaoNaTela> lista = leiloes.visao(jogo, usuario).orElseThrow().leiloes();
		assertThat(lista).hasSize(1);
		return lista.get(0);
	}

	private void saldo(long clube, long saldo) {
		jdbc.sql("UPDATE jogo_clube SET saldo = :saldo WHERE id = :c").param("saldo", saldo).param("c", clube).update();
	}

	private long saldo(long clube) {
		return jdbc.sql("SELECT saldo FROM jogo_clube WHERE id = :c").param("c", clube).query(Long.class).single();
	}

	private long clubeDe(long jogador) {
		return jdbc.sql("SELECT clube_id FROM jogo_jogador WHERE id = :j").param("j", jogador).query(Long.class).single();
	}

	private long lancamento(long clube, String tipo, long jogador) {
		return jdbc.sql("SELECT valor FROM jogo_lancamento WHERE clube_id = :c AND tipo = :t AND jogador_id = :j")
			.param("c", clube).param("t", tipo).param("j", jogador).query(Long.class).single();
	}
}
