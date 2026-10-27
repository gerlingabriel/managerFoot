package br.com.managerfoot.motor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class SorteioTest {

	@Test
	void itemComPesoZeroNuncaESorteado() {
		SplittableRandom aleatorio = new SplittableRandom(1);
		List<String> itens = List.of("a", "b");
		IntStream.range(0, 1000)
			.forEach(i -> assertThat(Sorteio.ponderado(itens, s -> s.equals("a") ? 0 : 1, aleatorio)).isEqualTo("b"));
	}

	@Test
	void frequenciaAcompanhaOPeso() {
		// semente fixa: pesos 1 e 3 devem ficar perto de 25% / 75% em 10.000 sorteios
		SplittableRandom aleatorio = new SplittableRandom(2);
		List<String> itens = List.of("fraco", "forte");
		Map<String, Long> contagem = IntStream.range(0, 10_000)
			.mapToObj(i -> Sorteio.ponderado(itens, s -> s.equals("forte") ? 3 : 1, aleatorio))
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		assertThat(contagem.get("forte")).isBetween(7_300L, 7_700L);
	}

	@Test
	void recusaListaSemPesoPositivo() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Sorteio.ponderado(List.of("a"), s -> 0, new SplittableRandom(1)));
	}
}
