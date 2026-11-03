package br.com.managerfoot.motor;

import br.com.managerfoot.dominio.Jogador;

/**
 * Algo que aconteceu na partida, atribuído a um time e a um jogador.
 *
 * @param penalti a finalização foi uma cobrança de pênalti
 * @param substituido em uma {@link TipoEvento#SUBSTITUICAO}, quem saiu; nos demais eventos, nulo
 * @param lesao em uma {@link TipoEvento#LESAO}, a gravidade sorteada; nos demais eventos, nula
 * @param goleiro no pênalti defendido, o goleiro adversário que defendeu; nos demais eventos, nulo
 */
public record EventoPartida(int minuto, Lado lado, TipoEvento tipo, Jogador jogador, boolean penalti,
		Jogador substituido, Lesao lesao, Jogador goleiro) {

	public EventoPartida(int minuto, Lado lado, TipoEvento tipo, Jogador jogador) {
		this(minuto, lado, tipo, jogador, false, null, null, null);
	}

	public EventoPartida(int minuto, Lado lado, TipoEvento tipo, Jogador jogador, boolean penalti) {
		this(minuto, lado, tipo, jogador, penalti, null, null, null);
	}

	public EventoPartida(int minuto, Lado lado, TipoEvento tipo, Jogador jogador, boolean penalti,
			Jogador substituido, Lesao lesao) {
		this(minuto, lado, tipo, jogador, penalti, substituido, lesao, null);
	}

	/** O goleiro de {@code lado.adversario()} defendeu o pênalti cobrado por {@code batedor}. */
	static EventoPartida penaltiDefendido(int minuto, Lado lado, Jogador batedor, Jogador goleiro) {
		return new EventoPartida(minuto, lado, TipoEvento.DEFESA, batedor, true, null, null, goleiro);
	}

	/** Pênalti que o goleiro adversário defendeu: aparece entre os lances da partida. */
	public boolean penaltiDefendido() {
		return penalti && tipo == TipoEvento.DEFESA;
	}

	static EventoPartida substituicao(int minuto, Lado lado, Jogador entrou, Jogador saiu) {
		return new EventoPartida(minuto, lado, TipoEvento.SUBSTITUICAO, entrou, false, saiu, null);
	}

	static EventoPartida lesao(int minuto, Lado lado, Jogador lesionado, Lesao lesao) {
		return new EventoPartida(minuto, lado, TipoEvento.LESAO, lesionado, false, null, lesao);
	}
}
