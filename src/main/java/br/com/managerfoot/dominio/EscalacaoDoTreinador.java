package br.com.managerfoot.dominio;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.function.LongToIntFunction;
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
	 * incompleta. O batedor continua escolhido mesmo fora do banco (rev. 99).
	 */
	public EscalacaoDoTreinador com(long jogadorId, Lugar lugar) {
		Set<Long> novosTitulares = menos(titulares, Set.of(jogadorId));
		Set<Long> novasReservas = menos(reservas, Set.of(jogadorId));
		switch (lugar) {
			case TITULAR -> novosTitulares.add(jogadorId);
			case BANCO -> novasReservas.add(jogadorId);
			case FORA -> { }
		}
		return new EscalacaoDoTreinador(formacao, novosTitulares, novasReservas, batedor);
	}

	public EscalacaoDoTreinador com(Formacao novaFormacao) {
		return new EscalacaoDoTreinador(novaFormacao, titulares, reservas, batedor);
	}

	/** @param novoBatedor nulo é o automático */
	public EscalacaoDoTreinador comBatedor(Long novoBatedor) {
		return new EscalacaoDoTreinador(formacao, titulares, reservas, novoBatedor);
	}

	/** A mesma escalação sem esses jogadores, que passam para fora do banco; o batedor continua escolhido. */
	public EscalacaoDoTreinador sem(Set<Long> jogadores) {
		return new EscalacaoDoTreinador(formacao, menos(titulares, jogadores), menos(reservas, jogadores), batedor);
	}

	/** Todos os jogadores fora do banco; a formação e o batedor ficam (SPEC §10.3, rev. 86 e 99). */
	public EscalacaoDoTreinador vazia() {
		return new EscalacaoDoTreinador(formacao, Set.of(), Set.of(), batedor);
	}

	/**
	 * Ao clicar na tática (SPEC §10.3, rev. 86 e 94): se falta titular em algum setor, completa com os
	 * melhores de cada posição, do banco ou de fora; quem já está escalado fica. O banco é refeito com
	 * um goleiro só e pelo menos um defensor, um meio-campista e um atacante, se houver; as outras vagas
	 * vão para os melhores jogadores de linha. Sem titular faltando, nada muda.
	 *
	 * <p>Ordem dos melhores: força, depois gols na temporada, depois nome. Os escolhidos aqui respeitam o
	 * limite de asteriscos sem penalidade, como na máquina, se houver outro jogador na posição.
	 *
	 * @param indisponivel suspenso ou lesionado
	 * @param limiteAsteriscos asteriscos em campo sem penalidade (parâmetro do motor)
	 * @param gols gols do jogador na temporada, para o desempate
	 */
	public EscalacaoDoTreinador completada(List<Jogador> plantel, LongPredicate indisponivel, int limiteAsteriscos,
			LongToIntFunction gols) {
		return completada(plantel, indisponivel, limiteAsteriscos, gols, Optional.empty());
	}

	/**
	 * Como {@link #completada(List, LongPredicate, int, LongToIntFunction)}; os escolhidos aqui também respeitam
	 * o limite de estrangeiros em campo (SPEC §8.4, rev. 130), se houver outro jogador na posição.
	 *
	 * @param estrangeiros vazio sem limite (continental e Mundial)
	 */
	public EscalacaoDoTreinador completada(List<Jogador> plantel, LongPredicate indisponivel, int limiteAsteriscos,
			LongToIntFunction gols, Optional<LimiteEstrangeiros> estrangeiros) {
		Collator alfabetica = Collator.getInstance(Locale.of("pt", "BR"));
		List<Jogador> disponiveis = plantel.stream().filter(j -> !indisponivel.test(j.id()))
			.sorted(Comparator.comparingInt(Jogador::forca).reversed()
				.thenComparing(Comparator.comparingInt((Jogador j) -> gols.applyAsInt(j.id())).reversed())
				.thenComparing(Jogador::nome, alfabetica)
				.thenComparingLong(Jogador::id))
			.toList();

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
		estrangeiros.ifPresent(l -> limitarEstrangeiros(disponiveis, novosTitulares, novos, l, limiteAsteriscos));

		// um de cada posição, com um goleiro só; o resto do banco é dos melhores de linha
		List<Jogador> restantes = disponiveis.stream().filter(j -> !novosTitulares.contains(j.id())).toList();
		Set<Long> novoBanco = new LinkedHashSet<>();
		for (Posicao posicao : Posicao.values()) {
			restantes.stream().filter(j -> j.posicao() == posicao).findFirst().map(Jogador::id).ifPresent(novoBanco::add);
		}
		restantes.stream().filter(j -> j.posicao() != Posicao.GOLEIRO).map(Jogador::id)
			.filter(id -> !novoBanco.contains(id)).limit(Escalacao.MAXIMO_RESERVAS - novoBanco.size())
			.forEach(novoBanco::add);
		return new EscalacaoDoTreinador(formacao, novosTitulares, novoBanco, batedor);
	}

	/**
	 * Troca os estrangeiros escolhidos automaticamente além do limite pelo melhor da mesma posição que não conta,
	 * perdendo o mínimo de força, sem passar o limite de asteriscos.
	 */
	private static void limitarEstrangeiros(List<Jogador> disponiveis, Set<Long> escolhidos, Set<Long> novos,
			LimiteEstrangeiros limite, int limiteAsteriscos) {
		while (limite.excedido(disponiveis.stream().filter(j -> escolhidos.contains(j.id())).toList())) {
			long asteriscos = disponiveis.stream().filter(j -> j.asterisco() && escolhidos.contains(j.id())).count();
			Jogador sai = null;
			Jogador entra = null;
			for (Jogador candidato : disponiveis) {
				if (!limite.conta(candidato) || !novos.contains(candidato.id())) {
					continue;
				}
				boolean cabeAsterisco = candidato.asterisco() || asteriscos < limiteAsteriscos;
				Optional<Jogador> substituto = disponiveis.stream()
					.filter(j -> j.posicao() == candidato.posicao() && !limite.conta(j) && !escolhidos.contains(j.id())
							&& (!j.asterisco() || cabeAsterisco))
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
		return problemas(plantel, suspenso, lesionado, Optional.empty());
	}

	/**
	 * Como {@link #problemas(List, LongPredicate, LongPredicate)}, com o limite de estrangeiros em campo da liga e
	 * da copa nacional (SPEC §8.4, rev. 130).
	 *
	 * @param estrangeiros vazio sem limite (continental e Mundial)
	 */
	public List<String> problemas(List<Jogador> plantel, LongPredicate suspenso, LongPredicate lesionado,
			Optional<LimiteEstrangeiros> estrangeiros) {
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
		estrangeiros.ifPresent(l -> {
			int emCampo = l.contar(titulares.stream().map(porId::get).filter(Objects::nonNull).toList());
			if (emCampo > l.limite()) {
				problemas.add(l.problema(emCampo));
			}
		});
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
		return escalacao(plantel, suspenso, lesionado, Optional.empty(), fatorGoleiroImprovisado);
	}

	/** Como {@link #escalacao(List, LongPredicate, LongPredicate, double)}, com o limite de estrangeiros. */
	public Escalacao escalacao(List<Jogador> plantel, LongPredicate suspenso, LongPredicate lesionado,
			Optional<LimiteEstrangeiros> estrangeiros, double fatorGoleiroImprovisado) {
		List<String> problemas = problemas(plantel, suspenso, lesionado, estrangeiros);
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
