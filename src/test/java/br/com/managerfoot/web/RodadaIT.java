package br.com.managerfoot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import java.util.List;

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
import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.EscalacaoRepositorio;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.RodadaRepositorio;
import br.com.managerfoot.temporada.RodadaAoVivo.PartidaDaRodada;

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

	@Autowired
	JogoRepositorio jogo;

	@Autowired
	EscalacaoRepositorio escalacoes;

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

	@Test
	void clicarNoProprioClubePausaEAbreAJanelaComSubstituicao() throws Exception {
		iniciar();
		agora(10);
		long mandante = partidaDoUsuario().jogo().mandante().id();

		mvc.perform(post(rodada + "/janela").param("mandante", String.valueOf(mandante)).with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl(rodada + "?janela=" + mandante));
		agora(60);
		mvc.perform(get(rodada).param("janela", String.valueOf(mandante)).with(como(treinador)))
			.andExpect(content().string(containsString("20&#39; · Estádio · público ")))
			.andExpect(content().string(containsString(" · partida pausada")))
			.andExpect(content().string(containsString("class=\"jogo-publico\"")))
			.andExpect(content().string(containsString("substituições: 0 de 5")))
			.andExpect(content().string(containsString("Substituir")))
			.andExpect(content().string(not(containsString("every 1s"))));

		Troca troca = troca();
		mvc.perform(get(rodada).param("janela", String.valueOf(mandante)).param("sai", troca.sai())
				.param("entra", troca.entra()).with(como(treinador)))
			.andExpect(content().string(containsString("Com a troca, fica")));
		mvc.perform(post(rodada + "/substituir").param("mandante", String.valueOf(mandante)).param("sai", troca.sai())
				.param("entra", troca.entra()).with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl(rodada + "?janela=" + mandante))
			.andExpect(flash().attributeCount(0));
		mvc.perform(get(rodada).param("janela", String.valueOf(mandante)).with(como(treinador)))
			.andExpect(content().string(containsString("substituições: 1 de 5")))
			.andExpect(content().string(containsString("20&#39; ")));

		mvc.perform(post(rodada + "/continuar").with(como(treinador)).with(csrf())).andExpect(redirectedUrl(rodada));
		agora(61);
		mvc.perform(get(rodada).with(como(treinador)))
			.andExpect(content().string(containsString("22&#39;")))
			.andExpect(content().string(containsString("every 1s")));
	}

	@Test
	void trocaInvalidaVoltaComOMotivo() throws Exception {
		iniciar();
		long mandante = partidaDoUsuario().jogo().mandante().id();
		mvc.perform(post(rodada + "/janela").param("mandante", String.valueOf(mandante)).with(como(treinador)).with(csrf()));

		mvc.perform(post(rodada + "/substituir").param("mandante", String.valueOf(mandante)).with(como(treinador)).with(csrf()))
			.andExpect(flash().attribute("erro", "Marque quem sai e quem entra."));
		Troca troca = troca();
		mvc.perform(post(rodada + "/substituir").param("mandante", String.valueOf(mandante)).param("sai", troca.entra())
				.param("entra", troca.sai()).with(como(treinador)).with(csrf()))
			.andExpect(flash().attribute("erro", "Quem sai precisa estar em campo."));
	}

	@Test
	void janelaDeOutroJogoMostraOsOnzeDeCadaLadoSemSubstituicao() throws Exception {
		iniciar();
		PartidaDaRodada doUsuario = partidaDoUsuario();
		long outro = rodadas.preparar(jogoId, 1, List.of()).partidas().stream()
			.filter(p -> p.prioridade() == doUsuario.prioridade() && p != doUsuario
					&& p.jogo().mandante().id() != doUsuario.jogo().mandante().id())
			.findFirst().orElseThrow().jogo().mandante().id();

		mvc.perform(post(rodada + "/janela").param("mandante", String.valueOf(outro)).with(como(treinador)).with(csrf()));
		mvc.perform(get(rodada).param("janela", String.valueOf(outro)).with(como(treinador)))
			.andExpect(content().string(containsString("Quem está jogando")))
			.andExpect(content().string(not(containsString("Substituir"))));
	}

	@Test
	void somDosGolsLigaEDesliga() throws Exception {
		iniciar();
		mvc.perform(get(rodada).with(como(treinador))).andExpect(content().string(containsString("data-som=\"true\"")));

		mvc.perform(post(rodada + "/som").param("ligado", "false").with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl(rodada));

		mvc.perform(get(rodada).with(como(treinador)))
			.andExpect(content().string(containsString("Som dos gols desligado")))
			.andExpect(content().string(containsString("data-som=\"false\"")));
	}

	@Test
	void divisaoSemUsuarioNaoTemClubesClicaveis() throws Exception {
		iniciar();
		// a divisão do usuário (a última) joga primeiro e dura 45 s em normal
		agora(46);
		mvc.perform(get(rodada).with(como(treinador)))
			.andExpect(content().string(containsString("Nenhum clube desta divisão é seu")))
			.andExpect(content().string(not(containsString("pausar e ver o jogo"))));
	}

	private void iniciar() throws Exception {
		mvc.perform(post(rodada).param("rodada", "1").with(como(treinador)).with(csrf())).andExpect(redirectedUrl(rodada));
	}

	private PartidaDaRodada partidaDoUsuario() {
		long clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		return rodadas.preparar(jogoId, 1, List.of()).partidas().stream()
			.filter(p -> p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube).findFirst().orElseThrow();
	}

	/** Um titular de linha e um reserva da mesma posição do clube do usuário. */
	private Troca troca() {
		long clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		EscalacaoDoTreinador escalacao = escalacoes.carregar(clube).orElseThrow();
		List<Jogador> plantel = jogo.carregarPlanteis(jogoId).get(clube);
		for (Jogador entra : plantel) {
			if (!escalacao.reservas().contains(entra.id()) || entra.posicao() == Posicao.GOLEIRO) {
				continue;
			}
			for (Jogador sai : plantel) {
				if (escalacao.titulares().contains(sai.id()) && sai.posicao() == entra.posicao()) {
					return new Troca(String.valueOf(sai.id()), String.valueOf(entra.id()));
				}
			}
		}
		throw new IllegalStateException("sem troca na mesma posição");
	}

	private record Troca(String sai, String entra) {
	}

	private void agora(long segundos) {
		when(relogio.instant()).thenReturn(T0.plusSeconds(segundos));
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
