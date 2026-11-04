package br.com.managerfoot.economia;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;

/**
 * Filtros da aba "Comprar" (modelo 20b): posição, força de/até, preço de/até, asterisco, "só quem cabe
 * no meu caixa" e a ordem da lista. Limite vazio é sem limite.
 *
 * @param posicoes vazio é todas
 * @param precoDe em centavos de Real
 * @param precoAte em centavos de Real
 */
public record FiltroMercado(Set<Posicao> posicoes, Integer forcaDe, Integer forcaAte, Long precoDe, Long precoAte,
		Asterisco asterisco, Ordem ordem, boolean soQuemCabe) {

	public FiltroMercado {
		posicoes = posicoes.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(posicoes));
		Objects.requireNonNull(asterisco);
		Objects.requireNonNull(ordem);
	}

	/** Sem filtro, com "só quem cabe no meu caixa" marcado e os mais fortes primeiro. */
	public static FiltroMercado padrao() {
		return new FiltroMercado(Set.of(), null, null, null, null, Asterisco.TODOS, Ordem.FORCA, true);
	}

	public enum Asterisco {
		TODOS, COM, SEM
	}

	public enum Ordem {
		/** Força, maior primeiro. */
		FORCA,
		/** Preço, menor primeiro. */
		PRECO,
		/** Gols na temporada, mais primeiro. */
		GOLS
	}

	/** Um jogador que pode ser comprado. */
	public record Oferta(Jogador jogador, long preco, int gols) {
	}

	/**
	 * Se a oferta passa nos filtros.
	 *
	 * @param saldo caixa do comprador, em centavos
	 */
	public boolean aceita(Oferta oferta, long saldo) {
		Jogador j = oferta.jogador();
		return (posicoes.isEmpty() || posicoes.contains(j.posicao()))
				&& (forcaDe == null || j.forca() >= forcaDe)
				&& (forcaAte == null || j.forca() <= forcaAte)
				&& (precoDe == null || oferta.preco() >= precoDe)
				&& (precoAte == null || oferta.preco() <= precoAte)
				&& (asterisco == Asterisco.TODOS || j.asterisco() == (asterisco == Asterisco.COM))
				&& (!soQuemCabe || oferta.preco() <= saldo);
	}

	/** A ordem da lista; no empate, força, depois nome. */
	public Comparator<Oferta> comparador() {
		Comparator<Oferta> porForca = Comparator.comparingInt((Oferta o) -> o.jogador().forca()).reversed();
		Comparator<Oferta> primeiro = switch (ordem) {
			case FORCA -> porForca;
			case PRECO -> Comparator.comparingLong(Oferta::preco);
			case GOLS -> Comparator.comparingInt(Oferta::gols).reversed();
		};
		return primeiro.thenComparing(porForca).thenComparing(o -> o.jogador().nome()).thenComparingLong(o -> o.jogador().id());
	}
}
