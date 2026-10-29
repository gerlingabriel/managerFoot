package br.com.managerfoot.persistencia;

/**
 * A rodada não começa enquanto um treinador tem uma decisão sem resposta: o patrocínio da temporada
 * (modelo 9a) ou um pedido de aumento (modelo 10a). A mensagem pode ser mostrada ao treinador.
 */
public class DecisaoPendenteException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	public DecisaoPendenteException(String mensagem) {
		super(mensagem);
	}
}
