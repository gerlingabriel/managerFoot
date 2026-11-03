package br.com.managerfoot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;

/** Novo jogo com o banco editável pelas rotas (modelos 17a a 17c). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BancoEditavelWebIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	BancoPadraoRepositorio bancoPadrao;

	@Autowired
	JogosDoTreinadorRepositorio jogos;

	Usuario participante;

	@BeforeEach
	void preparar() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());
		String admin = "admin" + System.nanoTime() + "@exemplo.com";
		long adminId = contas.criarAdministrador(admin, "Dono", "senha-do-admin").id();
		participante = contas.cadastrar(contas.gerarLinkCadastro(adminId), "p" + System.nanoTime() + "@exemplo.com", "Gabriel", "senha-boa-1");
	}

	@Test
	void baixarEditarEnviarECriarOJogo() throws Exception {
		String banco = mvc.perform(get("/jogos/novo/banco").with(como(participante)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		String editado = banco.replace("\"Liga do Brasil\"", "\"Campeonato da Turma\"");
		assertThat(editado).isNotEqualTo(banco);
		MockHttpSession sessao = new MockHttpSession();

		mvc.perform(multipart("/jogos/novo/banco").file(arquivo(editado)).session(sessao).with(como(participante)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/novo"))
			.andExpect(flash().attribute("resultado", containsString("Arquivo aceito: 6 campeonatos")));
		mvc.perform(get("/jogos/novo").session(sessao).with(como(participante)))
			.andExpect(content().string(containsString("Campeonato da Turma")))
			.andExpect(content().string(containsString("name=\"paisCodigo\"")))
			.andExpect(content().string(containsString("Trocar arquivo")));

		mvc.perform(post("/jogos").session(sessao).param("paisCodigo", "BRA").with(como(participante)).with(csrf()))
			.andExpect(redirectedUrlPattern("/jogos/*"));

		assertThat(jogos.listar(participante.id())).singleElement().satisfies(j -> assertThat(j.pais()).isEqualTo("Brasil"));
		assertThat(sessao.getAttribute(BancoEditavelController.SESSAO)).isNull();
		mvc.perform(get("/jogos/novo").session(sessao).with(como(participante)))
			.andExpect(content().string(containsString("Liga do Brasil")))
			.andExpect(content().string(not(containsString("Campeonato da Turma"))));
	}

	@Test
	void administradorEscolheQualquerClubeDoArquivo() throws Exception {
		Usuario admin = contas.criarAdministrador("adm" + System.nanoTime() + "@exemplo.com", "Admin", "senha-do-admin");
		String banco = mvc.perform(get("/jogos/novo/banco").with(como(admin))).andReturn().getResponse()
			.getContentAsString(StandardCharsets.UTF_8);
		String nome = new tools.jackson.databind.json.JsonMapper().readTree(banco).path("paises").get(0).path("clubes").get(0)
			.path("nome").asString();
		String editado = banco.replace("\"" + nome + "\"", "\"Clube da Turma\"");
		MockHttpSession sessao = new MockHttpSession();
		mvc.perform(multipart("/jogos/novo/banco").file(arquivo(editado)).session(sessao).with(como(admin)).with(csrf()));
		mvc.perform(get("/jogos/novo").session(sessao).with(como(admin)))
			.andExpect(content().string(containsString("Escolher clube")));

		mvc.perform(post("/jogos/escolher").session(sessao).param("paisCodigo", "BRA").with(como(admin)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/escolher"));
		mvc.perform(get("/jogos/escolher").session(sessao).param("clube", "1").with(como(admin)))
			.andExpect(content().string(containsString("Clube da Turma")))
			.andExpect(content().string(containsString("Assumir Clube da Turma")));
		mvc.perform(post("/jogos/escolher/assumir").session(sessao).param("clube", "1").with(como(admin)).with(csrf()))
			.andExpect(redirectedUrlPattern("/jogos/*"));

		assertThat(jogos.listar(admin.id())).singleElement().satisfies(j -> assertThat(j.clube().nome()).isEqualTo("Clube da Turma"));
		assertThat(sessao.getAttribute(BancoEditavelController.SESSAO)).isNull();
	}

	@Test
	void arquivoComErroMostraOsProblemasEContinuaNoPadrao() throws Exception {
		MockHttpSession sessao = new MockHttpSession();
		mvc.perform(multipart("/jogos/novo/banco").file(arquivo("{\"aplicacao\":\"managerFoot\",\"tipo\":\"banco\",\"formato\":1,\"paises\":[]}"))
			.session(sessao).with(como(participante)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/novo"))
			.andExpect(flash().attributeExists("problemas"));
		assertThat(sessao.getAttribute(BancoEditavelController.SESSAO)).isNull();
	}

	@Test
	void voltarAoBancoPadrao() throws Exception {
		String banco = mvc.perform(get("/jogos/novo/banco").with(como(participante))).andReturn().getResponse()
			.getContentAsString(StandardCharsets.UTF_8);
		MockHttpSession sessao = new MockHttpSession();
		mvc.perform(multipart("/jogos/novo/banco").file(arquivo(banco)).session(sessao).with(como(participante)).with(csrf()));
		assertThat(sessao.getAttribute(BancoEditavelController.SESSAO)).isNotNull();

		mvc.perform(post("/jogos/novo/banco/padrao").session(sessao).with(como(participante)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/novo"));

		assertThat(sessao.getAttribute(BancoEditavelController.SESSAO)).isNull();
	}

	private static MockMultipartFile arquivo(String conteudo) {
		return new MockMultipartFile("arquivo", "meu-banco.json", "application/json", conteudo.getBytes(StandardCharsets.UTF_8));
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
