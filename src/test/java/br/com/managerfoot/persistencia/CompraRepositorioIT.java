package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.economia.CaixaDaRodada;
import br.com.managerfoot.economia.PrecoJogador;

/** Migração V19 e compra direta ({@link CompraRepositorio}) contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CompraRepositorioIT {

	@Autowired
	CompraRepositorio compras;

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

	@BeforeEach
	void criaJogoComTemporada() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		usuario = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = :jogo").param("jogo", jogo)
			.query(Long.class).single();
		clubeDoUsuario = jogos.clubeDoTreinador(jogo, usuario).orElseThrow();
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
	}

	@Test
	void compraPagaOPrecoLevaOJogadorEGravaOsLancamentos() {
		Jogador alvo = maisBaratoAVenda();
		long vendedor = clubeDe(alvo.id());
		long esperado = PrecoJogador.padrao().preco(alvo, "BRA");

		long pago = compras.comprar(jogo, usuario, alvo.id());

		assertThat(pago).isEqualTo(esperado);
		assertThat(clubeDe(alvo.id())).isEqualTo(clubeDoUsuario);
		assertThat(jdbc.sql("SELECT a_venda FROM jogo_jogador WHERE id = :j").param("j", alvo.id())
			.query(Boolean.class).single()).isFalse();
		// caixas abertos com o saldo inicial: o comprador perde o preço e o vendedor ganha
		assertThat(saldo(clubeDoUsuario) - saldoInicial(clubeDoUsuario, alvo)).isEqualTo(-pago);
		assertThat(lancamento(clubeDoUsuario, "COMPRA", alvo.id())).isEqualTo(-pago);
		assertThat(lancamento(vendedor, "VENDA", alvo.id())).isEqualTo(pago);

		// o mesmo jogador não está mais à venda
		assertThatThrownBy(() -> compras.comprar(jogo, usuario, alvo.id())).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void naoCompraJogadorSemAnuncioNemDoProprioClube() {
		long semAnuncio = jdbc.sql("""
				SELECT jj.id FROM jogo_jogador jj JOIN jogo_clube c ON c.id = jj.clube_id
				WHERE c.jogo_id = :jogo AND jj.clube_id <> :clube AND NOT jj.a_venda LIMIT 1""")
			.param("jogo", jogo).param("clube", clubeDoUsuario).query(Long.class).single();
		assertThatThrownBy(() -> compras.comprar(jogo, usuario, semAnuncio)).isInstanceOf(NoSuchElementException.class);

		long doUsuario = jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :clube LIMIT 1")
			.param("clube", clubeDoUsuario).query(Long.class).single();
		jdbc.sql("UPDATE jogo_jogador SET a_venda = true WHERE id = :j").param("j", doUsuario).update();
		assertThatThrownBy(() -> compras.comprar(jogo, usuario, doUsuario)).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void recusaSemCaixaComPlantelCheioEDuranteARodada() {
		Jogador alvo = maisBaratoAVenda();
		compras.comprar(jogo, usuario, alvo.id()); // abre os caixas
		Jogador outro = maisBaratoAVenda();

		jdbc.sql("UPDATE jogo_clube SET saldo = 0 WHERE id = :c").param("c", clubeDoUsuario).update();
		assertThatThrownBy(() -> compras.comprar(jogo, usuario, outro.id()))
			.isInstanceOf(CompraRecusadaException.class).hasMessageContaining("Caixa insuficiente");

		jdbc.sql("UPDATE jogo_clube SET saldo = 1000000000000 WHERE id = :c").param("c", clubeDoUsuario).update();
		// 24 com a compra; mais um de outro clube completa 25
		long extra = jdbc.sql("""
				SELECT jj.id FROM jogo_jogador jj JOIN jogo_clube c ON c.id = jj.clube_id
				WHERE c.jogo_id = :jogo AND jj.clube_id <> :clube AND NOT jj.a_venda LIMIT 1""")
			.param("jogo", jogo).param("clube", clubeDoUsuario).query(Long.class).single();
		long origem = clubeDe(extra);
		jdbc.sql("UPDATE jogo_jogador SET clube_id = :c WHERE id = :j").param("c", clubeDoUsuario).param("j", extra).update();
		assertThatThrownBy(() -> compras.comprar(jogo, usuario, outro.id()))
			.isInstanceOf(CompraRecusadaException.class).hasMessageContaining("25");

		jdbc.sql("UPDATE jogo_jogador SET clube_id = :c WHERE id = :j").param("c", origem).param("j", extra).update();
		jdbc.sql("""
				INSERT INTO jogo_rodada_ao_vivo (jogo_id, temporada_id, rodada)
				SELECT :jogo, id, 1 FROM jogo_temporada WHERE jogo_id = :jogo""")
			.param("jogo", jogo).update();
		assertThatThrownBy(() -> compras.comprar(jogo, usuario, outro.id()))
			.isInstanceOf(CompraRecusadaException.class).hasMessageContaining("rodada");
	}

	private Jogador maisBaratoAVenda() {
		List<Long> ids = jdbc.sql("""
				SELECT jj.id FROM jogo_jogador jj JOIN jogo_clube c ON c.id = jj.clube_id
				WHERE c.jogo_id = :jogo AND jj.a_venda""")
			.param("jogo", jogo).query(Long.class).list();
		return jogos.carregarPlanteis(jogo).values().stream().flatMap(List::stream).filter(j -> ids.contains(j.id()))
			.min((a, b) -> Long.compare(PrecoJogador.padrao().preco(a, "BRA"), PrecoJogador.padrao().preco(b, "BRA")))
			.orElseThrow();
	}

	private long saldoInicial(long clube, Jogador comprado) {
		// saldo inicial do plantel de antes da compra
		List<Jogador> antes = jogos.carregarPlanteis(jogo).get(clube).stream().filter(j -> j.id() != comprado.id()).toList();
		return CaixaDaRodada.padrao().saldoInicial(antes);
	}

	private long clubeDe(long jogador) {
		return jdbc.sql("SELECT clube_id FROM jogo_jogador WHERE id = :j").param("j", jogador).query(Long.class).single();
	}

	private long saldo(long clube) {
		return jdbc.sql("SELECT saldo FROM jogo_clube WHERE id = :c").param("c", clube).query(Long.class).single();
	}

	private long lancamento(long clube, String tipo, long jogador) {
		return jdbc.sql("SELECT valor FROM jogo_lancamento WHERE clube_id = :c AND tipo = :t AND jogador_id = :j")
			.param("c", clube).param("t", tipo).param("j", jogador).query(Long.class).single();
	}
}
