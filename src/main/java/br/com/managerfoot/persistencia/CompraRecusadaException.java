package br.com.managerfoot.persistencia;

/** Compra direta recusada por uma regra do mercado; a mensagem pode ser mostrada ao treinador. */
public class CompraRecusadaException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	public CompraRecusadaException(String mensagem) {
		super(mensagem);
	}
}
