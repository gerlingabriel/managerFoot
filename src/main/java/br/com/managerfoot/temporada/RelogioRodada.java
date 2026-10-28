package br.com.managerfoot.temporada;

import java.util.List;

/**
 * Relógio da rodada ao vivo (docs/decisions/0010): as divisões jogam uma depois da outra, cada uma
 * com 90 minutos de jogo, na velocidade do usuário se tem clube dele e na rápida se não tem. O
 * progresso é contado em minutos de jogo desde o início da 1ª divisão exibida.
 *
 * @param comUsuario para cada divisão, na ordem de exibição, se ela tem clube de usuário
 * @param minutos minutos de cada partida
 * @param msPorMinutoSemUsuario velocidade fixa das divisões sem usuário
 */
public record RelogioRodada(List<Boolean> comUsuario, int minutos, int msPorMinutoSemUsuario) {

	/** Cerca de 5 s por divisão sem usuário (SPEC rev. 35, provisório). */
	public static final int MS_POR_MINUTO_SEM_USUARIO = 50;

	public RelogioRodada {
		comUsuario = List.copyOf(comUsuario);
		if (comUsuario.isEmpty() || minutos < 1 || msPorMinutoSemUsuario < 1) {
			throw new IllegalArgumentException("relógio precisa de divisões, minutos e velocidade positivos");
		}
	}

	public RelogioRodada(List<Boolean> comUsuario, int minutos) {
		this(comUsuario, minutos, MS_POR_MINUTO_SEM_USUARIO);
	}

	/** Progresso depois de passar o tempo, com o usuário na velocidade dada; para no fim. */
	public double avancar(double progresso, long msPassados, Velocidade velocidade) {
		double atual = progresso;
		double restante = msPassados;
		while (restante > 0 && !terminado(atual)) {
			int divisao = divisao(atual);
			int ms = comUsuario.get(divisao) ? velocidade.msPorMinuto() : msPorMinutoSemUsuario;
			double fimDaDivisao = (divisao + 1.0) * minutos;
			double msAteOFim = (fimDaDivisao - atual) * ms;
			if (restante < msAteOFim) {
				return atual + restante / ms;
			}
			atual = fimDaDivisao;
			restante -= msAteOFim;
		}
		return Math.min(atual, total());
	}

	public double total() {
		return (double) comUsuario.size() * minutos;
	}

	public boolean terminado(double progresso) {
		return progresso >= total();
	}

	/** Índice da divisão em jogo (a última, quando terminado). */
	public int divisao(double progresso) {
		return (int) Math.min(Math.floor(progresso / minutos), comUsuario.size() - 1);
	}

	/** Minuto já jogado na divisão em jogo (0 a {@link #minutos}). */
	public int minuto(double progresso) {
		return (int) Math.floor(Math.min(progresso, total()) - divisao(progresso) * (double) minutos);
	}
}
