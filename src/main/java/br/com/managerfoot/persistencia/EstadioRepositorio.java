package br.com.managerfoot.persistencia;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.economia.AmpliacaoEstadio;
import br.com.managerfoot.economia.MercadoDaMaquina;
import br.com.managerfoot.economia.ParametrosPublico;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;

/**
 * Estádio dos clubes do jogo (SPEC §9.2, rev. 56 e 57): obras de ampliação, a decisão da máquina de
 * ampliar e a posição final da temporada passada, que decide o público das primeiras rodadas. Só roda
 * dentro do avanço da rodada e do fim de temporada, que já garantem processamento único.
 */
@Repository
public class EstadioRepositorio {

	private final JdbcClient jdbc;
	private final CaixaRepositorio caixas;
	private final ParametrosPublico parametros = ParametrosPublico.padrao();
	private final AmpliacaoEstadio ampliacao = AmpliacaoEstadio.padrao();

	public EstadioRepositorio(JdbcClient jdbc, CaixaRepositorio caixas) {
		this.jdbc = jdbc;
		this.caixas = caixas;
	}

	/**
	 * Depois da rodada jogada: as obras andam uma rodada e as que terminam somam o bloco ao estádio
	 * (vale a partir da rodada seguinte); depois, cada clube da máquina numa divisão, sem obra, decide
	 * se amplia e paga na hora. Clube de usuário só amplia pela tela.
	 *
	 * @param planteis os planteis que jogaram a rodada
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void avancarObras(long jogoId, long temporadaId, int rodada, Map<Long, List<Jogador>> planteis) {
		jdbc.sql("""
				UPDATE jogo_clube
				SET estadio_capacidade = CASE WHEN obra_rodadas = 1 THEN estadio_capacidade + :bloco ELSE estadio_capacidade END,
				    obra_rodadas = nullif(obra_rodadas - 1, 0)
				WHERE jogo_id = :jogo AND obra_rodadas IS NOT NULL""")
			.param("bloco", parametros.bloco())
			.param("jogo", jogoId)
			.update();
		List<ClubeSemObra> candidatos = jdbc.sql("""
				SELECT c.id, c.estadio_capacidade, c.saldo FROM jogo_clube c
				WHERE c.jogo_id = :jogo AND c.divisao_id IS NOT NULL AND c.obra_rodadas IS NULL AND c.saldo IS NOT NULL
				  AND NOT EXISTS (SELECT 1 FROM jogo_treinador t WHERE t.clube_id = c.id)
				ORDER BY c.id""")
			.param("jogo", jogoId)
			.query((rs, n) -> new ClubeSemObra(rs.getLong("id"), rs.getInt("estadio_capacidade"), rs.getLong("saldo")))
			.list();
		for (ClubeSemObra c : candidatos) {
			List<Jogador> plantel = planteis.getOrDefault(c.id(), List.of());
			if (ampliacao.maquinaAmplia(c.estadio(), MercadoDaMaquina.forcaEmCampo(plantel), c.saldo(), plantel)) {
				caixas.lancarEstadio(temporadaId, rodada, c.id(), ampliacao.custo());
				jdbc.sql("UPDATE jogo_clube SET obra_rodadas = :rodadas WHERE id = :clube")
					.param("rodadas", parametros.rodadasDeObra())
					.param("clube", c.id())
					.update();
			}
		}
	}

	/**
	 * Grava a posição final de cada clube na divisão em que jogou a temporada que terminou. Quem vem da
	 * fila para uma divisão conta como o último dela (como no patrocínio, rev. 46); quem vai para a fila
	 * fica sem posição.
	 *
	 * @param finais classificação final por prioridade da divisão
	 * @param proxima a liga da temporada seguinte
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void gravarPosicoesAnteriores(long jogoId, Map<Integer, Classificacao> finais, ConfiguracaoLiga proxima) {
		jdbc.sql("UPDATE jogo_clube SET posicao_anterior = NULL, clubes_divisao_anterior = NULL WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.update();
		finais.values().forEach(classificacao -> {
			for (int i = 0; i < classificacao.linhas().size(); i++) {
				gravar(jogoId, classificacao.linhas().get(i).clube().id(), i + 1, classificacao.linhas().size());
			}
		});
		for (Divisao divisao : proxima.divisoes()) {
			int clubes = divisao.clubes().size();
			for (Clube clube : divisao.clubes()) {
				boolean jogou = finais.values().stream()
					.anyMatch(c -> c.linhas().stream().anyMatch(l -> l.clube().id() == clube.id()));
				if (!jogou) {
					gravar(jogoId, clube.id(), clubes, clubes);
				}
			}
		}
		for (Clube clube : proxima.filaDeEspera()) {
			jdbc.sql("UPDATE jogo_clube SET posicao_anterior = NULL, clubes_divisao_anterior = NULL WHERE id = :clube")
				.param("clube", clube.id())
				.update();
		}
	}

	private void gravar(long jogoId, long clubeId, int posicao, int clubes) {
		jdbc.sql("""
				UPDATE jogo_clube SET posicao_anterior = :posicao, clubes_divisao_anterior = :clubes
				WHERE id = :clube AND jogo_id = :jogo""")
			.param("posicao", posicao)
			.param("clubes", clubes)
			.param("clube", clubeId)
			.param("jogo", jogoId)
			.update();
	}

	private record ClubeSemObra(long id, int estadio, long saldo) {
	}
}
