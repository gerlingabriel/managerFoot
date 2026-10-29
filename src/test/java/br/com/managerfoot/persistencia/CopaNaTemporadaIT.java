package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.temporada.ConfrontoCopa;
import br.com.managerfoot.temporada.Copa;
import br.com.managerfoot.temporada.FaseCopa;

/** As etapas da copa jogadas entre as rodadas da liga (SPEC §8.2, rev. 70) contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CopaNaTemporadaIT {

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	FimDeTemporadaRepositorio fins;

	@Autowired
	CopaRepositorio copas;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	ConsultasRepositorio consultas;

	@Autowired
	CaixaRepositorio caixas;

	@Autowired
	JdbcClient jdbc;

	long jogo;
	long temporada;

	@BeforeEach
	void criaJogoComTemporada() {
		// 16 + 15 clubes: copa das oitavas, depois das rodadas 10 e 12
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		temporada = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :j").param("j", jogo).query(Long.class).single();
	}

	private void jogarAte(int rodada) {
		for (int r = rodadas.rodadasProcessadas(jogo) + 1; r <= rodada; r++) {
			RodadasDeTeste.jogarRodada(rodadas, jogo, r);
		}
	}

	@Test
	void depoisDaRodada10VemAIdaDasOitavasEARodada11Espera() {
		jogarAte(10);

		assertThat(rodadas.proximo(jogo)).isEqualTo(new RodadaRepositorio.Proximo.EtapaCopa(0,
				new Copa.Etapa(10, FaseCopa.OITAVAS, Copa.Perna.IDA)));
		assertThatIllegalStateException().isThrownBy(() -> rodadas.avancar(jogo, 11)).withMessageContaining("copa");
		assertThatIllegalStateException().isThrownBy(() -> rodadas.avancarCopa(jogo, 1, List.of()));

		RodadaRepositorio.EtapaJogada ida = rodadas.avancarCopa(jogo, 0, List.of());

		assertThat(ida.resultado().partidas()).hasSize(8);
		assertThat(rodadas.proximo(jogo)).isEqualTo(new RodadaRepositorio.Proximo.Liga(11));
		assertThat(copas.carregar(jogo, temporada).orElseThrow().confrontosDa(FaseCopa.OITAVAS))
			.allMatch(c -> c.ida().isPresent() && c.volta().isEmpty());
		// a mesma etapa não é jogada de novo
		assertThatIllegalStateException().isThrownBy(() -> rodadas.avancarCopa(jogo, 0, List.of()));
	}

	@Test
	void calendarioDoClubeMostraOsJogosDaCopaDepoisDaRodada() {
		jogarAte(10);
		rodadas.avancarCopa(jogo, 0, List.of());
		ConfrontoCopa confronto = copas.carregar(jogo, temporada).orElseThrow().confrontosDa(FaseCopa.OITAVAS).getFirst();
		long dono = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = :j").param("j", jogo)
			.query(Long.class).single();

		List<ConsultasRepositorio.JogoNoCalendario> calendario = consultas
			.calendario(jogo, dono, confronto.visitante().id()).orElseThrow().jogos();

		// modelo 12d: depois da rodada 10, a ida; depois da 12, a volta, com o placar da ida embaixo
		int ida = calendario.indexOf(calendario.stream().filter(j -> j.copa() != null).findFirst().orElseThrow());
		assertThat(calendario.get(ida - 1).rodada()).isEqualTo(10);
		assertThat(calendario.get(ida - 1).copa()).isNull();
		ConsultasRepositorio.JogoNoCalendario jogoDaIda = calendario.get(ida);
		assertThat(jogoDaIda.copa()).isEqualTo("Copa · Oitavas de final · ida");
		assertThat(jogoDaIda.emCasa()).isFalse();
		assertThat(jogoDaIda.adversario()).isEqualTo(confronto.mandante());
		assertThat(jogoDaIda.golsPro()).isEqualTo(confronto.ida().orElseThrow().golsVisitante());
		ConsultasRepositorio.JogoNoCalendario volta = calendario.stream()
			.filter(j -> "Copa · Oitavas de final · volta".equals(j.copa())).findFirst().orElseThrow();
		assertThat(volta.emCasa()).isTrue();
		assertThat(volta.golsPro()).isNull();
		assertThat(volta.detalhe()).isEqualTo("ida " + confronto.ida().get().golsVisitante() + " x "
				+ confronto.ida().get().golsMandante());
		assertThat(calendario.get(calendario.indexOf(volta) - 1).rodada()).isEqualTo(12);
		// a próxima é a rodada 11 da liga
		assertThat(calendario.stream().filter(ConsultasRepositorio.JogoNoCalendario::proximo).toList())
			.singleElement().matches(j -> j.copa() == null && j.rodada() == 11);
	}

	@Test
	void extratoTemALinhaDaCopaDepoisDaRodadaSemSalario() {
		jogarAte(10);
		ConfrontoCopa confronto = copas.carregar(jogo, temporada).orElseThrow().confrontosDa(FaseCopa.OITAVAS).getFirst();
		// o usuário passa a dirigir quem joga a ida em casa (só para ler o extrato desse clube)
		jdbc.sql("UPDATE jogo_treinador SET clube_id = :c WHERE jogo_id = :j").param("c", confronto.mandante().id())
			.param("j", jogo).update();
		long dono = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = :j").param("j", jogo)
			.query(Long.class).single();
		rodadas.avancarCopa(jogo, 0, List.of());

		CaixaRepositorio.VisaoFinancas f = caixas.financas(jogo, dono);

		// modelo 12f: a linha "Copa" vem logo depois da rodada 10, com bilheteria e sem salário
		List<CaixaRepositorio.LinhaExtrato> extrato = f.extrato();
		CaixaRepositorio.LinhaExtrato copa = extrato.stream().filter(l -> l.copa() != null).findFirst().orElseThrow();
		CaixaRepositorio.LinhaExtrato antes = extrato.get(extrato.indexOf(copa) - 1);
		assertThat(antes.rodada()).isEqualTo(10);
		assertThat(antes.copa()).isNull();
		assertThat(antes.salarios()).isNegative();
		assertThat(copa.copa()).isEqualTo("Oitavas de final · ida");
		assertThat(copa.emCasa()).isTrue();
		assertThat(copa.adversario()).isEqualTo(confronto.visitante());
		assertThat(copa.publico()).isPositive();
		assertThat(copa.bilheteria()).isPositive();
		assertThat(copa.salarios()).isZero();
		assertThat(copa.saldo()).isEqualTo(antes.saldo() + copa.resultado());
		assertThat(f.temCopa()).isTrue();
		assertThat(f.copaBilheteria()).isEqualTo(copa.bilheteria());
		assertThat(f.copaResultado()).isEqualTo(copa.resultado());
		assertThat(extrato.getLast().saldo()).isEqualTo(f.saldo());
	}

	@Test
	void etapaDaCopaTemBilheteriaSemSalarioEContaGolsECartoes() {
		jogarAte(10);
		long salariosAntes = soma("SALARIOS");
		rodadas.avancarCopa(jogo, 0, List.of());

		assertThat(soma("SALARIOS")).isEqualTo(salariosAntes);
		long bilheterias = jdbc.sql("""
				SELECT count(*) FROM jogo_lancamento WHERE temporada_id = :t AND competicao = 'COPA' AND tipo = 'BILHETERIA'""")
			.param("t", temporada).query(Long.class).single();
		assertThat(bilheterias).isEqualTo(8);
		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_partida WHERE temporada_id = :t AND perna = 'IDA' AND publico > 0
				  AND gols_mandante IS NOT NULL""").param("t", temporada).query(Long.class).single()).isEqualTo(8);
		long golsGravados = jdbc.sql("""
				SELECT count(*) FROM jogo_evento e JOIN jogo_partida p ON p.id = e.partida_id
				WHERE p.temporada_id = :t AND p.confronto_id IS NOT NULL AND e.tipo = 'GOL'""")
			.param("t", temporada).query(Long.class).single();
		long golsNoPlacar = jdbc.sql("""
				SELECT coalesce(sum(gols_mandante + gols_visitante), 0) FROM jogo_partida
				WHERE temporada_id = :t AND confronto_id IS NOT NULL""").param("t", temporada).query(Long.class).single();
		assertThat(golsGravados).isEqualTo(golsNoPlacar);
		// a classificação da liga não muda com a copa
		assertThat(rodadas.classificacao(jogo).get(1).linhas()).allMatch(l -> l.jogos() == 10);
	}

	@Test
	void temporadaInteiraTerminaComCampeaoPremioEHistorico() {
		int total = rodadas.totalDeRodadas(jogo);
		jogarAte(total - 1);
		rodadas.avancar(jogo, total);
		// a final ainda falta: a temporada não termina
		assertThat(rodadas.proximo(jogo)).isInstanceOf(RodadaRepositorio.Proximo.EtapaCopa.class);
		assertThatIllegalStateException().isThrownBy(() -> fins.avancarTemporada(jogo, 1, 3)).withMessageContaining("Final");

		RodadasDeTeste.jogarCopaPendente(rodadas, jogo);

		assertThat(rodadas.proximo(jogo)).isEqualTo(new RodadaRepositorio.Proximo.Fim());
		Copa copa = copas.carregar(jogo, temporada).orElseThrow();
		assertThat(copa.campeao()).isPresent();
		ConfrontoCopa decisao = copa.confrontosDa(FaseCopa.FINAL).getFirst();
		assertThat(decisao.ida()).isPresent();
		long campeao = copa.campeao().get().id();
		long vice = copa.vice().orElseThrow().id();
		assertThat(premio(campeao)).isEqualTo(3_968_000_00L);
		assertThat(premio(vice)).isEqualTo(1_984_000_00L);
		assertThat(jdbc.sql("SELECT clube_id FROM jogo_campeao_copa WHERE jogo_id = :j AND temporada = 1")
			.param("j", jogo).query(Long.class).single()).isEqualTo(campeao);
		// final em campo neutro: bilheteria para os dois
		assertThat(jdbc.sql("""
				SELECT count(DISTINCT clube_id) FROM jogo_lancamento
				WHERE temporada_id = :t AND competicao = 'COPA' AND tipo = 'BILHETERIA' AND rodada = :r""")
			.param("t", temporada).param("r", total).query(Long.class).single()).isEqualTo(2);

		// a coluna Copa do Histórico (modelo 12e, rev. 71) com o placar da final
		long dono = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = :j").param("j", jogo)
			.query(Long.class).single();
		ConsultasRepositorio.VisaoHistorico historico = consultas.historico(jogo, dono);
		assertThat(historico.temCopa()).isTrue();
		ConsultasRepositorio.CampeaoNoAno naCopa = historico.anos().getFirst().copa();
		assertThat(naCopa.clube()).isEqualTo(copa.campeao().get().nome());
		assertThat(naCopa.detalhe()).startsWith("final ").endsWith(" sobre " + copa.vice().get().nome());

		fins.avancarTemporada(jogo, 1, 3);
		long nova = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :j AND numero = 2").param("j", jogo)
			.query(Long.class).single();
		assertThat(copas.carregar(jogo, nova)).isPresent();
	}

	private long soma(String tipo) {
		return jdbc.sql("SELECT coalesce(sum(valor), 0) FROM jogo_lancamento WHERE temporada_id = :t AND tipo = :tipo")
			.param("t", temporada).param("tipo", tipo).query(Long.class).single();
	}

	private long premio(long clube) {
		return jdbc.sql("""
				SELECT valor FROM jogo_lancamento
				WHERE temporada_id = :t AND clube_id = :c AND competicao = 'COPA' AND tipo = 'PREMIO'""")
			.param("t", temporada).param("c", clube).query(Long.class).single();
	}
}
