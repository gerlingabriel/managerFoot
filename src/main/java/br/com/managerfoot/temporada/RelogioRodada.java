package br.com.managerfoot.temporada;

import java.util.List;

/**
 * Relógio da rodada ao vivo (docs/decisions/0010): as divisões jogam uma depois da outra, cada uma
 * com 90 minutos de jogo, na velocidade do usuário se tem clube dele e na rápida se não tem. O
 * progresso é contado em minutos de jogo desde o início da 1ª divisão exibida. Entre uma divisão e a seguinte, o
 * relógio fica parado no último minuto da que terminou, com os placares finais na tela (SPEC §11.2, rev. 111); o
 * fim da última fica na tela pelo mesmo tempo antes de a rodada ser gravada (rev. 110).
 *
 * @param comUsuario para cada divisão, na ordem de exibição, se ela tem clube de usuário
 * @param minutos minutos de cada partida
 * @param msPorMinutoSemUsuario velocidade fixa das divisões sem usuário
 * @param msFimNaTela quanto tempo o fim de uma divisão fica na tela antes da seguinte começar
 */
public record RelogioRodada(List<Boolean> comUsuario, int minutos, int msPorMinutoSemUsuario, long msFimNaTela) {

	/** Cerca de 5 s por divisão sem usuário (SPEC rev. 35, provisório). */
	public static final int MS_POR_MINUTO_SEM_USUARIO = 50;

	public RelogioRodada {
		comUsuario = List.copyOf(comUsuario);
		if (comUsuario.isEmpty() || minutos < 1 || msPorMinutoSemUsuario < 1 || msFimNaTela < 0) {
			throw new IllegalArgumentException("relógio precisa de divisões, minutos e velocidade positivos");
		}
	}

	public RelogioRodada(List<Boolean> comUsuario, int minutos, long msFimNaTela) {
		this(comUsuario, minutos, MS_POR_MINUTO_SEM_USUARIO, msFimNaTela);
	}

	/**
	 * Progresso depois de passar o tempo, com o usuário na velocidade dada; para no fim. No fim de cada divisão,
	 * menos a última, o progresso fica parado {@link #msFimNaTela} antes de a seguinte começar.
	 */
	public double avancar(double progresso, long msPassados, Velocidade velocidade) {
		double atual = progresso;
		double restante = msPassados;
		while (restante > 0 && !terminado(atual)) {
			if (noFimDeDivisao(atual)) {
				if (restante <= msFimNaTela) {
					return atual;
				}
				restante -= msFimNaTela;
			}
			int divisao = (int) Math.floor(atual / minutos);
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

	/**
	 * Índice da divisão em jogo (a última, quando terminado). No fim exato de uma divisão, é ela, com o último
	 * minuto na tela, e não a seguinte.
	 */
	public int divisao(double progresso) {
		int divisao = noFimDeDivisao(progresso) ? (int) Math.round(progresso / minutos) - 1
				: (int) Math.floor(progresso / minutos);
		return Math.min(divisao, comUsuario.size() - 1);
	}

	/** O relógio está parado no fim de uma divisão que não é a última: os placares finais dela estão na tela. */
	public boolean noFimDeDivisao(double progresso) {
		return progresso > 0 && !terminado(progresso) && progresso % minutos == 0;
	}

	/** Minuto já jogado na divisão em jogo (0 a {@link #minutos}). */
	public int minuto(double progresso) {
		return (int) Math.floor(Math.min(progresso, total()) - divisao(progresso) * (double) minutos);
	}
}
