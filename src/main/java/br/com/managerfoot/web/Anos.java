package br.com.managerfoot.web;

import org.springframework.stereotype.Component;

import br.com.managerfoot.persistencia.JogoRepositorio;

/** Ano da temporada atual, no topo das telas de um jogo (modelo 14a, rev. 83). Usado como {@code @anos.doJogo(id)}. */
@Component("anos")
public class Anos {

	private final JogoRepositorio jogos;

	Anos(JogoRepositorio jogos) {
		this.jogos = jogos;
	}

	public Integer doJogo(Long jogoId) {
		return jogoId == null ? null : jogos.anoAtual(jogoId).orElse(null);
	}
}
