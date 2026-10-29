package br.com.managerfoot.economia;

import java.util.OptionalDouble;
import java.util.random.RandomGenerator;

/**
 * Público de um jogo da liga (SPEC §9.2, rev. 56): a torcida do mandante segue a força dele; quanto
 * vai ao estádio depende da posição dele na tabela e da do adversário. O líder que recebe o 2º enche
 * o estádio do tamanho inicial; o meio da tabela fica perto da metade; o fim, bem abaixo, mesmo
 * recebendo o líder. Só um jogo decisivo no fim da temporada passa da torcida comum. Nunca passa da
 * capacidade. Até a rodada em que a tabela vale inteira, o público reflete a posição final da
 * temporada passada (rev. 57). Domínio puro.
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
	 * @param anteriorMandante posição final da temporada passada, de {@link #relativa(int, int)}; vazia na
	 *        1ª temporada
	 * @param anteriorVisitante idem
	 */
	public int publico(int capacidade, double forcaMandante, int posicaoMandante, int posicaoVisitante, int clubes, int rodada,
			int rodadas, OptionalDouble anteriorMandante, OptionalDouble anteriorVisitante, RandomGenerator aleatorio) {
		double ocupacao = ocupacao(posicaoMandante, posicaoVisitante, clubes, rodada, rodadas, anteriorMandante, anteriorVisitante)
				+ (aleatorio.nextDouble() * 2 - 1) * parametros.variacao();
		int publico = (int) Math.round(torcida(forcaMandante) * Math.max(0, ocupacao));
		return Math.clamp(publico, (int) Math.round(capacidade * parametros.ocupacaoMinima()), capacidade);
	}

	/** Fração da torcida esperada no jogo, antes do sorteio. */
	public double ocupacao(int posicaoMandante, int posicaoVisitante, int clubes, int rodada, int rodadas,
			OptionalDouble anteriorMandante, OptionalDouble anteriorVisitante) {
		boolean decisivo = rodada > rodadas - parametros.rodadasDecisivas() && posicaoMandante <= 2 && posicaoVisitante <= 2;
		return ocupacao(relativa(posicaoMandante, clubes), relativa(posicaoVisitante, clubes), decisivo, rodada, rodadas,
				anteriorMandante, anteriorVisitante);
	}

	/**
	 * Público da ida ou da volta da copa (SPEC §8.2, rev. 70): a conta da liga, com a posição de cada
	 * clube na própria divisão ({@link #relativa(int, int)}) e sem o bônus do jogo decisivo da liga.
	 *
	 * @param rodada a rodada da liga depois da qual o jogo é disputado
	 * @param rodadas total de rodadas da divisão do mandante
	 */
	public int publicoNaCopa(int capacidade, double forcaMandante, double relativaMandante, double relativaVisitante,
			int rodada, int rodadas, OptionalDouble anteriorMandante, OptionalDouble anteriorVisitante,
			RandomGenerator aleatorio) {
		double ocupacao = ocupacao(relativaMandante, relativaVisitante, false, rodada + 1, rodadas, anteriorMandante,
				anteriorVisitante) + (aleatorio.nextDouble() * 2 - 1) * parametros.variacao();
		int publico = (int) Math.round(torcida(forcaMandante) * Math.max(0, ocupacao));
		return Math.clamp(publico, (int) Math.round(capacidade * parametros.ocupacaoMinima()), capacidade);
	}

	/**
	 * Público da final da copa em campo neutro (SPEC §8.2, rev. 69; proposta do agente, provisória): as
	 * duas torcidas, no maior estádio dos dois, com a média da ocupação pela posição de cada um mais o
	 * bônus do jogo decisivo, e nunca abaixo de {@link ParametrosPublico#ocupacaoMinimaFinal()}.
	 *
	 * @param capacidade o maior estádio dos dois finalistas
	 */
	public int publicoNaFinal(int capacidade, double forcaA, double forcaB, double relativaA, double relativaB,
			RandomGenerator aleatorio) {
		double pelaTabela = (pelaPosicao(relativaA, relativaB) + pelaPosicao(relativaB, relativaA)) / 2
				+ parametros.bonusDecisivo();
		double ocupacao = Math.max(parametros.ocupacaoMinimaFinal(), pelaTabela)
				+ (aleatorio.nextDouble() * 2 - 1) * parametros.variacao();
		int publico = (int) Math.round((torcida(forcaA) + torcida(forcaB)) * Math.max(0, ocupacao));
		return Math.clamp(publico, (int) Math.round(capacidade * parametros.ocupacaoMinima()), capacidade);
	}

	private double ocupacao(double relativaMandante, double relativaVisitante, boolean decisivo, int rodada, int rodadas,
			OptionalDouble anteriorMandante, OptionalDouble anteriorVisitante) {
		double pelaTabela = pelaPosicao(relativaMandante, relativaVisitante);
		if (decisivo) {
			pelaTabela += parametros.bonusDecisivo();
		}
		// sem temporada passada (1ª do jogo), a ocupação de início
		double inicio = anteriorMandante.isPresent() && anteriorVisitante.isPresent()
				? pelaPosicao(anteriorMandante.getAsDouble(), anteriorVisitante.getAsDouble())
				: parametros.ocupacaoInicio();
		double peso = Math.min(1, (rodada - 1) / (double) (parametros.rodadaTabelaInteira() - 1));
		return inicio * (1 - peso) + pelaTabela * peso;
	}

	private double pelaPosicao(double pMandante, double pVisitante) {
		return parametros.ocupacaoLider() - (parametros.ocupacaoLider() - parametros.ocupacaoUltimo()) * pMandante
				+ parametros.bonusAdversario() * (1 - pVisitante) * (1 - pMandante);
	}

	/** 0 para o líder, 1 para o último. Serve também para a posição final na divisão da temporada passada. */
	public static double relativa(int posicao, int clubes) {
		return clubes <= 1 ? 0 : (posicao - 1) / (double) (clubes - 1);
	}
}
