package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.economia.PrecoJogador;
import br.com.managerfoot.persistencia.LeilaoRepositorio.Etapa;
import br.com.managerfoot.persistencia.LeilaoRepositorio.LeilaoNaTela;
import br.com.managerfoot.persistencia.LeilaoRepositorio.Situacao;

/** Migrações V20 a V22, leilões da máquina antes da rodada e venda do treinador por leilão ({@link LeilaoRepositorio}) contra PostgreSQL real. */
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

	@Autowired
	RodadaRepositorio rodadas;

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

	@Test
	void jogadorPostoALeilaoEsperaOOkDaTaticaESaiComOsLeiloesDaRodada() {
		Jogador jogador = maisForteDoUsuario();

		long leilaoId = leiloes.venderPorLeilao(jogo, usuario, jogador.id(), 0);

		// até o OK da tática ele fica no plantel, e não dá para pô-lo a leilão de novo
		assertThat(leiloes.venda(jogo, usuario, leilaoId).orElseThrow().leiloes().get(0).situacao()).isEqualTo(Situacao.ABERTO);
		assertThat(clubeDe(jogador.id())).isEqualTo(clubeDoUsuario);
		assertThatThrownBy(() -> leiloes.venderPorLeilao(jogo, usuario, jogador.id(), 0))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("já vai a leilão");

		assertThat(leiloes.preparar(jogo, usuario, 1)).isEqualTo(Etapa.LANCES);
		leiloes.darLances(jogo, usuario, 1, Map.of());

		List<LeilaoNaTela> naTela = leiloes.visao(jogo, usuario).orElseThrow().leiloes();
		assertThat(naTela).hasSize(2);
		LeilaoNaTela venda = naTela.stream().filter(l -> l.id() == leilaoId).findFirst().orElseThrow();
		assertThat(venda.situacao()).isEqualTo(Situacao.VENDIDO);
		assertThat(venda.vendidoPorVoce()).isTrue();
		assertThat(venda.lanceMinimo()).isZero();
		long comprador = venda.comprador().id();
		assertThat(comprador).isNotEqualTo(clubeDoUsuario);
		assertThat(clubeDe(jogador.id())).isEqualTo(comprador);
		assertThat(saldo(clubeDoUsuario)).isEqualTo(RICO + venda.valor());
		assertThat(lancamento(clubeDoUsuario, "VENDA", jogador.id())).isEqualTo(venda.valor());
		// a máquina dá um pouco abaixo do preço
		assertThat(venda.valor()).isLessThan(PrecoJogador.padrao().preco(jogador, "BRA"));

		// o leilão da máquina saiu junto; ele não é uma venda do treinador
		LeilaoNaTela daMaquina = naTela.stream().filter(l -> l.id() != leilaoId).findFirst().orElseThrow();
		assertThat(daMaquina.vendedor().id()).isEqualTo(vendedor);
		assertThat(leiloes.venda(jogo, usuario, daMaquina.id())).isEmpty();
	}

	@Test
	void semLanceNoMinimoOJogadorFicaESoVoltaDepoisDaRodada() {
		Jogador jogador = maisForteDoUsuario();
		long acima = PrecoJogador.padrao().preco(jogador, "BRA") / 100 * 2;

		long leilaoId = leiloes.venderPorLeilao(jogo, usuario, jogador.id(), acima);
		leiloes.preparar(jogo, usuario, 1);
		leiloes.darLances(jogo, usuario, 1, Map.of());

		LeilaoNaTela venda = leiloes.venda(jogo, usuario, leilaoId).orElseThrow().leiloes().get(0);
		assertThat(venda.situacao()).isEqualTo(Situacao.SEM_LANCE);
		assertThat(clubeDe(jogador.id())).isEqualTo(clubeDoUsuario);
		assertThatThrownBy(() -> leiloes.venderPorLeilao(jogo, usuario, jogador.id(), 0))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("já foi a leilão");
	}

	@Test
	void vendaRecusadaComMinimoNegativoJogadorDeOutroClubeOuRodadaAoVivo() {
		Jogador jogador = maisForteDoUsuario();
		long deOutro = jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :c LIMIT 1").param("c", vendedor)
			.query(Long.class).single();

		assertThatThrownBy(() -> leiloes.venderPorLeilao(jogo, usuario, jogador.id(), -1))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("negativo");
		assertThatThrownBy(() -> leiloes.venderPorLeilao(jogo, usuario, deOutro, 0)).isInstanceOf(NoSuchElementException.class);
		saldo(vendedor, RICO);
		aoVivo.iniciar(jogo, usuario, 1, Instant.now());
		assertThatThrownBy(() -> leiloes.venderPorLeilao(jogo, usuario, jogador.id(), 0))
			.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("em andamento");
		assertThat(clubeDe(jogador.id())).isEqualTo(clubeDoUsuario);
	}

	@Test
	void caixaNegativoPoeEmLeilaoSemMinimoUmJogadorSemContratoRenovado() {
		saldo(clubeDoUsuario, -1);
		// comprado nesta temporada: contrato renovado, fica fora do sorteio
		long renovado = jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :c ORDER BY id LIMIT 1")
			.param("c", clubeDoUsuario).query(Long.class).single();
		jdbc.sql("UPDATE jogo_jogador SET contrato_temporada = 1 WHERE id = :j").param("j", renovado).update();

		// o caixa do treinador não alcança o leilão do vendedor e ele não dá lance no próprio jogador
		assertThat(leiloes.preparar(jogo, usuario, 1)).isEqualTo(Etapa.RESULTADO);

		List<LeilaoNaTela> lista = leiloes.visao(jogo, usuario).orElseThrow().leiloes();
		LeilaoNaTela doUsuario = lista.stream().filter(LeilaoNaTela::vendidoPorVoce).findFirst().orElseThrow();
		assertThat(lista).hasSize(2);
		assertThat(doUsuario.lanceMinimo()).isZero();
		assertThat(doUsuario.jogador().id()).isNotEqualTo(renovado);
		assertThat(doUsuario.situacao()).isIn(Situacao.VENDIDO, Situacao.SEM_LANCE);
	}

	@Test
	void caixaNegativoComTodosRenovadosNaoVendeNinguemDuranteATemporada() {
		saldo(clubeDoUsuario, -1);
		saldo(vendedor, RICO);
		jdbc.sql("UPDATE jogo_jogador SET contrato_temporada = 1 WHERE clube_id = :c").param("c", clubeDoUsuario).update();

		assertThat(leiloes.preparar(jogo, usuario, 1)).isEqualTo(Etapa.RODADA);
		assertThat(leiloes.visao(jogo, usuario)).isEmpty();
	}

	@Test
	void compraRenovaOContratoAteOFimDaTemporada() {
		leiloes.preparar(jogo, usuario, 1);
		LeilaoNaTela leilao = unico();
		leiloes.darLances(jogo, usuario, 1, Map.of(leilao.id(), PrecoJogador.padrao().preco(leilao.jogador(), "BRA") / 100));

		assertThat(jdbc.sql("SELECT contrato_temporada FROM jogo_jogador WHERE id = :j").param("j", leilao.jogador().id())
			.query(Integer.class).single()).isEqualTo(1);
	}

	@Test
	void maquinaNaoPoeDeVoltaALeilaoQuemComprouNaTemporada() {
		// o mais caro de todos: sem a regra, seria o primeiro que a máquina venderia
		Jogador jogador = maisForteDoUsuario();
		jdbc.sql("UPDATE jogo_jogador SET forca = 50, asterisco = false WHERE id = :j").param("j", jogador.id()).update();
		saldo(vendedor, RICO);
		long leilaoId = leiloes.venderPorLeilao(jogo, usuario, jogador.id(), 0);
		leiloes.preparar(jogo, usuario, 1);
		long comprador = leiloes.venda(jogo, usuario, leilaoId).orElseThrow().leiloes().get(0).comprador().id();
		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
		RodadasDeTeste.jogarRodada(rodadas, jogo, 1);

		// a compra deixou o comprador abaixo do limite: ele precisa vender na rodada seguinte
		jdbc.sql("UPDATE jogo_clube SET saldo = :rico WHERE jogo_id = :jogo").param("rico", RICO).param("jogo", jogo).update();
		saldo(comprador, 0);
		saldo(clubeDoUsuario, 0);
		leiloes.preparar(jogo, usuario, 2);

		List<LeilaoNaTela> lista = leiloes.visao(jogo, usuario).orElseThrow().leiloes();
		assertThat(lista).singleElement().satisfies(l -> {
			assertThat(l.vendedor().id()).isEqualTo(comprador);
			assertThat(l.jogador().id()).isNotEqualTo(jogador.id());
		});
		assertThat(clubeDe(jogador.id())).isEqualTo(comprador);
	}

	private Jogador maisForteDoUsuario() {
		return jogos.carregarPlanteis(jogo).get(clubeDoUsuario).stream()
			.filter(j -> j.posicao() != Posicao.GOLEIRO)
			.max(Comparator.comparingInt(Jogador::forca)).orElseThrow();
	}

	@Test
	void oAsteriscoDaArtilhariaApareceNoLeilaoSoSeValeNaDivisaoDoTreinador() {
		assertThat(leiloes.preparar(jogo, usuario, 1)).isEqualTo(Etapa.LANCES);
		long jogador = unico().jogador().id();
		assertThat(unico().asteriscoDaArtilharia()).isFalse();
		long temporada = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :jogo").param("jogo", jogo)
			.query(Long.class).single();
		long divisao = jdbc.sql("SELECT divisao_id FROM jogo_clube WHERE id = :clube").param("clube", clubeDoUsuario)
			.query(Long.class).single();
		long outra = jdbc.sql("SELECT id FROM jogo_divisao WHERE jogo_id = :jogo AND id <> :divisao LIMIT 1")
			.param("jogo", jogo).param("divisao", divisao).query(Long.class).single();
		String sorteia = """
				INSERT INTO jogo_bonus_artilharia (temporada_id, divisao_id, jogador_id, extra) VALUES (:t, :d, :j, false)
				ON CONFLICT (temporada_id, jogador_id) DO UPDATE SET divisao_id = :d, extra = false""";

		jdbc.sql(sorteia).param("t", temporada).param("d", outra).param("j", jogador).update();
		assertThat(unico().asteriscoDaArtilharia()).isFalse();

		jdbc.sql(sorteia).param("t", temporada).param("d", divisao).param("j", jogador).update();
		assertThat(unico().asteriscoDaArtilharia()).isTrue();
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
