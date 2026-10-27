package br.com.managerfoot.dominio;

import java.util.ArrayList;
import java.util.List;

/** Monta titulares válidos para testes, com ids sequenciais. */
final class JogadoresDeTeste {

	private JogadoresDeTeste() {
	}

	static List<Jogador> titularesPara(Formacao formacao, int forca) {
		List<Jogador> titulares = new ArrayList<>();
		long id = 1;
		for (Posicao posicao : Posicao.values()) {
			for (int i = 0; i < formacao.quantidade(posicao); i++) {
				titulares.add(new Jogador(id, "Jogador " + id, posicao, forca, Temperamento.NORMAL));
				id++;
			}
		}
		return titulares;
	}
}
