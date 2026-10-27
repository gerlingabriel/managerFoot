package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.ResultadoPartida;
import br.com.managerfoot.motor.TipoEvento;

/**
 * Gols de cada jogador na temporada e na carreira (SPEC §5.2). Conta os gols de todas as
 * competições, inclusive pênaltis durante a partida. Gols de disputa de pênaltis não passam por
 * aqui: não são eventos da partida. Imutável: cada registro devolve uma nova artilharia.
 *
 * @param golsTemporada gols por id de jogador na temporada atual
 * @param golsCarreira gols por id de jogador desde o início do jogo salvo
 */
public record Artilharia(Map<Long, Integer> golsTemporada, Map<Long, Integer> golsCarreira) {

	/** Uma linha do ranking. Empatados dividem a posição (1º, 1º, 3º). */
	public record Artilheiro(int posicao, long jogadorId, int gols) {
	}

	public Artilharia {
		golsTemporada = Map.copyOf(Objects.requireNonNull(golsTemporada));
		golsCarreira = Map.copyOf(Objects.requireNonNull(golsCarreira));
	}

	/** Início do jogo salvo: todos começam com zero gols. */
	public static Artilharia vazia() {
		return new Artilharia(Map.of(), Map.of());
	}

	/** Soma os gols de uma partida de qualquer competição. */
	public Artilharia registrar(ResultadoPartida resultado) {
		Map<Long, Integer> temporada = new HashMap<>(golsTemporada);
		Map<Long, Integer> carreira = new HashMap<>(golsCarreira);
		for (EventoPartida evento : resultado.eventos()) {
			if (evento.tipo() == TipoEvento.GOL) {
				temporada.merge(evento.jogador().id(), 1, Integer::sum);
				carreira.merge(evento.jogador().id(), 1, Integer::sum);
			}
		}
		return new Artilharia(temporada, carreira);
	}

	/** Virada de temporada: zera os gols da temporada e mantém os da carreira. */
	public Artilharia novaTemporada() {
		return new Artilharia(Map.of(), golsCarreira);
	}

	public int golsNaTemporada(long jogadorId) {
		return golsTemporada.getOrDefault(jogadorId, 0);
	}

	public int golsNaCarreira(long jogadorId) {
		return golsCarreira.getOrDefault(jogadorId, 0);
	}

	/** Artilheiros da temporada, de quem fez mais gols; só quem marcou. */
	public List<Artilheiro> rankingTemporada() {
		return ranking(golsTemporada);
	}

	/** Artilheiros de todos os tempos, pelos gols na carreira; só quem marcou. */
	public List<Artilheiro> rankingCarreira() {
		return ranking(golsCarreira);
	}

	/**
	 * Quem divide o prêmio de artilheiro da temporada (SPEC §8.1): todos os empatados com mais gols.
	 * Vazio se ninguém marcou.
	 */
	public Set<Long> artilheirosDaTemporada() {
		return rankingTemporada().stream()
			.filter(a -> a.posicao() == 1)
			.map(Artilheiro::jogadorId)
			.collect(Collectors.toUnmodifiableSet());
	}

	private static List<Artilheiro> ranking(Map<Long, Integer> gols) {
		// a ordem entre empatados (por id) é só para a exibição ser estável; a posição é a mesma
		List<Map.Entry<Long, Integer>> ordenados = gols.entrySet().stream()
			.filter(e -> e.getValue() > 0)
			.sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder())
				.thenComparing(Map.Entry.comparingByKey()))
			.toList();
		List<Artilheiro> ranking = new ArrayList<>();
		for (int i = 0; i < ordenados.size(); i++) {
			Map.Entry<Long, Integer> e = ordenados.get(i);
			int posicao = i > 0 && e.getValue().equals(ordenados.get(i - 1).getValue())
					? ranking.get(i - 1).posicao()
					: i + 1;
			ranking.add(new Artilheiro(posicao, e.getKey(), e.getValue()));
		}
		return List.copyOf(ranking);
	}
}
