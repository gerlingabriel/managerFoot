package br.com.managerfoot.web;

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

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.economia.PropostasPatrocinio;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.LigaDeTeste;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.PatrocinioRepositorio;
import br.com.managerfoot.persistencia.SalaRepositorio;

/** OK de todos no jogo em rede pelas telas (SPEC §3.3 itens 4 e 5, rev. 92; modelo 18e). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TurnoIT {

	private static final Instant AGORA = Instant.parse("2026-11-03T20:00:00Z");

	@Autowired
	MockMvc mvc;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	JogosDoTreinadorRepositorio jogosDoTreinador;

	@Autowired
	SalaRepositorio salas;

	@Autowired
	PatrocinioRepositorio patrocinios;

	Usuario admin;
	Usuario marcos;
	long jogo;

	@BeforeEach
	void jogoEmRedeComecado() {
		long pais = LigaDeTeste.criarPais(moldes);
		admin = LigaDeTeste.administrador(contas);
		marcos = LigaDeTeste.participante(contas);
		jogo = jogosDoTreinador.criarEmRedeComSorteio(pais, admin.id(), AGORA).jogoId();
		salas.entrar(salas.visao(jogo, admin.id()).orElseThrow().convite().orElseThrow().token(), marcos.id(), AGORA);
		salas.comecar(jogo, admin.id(), AGORA, Duration.ofSeconds(30));
	}

	@Test
	void okSoComAsDecisoesDoProprioTreinadorEDepoisAFaixaAguarda() throws Exception {
		mvc.perform(post("/jogos/" + jogo + "/rodada").param("rodada", "1").with(como(admin)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/plantel"))
			.andExpect(flash().attribute("erro", containsString("patrocínio")));
		patrocinios.assinar(jogo, admin.id(), PropostasPatrocinio.Tipo.FIXA);

		// o patrocínio de Marcos ainda falta, mas não impede o OK do administrador
		mvc.perform(post("/jogos/" + jogo + "/rodada").param("rodada", "1").with(como(admin)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/plantel"));
		mvc.perform(get("/jogos/" + jogo + "/plantel").with(como(admin)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("OK dado · rodada 1")))
			.andExpect(content().string(containsString("Aguardando " + marcos.nome())))
			.andExpect(content().string(containsString("Tirar meu OK e mexer")))
			.andExpect(content().string(not(containsString("Confirmar e jogar rodada"))));
		mvc.perform(get("/jogos/" + jogo + "/plantel").with(como(marcos)))
			.andExpect(content().string(containsString("1 de 2 com OK")))
			.andExpect(content().string(containsString("Confirmar e jogar rodada")));

		// a faixa: no mesmo momento, só ela; com o momento mudado, a tela vai para o plantel
		mvc.perform(get("/jogos/" + jogo + "/turno").param("momento", "t1/r1/tatica").param("tela", "plantel")
			.with(como(marcos)).header("HX-Request", "true"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("id=\"turno\"")));
		mvc.perform(get("/jogos/" + jogo + "/turno").param("momento", "t1/r0/tatica").param("tela", "plantel")
			.with(como(marcos)).header("HX-Request", "true"))
			.andExpect(header().string("HX-Redirect", "/jogos/" + jogo + "/plantel"));

		mvc.perform(post("/jogos/" + jogo + "/ok/tirar").with(como(admin)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/plantel"));
		mvc.perform(get("/jogos/" + jogo + "/plantel").with(como(admin)))
			.andExpect(content().string(containsString("Confirmar e jogar rodada")));
	}

	@Test
	void oUltimoOkFazOJogoSeguirParaTodos() throws Exception {
		patrocinios.assinar(jogo, admin.id(), PropostasPatrocinio.Tipo.FIXA);
		patrocinios.assinar(jogo, marcos.id(), PropostasPatrocinio.Tipo.FIXA);
		mvc.perform(post("/jogos/" + jogo + "/rodada").param("rodada", "1").with(como(admin)).with(csrf()));

		mvc.perform(post("/jogos/" + jogo + "/rodada").param("rodada", "1").with(como(marcos)).with(csrf()))
			.andExpect(redirectedUrlPattern("/jogos/" + jogo + "/{tela:rodada|leiloes}"));
		mvc.perform(get("/jogos/" + jogo + "/turno").param("momento", "t1/r1/tatica").param("tela", "plantel")
			.with(como(admin)).header("HX-Request", "true"))
			.andExpect(header().string("HX-Redirect", "/jogos/" + jogo + "/plantel"));
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
