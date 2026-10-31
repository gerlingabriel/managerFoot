package br.com.managerfoot.persistencia;

/** Save recusado, ao baixar ou ao enviar; a mensagem pode ser mostrada ao treinador. */
public class SaveRecusadoException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	public SaveRecusadoException(String mensagem) {
		super(mensagem);
	}
}
