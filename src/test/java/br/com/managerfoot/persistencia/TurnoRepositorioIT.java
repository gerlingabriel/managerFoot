package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Lugar;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.economia.PropostasPatrocinio;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.multiplayer.Turno;
import br.com.managerfoot.multiplayer.Turno.Passo;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio.EstadoRodada;
import br.com.managerfoot.persistencia.TurnoRepositorio.Desfecho;
import br.com.managerfoot.temporada.Velocidade;

/** Migração V42 e {@link TurnoRepositorio}: o OK de todos no jogo em rede (SPEC §3.3 itens 4 a 6, rev. 92). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TurnoRepositorioIT {

	private static final Instant AGORA = Instant.parse("2026-11-03T20:00:00Z");

	@Autowired
	TurnoRepositorio turnos;

	@Autowired
	SalaRepositorio salas;

	@Autowired
	JogosDoTreinadorRepositorio jogosDoTreinador;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	PatrocinioRepositorio patrocinios;

	@Autowired
	PlantelRepositorio plantel;

	@Autowired
	LeilaoRepositorio leiloes;

	@Autowired
	RodadaAoVivoRepositorio aoVivo;

	@Autowired
	JdbcClient jdbc;

	Usuario admin;
	Usuario marcos;
	long jogo;

	@BeforeEach
	void jogoEmRedeComecado() {
		long pais = LigaDeTeste.criarPais(moldes);
		admin = LigaDeTeste.administrador(contas);
		marcos = LigaDeTeste.participante(contas);
		jogo = jogosDoTreinador.criarEmRedeComSorteio(pais, admin.id(), AGORA).jogoId();
		String token = salas.visao(jogo, admin.id()).orElseThrow().convite().orElseThrow().token();
		salas.entrar(token, marcos.id(), AGORA);
		salas.comecar(jogo, admin.id(), AGORA, Duration.ofSeconds(30));
		for (Usuario u : new Usuario[] { admin, marcos }) {
			patrocinios.assinar(jogo, u.id(), PropostasPatrocinio.Tipo.FIXA);
			plantel.visao(jogo, u.id()).orElseThrow();
		}
	}

	@Test
	void oJogoEsperaOOkDeTodosEQuemDeuOkPodeTirar() {
		assertThat(turnos.emRede(jogo)).isTrue();
		assertThat(turnos.darOk(jogo, admin.id(), Passo.PLANTEL, 0, AGORA)).isEqualTo(Desfecho.AGUARDANDO);
		assertThat(turnos.darOk(jogo, admin.id(), Passo.PLANTEL, 0, AGORA)).as("repetir não muda nada").isEqualTo(Desfecho.AGUARDANDO);

		Turno turno = turnos.turno(jogo).orElseThrow();
		assertThat(turno.momento()).isEqualTo("t1/r1/tatica");
		assertThat(turno.comOk()).isEqualTo(1);
		assertThat(turno.faltam()).isEqualTo(marcos.nome());
		assertThat(turno.treinadores().getFirst().usuarioId()).as("o administrador primeiro").isEqualTo(admin.id());
		long jogador = plantel.visao(jogo, admin.id()).orElseThrow().jogadores().getFirst().jogador().id();
		assertThatIllegalArgumentException().isThrownBy(() -> plantel.mover(jogo, admin.id(), jogador, Lugar.FORA))
			.withMessageContaining("Tirar meu OK");

		turnos.tirarOk(jogo, admin.id());
		assertThat(turnos.okDado(jogo, admin.id())).isFalse();
		plantel.mover(jogo, admin.id(), jogador, Lugar.FORA);
		plantel.mover(jogo, admin.id(), jogador, Lugar.TITULAR);
		assertThat(aoVivo.emAndamento(jogo)).isFalse();
	}

	@Test
	void comOOkDeTodosARodadaComecaDepoisDosLeiloes() {
		assertThat(ateAoVivo()).isEqualTo(Desfecho.AO_VIVO);
		assertThat(aoVivo.emAndamento(jogo)).isTrue();
		assertThat(turnos.turno(jogo)).as("sem faixa com a rodada ao vivo").isEmpty();
		assertThatIllegalStateException().isThrownBy(() -> turnos.darOk(jogo, marcos.id(), Passo.PLANTEL, 0, AGORA));
	}

	@Test
	void naRodadaAVelocidadeEhADoAdministradorParaTodos() {
		ateAoVivo();
		aoVivo.mudarVelocidade(jogo, admin.id(), Velocidade.RAPIDA, AGORA);

		var deMarcos = aoVivo.estado(jogo, marcos.id(), AGORA.plusSeconds(1)).orElseThrow();
		assertThat(deMarcos.velocidade()).isEqualTo(Velocidade.RAPIDA);
		assertThat(deMarcos.velocidadeDe()).isEqualTo(admin.nome());
		assertThat(aoVivo.estado(jogo, admin.id(), AGORA.plusSeconds(1)).orElseThrow().velocidadeDe()).isNull();
		assertThat(aoVivo.estado(jogo, admin.id(), AGORA.plusSeconds(2)).orElseThrow().minuto())
			.as("o mesmo relógio para os dois").isEqualTo(aoVivo.estado(jogo, marcos.id(), AGORA.plusSeconds(2)).orElseThrow().minuto());
		assertThatIllegalArgumentException().isThrownBy(() -> aoVivo.mudarVelocidade(jogo, marcos.id(), Velocidade.LENTA, AGORA))
			.withMessageContaining("administrador");
	}

	@Test
	void aParadaEhDeTodosEOJogoVoltaQuandoTodosFecham() {
		ateAoVivo();
		// sem lesão que pare o jogo neste teste: só a substituição
		jdbc.sql("UPDATE jogo_rodada_ao_vivo SET lesao_ate = 1e9 WHERE jogo_id = :jogo").param("jogo", jogo).update();
		Instant t = comAPartidaEmAndamento(admin);
		EstadoRodada antes = aoVivo.estado(jogo, admin.id(), t).orElseThrow();
		long meu = antes.jogoDoUsuario().mandante().id();
		long outro = antes.partidas().stream().filter(p -> !p.doUsuario()).findFirst().orElseThrow().mandante().id();

		aoVivo.pausar(jogo, admin.id(), outro, t);
		assertThat(aoVivo.estado(jogo, admin.id(), t).orElseThrow().pausada()).as("ver o clube de outro não para").isFalse();

		aoVivo.pausar(jogo, admin.id(), meu, t);
		EstadoRodada deMarcos = aoVivo.estado(jogo, marcos.id(), t.plusSeconds(20)).orElseThrow();
		assertThat(deMarcos.pausada()).as("parado para todos").isTrue();
		assertThat(deMarcos.minuto()).isEqualTo(antes.minuto());
		assertThat(deMarcos.pausa().descricao()).startsWith(admin.nome() + " (").endsWith("está fazendo substituição");
		assertThat(deMarcos.pausa().minha()).isFalse();
		assertThat(deMarcos.pausa().segundos()).isEqualTo(40);
		assertThat(deMarcos.janelaQueAbre()).isNull();
		EstadoRodada doAdmin = aoVivo.estado(jogo, admin.id(), t.plusSeconds(20)).orElseThrow();
		assertThat(doAdmin.pausa().minha()).isTrue();
		assertThat(doAdmin.janelaQueAbre()).as("a janela de quem parou abre sozinha").isEqualTo(meu);

		aoVivo.continuar(jogo, marcos.id(), t.plusSeconds(21));
		assertThat(aoVivo.estado(jogo, marcos.id(), t.plusSeconds(22)).orElseThrow().pausada())
			.as("quem não está na parada não faz o jogo voltar").isTrue();

		// Marcos abre um clube durante a parada e entra nela
		aoVivo.pausar(jogo, marcos.id(), outro, t.plusSeconds(23));
		var naParada = aoVivo.estado(jogo, marcos.id(), t.plusSeconds(23)).orElseThrow().pausa();
		assertThat(naParada.minha()).isTrue();
		assertThat(naParada.soQuemParou()).isFalse();
		assertThat(naParada.descricao()).contains(" e ").endsWith("estão mexendo nos times");
		assertThat(naParada.segundos()).as("o tempo é o de quem parou").isEqualTo(37);

		var janela = aoVivo.janela(jogo, admin.id(), meu, t.plusSeconds(25)).orElseThrow();
		var time = janela.doUsuario() == Lado.CASA ? janela.casa() : janela.fora();
		long sai = time.emCampo().stream().filter(j -> j.posicao() != Posicao.GOLEIRO).findFirst().orElseThrow().id();
		long entra = time.banco().stream().filter(j -> j.posicao() != Posicao.GOLEIRO).findFirst().orElseThrow().id();
		aoVivo.substituir(jogo, admin.id(), sai, entra, t.plusSeconds(25));

		aoVivo.continuar(jogo, admin.id(), t.plusSeconds(26));
		EstadoRodada soMarcos = aoVivo.estado(jogo, admin.id(), t.plusSeconds(27)).orElseThrow();
		assertThat(soMarcos.pausada()).as("Marcos ainda está mexendo: segue parado").isTrue();
		assertThat(soMarcos.pausa().minha()).isFalse();
		assertThat(soMarcos.pausa().descricao()).startsWith(marcos.nome()).endsWith("está mexendo nos times");
		assertThat(soMarcos.janelaQueAbre()).as("quem saiu não tem a janela reaberta").isNull();

		aoVivo.continuar(jogo, marcos.id(), t.plusSeconds(30));
		EstadoRodada voltou = aoVivo.estado(jogo, marcos.id(), t.plusSeconds(30)).orElseThrow();
		assertThat(voltou.pausada()).as("todos fecharam: o jogo volta").isFalse();
		assertThat(voltou.pausa()).isNull();
		assertThat(voltou.minuto()).isEqualTo(antes.minuto());
		var trocas = aoVivo.janela(jogo, admin.id(), meu, t.plusSeconds(31)).orElseThrow();
		assertThat((trocas.doUsuario() == Lado.CASA ? trocas.casa() : trocas.fora()).substituicoesFeitas())
			.as("a troca feita na parada vale").isEqualTo(1);
	}

	@Test
	void aos60sDeQuemParouOJogoVoltaParaTodos() {
		ateAoVivo();
		jdbc.sql("UPDATE jogo_rodada_ao_vivo SET lesao_ate = 1e9 WHERE jogo_id = :jogo").param("jogo", jogo).update();
		Instant t = comAPartidaEmAndamento(admin);
		EstadoRodada antes = aoVivo.estado(jogo, admin.id(), t).orElseThrow();
		long meu = antes.jogoDoUsuario().mandante().id();

		aoVivo.pausar(jogo, admin.id(), meu, t);
		aoVivo.pausar(jogo, marcos.id(), meu, t.plusSeconds(50));
		assertThat(aoVivo.pausaEmRede(jogo, admin.id(), t.plusSeconds(59))).isPresent();

		EstadoRodada depois = aoVivo.estado(jogo, marcos.id(), t.plusSeconds(60)).orElseThrow();
		assertThat(depois.pausada()).as("os 60 s acabaram: o jogo volta mesmo com os dois na parada").isFalse();
		assertThat(depois.pausa()).isNull();
		assertThat(depois.minuto()).isEqualTo(antes.minuto());
		assertThat(aoVivo.pausaEmRede(jogo, marcos.id(), t.plusSeconds(61))).isEmpty();
		assertThat(aoVivo.estado(jogo, admin.id(), t.plusSeconds(62)).orElseThrow().minuto()).isGreaterThan(antes.minuto());
		var janela = aoVivo.janela(jogo, admin.id(), meu, t.plusSeconds(62)).orElseThrow();
		var time = janela.doUsuario() == Lado.CASA ? janela.casa() : janela.fora();
		long sai = time.emCampo().stream().filter(j -> j.posicao() != Posicao.GOLEIRO).findFirst().orElseThrow().id();
		long entra = time.banco().getFirst().id();
		assertThatIllegalArgumentException().isThrownBy(() -> aoVivo.substituir(jogo, admin.id(), sai, entra, t.plusSeconds(62)))
			.withMessageContaining("voltou a andar");

		aoVivo.pausar(jogo, admin.id(), meu, t.plusSeconds(63));
		assertThat(aoVivo.estado(jogo, marcos.id(), t.plusSeconds(63)).orElseThrow().pausada())
			.as("quem parou pode parar de novo").isTrue();
	}

	/** Um instante com a partida do treinador em andamento, no começo do jogo. */
	private Instant comAPartidaEmAndamento(Usuario treinador) {
		for (int s = 1; s < 600; s += 2) {
			Instant t = AGORA.plusSeconds(s);
			EstadoRodada e = aoVivo.estado(jogo, treinador.id(), t).orElseThrow();
			if (e.comUsuario() && e.partidas().stream().anyMatch(p -> p.doUsuario()) && e.minuto() >= 5 && e.minuto() < 60) {
				return t;
			}
		}
		throw new AssertionError("a partida do treinador não começou");
	}

	/** Todos dão OK em tudo até a rodada 1 começar ao vivo. */
	private Desfecho ateAoVivo() {
		turnos.darOk(jogo, admin.id(), Passo.PLANTEL, 0, AGORA);
		Desfecho desfecho = turnos.darOk(jogo, marcos.id(), Passo.PLANTEL, 0, AGORA);
		assertThat(turnos.okDado(jogo, admin.id())).as("os OK são apagados quando o jogo segue").isFalse();
		if (desfecho == Desfecho.LANCES) {
			for (Usuario u : new Usuario[] { admin, marcos }) {
				if (!turnos.okDado(jogo, u.id())) {
					desfecho = turnos.darLances(jogo, u.id(), 1, Map.of(), AGORA);
				}
			}
		}
		if (desfecho == Desfecho.RESULTADO) {
			assertThat(turnos.turno(jogo).orElseThrow().passo()).isEqualTo(Passo.RESULTADO);
			turnos.darOk(jogo, admin.id(), Passo.RESULTADO, 0, AGORA);
			desfecho = turnos.darOk(jogo, marcos.id(), Passo.RESULTADO, 0, AGORA);
		}
		return desfecho;
	}

	@Test
	void lancesSecretosEsperamTodosEOsLeiloesSaoResolvidosDeUmaVez() {
		long jogadorAdmin = plantel.visao(jogo, admin.id()).orElseThrow().jogadores().stream()
			.filter(l -> l.lugar() == Lugar.FORA).findFirst().orElseThrow().jogador().id();
		long leilao = leiloes.venderPorLeilao(jogo, admin.id(), jogadorAdmin, 1);
		long clubeMarcos = jogos.clubeDoTreinador(jogo, marcos.id()).orElseThrow();
		long caixaMarcos = plantel.visao(jogo, marcos.id()).orElseThrow().saldo();

		turnos.darOk(jogo, admin.id(), Passo.PLANTEL, 0, AGORA);
		assertThat(turnos.darOk(jogo, marcos.id(), Passo.PLANTEL, 0, AGORA)).isEqualTo(Desfecho.LANCES);
		assertThat(turnos.turno(jogo).orElseThrow().passo()).isEqualTo(Passo.LANCES);
		assertThatIllegalArgumentException().as("ninguém dá lance no próprio jogador")
			.isThrownBy(() -> turnos.darLances(jogo, admin.id(), 1, Map.of(leilao, 1_000L), AGORA));

		long lance = caixaMarcos / 100 / 2;
		Desfecho depois = turnos.darLances(jogo, marcos.id(), 1, Map.of(leilao, lance), AGORA);
		if (depois == Desfecho.AGUARDANDO) {
			assertThat(situacao(leilao)).as("só resolve com os lances de todos").isEqualTo("ABERTO");
			assertThat(contar("SELECT count(*) FROM jogo_rede_lance WHERE leilao_id = :id", leilao)).isEqualTo(1);
			assertThatIllegalArgumentException().as("com o OK dado, mudar exige tirar o OK")
				.isThrownBy(() -> turnos.darLances(jogo, marcos.id(), 1, Map.of(), AGORA));
			depois = turnos.darLances(jogo, admin.id(), 1, Map.of(), AGORA);
		}

		assertThat(depois).isEqualTo(Desfecho.RESULTADO);
		assertThat(situacao(leilao)).isNotEqualTo("ABERTO");
		assertThat(contar("SELECT count(*) FROM jogo_rede_lance WHERE leilao_id = :id", leilao)).isZero();
		assertThat(contar("SELECT count(*) FROM jogo_leilao_lance WHERE leilao_id = :id AND clube_id = " + clubeMarcos, leilao))
			.as("o lance guardado passa para o resultado").isEqualTo(1);
	}

	@Test
	void okDeOutroPassoEhRecusado() {
		assertThatIllegalStateException().isThrownBy(() -> turnos.darOk(jogo, admin.id(), Passo.RESULTADO, 0, AGORA));
		assertThatIllegalStateException().isThrownBy(() -> turnos.darLances(jogo, admin.id(), 1, Map.of(), AGORA));
	}

	private String situacao(long leilao) {
		return jdbc.sql("SELECT situacao FROM jogo_leilao WHERE id = :id").param("id", leilao).query(String.class).single();
	}

	private long contar(String sql, long id) {
		return jdbc.sql(sql).param("id", id).query(Long.class).single();
	}
}
