package br.com.managerfoot.motor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;

/**
 * Situação de um time durante a partida: quem está em campo, quem está no gol, o banco
 * disponível, a formação atual e os amarelos. Parte da escalação e muda com expulsões
 * (SPEC §7.3) e substituições (SPEC §7.4.1).
 *
 * <p>A formação representa as vagas da tática e não muda durante a partida. Expulsão deixa o buraco;
 * na substituição, quem entra ocupa a vaga de quem saiu e conta naquele setor, com a própria força,
 * qualquer que seja a posição dele (SPEC §7.4.1, rev. 81).
 */
final class TimeEmCampo {

	private final Escalacao escalacao;
	private final double fatorGoleiroImprovisado;
	private final int limiteSubstituicoes;
	private final List<Jogador> linha = new ArrayList<>();
	private final List<Jogador> banco;
	private final Map<Long, Integer> amarelos = new HashMap<>();
	private final Map<Long, Posicao> setor = new HashMap<>();
	private final Formacao formacao;
	private Jogador goleiro;
	private boolean goleiroImprovisado;
	private int substituicoesFeitas;

	TimeEmCampo(Escalacao escalacao, double fatorGoleiroImprovisado, int limiteSubstituicoes) {
		this.escalacao = escalacao;
		this.fatorGoleiroImprovisado = fatorGoleiroImprovisado;
		this.limiteSubstituicoes = limiteSubstituicoes;
		this.formacao = escalacao.formacao();
		this.goleiro = escalacao.jogadoresNa(Posicao.GOLEIRO).getFirst();
		this.banco = new ArrayList<>(escalacao.reservas());
		escalacao.titulares().stream().filter(j -> j != goleiro).forEach(j -> {
			linha.add(j);
			setor.put(j.id(), j.posicao());
		});
	}

	Escalacao escalacao() {
		return escalacao;
	}

	Formacao formacao() {
		return formacao;
	}

	/** Jogadores atuando no setor; o goleiro improvisado deixa de contar no setor de origem. */
	List<Jogador> jogadoresNa(Posicao posicao) {
		if (posicao == Posicao.GOLEIRO) {
			return List.of(goleiro);
		}
		return linha.stream().filter(j -> setor.get(j.id()) == posicao).toList();
	}

	List<Jogador> emCampo() {
		List<Jogador> todos = new ArrayList<>(linha);
		todos.add(goleiro);
		return todos;
	}

	/** Reservas que ainda podem entrar; quem já entrou sai desta lista. */
	List<Jogador> reservasDisponiveis() {
		return List.copyOf(banco);
	}

	int substituicoesRestantes() {
		return limiteSubstituicoes - substituicoesFeitas;
	}

	Jogador goleiro() {
		return goleiro;
	}

	boolean goleiroImprovisado() {
		return goleiroImprovisado;
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
	 * Tira o jogador de campo, expulso ou lesionado sem substituto. Se for o goleiro, o defensor de menor força assume o gol; sem
	 * defensores, o jogador de linha de menor força (SPEC §7.3).
	 */
	void expulsar(Jogador jogador) {
		if (jogador != goleiro) {
			linha.remove(jogador);
			return;
		}
		Comparator<Jogador> menorForca = Comparator.comparingInt(Jogador::forca);
		Jogador substituto = linha.stream()
			.filter(j -> setor.get(j.id()) == Posicao.DEFENSOR)
			.min(menorForca)
			.orElseGet(() -> linha.stream().min(menorForca).orElseThrow());
		linha.remove(substituto);
		goleiro = substituto;
		goleiroImprovisado = true;
	}

	/**
	 * Troca {@code sai} (em campo) por {@code entra} (do banco), conforme SPEC §7.4.1.
	 * <ul>
	 * <li>Goleiro reserva só entra no gol: troca o goleiro ou, se houver goleiro improvisado,
	 * entra no lugar de um jogador de linha e o improvisado volta ao seu setor.</li>
	 * <li>Jogador de linha entra no lugar de jogador de linha e fica no setor dele; a formação não
	 * muda.</li>
	 * </ul>
	 *
	 * @throws IllegalArgumentException se a troca não for permitida, com o motivo
	 */
	void substituir(Jogador sai, Jogador entra) {
		if (substituicoesRestantes() <= 0) {
			throw new IllegalArgumentException("sem substituições restantes (limite de " + limiteSubstituicoes + ")");
		}
		if (!banco.contains(entra)) {
			throw new IllegalArgumentException(entra.nome() + " não está disponível no banco");
		}
		if (!emCampo().contains(sai)) {
			throw new IllegalArgumentException(sai.nome() + " não está em campo");
		}

		if (entra.posicao() == Posicao.GOLEIRO) {
			substituirComGoleiroReserva(sai, entra);
		} else {
			substituirJogadorDeLinha(sai, entra);
		}
		banco.remove(entra);
		substituicoesFeitas++;
	}

	private void substituirComGoleiroReserva(Jogador sai, Jogador entra) {
		if (!goleiroImprovisado) {
			if (sai != goleiro) {
				throw new IllegalArgumentException("goleiro reserva só pode entrar no lugar do goleiro " + goleiro.nome());
			}
			goleiro = entra;
			return;
		}
		// goleiro improvisado volta ao seu setor; a vaga de quem sai fica vazia (formação não muda)
		Jogador improvisado = goleiro;
		goleiro = entra;
		goleiroImprovisado = false;
		if (sai != improvisado) {
			linha.remove(sai);
			linha.add(improvisado);
		}
	}

	private void substituirJogadorDeLinha(Jogador sai, Jogador entra) {
		if (sai == goleiro) {
			throw new IllegalArgumentException(
					"só um goleiro pode entrar no lugar de " + sai.nome() + ", que está no gol");
		}
		linha.set(linha.indexOf(sai), entra);
		setor.put(entra.id(), setor.get(sai.id()));
	}

	/** Se o jogador pode entrar no lugar de outro de linha: o goleiro não entra aqui. */
	boolean trocaDeLinhaPermitida(Jogador sai, Jogador entra) {
		return sai != goleiro && entra.posicao() != Posicao.GOLEIRO;
	}

	/**
	 * Batedor de pênalti (SPEC §12): o escolhido pelo treinador, se estiver em campo; senão o
	 * jogador de linha de maior força, com desempate por asterisco e, depois, sorteio.
	 */
	/** O batedor escolhido pelo treinador, se está em campo. */
	Optional<Jogador> batedorPreferidoEmCampo() {
		return escalacao.batedorPreferido().filter(emCampo()::contains);
	}

	Jogador batedorPenalti(RandomGenerator aleatorio) {
		var escolhido = batedorPreferidoEmCampo();
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
