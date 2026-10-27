package br.com.managerfoot.dominio;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Os 11 titulares de um time em uma formação. Só pode ser criada se respeitar a formação
 * (SPEC §7.2): 1 goleiro e o número exato de defensores, meias e atacantes, sem repetição.
 */
public record Escalacao(Formacao formacao, List<Jogador> titulares) {

	public Escalacao {
		Objects.requireNonNull(formacao, "formação é obrigatória");
		Objects.requireNonNull(titulares, "lista de titulares é obrigatória");
		titulares = List.copyOf(titulares);
		List<String> problemas = problemas(formacao, titulares);
		if (!problemas.isEmpty()) {
			throw new IllegalArgumentException(
					"Escalação inválida para " + formacao.rotulo() + ": " + String.join("; ", problemas) + ".");
		}
	}

	public List<Jogador> jogadoresNa(Posicao posicao) {
		return titulares.stream().filter(j -> j.posicao() == posicao).toList();
	}

	private static List<String> problemas(Formacao formacao, List<Jogador> titulares) {
		List<String> problemas = new ArrayList<>();
		if (titulares.size() != Formacao.TITULARES) {
			problemas.add("são necessários " + Formacao.TITULARES + " titulares, mas há " + titulares.size());
		}

		Set<Long> ids = new HashSet<>();
		for (Jogador jogador : titulares) {
			if (!ids.add(jogador.id())) {
				problemas.add(jogador.nome() + " aparece mais de uma vez");
			}
		}

		Map<Posicao, Integer> contagem = new EnumMap<>(Posicao.class);
		titulares.forEach(j -> contagem.merge(j.posicao(), 1, Integer::sum));
		for (Posicao posicao : Posicao.values()) {
			int esperado = formacao.quantidade(posicao);
			int atual = contagem.getOrDefault(posicao, 0);
			if (atual < esperado) {
				problemas.add("faltam " + (esperado - atual) + " " + posicao.nome(esperado - atual));
			} else if (atual > esperado) {
				problemas.add("sobram " + (atual - esperado) + " " + posicao.nome(atual - esperado));
			}
		}
		return problemas;
	}
}
