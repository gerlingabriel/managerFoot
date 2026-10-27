package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import br.com.managerfoot.dominio.Clube;

/**
 * Calendário de pontos corridos em turno e returno (SPEC §8.1).
 *
 * <p>O turno usa o método de rodízio (tabela de Berger): um clube fica fixo e os demais giram;
 * o mando alterna de forma a deixar o mínimo teórico de sequências de dois jogos seguidos no
 * mesmo mando (N−2 no turno, com N par). O returno repete a ordem do turno com o mando
 * invertido. Com número ímpar de clubes, um clube fictício completa o rodízio e quem o enfrenta
 * folga na rodada.
 */
public record Calendario(List<Rodada> rodadas) {

	public Calendario {
		rodadas = List.copyOf(rodadas);
	}

	/**
	 * Gera o calendário na ordem recebida dos clubes. Sortear a ordem, se desejado, é
	 * responsabilidade de quem chama.
	 */
	public static Calendario turnoEReturno(List<Clube> clubes) {
		Objects.requireNonNull(clubes, "lista de clubes é obrigatória");
		if (clubes.size() < 2) {
			throw new IllegalArgumentException("o calendário precisa de ao menos 2 clubes, mas há " + clubes.size());
		}
		if (clubes.stream().map(Clube::id).distinct().count() != clubes.size()) {
			throw new IllegalArgumentException("há clubes repetidos na lista do calendário");
		}

		// null representa o clube fictício da folga quando o número é ímpar
		List<Clube> participantes = new ArrayList<>(clubes);
		if (participantes.size() % 2 == 1) {
			participantes.add(null);
		}
		int n = participantes.size();
		int giram = n - 1;
		Clube fixo = participantes.get(giram);

		List<Rodada> turno = new ArrayList<>();
		for (int r = 0; r < giram; r++) {
			int numero = r + 1;
			List<Jogo> jogos = new ArrayList<>();
			Clube folga = null;

			Clube daVez = participantes.get(r);
			// o clube fixo alterna o mando a cada rodada
			Clube[] confrontoFixo = r % 2 == 0 ? new Clube[] { daVez, fixo } : new Clube[] { fixo, daVez };
			folga = adicionar(jogos, numero, confrontoFixo[0], confrontoFixo[1], folga);

			for (int k = 1; k < n / 2; k++) {
				Clube a = participantes.get(Math.floorMod(r + k, giram));
				Clube b = participantes.get(Math.floorMod(r - k, giram));
				folga = k % 2 == 1 ? adicionar(jogos, numero, a, b, folga) : adicionar(jogos, numero, b, a, folga);
			}
			turno.add(new Rodada(numero, jogos, Optional.ofNullable(folga)));
		}

		List<Rodada> todas = new ArrayList<>(turno);
		for (Rodada ida : turno) {
			int numero = ida.numero() + turno.size();
			List<Jogo> volta = ida.jogos().stream().map(j -> new Jogo(numero, j.visitante(), j.mandante())).toList();
			todas.add(new Rodada(numero, volta, ida.folga()));
		}
		return new Calendario(todas);
	}

	/** Adiciona o jogo; se um dos lados for o clube fictício, devolve o outro como folga. */
	private static Clube adicionar(List<Jogo> jogos, int rodada, Clube mandante, Clube visitante, Clube folga) {
		if (mandante == null) {
			return visitante;
		}
		if (visitante == null) {
			return mandante;
		}
		jogos.add(new Jogo(rodada, mandante, visitante));
		return folga;
	}

	public List<Jogo> jogos() {
		return rodadas.stream().flatMap(r -> r.jogos().stream()).toList();
	}

	public List<Jogo> jogosDe(Clube clube) {
		return jogos().stream().filter(j -> j.envolve(clube)).toList();
	}
}
