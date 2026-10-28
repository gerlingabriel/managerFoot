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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.PaisPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.EscalacaoRepositorio;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.RodadaRepositorio;

/** Plantel e tática (SPEC §10.3, rev. 34) pela tela, com o banco padrão. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PlantelIT {

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
	JogoRepositorio jogo;

	@Autowired
	EscalacaoRepositorio escalacoes;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	JdbcClient jdbc;

	Usuario treinador;
	long jogoId;
	long clube;
	String plantel;

	@BeforeEach
	void jogoNovo() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao())
			.gerar(PaisPadrao.TODOS));
		long brasil = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow().paisId();
		treinador = contas.criarAdministrador("t" + System.nanoTime() + "@exemplo.com", "Gabriel", "senha-boa-1");
		jogoId = jogos.criarComSorteio(brasil, treinador.id(), 77).jogoId();
		clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		plantel = "/jogos/" + jogoId + "/plantel";
	}

	@Test
	void mostraOClubeAEscalacaoDaMaquinaEOProximoJogo() throws Exception {
		mvc.perform(get(plantel).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Escalação válida: 1 G")))
			.andExpect(content().string(containsString("11 de 11")))
			.andExpect(content().string(containsString("7 de 7 reservas")))
			.andExpect(content().string(containsString("Próximo jogo · Rodada 1")))
			.andExpect(content().string(containsString("/ 30 · verde")));
	}

	@Test
	void tirarUmTitularDeixaAEscalacaoIncompletaEVoltarConserta() throws Exception {
		EscalacaoDoTreinador antes = escalacoes.carregar(clube).orElseThrow();
		long titular = antes.titulares().iterator().next();

		mvc.perform(post(plantel + "/jogador/" + titular).param("lugar", "FORA").with(como(treinador)).with(csrf()))
			.andExpect(redirectedUrl(plantel));
		mvc.perform(get(plantel).with(como(treinador)))
			.andExpect(content().string(containsString("10 de 11")))
			.andExpect(content().string(containsString("Complete a escalação para jogar.")))
			.andExpect(content().string(not(containsString("Escalação válida"))));
		pedir(post(jogarRodada()).param("rodada", "1")).andExpect(flash().attribute("erro",
				"Complete a escalação antes de jogar a rodada."));
		assertThat(rodadas.rodadasProcessadas(jogoId)).isZero();

		pedir(post(plantel + "/jogador/" + titular).param("lugar", "TITULAR"));
		assertThat(escalacoes.carregar(clube)).hasValue(antes.comBatedor(antes.batedor()));
	}

	@Test
	void trocarFormacaoEBatedor() throws Exception {
		EscalacaoDoTreinador antes = escalacoes.carregar(clube).orElseThrow();
		long reserva = antes.reservas().iterator().next();
		String outra = antes.formacao().name().equals("F_3_6_1") ? "F_4_4_2" : "F_3_6_1";

		pedir(post(plantel + "/formacao").param("formacao", outra));
		pedir(post(plantel + "/batedor").param("batedor", String.valueOf(reserva)));

		EscalacaoDoTreinador depois = escalacoes.carregar(clube).orElseThrow();
		assertThat(depois.formacao().name()).isEqualTo(outra);
		assertThat(depois.batedor()).isEqualTo(reserva);

		long deFora = jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :c AND id NOT IN (:ids)")
			.param("c", clube).param("ids", depois.titulares().stream().toList()).query(Long.class).list().stream()
			.filter(id -> !depois.reservas().contains(id)).findFirst().orElseThrow();
		pedir(post(plantel + "/batedor").param("batedor", String.valueOf(deFora)))
			.andExpect(flash().attribute("erro", "O batedor precisa estar entre os titulares ou no banco."));
	}

	@Test
	void suspensoNaoPodeSerEscalado() throws Exception {
		long reserva = escalacoes.carregar(clube).orElseThrow().reservas().iterator().next();
		pedir(post(plantel + "/jogador/" + reserva).param("lugar", "FORA"));
		jdbc.sql("UPDATE jogo_jogador SET suspensao = 1 WHERE id = :j").param("j", reserva).update();

		pedir(post(plantel + "/jogador/" + reserva).param("lugar", "BANCO"))
			.andExpect(flash().attribute("erro", containsString("está suspenso")));
		mvc.perform(get(plantel).with(como(treinador))).andExpect(content().string(containsString(">suspenso<")));
	}

	@Test
	void outroUsuarioNaoVeNemMexeESemCsrfERecusado() throws Exception {
		Usuario outro = contas.criarAdministrador("o" + System.nanoTime() + "@exemplo.com", "Outro", "senha-boa-1");
		long titular = escalacoes.carregar(clube).orElseThrow().titulares().iterator().next();

		mvc.perform(get(plantel).with(como(outro))).andExpect(status().isNotFound());
		mvc.perform(post(plantel + "/jogador/" + titular).param("lugar", "FORA").with(como(outro)).with(csrf()))
			.andExpect(status().isNotFound());
		mvc.perform(post(jogarRodada()).param("rodada", "1").with(como(outro)).with(csrf()))
			.andExpect(status().isNotFound());
		mvc.perform(post(plantel + "/jogador/" + titular).param("lugar", "FORA").with(como(treinador)))
			.andExpect(status().isForbidden());

		assertThat(escalacoes.carregar(clube).orElseThrow().titulares()).contains(titular);
		assertThat(rodadas.rodadasProcessadas(jogoId)).isZero();
	}

	private ResultActions pedir(MockHttpServletRequestBuilder pedido) throws Exception {
		return mvc.perform(pedido.with(como(treinador)).with(csrf())).andExpect(redirectedUrl(plantel));
	}

	private String jogarRodada() {
		return "/jogos/" + jogoId + "/rodada";
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
