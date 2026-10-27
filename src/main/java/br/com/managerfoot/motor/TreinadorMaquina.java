package br.com.managerfoot.motor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;

/**
 * Decisões do treinador controlado pela máquina durante a partida. Por ora só reage ao goleiro
 * expulso (SPEC §7.4.1); substituição por lesão entra junto com as lesões.
 */
final class TreinadorMaquina {

	private TreinadorMaquina() {
	}

	/** Uma troca decidida pela máquina. */
	record Troca(Jogador sai, Jogador entra) {
	}

	/**
	 * Com goleiro improvisado, põe o goleiro reserva no lugar do atacante mais fraco; sem atacantes,
	 * do meia mais fraco (regra provisória). Vazio se não houver goleiro no banco ou substituições.
	 */
	static Optional<Troca> aposGoleiroExpulso(TimeEmCampo time) {
		if (!time.goleiroImprovisado() || time.substituicoesRestantes() <= 0) {
			return Optional.empty();
		}
		Optional<Jogador> goleiroReserva = time.reservasDisponiveis().stream()
			.filter(j -> j.posicao() == Posicao.GOLEIRO)
			.max(Comparator.comparingInt(Jogador::forca));
		if (goleiroReserva.isEmpty()) {
			return Optional.empty();
		}
		return maisFraco(time.jogadoresNa(Posicao.ATACANTE))
			.or(() -> maisFraco(time.jogadoresNa(Posicao.MEIO_CAMPISTA)))
			.map(sai -> new Troca(sai, goleiroReserva.get()));
	}

	private static Optional<Jogador> maisFraco(List<Jogador> jogadores) {
		return jogadores.stream().min(Comparator.comparingInt(Jogador::forca));
	}
}
