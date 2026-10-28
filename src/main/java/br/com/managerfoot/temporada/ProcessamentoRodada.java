package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.motor.CondicoesPartida;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.MotorPartida;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.RegrasPartida;
import br.com.managerfoot.motor.ResultadoPartida;

/**
 * Joga uma rodada da liga em processamento rápido (SPEC §7.4.1 e §11.2): os clubes de treinador
 * humano jogam com a escalação dele; os demais são escalados pela máquina. Domínio puro: quem chama carrega e grava o estado.
 *
 * <p>Ordem: da divisão de maior número de prioridade até a 1ª; dentro dela, a do calendário.
 * Um gerador derivado da semente da temporada e do número da rodada dá a semente de cada partida
 * e, logo depois dela, o sorteio das suspensões por vermelho direto. Mesmo estado, mesma rodada.
 */
public final class ProcessamentoRodada {

	/** Uma partida jogada, com a semente que a reproduz. */
	public record PartidaJogada(int prioridade, Jogo jogo, long semente, ResultadoPartida resultado) {
	}

	/** Partidas da rodada e o estado depois delas. */
	public record Resultado(List<PartidaJogada> partidas, Disciplina disciplina, Map<Long, Moral> moral) {

		public Resultado {
			partidas = List.copyOf(partidas);
			moral = Map.copyOf(moral);
		}
	}

	/** Mistura o número da rodada na semente (constante de ouro de 64 bits). */
	private static final long MISTURA_RODADA = 0x9E3779B97F4A7C15L;

	private final MotorPartida motor;
	private final EscalacaoMaquina escalacao;
	private final RegrasPartida regras;
	private final ParametrosSuspensao suspensao;
	private final ParametrosMoral parametrosMoral;

	public ProcessamentoRodada(ParametrosMotor parametrosMotor, RegrasPartida regras, ParametrosSuspensao suspensao,
			ParametrosMoral parametrosMoral) {
		this.motor = new MotorPartida(parametrosMotor);
		this.escalacao = new EscalacaoMaquina(parametrosMotor);
		this.regras = Objects.requireNonNull(regras);
		this.suspensao = Objects.requireNonNull(suspensao);
		this.parametrosMoral = Objects.requireNonNull(parametrosMoral);
	}

	public static ProcessamentoRodada padrao() {
		return new ProcessamentoRodada(ParametrosMotor.padrao(), RegrasPartida.padrao(), ParametrosSuspensao.padrao(),
				ParametrosMoral.padrao());
	}

	/**
	 * @param rodada número da rodada; divisões com menos rodadas não jogam
	 * @param planteis jogadores de cada clube por id do clube, com o asterisco permanente
	 * @param moral moral por id do clube; ausente é a inicial
	 * @throws IllegalArgumentException se nenhuma divisão tem a rodada
	 */
	public Resultado jogar(Temporada temporada, int rodada, Map<Long, List<Jogador>> planteis, Disciplina disciplina,
			Map<Long, Moral> moral) {
		return jogar(temporada, rodada, planteis, disciplina, moral, Map.of());
	}

	/**
	 * Como {@link #jogar(Temporada, int, Map, Disciplina, Map)}, com a escalação dos treinadores.
	 *
	 * @param escalacoes escalação por id do clube, com os jogadores do plantel; os clubes ausentes
	 *        são escalados pela máquina
	 * @throws IllegalStateException se uma escalação tem jogador suspenso
	 */
	public Resultado jogar(Temporada temporada, int rodada, Map<Long, List<Jogador>> planteis, Disciplina disciplina,
			Map<Long, Moral> moral, Map<Long, Escalacao> escalacoes) {
		SplittableRandom aleatorio = new SplittableRandom(temporada.semente() ^ (rodada * MISTURA_RODADA));
		Map<Long, Moral> novaMoral = new HashMap<>(moral);
		List<PartidaJogada> partidas = new ArrayList<>();
		List<Temporada.SorteiosDivisao> ordem = temporada.divisoes()
			.stream()
			.sorted(Comparator.comparingInt(Temporada.SorteiosDivisao::prioridade).reversed())
			.toList();
		for (Temporada.SorteiosDivisao divisao : ordem) {
			List<Jogo> jogos = divisao.calendario()
				.rodadas()
				.stream()
				.filter(r -> r.numero() == rodada)
				.flatMap(r -> r.jogos().stream())
				.toList();
			for (Jogo jogo : jogos) {
				long semente = aleatorio.nextLong();
				CondicoesPartida condicoes = new CondicoesPartida(
						Map.of(Lado.CASA, divisao.sorte().de(jogo.mandante()), Lado.FORA,
								divisao.sorte().de(jogo.visitante())),
						divisao.bonus().bonusExtra());
				ResultadoPartida resultado = motor.simular(regras,
						escalar(jogo.mandante(), divisao, planteis, disciplina, escalacoes),
						escalar(jogo.visitante(), divisao, planteis, disciplina, escalacoes), condicoes, semente);
				partidas.add(new PartidaJogada(divisao.prioridade(), jogo, semente, resultado));

				List<Long> jogadores = new ArrayList<>();
				plantel(jogo.mandante(), planteis).forEach(j -> jogadores.add(j.id()));
				plantel(jogo.visitante(), planteis).forEach(j -> jogadores.add(j.id()));
				disciplina = disciplina.aposPartida(jogadores, resultado, suspensao, aleatorio);
				atualizarMoral(novaMoral, jogo.mandante(), true, resultado.golsCasa(), resultado.golsFora());
				atualizarMoral(novaMoral, jogo.visitante(), false, resultado.golsFora(), resultado.golsCasa());
			}
		}
		if (partidas.isEmpty()) {
			throw new IllegalArgumentException("nenhuma divisão tem a rodada " + rodada);
		}
		return new Resultado(partidas, disciplina, novaMoral);
	}

	/**
	 * A escalação do treinador, se houver, ou a da máquina com quem não está suspenso; nas duas, o
	 * asterisco temporário da temporada.
	 */
	private Escalacao escalar(Clube clube, Temporada.SorteiosDivisao divisao, Map<Long, List<Jogador>> planteis,
			Disciplina disciplina, Map<Long, Escalacao> escalacoes) {
		Escalacao doTreinador = escalacoes.get(clube.id());
		if (doTreinador != null) {
			List<Jogador> suspensos = new ArrayList<>(doTreinador.titulares());
			suspensos.addAll(doTreinador.reservas());
			suspensos.removeIf(j -> !disciplina.suspenso(j.id()));
			if (!suspensos.isEmpty()) {
				throw new IllegalStateException(clube.nome() + ": " + suspensos.getFirst().nome() + " está suspenso");
			}
			return new Escalacao(doTreinador.formacao(), doTreinador.titulares().stream().map(divisao.bonus()::aplicar).toList(),
					doTreinador.reservas().stream().map(divisao.bonus()::aplicar).toList(),
					doTreinador.batedorPreferido().map(divisao.bonus()::aplicar).orElse(null));
		}
		List<Jogador> disponiveis = plantel(clube, planteis).stream()
			.filter(j -> !disciplina.suspenso(j.id()))
			.map(divisao.bonus()::aplicar)
			.toList();
		try {
			return escalacao.escalar(disponiveis);
		}
		catch (IllegalStateException e) {
			throw new IllegalStateException(clube.nome() + ": " + e.getMessage(), e);
		}
	}

	private static List<Jogador> plantel(Clube clube, Map<Long, List<Jogador>> planteis) {
		return planteis.getOrDefault(clube.id(), List.of());
	}

	private void atualizarMoral(Map<Long, Moral> moral, Clube clube, boolean emCasa, int golsPro, int golsContra) {
		Moral atual = moral.getOrDefault(clube.id(), Moral.inicial(parametrosMoral));
		moral.put(clube.id(), atual.aposJogo(emCasa, golsPro, golsContra, parametrosMoral));
	}
}
