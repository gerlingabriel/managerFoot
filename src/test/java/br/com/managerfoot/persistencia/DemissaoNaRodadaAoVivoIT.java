package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.persistencia.RodadaAoVivoRepositorio.EstadoRodada;

/** O usuário demitido ao fim da rodada ao vivo muda de clube no meio do fechamento: a rodada não pode travar. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DemissaoNaRodadaAoVivoIT {

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
	JdbcClient jdbc;

	@MockitoSpyBean
	TrocaRepositorio trocas;

	@Test
	void fimDaRodadaGravaEMostraOJogoDoClubeAntigo() {
		long jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		temporadas.iniciarPrimeiraTemporada(jogo, 9);
		long usuario = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = :j").param("j", jogo)
			.query(Long.class).single();
		long antigo = jogos.clubeDoTreinador(jogo, usuario).orElseThrow();
		long novo = jdbc.sql("""
				SELECT id FROM jogo_clube WHERE jogo_id = :j AND id <> :c AND divisao_id IS NOT NULL ORDER BY id LIMIT 1""")
			.param("j", jogo).param("c", antigo).query(Long.class).single();
		doAnswer(i -> jdbc.sql("UPDATE jogo_treinador SET clube_id = :novo WHERE jogo_id = :j")
			.param("novo", novo).param("j", jogo).update()).when(AopTestUtils.<TrocaRepositorio>getTargetObject(trocas))
			.demitirDepoisDaRodada(anyLong(), anyLong(), anyInt(), any(), any());
		aoVivo.iniciar(jogo, usuario, 1, T0);

		EstadoRodada fim = aoVivo.estado(jogo, usuario, T0.plusSeconds(3_600)).orElseThrow();

		assertThat(fim.terminada()).isTrue();
		assertThat(rodadas.rodadasProcessadas(jogo)).isEqualTo(1);
		assertThat(jogos.clubeDoTreinador(jogo, usuario)).contains(novo);
		assertThat(fim.jogoDoUsuario()).isNotNull();
		assertThat(fim.jogoDoUsuario().mandante().id() == antigo || fim.jogoDoUsuario().visitante().id() == antigo).isTrue();
	}
}
