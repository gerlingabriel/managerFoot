package br.com.managerfoot.persistencia;

import java.util.List;

/** Avanço da temporada nos testes, com as etapas da copa no meio das rodadas da liga (SPEC §8.2). */
public final class RodadasDeTeste {

	private RodadasDeTeste() {
	}

	/**
	 * Joga as etapas da copa pendentes antes da rodada e depois a rodada; na última rodada da liga,
	 * também a final da copa.
	 */
	public static RodadaRepositorio.RodadaJogada jogarRodada(RodadaRepositorio rodadas, long jogoId, int rodada) {
		jogarCopaPendente(rodadas, jogoId);
		RodadaRepositorio.RodadaJogada jogada = rodadas.avancar(jogoId, rodada);
		if (rodada == rodadas.totalDeRodadas(jogoId)) {
			jogarCopaPendente(rodadas, jogoId);
		}
		return jogada;
	}

	/** Joga as etapas da copa que vêm antes da próxima rodada da liga. */
	public static void jogarCopaPendente(RodadaRepositorio rodadas, long jogoId) {
		while (rodadas.proximo(jogoId) instanceof RodadaRepositorio.Proximo.EtapaCopa etapa) {
			rodadas.avancarCopa(jogoId, etapa.indice(), List.of());
		}
	}
}
