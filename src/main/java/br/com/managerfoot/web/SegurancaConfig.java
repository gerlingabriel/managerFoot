package br.com.managerfoot.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Login por e-mail e senha com sessão HTTP e CSRF (SPEC §3.4; docs/decisions/0009). Só a entrada, o
 * cadastro pelo link, os arquivos estáticos e o health check ficam abertos.
 */
@Configuration
class SegurancaConfig {

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	SecurityFilterChain seguranca(HttpSecurity http, AutenticacaoContas autenticacao,
			SecurityContextRepository contexto) throws Exception {
		http.authorizeHttpRequests(a -> a
				.requestMatchers("/entrar", "/cadastro", "/css/**", "/js/**", "/webjars/**", "/actuator/health", "/error").permitAll()
				.anyRequest().authenticated())
			.authenticationProvider(autenticacao)
			.securityContext(s -> s.securityContextRepository(contexto))
			.formLogin(f -> f.loginPage("/entrar")
				.usernameParameter("email")
				.passwordParameter("senha")
				.defaultSuccessUrl("/", true)
				.failureUrl("/entrar?erro"))
			.logout(l -> l.logoutUrl("/sair").logoutSuccessUrl("/entrar?saiu"))
			// o token do convite vai na URL: não pode vazar para outros sites (fontes) pelo Referer
			.headers(h -> h.referrerPolicy(r -> r.policy(ReferrerPolicy.SAME_ORIGIN)));
		return http.build();
	}
}
