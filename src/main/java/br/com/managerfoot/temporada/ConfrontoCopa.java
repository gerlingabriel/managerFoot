package br.com.managerfoot.temporada;

import java.util.Objects;
import java.util.Optional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.motor.JogoDecisivo;

/**
 * Um confronto da copa (SPEC §8.2): ida e volta, ou jogo único na final. Na volta o mando se
 * inverte; empatado o placar somado, decidem a prorrogação e os pênaltis da volta. Gol fora de
 * casa não vale (rev. 70).
 *
 * @param ordem posição do confronto na fase, de 0 em diante; os vencedores de 2k e 2k+1 se
 *        enfrentam na fase seguinte
 * @param mandante quem joga a ida em casa (sorteado); na final, só o primeiro nome do jogo
 * @param ida placar da ida (ou do jogo único), visto do {@code mandante}
 * @param volta placar da volta, visto de quem joga em casa nela: o {@code visitante}
 */
public record ConfrontoCopa(FaseCopa fase, int ordem, Clube mandante, Clube visitante, Optional<PlacarCopa> ida,
		Optional<PlacarCopa> volta) {

	public ConfrontoCopa {
		Objects.requireNonNull(fase);
		Objects.requireNonNull(mandante);
		Objects.requireNonNull(visitante);
		Objects.requireNonNull(ida);
		Objects.requireNonNull(volta);
		if (mandante.equals(visitante)) {
			throw new IllegalArgumentException(mandante.nome() + " não pode enfrentar a si mesmo");
		}
		if (volta.isPresent() && (fase.jogoUnico() || ida.isEmpty())) {
			throw new IllegalArgumentException("volta sem ida ou na final");
		}
		ida.ifPresent(p -> {
			if (!fase.jogoUnico() && (p.prorrogacao() || p.penaltis().isPresent())) {
				throw new IllegalArgumentException("a ida não tem prorrogação");
			}
		});
	}

	public static ConfrontoCopa novo(FaseCopa fase, int ordem, Clube mandante, Clube visitante) {
		return new ConfrontoCopa(fase, ordem, mandante, visitante, Optional.empty(), Optional.empty());
	}

	public ConfrontoCopa comIda(PlacarCopa placar) {
		if (ida.isPresent()) {
			throw new IllegalStateException("a ida de " + this + " já foi jogada");
		}
		ConfrontoCopa novo = new ConfrontoCopa(fase, ordem, mandante, visitante, Optional.of(placar), volta);
		novo.validarDecisao(placar, fase.jogoUnico());
		return novo;
	}

	public ConfrontoCopa comVolta(PlacarCopa placar) {
		if (ida.isEmpty() || volta.isPresent()) {
			throw new IllegalStateException("a volta de " + mandante.nome() + " x " + visitante.nome() + " não está pendente");
		}
		ConfrontoCopa novo = new ConfrontoCopa(fase, ordem, mandante, visitante, ida, Optional.of(placar));
		novo.validarDecisao(placar, true);
		return novo;
	}

	/** O placar da partida decisiva tem de decidir: com o somado empatado, precisa dos pênaltis. */
	private void validarDecisao(PlacarCopa placar, boolean decisiva) {
		if (!decisiva) {
			return;
		}
		boolean empatado = golsDoMandanteSomados() == golsDoVisitanteSomados();
		if (empatado && placar.penaltis().isEmpty()) {
			throw new IllegalArgumentException("o confronto empatou no placar somado e precisa de pênaltis");
		}
		if (!empatado && placar.penaltis().isPresent()) {
			throw new IllegalArgumentException("pênaltis sem empate no placar somado");
		}
	}

	/** Gols do {@code mandante} nas duas partidas. */
	public int golsDoMandanteSomados() {
		return ida.map(PlacarCopa::golsMandante).orElse(0) + volta.map(PlacarCopa::golsVisitante).orElse(0);
	}

	/** Gols do {@code visitante} nas duas partidas. */
	public int golsDoVisitanteSomados() {
		return ida.map(PlacarCopa::golsVisitante).orElse(0) + volta.map(PlacarCopa::golsMandante).orElse(0);
	}

	public boolean decidido() {
		return fase.jogoUnico() ? ida.isPresent() : volta.isPresent();
	}

	/** Condições da partida que decide: a volta (com a ida somada) ou a final em campo neutro. */
	public JogoDecisivo decisivo() {
		if (fase.jogoUnico()) {
			return JogoDecisivo.FINAL;
		}
		PlacarCopa placarIda = ida.orElseThrow(() -> new IllegalStateException("a volta só existe depois da ida"));
		// na volta o visitante da ida joga em casa
		return JogoDecisivo.volta(placarIda.golsVisitante(), placarIda.golsMandante());
	}

	public Optional<Clube> vencedor() {
		if (!decidido()) {
			return Optional.empty();
		}
		PlacarCopa decisiva = fase.jogoUnico() ? ida.orElseThrow() : volta.orElseThrow();
		int mandante = golsDoMandanteSomados();
		int visitante = golsDoVisitanteSomados();
		if (mandante == visitante) {
			PlacarCopa.Penaltis penaltis = decisiva.penaltis().orElseThrow();
			// na volta, o "mandante" da disputa é o visitante da ida
			boolean ganhouQuemMandouNaDecisiva = penaltis.mandante() > penaltis.visitante();
			boolean decisivaEmCasaDoMandante = fase.jogoUnico();
			return Optional.of(ganhouQuemMandouNaDecisiva == decisivaEmCasaDoMandante ? this.mandante : this.visitante);
		}
		return Optional.of(mandante > visitante ? this.mandante : this.visitante);
	}

	public Optional<Clube> perdedor() {
		return vencedor().map(v -> v.equals(mandante) ? visitante : mandante);
	}

	public boolean envolve(Clube clube) {
		return mandante.equals(clube) || visitante.equals(clube);
	}
}
