package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.economia.MetaPatrocinio;
import br.com.managerfoot.economia.PropostasPatrocinio;
import br.com.managerfoot.economia.PropostasPatrocinio.Tipo;

/** Migração V43: a meta do patrocínio paga na rodada em que fica garantida (SPEC §9.6, rev. 97; modelo 20a). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MetaGarantidaIT {

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	FimDeTemporadaRepositorio fim;

	@Autowired
	PatrocinioRepositorio patrocinios;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JdbcClient jdbc;

	long jogo;
	long clube;
	long usuario;

	@BeforeEach
	void jogoComMetaDeUmaVitoria() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		usuario = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = ?").param(jogo).query(Long.class).single();
		clube = jogos.clubeDoTreinador(jogo, usuario).orElseThrow();
		// a liga de teste não cria a oferta: o contrato com meta de 1 vitória vai direto
		jdbc.sql("""
				INSERT INTO jogo_patrocinio (clube_id, temporada_id, tabela, jogos, escolha, meta_vitorias, meta_posicao)
				SELECT :c, id, 100000000, 30, 'META_VITORIAS', 1, 1 FROM jogo_temporada WHERE jogo_id = :j AND numero = 1""")
			.param("c", clube).param("j", jogo).update();
	}

	@Test
	void pagaNaRodadaDaPrimeiraVitoriaEUmaVezSo() {
		long tabela = jdbc.sql("SELECT tabela FROM jogo_patrocinio WHERE clube_id = :c").param("c", clube).query(Long.class).single();
		long bonus = PropostasPatrocinio.padrao().propostasComMeta(tabela).stream().filter(p -> p.tipo() == Tipo.META_VITORIAS)
			.findFirst().orElseThrow().bonus();
		assertThat(patrocinios.meta(jogo, usuario)).hasValueSatisfying(m -> {
			assertThat(m.situacao().estado()).isEqualTo(MetaPatrocinio.Estado.EM_ANDAMENTO);
			assertThat(m.pagaNaRodada()).isNull();
		});

		Integer paga = null;
		for (int rodada = 1; rodada <= 30; rodada++) {
			RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
			RodadasDeTeste.jogarRodada(rodadas, jogo, rodada);
			paga = jdbc.sql("SELECT meta_paga_rodada FROM jogo_patrocinio WHERE clube_id = :c").param("c", clube)
				.query(Integer.class).optional().orElse(null);
			if (paga != null) {
				break;
			}
		}
		assertThat(paga).as("o clube vence alguma vez em 30 rodadas").isNotNull();
		int rodadaPaga = paga;
		assertThat(vitoriasAte(rodadaPaga)).isEqualTo(1);
		assertThat(patrocinioDaTemporada1()).containsExactly(bonus);
		assertThat(patrocinios.meta(jogo, usuario)).hasValueSatisfying(m -> {
			assertThat(m.situacao().estado()).isEqualTo(MetaPatrocinio.Estado.GARANTIDA);
			assertThat(m.pagaNaRodada()).isEqualTo(rodadaPaga);
		});

		for (int rodada = rodadaPaga + 1; rodada <= 30; rodada++) {
			RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
			RodadasDeTeste.jogarRodada(rodadas, jogo, rodada);
		}
		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
		fim.avancarTemporada(jogo, 1, 7);

		assertThat(patrocinioDaTemporada1()).as("o fim da temporada não paga de novo").containsExactly(bonus);
	}

	private long vitoriasAte(int rodada) {
		return jdbc.sql("""
				SELECT count(*) FROM jogo_partida p JOIN jogo_temporada t ON t.id = p.temporada_id
				WHERE t.jogo_id = :jogo AND t.numero = 1 AND p.rodada <= :rodada AND p.divisao_id IS NOT NULL
				  AND ((p.mandante_id = :c AND p.gols_mandante > p.gols_visitante)
				    OR (p.visitante_id = :c AND p.gols_visitante > p.gols_mandante))""")
			.param("jogo", jogo).param("rodada", rodada).param("c", clube).query(Long.class).single();
	}

	private java.util.List<Long> patrocinioDaTemporada1() {
		return jdbc.sql("""
				SELECT l.valor FROM jogo_lancamento l JOIN jogo_temporada t ON t.id = l.temporada_id
				WHERE l.clube_id = :c AND l.tipo = 'PATROCINIO' AND t.numero = 1 AND l.rodada > 0""")
			.param("c", clube).query(Long.class).list();
	}
}
