package br.com.managerfoot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.PaisPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.economia.AmpliacaoEstadio;
import br.com.managerfoot.economia.CaixaDaRodada;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.economia.ParametrosPublico;
import br.com.managerfoot.economia.PropostasPatrocinio;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.CaixaRepositorio;
import br.com.managerfoot.persistencia.CaixaRepositorio.LinhaExtrato;
import br.com.managerfoot.persistencia.CaixaRepositorio.VisaoFinancas;
import br.com.managerfoot.persistencia.ConsultasRepositorio;
import br.com.managerfoot.persistencia.ConsultasRepositorio.JogoNoCalendario;
import br.com.managerfoot.persistencia.ConsultasRepositorio.LinhaArtilheiro;
import br.com.managerfoot.persistencia.ConsultasRepositorio.VisaoArtilheiros;
import br.com.managerfoot.persistencia.ConsultasRepositorio.VisaoCalendario;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.CompraRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.PatrocinioRepositorio;
import br.com.managerfoot.persistencia.RodadaRepositorio;

/** Abas Classificação e Artilheiros e a ficha do jogador (SPEC §10.3, rev. 16), com o banco padrão. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AbasIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	BancoPadraoRepositorio bancoPadrao;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	JogosDoTreinadorRepositorio jogos;

	@Autowired
	JogoRepositorio jogo;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	ConsultasRepositorio consultas;

	@Autowired
	CaixaRepositorio caixas;

	@Autowired
	CompraRepositorio compras;

	@Autowired
	PatrocinioRepositorio patrocinios;

	@Autowired
	JdbcClient jdbc;

	Usuario treinador;
	long brasil;
	long jogoId;
	String base;

	@BeforeEach
	void jogoComTresRodadas() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao())
			.gerar(PaisPadrao.TODOS));
		brasil = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow().paisId();
		treinador = contas.criarAdministrador("a" + System.nanoTime() + "@exemplo.com", "Gabriel", "senha-boa-1");
		jogoId = jogos.criarComSorteio(brasil, treinador.id(), 13).jogoId();
		patrocinios.assinar(jogoId, treinador.id(), PropostasPatrocinio.Tipo.FIXA);
		// sem suspensos na escalação do treinador: a máquina escala todos para as rodadas do teste
		jdbc.sql("DELETE FROM jogo_escalacao e USING jogo_clube c WHERE c.id = e.clube_id AND c.jogo_id = :j")
			.param("j", jogoId).update();
		for (int rodada = 1; rodada <= 3; rodada++) {
			rodadas.avancar(jogoId, rodada);
		}
		base = "/jogos/" + jogoId;
	}

	@Test
	void classificacaoDaDivisaoDoUsuarioComODestaqueERebaixados() throws Exception {
		mvc.perform(get(base + "/classificacao").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("após a rodada 3")))
			.andExpect(content().string(containsString("do-usuario")))
			.andExpect(content().string(containsString("Desempate: pontos")))
			.andExpect(content().string(containsString("aria-current=\"page\">Classificação")));
		mvc.perform(get(base + "/classificacao").param("divisao", "1").with(como(treinador)))
			.andExpect(content().string(not(containsString("do-usuario"))))
			.andExpect(content().string(containsString("Rebaixados (")));
	}

	@Test
	void artilheirosDaTemporadaEDeTodosOsTemposBatemComOsGolsGravados() throws Exception {
		VisaoArtilheiros temporada = consultas.artilheirosDaTemporada(jogoId, treinador.id(), 1);
		VisaoArtilheiros todos = consultas.artilheirosDeTodosOsTempos(jogoId, treinador.id());
		LinhaArtilheiro primeiro = todos.linhas().getFirst();
		int golsGravados = jdbc.sql("SELECT count(*) FROM jogo_evento WHERE tipo = 'GOL' AND jogador_id = :j")
			.param("j", primeiro.jogadorId()).query(Integer.class).single();

		assertThat(primeiro.gols()).isEqualTo(golsGravados);
		assertThat(primeiro.posicao()).isEqualTo(1);
		assertThat(todos.linhas()).isSortedAccordingTo((a, b) -> Integer.compare(b.gols(), a.gols()));
		assertThat(temporada.linhas()).isNotEmpty().allMatch(l -> l.gols() > 0);

		mvc.perform(get(base + "/artilheiros").param("divisao", "1").with(como(treinador)))
			.andExpect(content().string(containsString("Temporada 2026 · ")))
			.andExpect(content().string(containsString(temporada.linhas().getFirst().nome())));
		mvc.perform(get(base + "/artilheiros/todos-os-tempos").with(como(treinador)))
			.andExpect(content().string(containsString("Clube atual")))
			.andExpect(content().string(containsString(primeiro.nome())));
	}

	@Test
	void fichaAbrePorCimaDoPlantelEDosArtilheiros() throws Exception {
		LinhaArtilheiro artilheiro = consultas.artilheirosDeTodosOsTempos(jogoId, treinador.id()).linhas().getFirst();

		mvc.perform(get(base + "/artilheiros/todos-os-tempos").param("ficha", String.valueOf(artilheiro.jogadorId()))
				.with(como(treinador)))
			.andExpect(content().string(containsString("Gols na carreira")))
			.andExpect(content().string(containsString("ficha-numero\">" + artilheiro.gols() + "<")));

		long doClube = jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :c LIMIT 1")
			.param("c", jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow()).query(Long.class).single();
		mvc.perform(get(base + "/plantel").param("ficha", String.valueOf(doClube)).with(como(treinador)))
			.andExpect(content().string(containsString("Amarelos na temporada")))
			.andExpect(content().string(containsString("aria-current=\"page\">Plantel")));
		// a venda por leilão só aparece na ficha do plantel, para jogador do próprio clube
		mvc.perform(get(base + "/plantel").param("ficha", String.valueOf(doClube)).with(como(treinador)))
			.andExpect(content().string(containsString("Vender por leilão")))
			.andExpect(content().string(containsString("Preço de tabela")));
		mvc.perform(get(base + "/artilheiros/todos-os-tempos").param("ficha", String.valueOf(artilheiro.jogadorId()))
				.with(como(treinador)))
			.andExpect(content().string(not(containsString("Vender por leilão"))));
		// jogador de fora deste jogo: a aba abre sem ficha
		mvc.perform(get(base + "/plantel").param("ficha", "-1").with(como(treinador)))
			.andExpect(content().string(not(containsString("Gols na carreira"))));
	}

	@Test
	void historicoAbreSemCampeoesNaPrimeiraTemporada() throws Exception {
		mvc.perform(get(base + "/historico").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("aria-current=\"page\">Histórico")))
			.andExpect(content().string(containsString("Nenhuma temporada terminou ainda.")))
			.andExpect(content().string(containsString("Ninguém foi campeão ainda.")));
	}

	@Test
	void vendaPorLeilaoPelaFichaMostraOResultadoNaTelaDosLeiloes() throws Exception {
		long clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		long atacante = jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :c AND posicao = 'ATACANTE' ORDER BY forca DESC LIMIT 1")
			.param("c", clube).query(Long.class).single();

		mvc.perform(post(base + "/mercado/vender").param("jogador", String.valueOf(atacante)).param("minimo", "dez")
				.with(como(treinador)).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(flash().attribute("erro", "Digite o lance mínimo só com números."));

		// mínimo acima de qualquer lance da máquina: ninguém leva
		String destino = mvc.perform(post(base + "/mercado/vender").param("jogador", String.valueOf(atacante)).param("minimo", "999.999.999")
				.with(como(treinador)).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andReturn().getResponse().getRedirectedUrl();
		assertThat(destino).startsWith(base + "/leiloes/");
		mvc.perform(get(destino).with(como(treinador)))
			.andExpect(content().string(containsString("Resultado do leilão")))
			.andExpect(content().string(containsString("fica no seu plantel")))
			.andExpect(content().string(containsString("Voltar ao plantel")))
			.andExpect(content().string(not(containsString("Ir para a rodada"))));

		// de novo antes da rodada: recusado, com o motivo
		mvc.perform(post(base + "/mercado/vender").param("jogador", String.valueOf(atacante)).param("minimo", "0")
				.with(como(treinador)).with(csrf()))
			.andExpect(flash().attribute("erro", containsString("já foi a leilão")));
	}

	@Test
	void calendarioDoClubeComResultadosEProximoJogo() throws Exception {
		long clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		VisaoCalendario calendario = consultas.calendario(jogoId, treinador.id(), clube).orElseThrow();

		assertThat(calendario.jogos()).hasSize(rodadas.totalDeRodadas(jogoId));
		assertThat(calendario.jogos().subList(0, 3)).allMatch(j -> j.adversario() == null || j.resultado() != null);
		assertThat(calendario.jogos().get(3).proximo()).isTrue();
		assertThat(calendario.jogos().subList(3, calendario.jogos().size())).allMatch(j -> j.resultado() == null);
		int pontos = calendario.jogos().stream().map(JogoNoCalendario::resultado)
			.mapToInt(r -> r == null ? 0 : r.equals("V") ? 3 : r.equals("E") ? 1 : 0).sum();
		assertThat(pontos).isEqualTo(calendario.linha().pontos());
		// faixa do clube (modelo 11c): os últimos resultados na ordem e a moral gravada
		assertThat(calendario.ultimos()).isEqualTo(calendario.jogos().subList(0, 3).stream()
			.map(JogoNoCalendario::resultado).filter(r -> r != null).toList());
		assertThat(calendario.forcaEmCampo()).isPositive();

		mvc.perform(get(base + "/classificacao").param("calendario", String.valueOf(clube)).with(como(treinador)))
			.andExpect(content().string(containsString("aria-label=\"Janela do clube\"")))
			.andExpect(content().string(containsString("Temporada 2026 · ")))
			.andExpect(content().string(containsString("Placar com os gols do clube primeiro")))
			.andExpect(content().string(containsString("Moral do treinador")))
			.andExpect(content().string(containsString("0–6 vermelho · 7–14 amarelo · 15–30 verde")));
		mvc.perform(get(base + "/classificacao").param("calendario", "-1").with(como(treinador)))
			.andExpect(content().string(not(containsString("aria-label=\"Janela do clube\""))));
	}

	@Test
	void plantelDoClubeMostraTodosComGolsEOBotaoDeComprarSoNosAVenda() throws Exception {
		long meu = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		long outro = jdbc.sql("""
				SELECT c.id FROM jogo_clube c JOIN jogo_jogador jj ON jj.clube_id = c.id
				WHERE c.jogo_id = :j AND jj.a_venda AND c.id <> :meu AND c.divisao_id =
				      (SELECT divisao_id FROM jogo_clube WHERE id = :meu) LIMIT 1""")
			.param("j", jogoId).param("meu", meu).query(Long.class).single();
		CompraRepositorio.Vitrine v = compras.vitrine(jogoId, treinador.id(), outro);
		assertThat(v.doUsuario()).isFalse();
		assertThat(v.jogadores()).hasSize(jogo.carregarPlanteis(jogoId).get(outro).size());
		assertThat(v.aVenda()).isBetween(5L, 6L);
		assertThat(v.jogadores()).filteredOn(l -> !l.aVenda()).allMatch(l -> l.preco() == null && l.impedimento() == null);
		long golsNaTemporada = jdbc.sql("""
				SELECT count(*) FROM jogo_evento e JOIN jogo_jogador jj ON jj.id = e.jogador_id
				WHERE jj.clube_id = :c AND e.tipo = 'GOL'""").param("c", outro).query(Long.class).single();
		assertThat(v.jogadores().stream().mapToInt(CompraRepositorio.JogadorNaVitrine::gols).sum()).isEqualTo(golsNaTemporada);
		assertThat(compras.vitrine(jogoId, treinador.id(), meu).jogadores()).noneMatch(CompraRepositorio.JogadorNaVitrine::aVenda);

		CompraRepositorio.JogadorNaVitrine alvo = v.jogadores().stream().filter(l -> l.aVenda() && l.impedimento() == null)
			.findFirst().orElseThrow();
		mvc.perform(get(base + "/classificacao").param("calendario", String.valueOf(outro)).param("aba", "plantel")
				.with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("à venda)")))
			.andExpect(content().string(containsString("Comprar " + alvo.jogador().nome())));

		mvc.perform(post(base + "/mercado/comprar").param("jogador", String.valueOf(alvo.jogador().id()))
				.param("clube", String.valueOf(outro)).with(como(treinador)).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(flash().attributeExists("resultado"));
		assertThat(jdbc.sql("SELECT clube_id FROM jogo_jogador WHERE id = :id").param("id", alvo.jogador().id())
			.query(Long.class).single()).isEqualTo(meu);

		// segunda compra do mesmo jogador: recusada com aviso, sem erro
		mvc.perform(post(base + "/mercado/comprar").param("jogador", String.valueOf(alvo.jogador().id()))
				.param("clube", String.valueOf(outro)).with(como(treinador)).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(flash().attribute("erro", "Este jogador não está mais à venda."));
	}

	@Test
	void financasDoClubeDoUsuarioBatemComOExtratoGravado() throws Exception {
		long clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		VisaoFinancas f = caixas.financas(jogoId, treinador.id());
		long saldoGravado = jdbc.sql("SELECT saldo FROM jogo_clube WHERE id = :c").param("c", clube).query(Long.class).single();
		// a folha muda a cada rodada com a evolução de força (rev. 55)
		long patrocinioLancado = jdbc.sql("""
				SELECT l.valor FROM jogo_lancamento l WHERE l.clube_id = :c AND l.tipo = 'PATROCINIO'""")
			.param("c", clube).query(Long.class).single();

		assertThat(f.clube().id()).isEqualTo(clube);
		assertThat(f.moeda()).isEqualTo(Moeda.REAL);
		assertThat(f.rodadas()).isEqualTo(3);
		assertThat(f.extrato()).extracting(LinhaExtrato::rodada).containsExactly(1, 2, 3);
		assertThat(f.extrato()).allMatch(l -> l.salarios() < 0);
		assertThat(f.extrato()).allMatch(l -> l.emCasa() == null || l.emCasa() == (l.publico() != null));
		assertThat(f.saldo()).isEqualTo(saldoGravado);
		assertThat(f.extrato().getLast().saldo()).isEqualTo(saldoGravado);
		// o saldo do início inclui o patrocínio da temporada, lançado na rodada 0
		assertThat(patrocinioLancado).isEqualTo(patrocinio(clube));
		assertThat(f.saldoInicioTemporada()).isGreaterThan(patrocinio(clube));
		assertThat(f.saldoInicioTemporada() + f.resultado()).isEqualTo(saldoGravado);

		mvc.perform(get(base + "/financas").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("aria-current=\"page\">Finanças")))
			.andExpect(content().string(containsString("Extrato por rodada")))
			.andExpect(content().string(containsString("depois da rodada 3")))
			.andExpect(content().string(containsString(Moeda.REAL.formatar(saldoGravado))));
		mvc.perform(get(base + "/plantel").with(como(treinador)))
			.andExpect(content().string(containsString("Caixa " + Moeda.REAL.formatar(saldoGravado))));
	}

	private long patrocinio(long clube) {
		return jdbc.sql("SELECT valor FROM jogo_lancamento WHERE clube_id = :c AND tipo = 'PATROCINIO' AND rodada = 0")
			.param("c", clube)
			.query(Long.class)
			.single();
	}

	@Test
	void compraApareceNaColunaDeMercadoDaRodadaComONomeDoJogador() throws Exception {
		long clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		long alvo = jdbc.sql("""
				SELECT jj.id FROM jogo_jogador jj JOIN jogo_clube c ON c.id = jj.clube_id
				WHERE c.jogo_id = :j AND jj.a_venda ORDER BY jj.forca, jj.id LIMIT 1""")
			.param("j", jogoId).query(Long.class).single();
		String nome = jdbc.sql("SELECT nome FROM jogo_jogador WHERE id = :id").param("id", alvo).query(String.class).single();

		long pago = compras.comprar(jogoId, treinador.id(), alvo);

		VisaoFinancas f = caixas.financas(jogoId, treinador.id());
		LinhaExtrato rodada3 = f.extrato().getLast();
		assertThat(rodada3.rodada()).isEqualTo(3);
		assertThat(rodada3.mercado()).isEqualTo(-pago);
		assertThat(rodada3.detalhes()).singleElement().asString().startsWith("Compra: " + nome + " (");
		assertThat(f.totalMercado()).isEqualTo(-pago);
		long saldoGravado = jdbc.sql("SELECT saldo FROM jogo_clube WHERE id = :c").param("c", clube).query(Long.class).single();
		assertThat(rodada3.saldo()).isEqualTo(saldoGravado).isEqualTo(f.saldo());
		assertThat(f.saldoInicioTemporada() + f.resultado()).isEqualTo(saldoGravado);

		mvc.perform(get(base + "/financas").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Mercado e prêmios")))
			.andExpect(content().string(containsString("Patrocínio da temporada")))
			.andExpect(content().string(containsString("Compra: " + nome)));
	}

	@Test
	void financasAntesDaPrimeiraRodadaMostramOSaldoInicial() {
		long novo = jogos.criarComSorteio(brasil, treinador.id(), 21).jogoId();
		patrocinios.assinar(novo, treinador.id(), PropostasPatrocinio.Tipo.FIXA);
		VisaoFinancas f = caixas.financas(novo, treinador.id());
		long clube = jogo.clubeDoTreinador(novo, treinador.id()).orElseThrow();
		long folha = CaixaDaRodada.padrao().folha(jogo.carregarPlanteis(novo).get(clube));

		assertThat(f.extrato()).isEmpty();
		assertThat(f.saldo()).isEqualTo(10 * folha + patrocinio(clube)).isEqualTo(f.saldoInicioTemporada());
		assertThat(f.publicoMedio()).isNull();
	}

	@Test
	void janelaDoEstadioAmpliaUmaVezPagaNaHoraERecusaASegundaObra() throws Exception {
		long clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		AmpliacaoEstadio ampliacao = AmpliacaoEstadio.padrao();
		jdbc.sql("UPDATE jogo_clube SET estadio_capacidade = 20000, obra_rodadas = NULL, saldo = :s WHERE id = :c")
			.param("s", 2 * ampliacao.custo()).param("c", clube).update();
		String preco = Moeda.REAL.formatar(ampliacao.custo());

		mvc.perform(get(base + "/financas?estadio=true").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Capacidade atual")))
			.andExpect(content().string(containsString("Ampliar por " + preco)));

		mvc.perform(post(base + "/estadio/ampliar").with(como(treinador)).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(flash().attributeExists("resultado"));

		assertThat(jdbc.sql("SELECT obra_rodadas FROM jogo_clube WHERE id = :c").param("c", clube).query(Integer.class)
			.single()).isEqualTo(ParametrosPublico.padrao().rodadasDeObra());
		assertThat(jdbc.sql("SELECT saldo FROM jogo_clube WHERE id = :c").param("c", clube).query(Long.class).single())
			.isEqualTo(ampliacao.custo());
		VisaoFinancas f = caixas.financas(jogoId, treinador.id());
		assertThat(f.estadio()).isEqualTo(20000);
		assertThat(f.extrato().getLast().detalhes()).contains("Ampliação do estádio");

		mvc.perform(post(base + "/estadio/ampliar").with(como(treinador)).with(csrf()))
			.andExpect(flash().attribute("erro", "Obra em andamento."));
		mvc.perform(get(base + "/financas?estadio=true").with(como(treinador)))
			.andExpect(content().string(containsString("Obra em andamento")))
			.andExpect(content().string(not(containsString("Ampliar por"))));
	}

	@Test
	void semCaixaOBotaoFicaDesligadoEOServidorRecusa() throws Exception {
		long clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		jdbc.sql("UPDATE jogo_clube SET obra_rodadas = NULL, saldo = 0 WHERE id = :c").param("c", clube).update();

		mvc.perform(get(base + "/financas?estadio=true").with(como(treinador)))
			.andExpect(content().string(containsString("Caixa insuficiente")));
		mvc.perform(post(base + "/estadio/ampliar").with(como(treinador)).with(csrf()))
			.andExpect(flash().attribute("erro", "Caixa insuficiente."));
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_lancamento WHERE clube_id = :c AND tipo = 'ESTADIO'")
			.param("c", clube).query(Long.class).single()).isZero();

		Usuario outro = contas.criarAdministrador("e" + System.nanoTime() + "@exemplo.com", "Outro", "senha-boa-1");
		mvc.perform(post(base + "/estadio/ampliar").with(como(outro)).with(csrf())).andExpect(status().isNotFound());
	}

	@Test
	void outroUsuarioNaoVeAsAbas() throws Exception {
		Usuario outro = contas.criarAdministrador("o" + System.nanoTime() + "@exemplo.com", "Outro", "senha-boa-1");

		mvc.perform(get(base + "/classificacao").with(como(outro))).andExpect(status().isNotFound());
		mvc.perform(get(base + "/artilheiros").with(como(outro))).andExpect(status().isNotFound());
		mvc.perform(get(base + "/artilheiros/todos-os-tempos").with(como(outro))).andExpect(status().isNotFound());
		mvc.perform(get(base + "/financas").with(como(outro))).andExpect(status().isNotFound());
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
