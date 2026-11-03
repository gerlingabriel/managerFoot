package br.com.managerfoot.multiplayer;

/** Como cada treinador recebe o clube no jogo em rede (SPEC §3.2, rev. 28). */
public enum ModoRede {
	/** Cada um recebe, ao entrar na sala, um clube sorteado entre os mais fracos da última divisão. */
	SORTEIO,
	/** Cada um escolhe qualquer clube de divisão sem treinador usuário. */
	ESCOLHA
}
