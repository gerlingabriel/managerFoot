package br.com.managerfoot.contas;

/** Já existe uma conta com o e-mail informado. */
public class EmailEmUsoException extends IllegalArgumentException {

	public EmailEmUsoException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
