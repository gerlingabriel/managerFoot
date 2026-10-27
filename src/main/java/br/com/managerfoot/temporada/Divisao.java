package br.com.managerfoot.temporada;

import java.util.List;
import java.util.Objects;

import br.com.managerfoot.dominio.Clube;

/**
 * Configuração de uma divisão (SPEC §4.2). Não se valida sozinha: a consistência depende das
 * outras divisões e é verificada por {@link ValidadorLiga}.
 *
 * @param prioridade 1 = liga principal, 2 = segunda divisão e assim por diante
 * @param limiteForca teto de força dos jogadores na divisão (a principal é sempre 50)
 * @param promovidos quantos sobem para a divisão de prioridade imediatamente menor
 * @param rebaixados quantos descem; na última divisão, vão para a fila de espera
 */
public record Divisao(String nome, int prioridade, int limiteForca, int promovidos, int rebaixados,
		List<Clube> clubes) {

	public Divisao {
		Objects.requireNonNull(clubes, "lista de clubes da divisão é obrigatória");
		clubes = List.copyOf(clubes);
	}
}
