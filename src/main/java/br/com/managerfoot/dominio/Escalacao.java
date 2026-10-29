package br.com.managerfoot.dominio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Os titulares de um time em uma formação, mais o banco. Só pode ser criada se respeitar a
 * formação (SPEC §7.2): 1 goleiro e o número exato de defensores, meias e atacantes, sem
 * repetição de jogadores entre titulares e reservas.
 *
 * <p>Com menos de 11 (de {@link #MINIMO_TITULARES} a 10), a formação fica com buracos, como se o
 * time começasse com expulsos (SPEC §9.3): cada setor tem no máximo as vagas da formação. Quem
 * decide se o clube pode jogar incompleto é quem monta a escalação (só quando faltam jogadores
 * disponíveis que caibam nas vagas).
 *
 * @param reservas banco para a partida, até {@link #MAXIMO_RESERVAS} (SPEC §7.4.1)
 * @param batedorPenalti escolha do treinador (pode ser nula); é uma preferência: se ele não estiver
 *        em campo na hora do pênalti, o motor aplica a regra padrão (SPEC §12)
 */
public record Escalacao(Formacao formacao, List<Jogador> titulares, List<Jogador> reservas, Jogador batedorPenalti) {

	public static final int MAXIMO_RESERVAS = 7;

	/** Menos que isso não começa a partida (regra do futebol); SPEC §9.3, provisório. */
	public static final int MINIMO_TITULARES = 7;

	public Escalacao {
		Objects.requireNonNull(formacao, "formação é obrigatória");
		Objects.requireNonNull(titulares, "lista de titulares é obrigatória");
		Objects.requireNonNull(reservas, "lista de reservas é obrigatória (pode ser vazia)");
		titulares = List.copyOf(titulares);
		reservas = List.copyOf(reservas);
		List<String> problemas = problemas(formacao, titulares, reservas, true);
		if (!problemas.isEmpty()) {
			throw new IllegalArgumentException(
					"Escalação inválida para " + formacao.rotulo() + ": " + String.join("; ", problemas) + ".");
		}
	}

	/** Escalação sem reservas e sem batedor de pênalti escolhido. */
	public Escalacao(Formacao formacao, List<Jogador> titulares) {
		this(formacao, titulares, List.of(), null);
	}

	public Escalacao(Formacao formacao, List<Jogador> titulares, Jogador batedorPenalti) {
		this(formacao, titulares, List.of(), batedorPenalti);
	}

	public Escalacao(Formacao formacao, List<Jogador> titulares, List<Jogador> reservas) {
		this(formacao, titulares, reservas, null);
	}

	public Optional<Jogador> batedorPreferido() {
		return Optional.ofNullable(batedorPenalti);
	}

	public List<Jogador> jogadoresNa(Posicao posicao) {
		return titulares.stream().filter(j -> j.posicao() == posicao).toList();
	}

	/** Se a formação tem vagas sem jogador (time com um a menos, SPEC §9.3). */
	public boolean incompleta() {
		return titulares.size() < Formacao.TITULARES;
	}

	/**
	 * Problemas que impedem a escalação, em mensagens para a tela; vazia se ela é válida. Aceita
	 * de {@link #MINIMO_TITULARES} a 11 titulares; para exigir os 11, use {@link #problemas}.
	 */
	public static List<String> problemasPermitindoBuracos(Formacao formacao, List<Jogador> titulares,
			List<Jogador> reservas) {
		return problemas(formacao, titulares, reservas, true);
	}

	/** Problemas que impedem a escalação completa, em mensagens para a tela; vazia se ela é válida. */
	public static List<String> problemas(Formacao formacao, List<Jogador> titulares, List<Jogador> reservas) {
		return problemas(formacao, titulares, reservas, false);
	}

	private static List<String> problemas(Formacao formacao, List<Jogador> titulares, List<Jogador> reservas,
			boolean buracos) {
		List<String> problemas = new ArrayList<>();
		int minimo = buracos ? MINIMO_TITULARES : Formacao.TITULARES;
		if (titulares.size() > Formacao.TITULARES || titulares.size() < minimo) {
			problemas.add("são necessários " + (buracos ? "de " + minimo + " a " : "") + Formacao.TITULARES
					+ " titulares, mas há " + titulares.size());
		}
		if (reservas.size() > MAXIMO_RESERVAS) {
			problemas.add("o banco aceita até " + MAXIMO_RESERVAS + " reservas, mas há " + reservas.size());
		}

		Set<Long> ids = new HashSet<>();
		List<Jogador> todos = new ArrayList<>(titulares);
		todos.addAll(reservas);
		for (Jogador jogador : todos) {
			if (!ids.add(jogador.id())) {
				problemas.add(jogador.nome() + " aparece mais de uma vez");
			}
		}

		Map<Posicao, Integer> contagem = new EnumMap<>(Posicao.class);
		titulares.forEach(j -> contagem.merge(j.posicao(), 1, Integer::sum));
		// com buracos, a vaga vazia só é problema no gol ou quando outro setor tem jogador sobrando
		boolean sobra = Arrays.stream(Posicao.values())
			.anyMatch(p -> contagem.getOrDefault(p, 0) > formacao.quantidade(p));
		for (Posicao posicao : Posicao.values()) {
			int esperado = formacao.quantidade(posicao);
			int atual = contagem.getOrDefault(posicao, 0);
			if (atual < esperado && (!buracos || sobra || posicao == Posicao.GOLEIRO)) {
				problemas.add("faltam " + (esperado - atual) + " " + posicao.nome(esperado - atual));
			} else if (atual > esperado) {
				problemas.add("sobram " + (atual - esperado) + " " + posicao.nome(atual - esperado));
			}
		}
		return problemas;
	}
}
