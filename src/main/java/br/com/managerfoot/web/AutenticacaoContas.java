package br.com.managerfoot.web;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.ContasRepositorio;

/** Liga o login do Spring Security à verificação de senha das contas (docs/decisions/0007). */
@Component
class AutenticacaoContas implements AuthenticationProvider {

	private final ContasRepositorio contas;

	AutenticacaoContas(ContasRepositorio contas) {
		this.contas = contas;
	}

	@Override
	public Authentication authenticate(Authentication pedido) {
		String senha = pedido.getCredentials() == null ? null : pedido.getCredentials().toString();
		Usuario usuario = contas.autenticar(pedido.getName(), senha)
			.orElseThrow(() -> new BadCredentialsException("e-mail ou senha incorretos"));
		return autenticado(usuario);
	}

	@Override
	public boolean supports(Class<?> tipo) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
	}

	/** O usuário como principal, com o papel como {@code ROLE_<PAPEL>}. Sem a senha. */
	static Authentication autenticado(Usuario usuario) {
		return UsernamePasswordAuthenticationToken.authenticated(usuario, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + usuario.papel().name())));
	}
}
