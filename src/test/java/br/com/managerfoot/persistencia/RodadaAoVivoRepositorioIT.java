package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Lugar;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.TipoEvento;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio.EstadoRodada;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio.Situacao;
import br.com.managerfoot.temporada.RodadaAoVivo;
import br.com.managerfoot.temporada.Velocidade;

/** Migração V13 e {@link RodadaAoVivoRepositorio}, com o relógio controlado pelo teste. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RodadaAoVivoRepositorioIT {

	/** Treinador na Série B (exibida primeiro); a Série A vem depois, sem usuário. */
	private static final Instant T0 = Instant.parse("2026-10-28T20:00:00Z");

	@Autowired
	RodadaAoVivoRepositorio aoVivo;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	EscalacaoRepositorio escalacoes;

	@Autowired
	PlantelRepositorio plantel;

	@Autowired
	JdbcClient jdbc;

	long jogo;
	long usuario;
	long clube;

	@BeforeEach
	void jogoComTemporada() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		temporadas.iniciarPrimeiraTemporada(jogo, 9);
		usuario = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = :j").param("j", jogo).query(Long.class)
			.single();
		clube = jogos.clubeDoTreinador(jogo, usuario).orElseThrow();
	}

	@Test
	void relogioAndaPeloTempoNaVelocidadeDoUsuario() {
		aoVivo.iniciar(jogo, usuario, 1, T0);

		EstadoRodada estado = estado(10);

		assertThat(estado.divisoes()).extracting(RodadaAoVivoRepositorio.DivisaoNaRodada::nome)
			.containsExactly("Série B", "Série A");
		assertThat(estado.divisoes()).extracting(RodadaAoVivoRepositorio.DivisaoNaRodada::situacao)
			.containsExactly(Situacao.JOGANDO, Situacao.AGUARDANDO);
		assertThat(estado.minuto()).isEqualTo(20);
		assertThat(estado.comUsuario()).isTrue();
		assertThat(estado.velocidade()).isEqualTo(Velocidade.NORMAL);
		assertThat(estado.partidas()).hasSize(7);
		assertThat(estado.partidas()).filteredOn(RodadaAoVivoRepositorio.PlacarAoVivo::doUsuario).hasSizeLessThanOrEqualTo(1);
	}

	@Test
	void pausaParaORelogioEContinuarRetoma() {
		aoVivo.iniciar(jogo, usuario, 1, T0);
		aoVivo.pausar(jogo, usuario, T0.plusSeconds(10));

		assertThat(estado(60).minuto()).isEqualTo(20);
		assertThat(estado(60).pausada()).isTrue();

		aoVivo.continuar(jogo, usuario, T0.plusSeconds(60));
		assertThat(estado(61).minuto()).isEqualTo(22);
		assertThat(estado(61).pausada()).isFalse();
	}

	@Test
	void trocarVelocidadeGuardaOMinutoEValeParaAsProximasRodadas() {
		aoVivo.iniciar(jogo, usuario, 1, T0);
		aoVivo.mudarVelocidade(jogo, usuario, Velocidade.RAPIDA, T0.plusSeconds(10));

		assertThat(estado(11).minuto()).isEqualTo(25);
		assertThat(estado(11).velocidade()).isEqualTo(Velocidade.RAPIDA);
	}

	@Test
	void divisaoSemUsuarioNaoPausa() {
		aoVivo.iniciar(jogo, usuario, 1, T0);
		// Série B inteira em 45 s (normal) e mais 1 s na Série A (20 minutos a 0,05 s)
		aoVivo.pausar(jogo, usuario, T0.plusSeconds(46));

		EstadoRodada estado = estado(47);
		assertThat(estado.divisaoAtual()).isEqualTo(1);
		assertThat(estado.comUsuario()).isFalse();
		assertThat(estado.pausada()).isFalse();
		assertThat(estado.minuto()).isEqualTo(40);
	}

	@Test
	void fimGravaARodadaUmaVezSo() {
		aoVivo.iniciar(jogo, usuario, 1, T0);
		aoVivo.iniciar(jogo, usuario, 1, T0.plusSeconds(1));

		EstadoRodada fim = estado(3_600);

		assertThat(fim.terminada()).isTrue();
		assertThat(fim.divisoes()).allMatch(d -> d.situacao() == Situacao.TERMINADA);
		assertThat(rodadas.rodadasProcessadas(jogo)).isEqualTo(1);
		assertThat(aoVivo.estado(jogo, usuario, T0.plusSeconds(3_601))).isEmpty();
		assertThat(aoVivo.emAndamento(jogo)).isFalse();
		assertThatIllegalStateException().isThrownBy(() -> aoVivo.iniciar(jogo, usuario, 1, T0.plusSeconds(3_700)));
		assertThatIllegalStateException().isThrownBy(() -> aoVivo.iniciar(jogo, usuario, 3, T0.plusSeconds(3_700)));
	}

	@Test
	void publicoAoVivoEOMesmoGravadoNoFimDaRodada() {
		aoVivo.iniciar(jogo, usuario, 1, T0);
		Map<Long, Integer> aoVivoPorMandante = new HashMap<>();
		estado(10).partidas().forEach(p -> aoVivoPorMandante.put(p.mandante().id(), p.publico()));

		estado(3_600);

		assertThat(aoVivoPorMandante).isNotEmpty().allSatisfy((mandante, publico) -> {
			assertThat(publico).isPositive();
			assertThat(jdbc.sql("SELECT publico FROM jogo_partida WHERE rodada = 1 AND mandante_id = :m")
				.param("m", mandante).query(Integer.class).single()).isEqualTo(publico);
		});
	}

	@Test
	void substituicaoSoComAPartidaPausadaEEntraNoJogo() {
		aoVivo.iniciar(jogo, usuario, 1, T0);
		EscalacaoDoTreinador escalacao = escalacoes.carregar(clube).orElseThrow();
		List<Jogador> doClube = jogos.carregarPlanteis(jogo).get(clube);
		Jogador sai = doClube.stream().filter(j -> escalacao.titulares().contains(j.id()) && j.posicao() == Posicao.ATACANTE)
			.findFirst().orElseThrow();
		Jogador entra = doClube.stream().filter(j -> escalacao.reservas().contains(j.id()) && j.posicao() == Posicao.ATACANTE)
			.findFirst().orElseThrow();

		assertThatIllegalArgumentException().isThrownBy(() -> aoVivo.substituir(jogo, usuario, sai.id(), entra.id(), T0))
			.withMessage("Pause a partida para substituir.");
		assertThatIllegalArgumentException().isThrownBy(() -> plantel.mover(jogo, usuario, sai.id(), Lugar.FORA))
			.withMessageContaining("em andamento");

		aoVivo.pausar(jogo, usuario, T0.plusSeconds(10));
		aoVivo.substituir(jogo, usuario, sai.id(), entra.id(), T0.plusSeconds(11));
		assertThatIllegalArgumentException()
			.isThrownBy(() -> aoVivo.substituir(jogo, usuario, sai.id(), entra.id(), T0.plusSeconds(12)))
			.withMessage("Quem sai precisa estar em campo.");
		assertThat(jdbc.sql("SELECT minuto FROM jogo_substituicao_ao_vivo WHERE jogo_id = :j").param("j", jogo)
			.query(Integer.class).single()).isEqualTo(20);

		aoVivo.continuar(jogo, usuario, T0.plusSeconds(20));
		assertThat(estado(3_600).terminada()).isTrue();
		assertThat(rodadas.rodadasProcessadas(jogo)).isEqualTo(1);
	}

	@Test
	void outroUsuarioNaoVeARodada() {
		aoVivo.iniciar(jogo, usuario, 1, T0);
		long outro = LigaDeTeste.participante(contas).id();

		assertThatThrownBy(() -> aoVivo.estado(jogo, outro, T0))
			.isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void lesaoNoClubeDoUsuarioParaARodadaEAbreAJanelaComOLesionado() {
		EventoPartida lesao = sementeComLesaoDoUsuarioNaRodada1();
		aoVivo.iniciar(jogo, usuario, 1, T0);

		// bem depois do minuto da lesão: o relógio voltou para ele e parou
		EstadoRodada parada = estado(200);
		assertThat(parada.pausada()).isTrue();
		assertThat(parada.minuto()).isEqualTo(lesao.minuto());
		long mandante = parada.jogoDoUsuario().mandante().id();
		assertThat(parada.janelaLesao()).isEqualTo(mandante);
		RodadaAoVivoRepositorio.JanelaJogo janela = aoVivo.janela(jogo, usuario, mandante, null, null, T0.plusSeconds(200))
			.orElseThrow();
		RodadaAoVivoRepositorio.TimeNaJanela meu = janela.doUsuario() == Lado.CASA ? janela.casa() : janela.fora();
		assertThat(meu.lesionado().id()).isEqualTo(lesao.jogador().id());
		assertThat(janela.lances()).anyMatch(e -> e.tipo() == TipoEvento.LESAO);

		// "Continuar" sem trocar: a máquina troca e a rodada não para de novo na mesma lesão
		// na velocidade normal (0,5 s por minuto), 0,6 s depois é o minuto seguinte
		Instant continuou = T0.plusSeconds(201);
		aoVivo.continuar(jogo, usuario, continuou);
		Instant minutoSeguinte = continuou.plusMillis(600);
		EstadoRodada seguinte = aoVivo.estado(jogo, usuario, minutoSeguinte).orElseThrow();
		assertThat(seguinte.janelaLesao()).isNull();
		assertThat(seguinte.pausada()).isFalse();
		assertThat(seguinte.minuto()).isEqualTo(lesao.minuto() + 1);
		RodadaAoVivoRepositorio.JanelaJogo depois = aoVivo.janela(jogo, usuario, mandante, null, null, minutoSeguinte)
			.orElseThrow();
		RodadaAoVivoRepositorio.TimeNaJanela meuDepois = depois.doUsuario() == Lado.CASA ? depois.casa() : depois.fora();
		assertThat(meuDepois.lesionado()).isNull();
		assertThat(meuDepois.emCampo()).noneMatch(j -> j.id() == lesao.jogador().id());
	}

	/**
	 * Recria o jogo com a primeira semente de temporada em que o clube do usuário tem uma lesão na
	 * rodada 1 (antes do minuto 88, para o minuto seguinte ainda ser da mesma divisão), e devolve a lesão.
	 */
	private EventoPartida sementeComLesaoDoUsuarioNaRodada1() {
		for (long semente = 0; semente < 400; semente++) {
			jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
			temporadas.iniciarPrimeiraTemporada(jogo, semente);
			usuario = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = :j").param("j", jogo)
				.query(Long.class).single();
			clube = jogos.clubeDoTreinador(jogo, usuario).orElseThrow();
			RodadaAoVivo rodada = rodadas.preparar(jogo, 1, List.of());
			for (RodadaAoVivo.PartidaDaRodada p : rodada.partidas()) {
				Lado lado = p.jogo().mandante().id() == clube ? Lado.CASA
						: p.jogo().visitante().id() == clube ? Lado.FORA : null;
				if (lado == null) {
					continue;
				}
				var lesao = rodada.noMinuto(p, Integer.MAX_VALUE).resultado().eventos().stream()
					.filter(e -> e.tipo() == TipoEvento.LESAO && e.lado() == lado && e.minuto() < 88)
					.findFirst();
				if (lesao.isPresent()) {
					return lesao.get();
				}
			}
		}
		throw new AssertionError("nenhuma semente com lesão do usuário na rodada 1");
	}

	private EstadoRodada estado(long segundos) {
		return aoVivo.estado(jogo, usuario, T0.plusSeconds(segundos)).orElseThrow();
	}
}
