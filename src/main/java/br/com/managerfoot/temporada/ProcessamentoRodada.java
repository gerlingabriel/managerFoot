package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.ParametrosMoral;
import br.com.managerfoot.dominio.Sorte;
import br.com.managerfoot.motor.CondicoesPartida;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.JogoDecisivo;
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

	/** Mistura o índice da etapa da copa na semente, separada da das rodadas. */
	private static final long MISTURA_ETAPA_COPA = 0xD6E8FEB86659FD93L;

	/** Mistura o índice da etapa da continental na semente, separada das da liga e da copa. */
	private static final long MISTURA_ETAPA_CONTINENTAL = 0xA0761D6478BD642FL;
	private static final long MISTURA_ETAPA_MUNDIAL = 0x8EBC6AF09C88C6E3L;

	/** Uma partida de etapa de copa ou continental: o jogo e, na volta e na final, o que decide. */
	public record PartidaDaEtapa(Jogo jogo, Optional<JogoDecisivo> decisivo) {
	}

	/** Prioridade das partidas da copa na rodada ao vivo: as divisões começam em 1. */
	public static final int PRIORIDADE_COPA = 0;

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
						escalar(jogo.mandante(), divisao.bonus(), planteis, disciplina, escalacoes),
						escalar(jogo.visitante(), divisao.bonus(), planteis, disciplina, escalacoes), doUsuario, condicoes));
			}
		}
		if (partidas.isEmpty()) {
			throw new IllegalArgumentException("nenhuma divisão tem a rodada " + rodada);
		}
		return new RodadaAoVivo(motor, regras, suspensao, parametrosMoral, partidas, planteis, disciplina,
				aleatorio.nextLong(), substituicoes);
	}

	/**
	 * Prepara uma etapa da copa (SPEC §8.2) como uma rodada ao vivo, com a {@link #PRIORIDADE_COPA}.
	 * Cada clube leva a sorte e o bônus de artilharia da própria divisão; a volta e a final precisam
	 * de vencedor. A semente vem da temporada e do índice da etapa no quadro.
	 *
	 * @param indice posição da etapa em {@link Copa#quadro}
	 * @throws IllegalArgumentException se a etapa não tem partidas
	 */
	public RodadaAoVivo prepararCopa(Temporada temporada, int indice, List<Copa.PartidaCopa> jogos,
			Map<Long, List<Jogador>> planteis, Disciplina disciplina, Map<Long, Escalacao> escalacoes,
			List<RodadaAoVivo.Substituicao> substituicoes) {
		if (jogos.isEmpty()) {
			throw new IllegalArgumentException("a etapa " + indice + " da copa não tem partidas");
		}
		return prepararEtapa(temporada, new SplittableRandom(temporada.semente() ^ ((indice + 1) * MISTURA_ETAPA_COPA)),
				jogos.stream().map(p -> new PartidaDaEtapa(p.jogo(), p.decisivo())).toList(), planteis, disciplina,
				escalacoes, substituicoes);
	}

	/**
	 * Prepara uma etapa da competição continental (SPEC §8.3.6) como a da copa. O clube estrangeiro não
	 * tem divisão: joga com sorte neutra e sem bônus de artilharia.
	 *
	 * @param indice posição da etapa em {@link Continental#quadro}
	 * @throws IllegalArgumentException se a etapa não tem partidas
	 */
	public RodadaAoVivo prepararContinental(Temporada temporada, int indice, List<PartidaDaEtapa> jogos,
			Map<Long, List<Jogador>> planteis, Disciplina disciplina, Map<Long, Escalacao> escalacoes,
			List<RodadaAoVivo.Substituicao> substituicoes) {
		if (jogos.isEmpty()) {
			throw new IllegalArgumentException("a etapa " + indice + " da continental não tem partidas");
		}
		return prepararEtapa(temporada,
				new SplittableRandom(temporada.semente() ^ ((indice + 1) * MISTURA_ETAPA_CONTINENTAL)), jogos, planteis,
				disciplina, escalacoes, substituicoes);
	}

	/**
	 * Uma etapa do Mundial (SPEC §8.3.4, rev. 88), como a da continental.
	 *
	 * @param indice posição da etapa em {@link Mundial#ETAPAS}
	 * @throws IllegalArgumentException se a etapa não tem partidas
	 */
	public RodadaAoVivo prepararMundial(Temporada temporada, int indice, List<PartidaDaEtapa> jogos, Map<Long, List<Jogador>> planteis,
			Disciplina disciplina, Map<Long, Escalacao> escalacoes, List<RodadaAoVivo.Substituicao> substituicoes) {
		if (jogos.isEmpty()) {
			throw new IllegalArgumentException("a etapa " + indice + " do Mundial não tem partidas");
		}
		return prepararEtapa(temporada, new SplittableRandom(temporada.semente() ^ ((indice + 1) * MISTURA_ETAPA_MUNDIAL)), jogos,
				planteis, disciplina, escalacoes, substituicoes);
	}

	private RodadaAoVivo prepararEtapa(Temporada temporada, SplittableRandom aleatorio, List<PartidaDaEtapa> jogos,
			Map<Long, List<Jogador>> planteis, Disciplina disciplina, Map<Long, Escalacao> escalacoes,
			List<RodadaAoVivo.Substituicao> substituicoes) {
		List<RodadaAoVivo.PartidaDaRodada> partidas = new ArrayList<>();
		for (PartidaDaEtapa p : jogos) {
			Jogo jogo = p.jogo();
			Optional<Temporada.SorteiosDivisao> daCasa = divisaoNaLiga(temporada, jogo.mandante());
			Optional<Temporada.SorteiosDivisao> deFora = divisaoNaLiga(temporada, jogo.visitante());
			Set<Long> bonusExtra = new HashSet<>();
			daCasa.ifPresent(d -> bonusExtra.addAll(d.bonus().bonusExtra()));
			deFora.ifPresent(d -> bonusExtra.addAll(d.bonus().bonusExtra()));
			CondicoesPartida condicoes = new CondicoesPartida(
					Map.of(Lado.CASA, daCasa.map(d -> d.sorte().de(jogo.mandante())).orElse(Sorte.NEUTRA), Lado.FORA,
							deFora.map(d -> d.sorte().de(jogo.visitante())).orElse(Sorte.NEUTRA)),
					bonusExtra, Map.of(Lado.CASA, lesoesPermitidas(disciplina, jogo.mandante()), Lado.FORA,
							lesoesPermitidas(disciplina, jogo.visitante())));
			if (p.decisivo().isPresent()) {
				condicoes = condicoes.comDecisivo(p.decisivo().get());
			}
			Set<Lado> doUsuario = EnumSet.noneOf(Lado.class);
			if (escalacoes.containsKey(jogo.mandante().id())) {
				doUsuario.add(Lado.CASA);
			}
			if (escalacoes.containsKey(jogo.visitante().id())) {
				doUsuario.add(Lado.FORA);
			}
			partidas.add(new RodadaAoVivo.PartidaDaRodada(PRIORIDADE_COPA, jogo, aleatorio.nextLong(),
					escalar(jogo.mandante(), bonusDe(daCasa), planteis, disciplina, escalacoes),
					escalar(jogo.visitante(), bonusDe(deFora), planteis, disciplina, escalacoes), doUsuario, condicoes));
		}
		return new RodadaAoVivo(motor, regras, suspensao, parametrosMoral, partidas, planteis, disciplina,
				aleatorio.nextLong(), substituicoes);
	}

	private static final BonusArtilharia SEM_BONUS = new BonusArtilharia(Set.of(), Set.of());

	private static BonusArtilharia bonusDe(Optional<Temporada.SorteiosDivisao> divisao) {
		return divisao.map(Temporada.SorteiosDivisao::bonus).orElse(SEM_BONUS);
	}

	/** A divisão do clube na liga da temporada; vazio para o estrangeiro. */
	private static Optional<Temporada.SorteiosDivisao> divisaoNaLiga(Temporada temporada, Clube clube) {
		return temporada.divisoes().stream()
			.filter(d -> d.calendario().jogos().stream().anyMatch(j -> j.envolve(clube)))
			.findFirst();
	}

	/** A divisão do clube na temporada, pelo calendário. */
	public static Temporada.SorteiosDivisao divisaoDe(Temporada temporada, Clube clube) {
		return temporada.divisoes().stream()
			.filter(d -> d.calendario().jogos().stream().anyMatch(j -> j.envolve(clube)))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(clube.nome() + " não joga a liga da temporada"));
	}

	/** Lesões que o clube ainda pode ter na temporada (SPEC §7.5). */
	private int lesoesPermitidas(Disciplina disciplina, Clube clube) {
		return Math.max(0, limiteLesoes - disciplina.lesoesDoClube(clube.id()));
	}

	/**
	 * A escalação do treinador, se houver, ou a da máquina com quem não está suspenso nem lesionado;
	 * nas duas, o asterisco temporário da temporada.
	 */
	private Escalacao escalar(Clube clube, BonusArtilharia bonus, Map<Long, List<Jogador>> planteis,
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
			return new Escalacao(doTreinador.formacao(), doTreinador.titulares().stream().map(bonus::aplicar).toList(),
					doTreinador.reservas().stream().map(bonus::aplicar).toList(),
					doTreinador.batedorPreferido().map(bonus::aplicar).orElse(null));
		}
		List<Jogador> disponiveis = plantel(clube, planteis).stream()
			.filter(j -> !disciplina.indisponivel(j.id()))
			.map(bonus::aplicar)
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
