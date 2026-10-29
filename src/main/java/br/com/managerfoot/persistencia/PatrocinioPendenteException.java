package br.com.managerfoot.persistencia;

/** A rodada não começa enquanto um treinador não escolhe o patrocínio da temporada (modelo 9a). */
public class PatrocinioPendenteException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	public PatrocinioPendenteException(String mensagem) {
		super(mensagem);
	}
}
