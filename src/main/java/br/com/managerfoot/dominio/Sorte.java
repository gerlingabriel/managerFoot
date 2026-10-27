package br.com.managerfoot.dominio;

/** Sorte do clube na temporada, sorteada e oculta (SPEC §5.3.1). */
public enum Sorte {
	/** Sem efeito. */
	NEUTRA,
	/** Mais chances de gol por partida. */
	BENEFICIADO,
	/** Menos domínio (posse de bola). */
	PREJUDICADO
}
