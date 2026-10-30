package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;

/**
 * Competição continental da temporada (SPEC §8.3.6, rev. 72 e 73): fase de grupos e, depois, o
 * mata-mata da copa (§8.2) a partir das oitavas. Domínio puro: quem chama joga as partidas e
 * registra os placares.
 *
 * @param grupos na ordem A, B, C...
 * @param mataMata criado quando os grupos terminam
 */
public record Continental(List<GrupoContinental> grupos, Optional<Copa> mataMata) {

	/** Uma partida da fase de grupos a jogar: a rodada do {@link Jogo} é a da liga depois da qual se joga. */
	public record PartidaDeGrupo(GrupoContinental grupo, int numero, Jogo jogo) {
	}

	/** Limite de tentativas do sorteio sem dois clubes do mesmo país no grupo. */
	private static final int TENTATIVAS_SEM_REPETIR_PAIS = 200_000;

	public Continental {
		grupos = List.copyOf(grupos);
		Objects.requireNonNull(mataMata);
		if (grupos.isEmpty()) {
			throw new IllegalArgumentException("a competição continental precisa de ao menos um grupo");
		}
	}

	/**
	 * Sorteia os grupos em potes pela força em campo (rev. 72): o pote 1 tem os mais fortes, e cada
	 * grupo recebe um clube de cada pote. Evita dois clubes do mesmo país no grupo; se não houver
	 * jeito, aceita o mínimo de repetições.
	 *
	 * @param inscritos exatamente {@link ParametrosContinental#clubes()}; com menos, a edição não é
	 *        realizada (§8.3.2 item 5)
	 * @param paises código do país de cada clube, por id
	 * @param planteis jogadores de cada clube, por id
	 */
	public static Continental sortear(List<Clube> inscritos, Map<Long, String> paises, Map<Long, List<Jogador>> planteis,
			ParametrosContinental parametros, RandomGenerator aleatorio) {
		if (inscritos.size() != parametros.clubes()) {
			throw new IllegalArgumentException("a competição continental tem " + parametros.clubes() + " clubes, mas há "
					+ inscritos.size() + " inscritos");
		}
		if (inscritos.stream().distinct().count() != inscritos.size()) {
			throw new IllegalArgumentException("há clubes repetidos entre os inscritos da competição continental");
		}
		inscritos.forEach(c -> {
			if (!paises.containsKey(c.id())) {
				throw new IllegalArgumentException(c.nome() + " está sem país");
			}
		});
		List<Clube> ordem = new ArrayList<>(inscritos);
		Collections.shuffle(ordem, aleatorio);
		ordem.sort(Comparator.comparingDouble(
				(Clube c) -> Clube.forcaEmCampo(planteis.getOrDefault(c.id(), List.of()))).reversed());

		int quantidade = parametros.grupos();
		List<List<Clube>> potes = new ArrayList<>();
		for (int p = 0; p < parametros.clubesPorGrupo(); p++) {
			List<Clube> pote = new ArrayList<>(ordem.subList(p * quantidade, (p + 1) * quantidade));
			Collections.shuffle(pote, aleatorio);
			potes.add(pote);
		}
		List<List<Clube>> montagem = new ArrayList<>();
		for (int g = 0; g < quantidade; g++) {
			montagem.add(new ArrayList<>());
		}
		int[] passos = { 0 };
		if (!distribuir(potes, 0, 0, montagem, paises, passos, aleatorio)) {
			montagem.forEach(List::clear);
			distribuirComRepeticao(potes, montagem, paises, aleatorio);
		}
		List<GrupoContinental> grupos = new ArrayList<>();
		for (int g = 0; g < quantidade; g++) {
			grupos.add(GrupoContinental.novo(g, montagem.get(g)));
		}
		return new Continental(grupos, Optional.empty());
	}

	/** Busca com retrocesso: coloca o clube {@code k} do pote {@code p} num grupo sem o país dele. */
	private static boolean distribuir(List<List<Clube>> potes, int p, int k, List<List<Clube>> grupos,
			Map<Long, String> paises, int[] passos, RandomGenerator aleatorio) {
		if (p == potes.size()) {
			return true;
		}
		if (k == potes.get(p).size()) {
			return distribuir(potes, p + 1, 0, grupos, paises, passos, aleatorio);
		}
		if (++passos[0] > TENTATIVAS_SEM_REPETIR_PAIS) {
			return false;
		}
		Clube clube = potes.get(p).get(k);
		String pais = paises.get(clube.id());
		List<Integer> livres = new ArrayList<>();
		for (int g = 0; g < grupos.size(); g++) {
			List<Clube> grupo = grupos.get(g);
			if (grupo.size() == p && grupo.stream().noneMatch(c -> paises.get(c.id()).equals(pais))) {
				livres.add(g);
			}
		}
		Collections.shuffle(livres, aleatorio);
		for (int g : livres) {
			grupos.get(g).add(clube);
			if (distribuir(potes, p, k + 1, grupos, paises, passos, aleatorio)) {
				return true;
			}
			grupos.get(g).removeLast();
		}
		return false;
	}

	/** Sem saída limpa: cada clube vai ao grupo livre com menos clubes do país dele. */
	private static void distribuirComRepeticao(List<List<Clube>> potes, List<List<Clube>> grupos,
			Map<Long, String> paises, RandomGenerator aleatorio) {
		for (int p = 0; p < potes.size(); p++) {
			for (Clube clube : potes.get(p)) {
				String pais = paises.get(clube.id());
				int pote = p;
				List<Integer> livres = new ArrayList<>();
				for (int g = 0; g < grupos.size(); g++) {
					if (grupos.get(g).size() == pote) {
						livres.add(g);
					}
				}
				Collections.shuffle(livres, aleatorio);
				int escolhido = livres.stream()
					.min(Comparator.comparingLong(g -> grupos.get(g).stream().filter(c -> paises.get(c.id()).equals(pais)).count()))
					.orElseThrow();
				grupos.get(escolhido).add(clube);
			}
		}
	}

	/**
	 * Partidas ainda não jogadas do jogo {@code numero} dos grupos (de 1 a 6).
	 *
	 * @param depoisDaRodada a rodada da liga depois da qual elas se jogam
	 */
	public List<PartidaDeGrupo> partidasDosGrupos(int numero, int depoisDaRodada) {
		List<PartidaDeGrupo> partidas = new ArrayList<>();
		for (GrupoContinental grupo : grupos) {
			grupo.partidasDoJogo(numero).stream()
				.filter(p -> p.placar().isEmpty())
				.forEach(p -> partidas.add(new PartidaDeGrupo(grupo, numero, new Jogo(depoisDaRodada, p.mandante(), p.visitante()))));
		}
		return partidas;
	}

	public Continental comPlacarDoGrupo(int grupo, int numero, Clube mandante, GrupoContinental.Placar placar) {
		if (mataMata.isPresent()) {
			throw new IllegalStateException("a fase de grupos já terminou");
		}
		List<GrupoContinental> novos = new ArrayList<>(grupos);
		novos.set(grupo, grupos.get(grupo).comPlacar(numero, mandante, placar));
		return new Continental(novos, mataMata);
	}

	public boolean gruposEncerrados() {
		return grupos.stream().allMatch(GrupoContinental::encerrado);
	}

	/**
	 * Cria as oitavas (rev. 72): cada 1º pega por sorteio um 2º de outro grupo e joga a volta em casa,
	 * ou seja, o 2º é o mandante da ida. Depois, a chave fica fixa como a da copa.
	 *
	 * @param sementeSorteio a do desempate dos grupos
	 */
	public Continental iniciarMataMata(long sementeSorteio, RandomGenerator aleatorio) {
		if (mataMata.isPresent()) {
			throw new IllegalStateException("o mata-mata já começou");
		}
		if (!gruposEncerrados()) {
			throw new IllegalStateException("a fase de grupos ainda não terminou");
		}
		List<Clube> primeiros = grupos.stream().map(g -> g.classificados(sementeSorteio).get(0)).toList();
		List<Clube> segundos = grupos.stream().map(g -> g.classificados(sementeSorteio).get(1)).toList();
		List<Integer> adversario = desarranjo(grupos.size(), aleatorio);
		FaseCopa fase = FaseCopa.comClubes(2 * grupos.size());
		List<ConfrontoCopa> confrontos = new ArrayList<>();
		for (int g = 0; g < grupos.size(); g++) {
			confrontos.add(ConfrontoCopa.novo(fase, g, segundos.get(adversario.get(g)), primeiros.get(g)));
		}
		return new Continental(grupos, Optional.of(new Copa(confrontos)));
	}

	/** Uma permutação sem ponto fixo: ninguém pega o 2º do próprio grupo. */
	private static List<Integer> desarranjo(int n, RandomGenerator aleatorio) {
		if (n < 2) {
			throw new IllegalArgumentException("o mata-mata precisa de ao menos 2 grupos");
		}
		List<Integer> ordem = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			ordem.add(i);
		}
		boolean valido;
		do {
			Collections.shuffle(ordem, aleatorio);
			valido = true;
			for (int i = 0; i < n; i++) {
				valido &= ordem.get(i) != i;
			}
		} while (!valido);
		return List.copyOf(ordem);
	}

	/** Troca o mata-mata depois de registrar um placar ou avançar de fase nele. */
	public Continental comMataMata(Copa novo) {
		if (mataMata.isEmpty()) {
			throw new IllegalStateException("o mata-mata ainda não começou");
		}
		return new Continental(grupos, Optional.of(novo));
	}

	public Optional<Clube> campeao() {
		return mataMata.flatMap(Copa::campeao);
	}

	public Optional<Clube> vice() {
		return mataMata.flatMap(Copa::vice);
	}

	/** O clube está nos grupos desta edição. */
	public boolean participa(Clube clube) {
		return grupos.stream().anyMatch(g -> g.clubes().contains(clube));
	}
}
