package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import br.com.managerfoot.dominio.Clube;

/**
 * Resultado do fim de temporada (SPEC §4.1, §8.1): a configuração da próxima temporada e quem
 * subiu, desceu ou ficou sem liga.
 *
 * @param proxima liga da temporada seguinte
 * @param promovidos clubes que subiram de divisão (inclusive os que saíram da fila)
 * @param rebaixados clubes que desceram de divisão (inclusive os que foram para a fila)
 * @param semLiga clubes rebaixados da última divisão para a fila nesta transição; se algum for de
 *        usuário, o usuário é demitido (SPEC §4.1, exceção à §5.4)
 */
public record TransicaoTemporada(ConfiguracaoLiga proxima, List<Clube> promovidos, List<Clube> rebaixados,
		List<Clube> semLiga) {

	public TransicaoTemporada {
		promovidos = List.copyOf(promovidos);
		rebaixados = List.copyOf(rebaixados);
		semLiga = List.copyOf(semLiga);
	}

	/**
	 * Aplica promoções e rebaixamentos pela classificação final de cada divisão.
	 *
	 * @param finais classificação final por prioridade da divisão
	 * @param aleatorio usado no sorteio da fila quando há mais clubes esperando do que vagas
	 * @throws IllegalArgumentException se a liga for inválida ou alguma divisão não tiver terminado
	 */
	public static TransicaoTemporada aplicar(ConfiguracaoLiga atual, Map<Integer, Classificacao> finais,
			RandomGenerator aleatorio) {
		ValidadorLiga.exigirValida(atual);
		Objects.requireNonNull(finais, "classificações finais são obrigatórias");
		List<Divisao> divisoes = atual.divisoes();
		for (Divisao divisao : divisoes) {
			exigirTerminada(divisao, finais.get(divisao.prioridade()));
		}

		List<List<Clube>> sobem = new ArrayList<>();
		List<List<Clube>> descem = new ArrayList<>();
		for (Divisao divisao : divisoes) {
			List<Clube> ordem = finais.get(divisao.prioridade()).linhas().stream().map(LinhaClassificacao::clube).toList();
			sobem.add(ordem.subList(0, divisao.promovidos()));
			descem.add(ordem.subList(ordem.size() - divisao.rebaixados(), ordem.size()));
		}

		// sorteio entre quem já esperava; os rebaixados agora ficam ao menos uma temporada fora
		Divisao ultima = divisoes.getLast();
		List<Clube> candidatosDaFila = new ArrayList<>(atual.filaDeEspera());
		Collections.shuffle(candidatosDaFila, aleatorio);
		List<Clube> sobemDaFila = candidatosDaFila.subList(0, ultima.rebaixados());

		List<Divisao> novas = new ArrayList<>();
		for (int i = 0; i < divisoes.size(); i++) {
			Divisao divisao = divisoes.get(i);
			Set<Clube> saem = new HashSet<>(sobem.get(i));
			saem.addAll(descem.get(i));
			List<Clube> clubes = new ArrayList<>(divisao.clubes().stream().filter(c -> !saem.contains(c)).toList());
			if (i > 0) {
				clubes.addAll(descem.get(i - 1));
			}
			clubes.addAll(i + 1 < divisoes.size() ? sobem.get(i + 1) : sobemDaFila);
			novas.add(new Divisao(divisao.nome(), divisao.prioridade(), divisao.limiteForca(), divisao.promovidos(),
					divisao.rebaixados(), clubes));
		}

		List<Clube> novaFila = new ArrayList<>(candidatosDaFila.subList(ultima.rebaixados(), candidatosDaFila.size()));
		List<Clube> semLiga = descem.getLast();
		novaFila.addAll(semLiga);
		// a fila mantém a ordem original de cadastro, não a do sorteio
		List<Clube> filaOrdenada = new ArrayList<>(atual.filaDeEspera().stream().filter(novaFila::contains).toList());
		filaOrdenada.addAll(semLiga);

		List<Clube> promovidos = new ArrayList<>();
		sobem.forEach(promovidos::addAll);
		promovidos.addAll(sobemDaFila);
		List<Clube> rebaixados = new ArrayList<>();
		descem.forEach(rebaixados::addAll);

		ConfiguracaoLiga proxima = new ConfiguracaoLiga(atual.nome(), novas, filaOrdenada);
		ValidadorLiga.exigirValida(proxima);
		return new TransicaoTemporada(proxima, promovidos, rebaixados, semLiga);
	}

	/** A temporada só acaba com todos os jogos de turno e returno disputados (SPEC §8.1). */
	private static void exigirTerminada(Divisao divisao, Classificacao classificacao) {
		String nome = "\"" + divisao.nome() + "\"";
		if (classificacao == null) {
			throw new IllegalArgumentException("falta a classificação final de " + nome);
		}
		Set<Long> esperados = divisao.clubes().stream().map(Clube::id).collect(Collectors.toSet());
		Set<Long> presentes = classificacao.linhas().stream().map(l -> l.clube().id()).collect(Collectors.toSet());
		if (!esperados.equals(presentes)) {
			throw new IllegalArgumentException("a classificação de " + nome + " não corresponde aos clubes da divisão");
		}
		int jogosPorClube = 2 * (divisao.clubes().size() - 1);
		List<String> incompletos = classificacao.linhas().stream()
			.filter(l -> l.jogos() != jogosPorClube)
			.map(l -> l.clube().nome() + " (" + l.jogos() + ")")
			.toList();
		if (!incompletos.isEmpty()) {
			throw new IllegalArgumentException(nome + " ainda não terminou: cada clube deve ter " + jogosPorClube
					+ " jogos, mas " + String.join(", ", incompletos));
		}
	}
}
