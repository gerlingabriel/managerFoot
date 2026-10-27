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
				titulares.add(new Jogador(id, "J" + id, posicao, forca, temperamento, "BRA"));
				id++;
			}
		}
		return titulares;
	}

	/** 4-4-2 com os {@code quantidade} primeiros jogadores com asterisco: atacantes, depois meias, depois defensores. */
	static Escalacao comAsteriscos(int quantidade, int forca, long primeiroId) {
		List<Jogador> titulares = new ArrayList<>(titulares(Formacao.F_4_4_2, forca, primeiroId, Temperamento.NORMAL));
		int marcados = 0;
		for (Posicao posicao : new Posicao[] { Posicao.ATACANTE, Posicao.MEIO_CAMPISTA, Posicao.DEFENSOR }) {
			for (int i = 0; i < titulares.size() && marcados < quantidade; i++) {
				Jogador j = titulares.get(i);
				if (j.posicao() == posicao) {
					titulares.set(i, j.comAsterisco(true));
					marcados++;
				}
			}
		}
		return new Escalacao(Formacao.F_4_4_2, titulares);
	}
}
