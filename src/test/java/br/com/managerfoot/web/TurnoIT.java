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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.economia.PropostasPatrocinio;
import br.com.managerfoot.multiplayer.Turno.Passo;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.LigaDeTeste;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.PatrocinioRepositorio;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio;
import br.com.managerfoot.persistencia.SalaRepositorio;
import br.com.managerfoot.persistencia.TurnoRepositorio;
import br.com.managerfoot.persistencia.TurnoRepositorio.Desfecho;

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

	@Autowired
	TurnoRepositorio turnos;

	@Autowired
	RodadaAoVivoRepositorio aoVivo;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	Clock relogio;

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
		// a lista de jogos do início diz o que o jogo espera (modelo 18a)
		mvc.perform(get("/").with(como(marcos)))
			.andExpect(content().string(containsString("Aguardando o seu OK")));
		mvc.perform(get("/").with(como(admin)))
			.andExpect(content().string(containsString("Você deu OK · aguardando " + marcos.nome())));

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

	@Test
	void aSubstituicaoDeUmParaOJogoDeTodosComAContagem() throws Exception {
		patrocinios.assinar(jogo, admin.id(), PropostasPatrocinio.Tipo.FIXA);
		patrocinios.assinar(jogo, marcos.id(), PropostasPatrocinio.Tipo.FIXA);
		Instant agora = relogio.instant();
		turnos.darOk(jogo, admin.id(), Passo.PLANTEL, 0, agora);
		Desfecho desfecho = turnos.darOk(jogo, marcos.id(), Passo.PLANTEL, 0, agora);
		if (desfecho == Desfecho.LANCES) {
			// leilões com lance possível (sorteio): cada um dá os seus
			for (Usuario u : new Usuario[] { admin, marcos }) {
				if (!turnos.okDado(jogo, u.id())) {
					desfecho = turnos.darLances(jogo, u.id(), 1, Map.of(), agora);
				}
			}
		}
		if (desfecho == Desfecho.RESULTADO) {
			turnos.darOk(jogo, admin.id(), Passo.RESULTADO, 0, agora);
			desfecho = turnos.darOk(jogo, marcos.id(), Passo.RESULTADO, 0, agora);
		}
		assertThat(desfecho).isEqualTo(Desfecho.AO_VIVO);
		// o relógio parado pelo administrador no minuto 20 da divisão dele, com 60 s pela frente
		for (int divisao = 0; divisao < 10; divisao++) {
			jdbc.sql("""
					UPDATE jogo_rodada_ao_vivo SET progresso = :p, andando_desde = NULL, lesao_ate = 1e9, pausada_por = :admin,
					       pausa_ate = :ate WHERE jogo_id = :jogo""")
				.param("p", divisao * 90 + 20.0).param("admin", admin.id())
				.param("ate", OffsetDateTime.ofInstant(relogio.instant().plusSeconds(60), ZoneOffset.UTC)).param("jogo", jogo)
				.update();
			jdbc.sql("""
					INSERT INTO jogo_pausa_participante (jogo_id, usuario_id, desde) VALUES (:jogo, :admin, now())
					ON CONFLICT DO NOTHING""").param("jogo", jogo).param("admin", admin.id()).update();
			if (aoVivo.estado(jogo, admin.id(), relogio.instant()).orElseThrow().janelaQueAbre() != null) {
				break;
			}
		}

		mvc.perform(get("/jogos/" + jogo + "/rodada").with(como(marcos)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Jogo parado para todos: " + admin.nome())))
			.andExpect(content().string(containsString("hx-get=\"/jogos/" + jogo + "/rodada\"")));
		mvc.perform(get("/jogos/" + jogo + "/rodada").with(como(admin)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Substituição · ")))
			.andExpect(content().string(containsString("Os outros treinadores esperam por você")))
			.andExpect(content().string(not(containsString("hx-get=\"/jogos/" + jogo + "/rodada\""))));
		mvc.perform(get("/jogos/" + jogo + "/rodada/pausa").with(como(admin)).header("HX-Request", "true"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("id=\"pausa-contagem\"")));

		mvc.perform(post("/jogos/" + jogo + "/rodada/continuar").with(como(marcos)).with(csrf()));
		mvc.perform(get("/jogos/" + jogo + "/rodada/pausa").with(como(admin)).header("HX-Request", "true"))
			.andExpect(content().string(containsString("id=\"pausa-contagem\"")));
		mvc.perform(post("/jogos/" + jogo + "/rodada/continuar").with(como(admin)).with(csrf()));
		mvc.perform(get("/jogos/" + jogo + "/rodada/pausa").with(como(admin)).header("HX-Request", "true"))
			.andExpect(header().string("HX-Redirect", "/jogos/" + jogo + "/rodada"));
	}

	@Test
	void soQuemCriouOJogoVeTirarDoJogoEConfirmaNumaTelaAParte() throws Exception {
		patrocinios.assinar(jogo, admin.id(), PropostasPatrocinio.Tipo.FIXA);
		patrocinios.assinar(jogo, marcos.id(), PropostasPatrocinio.Tipo.FIXA);
		String link = "/jogos/" + jogo + "/rede/tirar?usuario=" + marcos.id();
		mvc.perform(get("/jogos/" + jogo + "/plantel").with(como(admin)))
			.andExpect(content().string(containsString("Tirar do jogo")));
		mvc.perform(get("/jogos/" + jogo + "/plantel").with(como(marcos)))
			.andExpect(content().string(not(containsString("Tirar do jogo"))));
		mvc.perform(get("/jogos/" + jogo + "/rede/tirar").param("usuario", String.valueOf(admin.id())).with(como(marcos)))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/plantel"));
		mvc.perform(get(link).with(como(admin)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Tirar " + marcos.nome() + " do jogo?")))
			.andExpect(content().string(containsString("passa a ser da máquina")));
		mvc.perform(post("/jogos/" + jogo + "/rede/tirar").param("usuario", String.valueOf(admin.id())).with(como(marcos)).with(csrf()))
			.andExpect(status().isForbidden());

		mvc.perform(post("/jogos/" + jogo + "/rede/tirar").param("usuario", String.valueOf(marcos.id())).with(como(admin)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/plantel"))
			.andExpect(flash().attribute("resultado", containsString("saiu do jogo")));
		mvc.perform(get("/jogos/" + jogo + "/plantel").with(como(marcos)))
			.andExpect(status().isNotFound());
		// a faixa aberta na tela de quem saiu leva ao início com o aviso
		mvc.perform(get("/jogos/" + jogo + "/turno").param("momento", "t1/r1/tatica").param("tela", "plantel")
			.with(como(marcos)).header("HX-Request", "true"))
			.andExpect(header().string("HX-Redirect", "/"));
	}

	@Test
	void avisoDeQuemSaiuChegaATelaAondeOJogoSeguiu() throws Exception {
		patrocinios.assinar(jogo, admin.id(), PropostasPatrocinio.Tipo.FIXA);
		mvc.perform(post("/jogos/" + jogo + "/rodada").param("rodada", "1").with(como(admin)).with(csrf()))
			.andExpect(redirectedUrl("/jogos/" + jogo + "/plantel"));
		// só faltava Marcos: tirado, o jogo segue e o plantel manda o administrador adiante, com o aviso
		mvc.perform(post("/jogos/" + jogo + "/rede/tirar").param("usuario", String.valueOf(marcos.id())).with(como(admin)).with(csrf()))
			.andExpect(flash().attribute("resultado", containsString("saiu do jogo")));
		String aviso = marcos.nome() + " saiu do jogo. O clube dele agora é da máquina.";
		String adiante = mvc.perform(get("/jogos/" + jogo + "/plantel").flashAttr("resultado", aviso).with(como(admin)))
			.andExpect(status().is3xxRedirection())
			.andExpect(flash().attribute("resultado", aviso))
			.andReturn().getResponse().getRedirectedUrl();
		assertThat(adiante).isIn("/jogos/" + jogo + "/leiloes", "/jogos/" + jogo + "/rodada");
		mvc.perform(get(adiante).flashAttr("resultado", aviso).with(como(admin)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString(aviso)));
	}

	@Test
	void soOConvidadoVeSairDaGravacaoNoInicioESaiDeVez() throws Exception {
		patrocinios.assinar(jogo, admin.id(), PropostasPatrocinio.Tipo.FIXA);
		patrocinios.assinar(jogo, marcos.id(), PropostasPatrocinio.Tipo.FIXA);
		mvc.perform(get("/").with(como(admin)))
			.andExpect(content().string(not(containsString("Sair da gravação"))));
		mvc.perform(get("/").param("sair", String.valueOf(jogo)).with(como(admin)))
			.andExpect(content().string(not(containsString("Sair de vez"))));
		mvc.perform(get("/").with(como(marcos)))
			.andExpect(content().string(containsString("/?sair=" + jogo)));
		mvc.perform(get("/").param("sair", String.valueOf(jogo)).with(como(marcos)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Sair de vez do jogo do ")))
			.andExpect(content().string(containsString("Você não volta mais a este jogo em rede.")))
			.andExpect(content().string(containsString("action=\"/jogos/" + jogo + "/rede/sair\"")));
		mvc.perform(post("/jogos/" + jogo + "/rede/sair").with(como(admin)).with(csrf()))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attribute("erro", containsString("apagar a gravação")));

		mvc.perform(post("/jogos/" + jogo + "/rede/sair").with(como(marcos)).with(csrf()))
			.andExpect(redirectedUrl("/"))
			.andExpect(flash().attribute("resultado", containsString("O clube agora é da máquina.")));
		mvc.perform(get("/jogos/" + jogo + "/plantel").with(como(marcos)))
			.andExpect(status().isNotFound());
		mvc.perform(post("/jogos/" + jogo + "/rede/sair").with(como(marcos)).with(csrf()))
			.andExpect(flash().attribute("erro", containsString("não está mais")));
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
