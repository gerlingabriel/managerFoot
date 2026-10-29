package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.dominio.Jogador;

/** Migração V12, {@link EscalacaoRepositorio} e a rodada com a escalação do treinador. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EscalacaoRepositorioIT {

	@Autowired
	EscalacaoRepositorio escalacoes;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	TransactionTemplate transacao;

	long jogo;
	long clube;
	List<Jogador> plantel;

	@BeforeEach
	void jogoComTemporada() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		temporadas.iniciarPrimeiraTemporada(jogo, 5);
		clube = jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo").param("jogo", jogo)
			.query(Long.class).single();
		plantel = jogos.carregarPlanteis(jogo).get(clube);
	}

	@Test
	void jogoNovoJaTemAEscalacaoDaMaquinaParaOTreinadorESoParaEle() {
		EscalacaoDoTreinador escalacao = escalacoes.carregar(clube).orElseThrow();

		assertThat(escalacao.problemas(plantel, id -> false)).isEmpty();
		assertThat(escalacao.reservas()).hasSize(7);
		assertThat(escalacoes.carregarDoJogo(jogo)).containsOnlyKeys(clube);
	}

	@Test
	void gravaECarregaInclusiveIncompletaERecusaJogadorDeOutroClube() {
		EscalacaoDoTreinador escalacao = escalacoes.carregar(clube).orElseThrow();
		long batedor = escalacao.titulares().iterator().next();
		EscalacaoDoTreinador incompleta = new EscalacaoDoTreinador(escalacao.formacao(),
				escalacao.titulares().stream().skip(1).collect(Collectors.toSet()), Set.of(), batedor);

		escalacoes.gravar(clube, incompleta);

		assertThat(escalacoes.carregar(clube)).hasValue(incompleta);
		long deOutroClube = jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id <> :clube LIMIT 1").param("clube", clube)
			.query(Long.class).single();
		assertThatThrownBy(() -> escalacoes.gravar(clube,
				new EscalacaoDoTreinador(escalacao.formacao(), Set.of(deOutroClube), Set.of(), null)))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(escalacoes.carregar(clube)).hasValue(incompleta);
	}

	@Test
	void rodadaUsaAEscalacaoGravada() {
		EscalacaoDoTreinador escalacao = escalacoes.carregar(clube).orElseThrow();
		Set<Long> escalados = new HashSet<>(escalacao.titulares());
		escalados.addAll(escalacao.reservas());
		Set<Long> doClube = plantel.stream().map(Jogador::id).collect(Collectors.toSet());

		for (int rodada = 1; rodada <= 6; rodada++) {
			RodadasDeTeste.jogarRodada(rodadas, jogo, rodada);
			// recompõe o time se alguém foi suspenso, para a próxima rodada poder sair
			escalacoes.gravar(clube, escalacao);
			jdbc.sql("UPDATE jogo_jogador SET suspensao = 0 WHERE clube_id = :clube").param("clube", clube).update();
		}

		List<Long> nosEventos = jdbc.sql("""
				SELECT e.jogador_id FROM jogo_evento e JOIN jogo_partida p ON p.id = e.partida_id
				JOIN jogo_temporada t ON t.id = p.temporada_id WHERE t.jogo_id = :jogo""")
			.param("jogo", jogo).query(Long.class).list();
		assertThat(nosEventos.stream().filter(doClube::contains)).isNotEmpty().allMatch(escalados::contains);
	}

	@Test
	void escalacaoInvalidaNaoDeixaARodadaSair() {
		EscalacaoDoTreinador escalacao = escalacoes.carregar(clube).orElseThrow();
		escalacoes.gravar(clube, escalacao.sem(Set.of(escalacao.titulares().iterator().next())));

		assertThatIllegalStateException().isThrownBy(() -> rodadas.avancar(jogo, 1))
			.withMessageContaining("não pode jogar a rodada 1")
			.withMessageContaining("faltam 1");
		assertThat(rodadas.rodadasProcessadas(jogo)).isZero();
	}

	@Test
	void suspensoSaiDaEscalacaoEDoPenalti() {
		EscalacaoDoTreinador escalacao = escalacoes.carregar(clube).orElseThrow();
		long titular = escalacao.titulares().iterator().next();
		long reserva = escalacao.reservas().iterator().next();
		escalacoes.gravar(clube, new EscalacaoDoTreinador(escalacao.formacao(), escalacao.titulares(),
				escalacao.reservas(), titular));
		jdbc.sql("UPDATE jogo_jogador SET suspensao = 1 WHERE id IN (:ids)").param("ids", List.of(titular, reserva)).update();

		transacao.executeWithoutResult(s -> escalacoes.retirarSuspensos(jogo));

		EscalacaoDoTreinador depois = escalacoes.carregar(clube).orElseThrow();
		assertThat(depois).isEqualTo(escalacao.sem(Set.of(titular, reserva)));
		assertThat(depois.batedor()).isNull();
		assertThat(depois.titulares()).hasSize(10);
	}
}
