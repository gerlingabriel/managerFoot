package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Lugar;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.persistencia.PlantelRepositorio.LinhaJogador;

/**
 * O asterisco temporário do bônus de artilharia (SPEC §5.2) aparece no plantel, conta nos asteriscos em
 * campo e no completar da tática, como no motor: só o sorteado na divisão do clube.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AsteriscoDaArtilhariaNaTelaIT {

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	PlantelRepositorio plantel;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JdbcClient jdbc;

	long jogo;
	long usuario;
	long clube;
	long temporada;
	long divisao;
	long outraDivisao;

	@BeforeEach
	void criaJogoSemAsteriscosNoClubeDoUsuario() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		usuario = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = :jogo").param("jogo", jogo)
			.query(Long.class).single();
		clube = jogos.clubeDoTreinador(jogo, usuario).orElseThrow();
		temporada = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :jogo").param("jogo", jogo).query(Long.class)
			.single();
		divisao = jdbc.sql("SELECT divisao_id FROM jogo_clube WHERE id = :clube").param("clube", clube).query(Long.class)
			.single();
		outraDivisao = jdbc.sql("SELECT id FROM jogo_divisao WHERE jogo_id = :jogo AND id <> :divisao LIMIT 1")
			.param("jogo", jogo).param("divisao", divisao).query(Long.class).single();
		// o clube do usuário fica sem asterisco nenhum: nem próprio, nem do treinador, nem da artilharia
		jdbc.sql("UPDATE jogo_jogador SET asterisco = false WHERE clube_id = :clube").param("clube", clube).update();
		jdbc.sql("DELETE FROM jogo_bonus_treinador WHERE clube_id = :clube").param("clube", clube).update();
		jdbc.sql("DELETE FROM jogo_bonus_artilharia b USING jogo_jogador j WHERE j.id = b.jogador_id AND j.clube_id = :clube")
			.param("clube", clube).update();
	}

	@Test
	void oSorteadoNaDivisaoDoClubeApareceNoPlantelEContaEmCampo() {
		List<Jogador> defensores = defensoresMaisFortes();
		Jogador sorteado = defensores.get(0);
		Jogador deOutraDivisao = defensores.get(1);
		artilharia(sorteado.id(), divisao);
		// sorteado na outra divisão não vale aqui, como no motor
		artilharia(deOutraDivisao.id(), outraDivisao);

		assertThat(temporadas.asteriscosDaArtilharia(jogo, clube)).contains(sorteado.id()).doesNotContain(deOutraDivisao.id());
		List<LinhaJogador> linhas = plantel.visao(jogo, usuario).orElseThrow().jogadores();
		LinhaJogador comBonus = linha(linhas, sorteado.id());
		assertThat(comBonus.asteriscoDaArtilharia()).isTrue();
		assertThat(comBonus.asteriscoEmCampo()).isTrue();
		assertThat(linha(linhas, deOutraDivisao.id()).asteriscoEmCampo()).isFalse();
		assertThat(linhas).filteredOn(LinhaJogador::asteriscoDaArtilharia).hasSize(1);
	}

	@Test
	void completarATaticaRespeitaOLimiteContandoOAsteriscoDaArtilharia() {
		// os 4 defensores mais fortes: 3 com asterisco próprio e 1 com o da artilharia
		List<Jogador> defensores = defensoresMaisFortes();
		for (Jogador j : defensores.subList(0, 3)) {
			jdbc.sql("UPDATE jogo_jogador SET asterisco = true WHERE id = :j").param("j", j.id()).update();
		}
		artilharia(defensores.get(3).id(), divisao);

		plantel.retirarTodos(jogo, usuario);
		plantel.definirFormacao(jogo, usuario, Formacao.F_4_4_2);

		List<LinhaJogador> titulares = plantel.visao(jogo, usuario).orElseThrow().jogadores().stream()
			.filter(l -> l.lugar() == Lugar.TITULAR).toList();
		assertThat(titulares).hasSize(11);
		assertThat(titulares).filteredOn(LinhaJogador::asteriscoEmCampo).hasSizeLessThanOrEqualTo(3);
	}

	private List<Jogador> defensoresMaisFortes() {
		return jogos.carregarPlanteis(jogo).get(clube).stream().filter(j -> j.posicao() == Posicao.DEFENSOR)
			.sorted(java.util.Comparator.comparingInt(Jogador::forca).reversed().thenComparingLong(Jogador::id))
			.toList();
	}

	private void artilharia(long jogador, long divisaoId) {
		jdbc.sql("""
				INSERT INTO jogo_bonus_artilharia (temporada_id, divisao_id, jogador_id, extra) VALUES (:t, :d, :j, false)
				ON CONFLICT (temporada_id, jogador_id) DO UPDATE SET divisao_id = :d, extra = false""")
			.param("t", temporada).param("d", divisaoId).param("j", jogador).update();
	}

	private static LinhaJogador linha(List<LinhaJogador> linhas, long jogador) {
		return linhas.stream().filter(l -> l.jogador().id() == jogador).findFirst().orElseThrow();
	}

}
