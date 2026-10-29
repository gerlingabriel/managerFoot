package br.com.managerfoot.persistencia;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.dominio.Formacao;

/**
 * Escalação gravada do treinador humano (migração V12; SPEC §10.3, rev. 34). Grava também a
 * incompleta: a validade é conferida antes da rodada.
 */
@Repository
public class EscalacaoRepositorio {

	private final JdbcClient jdbc;

	public EscalacaoRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public Optional<EscalacaoDoTreinador> carregar(long clubeId) {
		return Optional.ofNullable(carregarDoJogo("e.clube_id = :id", clubeId).get(clubeId));
	}

	/** Escalações gravadas dos clubes do jogo, por id do clube. */
	@Transactional(readOnly = true)
	public Map<Long, EscalacaoDoTreinador> carregarDoJogo(long jogoId) {
		return carregarDoJogo("c.jogo_id = :id", jogoId);
	}

	/**
	 * Substitui a escalação gravada do clube.
	 *
	 * @throws org.springframework.dao.DataIntegrityViolationException se algum jogador não é do clube
	 */
	@Transactional
	public void gravar(long clubeId, EscalacaoDoTreinador escalacao) {
		jdbc.sql("""
				INSERT INTO jogo_escalacao (clube_id, formacao, batedor_id) VALUES (:clube, :formacao, :batedor)
				ON CONFLICT (clube_id) DO UPDATE SET formacao = excluded.formacao, batedor_id = excluded.batedor_id""")
			.param("clube", clubeId)
			.param("formacao", escalacao.formacao().name())
			.param("batedor", escalacao.batedor())
			.update();
		jdbc.sql("DELETE FROM jogo_escalacao_jogador WHERE clube_id = :clube").param("clube", clubeId).update();
		escalacao.titulares().forEach(id -> inserirJogador(clubeId, id, "TITULAR"));
		escalacao.reservas().forEach(id -> inserirJogador(clubeId, id, "RESERVA"));
	}

	/**
	 * Tira das escalações do jogo os jogadores suspensos e os lesionados (SPEC §7.5), que passam para
	 * fora do banco (SPEC §10.3, rev. 34). O treinador completa o time antes da próxima rodada.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void retirarSuspensos(long jogoId) {
		jdbc.sql("""
				DELETE FROM jogo_escalacao_jogador ej USING jogo_jogador j, jogo_clube c
				WHERE j.id = ej.jogador_id AND c.id = j.clube_id AND c.jogo_id = :jogo AND (j.suspensao > 0 OR j.lesao > 0)""")
			.param("jogo", jogoId)
			.update();
		jdbc.sql("""
				UPDATE jogo_escalacao e SET batedor_id = NULL FROM jogo_jogador j, jogo_clube c
				WHERE j.id = e.batedor_id AND c.id = j.clube_id AND c.jogo_id = :jogo AND (j.suspensao > 0 OR j.lesao > 0)""")
			.param("jogo", jogoId)
			.update();
	}

	private void inserirJogador(long clubeId, long jogadorId, String papel) {
		jdbc.sql("INSERT INTO jogo_escalacao_jogador (clube_id, jogador_id, papel) VALUES (:clube, :jogador, :papel)")
			.param("clube", clubeId)
			.param("jogador", jogadorId)
			.param("papel", papel)
			.update();
	}

	private Map<Long, EscalacaoDoTreinador> carregarDoJogo(String filtro, long id) {
		Map<Long, Formacao> formacoes = new HashMap<>();
		Map<Long, Long> batedores = new HashMap<>();
		jdbc.sql("SELECT e.clube_id, e.formacao, e.batedor_id FROM jogo_escalacao e JOIN jogo_clube c ON c.id = e.clube_id WHERE "
				+ filtro)
			.param("id", id)
			.query(rs -> {
				long clube = rs.getLong("clube_id");
				formacoes.put(clube, Formacao.valueOf(rs.getString("formacao")));
				long batedor = rs.getLong("batedor_id");
				batedores.put(clube, rs.wasNull() ? null : batedor);
			});
		Map<Long, Set<Long>> titulares = new HashMap<>();
		Map<Long, Set<Long>> reservas = new HashMap<>();
		jdbc.sql("""
				SELECT ej.clube_id, ej.jogador_id, ej.papel FROM jogo_escalacao_jogador ej
				JOIN jogo_escalacao e ON e.clube_id = ej.clube_id JOIN jogo_clube c ON c.id = e.clube_id"""
				+ " WHERE " + filtro + " ORDER BY ej.jogador_id")
			.param("id", id)
			.query(rs -> {
				Map<Long, Set<Long>> destino = rs.getString("papel").equals("TITULAR") ? titulares : reservas;
				destino.computeIfAbsent(rs.getLong("clube_id"), k -> new LinkedHashSet<>()).add(rs.getLong("jogador_id"));
			});
		Map<Long, EscalacaoDoTreinador> escalacoes = new HashMap<>();
		formacoes.forEach((clube, formacao) -> escalacoes.put(clube, new EscalacaoDoTreinador(formacao,
				titulares.getOrDefault(clube, Set.of()), reservas.getOrDefault(clube, Set.of()), batedores.get(clube))));
		return escalacoes;
	}
}
