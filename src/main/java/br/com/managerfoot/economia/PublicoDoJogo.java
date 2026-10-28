package br.com.managerfoot.economia;

import java.util.random.RandomGenerator;

/**
 * Público de um jogo da liga (SPEC §9.2, rev. 56): a torcida do mandante segue a força dele; quanto
 * vai ao estádio depende da posição dele na tabela e da do adversário. O líder que recebe o 2º enche
 * o estádio do tamanho inicial; o meio da tabela fica perto da metade; o fim, bem abaixo, mesmo
 * recebendo o líder. Só um jogo decisivo no fim da temporada passa da torcida comum. Nunca passa da
 * capacidade. Domínio puro.
 */
public final class PublicoDoJogo {

	private final ParametrosPublico parametros;
	private final CaixaDaRodada caixa;

	public PublicoDoJogo(ParametrosPublico parametros, CaixaDaRodada caixa) {
		this.parametros = parametros;
		this.caixa = caixa;
	}

	public static PublicoDoJogo padrao() {
		return new PublicoDoJogo(ParametrosPublico.padrao(), CaixaDaRodada.padrao());
	}

	/** Torcida do clube: quantos iriam ao jogo mais importante, pela força em campo. */
	public int torcida(double forcaEmCampo) {
		return caixa.capacidadeInicial((int) Math.round(forcaEmCampo));
	}

	/** Estádio do início do jogo, em múltiplos de 500, nunca acima do máximo. */
	public int estadioInicial(int forcaClube) {
		int lugares = (int) Math.round(caixa.capacidadeInicial(forcaClube) * parametros.estadioInicialSobreTorcida() / 500) * 500;
		return Math.min(parametros.capacidadeMaxima(), Math.max(500, lugares));
	}

	/**
	 * @param posicaoMandante posição na tabela antes da rodada (1 = líder)
	 * @param posicaoVisitante idem
	 * @param clubes clubes da divisão
	 * @param rodada rodada que vai ser jogada
	 * @param rodadas total de rodadas da divisão
	 */
	public int publico(int capacidade, double forcaMandante, int posicaoMandante, int posicaoVisitante, int clubes, int rodada,
			int rodadas, RandomGenerator aleatorio) {
		double ocupacao = ocupacao(posicaoMandante, posicaoVisitante, clubes, rodada, rodadas)
				+ (aleatorio.nextDouble() * 2 - 1) * parametros.variacao();
		int publico = (int) Math.round(torcida(forcaMandante) * Math.max(0, ocupacao));
		return Math.clamp(publico, (int) Math.round(capacidade * parametros.ocupacaoMinima()), capacidade);
	}

	/** Fração da torcida esperada no jogo, antes do sorteio. */
	public double ocupacao(int posicaoMandante, int posicaoVisitante, int clubes, int rodada, int rodadas) {
		double pMandante = relativa(posicaoMandante, clubes);
		double pVisitante = relativa(posicaoVisitante, clubes);
		double pelaTabela = parametros.ocupacaoLider() - (parametros.ocupacaoLider() - parametros.ocupacaoUltimo()) * pMandante
				+ parametros.bonusAdversario() * (1 - pVisitante) * (1 - pMandante);
		if (rodada > rodadas - parametros.rodadasDecisivas() && posicaoMandante <= 2 && posicaoVisitante <= 2) {
			pelaTabela += parametros.bonusDecisivo();
		}
		double peso = Math.min(1, (rodada - 1) / (double) (parametros.rodadaTabelaInteira() - 1));
		return parametros.ocupacaoInicio() * (1 - peso) + pelaTabela * peso;
	}

	/** 0 para o líder, 1 para o último. */
	private static double relativa(int posicao, int clubes) {
		return clubes <= 1 ? 0 : (posicao - 1) / (double) (clubes - 1);
	}
}
