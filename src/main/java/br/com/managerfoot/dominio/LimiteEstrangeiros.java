package br.com.managerfoot.dominio;

import java.util.Collection;
import java.util.Objects;

/**
 * Limite de estrangeiros em campo numa partida da liga ou da copa nacional (SPEC §8.4, rev. 130). Conta só
 * quem está em campo, pela regra de quem é estrangeiro no país da liga ({@link RegraEstrangeiros}); o banco
 * pode ter mais. A continental e o Mundial não têm limite.
 *
 * @param paisDaLiga código FIFA do país jogado
 * @param limite quantos estrangeiros podem estar em campo ao mesmo tempo
 */
public record LimiteEstrangeiros(String paisDaLiga, int limite) {

	/** Valor padrão quando o banco não traz o limite do país (rev. 130). */
	public static final int PADRAO = 5;

	public LimiteEstrangeiros {
		Objects.requireNonNull(paisDaLiga, "país da liga é obrigatório");
		if (limite < 0 || limite > Formacao.TITULARES) {
			throw new IllegalArgumentException("o limite de estrangeiros vai de 0 a " + Formacao.TITULARES
					+ ", mas foi " + limite);
		}
	}

	/** O jogador conta no limite. */
	public boolean conta(Jogador jogador) {
		return RegraEstrangeiros.contaComoEstrangeiro(jogador.nacionalidade(), paisDaLiga);
	}

	/** Quantos dos jogadores contam no limite. */
	public int contar(Collection<Jogador> jogadores) {
		return (int) jogadores.stream().filter(this::conta).count();
	}

	/** Os jogadores em campo passam do limite. */
	public boolean excedido(Collection<Jogador> emCampo) {
		return contar(emCampo) > limite;
	}

	/** A troca deixa o time acima do limite: entra um estrangeiro no lugar de quem não conta, sem vaga. */
	public boolean trocaExcede(Collection<Jogador> emCampo, Jogador sai, Jogador entra) {
		return conta(entra) && !conta(sai) && contar(emCampo) >= limite;
	}

	/** "6 estrangeiros em campo: o limite é 5" */
	public String problema(int emCampo) {
		return emCampo + " estrangeiros em campo: o limite é " + limite;
	}

	/** O aviso da substituição recusada (modelo aprovado na rev. 130). */
	public String trocaRecusada() {
		return "Já há " + limite + (limite == 1 ? " estrangeiro" : " estrangeiros")
				+ " em campo, o limite. Para entrar um estrangeiro, tire outro estrangeiro.";
	}
}
