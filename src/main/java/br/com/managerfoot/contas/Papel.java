package br.com.managerfoot.contas;

/** Papel do usuário (SPEC §3.1, §3.2, §3.4). */
public enum Papel {
	/** Criado por script; gera links de cadastro e cria jogos em rede. */
	ADMINISTRADOR,
	/** Cadastrado por link; joga nas configurações disponíveis. */
	PARTICIPANTE
}
