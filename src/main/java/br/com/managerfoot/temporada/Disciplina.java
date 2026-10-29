package br.com.managerfoot.temporada;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.ResultadoPartida;

/**
 * Amarelos acumulados na temporada, jogos de suspensão a cumprir (SPEC §7.4) e lesões (SPEC §7.5).
 * Imutável.
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
 * <li>A lesão segue a mesma conta: quem estava lesionado antes da partida do clube cumpre um jogo
 * fora; quem se lesiona fica fora os jogos sorteados, a partir da partida seguinte.</li>
 * </ul>
 *
 * @param amarelos amarelos acumulados por id do jogador; ausente é zero
 * @param suspensoes jogos de suspensão a cumprir por id do jogador; ausente é zero
 * @param lesoes jogos fora por lesão a cumprir por id do jogador; ausente é zero
 * @param lesoesDosClubes lesões na temporada por id do clube (limite da SPEC §7.5); ausente é zero
 */
public record Disciplina(Map<Long, Integer> amarelos, Map<Long, Integer> suspensoes, Map<Long, Integer> lesoes,
		Map<Long, Integer> lesoesDosClubes) {

	public Disciplina {
		amarelos = semZeros(amarelos);
		suspensoes = semZeros(suspensoes);
		lesoes = semZeros(lesoes);
		lesoesDosClubes = semZeros(lesoesDosClubes);
	}

	/** Sem lesões. */
	public Disciplina(Map<Long, Integer> amarelos, Map<Long, Integer> suspensoes) {
		this(amarelos, suspensoes, Map.of(), Map.of());
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

	public int lesaoDe(long jogadorId) {
		return lesoes.getOrDefault(jogadorId, 0);
	}

	public boolean lesionado(long jogadorId) {
		return lesaoDe(jogadorId) > 0;
	}

	/** Suspenso ou lesionado: não pode ser escalado. */
	public boolean indisponivel(long jogadorId) {
		return suspenso(jogadorId) || lesionado(jogadorId);
	}

	public int lesoesDoClube(long clubeId) {
		return lesoesDosClubes.getOrDefault(clubeId, 0);
	}

	/** Como {@link #aposPartida(Collection, ResultadoPartida, ParametrosSuspensao, RandomGenerator, Map)}, sem contar as lesões por clube. */
	public Disciplina aposPartida(Collection<Long> jogadoresDosClubes, ResultadoPartida resultado,
			ParametrosSuspensao parametros, RandomGenerator aleatorio) {
		return aposPartida(jogadoresDosClubes, resultado, parametros, aleatorio, Map.of());
	}

	/**
	 * @param jogadoresDosClubes ids dos planteis dos dois clubes da partida
	 * @param aleatorio usado só no sorteio do vermelho direto, na ordem dos eventos
	 * @param clubes id do clube de cada lado, para contar as lesões da temporada
	 */
	public Disciplina aposPartida(Collection<Long> jogadoresDosClubes, ResultadoPartida resultado,
			ParametrosSuspensao parametros, RandomGenerator aleatorio, Map<Lado, Long> clubes) {
		Map<Long, Integer> novosAmarelos = new HashMap<>(amarelos);
		Map<Long, Integer> novasSuspensoes = new HashMap<>(suspensoes);
		Map<Long, Integer> novasLesoes = new HashMap<>(lesoes);
		Map<Long, Integer> novasDosClubes = new HashMap<>(lesoesDosClubes);
		for (long id : jogadoresDosClubes) {
			novasSuspensoes.computeIfPresent(id, (k, jogos) -> jogos - 1);
			novasLesoes.computeIfPresent(id, (k, jogos) -> jogos - 1);
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
				case LESAO -> {
					novasLesoes.merge(id, evento.lesao().jogos(), Math::max);
					Long clube = clubes.get(evento.lado());
					if (clube != null) {
						novasDosClubes.merge(clube, 1, Integer::sum);
					}
				}
				default -> {
				}
			}
		}
		return new Disciplina(novosAmarelos, novasSuspensoes, novasLesoes, novasDosClubes);
	}

	/**
	 * Na virada da temporada os amarelos e a contagem de lesões dos clubes zeram; suspensões e
	 * lesões pendentes continuam (SPEC §7.4, §7.5).
	 */
	public Disciplina novaTemporada() {
		return new Disciplina(Map.of(), suspensoes, lesoes, Map.of());
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
