package br.com.managerfoot.persistencia;

/** Competição de um lançamento do extrato (V31) ou de uma partida do calendário (V34). */
public enum Competicao {
	LIGA,
	COPA,
	/** Grupos e mata-mata da competição continental (SPEC §8.3.6). */
	CONTINENTAL,
	MUNDIAL
}
