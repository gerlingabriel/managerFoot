package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
 * Um gerador derivado da semente da temporada e do número da rodada dá a semente de cada partida,
 * todas antes do primeiro minuto, e depois a semente do sorteio das suspensões por vermelho direto
 * (docs/decisions/0010). Mesmo estado, mesma rodada.
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
	private final int limiteLesoes;

	public ProcessamentoRodada(ParametrosMotor parametrosMotor, RegrasPartida regras, ParametrosSuspensao suspensao,
			ParametrosMoral parametrosMoral) {
		this.motor = new MotorPartida(parametrosMotor);
		this.escalacao = new EscalacaoMaquina(parametrosMotor);
		this.limiteLesoes = parametrosMotor.lesao().limitePorTemporada();
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
		return preparar(temporada, rodada, planteis, disciplina, escalacoes, List.of()).finalizar(moral);
	}

	/**
	 * Prepara a rodada para jogar ao vivo (docs/decisions/0010): escala os times e sorteia a semente
	 * de cada partida antes do primeiro minuto. As suspensões usam um gerador separado, para uma
	 * substituição numa partida não mudar as outras.
	 *
	 * @param substituicoes as já feitas pelos treinadores nesta rodada
	 * @throws IllegalArgumentException se nenhuma divisão tem a rodada
	 * @throws IllegalStateException se uma escalação tem jogador suspenso ou um clube não tem o mínimo para começar
	 */
	public RodadaAoVivo preparar(Temporada temporada, int rodada, Map<Long, List<Jogador>> planteis,
			Disciplina disciplina, Map<Long, Escalacao> escalacoes, List<RodadaAoVivo.Substituicao> substituicoes) {
		SplittableRandom aleatorio = new SplittableRandom(temporada.semente() ^ (rodada * MISTURA_RODADA));
		List<RodadaAoVivo.PartidaDaRodada> partidas = new ArrayList<>();
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
				CondicoesPartida condicoes = new CondicoesPartida(
						Map.of(Lado.CASA, divisao.sorte().de(jogo.mandante()), Lado.FORA,
								divisao.sorte().de(jogo.visitante())),
						divisao.bonus().bonusExtra(),
						Map.of(Lado.CASA, lesoesPermitidas(disciplina, jogo.mandante()), Lado.FORA,
								lesoesPermitidas(disciplina, jogo.visitante())));
				Set<Lado> doUsuario = EnumSet.noneOf(Lado.class);
				if (escalacoes.containsKey(jogo.mandante().id())) {
					doUsuario.add(Lado.CASA);
				}
				if (escalacoes.containsKey(jogo.visitante().id())) {
					doUsuario.add(Lado.FORA);
				}
				partidas.add(new RodadaAoVivo.PartidaDaRodada(divisao.prioridade(), jogo, aleatorio.nextLong(),
						escalar(jogo.mandante(), divisao, planteis, disciplina, escalacoes),
						escalar(jogo.visitante(), divisao, planteis, disciplina, escalacoes), doUsuario, condicoes));
			}
		}
		if (partidas.isEmpty()) {
			throw new IllegalArgumentException("nenhuma divisão tem a rodada " + rodada);
		}
		return new RodadaAoVivo(motor, regras, suspensao, parametrosMoral, partidas, planteis, disciplina,
				aleatorio.nextLong(), substituicoes);
	}

	/** Lesões que o clube ainda pode ter na temporada (SPEC §7.5). */
	private int lesoesPermitidas(Disciplina disciplina, Clube clube) {
		return Math.max(0, limiteLesoes - disciplina.lesoesDoClube(clube.id()));
	}

	/**
	 * A escalação do treinador, se houver, ou a da máquina com quem não está suspenso nem lesionado;
	 * nas duas, o asterisco temporário da temporada.
	 */
	private Escalacao escalar(Clube clube, Temporada.SorteiosDivisao divisao, Map<Long, List<Jogador>> planteis,
			Disciplina disciplina, Map<Long, Escalacao> escalacoes) {
		Escalacao doTreinador = escalacoes.get(clube.id());
		if (doTreinador != null) {
			List<Jogador> suspensos = new ArrayList<>(doTreinador.titulares());
			suspensos.addAll(doTreinador.reservas());
			suspensos.removeIf(j -> !disciplina.indisponivel(j.id()));
			if (!suspensos.isEmpty()) {
				Jogador fora = suspensos.getFirst();
				throw new IllegalStateException(clube.nome() + ": " + fora.nome() + " está "
						+ (disciplina.suspenso(fora.id()) ? "suspenso" : "lesionado"));
			}
			return new Escalacao(doTreinador.formacao(), doTreinador.titulares().stream().map(divisao.bonus()::aplicar).toList(),
					doTreinador.reservas().stream().map(divisao.bonus()::aplicar).toList(),
					doTreinador.batedorPreferido().map(divisao.bonus()::aplicar).orElse(null));
		}
		List<Jogador> disponiveis = plantel(clube, planteis).stream()
			.filter(j -> !disciplina.indisponivel(j.id()))
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
}
