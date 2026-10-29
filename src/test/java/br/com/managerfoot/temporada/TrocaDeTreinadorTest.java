package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.temporada.TrocaDeTreinador.Candidato;

class TrocaDeTreinadorTest {

	private static final Moral VERDE = new Moral(15);
	private static final Moral AMARELA = new Moral(14);

	private final TrocaDeTreinador troca = TrocaDeTreinador.padrao();

	@Test
	void chamaOInferiorDeMoralVerdeMaisForte() {
		// exemplo do usuário: o clube de força 35 demite e o de força 29, com moral verde, cede o treinador
		List<Candidato> clubes = List.of(maquina(1, 40, VERDE), maquina(2, 35, VERDE), maquina(3, 31, AMARELA),
				maquina(4, 29, VERDE), maquina(5, 20, VERDE));

		// o de força 20 fica 15 abaixo, além do limite de 12
		assertThat(troca.chamados(35, clubes)).extracting(Candidato::clubeId).containsExactly(4L);
	}

	@Test
	void usuarioEntreOsTresMaisFortesDecidePrimeiro() {
		List<Candidato> clubes = List.of(maquina(1, 34, VERDE), maquina(2, 32, VERDE), usuario(3, 30, VERDE),
				maquina(4, 28, VERDE), usuario(5, 26, VERDE));

		// o usuário 3 está entre os 3 mais fortes; o usuário 5 não, e fica na ordem da força
		assertThat(troca.chamados(35, clubes)).extracting(Candidato::clubeId).containsExactly(3L, 1L, 2L, 4L, 5L);
	}

	@Test
	void semInferiorDeMoralVerdeNinguemComClubeVem() {
		List<Candidato> clubes = List.of(maquina(1, 40, VERDE), maquina(2, 20, AMARELA), maquina(3, 20, new Moral(3)));

		assertThat(troca.chamados(35, clubes)).isEmpty();
		assertThat(troca.aceitaTreinadorSemClube(35)).isFalse();
		assertThat(troca.aceitaTreinadorSemClube(10)).isTrue();
		assertThat(troca.aceitaTreinadorSemClube(10.5)).isFalse();
	}

	@Test
	void mesmaForcaNaoEInferiorEEmpateDeForcaVaiPeloClube() {
		List<Candidato> clubes = List.of(maquina(9, 30, VERDE), maquina(7, 30, VERDE), maquina(8, 35, VERDE));

		assertThat(troca.chamados(35, clubes)).extracting(Candidato::clubeId).containsExactly(7L, 9L);
	}

	@Test
	void clubeDoisPontosAlemDeDozeAbaixoNaoPodeAssumir() {
		// força 50 com moral vermelha: o de força 20 não serve; o de 38 (12 abaixo) serve
		List<Candidato> clubes = List.of(maquina(1, 20, VERDE), maquina(2, 37.9, VERDE), maquina(3, 38, VERDE));

		assertThat(troca.chamados(50, clubes)).extracting(Candidato::clubeId).containsExactly(3L);
	}

	@Test
	void osDoisComecamComAMoralInicial() {
		assertThat(troca.moralDepoisDaTroca()).isEqualTo(new Moral(22));
	}

	private static Candidato maquina(long id, double forca, Moral moral) {
		return new Candidato(id, forca, moral, false);
	}

	private static Candidato usuario(long id, double forca, Moral moral) {
		return new Candidato(id, forca, moral, true);
	}
}
