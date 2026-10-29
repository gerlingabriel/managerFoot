package br.com.managerfoot.persistencia;

/** Ampliação do estádio recusada por uma regra; a mensagem pode ser mostrada ao treinador. */
public class AmpliacaoRecusadaException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	public AmpliacaoRecusadaException(String mensagem) {
		super(mensagem);
	}
}
