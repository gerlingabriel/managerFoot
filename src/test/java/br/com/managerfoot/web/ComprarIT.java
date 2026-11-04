package br.com.managerfoot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.LigaDeTeste;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.TemporadaRepositorio;

/** Aba "Comprar" e "Convidar pessoas" do administrador (modelos 20b e 20c). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ComprarIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	TemporadaRepositorio temporadas;

	long jogo;
	Usuario treinador;
	long goleiroAVenda;
	long atacanteAVenda;

	@BeforeEach
	void jogoComDoisAVenda() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		long usuario = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = ?").param(jogo).query(Long.class).single();
		treinador = contas.buscar(usuario).orElseThrow();
		long meu = jogos.clubeDoTreinador(jogo, usuario).orElseThrow();
		jdbc.sql("UPDATE jogo_jogador j SET a_venda = false FROM jogo_clube c WHERE c.id = j.clube_id AND c.jogo_id = ?").param(jogo).update();
		goleiroAVenda = umDe("GOLEIRO", meu);
		atacanteAVenda = umDe("ATACANTE", meu);
		jdbc.sql("UPDATE jogo_jogador SET a_venda = true, forca = 5 WHERE id IN (?, ?)").param(goleiroAVenda).param(atacanteAVenda).update();
	}

	@Test
	void listaOsAVendaComFiltrosGuardadosECompraVoltaParaALista() throws Exception {
		MockHttpSession sessao = new MockHttpSession();
		String goleiro = nome(goleiroAVenda);
		String atacante = nome(atacanteAVenda);

		mvc.perform(get("/jogos/" + jogo + "/comprar").with(como(treinador)).session(sessao))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("2 jogadores")))
			.andExpect(content().string(containsString(goleiro)))
			.andExpect(content().string(containsString(atacante)));
		mvc.perform(get("/jogos/" + jogo + "/comprar").param("filtrar", "1").param("pos", "GOLEIRO").param("cabe", "1")
			.with(como(treinador)).session(sessao))
			.andExpect(content().string(containsString(goleiro)))
			.andExpect(content().string(not(containsString(atacante))));
		// sem o formulário, valem os filtros guardados
		mvc.perform(get("/jogos/" + jogo + "/comprar").with(como(treinador)).session(sessao))
			.andExpect(content().string(not(containsString(atacante))));
		mvc.perform(get("/jogos/" + jogo + "/comprar").param("filtrar", "1").param("forcaDe", "6").with(como(treinador)).session(sessao))
			.andExpect(content().string(containsString("Nenhum jogador à venda com estes filtros")));

		long vendedor = jdbc.sql("SELECT clube_id FROM jogo_jogador WHERE id = ?").param(goleiroAVenda).query(Long.class).single();
		mvc.perform(post("/jogos/" + jogo + "/mercado/comprar").param("jogador", String.valueOf(goleiroAVenda))
			.param("clube", String.valueOf(vendedor)).param("voltar", "comprar").with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/comprar"))
			.andExpect(flash().attribute("resultado", containsString("comprado")));
		assertThat(jogos.clubeDoTreinador(jogo, treinador.id()).orElseThrow())
			.isEqualTo(jdbc.sql("SELECT clube_id FROM jogo_jogador WHERE id = ?").param(goleiroAVenda).query(Long.class).single());
	}

	@Test
	void soOAdministradorGeraLinkDeCadastroNoInicio() throws Exception {
		Usuario admin = contas.criarAdministrador("admin" + System.nanoTime() + "@exemplo.com", "Gabriel", "senha-do-admin");

		mvc.perform(get("/").with(como(admin)))
			.andExpect(content().string(containsString("Convidar pessoas")))
			.andExpect(content().string(containsString("Gerar link de cadastro")));
		mvc.perform(post("/convites/cadastro").with(como(admin)).with(csrf()))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attribute("linkCadastro", startsWith("http://localhost:8080/cadastro?convite=")));

		mvc.perform(get("/").with(como(treinador)))
			.andExpect(content().string(not(containsString("Convidar pessoas"))));
		mvc.perform(post("/convites/cadastro").with(como(treinador)).with(csrf()))
			.andExpect(status().isForbidden());
	}

	private long umDe(String posicao, long meu) {
		return jdbc.sql("""
				SELECT j.id FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id
				WHERE c.jogo_id = ? AND c.id <> ? AND c.divisao_id IS NOT NULL AND j.posicao = ? ORDER BY j.id LIMIT 1""")
			.param(jogo).param(meu).param(posicao).query(Long.class).single();
	}

	private String nome(long jogador) {
		return jdbc.sql("SELECT nome FROM jogo_jogador WHERE id = ?").param(jogador).query(String.class).single();
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
