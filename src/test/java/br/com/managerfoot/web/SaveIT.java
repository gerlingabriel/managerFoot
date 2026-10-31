package br.com.managerfoot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;
import br.com.managerfoot.persistencia.SaveRepositorio.ArquivoDeSave;

/** Baixar, enviar e apagar um jogo pelas rotas (SPEC §3.5). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SaveIT {

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

	Usuario treinador;
	Usuario outro;
	long jogo;

	@BeforeEach
	void criarJogo() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());
		CampeonatoDisponivel brasil = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow();
		String admin = "admin" + System.nanoTime() + "@exemplo.com";
		long adminId = contas.criarAdministrador(admin, "Dono", "senha-do-admin").id();
		treinador = contas.cadastrar(contas.gerarLinkCadastro(adminId), "t" + System.nanoTime() + "@exemplo.com", "Treinador", "senha-boa-1");
		outro = contas.cadastrar(contas.gerarLinkCadastro(adminId), "o" + System.nanoTime() + "@exemplo.com", "Outro", "senha-boa-2");
		jogo = jogos.criarComSorteio(brasil.paisId(), treinador.id(), 3).jogoId();
	}

	@Test
	void baixarEnviarDeVoltaEApagar() throws Exception {
		byte[] arquivo = mvc.perform(get("/jogos/" + jogo + "/arquivo").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", containsString("managerfoot-")))
			.andReturn().getResponse().getContentAsByteArray();

		mvc.perform(multipart("/jogos/arquivo").file(new MockMultipartFile("arquivo", "jogo.json", "application/json", arquivo))
			.with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attributeExists("resultado"));
		assertThat(jogos.listar(treinador.id())).hasSize(2);

		mvc.perform(post("/jogos/" + jogo + "/apagar").with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attributeExists("resultado"));
		assertThat(jogos.listar(treinador.id())).singleElement().satisfies(j -> assertThat(j.jogoId()).isNotEqualTo(jogo));
	}

	@Test
	void arquivoRuimVoltaComAMensagem() throws Exception {
		byte[] ruim = "nao e um jogo".getBytes(StandardCharsets.UTF_8);
		mvc.perform(multipart("/jogos/arquivo").file(new MockMultipartFile("arquivo", "x.json", "application/json", ruim))
			.with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attribute("erro", containsString("não é um jogo salvo")));
		assertThat(jogos.listar(treinador.id())).hasSize(1);
	}

	@Test
	void outroTreinadorNaoBaixaNemApaga() throws Exception {
		mvc.perform(get("/jogos/" + jogo + "/arquivo").with(como(outro))).andExpect(status().isNotFound());
		mvc.perform(post("/jogos/" + jogo + "/apagar").with(como(outro)).with(csrf())).andExpect(status().isNotFound());
		assertThat(jogos.listar(treinador.id())).hasSize(1);
	}

	@Test
	void apagarExigeOTokenDoFormulario() throws Exception {
		mvc.perform(post("/jogos/" + jogo + "/apagar").with(como(treinador))).andExpect(status().isForbidden());
		assertThat(jogos.listar(treinador.id())).hasSize(1);
	}

	@Test
	void nomeDoArquivoSoComLetrasSimples() {
		assertThat(SaveController.nomeDoArquivo(new ArquivoDeSave("São João de Cá!", 2027, 5, new byte[0])))
			.isEqualTo("managerfoot-sao-joao-de-ca-2027-rodada-5.json");
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
