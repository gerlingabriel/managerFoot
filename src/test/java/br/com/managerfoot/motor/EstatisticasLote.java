package br.com.managerfoot.motor;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import br.com.managerfoot.dominio.Escalacao;

/**
 * Agrega N partidas simuladas com sementes consecutivas (reproduzível). Ferramenta de calibração
 * (SPEC §7.6), não faz parte do jogo.
 */
final class EstatisticasLote {

	final int partidas;
	long golsCasa;
	long golsFora;
	int vitoriasCasa;
	int empates;
	int vitoriasFora;
	long amarelos;
	long expulsoes;
	long penaltis;
	long penaltisConvertidos;
	final Map<String, Integer> placares = new TreeMap<>();

	private EstatisticasLote(int partidas) {
		this.partidas = partidas;
	}

	static EstatisticasLote medir(MotorPartida motor, Escalacao casa, Escalacao fora, long sementeInicial,
			int partidas) {
		return medir(motor, casa, fora, CondicoesPartida.NEUTRAS, sementeInicial, partidas);
	}

	static EstatisticasLote medir(MotorPartida motor, Escalacao casa, Escalacao fora, CondicoesPartida condicoes,
			long sementeInicial, int partidas) {
		EstatisticasLote est = new EstatisticasLote(partidas);
		for (long s = sementeInicial; s < sementeInicial + partidas; s++) {
			ResultadoPartida r = motor.simular(RegrasPartida.padrao(), casa, fora, condicoes, s);
			est.golsCasa += r.golsCasa();
			est.golsFora += r.golsFora();
			if (r.golsCasa() > r.golsFora()) {
				est.vitoriasCasa++;
			} else if (r.golsCasa() == r.golsFora()) {
				est.empates++;
			} else {
				est.vitoriasFora++;
			}
			est.placares.merge(r.golsCasa() + "x" + r.golsFora(), 1, Integer::sum);
			for (EventoPartida e : r.eventos()) {
				switch (e.tipo()) {
					case CARTAO_AMARELO, SEGUNDO_AMARELO -> est.amarelos++;
					default -> {
					}
				}
				if (e.tipo().expulsao()) {
					est.expulsoes++;
				}
				if (e.penalti()) {
					est.penaltis++;
					if (e.tipo() == TipoEvento.GOL) {
						est.penaltisConvertidos++;
					}
				}
			}
		}
		return est;
	}

	double mediaGols() {
		return (double) (golsCasa + golsFora) / partidas;
	}

	double pctCasa() {
		return 100.0 * vitoriasCasa / partidas;
	}

	double pctEmpate() {
		return 100.0 * empates / partidas;
	}

	double pctFora() {
		return 100.0 * vitoriasFora / partidas;
	}

	double porPartida(long total) {
		return (double) total / partidas;
	}

	/** Os placares mais frequentes, do mais comum ao menos comum. */
	Map<String, Integer> placaresMaisComuns(int limite) {
		return placares.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
			.limit(limite)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	/** Partidas em que algum time fez 5 gols ou mais. */
	double pctGoleadas() {
		int goleadas = placares.entrySet().stream()
			.filter(e -> {
				String[] g = e.getKey().split("x");
				return Integer.parseInt(g[0]) >= 5 || Integer.parseInt(g[1]) >= 5;
			})
			.mapToInt(Map.Entry::getValue)
			.sum();
		return 100.0 * goleadas / partidas;
	}
}
