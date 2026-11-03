package br.com.managerfoot.web;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Login por e-mail e senha com sessão HTTP e CSRF (SPEC §3.4; docs/decisions/0009). Só a entrada, o
 * cadastro pelo link, o link do convite para jogo em rede, os arquivos estáticos e o health check ficam abertos.
 */
@Configuration
class SegurancaConfig {

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean

	@ConditionalOnWebApplication
	SecurityFilterChain seguranca(HttpSecurity http, AutenticacaoContas autenticacao,
			SecurityContextRepository contexto) throws Exception {
		http.authorizeHttpRequests(a -> a
				.requestMatchers("/entrar", "/cadastro", "/css/**", "/js/**", "/audio/**", "/webjars/**", "/actuator/health", "/error").permitAll()
				// o link do convite mostra o formulário de entrar a quem ainda não entrou (modelo 18d, caso 2)
				.requestMatchers(HttpMethod.GET, "/convite/*").permitAll()
				.requestMatchers("/jogos/escolher", "/jogos/escolher/**").hasRole("ADMINISTRADOR")
				.anyRequest().authenticated())
			.authenticationProvider(autenticacao)
			.securityContext(s -> s.securityContextRepository(contexto))
			.formLogin(f -> f.loginPage("/entrar")
				.usernameParameter("email")
				.passwordParameter("senha")
				.successHandler((request, response, entrou) -> response.sendRedirect(request.getContextPath()
						+ conviteDoFormulario(request).map(t -> "/convite/" + t).orElse("/")))
				.failureHandler((request, response, erro) -> response.sendRedirect(request.getContextPath()
						+ conviteDoFormulario(request).map(t -> "/convite/" + t + "?erro").orElse("/entrar?erro"))))
			.logout(l -> l.logoutUrl("/sair").logoutSuccessUrl("/entrar?saiu"))
			// o token do convite vai na URL: não pode vazar para outros sites (fontes) pelo Referer
			.headers(h -> h.referrerPolicy(r -> r.policy(ReferrerPolicy.SAME_ORIGIN)));
		return http.build();
	}

	/** Formato do token do convite (Base64 para URL): o que vier fora dele não vira endereço de volta. */
	private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{20,100}");

	/** O convite de onde veio o formulário de entrar, para voltar a ele. */
	static Optional<String> conviteDoFormulario(HttpServletRequest request) {
		return Optional.ofNullable(request.getParameter("convite")).filter(t -> TOKEN.matcher(t).matches());
	}
}
