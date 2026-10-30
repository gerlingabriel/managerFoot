package br.com.managerfoot.bancopadrao;

import java.util.List;
import java.util.Map;

import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

/**
 * Números do gerador do banco padrão (SPEC §4.3, §5.2 e §5.3, rev. 31). Ficam aqui para serem
 * ajustados sem mexer no algoritmo.
 *
 * @param semente mesma semente, mesmo banco
 * @param faixas faixa de força dos clubes por divisão, da principal para baixo; o topo é o teto da
 *        divisão
 * @param composicao jogadores gerados por posição (20 no total desde a rev. 60)
 * @param variacaoTitulares os 11 da escalação nascem a até esta distância da força do clube
 * @param quedaReservasMinima os reservas nascem pelo menos isto abaixo da força do clube
 * @param quedaReservasMaxima e no máximo isto abaixo
 * @param maximoAsteriscos asteriscos por clube (0 a este número)
 * @param chanceAsteriscoMaisFraco chance de cada asterisco no clube mais fraco possível
 * @param chanceAsteriscoMaisForte chance de cada asterisco no clube de força 50
 * @param pesosTemperamento sorteio com peso: Normal comum, Violento raro (provisório)
 * @param pesoNomeComposto peso de "Nome Sobrenome"
 * @param pesoSoNome peso de só o nome
 * @param pesoSoSobrenome peso de só o sobrenome
 * @param cores cores dos escudos, no formato #RRGGBB
 * @param faixasEstrangeiros faixa de força dos clubes dos países gerados só para a continental, por
 *        nível (rev. 73, provisório)
 * @param estrangeirosNoPlantel quantos jogadores de outro país cada clube tem, pela região dele (rev. 87,
 *        provisório)
 * @param pesoMesmaRegiao peso, no sorteio do país do estrangeiro, de um país da região do clube; os
 *        outros pesam 1 (rev. 87, provisório)
 */
public record ParametrosBancoPadrao(long semente, List<FaixaForca> faixas, Map<Posicao, Integer> composicao,
		int variacaoTitulares, int quedaReservasMinima, int quedaReservasMaxima, int maximoAsteriscos,
		double chanceAsteriscoMaisFraco, double chanceAsteriscoMaisForte, Map<Temperamento, Integer> pesosTemperamento,
		int pesoNomeComposto, int pesoSoNome, int pesoSoSobrenome, List<String> cores,
		Map<NivelEstrangeiro, FaixaForca> faixasEstrangeiros, Map<Regiao, FaixaQuantidade> estrangeirosNoPlantel,
		int pesoMesmaRegiao) {

	/** Faixa de força dos clubes de uma divisão do banco padrão. */
	public record FaixaForca(int minima, int maxima) {
	}

	/** Quantos jogadores de outro país um clube tem, sorteados nesta faixa. */
	public record FaixaQuantidade(int minima, int maxima) {
	}

	/** Faixas da rev. 31: 1ª de 35 a 50, 2ª de 22 a 30 (a 3ª, de 12 a 22, saiu na rev. 60). */
	public static final List<FaixaForca> FAIXAS = List.of(new FaixaForca(35, 50), new FaixaForca(22, 30));

	public ParametrosBancoPadrao {
		faixas = List.copyOf(faixas);
		composicao = Map.copyOf(composicao);
		pesosTemperamento = Map.copyOf(pesosTemperamento);
		cores = List.copyOf(cores);
		faixasEstrangeiros = Map.copyOf(faixasEstrangeiros);
		estrangeirosNoPlantel = Map.copyOf(estrangeirosNoPlantel);
		if (estrangeirosNoPlantel.keySet().size() != Regiao.values().length) {
			throw new IllegalArgumentException("toda região precisa da faixa de estrangeiros: " + estrangeirosNoPlantel);
		}
		if (faixasEstrangeiros.keySet().size() != NivelEstrangeiro.values().length) {
			throw new IllegalArgumentException("todo nível de estrangeiro precisa de faixa: " + faixasEstrangeiros);
		}
		if (pesosTemperamento.keySet().size() != Temperamento.values().length) {
			throw new IllegalArgumentException("todo temperamento precisa de peso: " + pesosTemperamento);
		}
		if (cores.size() < 2) {
			throw new IllegalArgumentException("os escudos precisam de ao menos duas cores");
		}
	}

	/** Níveis da rev. 73: A com a faixa da 1ª divisão, B e C abaixo (a conferir por simulação). */
	public static final Map<NivelEstrangeiro, FaixaForca> FAIXAS_ESTRANGEIROS = Map.of(NivelEstrangeiro.A,
			FAIXAS.getFirst(), NivelEstrangeiro.B, new FaixaForca(31, 45), NivelEstrangeiro.C, new FaixaForca(27, 40));

	public static ParametrosBancoPadrao padrao() {
		return new ParametrosBancoPadrao(19980101L, FAIXAS,
				Map.of(Posicao.GOLEIRO, 3, Posicao.DEFENSOR, 6, Posicao.MEIO_CAMPISTA, 6, Posicao.ATACANTE, 5), 2, 3, 8,
				5, 0.15, 0.60,
				Map.of(Temperamento.NORMAL, 40, Temperamento.CALMO, 20, Temperamento.EDUCADO, 20, Temperamento.ANSIOSO,
						13, Temperamento.VIOLENTO, 7),
				40, 30, 30,
				List.of("#FFFFFF", "#000000", "#C8102E", "#003DA5", "#00843D", "#FFD100", "#6CACE4", "#7A263A",
						"#F57C00", "#5B2C83", "#1B365D", "#8A8D8F"),
				FAIXAS_ESTRANGEIROS,
				Map.of(Regiao.AMERICA_DO_SUL, new FaixaQuantidade(2, 5), Regiao.EUROPA, new FaixaQuantidade(5, 9)), 3);
	}
}
