package br.com.managerfoot.multiplayer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.multiplayer.Sala.ClubeNaSala;
import br.com.managerfoot.multiplayer.Sala.Participante;
import br.com.managerfoot.multiplayer.Sala.Situacao;

class SalaTest {

	private static final Instant AGORA = Instant.parse("2026-11-03T20:00:00Z");
	private static final Duration TRINTA = Duration.ofSeconds(30);

	private static Participante com(long id, String nome, boolean clube, Duration desde) {
		return new Participante(id, nome, id == 1, clube ? Optional.of(new ClubeNaSala(new Clube(id, "Clube " + id, 20), "Série A"))
				: Optional.empty(), AGORA.minus(desde));
	}

	@Test
	void situacaoPeloClubeEPelaUltimaConsulta() {
		assertThat(com(1, "Gabriel", true, Duration.ofSeconds(2)).situacao(AGORA, TRINTA)).isEqualTo(Situacao.PRONTO);
		assertThat(com(2, "Lúcia", false, Duration.ofSeconds(2)).situacao(AGORA, TRINTA)).isEqualTo(Situacao.ESCOLHENDO);
		assertThat(com(3, "Renata", true, Duration.ofSeconds(31)).situacao(AGORA, TRINTA)).isEqualTo(Situacao.SEM_CONEXAO);
		// exatamente no limite ainda conta como conectado
		assertThat(com(4, "Marcos", true, TRINTA).situacao(AGORA, TRINTA)).isEqualTo(Situacao.PRONTO);
		// sem conexão vale mais que escolhendo
		assertThat(com(5, "Ana", false, Duration.ofMinutes(2)).situacao(AGORA, TRINTA)).isEqualTo(Situacao.SEM_CONEXAO);
	}

	@Test
	void soComecaComTodosProntosEDizOQueFalta() {
		Sala sala = new Sala(9, "Gabriel", ModoRede.ESCOLHA, List.of(com(1, "Gabriel", true, Duration.ZERO),
				com(2, "Marcos", true, Duration.ofSeconds(5)), com(3, "Lúcia", false, Duration.ofSeconds(1)),
				com(4, "Renata", true, Duration.ofMinutes(2))));

		assertThat(sala.podeComecar(AGORA, TRINTA)).isFalse();
		assertThat(sala.falta(AGORA, TRINTA)).isEqualTo("Lúcia escolher o clube; Renata voltar");
		assertThat(sala.conectados(AGORA, TRINTA)).isEqualTo(3);
	}

	@Test
	void administradorSozinhoComClubePodeComecar() {
		Sala sala = new Sala(9, "Gabriel", ModoRede.SORTEIO, List.of(com(1, "Gabriel", true, Duration.ZERO)));
		assertThat(sala.podeComecar(AGORA, TRINTA)).isTrue();
		assertThat(sala.falta(AGORA, TRINTA)).isEmpty();
	}
}
