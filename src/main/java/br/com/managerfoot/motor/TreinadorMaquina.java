package br.com.managerfoot.motor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;

/**
 * Decisões do treinador controlado pela máquina durante a partida: reage ao goleiro expulso
 * (SPEC §7.4.1) e substitui o lesionado (SPEC §7.5).
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

	/**
	 * Substituto do lesionado (provisório): goleiro pelo goleiro reserva mais forte; jogador de linha
	 * pelo reserva mais forte da mesma posição ou, sem ele, pelo reserva de linha mais forte que
	 * mantenha uma formação permitida. Vazio sem substituições ou sem reserva que sirva: o time fica
	 * com um a menos.
	 */
	static Optional<Troca> aposLesao(TimeEmCampo time, Jogador lesionado) {
		if (time.substituicoesRestantes() <= 0) {
			return Optional.empty();
		}
		Comparator<Jogador> maisForte = Comparator.comparingInt(Jogador::forca).reversed()
			.thenComparingLong(Jogador::id);
		List<Jogador> banco = time.reservasDisponiveis().stream().sorted(maisForte).toList();
		if (lesionado == time.goleiro()) {
			return banco.stream()
				.filter(j -> j.posicao() == Posicao.GOLEIRO)
				.findFirst()
				.map(entra -> new Troca(lesionado, entra));
		}
		return banco.stream()
			.filter(j -> j.posicao() == lesionado.posicao())
			.findFirst()
			.or(() -> banco.stream().filter(j -> time.trocaDeLinhaPermitida(lesionado, j)).findFirst())
			.map(entra -> new Troca(lesionado, entra));
	}

	private static Optional<Jogador> maisFraco(List<Jogador> jogadores) {
		return jogadores.stream().min(Comparator.comparingInt(Jogador::forca));
	}
}
