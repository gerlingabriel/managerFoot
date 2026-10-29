package br.com.managerfoot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;
import br.com.managerfoot.temporada.ConfiguracaoLiga;

/** Caminho "Escolher clube" do administrador (SPEC §10.5, rev. 28 e 29), com o banco padrão. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EscolhaClubeIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	BancoPadraoRepositorio bancoPadrao;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	JogoRepositorio jogo;

	@Autowired
	JogosDoTreinadorRepositorio jogos;

	Usuario admin;
	Usuario participante;
	CampeonatoDisponivel brasil;

	@BeforeEach
	void carregarBancoPadraoECriarUsuarios() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao())
			.gerarBanco());
		brasil = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow();
		admin = contas.criarAdministrador("admin" + System.nanoTime() + "@exemplo.com", "Dono", "senha-do-admin");
		String token = contas.gerarLinkCadastro(admin.id());
		participante = contas.cadastrar(token, "p" + System.nanoTime() + "@exemplo.com", "Ana", "senha-boa-1");
	}

	@Test
	void soOAdministradorVeOsDoisCaminhosEChegaAoQuadro() throws Exception {
		mvc.perform(get("/jogos/novo").with(como(admin)))
			.andExpect(content().string(containsString("Como começar")))
			.andExpect(content().string(containsString("Escolher clube")));
		mvc.perform(get("/jogos/novo").with(como(participante)))
			.andExpect(content().string(containsString("Seu clube")))
			.andExpect(content().string(not(containsString("Escolher clube"))));

		mvc.perform(post("/jogos/escolher").with(como(participante)).with(csrf()).param("paisId", id()))
			.andExpect(status().isForbidden());
		mvc.perform(get("/jogos/escolher").with(como(participante))).andExpect(status().isForbidden());
		mvc.perform(post("/jogos/escolher/assumir").with(como(participante)).with(csrf()).param("clube", "1"))
			.andExpect(status().isForbidden());
	}

	@Test
	void quadroMostraADistribuicaoEOPlantelDoClubeClicado() throws Exception {
		MockHttpSession sessao = abrirQuadro();
		ConfiguracaoLiga liga = distribuicao(sessao);
		Clube clube = liga.divisoes().getFirst().clubes().getFirst();

		mvc.perform(get("/jogos/escolher").session(sessao).with(como(admin)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Escolha seu clube")))
			.andExpect(content().string(containsString(liga.divisoes().getLast().nome())))
			.andExpect(content().string(containsString("Clique num clube para ver o plantel.")))
			.andExpect(content().string(not(containsString("Assumir"))));

		mvc.perform(get("/jogos/escolher").param("clube", String.valueOf(clube.id())).session(sessao).with(como(admin)))
			.andExpect(content().string(containsString("Assumir " + clube.nome())))
			.andExpect(content().string(containsString("força " + clube.forca())));

		// clube de fora do quadro (ou da fila) não abre o plantel
		mvc.perform(get("/jogos/escolher").param("clube", "-5").session(sessao).with(como(admin)))
			.andExpect(content().string(not(containsString("Assumir"))));
	}

	@Test
	void assumirCriaOJogoComOClubeEscolhidoUmaVezSo() throws Exception {
		MockHttpSession sessao = abrirQuadro();
		Clube clube = distribuicao(sessao).divisoes().get(1).clubes().getFirst();

		String url = mvc.perform(post("/jogos/escolher/assumir").session(sessao).with(como(admin)).with(csrf())
				.param("clube", String.valueOf(clube.id())))
			.andExpect(redirectedUrlPattern("/jogos/*"))
			.andReturn().getResponse().getRedirectedUrl();

		mvc.perform(get(url).with(como(admin)))
			.andExpect(content().string(containsString("Você é o treinador do " + clube.nome())));
		// segundo clique: a semente já foi usada, nenhum jogo novo
		mvc.perform(post("/jogos/escolher/assumir").session(sessao).with(como(admin)).with(csrf())
				.param("clube", String.valueOf(clube.id())))
			.andExpect(redirectedUrl("/"));
		assertThat(jogos.listar(admin.id())).singleElement()
			.satisfies(j -> assertThat(j.clube().nome()).isEqualTo(clube.nome()));
	}

	@Test
	void semQuadroAbertoVoltaAoNovoJogo() throws Exception {
		mvc.perform(get("/jogos/escolher").with(como(admin))).andExpect(redirectedUrl("/jogos/novo"));
		mvc.perform(post("/jogos/escolher").with(como(admin)).with(csrf()).param("paisId", "-1"))
			.andExpect(redirectedUrl("/jogos/novo"));
	}

	private MockHttpSession abrirQuadro() throws Exception {
		MockHttpSession sessao = new MockHttpSession();
		mvc.perform(post("/jogos/escolher").session(sessao).with(como(admin)).with(csrf()).param("paisId", id()))
			.andExpect(redirectedUrl("/jogos/escolher"));
		return sessao;
	}

	private ConfiguracaoLiga distribuicao(MockHttpSession sessao) {
		EscolhaClubeController.Quadro quadro = (EscolhaClubeController.Quadro) sessao
			.getAttribute(EscolhaClubeController.SESSAO);
		return jogo.distribuicao(quadro.paisId(), quadro.semente());
	}

	private String id() {
		return String.valueOf(brasil.paisId());
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
