package br.com.managerfoot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.contas.Papel;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.ContasRepositorio;

/** Entrar, cadastro pelo link e sair (SPEC §3.4, §10.5; docs/decisions/0009), com PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EntradaIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	ContasRepositorio contas;

	Usuario admin;

	@BeforeEach
	void criarAdministrador() {
		admin = contas.criarAdministrador(unico("admin"), "Dono", "senha-do-admin");
	}

	@Test
	void semEntrarVaiParaATelaDeEntrarMasHealthCheckFicaAberto() throws Exception {
		mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/entrar"));
		mvc.perform(get("/entrar")).andExpect(status().isOk()).andExpect(content().string(containsString("Entrar")));
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mvc.perform(get("/webjars/htmx.org/dist/htmx.min.js")).andExpect(status().isOk());
	}

	@Test
	void entraComEMailESenhaEmostraONome() throws Exception {
		MockHttpSession sessao = (MockHttpSession) mvc
			.perform(post("/entrar").param("email", admin.email().toUpperCase()).param("senha", "senha-do-admin").with(csrf()))
			.andExpect(redirectedUrl("/"))
			.andExpect(authenticated().withRoles("ADMINISTRADOR"))
			.andReturn().getRequest().getSession();

		mvc.perform(get("/").session(sessao)).andExpect(status().isOk()).andExpect(content().string(containsString("Dono · administrador")));
	}

	@Test
	void senhaErradaVoltaComErroSemDizerSeOEMailExiste() throws Exception {
		mvc.perform(post("/entrar").param("email", admin.email()).param("senha", "senha-errada").with(csrf()))
			.andExpect(redirectedUrl("/entrar?erro"))
			.andExpect(unauthenticated());
		mvc.perform(post("/entrar").param("email", unico("ninguem")).param("senha", "senha-errada").with(csrf()))
			.andExpect(redirectedUrl("/entrar?erro"));
		mvc.perform(get("/entrar").param("erro", "")).andExpect(content().string(containsString("E-mail ou senha incorretos.")));
	}

	@Test
	void entrarSemTokenCsrfERecusado() throws Exception {
		mvc.perform(post("/entrar").param("email", admin.email()).param("senha", "senha-do-admin"))
			.andExpect(status().isForbidden())
			.andExpect(unauthenticated());
	}

	@Test
	void sairEncerraASessao() throws Exception {
		MockHttpSession sessao = (MockHttpSession) mvc
			.perform(post("/entrar").param("email", admin.email()).param("senha", "senha-do-admin").with(csrf()))
			.andReturn().getRequest().getSession();

		mvc.perform(post("/sair").session(sessao).with(csrf())).andExpect(redirectedUrl("/entrar?saiu"));

		assertThat(sessao.isInvalid()).isTrue();
		mvc.perform(get("/")).andExpect(redirectedUrl("/entrar"));
	}

	@Test
	void cadastroPeloLinkCriaParticipanteEJaEntra() throws Exception {
		String convite = contas.gerarLinkCadastro(admin.id());
		mvc.perform(get("/cadastro").param("convite", convite))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("válido até")))
			.andExpect(header().string("Referrer-Policy", "same-origin"));

		String email = unico("novo");
		MockHttpSession sessao = (MockHttpSession) mvc
			.perform(post("/cadastro").with(csrf()).param("convite", convite).param("nome", "Gabriel")
				.param("email", email).param("senha", "senha-nova-1").param("senhaRepetida", "senha-nova-1"))
			.andExpect(redirectedUrl("/"))
			.andExpect(authenticated().withRoles("PARTICIPANTE"))
			.andReturn().getRequest().getSession();

		mvc.perform(get("/").session(sessao)).andExpect(content().string(containsString("Gabriel · participante")));
		assertThat(contas.autenticar(email, "senha-nova-1")).hasValueSatisfying(u -> assertThat(u.papel()).isEqualTo(Papel.PARTICIPANTE));
		// uso único: o mesmo link não abre outro cadastro
		mvc.perform(get("/cadastro").param("convite", convite))
			.andExpect(content().string(containsString("Este convite não vale mais")))
			.andExpect(content().string(not(containsString("Criar conta e entrar"))));
	}

	@Test
	void cadastroComErrosVoltaComAsMensagensNosCamposSemCriarConta() throws Exception {
		String convite = contas.gerarLinkCadastro(admin.id());

		mvc.perform(post("/cadastro").with(csrf()).param("convite", convite).param("nome", "Ana")
				.param("email", "ana-sem-arroba").param("senha", "1234").param("senhaRepetida", "1234"))
			.andExpect(status().isOk())
			.andExpect(unauthenticated())
			.andExpect(content().string(containsString("Informe um e-mail válido.")))
			.andExpect(content().string(containsString("A senha precisa ter pelo menos 8 caracteres.")))
			.andExpect(content().string(containsString("value=\"Ana\"")));

		mvc.perform(post("/cadastro").with(csrf()).param("convite", convite).param("nome", "Ana")
				.param("email", unico("ana")).param("senha", "senha-boa-1").param("senhaRepetida", "senha-boa-2"))
			.andExpect(content().string(containsString("As senhas não conferem.")));

		mvc.perform(post("/cadastro").with(csrf()).param("convite", convite).param("nome", "Ana")
				.param("email", admin.email()).param("senha", "senha-boa-1").param("senhaRepetida", "senha-boa-1"))
			.andExpect(content().string(containsString("Já existe uma conta com este e-mail.")));

		// nenhum dos erros gastou o link
		assertThat(contas.validadeDoLink(convite)).isPresent();
	}

	@Test
	void conviteInexistenteNaoMostraOFormulario() throws Exception {
		mvc.perform(get("/cadastro").param("convite", "token-que-nao-existe"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Este convite não vale mais. Peça um novo ao administrador.")))
			.andExpect(content().string(not(containsString("Criar conta e entrar"))));
		mvc.perform(post("/cadastro").with(csrf()).param("convite", "token-que-nao-existe").param("nome", "Ana")
				.param("email", unico("ana")).param("senha", "senha-boa-1").param("senhaRepetida", "senha-boa-1"))
			.andExpect(unauthenticated())
			.andExpect(content().string(containsString("Este convite não vale mais")));
	}

	private static String unico(String prefixo) {
		return prefixo + System.nanoTime() + "@exemplo.com";
	}
}
