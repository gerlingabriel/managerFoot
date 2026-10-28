package br.com.managerfoot.economia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.economia.Leilao.Lance;

/**
 * Leilões da máquina antes de uma rodada (SPEC §9.4, rev. 41 a 45; modelo 7b): cada clube da
 * máquina abaixo do limite põe um jogador em leilão; os treinadores humanos dão seus lances e depois
 * a máquina dá os dela. A mesma regra da simulação em {@code docs/simulacoes/mercado-maquina.md}.
 * Domínio puro; dinheiro em centavos de Real.
 */
public final class LeiloesDaRodada {

	private final MercadoDaMaquina mercado;
	private final PrecoJogador preco;

	public LeiloesDaRodada(MercadoDaMaquina mercado, PrecoJogador preco) {
		this.mercado = mercado;
		this.preco = preco;
	}

	public static LeiloesDaRodada padrao() {
		return new LeiloesDaRodada(MercadoDaMaquina.padrao(), PrecoJogador.padrao());
	}

	/**
	 * Os leilões que abrem: 1 por clube da máquina abaixo do limite, na ordem dos clubes. Com saldo
	 * negativo, o leilão é sem lance mínimo.
	 *
	 * @param daMaquina clubes da máquina que estão nas divisões
	 */
	public List<Oferta> abrir(List<ClubeNoMercado> daMaquina, String paisDaLiga) {
		List<Oferta> ofertas = new ArrayList<>();
		for (ClubeNoMercado c : ordenados(daMaquina)) {
			if (!mercado.precisaVender(c.saldo(), c.plantel())) {
				continue;
			}
			mercado.jogadorParaVender(c.plantel(), paisDaLiga).ifPresent(j -> ofertas.add(new Oferta(j, c.clube().id(),
					c.saldo() < 0 ? 0 : preco.lanceInicialLeilao(j, paisDaLiga))));
		}
		return List.copyOf(ofertas);
	}

	/**
	 * Leilão do clube de um treinador com caixa negativo (SPEC §9.3 e §9.4, rev. 43 e 53): sem lance
	 * mínimo, de um jogador sorteado entre os sem contrato renovado, para que qualquer um deles possa
	 * sair. Proposta do agente: a saída não deixa a posição abaixo do elenco mínimo. Vazio com caixa
	 * zero ou positivo, ou sem candidato (todos renovados).
	 *
	 * @param renovados ids dos jogadores com contrato renovado nesta temporada
	 */
	public Optional<Oferta> abrirPorCaixaNegativo(ClubeNoMercado clube, Set<Long> renovados, RandomGenerator aleatorio) {
		if (clube.saldo() >= 0) {
			return Optional.empty();
		}
		// ordem fixa antes do sorteio, para a mesma semente dar o mesmo jogador
		List<Jogador> candidatos = clube.plantel().stream()
			.filter(j -> !renovados.contains(j.id()) && mercado.podeSair(clube.plantel(), j))
			.sorted(Comparator.comparingLong(Jogador::id))
			.toList();
		if (candidatos.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new Oferta(candidatos.get(aleatorio.nextInt(candidatos.size())), clube.clube().id(), 0));
	}

	/**
	 * Resolve os leilões na ordem dada. O lance de um humano só vale se for pelo menos o mínimo, maior
	 * que zero, e se o caixa e o plantel ainda comportarem a compra depois dos leilões que ele já
	 * levou. Cada clube da máquina compra no máximo 1 por rodada e não dá lance no próprio jogador.
	 *
	 * @param daMaquina clubes da máquina que estão nas divisões
	 * @param humanos clubes dos treinadores humanos
	 * @param lancesHumanos por id do jogador, os lances dos humanos
	 * @param aleatorio sorteio dos empates
	 */
	public List<Desfecho> resolver(List<Oferta> ofertas, List<ClubeNoMercado> daMaquina, List<ClubeNoMercado> humanos,
			Map<Long, List<Lance>> lancesHumanos, String paisDaLiga, RandomGenerator aleatorio) {
		return resolver(ofertas, daMaquina, humanos, lancesHumanos, Set.of(), paisDaLiga, aleatorio);
	}

	/**
	 * Como {@link #resolver(List, List, List, Map, String, RandomGenerator)}, sem lance dos clubes da
	 * máquina que já compraram antes da mesma rodada.
	 *
	 * @param jaCompraram ids dos clubes da máquina que já levaram um jogador antes desta rodada
	 */
	public List<Desfecho> resolver(List<Oferta> ofertas, List<ClubeNoMercado> daMaquina, List<ClubeNoMercado> humanos,
			Map<Long, List<Lance>> lancesHumanos, Set<Long> jaCompraram, String paisDaLiga, RandomGenerator aleatorio) {
		Map<Long, Estado> estados = new HashMap<>();
		for (ClubeNoMercado c : daMaquina) {
			estados.put(c.clube().id(), new Estado(c.clube(), c.saldo(), new ArrayList<>(c.plantel())));
		}
		for (ClubeNoMercado c : humanos) {
			estados.put(c.clube().id(), new Estado(c.clube(), c.saldo(), new ArrayList<>(c.plantel())));
		}
		Set<Long> idsHumanos = new HashSet<>();
		humanos.forEach(c -> idsHumanos.add(c.clube().id()));
		List<Long> compradoresDaMaquina = ordenados(daMaquina).stream().map(c -> c.clube().id()).toList();
		Set<Long> compraram = new HashSet<>(jaCompraram);
		List<Desfecho> desfechos = new ArrayList<>();
		for (Oferta oferta : ofertas) {
			Jogador jogador = oferta.jogador();
			List<Lance> lances = new ArrayList<>();
			for (Lance l : lancesHumanos.getOrDefault(jogador.id(), List.of())) {
				Estado e = estados.get(l.clubeId());
				if (idsHumanos.contains(l.clubeId()) && l.clubeId() != oferta.vendedorId() && l.valor() > 0
						&& l.valor() >= oferta.lanceMinimo() && l.valor() <= e.saldo
						&& e.plantel.size() < Clube.MAXIMO_JOGADORES) {
					lances.add(l);
				}
			}
			for (long comprador : compradoresDaMaquina) {
				if (comprador == oferta.vendedorId() || compraram.contains(comprador)) {
					continue;
				}
				Estado e = estados.get(comprador);
				mercado.lance(e.clube, e.saldo, e.plantel, jogador, paisDaLiga, oferta.lanceMinimo())
					.ifPresent(v -> lances.add(new Lance(comprador, v)));
			}
			Optional<Lance> vencedor = Leilao.vencedor(lances, oferta.lanceMinimo(), aleatorio);
			boolean empate = vencedor.isPresent()
					&& lances.stream().filter(l -> l.valor() == vencedor.get().valor()).count() > 1;
			vencedor.ifPresent(v -> {
				Estado vendedor = estados.get(oferta.vendedorId());
				Estado comprador = estados.get(v.clubeId());
				vendedor.plantel.removeIf(j -> j.id() == jogador.id());
				vendedor.saldo += v.valor();
				comprador.plantel.add(jogador);
				comprador.saldo -= v.valor();
				if (!idsHumanos.contains(v.clubeId())) {
					compraram.add(v.clubeId());
				}
			});
			desfechos.add(new Desfecho(oferta, List.copyOf(lances), vencedor, empate));
		}
		return List.copyOf(desfechos);
	}

	private static List<ClubeNoMercado> ordenados(List<ClubeNoMercado> clubes) {
		return clubes.stream().sorted(Comparator.comparingLong(c -> c.clube().id())).toList();
	}

	/** Clube no mercado: caixa e plantel antes dos leilões. */
	public record ClubeNoMercado(Clube clube, long saldo, List<Jogador> plantel) {

		public ClubeNoMercado {
			plantel = List.copyOf(plantel);
		}
	}

	/**
	 * Jogador posto em leilão.
	 *
	 * @param lanceMinimo lance inicial (zero no leilão sem lance mínimo)
	 */
	public record Oferta(Jogador jogador, long vendedorId, long lanceMinimo) {
	}

	/**
	 * Como terminou um leilão.
	 *
	 * @param lances os lances que valeram, dos humanos e da máquina
	 * @param vencedor vazio se ninguém deu lance: o jogador fica no clube
	 * @param empate o maior lance foi dado por mais de um clube e o vencedor foi sorteado
	 */
	public record Desfecho(Oferta oferta, List<Lance> lances, Optional<Lance> vencedor, boolean empate) {
	}

	private static final class Estado {

		private final Clube clube;
		private long saldo;
		private final List<Jogador> plantel;

		private Estado(Clube clube, long saldo, List<Jogador> plantel) {
			this.clube = clube;
			this.saldo = saldo;
			this.plantel = plantel;
		}
	}
}
