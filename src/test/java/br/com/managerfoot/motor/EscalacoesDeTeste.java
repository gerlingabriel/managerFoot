package br.com.managerfoot.motor;

import java.util.ArrayList;
import java.util.List;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

/** Escalações válidas com todos os titulares da mesma força. */
final class EscalacoesDeTeste {

	private EscalacoesDeTeste() {
	}

	/** {@code primeiroId} evita ids repetidos entre os dois times de uma partida. */
	static Escalacao escalacao(Formacao formacao, int forca, long primeiroId) {
		return escalacao(formacao, forca, primeiroId, Temperamento.NORMAL);
	}

	static Escalacao escalacao(Formacao formacao, int forca, long primeiroId, Temperamento temperamento) {
		return new Escalacao(formacao, titulares(formacao, forca, primeiroId, temperamento));
	}

	static List<Jogador> titulares(Formacao formacao, int forca, long primeiroId, Temperamento temperamento) {
		List<Jogador> titulares = new ArrayList<>();
		long id = primeiroId;
		for (Posicao posicao : Posicao.values()) {
			for (int i = 0; i < formacao.quantidade(posicao); i++) {
				titulares.add(new Jogador(id, "J" + id, posicao, forca, temperamento));
				id++;
			}
		}
		return titulares;
	}
}
