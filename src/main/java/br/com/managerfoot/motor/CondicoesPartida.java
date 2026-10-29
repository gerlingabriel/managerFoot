package br.com.managerfoot.motor;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import br.com.managerfoot.dominio.Sorte;

/**
 * Condições da temporada que afetam a partida, vindas do sorteio do início da temporada.
 *
 * @param sorte sorte de cada lado (SPEC §5.3.1); lado ausente é {@link Sorte#NEUTRA}
 * @param bonusExtra ids dos jogadores com asterisco sorteados no bônus de artilharia, que somam um
 *        segundo bônus de finalização (SPEC §5.2). Quem não tinha asterisco já chega à partida com o
 *        asterisco temporário no próprio {@link br.com.managerfoot.dominio.Jogador}.
 * @param lesoesPermitidas quantas lesões cada lado ainda pode ter na temporada (SPEC §7.5); lado
 *        ausente tem o limite da temporada inteiro
 */
public record CondicoesPartida(Map<Lado, Sorte> sorte, Set<Long> bonusExtra, Map<Lado, Integer> lesoesPermitidas) {

	public static final CondicoesPartida NEUTRAS = new CondicoesPartida(Map.of(), Set.of());

	public CondicoesPartida {
		sorte = Map.copyOf(Objects.requireNonNull(sorte));
		bonusExtra = Set.copyOf(Objects.requireNonNull(bonusExtra));
		lesoesPermitidas = Map.copyOf(Objects.requireNonNull(lesoesPermitidas));
	}

	public CondicoesPartida(Map<Lado, Sorte> sorte, Set<Long> bonusExtra) {
		this(sorte, bonusExtra, Map.of());
	}

	public static CondicoesPartida comSorte(Map<Lado, Sorte> sorte) {
		return new CondicoesPartida(sorte, Set.of());
	}

	int lesoesPermitidas(Lado lado, int limiteDaTemporada) {
		return lesoesPermitidas.getOrDefault(lado, limiteDaTemporada);
	}

	public Sorte sorteDe(Lado lado) {
		return sorte.getOrDefault(lado, Sorte.NEUTRA);
	}
}
