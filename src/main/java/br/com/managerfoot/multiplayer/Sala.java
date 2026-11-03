package br.com.managerfoot.multiplayer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import br.com.managerfoot.dominio.Clube;

/**
 * Sala do jogo em rede antes da 1ª rodada (SPEC §3.3, rev. 90 e 92; modelos 18c e 18d). Quem está nela,
 * com ou sem clube, e o que falta para o administrador começar a temporada.
 *
 * @param participantes na ordem de entrada; o administrador é o primeiro
 */
public record Sala(long jogoId, String administrador, ModoRede modo, List<Participante> participantes) {

	public Sala {
		Objects.requireNonNull(modo);
		participantes = List.copyOf(participantes);
	}

	/**
	 * Clube de um participante.
	 *
	 * @param divisao nome da divisão em que o clube joga
	 */
	public record ClubeNaSala(Clube clube, String divisao) {
	}

	/** Situação de cada participante na sala. */
	public enum Situacao {
		PRONTO, ESCOLHENDO, SEM_CONEXAO
	}

	/**
	 * Participante da sala.
	 *
	 * @param clube vazio enquanto escolhe (modo escolha)
	 * @param vistoEm última consulta do navegador dele ao servidor
	 */
	public record Participante(long usuarioId, String nome, boolean administrador, Optional<ClubeNaSala> clube, Instant vistoEm) {

		/** Sem conexão vale mais que escolhendo: quem não está na tela não vai escolher. */
		public Situacao situacao(Instant agora, Duration semConexaoApos) {
			if (vistoEm.plus(semConexaoApos).isBefore(agora)) {
				return Situacao.SEM_CONEXAO;
			}
			return clube.isPresent() ? Situacao.PRONTO : Situacao.ESCOLHENDO;
		}
	}

	public long conectados(Instant agora, Duration semConexaoApos) {
		return participantes.stream().filter(p -> p.situacao(agora, semConexaoApos) != Situacao.SEM_CONEXAO).count();
	}

	/** O administrador só começa com todos prontos: com clube e conectados. */
	public boolean podeComecar(Instant agora, Duration semConexaoApos) {
		return participantes.stream().allMatch(p -> p.situacao(agora, semConexaoApos) == Situacao.PRONTO);
	}

	/** "Lúcia escolher o clube; Renata voltar", ou vazio se todos estão prontos. */
	public String falta(Instant agora, Duration semConexaoApos) {
		List<String> faltas = new ArrayList<>();
		for (Participante p : participantes) {
			switch (p.situacao(agora, semConexaoApos)) {
				case ESCOLHENDO -> faltas.add(p.nome() + " escolher o clube");
				case SEM_CONEXAO -> faltas.add(p.nome() + " voltar");
				case PRONTO -> {
				}
			}
		}
		return String.join("; ", faltas);
	}
}
