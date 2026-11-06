package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.temporada.BonusTreinador;
import br.com.managerfoot.temporada.ParametrosBonusTreinador;
import br.com.managerfoot.temporada.RodadaAoVivo;

/** Bônus do treinador (SPEC §5.3.2, rev. 121) contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BonusTreinadorIT {

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JdbcClient jdbc;

	long jogo;

	@BeforeEach
	void criaJogoComTemporada() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
	}

	@Test
	void oClubeDoUsuarioSorteiaNoInicioDaTemporadaPelaFaixaDaForcaEmCampo() {
		long clube = clubeDoUsuario();
		List<Jogador> plantel = jogos.carregarPlanteis(jogo).get(clube);
		ParametrosBonusTreinador.Faixa faixa = ParametrosBonusTreinador.padrao().faixa(Clube.forcaEmCampo(plantel)).orElseThrow();
		BonusTreinador bonus = temporadas.carregarAtual(jogo).bonusTreinador();

		assertThat(bonus.sorteados()).hasSize(faixa.jogadores());
		assertThat(bonus.sorteados().values()).allMatch(s -> s.forcaExtra() == faixa.forcaExtra());
		assertThat(plantel).filteredOn(j -> bonus.sorteados().containsKey(j.id()))
			.hasSize(faixa.jogadores())
			.allMatch(j -> j.posicao() != Posicao.GOLEIRO)
			.allMatch(j -> j.asterisco() == bonus.sorteados().get(j.id()).extra());
		// a força gravada não muda
		assertThat(jogos.carregarPlanteis(jogo).get(clube)).isEqualTo(plantel);

		// de novo não sorteia
		temporadas.garantirBonusTreinador(jogo);
		assertThat(temporadas.bonusTreinadorAtual(jogo)).isEqualTo(bonus);
	}

	@Test
	void naTrocaDeClubeONovoSorteiaEODeixadoMantem() {
		long antigo = clubeDoUsuario();
		BonusTreinador antes = temporadas.bonusTreinadorAtual(jogo);
		// o usuário passa para um clube da Série B (força 22): 3 jogadores com +10
		long novo = jdbc.sql("""
				SELECT c.id FROM jogo_clube c JOIN jogo_divisao d ON d.id = c.divisao_id
				WHERE c.jogo_id = :jogo AND d.prioridade = 2 AND c.id <> :antigo ORDER BY c.id LIMIT 1""")
			.param("jogo", jogo).param("antigo", antigo).query(Long.class).single();
		jdbc.sql("UPDATE jogo_treinador SET clube_id = :novo WHERE jogo_id = :jogo").param("novo", novo).param("jogo", jogo)
			.update();

		temporadas.garantirBonusTreinador(jogo);

		BonusTreinador depois = temporadas.bonusTreinadorAtual(jogo);
		assertThat(depois.sorteados()).containsAllEntriesOf(antes.sorteados());
		List<Long> doNovo = jogos.carregarPlanteis(jogo).get(novo).stream().map(Jogador::id)
			.filter(depois.sorteados()::containsKey).toList();
		assertThat(doNovo).hasSize(3).allMatch(id -> depois.sorteados().get(id).forcaExtra() == 10);
	}

	@Test
	void naPartidaOSorteadoEntraComAForcaExtraEOPlantelNaoMuda() {
		long clube = clubeDoUsuario();
		BonusTreinador bonus = temporadas.bonusTreinadorAtual(jogo);
		Map<Long, Jogador> gravados = new java.util.HashMap<>();
		jogos.carregarPlanteis(jogo).get(clube).forEach(j -> gravados.put(j.id(), j));

		RodadaAoVivo aoVivo = rodadas.preparar(jogo, 1, List.of());
		Escalacao escalacao = aoVivo.partidas().stream()
			.filter(p -> p.jogo().mandante().id() == clube || p.jogo().visitante().id() == clube)
			.map(p -> p.jogo().mandante().id() == clube ? p.casa() : p.fora())
			.findFirst().orElseThrow();
		List<Jogador> emCampo = new java.util.ArrayList<>(escalacao.titulares());
		emCampo.addAll(escalacao.reservas());
		for (Jogador j : emCampo) {
			Jogador gravado = gravados.get(j.id());
			BonusTreinador.Sorteado s = bonus.sorteados().get(j.id());
			int extra = s == null ? 0 : s.forcaExtra();
			assertThat(j.forca()).isEqualTo(Math.min(50, gravado.forca() + extra));
			if (s != null) {
				assertThat(j.asterisco()).isTrue();
			}
		}
	}

	private long clubeDoUsuario() {
		return jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo").param("jogo", jogo).query(Long.class)
			.single();
	}
}
