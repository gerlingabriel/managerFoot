package br.com.managerfoot.motor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;

/**
 * Escalação e tática de um clube controlado pela máquina, pelo plantel (SPEC §7.4.1, rev. 22).
 *
 * <ul>
 * <li>Entre as formações que o plantel completa, escolhe a de maior soma de força dos titulares,
 * com os mais fortes de cada posição. No empate, vale a ordem de {@link Formacao}.</li>
 * <li>Troca asteriscos excedentes pelo melhor sem asterisco da mesma posição, perdendo o mínimo
 * de força, até o limite sem penalidade ou até não haver troca.</li>
 * <li>Banco: o goleiro reserva mais forte, se houver, e os mais fortes restantes.</li>
 * <li>Se nenhuma formação fecha, improvisa na de menor falta: sem goleiro, o defensor mais fraco
 * (ou o jogador de linha mais fraco) vai para o gol com a força reduzida como no goleiro expulso;
 * vagas de linha vão para os mais fortes que sobraram. O improvisado mantém o id e entra com a
 * posição em que atua.</li>
 * <li>Com menos de 11 disponíveis, todos jogam e as vagas que sobram ficam vazias, como se o time
 * começasse com expulsos (SPEC §9.3).</li>
 * </ul>
 */
public final class EscalacaoMaquina {

	/** Mais forte primeiro; no empate, menor id, para o resultado não depender da ordem recebida. */
	private static final Comparator<Jogador> MAIS_FORTE = Comparator.comparingInt(Jogador::forca)
		.reversed()
		.thenComparingLong(Jogador::id);

	private final double fatorGoleiroImprovisado;
	private final int limiteAsteriscos;

	public EscalacaoMaquina(ParametrosMotor parametros) {
		this.fatorGoleiroImprovisado = parametros.penalti().fatorGoleiroImprovisado();
		this.limiteAsteriscos = parametros.asterisco().limiteSemPenalidade();
	}

	/**
	 * @param disponiveis quem pode jogar (sem suspensos), já com o asterisco temporário da temporada
	 * @throws IllegalStateException se houver menos de {@link Escalacao#MINIMO_TITULARES} disponíveis
	 */
	public Escalacao escalar(List<Jogador> disponiveis) {
		if (disponiveis.size() < Escalacao.MINIMO_TITULARES) {
			throw new IllegalStateException("são necessários " + Escalacao.MINIMO_TITULARES
					+ " jogadores disponíveis para escalar, mas há " + disponiveis.size());
		}
		Map<Posicao, List<Jogador>> porPosicao = porPosicao(disponiveis);

		Formacao escolhida = null;
		List<Jogador> titulares = null;
		for (Formacao formacao : Formacao.values()) {
			if (falta(formacao, porPosicao) == 0) {
				List<Jogador> candidatos = limitarAsteriscos(completa(formacao, porPosicao), porPosicao);
				if (titulares == null || soma(candidatos) > soma(titulares)) {
					escolhida = formacao;
					titulares = candidatos;
				}
			}
		}
		if (titulares == null) {
			escolhida = Formacao.values()[0];
			for (Formacao formacao : Formacao.values()) {
				if (falta(formacao, porPosicao) < falta(escolhida, porPosicao)) {
					escolhida = formacao;
				}
			}
			titulares = limitarAsteriscos(improvisada(escolhida, porPosicao, disponiveis), porPosicao);
		}
		return new Escalacao(escolhida, titulares, banco(disponiveis, titulares));
	}

	private static Map<Posicao, List<Jogador>> porPosicao(List<Jogador> jogadores) {
		Map<Posicao, List<Jogador>> mapa = new EnumMap<>(Posicao.class);
		for (Posicao posicao : Posicao.values()) {
			mapa.put(posicao, jogadores.stream().filter(j -> j.posicao() == posicao).sorted(MAIS_FORTE).toList());
		}
		return mapa;
	}

	private static int falta(Formacao formacao, Map<Posicao, List<Jogador>> porPosicao) {
		int falta = 0;
		for (Posicao posicao : Posicao.values()) {
			falta += Math.max(0, formacao.quantidade(posicao) - porPosicao.get(posicao).size());
		}
		return falta;
	}

	private static List<Jogador> completa(Formacao formacao, Map<Posicao, List<Jogador>> porPosicao) {
		List<Jogador> titulares = new ArrayList<>();
		for (Posicao posicao : Posicao.values()) {
			titulares.addAll(porPosicao.get(posicao).subList(0, formacao.quantidade(posicao)));
		}
		return titulares;
	}

	private List<Jogador> improvisada(Formacao formacao, Map<Posicao, List<Jogador>> porPosicao,
			List<Jogador> disponiveis) {
		Map<Posicao, List<Jogador>> escalados = new EnumMap<>(Posicao.class);
		for (Posicao posicao : Posicao.values()) {
			List<Jogador> daPosicao = porPosicao.get(posicao);
			escalados.put(posicao,
					new ArrayList<>(daPosicao.subList(0, Math.min(formacao.quantidade(posicao), daPosicao.size()))));
		}
		Set<Long> usados = new HashSet<>();
		escalados.values().forEach(l -> l.forEach(j -> usados.add(j.id())));

		if (escalados.get(Posicao.GOLEIRO).isEmpty()) {
			List<Jogador> defensores = porPosicao.get(Posicao.DEFENSOR);
			Jogador vaiProGol = (defensores.isEmpty() ? disponiveis : defensores).stream()
				.min(Comparator.comparingInt(Jogador::forca).thenComparingLong(Jogador::id))
				.orElseThrow();
			escalados.values().forEach(l -> l.removeIf(j -> j.id() == vaiProGol.id()));
			usados.add(vaiProGol.id());
			int forca = Math.max(Jogador.FORCA_MINIMA, (int) Math.round(vaiProGol.forca() * fatorGoleiroImprovisado));
			escalados.get(Posicao.GOLEIRO).add(comoPosicao(vaiProGol, Posicao.GOLEIRO, forca));
		}

		// sobras de linha primeiro; goleiros reservas só se não houver mais ninguém
		List<Jogador> sobras = new ArrayList<>(disponiveis.stream().filter(j -> !usados.contains(j.id())).toList());
		sobras.sort(Comparator.comparing((Jogador j) -> j.posicao() == Posicao.GOLEIRO).thenComparing(MAIS_FORTE));
		for (Posicao posicao : List.of(Posicao.DEFENSOR, Posicao.MEIO_CAMPISTA, Posicao.ATACANTE)) {
			List<Jogador> setor = escalados.get(posicao);
			while (setor.size() < formacao.quantidade(posicao) && !sobras.isEmpty()) {
				Jogador completa = sobras.removeFirst();
				setor.add(comoPosicao(completa, posicao, completa.forca()));
			}
		}
		List<Jogador> titulares = new ArrayList<>();
		escalados.values().forEach(titulares::addAll);
		return titulares;
	}

	/** O jogador atuando em outra posição, com o mesmo id para os eventos da partida. */
	private static Jogador comoPosicao(Jogador jogador, Posicao posicao, int forca) {
		return new Jogador(jogador.id(), jogador.nome(), posicao, forca, jogador.temperamento(), jogador.asterisco(),
				jogador.nacionalidade());
	}

	private List<Jogador> limitarAsteriscos(List<Jogador> titulares, Map<Posicao, List<Jogador>> porPosicao) {
		List<Jogador> resultado = new ArrayList<>(titulares);
		while (resultado.stream().filter(Jogador::asterisco).count() > limiteAsteriscos) {
			Set<Long> escalados = new HashSet<>();
			resultado.forEach(j -> escalados.add(j.id()));
			int melhorIndice = -1;
			Jogador melhorEntra = null;
			int menorPerda = Integer.MAX_VALUE;
			for (int i = 0; i < resultado.size(); i++) {
				Jogador sai = resultado.get(i);
				if (!sai.asterisco()) {
					continue;
				}
				Optional<Jogador> entra = porPosicao.get(sai.posicao())
					.stream()
					.filter(j -> !j.asterisco() && !escalados.contains(j.id()))
					.findFirst();
				if (entra.isPresent() && sai.forca() - entra.get().forca() < menorPerda) {
					menorPerda = sai.forca() - entra.get().forca();
					melhorIndice = i;
					melhorEntra = entra.get();
				}
			}
			if (melhorEntra == null) {
				break;
			}
			resultado.set(melhorIndice, melhorEntra);
		}
		return resultado;
	}

	private static List<Jogador> banco(List<Jogador> disponiveis, List<Jogador> titulares) {
		Set<Long> escalados = new HashSet<>();
		titulares.forEach(j -> escalados.add(j.id()));
		List<Jogador> restantes = disponiveis.stream().filter(j -> !escalados.contains(j.id())).sorted(MAIS_FORTE).toList();
		List<Jogador> banco = new ArrayList<>();
		restantes.stream().filter(j -> j.posicao() == Posicao.GOLEIRO).findFirst().ifPresent(banco::add);
		restantes.stream()
			.filter(j -> !banco.contains(j))
			.limit(Escalacao.MAXIMO_RESERVAS - banco.size())
			.forEach(banco::add);
		return banco;
	}

	private static int soma(List<Jogador> jogadores) {
		return jogadores.stream().mapToInt(Jogador::forca).sum();
	}
}
