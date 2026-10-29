package br.com.managerfoot.economia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.Rodada;

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

	/** Preço do ingresso na divisão. */
	public long ingresso(int prioridadeDivisao) {
		return parametros.ingresso(prioridadeDivisao);
	}

	/** Salário por rodada do jogador, pela força. */
	public long salario(Jogador jogador) {
		return salario(jogador.forca());
	}

	/**
	 * Salário por rodada de um jogador da força dada (a do contrato, rev. 58), com o ajuste da faixa de
	 * força (rev. 61).
	 */
	public long salario(int forca) {
		return Math.round(salarioSemFaixa(forca) * parametros.fatorSalario(forca));
	}

	/** Salário da força dada sem o ajuste da faixa (rev. 65). */
	public double salarioSemFaixa(int forca) {
		return parametros.salarioReferencia() * Math.pow(parametros.crescimentoSalario(), forca - parametros.forcaReferencia());
	}

	/** Ajuste da faixa para a força dada, de jogador ou de clube (rev. 61 e 65). */
	public double fatorDaFaixa(double forca) {
		return parametros.fatorSalario(forca);
	}

	/**
	 * Folha por rodada. Com o salário pelo clube (rev. 65), a faixa é a da força em campo do clube,
	 * a mesma para todos os jogadores.
	 */
	public long folha(List<Jogador> plantel) {
		if (parametros.salarioPeloClube()) {
			double fator = fatorDaFaixa(Clube.forcaEmCampo(plantel));
			return plantel.stream().mapToLong(j -> Math.round(salarioSemFaixa(j.forca()) * fator)).sum();
		}
		return plantel.stream().mapToLong(this::salario).sum();
	}

	public long saldoInicial(List<Jogador> plantel) {
		return folha(plantel) * parametros.rodadasDeFolhaNoInicio();
	}

	/**
	 * Público do jogo em casa pela regra da rev. 37: a ocupação sobe com a moral do mandante e com um
	 * adversário mais forte, com uma pequena variação sorteada; nunca passa da capacidade. O jogo usa
	 * {@link PublicoDoJogo} desde a rev. 57; esta fica para as simulações de economia e mercado.
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
		return lancamentos(local, prioridadeDivisao, publico, folha(plantel));
	}

	/** Como {@link #lancamentos(Local, int, int, List)}, com a folha já somada (salários gravados, rev. 58). */
	public List<Lancamento> lancamentos(Local local, int prioridadeDivisao, int publico, long folha) {
		List<Lancamento> lancamentos = new ArrayList<>();
		if (local == Local.CASA) {
			long bilheteria = publico * parametros.ingresso(prioridadeDivisao);
			lancamentos.add(new Lancamento(TipoLancamento.BILHETERIA, bilheteria));
			lancamentos.add(new Lancamento(TipoLancamento.CUSTO_DO_JOGO, -Math.round(bilheteria * parametros.custoDoJogo())));
		} else if (local == Local.FORA) {
			lancamentos.add(new Lancamento(TipoLancamento.DESLOCAMENTO, -parametros.deslocamento(prioridadeDivisao)));
		}
		lancamentos.add(new Lancamento(TipoLancamento.SALARIOS, -folha));
		return lancamentos;
	}

	/**
	 * Caixa de uma divisão numa rodada: os lançamentos de cada clube, inclusive o da folga.
	 *
	 * @param publicos pagantes de cada jogo, pelo id do mandante ({@link PublicoDoJogo})
	 * @param folhas folha de cada clube por rodada, pelos salários gravados
	 */
	public Movimento rodada(Rodada rodada, int prioridadeDivisao, Map<Long, Integer> publicos, Map<Long, Long> folhas) {
		Map<Long, Integer> publicoPorMandante = new HashMap<>();
		Map<Long, List<Lancamento>> lancamentos = new HashMap<>();
		for (Jogo jogo : rodada.jogos()) {
			long mandante = jogo.mandante().id();
			int publico = publicos.get(mandante);
			publicoPorMandante.put(mandante, publico);
			lancamentos.put(mandante, lancamentos(Local.CASA, prioridadeDivisao, publico, folhas.getOrDefault(mandante, 0L)));
			long visitante = jogo.visitante().id();
			lancamentos.put(visitante, lancamentos(Local.FORA, prioridadeDivisao, 0, folhas.getOrDefault(visitante, 0L)));
		}
		rodada.folga().ifPresent(c -> lancamentos.put(c.id(),
				lancamentos(Local.FOLGA, prioridadeDivisao, 0, folhas.getOrDefault(c.id(), 0L))));
		return new Movimento(publicoPorMandante, lancamentos);
	}

	/** Público de cada jogo pelo id do mandante e lançamentos de cada clube pelo id. */
	public record Movimento(Map<Long, Integer> publicoPorMandante, Map<Long, List<Lancamento>> lancamentos) {

		public Movimento {
			publicoPorMandante = Map.copyOf(publicoPorMandante);
			lancamentos = Map.copyOf(lancamentos);
		}
	}

	public enum Local {
		CASA,
		FORA,
		FOLGA
	}
}
