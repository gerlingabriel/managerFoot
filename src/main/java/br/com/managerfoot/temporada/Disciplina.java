package br.com.managerfoot.temporada;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.ResultadoPartida;

/**
 * Amarelos acumulados na temporada e jogos de suspensão a cumprir (SPEC §7.4). Imutável.
 *
 * <ul>
 * <li>Cada amarelo soma 1 ao contador, inclusive o segundo na mesma partida; ao chegar ao
 * limite, suspende 1 jogo e o contador recomeça (SPEC rev. 23).</li>
 * <li>O segundo amarelo na partida ({@code SEGUNDO_AMARELO}) também é expulsão: mais 1 jogo. Com 4
 * amarelos antes, dois na partida dão 1 jogo pelo acumulado, contador em 1, e 1 jogo pela
 * expulsão.</li>
 * <li>Vermelho direto: sorteio ponderado de {@link ParametrosSuspensao#pesosVermelhoDireto()}.</li>
 * <li>A suspensão é cumprida na próxima partida do clube: quem estava suspenso antes da partida
 * cumpre um jogo, e só depois contam os cartões dela.</li>
 * </ul>
 *
 * @param amarelos amarelos acumulados por id do jogador; ausente é zero
 * @param suspensoes jogos de suspensão a cumprir por id do jogador; ausente é zero
 */
public record Disciplina(Map<Long, Integer> amarelos, Map<Long, Integer> suspensoes) {

	public Disciplina {
		amarelos = semZeros(amarelos);
		suspensoes = semZeros(suspensoes);
	}

	public static Disciplina vazia() {
		return new Disciplina(Map.of(), Map.of());
	}

	public int amarelosDe(long jogadorId) {
		return amarelos.getOrDefault(jogadorId, 0);
	}

	public int suspensaoDe(long jogadorId) {
		return suspensoes.getOrDefault(jogadorId, 0);
	}

	public boolean suspenso(long jogadorId) {
		return suspensaoDe(jogadorId) > 0;
	}

	/**
	 * @param jogadoresDosClubes ids dos planteis dos dois clubes da partida
	 * @param aleatorio usado só no sorteio do vermelho direto, na ordem dos eventos
	 */
	public Disciplina aposPartida(Collection<Long> jogadoresDosClubes, ResultadoPartida resultado,
			ParametrosSuspensao parametros, RandomGenerator aleatorio) {
		Map<Long, Integer> novosAmarelos = new HashMap<>(amarelos);
		Map<Long, Integer> novasSuspensoes = new HashMap<>(suspensoes);
		for (long id : jogadoresDosClubes) {
			novasSuspensoes.computeIfPresent(id, (k, jogos) -> jogos - 1);
		}
		for (EventoPartida evento : resultado.eventos()) {
			long id = evento.jogador().id();
			switch (evento.tipo()) {
				case CARTAO_AMARELO -> somarAmarelo(id, novosAmarelos, novasSuspensoes, parametros);
				case SEGUNDO_AMARELO -> {
					somarAmarelo(id, novosAmarelos, novasSuspensoes, parametros);
					novasSuspensoes.merge(id, 1, Integer::sum);
				}
				case CARTAO_VERMELHO -> novasSuspensoes.merge(id, sortearJogos(parametros, aleatorio), Integer::sum);
				default -> {
				}
			}
		}
		return new Disciplina(novosAmarelos, novasSuspensoes);
	}

	/** Na virada da temporada os amarelos zeram; suspensões pendentes continuam (SPEC §7.4). */
	public Disciplina novaTemporada() {
		return new Disciplina(Map.of(), suspensoes);
	}

	private static void somarAmarelo(long id, Map<Long, Integer> amarelos, Map<Long, Integer> suspensoes,
			ParametrosSuspensao parametros) {
		int total = amarelos.getOrDefault(id, 0) + 1;
		if (total >= parametros.amarelosParaSuspensao()) {
			suspensoes.merge(id, 1, Integer::sum);
			total -= parametros.amarelosParaSuspensao();
		}
		amarelos.put(id, total);
	}

	private static int sortearJogos(ParametrosSuspensao parametros, RandomGenerator aleatorio) {
		int total = parametros.pesosVermelhoDireto().stream().mapToInt(Integer::intValue).sum();
		int alvo = aleatorio.nextInt(total);
		for (int i = 0; i < parametros.pesosVermelhoDireto().size(); i++) {
			alvo -= parametros.pesosVermelhoDireto().get(i);
			if (alvo < 0) {
				return i + 1;
			}
		}
		throw new IllegalStateException("sorteio ponderado sem resultado");
	}

	private static Map<Long, Integer> semZeros(Map<Long, Integer> mapa) {
		Objects.requireNonNull(mapa);
		Map<Long, Integer> limpo = new HashMap<>();
		mapa.forEach((id, valor) -> {
			if (valor < 0) {
				throw new IllegalArgumentException("valor negativo para o jogador " + id + ": " + valor);
			}
			if (valor > 0) {
				limpo.put(id, valor);
			}
		});
		return Map.copyOf(limpo);
	}
}
