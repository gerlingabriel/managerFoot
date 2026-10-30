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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.economia.PropostasPatrocinio;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.RodadaRepositorio;
import br.com.managerfoot.persistencia.RodadasDeTeste;
import br.com.managerfoot.temporada.Classificacao;

/** Fim de temporada na tela: o usuário só confirma (SPEC §15, 3c.4), com o banco padrão. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FimDeTemporadaIT {

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
	JdbcClient jdbc;

	Usuario treinador;
	long jogoId;
	String plantel;

	@BeforeEach
	void temporadaInteiraJogada() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao())
			.gerarBanco());
		long portugal = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> c.codigoPais().equals("POR")).findFirst().orElseThrow().paisId();
		treinador = contas.criarAdministrador("f" + System.nanoTime() + "@exemplo.com", "Gabriel", "senha-boa-1");
		jogoId = jogos.criarComSorteio(portugal, treinador.id(), 5).jogoId();
		plantel = "/jogos/" + jogoId + "/plantel";
		// a máquina escala o clube do treinador: suspensões não param a temporada do teste
		for (int rodada = 1; rodada <= rodadas.totalDeRodadas(jogoId); rodada++) {
			RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogoId);
			RodadasDeTeste.jogarRodada(rodadas, jogoId, rodada);
		}
		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogoId);
	}

	@Test
	void okComecaAProximaTemporadaUmaVezSo() throws Exception {
		mvc.perform(get(plantel).with(como(treinador)))
			.andExpect(content().string(containsString("Temporada 2026 encerrada")))
			.andExpect(content().string(containsString("OK, começar a próxima")));
		// o extrato da temporada inteira abre, com as linhas da copa se o clube a jogou (modelo 12f)
		boolean jogouCopa = jdbc.sql("""
				SELECT count(*) > 0 FROM jogo_partida p JOIN jogo_treinador t ON t.clube_id IN (p.mandante_id, p.visitante_id)
				WHERE t.jogo_id = :j AND p.confronto_id IS NOT NULL""").param("j", jogoId).query(Boolean.class).single();
		mvc.perform(get("/jogos/" + jogoId + "/financas").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(jogouCopa ? containsString("Copa na temporada") : containsString("Extrato por rodada")));

		mvc.perform(post("/jogos/" + jogoId + "/temporada").param("temporada", "1").with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl(plantel))
			.andExpect(flash().attribute("resultado", containsString("Começou a temporada 2027.")));
		mvc.perform(post("/jogos/" + jogoId + "/temporada").param("temporada", "1").with(como(treinador)).with(csrf()))
			.andExpect(flash().attribute("erro", "A temporada 2026 já foi encerrada."));

		assertThat(jdbc.sql("SELECT max(numero) FROM jogo_temporada WHERE jogo_id = :j").param("j", jogoId)
			.query(Integer.class).single()).isEqualTo(2);
		// o campeão de 2026 da 1ª divisão aparece no histórico com o treinador (modelo 8a)
		String campeao = jdbc.sql("""
				SELECT clube_nome FROM jogo_campeao WHERE jogo_id = :j AND divisao_prioridade = 1""")
			.param("j", jogoId).query(String.class).single();
		mvc.perform(get("/jogos/" + jogoId + "/historico").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString(">2026<")))
			.andExpect(content().string(containsString(campeao)))
			.andExpect(content().string(not(containsString("Ninguém foi campeão ainda."))))
			// o quadro da Copa com o campeão e o placar da final (modelo 13a, rev. 79)
			.andExpect(content().string(containsString(">Copa</h2>")))
			.andExpect(content().string(containsString(jdbc.sql("SELECT clube_nome FROM jogo_campeao_copa WHERE jogo_id = :j")
				.param("j", jogoId).query(String.class).single())))
			.andExpect(content().string(containsString("final ")));
		boolean naFila = jdbc.sql("""
				SELECT c.divisao_id IS NULL FROM jogo_treinador t JOIN jogo_clube c ON c.id = t.clube_id
				WHERE t.jogo_id = :j""").param("j", jogoId).query(Boolean.class).single();
		mvc.perform(get(plantel).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString(naFila ? "fila de espera" : "Próximo jogo · Rodada 1")));
	}

	@Test
	void convitePendenteSeguraAProximaTemporada() throws Exception {
		long clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		long outro = jdbc.sql("""
				SELECT id FROM jogo_clube WHERE jogo_id = :j AND id <> :c AND divisao_id IS NOT NULL ORDER BY id LIMIT 1""")
			.param("j", jogoId).param("c", clube).query(Long.class).single();
		long convite = jdbc.sql("""
				INSERT INTO jogo_troca_treinador (jogo_id, temporada_id, rodada, clube_id, outro_clube_id, convidado_id, situacao)
				SELECT :j, t.id, t.rodadas_processadas, :outro, :c, :u, 'CONVITE' FROM jogo_temporada t WHERE t.jogo_id = :j
				RETURNING id""")
			.param("j", jogoId).param("outro", outro).param("c", clube).param("u", treinador.id())
			.query(Long.class).single();

		mvc.perform(post("/jogos/" + jogoId + "/temporada").param("temporada", "1").with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl(plantel))
			.andExpect(flash().attribute("erro",
					"Responda ao convite para assumir outro clube antes de começar a próxima temporada."));
		assertThat(jdbc.sql("SELECT max(numero) FROM jogo_temporada WHERE jogo_id = :j").param("j", jogoId)
			.query(Integer.class).single()).isEqualTo(1);

		mvc.perform(post("/jogos/" + jogoId + "/convite").param("convite", String.valueOf(convite))
			.param("resposta", "RECUSAR").with(como(treinador)).with(csrf()))
			.andExpect(flash().attribute("resultado", containsString("Convite recusado")));
		mvc.perform(post("/jogos/" + jogoId + "/temporada").param("temporada", "1").with(como(treinador)).with(csrf()))
			.andExpect(flash().attribute("resultado", containsString("Começou a temporada 2027.")));
	}

	@Test
	void bonusDoPatrocinioPorAcessoEntraNaUltimaRodadaQuandoOClubeSobe() throws Exception {
		// o treinador passa a dirigir o líder da última divisão, com a oferta da temporada: ele sobe
		long antigo = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		Map<Integer, Classificacao> tabela = rodadas.classificacao(jogoId);
		long clube = tabela.get(tabela.keySet().stream().max(Integer::compare).orElseThrow()).linhas().getFirst().clube().id();
		jdbc.sql("UPDATE jogo_patrocinio SET clube_id = :novo WHERE clube_id = :antigo")
			.param("novo", clube).param("antigo", antigo).update();
		jdbc.sql("UPDATE jogo_treinador SET clube_id = :novo WHERE jogo_id = :j").param("novo", clube).param("j", jogoId).update();
		mvc.perform(post("/jogos/" + jogoId + "/patrocinio").param("proposta", "SE_SUBIR").with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl(plantel));
		Classificacao classificacao = tabela.values().stream()
			.filter(c -> c.linhas().stream().anyMatch(l -> l.clube().id() == clube)).findFirst().orElseThrow();
		int posicao = 1 + classificacao.linhas().stream().map(l -> l.clube().id()).toList().indexOf(clube);
		var divisao = jogo.carregarLiga(jogoId).divisoes().stream()
			.filter(d -> d.clubes().stream().anyMatch(c -> c.id() == clube)).findFirst().orElseThrow();
		boolean subiu = PropostasPatrocinio.padrao().subiu(divisao.prioridade(), posicao, divisao.promovidos());
		long valorDaTabela = jdbc.sql("SELECT tabela FROM jogo_patrocinio WHERE clube_id = :c").param("c", clube)
			.query(Long.class).single();
		int jogos = jdbc.sql("SELECT jogos FROM jogo_patrocinio WHERE clube_id = :c").param("c", clube)
			.query(Integer.class).single();
		long bonus = PropostasPatrocinio.padrao().propostas(valorDaTabela, jogos).get(2).seSubir();

		mvc.perform(post("/jogos/" + jogoId + "/temporada").param("temporada", "1").with(como(treinador)).with(csrf()));

		List<Long> pagos = jdbc.sql("""
				SELECT l.valor FROM jogo_lancamento l JOIN jogo_temporada t ON t.id = l.temporada_id
				WHERE l.clube_id = :c AND l.tipo = 'PATROCINIO' AND t.numero = 1 AND l.rodada > 0""")
			.param("c", clube).query(Long.class).list();
		assertThat(subiu).isTrue();
		assertThat(pagos).containsExactly(bonus);
	}

	@Test
	void clubeNaFilaMostraUmAvisoEmVezDeErro() throws Exception {
		mvc.perform(post("/jogos/" + jogoId + "/temporada").param("temporada", "1").with(como(treinador)).with(csrf()));
		long clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		jdbc.sql("UPDATE jogo_clube SET divisao_id = NULL WHERE id = :c").param("c", clube).update();

		mvc.perform(get(plantel).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Seu clube caiu para a fila de espera")));
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
