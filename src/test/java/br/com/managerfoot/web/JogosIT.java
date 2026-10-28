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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;

/** Início, novo jogo por sorteio e abrir jogo (SPEC §10.5, rev. 29), com o banco padrão carregado. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class JogosIT {

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

	Usuario participante;
	CampeonatoDisponivel brasil;

	@BeforeEach
	void carregarBancoPadraoECriarParticipante() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao())
			.gerar(PaisPadrao.TODOS));
		List<CampeonatoDisponivel> campeonatos = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow());
		brasil = campeonatos.stream().filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow();
		String admin = "admin" + System.nanoTime() + "@exemplo.com";
		String token = contas.gerarLinkCadastro(contas.criarAdministrador(admin, "Dono", "senha-do-admin").id());
		participante = contas.cadastrar(token, "p" + System.nanoTime() + "@exemplo.com", "Gabriel", "senha-boa-1");
	}

	@Test
	void inicioSemJogosConvidaAComecar() throws Exception {
		mvc.perform(get("/").with(como(participante)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Gabriel · participante")))
			.andExpect(content().string(containsString("Você ainda não tem jogos. Comece um novo.")));
	}

	@Test
	void novoJogoMostraOsCampeonatosDoBancoPadrao() throws Exception {
		mvc.perform(get("/jogos/novo").with(como(participante)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Brasil")))
			.andExpect(content().string(containsString("Portugal")))
			.andExpect(content().string(containsString("Sortear clube e começar")));
	}

	@Test
	void sortearCriaOJogoMostraOClubeEAListaPassaATerOJogo() throws Exception {
		MvcResult criado = mvc.perform(post("/jogos").with(como(participante)).with(csrf())
				.param("paisId", String.valueOf(brasil.paisId())))
			.andExpect(redirectedUrlPattern("/jogos/*"))
			.andExpect(flash().attributeExists("sorteadoEntre"))
			.andReturn();
		String url = criado.getResponse().getRedirectedUrl();

		mvc.perform(get(url).with(como(participante)).flashAttrs(criado.getFlashMap()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Você é o treinador do")))
			.andExpect(content().string(containsString("sorteado entre")));

		assertThat(jogos.listar(participante.id())).hasSize(1);
		mvc.perform(get("/").with(como(participante)))
			.andExpect(content().string(containsString("Brasil · ")))
			.andExpect(content().string(containsString("temporada 2026, rodada 1")))
			.andExpect(content().string(containsString("href=\"" + url + "/plantel\"")))
			.andExpect(content().string(not(containsString("Você ainda não tem jogos"))));
	}

	@Test
	void jogoDeOutroUsuarioNaoExisteParaQuemPede() throws Exception {
		long jogo = jogos.criarComSorteio(brasil.paisId(), participante.id()).jogoId();
		Usuario outro = contas.criarAdministrador("outro" + System.nanoTime() + "@exemplo.com", "Outro", "senha-boa-1");

		mvc.perform(get("/jogos/" + jogo).with(como(outro))).andExpect(status().isNotFound());
		mvc.perform(get("/jogos/" + jogo).with(como(participante))).andExpect(status().isOk());
	}

	@Test
	void recusaCampeonatoForaDaListaESemCsrf() throws Exception {
		mvc.perform(post("/jogos").with(como(participante)).with(csrf()).param("paisId", "-1"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Escolha um dos campeonatos da lista.")));
		mvc.perform(post("/jogos").with(como(participante)).param("paisId", String.valueOf(brasil.paisId())))
			.andExpect(status().isForbidden());

		assertThat(jogos.listar(participante.id())).isEmpty();
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
