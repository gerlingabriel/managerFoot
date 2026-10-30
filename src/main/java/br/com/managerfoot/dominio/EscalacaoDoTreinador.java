package br.com.managerfoot.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
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

	/** Todos os jogadores fora do banco e o batedor no automático; a formação fica (SPEC §10.3, rev. 86). */
	public EscalacaoDoTreinador vazia() {
		return new EscalacaoDoTreinador(formacao, Set.of(), Set.of(), null);
	}

	/**
	 * Ao clicar na tática (SPEC §10.3, rev. 86): se falta titular em algum setor, completa com os mais
	 * fortes de cada posição, do banco ou de fora; quem já está escalado fica. O que sobra na tática
	 * nova sai, do mais fraco para o mais forte, para o banco. As vagas livres do banco vão para os
	 * mais fortes que restaram, com um goleiro primeiro. Sem titular faltando, nada muda.
	 *
	 * <p>Empate de força: asterisco primeiro, depois sorte. Os escolhidos aqui respeitam o limite de
	 * asteriscos sem penalidade, como na máquina, se houver outro jogador na posição.
	 *
	 * @param indisponivel suspenso ou lesionado
	 * @param limiteAsteriscos asteriscos em campo sem penalidade (parâmetro do motor)
	 * @param sorte desempate; recebida de fora para o resultado poder ser reproduzido
	 */
	public EscalacaoDoTreinador completada(List<Jogador> plantel, LongPredicate indisponivel, int limiteAsteriscos,
			Random sorte) {
		List<Jogador> disponiveis = plantel.stream().filter(j -> !indisponivel.test(j.id()))
			.collect(Collectors.toCollection(ArrayList::new));
		Collections.shuffle(disponiveis, sorte);
		// a ordenação é estável: quem empata em força e asterisco fica na ordem sorteada
		disponiveis.sort(Comparator.comparingInt(Jogador::forca).reversed()
			.thenComparing(Jogador::asterisco, Comparator.reverseOrder()));

		// sem goleiro disponível, um jogador de linha a mais vai para o gol (§9.3)
		boolean semGoleiro = disponiveis.stream().noneMatch(j -> j.posicao() == Posicao.GOLEIRO);
		Map<Posicao, Integer> vagas = new EnumMap<>(Posicao.class);
		for (Posicao posicao : Posicao.values()) {
			vagas.put(posicao, formacao.quantidade(posicao));
		}
		if (semGoleiro) {
			vagas.merge(disponiveis.stream().anyMatch(j -> j.posicao() == Posicao.DEFENSOR) ? Posicao.DEFENSOR
					: Posicao.ATACANTE, 1, Integer::sum);
		}

		Set<Long> novosTitulares = new LinkedHashSet<>();
		Set<Long> novos = new LinkedHashSet<>();
		boolean faltava = false;
		for (Posicao posicao : Posicao.values()) {
			List<Jogador> daPosicao = disponiveis.stream().filter(j -> j.posicao() == posicao).toList();
			List<Jogador> escalados = daPosicao.stream().filter(j -> titulares.contains(j.id())).limit(vagas.get(posicao))
				.toList();
			escalados.forEach(j -> novosTitulares.add(j.id()));
			int falta = vagas.get(posicao) - escalados.size();
			List<Jogador> entram = daPosicao.stream().filter(j -> !titulares.contains(j.id())).limit(falta).toList();
			entram.forEach(j -> {
				novosTitulares.add(j.id());
				novos.add(j.id());
			});
			faltava |= !entram.isEmpty();
		}
		if (!faltava) {
			return this;
		}
		limitarAsteriscos(disponiveis, novosTitulares, novos, limiteAsteriscos);

		// quem saiu dos titulares vai para o banco; do que ainda couber, goleiro primeiro e os mais fortes
		Set<Long> novoBanco = new LinkedHashSet<>();
		disponiveis.stream().filter(j -> titulares.contains(j.id()) && !novosTitulares.contains(j.id()))
			.map(Jogador::id).forEach(novoBanco::add);
		disponiveis.stream().filter(j -> reservas.contains(j.id()) && !novosTitulares.contains(j.id()))
			.map(Jogador::id).forEach(novoBanco::add);
		List<Jogador> restantes = disponiveis.stream()
			.filter(j -> !novosTitulares.contains(j.id()) && !novoBanco.contains(j.id())).toList();
		boolean temGoleiroNoBanco = disponiveis.stream()
			.anyMatch(j -> j.posicao() == Posicao.GOLEIRO && novoBanco.contains(j.id()));
		if (!temGoleiroNoBanco) {
			restantes.stream().filter(j -> j.posicao() == Posicao.GOLEIRO).findFirst().map(Jogador::id)
				.ifPresent(novoBanco::add);
		}
		restantes.stream().map(Jogador::id).filter(id -> !novoBanco.contains(id)).forEach(novoBanco::add);
		Set<Long> bancoFinal = novoBanco.stream().limit(Escalacao.MAXIMO_RESERVAS)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		Long novoBatedor = batedor != null && (novosTitulares.contains(batedor) || bancoFinal.contains(batedor)) ? batedor
				: null;
		return new EscalacaoDoTreinador(formacao, novosTitulares, bancoFinal, novoBatedor);
	}

	/** Troca os asteriscos escolhidos automaticamente além do limite, perdendo o mínimo de força. */
	private static void limitarAsteriscos(List<Jogador> disponiveis, Set<Long> escolhidos, Set<Long> novos, int limite) {
		while (disponiveis.stream().filter(j -> j.asterisco() && escolhidos.contains(j.id())).count() > limite) {
			Jogador sai = null;
			Jogador entra = null;
			for (Jogador candidato : disponiveis) {
				if (!candidato.asterisco() || !novos.contains(candidato.id())) {
					continue;
				}
				Optional<Jogador> substituto = disponiveis.stream()
					.filter(j -> j.posicao() == candidato.posicao() && !j.asterisco() && !escolhidos.contains(j.id()))
					.findFirst();
				if (substituto.isPresent()
						&& (sai == null || candidato.forca() - substituto.get().forca() < sai.forca() - entra.forca())) {
					sai = candidato;
					entra = substituto.get();
				}
			}
			if (sai == null) {
				return;
			}
			escolhidos.remove(sai.id());
			novos.remove(sai.id());
			escolhidos.add(entra.id());
			novos.add(entra.id());
		}
	}

	/**
	 * O que impede esta escalação de jogar, em mensagens para a tela; vazia se pode jogar.
	 *
	 * @param plantel jogadores do clube
	 * @param suspenso diz se o jogador está suspenso para a próxima partida
	 */
	public List<String> problemas(List<Jogador> plantel, LongPredicate suspenso) {
		return problemas(plantel, suspenso, id -> false);
	}

	/**
	 * Como {@link #problemas(List, LongPredicate)}, com os lesionados (SPEC §7.5), que também não
	 * podem jogar.
	 */
	public List<String> problemas(List<Jogador> plantel, LongPredicate suspenso, LongPredicate lesionado) {
		LongPredicate indisponivel = suspenso.or(lesionado);
		Map<Long, Jogador> porId = plantel.stream().collect(Collectors.toMap(Jogador::id, Function.identity()));
		List<String> problemas = new ArrayList<>();
		for (long id : todos()) {
			Jogador jogador = porId.get(id);
			if (jogador == null) {
				problemas.add("o jogador " + id + " não é do clube");
			} else if (suspenso.test(id)) {
				problemas.add(jogador.nome() + " está suspenso");
			} else if (lesionado.test(id)) {
				problemas.add(jogador.nome() + " está lesionado");
			}
		}
		if (batedor != null && !titulares.contains(batedor) && !reservas.contains(batedor)) {
			problemas.add("o batedor de pênalti precisa estar entre os titulares ou no banco");
		}
		if (problemas.isEmpty()) {
			Optional<Jogador> improvisado = goleiroImprovisado(plantel, indisponivel);
			List<Jogador> doTime = emCampo(porId, improvisado, 1);
			List<Jogador> banco = jogadores(reservas, porId);
			problemas.addAll(ninguemCabeNasVagas(plantel, indisponivel, doTime)
					? Escalacao.problemasPermitindoBuracos(formacao, doTime, banco)
					: Escalacao.problemas(formacao, doTime, banco));
			if (!problemas.isEmpty() && improvisado.isPresent()) {
				problemas.add("sem goleiro disponível, " + improvisado.get().nome() + " vai para o gol");
			}
		}
		return problemas;
	}

	/**
	 * Sem goleiro disponível no plantel (SPEC §9.3, rev. 68): o defensor mais fraco entre os
	 * titulares vai para o gol; sem defensores, o jogador de linha mais fraco. É a mesma regra da
	 * máquina ({@code EscalacaoMaquina}). Vazio se há goleiro que possa jogar.
	 *
	 * @param indisponivel suspenso ou lesionado
	 */
	public Optional<Jogador> goleiroImprovisado(List<Jogador> plantel, LongPredicate indisponivel) {
		if (plantel.stream().anyMatch(j -> j.posicao() == Posicao.GOLEIRO && !indisponivel.test(j.id()))) {
			return Optional.empty();
		}
		List<Jogador> doTime = plantel.stream().filter(j -> titulares.contains(j.id())).toList();
		Comparator<Jogador> maisFraco = Comparator.comparingInt(Jogador::forca).thenComparingLong(Jogador::id);
		return doTime.stream()
			.filter(j -> j.posicao() == Posicao.DEFENSOR)
			.min(maisFraco)
			.or(() -> doTime.stream().min(maisFraco));
	}

	/**
	 * A escalação para a partida. O goleiro improvisado, se houver, entra como goleiro com a força
	 * reduzida, como o da máquina.
	 *
	 * @param fatorGoleiroImprovisado fração da força do jogador de linha no gol (parâmetro do motor)
	 * @throws IllegalStateException com os {@link #problemas}, se ela não pode jogar
	 */
	public Escalacao escalacao(List<Jogador> plantel, LongPredicate suspenso, double fatorGoleiroImprovisado) {
		return escalacao(plantel, suspenso, id -> false, fatorGoleiroImprovisado);
	}

	/** Como {@link #escalacao(List, LongPredicate, double)}, com os lesionados. */
	public Escalacao escalacao(List<Jogador> plantel, LongPredicate suspenso, LongPredicate lesionado,
			double fatorGoleiroImprovisado) {
		List<String> problemas = problemas(plantel, suspenso, lesionado);
		if (!problemas.isEmpty()) {
			throw new IllegalStateException("escalação inválida: " + String.join("; ", problemas));
		}
		Map<Long, Jogador> porId = plantel.stream().collect(Collectors.toMap(Jogador::id, Function.identity()));
		List<Jogador> doTime = emCampo(porId, goleiroImprovisado(plantel, suspenso.or(lesionado)),
				fatorGoleiroImprovisado);
		Jogador oBatedor = batedor == null ? null
				: doTime.stream().filter(j -> j.id() == batedor).findFirst().orElse(porId.get(batedor));
		return new Escalacao(formacao, doTime, jogadores(reservas, porId), oBatedor);
	}

	/** Os titulares, com o goleiro improvisado atuando no gol. */
	private List<Jogador> emCampo(Map<Long, Jogador> porId, Optional<Jogador> improvisado, double fator) {
		return titulares.stream().map(porId::get).map(j -> improvisado.filter(i -> i.id() == j.id())
			.map(i -> new Jogador(i.id(), i.nome(), Posicao.GOLEIRO,
					Math.max(Jogador.FORCA_MINIMA, (int) Math.round(i.forca() * fator)), i.temperamento(), i.asterisco(),
					i.nacionalidade()))
			.orElse(j)).toList();
	}

	/**
	 * Time com um a menos (SPEC §9.3): só pode jogar incompleto se nenhum jogador disponível fora
	 * dos titulares é da posição de uma vaga vazia.
	 */
	private boolean ninguemCabeNasVagas(List<Jogador> plantel, LongPredicate indisponivel, List<Jogador> doTime) {
		Map<Posicao, Long> emCampo = doTime.stream()
			.collect(Collectors.groupingBy(Jogador::posicao, Collectors.counting()));
		return plantel.stream()
			.filter(j -> !titulares.contains(j.id()) && !indisponivel.test(j.id()))
			.noneMatch(j -> emCampo.getOrDefault(j.posicao(), 0L) < formacao.quantidade(j.posicao()));
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
