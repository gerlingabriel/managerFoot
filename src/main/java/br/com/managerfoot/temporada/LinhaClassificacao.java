package br.com.managerfoot.temporada;

import br.com.managerfoot.dominio.Clube;

/** Desempenho acumulado de um clube na tabela. */
public record LinhaClassificacao(Clube clube, int jogos, int vitorias, int empates, int derrotas, int golsPro,
		int golsContra) {

	public int pontos() {
		return vitorias * Classificacao.PONTOS_VITORIA + empates * Classificacao.PONTOS_EMPATE;
	}

	public int saldo() {
		return golsPro - golsContra;
	}

	LinhaClassificacao comResultado(int marcados, int sofridos) {
		return new LinhaClassificacao(clube, jogos + 1, vitorias + (marcados > sofridos ? 1 : 0),
				empates + (marcados == sofridos ? 1 : 0), derrotas + (marcados < sofridos ? 1 : 0), golsPro + marcados,
				golsContra + sofridos);
	}
}
