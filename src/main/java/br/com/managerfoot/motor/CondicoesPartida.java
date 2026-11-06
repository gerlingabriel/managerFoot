package br.com.managerfoot.motor;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
 * @param decisivo se a partida precisa de vencedor, com prorrogação e pênaltis (SPEC §8.2)
 * @param copa partida da copa nacional, com a zebra do mais fraco (SPEC §8.2, rev. 117); vazia nas demais
 */
public record CondicoesPartida(Map<Lado, Sorte> sorte, Set<Long> bonusExtra, Map<Lado, Integer> lesoesPermitidas,
		Optional<JogoDecisivo> decisivo, Optional<CopaNacional> copa) {

	public static final CondicoesPartida NEUTRAS = new CondicoesPartida(Map.of(), Set.of());

	public CondicoesPartida {
		sorte = Map.copyOf(Objects.requireNonNull(sorte));
		bonusExtra = Set.copyOf(Objects.requireNonNull(bonusExtra));
		lesoesPermitidas = Map.copyOf(Objects.requireNonNull(lesoesPermitidas));
		Objects.requireNonNull(decisivo);
		Objects.requireNonNull(copa);
	}

	public CondicoesPartida(Map<Lado, Sorte> sorte, Set<Long> bonusExtra, Map<Lado, Integer> lesoesPermitidas,
			Optional<JogoDecisivo> decisivo) {
		this(sorte, bonusExtra, lesoesPermitidas, decisivo, Optional.empty());
	}

	public CondicoesPartida(Map<Lado, Sorte> sorte, Set<Long> bonusExtra, Map<Lado, Integer> lesoesPermitidas) {
		this(sorte, bonusExtra, lesoesPermitidas, Optional.empty());
	}

	public CondicoesPartida(Map<Lado, Sorte> sorte, Set<Long> bonusExtra) {
		this(sorte, bonusExtra, Map.of());
	}

	public static CondicoesPartida comSorte(Map<Lado, Sorte> sorte) {
		return new CondicoesPartida(sorte, Set.of());
	}

	/** As mesmas condições numa partida que precisa de vencedor. */
	public CondicoesPartida comDecisivo(JogoDecisivo jogo) {
		return new CondicoesPartida(sorte, bonusExtra, lesoesPermitidas, Optional.of(jogo), copa);
	}

	/** As mesmas condições numa partida da copa nacional. */
	public CondicoesPartida comCopa(CopaNacional copaNacional) {
		return new CondicoesPartida(sorte, bonusExtra, lesoesPermitidas, decisivo, Optional.of(copaNacional));
	}

	int lesoesPermitidas(Lado lado, int limiteDaTemporada) {
		return lesoesPermitidas.getOrDefault(lado, limiteDaTemporada);
	}

	public Sorte sorteDe(Lado lado) {
		return sorte.getOrDefault(lado, Sorte.NEUTRA);
	}
}
