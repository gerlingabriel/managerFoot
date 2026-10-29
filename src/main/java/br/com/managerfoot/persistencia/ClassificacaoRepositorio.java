package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.ResultadoJogo;
import br.com.managerfoot.temporada.Temporada;

/**
 * Classificação de cada divisão de uma temporada, recalculada dos placares gravados. Serve à tabela
 * (todos os placares) e ao público (só as rodadas antes da que vai ser jogada, SPEC §9.2).
 */
@Repository
public class ClassificacaoRepositorio {

	private final JdbcClient jdbc;

	public ClassificacaoRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * @param temporadaId a linha de {@code jogo_temporada} de {@code temporada}
	 * @param antesDaRodada conta só os placares das rodadas anteriores a esta
	 * @return a classificação por prioridade da divisão
	 */
	@Transactional(readOnly = true)
	public Map<Integer, Classificacao> calcular(Temporada temporada, long temporadaId, int antesDaRodada) {
		Map<Integer, Classificacao> classificacoes = new TreeMap<>();
		for (Temporada.SorteiosDivisao divisao : temporada.divisoes()) {
			Map<String, Jogo> porConfronto = new HashMap<>();
			divisao.calendario().jogos().forEach(j -> porConfronto.put(confronto(j.mandante().id(), j.visitante().id()), j));
			List<ResultadoJogo> resultados = new ArrayList<>();
			jdbc.sql("""
					SELECT p.mandante_id, p.visitante_id, p.gols_mandante, p.gols_visitante
					FROM jogo_partida p JOIN jogo_divisao d ON d.id = p.divisao_id
					WHERE p.temporada_id = :temporada AND d.prioridade = :prioridade AND p.gols_mandante IS NOT NULL
					  AND p.rodada < :antes""")
				.param("temporada", temporadaId)
				.param("prioridade", divisao.prioridade())
				.param("antes", antesDaRodada)
				.query((rs, n) -> new ResultadoJogo(
						porConfronto.get(confronto(rs.getLong("mandante_id"), rs.getLong("visitante_id"))),
						rs.getInt("gols_mandante"), rs.getInt("gols_visitante")))
				.list()
				.forEach(resultados::add);
			List<Clube> clubes = divisao.calendario()
				.jogos()
				.stream()
				.flatMap(j -> Stream.of(j.mandante(), j.visitante()))
				.distinct()
				.sorted(Comparator.comparingLong(Clube::id))
				.toList();
			classificacoes.put(divisao.prioridade(), Classificacao.calcular(clubes, resultados, temporada.semente()));
		}
		return classificacoes;
	}

	private static String confronto(long mandante, long visitante) {
		return mandante + "x" + visitante;
	}
}
