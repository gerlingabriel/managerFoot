package br.com.managerfoot.persistencia;

/** Pedido da sala do jogo em rede que não pode ser feito agora; a mensagem pode ser mostrada ao treinador. */
public class SalaRecusadaException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	public SalaRecusadaException(String mensagem) {
		super(mensagem);
	}
}
