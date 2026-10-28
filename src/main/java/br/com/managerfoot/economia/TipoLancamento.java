package br.com.managerfoot.economia;

/** Tipos de lançamento do extrato: caixa da rodada (rev. 37), patrocínio e prêmio da liga (rev. 48). */
public enum TipoLancamento {
	BILHETERIA,
	CUSTO_DO_JOGO,
	DESLOCAMENTO,
	SALARIOS,
	/** Patrocínio da temporada, na rodada 0 (o início). */
	PATROCINIO,
	/** Prêmio da liga, na última rodada da temporada. */
	PREMIO
}
