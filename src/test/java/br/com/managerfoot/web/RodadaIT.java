package br.com.managerfoot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.PaisPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.RodadaRepositorio;

/** Jogos da rodada ao vivo pela tela (modelo 1a; docs/decisions/0010), com o relógio do teste. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RodadaIT {

	private static final Instant T0 = Instant.parse("2026-10-28T20:00:00Z");

	@MockitoBean
	Clock relogio;

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
	RodadaRepositorio rodadas;

	@Autowired
	JdbcClient jdbc;

	Usuario treinador;
	long jogoId;
	String rodada;
	String plantel;

	@BeforeEach
	void jogoNovo() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao())
			.gerar(PaisPadrao.TODOS));
		long brasil = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow().paisId();
		treinador = contas.criarAdministrador("r" + System.nanoTime() + "@exemplo.com", "Gabriel", "senha-boa-1");
		jogoId = jogos.criarComSorteio(brasil, treinador.id(), 31).jogoId();
		rodada = "/jogos/" + jogoId + "/rodada";
		plantel = "/jogos/" + jogoId + "/plantel";
		agora(0);
	}

	@Test
	void confirmarAbreAQuadroQueAndaPeloRelogio() throws Exception {
		mvc.perform(post(rodada).param("rodada", "1").with(como(treinador)).with(csrf())).andExpect(redirectedUrl(rodada));

		agora(10);
		mvc.perform(get(rodada).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Rodada 1")))
			.andExpect(content().string(containsString("20&#39;")))
			.andExpect(content().string(containsString("do-usuario")))
			.andExpect(content().string(containsString("Velocidade da partida")))
			.andExpect(content().string(containsString("every 1s")));
		// durante a rodada o plantel leva ao quadro
		mvc.perform(get(plantel).with(como(treinador))).andExpect(redirectedUrl(rodada));
	}

	@Test
	void trocarVelocidadeGuardaAEscolha() throws Exception {
		mvc.perform(post(rodada).param("rodada", "1").with(como(treinador)).with(csrf()));

		mvc.perform(post(rodada + "/velocidade").param("velocidade", "RAPIDA").with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl(rodada));

		assertThat(jdbc.sql("SELECT velocidade FROM jogo_treinador WHERE jogo_id = :j").param("j", jogoId)
			.query(String.class).single()).isEqualTo("RAPIDA");
	}

	@Test
	void fimDaRodadaVoltaAoPlantelComOPlacar() throws Exception {
		mvc.perform(post(rodada).param("rodada", "1").with(como(treinador)).with(csrf()));
		agora(3_600);

		// a consulta do HTMX recebe o redirecionamento por cabeçalho
		MockHttpSession sessao = new MockHttpSession();
		mvc.perform(get(rodada).header("HX-Request", "true").session(sessao).with(como(treinador)))
			.andExpect(header().string("HX-Redirect", plantel));
		mvc.perform(get(plantel).session(sessao).with(como(treinador)))
			.andExpect(content().string(containsString("Rodada 1: ")))
			.andExpect(content().string(containsString("Próximo jogo · Rodada 2")));
		assertThat(rodadas.rodadasProcessadas(jogoId)).isEqualTo(1);

		// sem rodada ao vivo, o quadro leva ao plantel; repetir o "jogar" da rodada 1 não joga outra
		mvc.perform(get(rodada).with(como(treinador))).andExpect(redirectedUrl(plantel));
		mvc.perform(post(rodada).param("rodada", "1").with(como(treinador)).with(csrf()))
			.andExpect(flash().attribute("erro", "A rodada 1 já foi jogada. Confira o plantel."));
		assertThat(rodadas.rodadasProcessadas(jogoId)).isEqualTo(1);
	}

	@Test
	void fimSemHtmxRedirecionaComOPlacar() throws Exception {
		mvc.perform(post(rodada).param("rodada", "1").with(como(treinador)).with(csrf()));
		agora(3_600);

		mvc.perform(get(rodada).with(como(treinador)))
			.andExpect(redirectedUrl(plantel))
			.andExpect(flash().attribute("resultado", containsString("Rodada 1: ")));
	}

	@Test
	void outroUsuarioNaoVeARodada() throws Exception {
		mvc.perform(post(rodada).param("rodada", "1").with(como(treinador)).with(csrf()));
		Usuario outro = contas.criarAdministrador("o" + System.nanoTime() + "@exemplo.com", "Outro", "senha-boa-1");

		mvc.perform(get(rodada).with(como(outro))).andExpect(status().isNotFound());
		mvc.perform(post(rodada + "/velocidade").param("velocidade", "LENTA").with(como(outro)).with(csrf()))
			.andExpect(status().isNotFound());
	}

	private void agora(long segundos) {
		when(relogio.instant()).thenReturn(T0.plusSeconds(segundos));
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
