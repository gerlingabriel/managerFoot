package br.com.managerfoot.contas;

/** A ação exige um papel que o usuário não tem. */
public class AcessoNegadoException extends RuntimeException {

	public AcessoNegadoException(String mensagem) {
		super(mensagem);
	}
}
