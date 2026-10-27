package br.com.managerfoot.motor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;

/**
 * Situação de um time durante a partida: quem segue em campo, quem está no gol e os amarelos
 * recebidos. Parte da escalação e muda com as expulsões (SPEC §7.3).
 */
final class TimeEmCampo {

	private final Escalacao escalacao;
	private final double fatorGoleiroImprovisado;
	private final List<Jogador> linha = new ArrayList<>();
	private final Map<Long, Integer> amarelos = new HashMap<>();
	private Jogador goleiro;
	private boolean goleiroImprovisado;

	TimeEmCampo(Escalacao escalacao, double fatorGoleiroImprovisado) {
		this.escalacao = escalacao;
		this.fatorGoleiroImprovisado = fatorGoleiroImprovisado;
		this.goleiro = escalacao.jogadoresNa(Posicao.GOLEIRO).getFirst();
		escalacao.titulares().stream().filter(j -> j != goleiro).forEach(linha::add);
	}

	Escalacao escalacao() {
		return escalacao;
	}

	/** Jogadores atuando no setor; o goleiro improvisado deixa de contar no setor de origem. */
	List<Jogador> jogadoresNa(Posicao posicao) {
		if (posicao == Posicao.GOLEIRO) {
			return List.of(goleiro);
		}
		return linha.stream().filter(j -> j.posicao() == posicao).toList();
	}

	List<Jogador> emCampo() {
		List<Jogador> todos = new ArrayList<>(linha);
		todos.add(goleiro);
		return todos;
	}

	Jogador goleiro() {
		return goleiro;
	}

	double forcaGoleiro() {
		return goleiroImprovisado ? goleiro.forca() * fatorGoleiroImprovisado : goleiro.forca();
	}

	/** Soma das forças do setor, usando a força efetiva do goleiro. */
	double soma(Posicao posicao) {
		if (posicao == Posicao.GOLEIRO) {
			return forcaGoleiro();
		}
		return jogadoresNa(posicao).stream().mapToInt(Jogador::forca).sum();
	}

	int amarelos(Jogador jogador) {
		return amarelos.getOrDefault(jogador.id(), 0);
	}

	void registrarAmarelo(Jogador jogador) {
		amarelos.merge(jogador.id(), 1, Integer::sum);
	}

	/**
	 * Tira o jogador de campo. Se for o goleiro, o defensor de menor força assume o gol; sem
	 * defensores, o jogador de linha de menor força (SPEC §7.3).
	 */
	void expulsar(Jogador jogador) {
		if (jogador != goleiro) {
			linha.remove(jogador);
			return;
		}
		Comparator<Jogador> menorForca = Comparator.comparingInt(Jogador::forca);
		Jogador substituto = linha.stream()
			.filter(j -> j.posicao() == Posicao.DEFENSOR)
			.min(menorForca)
			.orElseGet(() -> linha.stream().min(menorForca).orElseThrow());
		linha.remove(substituto);
		goleiro = substituto;
		goleiroImprovisado = true;
	}

	/**
	 * Batedor de pênalti (SPEC §12): o escolhido pelo treinador, se estiver em campo; senão o
	 * jogador de linha de maior força, com desempate por asterisco e, depois, sorteio.
	 */
	Jogador batedorPenalti(RandomGenerator aleatorio) {
		var escolhido = escalacao.batedorPreferido().filter(emCampo()::contains);
		if (escolhido.isPresent()) {
			return escolhido.get();
		}
		if (linha.isEmpty()) {
			return goleiro;
		}
		int maiorForca = linha.stream().mapToInt(Jogador::forca).max().orElseThrow();
		List<Jogador> candidatos = linha.stream().filter(j -> j.forca() == maiorForca).toList();
		if (candidatos.stream().anyMatch(Jogador::asterisco)) {
			candidatos = candidatos.stream().filter(Jogador::asterisco).toList();
		}
		return candidatos.get(aleatorio.nextInt(candidatos.size()));
	}
}
