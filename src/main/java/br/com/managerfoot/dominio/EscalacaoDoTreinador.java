package br.com.managerfoot.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;

/**
 * Escalação que o treinador humano monta no plantel (SPEC §7.2 e §10.3): formação, titulares,
 * banco e batedor de pênalti, por id dos jogadores. Pode estar incompleta enquanto ele monta o
 * time; {@link #problemas} diz o que falta e {@link #escalacao} só a entrega se estiver válida.
 * Quem não está em titulares nem no banco está fora do banco.
 *
 * @param batedor batedor de pênalti escolhido; nulo é o automático
 */
public record EscalacaoDoTreinador(Formacao formacao, Set<Long> titulares, Set<Long> reservas, Long batedor) {

	public EscalacaoDoTreinador {
		Objects.requireNonNull(formacao, "formação é obrigatória");
		titulares = Collections.unmodifiableSet(new LinkedHashSet<>(titulares));
		reservas = Collections.unmodifiableSet(new LinkedHashSet<>(reservas));
		if (titulares.stream().anyMatch(reservas::contains)) {
			throw new IllegalArgumentException("um jogador não pode ser titular e reserva ao mesmo tempo");
		}
	}

	/** A escalação montada pela máquina, como ponto de partida do treinador. */
	public static EscalacaoDoTreinador de(Escalacao escalacao) {
		return new EscalacaoDoTreinador(escalacao.formacao(), ids(escalacao.titulares()), ids(escalacao.reservas()),
				escalacao.batedorPreferido().map(Jogador::id).orElse(null));
	}

	/** Onde o jogador está. */
	public Lugar lugarDe(long jogadorId) {
		if (titulares.contains(jogadorId)) {
			return Lugar.TITULAR;
		}
		return reservas.contains(jogadorId) ? Lugar.BANCO : Lugar.FORA;
	}

	/**
	 * O jogador movido para o lugar (SPEC §10.3, rev. 34). A escalação resultante pode ficar
	 * incompleta; quem vai para fora deixa de ser o batedor.
	 */
	public EscalacaoDoTreinador com(long jogadorId, Lugar lugar) {
		Set<Long> novosTitulares = menos(titulares, Set.of(jogadorId));
		Set<Long> novasReservas = menos(reservas, Set.of(jogadorId));
		switch (lugar) {
			case TITULAR -> novosTitulares.add(jogadorId);
			case BANCO -> novasReservas.add(jogadorId);
			case FORA -> { }
		}
		Long novoBatedor = lugar == Lugar.FORA && Objects.equals(batedor, jogadorId) ? null : batedor;
		return new EscalacaoDoTreinador(formacao, novosTitulares, novasReservas, novoBatedor);
	}

	public EscalacaoDoTreinador com(Formacao novaFormacao) {
		return new EscalacaoDoTreinador(novaFormacao, titulares, reservas, batedor);
	}

	/** @param novoBatedor nulo é o automático */
	public EscalacaoDoTreinador comBatedor(Long novoBatedor) {
		return new EscalacaoDoTreinador(formacao, titulares, reservas, novoBatedor);
	}

	/** A mesma escalação sem esses jogadores, que passam para fora do banco (e deixam de bater pênalti). */
	public EscalacaoDoTreinador sem(Set<Long> jogadores) {
		return new EscalacaoDoTreinador(formacao, menos(titulares, jogadores), menos(reservas, jogadores),
				batedor != null && jogadores.contains(batedor) ? null : batedor);
	}

	/**
	 * O que impede esta escalação de jogar, em mensagens para a tela; vazia se pode jogar.
	 *
	 * @param plantel jogadores do clube
	 * @param suspenso diz se o jogador está suspenso para a próxima partida
	 */
	public List<String> problemas(List<Jogador> plantel, LongPredicate suspenso) {
		Map<Long, Jogador> porId = plantel.stream().collect(Collectors.toMap(Jogador::id, Function.identity()));
		List<String> problemas = new ArrayList<>();
		for (long id : todos()) {
			Jogador jogador = porId.get(id);
			if (jogador == null) {
				problemas.add("o jogador " + id + " não é do clube");
			} else if (suspenso.test(id)) {
				problemas.add(jogador.nome() + " está suspenso");
			}
		}
		if (batedor != null && !titulares.contains(batedor) && !reservas.contains(batedor)) {
			problemas.add("o batedor de pênalti precisa estar entre os titulares ou no banco");
		}
		if (problemas.isEmpty()) {
			problemas.addAll(Escalacao.problemas(formacao, jogadores(titulares, porId), jogadores(reservas, porId)));
		}
		return problemas;
	}

	/**
	 * A escalação para a partida.
	 *
	 * @throws IllegalStateException com os {@link #problemas}, se ela não pode jogar
	 */
	public Escalacao escalacao(List<Jogador> plantel, LongPredicate suspenso) {
		List<String> problemas = problemas(plantel, suspenso);
		if (!problemas.isEmpty()) {
			throw new IllegalStateException("escalação inválida: " + String.join("; ", problemas));
		}
		Map<Long, Jogador> porId = plantel.stream().collect(Collectors.toMap(Jogador::id, Function.identity()));
		return new Escalacao(formacao, jogadores(titulares, porId), jogadores(reservas, porId),
				batedor == null ? null : porId.get(batedor));
	}

	private Set<Long> todos() {
		Set<Long> todos = new LinkedHashSet<>(titulares);
		todos.addAll(reservas);
		return todos;
	}

	private static List<Jogador> jogadores(Set<Long> ids, Map<Long, Jogador> porId) {
		return ids.stream().map(porId::get).toList();
	}

	private static Set<Long> ids(List<Jogador> jogadores) {
		return jogadores.stream().map(Jogador::id).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<Long> menos(Set<Long> ids, Set<Long> retirar) {
		return ids.stream().filter(id -> !retirar.contains(id)).collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
