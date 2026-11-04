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
import br.com.managerfoot.economia.PropostasPatrocinio;
import br.com.managerfoot.multiplayer.Turno;
import br.com.managerfoot.multiplayer.Turno.Passo;
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
