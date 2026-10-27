package br.com.managerfoot.motor;

import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.random.RandomGenerator;

/** Sorteio ponderado: cada item tem chance proporcional ao seu peso. */
final class Sorteio {

	private Sorteio() {
	}

	static <T> T ponderado(List<T> itens, ToDoubleFunction<T> peso, RandomGenerator aleatorio) {
		double total = itens.stream().mapToDouble(peso).sum();
		if (itens.isEmpty() || total <= 0) {
			throw new IllegalArgumentException("sorteio ponderado precisa de ao menos um item com peso positivo");
		}
		double ponto = aleatorio.nextDouble() * total;
		double acumulado = 0;
		for (T item : itens) {
			acumulado += peso.applyAsDouble(item);
			if (ponto < acumulado) {
				return item;
			}
		}
		// arredondamento de ponto flutuante: devolve o último item com peso
		for (int i = itens.size() - 1; i >= 0; i--) {
			if (peso.applyAsDouble(itens.get(i)) > 0) {
				return itens.get(i);
			}
		}
		throw new IllegalStateException("inalcançável");
	}
}
