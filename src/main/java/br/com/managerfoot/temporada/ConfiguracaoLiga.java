package br.com.managerfoot.temporada;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import br.com.managerfoot.dominio.Clube;

/**
 * Estrutura de uma liga antes do início (SPEC §4). Pode estar incompleta enquanto é editada; só
 * inicia se {@link ValidadorLiga} não apontar problemas.
 *
 * @param filaDeEspera clubes sem divisão, aguardando vaga na última divisão (SPEC §4.1)
 */
public record ConfiguracaoLiga(String nome, List<Divisao> divisoes, List<Clube> filaDeEspera) {

	public ConfiguracaoLiga {
		Objects.requireNonNull(divisoes, "lista de divisões é obrigatória");
		Objects.requireNonNull(filaDeEspera, "fila de espera é obrigatória (pode ser vazia)");
		divisoes = divisoes.stream().sorted(Comparator.comparingInt(Divisao::prioridade)).toList();
		filaDeEspera = List.copyOf(filaDeEspera);
	}

	/** Clubes necessários: soma das divisões + rebaixados da última (SPEC §4.1). */
	public int clubesNecessarios() {
		int soma = divisoes.stream().mapToInt(d -> d.clubes().size()).sum();
		return soma + (divisoes.isEmpty() ? 0 : divisoes.getLast().rebaixados());
	}
}
