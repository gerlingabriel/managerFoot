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

		assertThat(troca.chamados(35, clubes)).extracting(Candidato::clubeId).containsExactly(4L, 5L);
	}

	@Test
	void usuarioEntreOsTresMaisFortesDecidePrimeiro() {
		List<Candidato> clubes = List.of(maquina(1, 30, VERDE), maquina(2, 28, VERDE), usuario(3, 26, VERDE),
				maquina(4, 24, VERDE), usuario(5, 22, VERDE));

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
