package br.com.managerfoot.economia;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;

/**
 * Caixa da rodada (SPEC §9.1 e §9.2, rev. 37): estádio, público, bilheteria, despesas de jogo e
 * salários. Domínio puro e reproduzível: o público usa o gerador recebido. Dinheiro em centavos de
 * Real.
 */
public final class CaixaDaRodada {

	private final ParametrosEconomia parametros;
	private final ParametrosMoral parametrosMoral;

	public CaixaDaRodada(ParametrosEconomia parametros, ParametrosMoral parametrosMoral) {
		this.parametros = parametros;
		this.parametrosMoral = parametrosMoral;
	}

	public static CaixaDaRodada padrao() {
		return new CaixaDaRodada(ParametrosEconomia.padrao(), ParametrosMoral.padrao());
	}

	/**
	 * Capacidade inicial do estádio pela força do clube, em múltiplos de 500 (SPEC §9.2): cresce na
	 * mesma proporção a cada ponto de força, do mínimo ao máximo.
	 */
	public int capacidadeInicial(int forcaClube) {
		int pontos = Math.clamp(forcaClube - parametros.forcaCapacidadeMinima(), 0,
				Jogador.FORCA_MAXIMA - parametros.forcaCapacidadeMinima());
		double crescimento = Math.pow((double) parametros.capacidadeMaxima() / parametros.capacidadeMinima(),
				1.0 / (Jogador.FORCA_MAXIMA - parametros.forcaCapacidadeMinima()));
		return (int) Math.round(parametros.capacidadeMinima() * Math.pow(crescimento, pontos) / 500) * 500;
	}

	/** Salário por rodada do jogador, pela força. */
	public long salario(Jogador jogador) {
		return Math.round(parametros.salarioReferencia()
				* Math.pow(parametros.crescimentoSalario(), jogador.forca() - parametros.forcaReferencia()));
	}

	public long folha(List<Jogador> plantel) {
		return plantel.stream().mapToLong(this::salario).sum();
	}

	public long saldoInicial(List<Jogador> plantel) {
		return folha(plantel) * parametros.rodadasDeFolhaNoInicio();
	}

	/**
	 * Público do jogo em casa: a ocupação sobe com a moral do mandante e com um adversário mais forte
	 * (SPEC §9.2), com uma pequena variação sorteada; nunca passa da capacidade.
	 */
	public int publico(int capacidade, Moral moralMandante, int forcaMandante, int forcaVisitante,
			RandomGenerator aleatorio) {
		double moral = (moralMandante.valor() - parametrosMoral.inicial()) / (double) parametrosMoral.maximo();
		double adversario = Math.clamp((forcaVisitante - forcaMandante) / 20.0, -1, 1);
		double ocupacao = parametros.ocupacaoBase() + parametros.pesoMoral() * moral + parametros.pesoAdversario() * adversario
				+ (aleatorio.nextDouble() * 2 - 1) * parametros.variacaoPublico();
		return (int) Math.round(capacidade * Math.clamp(ocupacao, 0.1, 1.0));
	}

	/**
	 * Lançamentos do clube numa rodada: em casa, bilheteria e custo do jogo; fora, deslocamento; e a
	 * folha de salários. Na folga, só a folha.
	 *
	 * @param publico pagantes do jogo em casa (ignorado fora de casa ou na folga)
	 */
	public List<Lancamento> lancamentos(Local local, int prioridadeDivisao, int publico, List<Jogador> plantel) {
		List<Lancamento> lancamentos = new ArrayList<>();
		if (local == Local.CASA) {
			long bilheteria = publico * parametros.ingresso(prioridadeDivisao);
			lancamentos.add(new Lancamento(TipoLancamento.BILHETERIA, bilheteria));
			lancamentos.add(new Lancamento(TipoLancamento.CUSTO_DO_JOGO, -Math.round(bilheteria * parametros.custoDoJogo())));
		} else if (local == Local.FORA) {
			lancamentos.add(new Lancamento(TipoLancamento.DESLOCAMENTO, -parametros.deslocamento(prioridadeDivisao)));
		}
		lancamentos.add(new Lancamento(TipoLancamento.SALARIOS, -folha(plantel)));
		return lancamentos;
	}

	public enum Local {
		CASA,
		FORA,
		FOLGA
	}
}
