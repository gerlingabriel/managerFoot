package br.com.managerfoot.motor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

/**
 * Uma partida em andamento, avançada minuto a minuto (SPEC §7, decisão 0004: o servidor
 * controla o relógio). A cada minuto:
 * <ol>
 * <li>pode haver um pênalti ou uma finalização. Meio contra meio decide qual time tem a jogada (o
 * mandante recebe {@code fatorMando}); o finalizador sai de um setor conforme a formação e, dentro
 * dele, com peso pela força; ataque contra defesa+goleiro e finalizador contra goleiro decidem o
 * gol;</li>
 * <li>cada time pode receber um cartão, com o infrator sorteado pela roleta de temperamento.</li>
 * </ol>
 * Toda aleatoriedade vem do {@link RandomGenerator} recebido, para a partida ser reproduzível.
 */
public final class Partida {

	/** Sem jogadores suficientes, o time deixa de receber cartões (proteção técnica, não regra do jogo). */
	private static final int MINIMO_EM_CAMPO_PARA_CARTAO = 2;

	private final ParametrosMotor parametros;
	private final TimeEmCampo casa;
	private final TimeEmCampo fora;
	private final RandomGenerator aleatorio;
	private final List<EventoPartida> eventos = new ArrayList<>();
	private int minuto;
	private int golsCasa;
	private int golsFora;

	public Partida(ParametrosMotor parametros, Escalacao casa, Escalacao fora, RandomGenerator aleatorio) {
		this.parametros = Objects.requireNonNull(parametros);
		double fatorImprovisado = parametros.penalti().fatorGoleiroImprovisado();
		this.casa = new TimeEmCampo(Objects.requireNonNull(casa), fatorImprovisado);
		this.fora = new TimeEmCampo(Objects.requireNonNull(fora), fatorImprovisado);
		this.aleatorio = Objects.requireNonNull(aleatorio);
	}

	/** Avança um minuto e devolve os eventos ocorridos nele. */
	public List<EventoPartida> avancarMinuto() {
		if (terminada()) {
			throw new IllegalStateException("a partida já terminou no minuto " + minuto);
		}
		minuto++;
		int antes = eventos.size();
		if (aleatorio.nextDouble() < parametros.penalti().penaltisPorMinuto()) {
			cobrarPenalti(sortearQuemDomina());
		} else if (aleatorio.nextDouble() < parametros.chancePorMinuto()) {
			finalizar(sortearQuemDomina());
		}
		verificarCartao(Lado.CASA);
		verificarCartao(Lado.FORA);
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
		double meioCasa = potencia(casa.soma(Posicao.MEIO_CAMPISTA)) * parametros.fatorMando();
		double meioFora = potencia(fora.soma(Posicao.MEIO_CAMPISTA));
		if (meioCasa + meioFora == 0) {
			return aleatorio.nextBoolean() ? Lado.CASA : Lado.FORA;
		}
		return aleatorio.nextDouble() * (meioCasa + meioFora) < meioCasa ? Lado.CASA : Lado.FORA;
	}

	private void finalizar(Lado lado) {
		TimeEmCampo ataque = time(lado);
		TimeEmCampo defesa = time(lado.adversario());
		Jogador finalizador = sortearFinalizador(ataque);

		double pressao = disputa(ataque.soma(Posicao.ATACANTE),
				defesa.soma(Posicao.DEFENSOR) + defesa.soma(Posicao.GOLEIRO));
		double duelo = disputa(finalizador.forca(), defesa.forcaGoleiro());
		double antiGoleada = Math.pow(parametros.fatorPorGolMarcado(), gols(lado));
		double conversao = Math.clamp(parametros.conversaoBase() * pressao * duelo * antiGoleada,
				parametros.conversaoMinima(), parametros.conversaoMaxima());

		registrarFinalizacao(lado, finalizador, conversao, false);
	}

	private void cobrarPenalti(Lado lado) {
		Jogador batedor = time(lado).batedorPenalti(aleatorio);
		registrarFinalizacao(lado, batedor, parametros.penalti().conversao(), true);
	}

	private void registrarFinalizacao(Lado lado, Jogador finalizador, double conversao, boolean penalti) {
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
		eventos.add(new EventoPartida(minuto, lado, tipo, finalizador, penalti));
	}

	/**
	 * A chance de cartão cresce levemente com a soma dos pesos de temperamento do time em campo,
	 * comparada a um time completo só de jogadores "normais" (SPEC §7.4).
	 */
	private void verificarCartao(Lado lado) {
		TimeEmCampo time = time(lado);
		List<Jogador> emCampo = time.emCampo();
		if (emCampo.size() < MINIMO_EM_CAMPO_PARA_CARTAO) {
			return;
		}
		ParametrosDisciplina disciplina = parametros.disciplina();
		double somaPesos = emCampo.stream().mapToDouble(j -> disciplina.peso(j.temperamento())).sum();
		double referencia = disciplina.peso(Temperamento.NORMAL) * 11;
		double chance = disciplina.cartoesPorMinuto()
				* Math.pow(somaPesos / referencia, disciplina.sensibilidadeTemperamento());
		if (aleatorio.nextDouble() >= chance) {
			return;
		}

		Jogador infrator = Sorteio.ponderado(emCampo, j -> disciplina.peso(j.temperamento()), aleatorio);
		TipoEvento tipo;
		if (aleatorio.nextDouble() < disciplina.proporcaoVermelhoDireto()) {
			tipo = TipoEvento.CARTAO_VERMELHO;
		} else if (time.amarelos(infrator) >= 1) {
			tipo = TipoEvento.SEGUNDO_AMARELO;
		} else {
			tipo = TipoEvento.CARTAO_AMARELO;
		}
		if (tipo != TipoEvento.CARTAO_VERMELHO) {
			time.registrarAmarelo(infrator);
		}
		if (tipo.expulsao()) {
			time.expulsar(infrator);
		}
		eventos.add(new EventoPartida(minuto, lado, tipo, infrator));
	}

	private Jogador sortearFinalizador(TimeEmCampo time) {
		PesosFinalizador pesos = parametros.pesosFinalizador().get(time.escalacao().formacao());
		// setores sem jogadores (após expulsões) ficam de fora do sorteio
		List<Posicao> setores = Arrays.stream(Posicao.values())
			.filter(p -> !time.jogadoresNa(p).isEmpty() && pesos.peso(p) > 0)
			.toList();
		Posicao setor = Sorteio.ponderado(setores, pesos::peso, aleatorio);
		return Sorteio.ponderado(time.jogadoresNa(setor), Jogador::forca, aleatorio);
	}

	/** Fator em torno de 1: vale 1 quando os lados se equivalem, até 2 quando o primeiro domina. */
	private double disputa(double a, double b) {
		double pa = potencia(a);
		double pb = potencia(b);
		return pa + pb == 0 ? 1 : 2 * pa / (pa + pb);
	}

	private double potencia(double forca) {
		return Math.pow(forca, parametros.sensibilidadeForca());
	}

	private TimeEmCampo time(Lado lado) {
		return lado == Lado.CASA ? casa : fora;
	}

	private int gols(Lado lado) {
		return lado == Lado.CASA ? golsCasa : golsFora;
	}
}
