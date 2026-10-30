package br.com.managerfoot.persistencia;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;

/** Avanço da temporada nos testes, com as etapas da copa e da continental no meio das rodadas da liga. */
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

	/**
	 * Deixa o usuário fora do caminho nas temporadas inteiras dos testes: a máquina escala o clube dele,
	 * inclusive o que ele recebeu numa troca pela moral, e o convite aberto fica recusado.
	 */
	public static void semDecisoesDoTreinador(JdbcClient jdbc, long jogoId) {
		jdbc.sql("DELETE FROM jogo_escalacao e USING jogo_clube c WHERE c.id = e.clube_id AND c.jogo_id = :j")
			.param("j", jogoId).update();
		jdbc.sql("UPDATE jogo_troca_treinador SET situacao = 'RECUSADO' WHERE jogo_id = :j AND situacao = 'CONVITE'")
			.param("j", jogoId).update();
	}

	/** Joga as etapas da copa, da continental e do Mundial que vêm antes da próxima rodada da liga. */
	public static void jogarCopaPendente(RodadaRepositorio rodadas, long jogoId) {
		while (true) {
			RodadaRepositorio.Proximo proximo = rodadas.proximo(jogoId);
			if (proximo instanceof RodadaRepositorio.Proximo.EtapaCopa etapa) {
				rodadas.avancarCopa(jogoId, etapa.indice(), List.of());
			} else if (proximo instanceof RodadaRepositorio.Proximo.EtapaContinental etapa) {
				rodadas.avancarContinental(jogoId, etapa.indice(), List.of());
			} else if (proximo instanceof RodadaRepositorio.Proximo.EtapaMundial etapa) {
				rodadas.avancarMundial(jogoId, etapa.indice(), List.of());
			} else {
				return;
			}
		}
	}
}
