package br.com.managerfoot.contas;

/** O link de cadastro não existe, já foi usado ou expirou. */
public class LinkInvalidoException extends IllegalArgumentException {

	public LinkInvalidoException(String mensagem) {
		super(mensagem);
	}
}
