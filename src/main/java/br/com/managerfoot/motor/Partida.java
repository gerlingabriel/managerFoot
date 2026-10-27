package br.com.managerfoot.motor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;

/**
 * Uma partida em andamento, avançada minuto a minuto (SPEC §7, decisão 0004: o servidor
 * controla o relógio). A cada minuto pode haver uma finalização:
 * <ol>
 * <li>meio contra meio decide qual time domina a jogada (o mandante recebe {@code fatorMando});</li>
 * <li>o finalizador sai de um setor conforme a formação e, dentro dele, com peso pela força;</li>
 * <li>ataque contra defesa+goleiro e finalizador contra goleiro decidem se vira gol.</li>
 * </ol>
 * Toda aleatoriedade vem do {@link RandomGenerator} recebido, para a partida ser reproduzível.
 */
public final class Partida {

	private final ParametrosMotor parametros;
	private final Escalacao casa;
	private final Escalacao fora;
	private final RandomGenerator aleatorio;
	private final List<EventoPartida> eventos = new ArrayList<>();
	private int minuto;
	private int golsCasa;
	private int golsFora;

	public Partida(ParametrosMotor parametros, Escalacao casa, Escalacao fora, RandomGenerator aleatorio) {
		this.parametros = Objects.requireNonNull(parametros);
		this.casa = Objects.requireNonNull(casa);
		this.fora = Objects.requireNonNull(fora);
		this.aleatorio = Objects.requireNonNull(aleatorio);
	}

	/** Avança um minuto e devolve os eventos ocorridos nele. */
	public List<EventoPartida> avancarMinuto() {
		if (terminada()) {
			throw new IllegalStateException("a partida já terminou no minuto " + minuto);
		}
		minuto++;
		int antes = eventos.size();
		if (aleatorio.nextDouble() < parametros.chancePorMinuto()) {
			finalizar(sortearQuemDomina());
		}
		return List.copyOf(eventos.subList(antes, eventos.size()));
	}

	public boolean terminada() {
		return minuto >= parametros.minutos();
	}

	public int minuto() {
		return minuto;
	}

	public ResultadoPartida resultado() {
		return new ResultadoPartida(golsCasa, golsFora, eventos);
	}

	private Lado sortearQuemDomina() {
		double meioCasa = potencia(soma(casa, Posicao.MEIO_CAMPISTA)) * parametros.fatorMando();
		double meioFora = potencia(soma(fora, Posicao.MEIO_CAMPISTA));
		return aleatorio.nextDouble() * (meioCasa + meioFora) < meioCasa ? Lado.CASA : Lado.FORA;
	}

	private void finalizar(Lado lado) {
		Escalacao ataque = escalacao(lado);
		Escalacao defesa = escalacao(lado.adversario());
		Jogador finalizador = sortearFinalizador(ataque);
		Jogador goleiro = defesa.jogadoresNa(Posicao.GOLEIRO).getFirst();

		double pressao = disputa(soma(ataque, Posicao.ATACANTE),
				soma(defesa, Posicao.DEFENSOR) + soma(defesa, Posicao.GOLEIRO));
		double duelo = disputa(finalizador.forca(), goleiro.forca());
		double antiGoleada = Math.pow(parametros.fatorPorGolMarcado(), gols(lado));
		double conversao = Math.clamp(parametros.conversaoBase() * pressao * duelo * antiGoleada,
				parametros.conversaoMinima(), parametros.conversaoMaxima());

		TipoEvento tipo;
		if (aleatorio.nextDouble() < conversao) {
			tipo = TipoEvento.GOL;
			if (lado == Lado.CASA) {
				golsCasa++;
			} else {
				golsFora++;
			}
		} else {
			double desfecho = aleatorio.nextDouble();
			if (desfecho < parametros.proporcaoDefesa()) {
				tipo = TipoEvento.DEFESA;
			} else if (desfecho < parametros.proporcaoDefesa() + parametros.proporcaoTrave()) {
				tipo = TipoEvento.TRAVE;
			} else {
				tipo = TipoEvento.FORA;
			}
		}
		eventos.add(new EventoPartida(minuto, lado, tipo, finalizador));
	}

	private Jogador sortearFinalizador(Escalacao escalacao) {
		PesosFinalizador pesos = parametros.pesosFinalizador().get(escalacao.formacao());
		// setores sem jogadores ficam de fora do sorteio (necessário quando houver expulsões)
		List<Posicao> setores = Arrays.stream(Posicao.values())
			.filter(p -> !escalacao.jogadoresNa(p).isEmpty())
			.toList();
		Posicao setor = Sorteio.ponderado(setores, pesos::peso, aleatorio);
		return Sorteio.ponderado(escalacao.jogadoresNa(setor), Jogador::forca, aleatorio);
	}

	/** Fator em torno de 1: vale 1 quando os lados se equivalem, até 2 quando o primeiro domina. */
	private double disputa(double a, double b) {
		double pa = potencia(a);
		return 2 * pa / (pa + potencia(b));
	}

	private double potencia(double forca) {
		return Math.pow(forca, parametros.sensibilidadeForca());
	}

	private static int soma(Escalacao escalacao, Posicao posicao) {
		return escalacao.jogadoresNa(posicao).stream().mapToInt(Jogador::forca).sum();
	}

	private Escalacao escalacao(Lado lado) {
		return lado == Lado.CASA ? casa : fora;
	}

	private int gols(Lado lado) {
		return lado == Lado.CASA ? golsCasa : golsFora;
	}
}
