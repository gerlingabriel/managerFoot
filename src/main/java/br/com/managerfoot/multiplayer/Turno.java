package br.com.managerfoot.multiplayer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * O OK de todos no jogo em rede depois que a temporada começou (SPEC §3.3 itens 4 a 7, rev. 90 e 92;
 * modelo 18e): em cada momento (tática, lances, resultado dos leilões, etapa de copa, fim de temporada),
 * o jogo só segue quando todos derem OK. Ninguém joga pelo ausente.
 *
 * @param momento o passo do jogo em que os OK valem
 * @param treinadores na ordem de entrada na sala
 */
public record Turno(String momento, Passo passo, List<Treinador> treinadores) {

	public Turno {
		Objects.requireNonNull(momento);
		Objects.requireNonNull(passo);
		treinadores = List.copyOf(treinadores);
	}

	/** O que o OK confirma. */
	public enum Passo {
		/** Tática da rodada ou da etapa, ou o fim da temporada: o OK do plantel. */
		PLANTEL,
		/** Os lances dos leilões antes da rodada. */
		LANCES,
		/** O resultado dos leilões visto: "Ir para a rodada". */
		RESULTADO
	}

	/** Situação de cada treinador no momento. */
	public enum Situacao {
		OK, MONTANDO, SEM_CONEXAO
	}

	/**
	 * Treinador do jogo em rede.
	 *
	 * @param clube nome do clube que ele treina
	 * @param vistoEm última consulta do navegador dele ao servidor
	 */
	public record Treinador(long usuarioId, String nome, String clube, boolean ok, Instant vistoEm) {

		/** Com o OK dado, ficar sem conexão não atrasa ninguém: vale o OK. */
		public Situacao situacao(Instant agora, Duration semConexaoApos) {
			if (ok) {
				return Situacao.OK;
			}
			return vistoEm.plus(semConexaoApos).isBefore(agora) ? Situacao.SEM_CONEXAO : Situacao.MONTANDO;
		}
	}

	public long comOk() {
		return treinadores.stream().filter(Treinador::ok).count();
	}

	public boolean todosDeramOk() {
		return treinadores.stream().allMatch(Treinador::ok);
	}

	public boolean deuOk(long usuarioId) {
		return treinadores.stream().anyMatch(t -> t.usuarioId() == usuarioId && t.ok());
	}

	/** "Lúcia e Renata": quem falta dar OK, ou vazio. */
	public String faltam() {
		List<String> nomes = treinadores.stream().filter(t -> !t.ok()).map(Treinador::nome).toList();
		if (nomes.size() <= 1) {
			return String.join("", nomes);
		}
		return String.join(", ", nomes.subList(0, nomes.size() - 1)) + " e " + nomes.getLast();
	}
}
