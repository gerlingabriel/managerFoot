package br.com.managerfoot.motor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

/**
 * Uma partida em andamento, avançada minuto a minuto (SPEC §7, decisão 0004: o servidor
 * controla o relógio). A cada minuto:
 * <ol>
 * <li>pode haver um pênalti ou uma finalização. Meio contra meio decide qual time tem a jogada (o
 * mandante recebe {@code fatorMando}); o finalizador sai de um setor conforme a formação e, dentro
 * dele, com peso pela força; ataque contra defesa e finalizador contra goleiro decidem o gol;</li>
 * <li>cada time pode receber um cartão, com o infrator sorteado pela roleta de temperamento.</li>
 * </ol>
 * Entre os minutos, o treinador do usuário pode substituir ({@link #substituir}); os times da
 * máquina só reagem ao goleiro expulso ({@link TreinadorMaquina}).
 * Toda aleatoriedade vem do {@link RandomGenerator} recebido, para a partida ser reproduzível.
 */
public final class Partida {

	/** Sem jogadores suficientes, o time deixa de receber cartões (proteção técnica, não regra do jogo). */
	private static final int MINIMO_EM_CAMPO_PARA_CARTAO = 2;


	private final ParametrosMotor parametros;
	private final TimeEmCampo casa;
	private final TimeEmCampo fora;
	private final RandomGenerator aleatorio;
	private final Set<Lado> ladosDaMaquina;
	private final List<EventoPartida> eventos = new ArrayList<>();
	private int minuto;
	private int golsCasa;
	private int golsFora;

	/**
	 * @param ladosDoUsuario times controlados por usuários; os demais seguem o {@link TreinadorMaquina}
	 */
	public Partida(ParametrosMotor parametros, RegrasPartida regras, Escalacao casa, Escalacao fora,
			Set<Lado> ladosDoUsuario, RandomGenerator aleatorio) {
		this.parametros = Objects.requireNonNull(parametros);
		double fatorImprovisado = parametros.penalti().fatorGoleiroImprovisado();
		this.casa = new TimeEmCampo(Objects.requireNonNull(casa), fatorImprovisado, regras.substituicoes());
		this.fora = new TimeEmCampo(Objects.requireNonNull(fora), fatorImprovisado, regras.substituicoes());
		this.ladosDaMaquina = EnumSet.complementOf(
				ladosDoUsuario.isEmpty() ? EnumSet.noneOf(Lado.class) : EnumSet.copyOf(ladosDoUsuario));
		this.aleatorio = Objects.requireNonNull(aleatorio);
	}

	/** Partida só entre times da máquina, com as regras padrão. */
	public Partida(ParametrosMotor parametros, Escalacao casa, Escalacao fora, RandomGenerator aleatorio) {
		this(parametros, RegrasPartida.padrao(), casa, fora, Set.of(), aleatorio);
	}

	/**
	 * Substitui um jogador em campo por um reserva disponível, entre os minutos (SPEC §7.4.1).
	 *
	 * @throws IllegalArgumentException se a troca não for permitida, com o motivo
	 * @throws IllegalStateException se a partida já terminou
	 */
	public EventoPartida substituir(Lado lado, Jogador sai, Jogador entra) {
		if (terminada()) {
			throw new IllegalStateException("a partida já terminou");
		}
		return registrarSubstituicao(lado, sai, entra);
	}

	/** Sem checar o fim: a máquina pode reagir a uma expulsão ocorrida no último minuto. */
	private EventoPartida registrarSubstituicao(Lado lado, Jogador sai, Jogador entra) {
		time(lado).substituir(sai, entra);
		EventoPartida evento = EventoPartida.substituicao(minuto, lado, entra, sai);
		eventos.add(evento);
		return evento;
	}

	public List<Jogador> emCampo(Lado lado) {
		return time(lado).emCampo();
	}

	public Jogador goleiro(Lado lado) {
		return time(lado).goleiro();
	}

	/** Reservas que ainda podem entrar; quem já entrou não aparece mais (SPEC §7.4.1). */
	public List<Jogador> reservasDisponiveis(Lado lado) {
		return time(lado).reservasDisponiveis();
	}

	public int substituicoesRestantes(Lado lado) {
		return time(lado).substituicoesRestantes();
	}

	public Formacao formacaoAtual(Lado lado) {
		return time(lado).formacao();
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
		double meioCasa = potencia(forcaSetor(casa, Posicao.MEIO_CAMPISTA)) * parametros.fatorMando();
		double meioFora = potencia(forcaSetor(fora, Posicao.MEIO_CAMPISTA));
		if (meioCasa + meioFora == 0) {
			return aleatorio.nextBoolean() ? Lado.CASA : Lado.FORA;
		}
		return aleatorio.nextDouble() * (meioCasa + meioFora) < meioCasa ? Lado.CASA : Lado.FORA;
	}

	private void finalizar(Lado lado) {
		TimeEmCampo ataque = time(lado);
		TimeEmCampo defesa = time(lado.adversario());
		Jogador finalizador = sortearFinalizador(ataque);

		double pressao = disputa(forcaSetor(ataque, Posicao.ATACANTE), forcaSetor(defesa, Posicao.DEFENSOR));
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
		boolean eraGoleiro = infrator == time.goleiro();
		if (tipo.expulsao()) {
			time.expulsar(infrator);
		}
		eventos.add(new EventoPartida(minuto, lado, tipo, infrator));
		if (tipo.expulsao() && eraGoleiro && ladosDaMaquina.contains(lado)) {
			TreinadorMaquina.aposGoleiroExpulso(time).ifPresent(t -> registrarSubstituicao(lado, t.sai(), t.entra()));
		}
	}

	private Jogador sortearFinalizador(TimeEmCampo time) {
		PesosFinalizador pesos = parametros.pesosFinalizador().get(time.formacao());
		// setores sem jogadores (após expulsões) ficam de fora do sorteio
		List<Posicao> setores = Arrays.stream(Posicao.values())
			.filter(p -> !time.jogadoresNa(p).isEmpty() && pesos.peso(p) > 0)
			.toList();
		Posicao setor = Sorteio.ponderado(setores, pesos::peso, aleatorio);
		return Sorteio.ponderado(time.jogadoresNa(setor), Jogador::forca, aleatorio);
	}

	/**
	 * Força de um setor: média dos jogadores × referência × (quantidade / referência) ^ expoente.
	 * Com expoente 1 é a soma simples. O expoente controla quanto um jogador a mais ou a menos no
	 * setor muda a força; o setor com a quantidade de referência (a do 4-4-2) vale o mesmo em
	 * qualquer expoente, então o expoente equilibra formações sem mudar a média de gols.
	 */
	private double forcaSetor(TimeEmCampo time, Posicao posicao) {
		int quantidade = time.jogadoresNa(posicao).size();
		if (quantidade == 0) {
			return 0;
		}
		int referencia = Formacao.F_4_4_2.quantidade(posicao);
		double expoente = switch (posicao) {
			case MEIO_CAMPISTA -> parametros.expoenteMeias();
			case ATACANTE -> parametros.expoenteAtacantes();
			case DEFENSOR -> parametros.expoenteDefensores();
			case GOLEIRO -> 1;
		};
		double media = time.soma(posicao) / quantidade;
		return media * referencia * Math.pow((double) quantidade / referencia, expoente);
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
