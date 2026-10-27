package br.com.managerfoot.contas;

import java.util.Objects;

/** Usuário sem a senha: o hash fica só na persistência. */
public record Usuario(long id, String email, String nome, Papel papel) {

	public Usuario {
		Objects.requireNonNull(email, "e-mail é obrigatório");
		Objects.requireNonNull(nome, "nome é obrigatório");
		Objects.requireNonNull(papel, "papel é obrigatório");
	}
}
