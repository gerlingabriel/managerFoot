package br.com.managerfoot.multiplayer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.multiplayer.Turno.Passo;
import br.com.managerfoot.multiplayer.Turno.Situacao;
import br.com.managerfoot.multiplayer.Turno.Treinador;

class TurnoTest {

	private static final Instant AGORA = Instant.parse("2026-11-03T20:00:00Z");
	private static final Duration TRINTA = Duration.ofSeconds(30);

	@Test
	void faltamListaQuemNaoDeuOkEmPortugues() {
		Turno turno = new Turno("t1/r6/tatica", Passo.PLANTEL, List.of(treinador(1, "Gabriel", true, AGORA),
				treinador(2, "Marcos", true, AGORA), treinador(3, "Lúcia", false, AGORA), treinador(4, "Renata", false, AGORA)));

		assertThat(turno.comOk()).isEqualTo(2);
		assertThat(turno.faltam()).isEqualTo("Lúcia e Renata");
		assertThat(turno.todosDeramOk()).isFalse();
		assertThat(turno.deuOk(2)).isTrue();
		assertThat(turno.deuOk(3)).isFalse();
		assertThat(new Turno("x", Passo.PLANTEL, List.of(treinador(1, "Gabriel", false, AGORA))).faltam()).isEqualTo("Gabriel");
		assertThat(new Turno("x", Passo.PLANTEL, List.of(treinador(1, "A", false, AGORA), treinador(2, "B", false, AGORA),
				treinador(3, "C", false, AGORA))).faltam()).isEqualTo("A, B e C");
	}

	@Test
	void semConexaoSoQuemAindaNaoDeuOk() {
		Instant antigo = AGORA.minusSeconds(31);

		assertThat(treinador(1, "A", false, antigo).situacao(AGORA, TRINTA)).isEqualTo(Situacao.SEM_CONEXAO);
		assertThat(treinador(1, "A", true, antigo).situacao(AGORA, TRINTA)).isEqualTo(Situacao.OK);
		assertThat(treinador(1, "A", false, AGORA.minusSeconds(30)).situacao(AGORA, TRINTA)).isEqualTo(Situacao.MONTANDO);
	}

	private static Treinador treinador(long id, String nome, boolean ok, Instant visto) {
		return new Treinador(id, nome, "Clube " + id, ok, visto);
	}
}
