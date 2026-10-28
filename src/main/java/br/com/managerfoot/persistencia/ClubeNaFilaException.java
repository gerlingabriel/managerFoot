package br.com.managerfoot.persistencia;

/**
 * O clube do treinador está na fila de espera e não joga a temporada. O que acontece com o treinador
 * rebaixado para a fila ainda está em aberto (SPEC §4.1).
 */
public class ClubeNaFilaException extends RuntimeException {

	public ClubeNaFilaException(String mensagem) {
		super(mensagem);
	}
}
