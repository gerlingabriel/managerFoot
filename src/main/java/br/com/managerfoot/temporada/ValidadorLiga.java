package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;

/**
 * Verifica se uma liga pode iniciar (SPEC §4, §13): o formato do molde com os clubes do país, ou a
 * liga já distribuída. Devolve todos os problemas de uma vez, com
 * mensagens que dizem o que falta, para o editor exibir.
 */
public final class ValidadorLiga {

	/** Faixa de clubes por divisão (SPEC §4.1). */
	public static final int MINIMO_CLUBES_POR_DIVISAO = 15;
	public static final int MAXIMO_CLUBES_POR_DIVISAO = 20;
	/** Divisões por país (SPEC §4.1, rev. 60): a 3ª foi retirada. */
	public static final int MAXIMO_DIVISOES = 2;
	/** A liga principal tem limite de força máximo (SPEC §4.2). */
	public static final int LIMITE_FORCA_PRINCIPAL = Jogador.FORCA_MAXIMA;

	private ValidadorLiga() {
	}

	public static List<String> problemas(ConfiguracaoLiga liga) {
		List<String> problemas = new ArrayList<>();
		if (liga.nome() == null || liga.nome().isBlank()) {
			problemas.add("A liga precisa de um nome.");
		}
		List<Divisao> divisoes = liga.divisoes();
		if (divisoes.isEmpty()) {
			problemas.add("A liga precisa de ao menos uma divisão.");
			return problemas;
		}
		validarQuantidadeDivisoes(divisoes.size(), problemas);

		for (int i = 0; i < divisoes.size(); i++) {
			Divisao divisao = divisoes.get(i);
			int prioridadeEsperada = i + 1;
			if (divisao.prioridade() != prioridadeEsperada) {
				problemas.add("As prioridades devem ser 1, 2, 3... sem repetir nem pular: " + descrever(divisao)
						+ " deveria ter prioridade " + prioridadeEsperada + ".");
			}
			validarDivisao(divisao, i == 0, problemas);
			if (i + 1 < divisoes.size()) {
				Divisao abaixo = divisoes.get(i + 1);
				if (divisao.rebaixados() != abaixo.promovidos()) {
					problemas.add(descrever(divisao) + " rebaixa " + divisao.rebaixados() + " clube(s), mas "
							+ descrever(abaixo) + " promove " + abaixo.promovidos()
							+ "; os números devem ser iguais para as divisões manterem o tamanho.");
				}
			}
		}

		validarClubesUnicos(liga, problemas);

		Divisao ultima = divisoes.getLast();
		if (liga.filaDeEspera().size() < ultima.rebaixados()) {
			int faltam = ultima.rebaixados() - liga.filaDeEspera().size();
			problemas.add("A liga precisa de " + liga.clubesNecessarios() + " clubes (soma das divisões + "
					+ ultima.rebaixados() + " rebaixado(s) da última divisão), mas há "
					+ (liga.clubesNecessarios() - faltam) + ". Faltam " + faltam
					+ " clube(s) na fila de espera para substituir os rebaixados de " + descrever(ultima) + ".");
		}
		return problemas;
	}

	/**
	 * Problemas do formato da liga do molde (SPEC §4.1, rev. 27), incluindo a falta de clubes no
	 * país. Os promovidos não são validados: vêm dos rebaixados da divisão de cima.
	 */
	public static List<String> problemas(FormatoLiga formato, int clubesNoPais) {
		List<String> problemas = new ArrayList<>();
		if (formato.nome() == null || formato.nome().isBlank()) {
			problemas.add("A liga precisa de um nome.");
		}
		List<FormatoDivisao> divisoes = formato.divisoes();
		if (divisoes.isEmpty()) {
			problemas.add("A liga precisa de ao menos uma divisão.");
			return problemas;
		}
		validarQuantidadeDivisoes(divisoes.size(), problemas);
		for (int i = 0; i < divisoes.size(); i++) {
			FormatoDivisao d = divisoes.get(i);
			if (d.prioridade() != i + 1) {
				problemas.add("As prioridades devem ser 1, 2, 3... sem repetir nem pular: " + descrever(d.nome(),
						d.prioridade()) + " deveria ter prioridade " + (i + 1) + ".");
			}
			validarDivisao(d.nome(), d.prioridade(), d.quantidadeClubes(), d.limiteForca(), formato.promovidos(i),
					d.rebaixados(), i == 0, problemas);
		}
		if (clubesNoPais < formato.clubesNecessarios()) {
			problemas.add("A liga precisa de " + formato.clubesNecessarios() + " clubes do país (soma das divisões + "
					+ divisoes.getLast().rebaixados() + " rebaixado(s) da última divisão), mas há " + clubesNoPais
					+ ". Faltam " + (formato.clubesNecessarios() - clubesNoPais) + " clube(s).");
		}
		return problemas;
	}

	/** @throws IllegalArgumentException com todos os problemas, se a liga não puder iniciar */
	public static void exigirValida(ConfiguracaoLiga liga) {
		lancarSeHouver(problemas(liga));
	}

	/** @throws IllegalArgumentException com todos os problemas, se a liga não puder iniciar */
	public static void exigirValido(FormatoLiga formato, int clubesNoPais) {
		lancarSeHouver(problemas(formato, clubesNoPais));
	}

	private static void lancarSeHouver(List<String> problemas) {
		if (!problemas.isEmpty()) {
			throw new IllegalArgumentException("A liga não pode iniciar:\n- " + String.join("\n- ", problemas));
		}
	}

	private static void validarQuantidadeDivisoes(int divisoes, List<String> problemas) {
		if (divisoes > MAXIMO_DIVISOES) {
			problemas.add("A liga tem " + divisoes + " divisões; o máximo é " + MAXIMO_DIVISOES + ".");
		}
	}

	private static void validarDivisao(Divisao divisao, boolean principal, List<String> problemas) {
		validarDivisao(divisao.nome(), divisao.prioridade(), divisao.clubes().size(), divisao.limiteForca(),
				divisao.promovidos(), divisao.rebaixados(), principal, problemas);
	}

	private static void validarDivisao(String nomeDivisao, int prioridade, int clubes, int limiteForca,
			int promovidos, int rebaixados, boolean principal, List<String> problemas) {
		String nome = descrever(nomeDivisao, prioridade);
		if (nomeDivisao == null || nomeDivisao.isBlank()) {
			problemas.add("A divisão de prioridade " + prioridade + " precisa de um nome.");
		}
		if (clubes < MINIMO_CLUBES_POR_DIVISAO || clubes > MAXIMO_CLUBES_POR_DIVISAO) {
			problemas.add(nome + " tem " + clubes + " clubes; cada divisão deve ter de " + MINIMO_CLUBES_POR_DIVISAO
					+ " a " + MAXIMO_CLUBES_POR_DIVISAO + ".");
		}
		if (principal && limiteForca != LIMITE_FORCA_PRINCIPAL) {
			problemas.add(nome + " é a liga principal e deve ter limite de força " + LIMITE_FORCA_PRINCIPAL + ", mas tem "
					+ limiteForca + ".");
		}
		if (limiteForca < Jogador.FORCA_MINIMA || limiteForca > Jogador.FORCA_MAXIMA) {
			problemas.add(nome + " tem limite de força " + limiteForca + "; deve estar entre "
					+ Jogador.FORCA_MINIMA + " e " + Jogador.FORCA_MAXIMA + ".");
		}
		if (principal && promovidos != 0) {
			problemas.add(nome + " é a liga principal e não pode ter promovidos (não há divisão acima).");
		}
		if (promovidos < 0 || rebaixados < 0) {
			problemas.add(nome + " não pode ter promovidos ou rebaixados negativos.");
		}
		if (promovidos + rebaixados > clubes) {
			problemas.add(nome + " promove " + promovidos + " e rebaixa " + rebaixados
					+ ", mais do que os " + clubes + " clubes que tem.");
		}
	}

	private static void validarClubesUnicos(ConfiguracaoLiga liga, List<String> problemas) {
		Set<Long> ids = new HashSet<>();
		List<Clube> todos = new ArrayList<>();
		liga.divisoes().forEach(d -> todos.addAll(d.clubes()));
		todos.addAll(liga.filaDeEspera());
		for (Clube clube : todos) {
			if (!ids.add(clube.id())) {
				problemas.add(clube.nome() + " aparece mais de uma vez (em divisões ou na fila de espera).");
			}
		}
	}

	private static String descrever(Divisao divisao) {
		return descrever(divisao.nome(), divisao.prioridade());
	}

	private static String descrever(String nome, int prioridade) {
		return "\"" + nome + "\" (prioridade " + prioridade + ")";
	}
}
