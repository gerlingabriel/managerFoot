package br.com.managerfoot.persistencia;

import java.util.List;

/** Banco editável recusado; os problemas podem ser mostrados à pessoa, um por linha. */
public class BancoRecusadoException extends IllegalArgumentException {

	private static final long serialVersionUID = 1L;

	private final List<String> problemas;

	public BancoRecusadoException(List<String> problemas) {
		super(String.join(" ", problemas));
		this.problemas = List.copyOf(problemas);
	}

	public List<String> problemas() {
		return problemas;
	}
}
