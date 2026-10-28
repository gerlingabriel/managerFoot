package br.com.managerfoot.contas;

import java.io.Serializable;
import java.util.Objects;

/** Usuário sem a senha: o hash fica só na persistência. Serializável porque fica na sessão HTTP. */
public record Usuario(long id, String email, String nome, Papel papel) implements Serializable {

	public Usuario {
		Objects.requireNonNull(email, "e-mail é obrigatório");
		Objects.requireNonNull(nome, "nome é obrigatório");
		Objects.requireNonNull(papel, "papel é obrigatório");
	}
}
