package br.com.managerfoot.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.RequestPostProcessor;
import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.economia.PropostasPatrocinio;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.AumentoRepositorio;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.MundialRepositorio;
import br.com.managerfoot.persistencia.PatrocinioRepositorio;
import br.com.managerfoot.persistencia.RodadaRepositorio;
import br.com.managerfoot.persistencia.RodadasDeTeste;
import br.com.managerfoot.temporada.Mundial;

/** O Mundial pela tela (SPEC §8.3.4, modelos 15a a 15c, rev. 88), com o relógio do teste. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MundialTelasIT {

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
	JogoRepositorio jogo;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	MundialRepositorio mundiais;

	@Autowired
	PatrocinioRepositorio patrocinios;

	@Autowired
	AumentoRepositorio aumentos;

	@Autowired
	JdbcClient jdbc;

	Usuario treinador;
	long jogoId;
	long temporada;
	long clube;
	String rodada;
	String plantel;

	@BeforeEach
	void jogarAteOMundialComOClubeDoTreinadorNaSemifinal() {
		when(relogio.instant()).thenReturn(T0);
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());
		long brasil = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow().paisId();
		treinador = contas.criarAdministrador("m" + System.nanoTime() + "@exemplo.com", "Gabriel", "senha-boa-1");
		jogoId = jogos.criarComSorteio(brasil, treinador.id(), 31).jogoId();
		temporada = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :j").param("j", jogoId).query(Long.class).single();
		rodada = "/jogos/" + jogoId + "/rodada";
		plantel = "/jogos/" + jogoId + "/plantel";
		patrocinios.assinar(jogoId, treinador.id(), PropostasPatrocinio.Tipo.FIXA);
		int total = rodadas.totalDeRodadas(jogoId);
		while (!(rodadas.proximo(jogoId) instanceof RodadaRepositorio.Proximo.EtapaMundial)) {
			RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogoId);
			jdbc.sql("UPDATE jogo_clube SET moral = 30 WHERE jogo_id = :j").param("j", jogoId).update();
			switch (rodadas.proximo(jogoId)) {
				case RodadaRepositorio.Proximo.Liga(int r) -> rodadas.avancar(jogoId, r);
				case RodadaRepositorio.Proximo.EtapaCopa e -> rodadas.avancarCopa(jogoId, e.indice(), List.of());
				case RodadaRepositorio.Proximo.EtapaContinental e -> rodadas.avancarContinental(jogoId, e.indice(), List.of());
				default -> throw new IllegalStateException("temporada de " + total + " rodadas sem Mundial");
			}
		}
		// o clube do treinador entra no lugar do campeão continental, seja quem for
		clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		Mundial mundial = mundiais.carregar(jogoId, temporada).orElseThrow();
		long campeao = mundial.semifinais().get(1).segundo().id();
		jdbc.sql("UPDATE jogo_copa_confronto SET visitante_id = :c WHERE temporada_id = :t AND competicao = 'MUNDIAL' AND visitante_id = :a")
			.param("c", clube).param("t", temporada).param("a", campeao).update();
		jdbc.sql("""
				UPDATE jogo_partida SET visitante_id = :c WHERE temporada_id = :t AND visitante_id = :a
				  AND confronto_id IN (SELECT id FROM jogo_copa_confronto WHERE competicao = 'MUNDIAL')""")
			.param("c", clube).param("t", temporada).param("a", campeao).update();
		jdbc.sql("DELETE FROM jogo_escalacao WHERE clube_id = :c").param("c", clube).update();
		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogoId);
		responderAumentos();
	}

	/** Os pedidos de aumento seguram o jogo até a resposta, como na tela. */
	void responderAumentos() {
		while (aumentos.pendente(jogoId, treinador.id()).isPresent()) {
			aumentos.aceitar(jogoId, treinador.id(), aumentos.pendente(jogoId, treinador.id()).orElseThrow().jogador().id());
		}
	}

	@Test
	void plantelAoVivoAbaCalendarioExtratoEHistoricoMostramOMundial() throws Exception {
		mvc.perform(get("/jogos/" + jogoId + "/mundial").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Mundial de Clubes · Temporada 2026")))
			.andExpect(content().string(containsString("Semifinais")))
			.andExpect(content().string(containsString("América do Norte e Central")))
			.andExpect(content().string(containsString("Por força com sorteio")))
			.andExpect(content().string(containsString("(você)")));

		mvc.perform(get(plantel).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Próximo jogo · Mundial · Semifinal")))
			.andExpect(content().string(containsString("Confirmar e jogar o Mundial")))
			.andExpect(content().string(containsString("name=\"mundial\"")));

		String iniciar = String.valueOf(rodadas.totalDeRodadas(jogoId) + 1);
		mvc.perform(post(rodada).param("rodada", iniciar).param("etapa", "0").param("mundial", "true")
			.with(como(treinador)).with(csrf())).andExpect(redirectedUrl(rodada));
		when(relogio.instant()).thenReturn(T0.plusSeconds(10));
		mvc.perform(get(rodada).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Mundial · Semifinal")))
			.andExpect(content().string(containsString("Mundial de Clubes · Semifinal")));
		when(relogio.instant()).thenReturn(T0.plusSeconds(3_600));
		mvc.perform(get(rodada).with(como(treinador))).andExpect(redirectedUrl(plantel));
		assertThat(rodadas.proximo(jogoId)).isInstanceOf(RodadaRepositorio.Proximo.EtapaMundial.class);

		mvc.perform(get("/jogos/" + jogoId + "/financas").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("extrato-mundial")))
			.andExpect(content().string(containsString("Semifinal")));
		mvc.perform(get("/jogos/" + jogoId + "/clube/" + clube).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Mundial · Semifinal")))
			.andExpect(content().string(containsString("calendario-mundial")));

		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogoId);
		rodadas.avancarMundial(jogoId, 1, List.of());
		assertThat(rodadas.proximo(jogoId)).isInstanceOf(RodadaRepositorio.Proximo.Fim.class);
		mvc.perform(get("/jogos/" + jogoId + "/mundial").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Campeão")));
		String campeao = jdbc.sql("SELECT clube_nome FROM jogo_campeao_mundial WHERE jogo_id = :j").param("j", jogoId)
			.query(String.class).single();
		mvc.perform(get("/jogos/" + jogoId + "/historico").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Mund.")))
			.andExpect(content().string(containsString(campeao)));
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
