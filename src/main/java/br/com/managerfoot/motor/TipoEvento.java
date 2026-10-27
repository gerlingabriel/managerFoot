package br.com.managerfoot.motor;

/** Eventos da partida: desfechos de finalização (referência Elifoot 98), cartões e substituições. */
public enum TipoEvento {
	GOL,
	DEFESA,
	TRAVE,
	FORA,
	CARTAO_AMARELO,
	/** Segundo amarelo na mesma partida: expulsão (SPEC §7.4; suspensão de 1 jogo). */
	SEGUNDO_AMARELO,
	/** Vermelho direto: expulsão (suspensão sorteada de 1 a 3 jogos). */
	CARTAO_VERMELHO,
	/** O jogador do evento entrou no lugar de {@link EventoPartida#substituido()}. */
	SUBSTITUICAO;

	public boolean expulsao() {
		return this == SEGUNDO_AMARELO || this == CARTAO_VERMELHO;
	}
}
