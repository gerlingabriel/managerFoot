package br.com.managerfoot.economia;

import java.util.Comparator;
import java.util.List;

/**
 * Valores do caixa da rodada (SPEC §9, rev. 37). Todos provisórios: propostos pelo agente e
 * calibrados por simulação ({@code docs/simulacoes/economia-caixa.md}) antes da aprovação.
 * Dinheiro em centavos de Real.
 *
 * @param forcaCapacidadeMinima força até a qual o clube tem o estádio mínimo
 * @param capacidadeMinima estádio do clube mais fraco
 * @param capacidadeMaxima estádio do clube de força máxima; entre os dois, a capacidade cresce na mesma
 *        proporção a cada ponto de força, como o salário, para o clube forte não gastar mais do que arrecada
 * @param ingressoPorDivisao preço do ingresso por prioridade da divisão (1ª, 2ª, ...); a última vale
 *        para as de baixo
 * @param ocupacaoBase fração do estádio ocupada com moral inicial e adversário de mesma força
 * @param pesoMoral quanto a moral (de 0 ao máximo) mexe na ocupação, em torno da moral inicial
 * @param pesoAdversario quanto um adversário 20 pontos mais forte aumenta a ocupação
 * @param variacaoPublico sorteio uniforme de ± esta fração na ocupação
 * @param custoDoJogo fração da bilheteria gasta para abrir o estádio
 * @param deslocamentoPorDivisao despesa de cada jogo fora, por prioridade da divisão
 * @param salarioReferencia salário por rodada do jogador de força {@code forcaReferencia}
 * @param crescimentoSalario multiplicador do salário por ponto de força
 * @param rodadasDeFolhaNoInicio saldo inicial, em rodadas da folha do clube
 * @param faixasSalario ajuste do salário pela força (rev. 61): cada faixa vale da sua força mínima até a
 *        da faixa seguinte; abaixo da menor, o salário fica sem ajuste
 * @param salarioPeloClube a faixa do salário segue a força em campo do clube, não a do jogador
 *        (rev. 65 e 66, pelo usuário)
 */
public record ParametrosEconomia(int forcaCapacidadeMinima, int capacidadeMinima, int capacidadeMaxima,
		List<Long> ingressoPorDivisao, double ocupacaoBase, double pesoMoral, double pesoAdversario,
		double variacaoPublico, double custoDoJogo, List<Long> deslocamentoPorDivisao, long salarioReferencia,
		int forcaReferencia, double crescimentoSalario, int rodadasDeFolhaNoInicio, List<FaixaSalario> faixasSalario,
		boolean salarioPeloClube) {

	/** A partir de {@code forcaMinima}, o salário é multiplicado por {@code fator}. */
	public record FaixaSalario(int forcaMinima, double fator) {

		public FaixaSalario {
			if (fator <= 0) {
				throw new IllegalArgumentException("o fator do salário deve ser positivo");
			}
		}
	}

	public ParametrosEconomia {
		ingressoPorDivisao = List.copyOf(ingressoPorDivisao);
		deslocamentoPorDivisao = List.copyOf(deslocamentoPorDivisao);
		faixasSalario = faixasSalario.stream().sorted(Comparator.comparingInt(FaixaSalario::forcaMinima)).toList();
		if (capacidadeMinima < 1 || capacidadeMaxima < capacidadeMinima || ingressoPorDivisao.isEmpty()
				|| deslocamentoPorDivisao.isEmpty() || custoDoJogo < 0 || custoDoJogo >= 1 || crescimentoSalario <= 1) {
			throw new IllegalArgumentException("parâmetros de economia inválidos");
		}
	}

	/**
	 * Proposta inicial (rev. 37), em centavos de Real. Faixas do salário pela força em campo do clube
	 * (rev. 66; +10% e +20% em cima pelo usuário, deflação embaixo escolhida pelo agente na simulação):
	 * força 45 a 50 +20%, 35 a 44 +10%, 25 a 34 sem ajuste, 15 a 24 −15%, abaixo de 15 −30%.
	 */
	public static ParametrosEconomia padrao() {
		return new ParametrosEconomia(12, 5_000, 80_000, List.of(40_00L, 40_00L, 40_00L), 0.45, 0.5, 0.15, 0.08, 0.15,
				List.of(60_000_00L, 30_000_00L, 15_000_00L), 2_000_00L, 17, 1.072, 10,
				List.of(new FaixaSalario(0, 0.70), new FaixaSalario(15, 0.85), new FaixaSalario(25, 1.0),
						new FaixaSalario(35, 1.10), new FaixaSalario(45, 1.20)), true);
	}

	/** Os mesmos valores com outras faixas e a faixa do salário pelo clube ou pelo jogador (rev. 65). */
	public ParametrosEconomia comFaixas(List<FaixaSalario> faixas, boolean peloClube) {
		return new ParametrosEconomia(forcaCapacidadeMinima, capacidadeMinima, capacidadeMaxima, ingressoPorDivisao,
				ocupacaoBase, pesoMoral, pesoAdversario, variacaoPublico, custoDoJogo, deslocamentoPorDivisao,
				salarioReferencia, forcaReferencia, crescimentoSalario, rodadasDeFolhaNoInicio, faixas, peloClube);
	}

	/** Fator do salário na força dada (rev. 61). */
	public double fatorSalario(double forca) {
		double fator = 1.0;
		for (FaixaSalario faixa : faixasSalario) {
			if (forca >= faixa.forcaMinima()) {
				fator = faixa.fator();
			}
		}
		return fator;
	}

	public long ingresso(int prioridade) {
		return ingressoPorDivisao.get(Math.min(prioridade, ingressoPorDivisao.size()) - 1);
	}

	public long deslocamento(int prioridade) {
		return deslocamentoPorDivisao.get(Math.min(prioridade, deslocamentoPorDivisao.size()) - 1);
	}
}
