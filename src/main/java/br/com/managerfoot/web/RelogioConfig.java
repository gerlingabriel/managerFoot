package br.com.managerfoot.web;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** O instante "agora" da rodada ao vivo (docs/decisions/0010) vem daqui, para os testes o controlarem. */
@Configuration
class RelogioConfig {

	@Bean
	Clock relogio() {
		return Clock.systemUTC();
	}
}
