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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.economia.PropostasPatrocinio;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.ConsultasRepositorio;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.ContinentalRepositorio;
import br.com.managerfoot.persistencia.EscalacaoRepositorio;
import br.com.managerfoot.persistencia.FimDeTemporadaRepositorio;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.PatrocinioRepositorio;
import br.com.managerfoot.persistencia.RodadaRepositorio;
import br.com.managerfoot.persistencia.RodadasDeTeste;
import br.com.managerfoot.temporada.Continental;

/** A continental pela tela (SPEC §8.3.6, modelos 13a a 13d, rev. 79), com o relógio do teste. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ContinentalTelasIT {

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
	ContinentalRepositorio continentais;

	@Autowired
	JogoRepositorio jogo;

	@Autowired
	EscalacaoRepositorio escalacoes;

	@Autowired
	PatrocinioRepositorio patrocinios;

	@Autowired
	FimDeTemporadaRepositorio fins;

	@Autowired
	ConsultasRepositorio consultas;

	@Autowired
	JdbcClient jdbc;

	Usuario treinador;
	long jogoId;
	long temporada;
	String rodada;
	String plantel;

	@BeforeEach
	void jogoNovo() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());
		long brasil = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow().paisId();
		treinador = contas.criarAdministrador("c" + System.nanoTime() + "@exemplo.com", "Gabriel", "senha-boa-1");
		jogoId = jogos.criarComSorteio(brasil, treinador.id(), 31).jogoId();
		temporada = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :j").param("j", jogoId).query(Long.class).single();
		rodada = "/jogos/" + jogoId + "/rodada";
		plantel = "/jogos/" + jogoId + "/plantel";
		agora(0);
	}

	void agora(long segundos) {
		when(relogio.instant()).thenReturn(T0.plusSeconds(segundos));
	}

	Continental continental() {
		return continentais.carregar(jogoId, temporada).orElseThrow();
	}

	/** Um clube do Brasil que está num grupo. */
	Clube brasileiroNaContinental() {
		Map<Long, String> deFora = continentais.paisesDeFora(jogoId);
		return continental().grupos().stream().flatMap(g -> g.clubes().stream()).filter(c -> !deFora.containsKey(c.id()))
			.findFirst().orElseThrow();
	}

	/** Passa o treinador para o clube, com a escalação da máquina. */
	void assumir(Clube clube) {
		long antigo = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		jdbc.sql("DELETE FROM jogo_escalacao WHERE clube_id = :c").param("c", antigo).update();
		jdbc.sql("UPDATE jogo_treinador SET clube_id = :c WHERE jogo_id = :j").param("c", clube.id()).param("j", jogoId).update();
		jdbc.sql("UPDATE jogo_patrocinio SET clube_id = :c WHERE clube_id = :a").param("c", clube.id()).param("a", antigo).update();
	}

	/** A escalação da máquina para o treinador, só com quem está disponível. */
	void escalarOTreinador() {
		long clube = jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow();
		List<Long> fora = jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :c AND (suspensao > 0 OR lesao > 0)")
			.param("c", clube).query(Long.class).list();
		escalacoes.gravar(clube, EscalacaoDoTreinador.de(new EscalacaoMaquina(ParametrosMotor.padrao())
			.escalar(jogo.carregarPlantel(clube).stream().filter(j -> !fora.contains(j.id())).toList())));
	}

	void jogarAteAContinental() {
		patrocinios.assinar(jogoId, treinador.id(), PropostasPatrocinio.Tipo.FIXA);
		jdbc.sql("DELETE FROM jogo_escalacao WHERE clube_id = :c")
			.param("c", jogo.clubeDoTreinador(jogoId, treinador.id()).orElseThrow()).update();
		RodadasDeTeste.jogarRodada(rodadas, jogoId, 1);
		RodadasDeTeste.jogarRodada(rodadas, jogoId, 2);
		escalarOTreinador();
		assertThat(rodadas.proximo(jogoId)).isInstanceOf(RodadaRepositorio.Proximo.EtapaContinental.class);
	}

	@Test
	void comOClubeDoUsuarioAEtapaCorreNaVelocidadeDeleComTodosOsGruposNumBlocoSo() throws Exception {
		Clube clube = brasileiroNaContinental();
		assumir(clube);
		jogarAteAContinental();

		mvc.perform(get(plantel).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Próximo jogo · Continental · Grupos · 1º jogo")))
			.andExpect(content().string(containsString("Confirmar e jogar a continental")))
			.andExpect(content().string(containsString("name=\"continental\"")))
			.andExpect(content().string(containsString("º com 0 pontos")));

		mvc.perform(post(rodada).param("rodada", "3").param("etapa", "0").param("continental", "true")
			.with(como(treinador)).with(csrf())).andExpect(redirectedUrl(rodada));

		agora(10);
		mvc.perform(get(rodada).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Continental · Grupos · 1º jogo")))
			.andExpect(content().string(containsString("Copa dos Clubes da América do Sul · Grupos · 1º jogo")))
			.andExpect(content().string(containsString("Grupo A")))
			.andExpect(content().string(containsString("Grupo H")))
			.andExpect(content().string(containsString("20&#39;")))
			.andExpect(content().string(containsString("do-usuario")))
			.andExpect(content().string(containsString("Velocidade da partida")))
			.andExpect(content().string(not(containsString("1º jogo</span>"))));

		// clicar no jogo com um clube de fora abre a janela com os dois times
		long mandante = jdbc.sql("""
				SELECT p.mandante_id FROM jogo_partida p
				WHERE p.temporada_id = :t AND p.grupo IS NOT NULL AND p.jogo_do_grupo = 1
				  AND (p.mandante_id = :c OR p.visitante_id = :c)""")
			.param("t", temporada).param("c", clube.id()).query(Long.class).single();
		mvc.perform(get(rodada).param("janela", String.valueOf(mandante)).with(como(treinador))).andExpect(status().isOk());

		agora(3_600);
		mvc.perform(get(rodada).with(como(treinador))).andExpect(redirectedUrl(plantel));
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_partida WHERE temporada_id = :t AND grupo IS NOT NULL AND gols_mandante IS NOT NULL")
			.param("t", temporada).query(Long.class).single()).isEqualTo(16);
		assertThat(rodadas.proximo(jogoId)).isEqualTo(new RodadaRepositorio.Proximo.Liga(3));
		// repetir o pedido não joga de novo
		mvc.perform(post(rodada).param("rodada", "3").param("etapa", "0").param("continental", "true")
			.with(como(treinador)).with(csrf())).andExpect(redirectedUrl(plantel));
	}

	@Test
	void semOClubeDoUsuarioAEtapaCorreRapidoESemControles() throws Exception {
		jogarAteAContinental();

		mvc.perform(get(plantel).with(como(treinador)))
			.andExpect(content().string(containsString("Seu clube não está na continental desta temporada")))
			.andExpect(content().string(containsString("Confirmar e jogar a continental")));
		mvc.perform(post(rodada).param("rodada", "3").param("etapa", "0").param("continental", "true")
			.with(como(treinador)).with(csrf())).andExpect(redirectedUrl(rodada));

		agora(1);
		mvc.perform(get(rodada).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Seu clube não joga esta etapa da continental")))
			.andExpect(content().string(not(containsString("Velocidade da partida"))));
		// cerca de 5 s no bloco sem usuário
		agora(6);
		mvc.perform(get(rodada).with(como(treinador))).andExpect(redirectedUrl(plantel));
		assertThat(List.of(rodadas.proximo(jogoId))).containsExactly(new RodadaRepositorio.Proximo.Liga(3));
	}

	@Test
	void publicoNaTelaEOMesmoGravadoComClubeDeForaEmCasa() throws Exception {
		jogarAteAContinental();
		mvc.perform(post(rodada).param("rodada", "3").param("etapa", "0").param("continental", "true")
			.with(como(treinador)).with(csrf()));
		agora(1);
		String tela = mvc.perform(get(rodada).with(como(treinador))).andReturn().getResponse().getContentAsString();
		agora(6);
		mvc.perform(get(rodada).with(como(treinador)));

		List<Integer> publicos = jdbc.sql("""
				SELECT p.publico FROM jogo_partida p JOIN jogo_clube c ON c.id = p.mandante_id
				WHERE p.temporada_id = :t AND p.grupo IS NOT NULL AND p.jogo_do_grupo = 1 AND c.pais_codigo IS NOT NULL""")
			.param("t", temporada).query(Integer.class).list();
		assertThat(publicos).isNotEmpty().allSatisfy(p -> {
			assertThat(p).isPositive();
			assertThat(tela).contains(String.format(java.util.Locale.forLanguageTag("pt-BR"), "%,d", p));
		});
	}

	@Test
	void abaContinentalComGruposJogosDoGrupoEChave() throws Exception {
		Clube clube = brasileiroNaContinental();
		assumir(clube);
		String aba = "/jogos/" + jogoId + "/continental";

		mvc.perform(get(aba).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("aria-current=\"page\">Continental")))
			.andExpect(content().string(containsString("Copa dos Clubes da América do Sul · Temporada 2026")))
			.andExpect(content().string(containsString("32 clubes de 10 países")))
			.andExpect(content().string(containsString("º lugar")))
			.andExpect(content().string(containsString("seu grupo")))
			.andExpect(content().string(containsString("0 de 6 jogos · próximo depois da 2")))
			.andExpect(content().string(containsString("class=\"sigla-pais\">ARG")))
			.andExpect(content().string(containsString("Sorteio depois do 6º jogo dos grupos")))
			.andExpect(content().string(containsString("1º · depois da 2")))
			.andExpect(content().string(containsString(clube.nome() + " (você)")));
		// outro grupo pelo link, e uma fase inválida cai na atual
		mvc.perform(get(aba).param("fase", "GRUPOS").param("grupo", "7").with(como(treinador)))
			.andExpect(content().string(containsString("Jogos do Grupo H")));
		mvc.perform(get(aba).param("fase", "QUALQUER").with(como(treinador))).andExpect(status().isOk());
		mvc.perform(get(aba).param("fase", "QUARTAS").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Os confrontos desta fase saem quando a anterior terminar.")));
	}

	@Test
	void temporadaInteiraAparecePeloCalendarioPelasFinancasEPeloHistorico() throws Exception {
		Clube clube = brasileiroNaContinental();
		assumir(clube);
		patrocinios.assinar(jogoId, treinador.id(), PropostasPatrocinio.Tipo.FIXA);
		jdbc.sql("DELETE FROM jogo_escalacao WHERE clube_id = :c").param("c", clube.id()).update();
		int total = rodadas.totalDeRodadas(jogoId);
		for (int r = 1; r <= total; r++) {
			RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogoId);
			RodadasDeTeste.jogarRodada(rodadas, jogoId, r);
		}
		assertThat(rodadas.proximo(jogoId)).isInstanceOf(RodadaRepositorio.Proximo.Fim.class);

		mvc.perform(get("/jogos/" + jogoId + "/classificacao").param("calendario", String.valueOf(clube.id())).with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Continental · Grupos · 1º jogo")))
			.andExpect(content().string(containsString("calendario-continental")))
			.andExpect(content().string(containsString("class=\"sigla-pais\"")));
		mvc.perform(get("/jogos/" + jogoId + "/financas").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Continental na temporada")))
			.andExpect(content().string(containsString("extrato-continental")));
		mvc.perform(get("/jogos/" + jogoId + "/continental").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Campeão: " + continental().campeao().orElseThrow().nome())));

		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogoId);
		fins.avancarTemporada(jogoId, 1, 99);
		Continental encerrada = continentais.carregar(jogoId, temporada).orElseThrow();
		ConsultasRepositorio.VisaoHistorico historico = consultas.historico(jogoId, treinador.id());
		assertThat(historico.nomeContinental()).isEqualTo("Copa dos Clubes da América do Sul");
		assertThat(historico.continental()).singleElement().satisfies(l -> {
			assertThat(l.campeao().clube()).isEqualTo(encerrada.campeao().orElseThrow().nome());
			assertThat(l.campeao().detalhe()).startsWith("final ");
			assertThat(l.vice().clube()).isEqualTo(encerrada.vice().orElseThrow().nome());
			assertThat(l.campeao().pais()).isEqualTo(continentais.paisesDeFora(jogoId).get(encerrada.campeao().orElseThrow().id()));
		});
		assertThat(historico.copa()).singleElement().satisfies(l -> {
			// a final certa: o placar é o da final da copa, não o da continental
			String esperado = jdbc.sql("""
					SELECT 'final ' || CASE WHEN p.mandante_id = c.clube_id THEN p.gols_mandante ELSE p.gols_visitante END || ' x '
					       || CASE WHEN p.mandante_id = c.clube_id THEN p.gols_visitante ELSE p.gols_mandante END
					FROM jogo_campeao_copa c JOIN jogo_copa_confronto k ON k.fase = 'FINAL' AND k.competicao = 'COPA' AND k.temporada_id = :t
					JOIN jogo_partida p ON p.confronto_id = k.id WHERE c.jogo_id = :j""")
				.param("t", temporada).param("j", jogoId).query(String.class).single();
			assertThat(l.campeao().detalhe()).startsWith(esperado);
		});
		// todos os títulos: só nacionais; somam a liga, a copa e a continental ganha por clube do país
		boolean campeaoDoPais = !continentais.paisesDeFora(jogoId).containsKey(encerrada.campeao().orElseThrow().id());
		assertThat(historico.titulos().stream().mapToInt(ConsultasRepositorio.TreinadorNoRanking::total).sum())
			.isEqualTo(2 + (campeaoDoPais ? 1 : 0));
		mvc.perform(get("/jogos/" + jogoId + "/historico").with(como(treinador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Liga · ")))
			.andExpect(content().string(containsString(">Vice</th>")))
			.andExpect(content().string(containsString(">3º</th>")))
			.andExpect(content().string(containsString("Copa dos Clubes da América do Sul</h2>")))
			.andExpect(content().string(containsString("Mundial de Clubes")))
			.andExpect(content().string(containsString("Todos os títulos")))
			.andExpect(content().string(not(containsString("Ranking de treinadores"))))
			.andExpect(content().string(not(containsString(">Série B"))));
	}

	private static RequestPostProcessor como(Usuario usuario) {
		return authentication(AutenticacaoContas.autenticado(usuario));
	}
}
