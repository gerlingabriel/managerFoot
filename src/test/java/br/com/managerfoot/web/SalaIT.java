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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;

/** Jogo em rede pelas telas: novo jogo, sala, convite e o início (SPEC §3.3, rev. 92; modelos 18a a 18d). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SalaIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	BancoPadraoRepositorio bancoPadrao;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	JdbcClient jdbc;

	Usuario admin;
	Usuario marcos;
	String emailMarcos;
	CampeonatoDisponivel brasil;

	@BeforeEach
	void carregarBancoECriarContas() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());
		List<CampeonatoDisponivel> campeonatos = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow());
		brasil = campeonatos.stream().filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow();
		admin = contas.criarAdministrador("admin" + System.nanoTime() + "@exemplo.com", "Gabriel", "senha-do-admin");
		emailMarcos = "marcos" + System.nanoTime() + "@exemplo.com";
		marcos = contas.cadastrar(contas.gerarLinkCadastro(admin.id()), emailMarcos, "Marcos", "senha-boa-1");
	}

	@Test
	void administradorCriaEmRedeEVaiParaASalaComOLink() throws Exception {
		long jogo = criarEmRede();

		mvc.perform(get("/jogos/" + jogo + "/sala").with(como(admin)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Sala do jogo em rede")))
			.andExpect(content().string(containsString("/convite/" + token(jogo))))
			.andExpect(content().string(containsString("Modo sorteio · 2 divisões")))
			.andExpect(content().string(containsString("1 treinador · 1 conectado")))
			.andExpect(content().string(containsString("Começar a temporada")));
		mvc.perform(get("/jogos/" + jogo + "/sala").with(como(marcos))).andExpect(status().isNotFound());
		// a consulta da tela (HTMX) devolve só o corpo da sala
		mvc.perform(get("/jogos/" + jogo + "/sala").with(como(admin)).header("HX-Request", "true"))
			.andExpect(status().isOk())
			.andExpect(content().string(not(containsString("<html"))))
			.andExpect(content().string(containsString("id=\"sala\"")));
	}

	@Test
	void conviteSemContaPedeEntrarEVoltaAoConvite() throws Exception {
		long jogo = criarEmRede();
		String token = token(jogo);

		mvc.perform(get("/convite/" + token))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Convite de Gabriel")))
			.andExpect(content().string(containsString("name=\"convite\"")));
		mvc.perform(post("/entrar").param("email", emailMarcos).param("senha", "senha-boa-1").param("convite", token).with(csrf()))
			.andExpect(redirectedUrl("/convite/" + token));
		mvc.perform(post("/entrar").param("email", emailMarcos).param("senha", "errada-123").param("convite", token).with(csrf()))
			.andExpect(redirectedUrl("/convite/" + token + "?erro"));
		// convite fora do formato não vira endereço de volta
		mvc.perform(post("/entrar").param("email", emailMarcos).param("senha", "senha-boa-1").param("convite", "//outro.site").with(csrf()))
			.andExpect(redirectedUrl("/"));
		mvc.perform(get("/convite/nao-existe-este-token-de-convite")).andExpect(status().isNotFound());
	}

	@Test
	void participanteEntraPeloLinkVeASalaENaoJogaNemApagaNemBaixa() throws Exception {
		long jogo = criarEmRede();
		String token = token(jogo);

		mvc.perform(get("/convite/" + token).with(como(marcos)))
			.andExpect(content().string(containsString("Gabriel chamou você")))
			.andExpect(content().string(containsString("Entrar e receber o clube sorteado")))
			.andExpect(content().string(containsString("entre os 5 mais fracos")));
		mvc.perform(post("/convite/" + token + "/entrar").with(como(marcos)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/sala"));
		mvc.perform(get("/convite/" + token).with(como(marcos))).andExpect(redirectedUrl("/jogos/" + jogo + "/sala"));

		mvc.perform(get("/jogos/" + jogo + "/sala").with(como(marcos)))
			.andExpect(content().string(containsString("Aguardando Gabriel começar a temporada.")))
			.andExpect(content().string(not(containsString("/convite/"))))
			.andExpect(content().string(not(containsString("Começar a temporada"))));
		mvc.perform(get("/").with(como(marcos)))
			.andExpect(content().string(containsString("Em rede")))
			.andExpect(content().string(containsString("Sala de Gabriel · Brasil · aguardando o início da temporada")))
			.andExpect(content().string(containsString("Abrir a sala")))
			.andExpect(content().string(not(containsString("Baixar arquivo"))))
			.andExpect(content().string(not(containsString("Apagar"))));
		mvc.perform(get("/").with(como(admin))).andExpect(content().string(containsString("Apagar")));

		mvc.perform(get("/jogos/" + jogo + "/plantel").with(como(marcos)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Voltar à sala")));
		mvc.perform(post("/jogos/" + jogo + "/rodada").param("rodada", "1").with(como(marcos)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/plantel"))
			.andExpect(flash().attribute("erro", containsString("ainda não começou")));
		mvc.perform(post("/jogos/" + jogo + "/sala/comecar").with(como(marcos)).with(csrf())).andExpect(status().isForbidden());
		mvc.perform(post("/jogos/" + jogo + "/apagar").with(como(marcos)).with(csrf()));
		assertThat(jdbc.sql("SELECT count(*) FROM jogo WHERE id = :jogo").param("jogo", jogo).query(Long.class).single()).isOne();
	}

	@Test
	void administradorComecaETodosVaoParaOPlantel() throws Exception {
		long jogo = criarEmRede();
		mvc.perform(post("/convite/" + token(jogo) + "/entrar").with(como(marcos)).with(csrf()));

		mvc.perform(post("/jogos/" + jogo + "/sala/comecar").with(como(admin)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/plantel"));
		// quem ainda estava na sala é levado ao plantel na próxima consulta
		mvc.perform(get("/jogos/" + jogo + "/sala").with(como(marcos)).header("HX-Request", "true"))
			.andExpect(header().string("HX-Redirect", "/jogos/" + jogo + "/plantel"));
		mvc.perform(post("/jogos/" + jogo + "/sala/convite").with(como(admin)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/sala"))
			.andExpect(flash().attribute("erro", containsString("já começou")));
	}

	@Test
	void modoEscolhaAbreOQuadroDoJogoParaQuemEntra() throws Exception {
		MockHttpSession sessao = new MockHttpSession();
		mvc.perform(post("/jogos/escolher").param("paisId", String.valueOf(brasil.paisId())).param("emRede", "true")
			.session(sessao).with(como(admin)).with(csrf()));
		// o quadro do administrador; o primeiro clube do quadro serve
		String quadro = mvc.perform(get("/jogos/escolher").session(sessao).with(como(admin))).andReturn().getResponse()
			.getContentAsString();
		String clube = quadro.replaceAll("(?s).*?/jogos/escolher\\?clube=(\\d+).*", "$1");
		String destino = mvc.perform(post("/jogos/escolher/assumir").param("clube", clube).session(sessao).with(como(admin))
			.with(csrf()))
			.andExpect(redirectedUrlPattern("/jogos/*/sala"))
			.andReturn().getResponse().getRedirectedUrl();
		long jogo = Long.parseLong(destino.replaceAll("\\D", ""));

		mvc.perform(post("/convite/" + token(jogo) + "/entrar").with(como(marcos)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/sala/escolher"));
		long doAdmin = jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo").param("jogo", jogo).query(Long.class).single();
		mvc.perform(get("/jogos/" + jogo + "/sala/escolher").with(como(marcos)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Sala de Gabriel · Brasil")))
			.andExpect(content().string(containsString("já tem treinador")))
			.andExpect(content().string(not(containsString("/jogos/" + jogo + "/sala/escolher?clube=" + doAdmin + "\""))));
		mvc.perform(post("/jogos/" + jogo + "/sala/escolher/assumir").param("clube", String.valueOf(doAdmin)).with(como(marcos)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/sala/escolher"))
			.andExpect(flash().attribute("erro", containsString("já tem treinador")));
		mvc.perform(get("/").with(como(marcos))).andExpect(content().string(containsString("você ainda não escolheu o clube")));
	}

	private long criarEmRede() throws Exception {
		String destino = mvc.perform(post("/jogos").param("paisId", String.valueOf(brasil.paisId())).param("emRede", "true")
			.with(como(admin)).with(csrf()))
			.andExpect(redirectedUrlPattern("/jogos/*/sala"))
			.andReturn().getResponse().getRedirectedUrl();
		return Long.parseLong(destino.replaceAll("\\D", ""));
	}

	private String token(long jogo) {
		return jdbc.sql("SELECT token FROM jogo_rede_convite WHERE jogo_id = :jogo AND trocado_em IS NULL")
			.param("jogo", jogo).query(String.class).single();
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
