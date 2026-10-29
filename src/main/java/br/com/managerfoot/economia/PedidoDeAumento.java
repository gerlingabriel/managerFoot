package br.com.managerfoot.economia;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import br.com.managerfoot.dominio.Jogador;

/**
 * Pedido de aumento do jogador do usuário (SPEC §9.5, rev. 58, modelo 10a): o salário fica gravado
 * pela força do último contrato; quando a força passa dela por um intervalo, o jogador pede o
 * salário da força atual. Um pedido por rodada no clube: o de quem subiu mais. Domínio puro.
 *
 * @param intervalo pontos de força acima da força do salário para pedir (o usuário: "começou com 10 e
 *        está com 15, 16")
 */
public record PedidoDeAumento(int intervalo) {

	public PedidoDeAumento {
		if (intervalo < 1) {
			throw new IllegalArgumentException("o intervalo do pedido de aumento deve ser positivo");
		}
	}

	public static PedidoDeAumento padrao() {
		return new PedidoDeAumento(5);
	}

	/** Um jogador que ainda pode pedir nesta temporada, com a força em que o salário foi fixado. */
	public record Candidato(Jogador jogador, int forcaDoSalario) {

		int subiu() {
			return jogador.forca() - forcaDoSalario;
		}
	}

	public boolean pede(Candidato candidato) {
		return candidato.subiu() >= intervalo;
	}

	/** Quem pede nesta rodada: o que mais subiu; no empate, o de menor id. */
	public Optional<Candidato> quemPede(List<Candidato> candidatos) {
		return candidatos.stream()
			.filter(this::pede)
			.min(Comparator.comparingInt(Candidato::subiu).reversed().thenComparingLong(c -> c.jogador().id()));
	}
}
