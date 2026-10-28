package br.com.managerfoot.persistencia;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.LeilaoRepositorio.VendaPorCaixa;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ClubeInicial;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.Temporada;
import br.com.managerfoot.temporada.TransicaoTemporada;

/**
 * Fim de temporada (SPEC §4.1, §8.1; Etapa 3c.4): promoção, rebaixamento e fila de espera pela
 * classificação final, amarelos zerados (suspensões continuam, §7.4) e início da temporada
 * seguinte. Moral e força dos jogadores não mudam aqui.
 */
@Repository
public class FimDeTemporadaRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final TemporadaRepositorio temporadas;
	private final RodadaRepositorio rodadas;
	private final EscalacaoRepositorio escalacoes;
	private final CaixaRepositorio caixas;
	private final LeilaoRepositorio leiloes;
	private final int candidatosClubeInicial;
	private final EscalacaoMaquina escalacaoMaquina = new EscalacaoMaquina(ParametrosMotor.padrao());

	public FimDeTemporadaRepositorio(JdbcClient jdbc, JogoRepositorio jogos, TemporadaRepositorio temporadas,
			RodadaRepositorio rodadas, EscalacaoRepositorio escalacoes, CaixaRepositorio caixas, LeilaoRepositorio leiloes,
			@Value("${managerfoot.jogo.candidatos-clube-inicial}") int candidatosClubeInicial) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.temporadas = temporadas;
		this.rodadas = rodadas;
		this.escalacoes = escalacoes;
		this.caixas = caixas;
		this.leiloes = leiloes;
		this.candidatosClubeInicial = candidatosClubeInicial;
	}

	/**
	 * Encerra a temporada atual e inicia a seguinte. A semente decide o sorteio da fila e os
	 * sorteios da nova temporada.
	 *
	 * @param temporadaAtual a temporada que quem pede espera encerrar; um pedido repetido é recusado
	 * @return a transição e a nova temporada
	 * @throws NoSuchElementException se o jogo não existe ou não tem temporada
	 * @throws IllegalStateException se a temporada não é a atual ou ainda tem rodadas a jogar
	 */
	@Transactional
	public NovaTemporada avancarTemporada(long jogoId, int temporadaAtual, long semente) {
		jogos.travar(jogoId);
		int numero = temporadas.carregarAtual(jogoId).numero();
		if (numero != temporadaAtual) {
			throw new IllegalStateException("a temporada atual do jogo " + jogoId + " é a " + numero + ", não a "
					+ temporadaAtual);
		}
		int jogadas = rodadas.rodadasProcessadas(jogoId);
		int total = rodadas.totalDeRodadas(jogoId);
		if (jogadas < total) {
			throw new IllegalStateException("a temporada " + numero + " do jogo " + jogoId + " ainda tem "
					+ (total - jogadas) + " rodada(s) a jogar");
		}

		Map<Integer, Classificacao> finais = rodadas.classificacao(jogoId);
		gravarCampeoes(jogoId, numero, finais);
		caixas.pagarPremios(jogoId, finais);
		// gerador da fila separado do usado por Temporada.iniciar com a mesma semente
		TransicaoTemporada transicao = TransicaoTemporada.aplicar(jogos.carregarLiga(jogoId), finais,
				new SplittableRandom(~semente));
		gravarDivisoes(jogoId, transicao.proxima());
		jdbc.sql("""
				UPDATE jogo_jogador SET amarelos = 0
				WHERE amarelos > 0 AND clube_id IN (SELECT id FROM jogo_clube WHERE jogo_id = :jogo)""")
			.param("jogo", jogoId)
			.update();

		Map<Long, Clube> demitidos = demitirDaFila(jogoId, transicao.proxima(), new SplittableRandom(semente ^ MISTURA_FILA));

		Temporada nova = Temporada.iniciar(numero + 1, transicao.proxima(), jogos.carregarPlanteis(jogoId), semente);
		temporadas.gravar(jogoId, nova);
		// caixa negativo: a venda vem antes de qualquer coisa da temporada nova, inclusive do patrocínio
		List<VendaPorCaixa> vendas = leiloes.venderNoInicioDaTemporada(jogoId);
		caixas.pagarPatrocinio(jogoId, finais);
		return new NovaTemporada(transicao, nova, demitidos, vendas);
	}

	/**
	 * Histórico de campeões (SPEC §8.1.1, rev. 40): o primeiro da classificação final de cada
	 * divisão, com o ano da temporada e o usuário que dirigia o clube (nulo para a máquina).
	 */
	private void gravarCampeoes(long jogoId, int temporada, Map<Integer, Classificacao> finais) {
		finais.forEach((prioridade, classificacao) -> {
			Clube campeao = classificacao.linhas().getFirst().clube();
			jdbc.sql("""
					INSERT INTO jogo_campeao (jogo_id, temporada, ano, divisao_prioridade, divisao_nome, clube_id,
					                          clube_nome, usuario_id)
					SELECT j.id, :temporada, j.ano_inicial + :temporada - 1, d.prioridade, d.nome, c.id, c.nome,
					       (SELECT t.usuario_id FROM jogo_treinador t WHERE t.clube_id = c.id)
					FROM jogo j JOIN jogo_divisao d ON d.jogo_id = j.id AND d.prioridade = :prioridade
					JOIN jogo_clube c ON c.id = :clube
					WHERE j.id = :jogo""")
				.param("temporada", temporada)
				.param("prioridade", prioridade)
				.param("clube", campeao.id())
				.param("jogo", jogoId)
				.update();
		});
	}

	/**
	 * Treinador humano cujo clube caiu para a fila é demitido e assume um clube sorteado entre os mais
	 * fracos da última divisão, como no início do jogo (SPEC §4.1, rev. 36, provisório até existirem
	 * os convites). Clube de outro treinador não entra no sorteio (rev. 29). O clube novo começa com
	 * a escalação da máquina; o antigo volta a ser escalado pela máquina.
	 *
	 * @return o clube novo de cada usuário demitido
	 */
	private Map<Long, Clube> demitirDaFila(long jogoId, ConfiguracaoLiga proxima, SplittableRandom aleatorio) {
		Set<Long> naFila = proxima.filaDeEspera().stream().map(Clube::id).collect(Collectors.toSet());
		Map<Long, Long> treinadores = new LinkedHashMap<>();
		jdbc.sql("SELECT usuario_id, clube_id FROM jogo_treinador WHERE jogo_id = :jogo ORDER BY id")
			.param("jogo", jogoId)
			.query(rs -> {
				treinadores.put(rs.getLong("usuario_id"), rs.getLong("clube_id"));
			});
		Map<Long, Clube> demitidos = new LinkedHashMap<>();
		Divisao ultima = proxima.divisoes().getLast();
		for (Map.Entry<Long, Long> treinador : treinadores.entrySet()) {
			if (!naFila.contains(treinador.getValue())) {
				continue;
			}
			Set<Long> ocupados = new HashSet<>(treinadores.values());
			List<Clube> livres = ClubeInicial.candidatos(ultima, candidatosClubeInicial).stream()
				.filter(c -> !ocupados.contains(c.id())).toList();
			if (livres.isEmpty()) {
				livres = ultima.clubes().stream().filter(c -> !ocupados.contains(c.id())).toList();
			}
			if (livres.isEmpty()) {
				continue;
			}
			Clube novo = livres.get(aleatorio.nextInt(livres.size()));
			jdbc.sql("DELETE FROM jogo_escalacao WHERE clube_id = :clube").param("clube", treinador.getValue()).update();
			jdbc.sql("UPDATE jogo_treinador SET clube_id = :novo WHERE jogo_id = :jogo AND usuario_id = :usuario")
				.param("novo", novo.id())
				.param("jogo", jogoId)
				.param("usuario", treinador.getKey())
				.update();
			Set<Long> suspensos = new HashSet<>(jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :c AND suspensao > 0")
				.param("c", novo.id()).query(Long.class).list());
			List<Jogador> disponiveis = jogos.carregarPlanteis(jogoId).get(novo.id()).stream()
				.filter(j -> !suspensos.contains(j.id())).toList();
			if (disponiveis.size() >= Formacao.TITULARES) {
				escalacoes.gravar(novo.id(), EscalacaoDoTreinador.de(escalacaoMaquina.escalar(disponiveis)));
			}
			treinadores.put(treinador.getKey(), novo.id());
			demitidos.put(treinador.getKey(), novo);
		}
		return demitidos;
	}

	private void gravarDivisoes(long jogoId, ConfiguracaoLiga proxima) {
		for (Divisao divisao : proxima.divisoes()) {
			for (Clube clube : divisao.clubes()) {
				jdbc.sql("""
						UPDATE jogo_clube SET divisao_id =
						  (SELECT id FROM jogo_divisao WHERE jogo_id = :jogo AND prioridade = :prioridade)
						WHERE id = :clube AND jogo_id = :jogo""")
					.param("jogo", jogoId)
					.param("prioridade", divisao.prioridade())
					.param("clube", clube.id())
					.update();
			}
		}
		for (Clube clube : proxima.filaDeEspera()) {
			jdbc.sql("UPDATE jogo_clube SET divisao_id = NULL WHERE id = :clube AND jogo_id = :jogo")
				.param("clube", clube.id())
				.param("jogo", jogoId)
				.update();
		}
	}

	/** Mistura a semente para o sorteio do clube do treinador demitido. */
	private static final long MISTURA_FILA = 0x5DEECE66DL;

	/**
	 * Resultado do fim de temporada.
	 *
	 * @param demitidos clube novo de cada usuário cujo clube caiu para a fila
	 * @param vendas leilões dos clubes de treinador com caixa negativo no início da temporada nova
	 */
	public record NovaTemporada(TransicaoTemporada transicao, Temporada temporada, Map<Long, Clube> demitidos,
			List<VendaPorCaixa> vendas) {
	}
}
