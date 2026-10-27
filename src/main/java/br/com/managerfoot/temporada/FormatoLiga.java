package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Clube;

/**
 * Formato da liga de um país no molde (SPEC §4.1, rev. 27): só as divisões, sem clubes. Os clubes
 * pertencem ao país e são distribuídos pela força quando o jogo é criado. Pode estar incompleto
 * enquanto é editado; {@link ValidadorLiga} diz se pode iniciar.
 *
 * @param divisoes em ordem de prioridade
 */
public record FormatoLiga(String nome, List<FormatoDivisao> divisoes) {

	public FormatoLiga {
		Objects.requireNonNull(divisoes, "lista de divisões é obrigatória");
		divisoes = divisoes.stream().sorted(Comparator.comparingInt(FormatoDivisao::prioridade)).toList();
	}

	/** Promovidos da divisão: os rebaixados da divisão de cima; a principal não tem. */
	public int promovidos(int indice) {
		return indice == 0 ? 0 : divisoes.get(indice - 1).rebaixados();
	}

	/** Clubes necessários: soma das divisões + rebaixados da última (SPEC §4.1). */
	public int clubesNecessarios() {
		int soma = divisoes.stream().mapToInt(FormatoDivisao::quantidadeClubes).sum();
		return soma + (divisoes.isEmpty() ? 0 : divisoes.getLast().rebaixados());
	}

	/**
	 * Distribui os clubes do país pela força, do mais forte ao mais fraco: os primeiros preenchem a
	 * divisão principal, os seguintes as de baixo, e o resto vai para a fila. Empate de força é
	 * decidido por sorteio. As forças não mudam (rev. 26). Divisões e fila saem em ordem de id.
	 *
	 * @throws IllegalArgumentException se o formato não puder iniciar com esses clubes
	 */
	public ConfiguracaoLiga distribuir(List<Clube> clubesDoPais, RandomGenerator aleatorio) {
		ValidadorLiga.exigirValido(this, clubesDoPais.size());
		List<Clube> ordem = new ArrayList<>(clubesDoPais);
		ordem.sort(Comparator.comparingLong(Clube::id));
		embaralhar(ordem, aleatorio);
		// ordenação estável: entre clubes de mesma força vale a ordem embaralhada
		ordem.sort(Comparator.comparingInt(Clube::forca).reversed());

		List<Divisao> distribuidas = new ArrayList<>();
		int inicio = 0;
		for (int i = 0; i < divisoes.size(); i++) {
			FormatoDivisao d = divisoes.get(i);
			List<Clube> clubes = porId(ordem.subList(inicio, inicio + d.quantidadeClubes()));
			distribuidas.add(new Divisao(d.nome(), d.prioridade(), d.limiteForca(),
					promovidos(i), d.rebaixados(), clubes));
			inicio += d.quantidadeClubes();
		}
		return new ConfiguracaoLiga(nome, distribuidas, porId(ordem.subList(inicio, ordem.size())));
	}

	/** Fisher-Yates com o gerador recebido, para o sorteio ser reproduzível pela semente. */
	private static void embaralhar(List<Clube> clubes, RandomGenerator aleatorio) {
		for (int i = clubes.size() - 1; i > 0; i--) {
			Collections.swap(clubes, i, aleatorio.nextInt(i + 1));
		}
	}

	private static List<Clube> porId(List<Clube> clubes) {
		return clubes.stream().sorted(Comparator.comparingLong(Clube::id)).toList();
	}
}
