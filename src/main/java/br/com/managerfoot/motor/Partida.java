package br.com.managerfoot.motor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Sorte;
import br.com.managerfoot.dominio.Temperamento;

/**
 * Uma partida em andamento, avançada minuto a minuto (SPEC §7, decisão 0004: o servidor
 * controla o relógio). A cada minuto:
 * <ol>
 * <li>pode haver um pênalti ou uma finalização. Meio contra meio decide qual time tem a jogada (o
 * mandante recebe {@code fatorMando}); o finalizador sai de um setor conforme a formação e, dentro
 * dele, com peso pela força; ataque contra defesa e finalizador contra goleiro decidem o gol;</li>
 * <li>cada time pode receber um cartão, com o infrator sorteado pela roleta de temperamento;</li>
 * <li>cada time pode ter um lesionado (SPEC §7.5), também sorteado pelo temperamento, até o limite da
 * temporada. Na máquina a troca é na hora; no usuário o lesionado espera a troca dele até o início
 * do minuto seguinte e, sem ela, a máquina decide.</li>
 * </ol>
 * A sorte da temporada (SPEC §5.3.1) dá ao beneficiado uma chance extra de finalização por minuto
 * e tira domínio do prejudicado. O asterisco (SPEC §5.3) aumenta a chance de ser o finalizador e de
 * marcar; acima do limite em campo, cada asterisco a mais tira domínio do time. Cada formação
 * tem um ajuste de equilíbrio no meio, no ataque e na defesa ({@link AjusteFormacao}, SPEC §7.2).
 * Entre os minutos, o treinador do usuário pode substituir ({@link #substituir}); os times da
 * máquina só reagem ao goleiro expulso ({@link TreinadorMaquina}).
 * Numa partida que precisa de vencedor ({@link JogoDecisivo}, SPEC §8.2), o placar somado empatado
 * no fim dos 90 minutos leva à prorrogação e, persistindo, à disputa de pênaltis ao fim dela.
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
	private final CondicoesPartida condicoes;
	private final List<EventoPartida> eventos = new ArrayList<>();
	private final Map<Lado, Integer> lesoes = new EnumMap<>(Lado.class);
	private final Map<Lado, Jogador> lesionadoAguardando = new EnumMap<>(Lado.class);
	private int minuto;
	private int fim;
	private int golsCasa;
	private int golsFora;
	private boolean prorrogacao;
	private DisputaPenaltis penaltis;
	/** Zebra da copa (rev. 117): o lado mais fraco que joga com bônus, decidido no início; nulo sem bônus. */
	private final Lado bonificado;
	private final double bonusForca;
	private final boolean superBonus;

	/**
	 * @param ladosDoUsuario times controlados por usuários; os demais seguem o {@link TreinadorMaquina}
	 * @param condicoes sorte e bônus de artilharia da temporada
	 */
	public Partida(ParametrosMotor parametros, RegrasPartida regras, Escalacao casa, Escalacao fora,
			Set<Lado> ladosDoUsuario, CondicoesPartida condicoes, RandomGenerator aleatorio) {
		this.parametros = Objects.requireNonNull(parametros);
		double fatorImprovisado = parametros.penalti().fatorGoleiroImprovisado();
		this.casa = new TimeEmCampo(Objects.requireNonNull(casa), fatorImprovisado, regras.substituicoes());
		this.fora = new TimeEmCampo(Objects.requireNonNull(fora), fatorImprovisado, regras.substituicoes());
		this.ladosDaMaquina = EnumSet.complementOf(
				ladosDoUsuario.isEmpty() ? EnumSet.noneOf(Lado.class) : EnumSet.copyOf(ladosDoUsuario));
		this.condicoes = Objects.requireNonNull(condicoes);
		this.aleatorio = Objects.requireNonNull(aleatorio);
		this.fim = parametros.minutos();
		// zebra da copa (SPEC §8.2, rev. 117): pela força em campo dos titulares, no início da partida;
		// só sorteia o super bônus quando ele pode valer, para não mudar as outras partidas da mesma semente
		ParametrosZebraCopa zebra = parametros.zebraCopa();
		double mediaCasa = casa.titulares().stream().mapToInt(Jogador::forca).average().orElse(0);
		double mediaFora = fora.titulares().stream().mapToInt(Jogador::forca).average().orElse(0);
		double diferenca = Math.abs(mediaCasa - mediaFora);
		if (condicoes.copa().isPresent() && diferenca >= zebra.diferencaMinima()) {
			this.bonificado = mediaCasa < mediaFora ? Lado.CASA : Lado.FORA;
			// só sobre o que passa do limite: sem salto em 15, quem está mais perto segue com mais chance
			this.bonusForca = zebra.fracaoBonus() * (diferenca - zebra.diferencaMinima());
			this.superBonus = condicoes.copa().get().primeirasFases() && zebra.chanceSuperBonus() > 0
					&& this.aleatorio.nextDouble() < zebra.chanceSuperBonus();
		} else {
			this.bonificado = null;
			this.bonusForca = 0;
			this.superBonus = false;
		}
	}

	/**
	 * Força com a zebra da copa: o mais fraco joga com o bônus (até o teto) ou, no super bônus, com a força
	 * do super bônus; os demais com a própria força.
	 */
	private double forcaEfetiva(Lado lado, double forca) {
		if (lado != bonificado) {
			return forca;
		}
		double teto = parametros.zebraCopa().forcaSuperBonus();
		return superBonus ? teto : Math.min(teto, forca + bonusForca);
	}

	/** O lado que joga com a zebra da copa nesta partida, se houver (testes e relatórios). */
	Optional<Lado> ladoComBonusDeCopa() {
		return Optional.ofNullable(bonificado);
	}

	/** Se o lado com a zebra da copa joga com o super bônus (testes e relatórios). */
	boolean comSuperBonusDeCopa() {
		return superBonus;
	}

	/** Partida sem sorte nem bônus de artilharia. */
	public Partida(ParametrosMotor parametros, RegrasPartida regras, Escalacao casa, Escalacao fora,
			Set<Lado> ladosDoUsuario, RandomGenerator aleatorio) {
		this(parametros, regras, casa, fora, ladosDoUsuario, CondicoesPartida.NEUTRAS, aleatorio);
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
		// limite de estrangeiros em campo (SPEC §8.4, rev. 130)
		condicoes.estrangeiros().filter(l -> l.trocaExcede(time(lado).emCampo(), sai, entra)).ifPresent(l -> {
			throw new IllegalArgumentException(l.trocaRecusada());
		});
		return registrarSubstituicao(lado, sai, entra);
	}

	/** Sem checar o fim: a máquina pode reagir a uma expulsão ocorrida no último minuto. */
	private EventoPartida registrarSubstituicao(Lado lado, Jogador sai, Jogador entra) {
		time(lado).substituir(sai, entra);
		lesionadoAguardando.remove(lado, sai);
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

	/** Lesionado do time do usuário ainda em campo, esperando a troca (SPEC §7.5). */
	public Optional<Jogador> lesionadoAguardandoTroca(Lado lado) {
		return Optional.ofNullable(lesionadoAguardando.get(lado));
	}

	/** Avança um minuto e devolve os eventos ocorridos nele. */
	public List<EventoPartida> avancarMinuto() {
		if (terminada()) {
			throw new IllegalStateException("a partida já terminou no minuto " + minuto);
		}
		int antes = eventos.size();
		// o usuário não trocou o lesionado: a máquina decide, ainda no minuto da lesão
		for (Lado lado : Lado.values()) {
			Jogador lesionado = lesionadoAguardando.remove(lado);
			if (lesionado != null) {
				tirarLesionado(lado, lesionado);
			}
		}
		minuto++;
		if (aleatorio.nextDouble() < parametros.penalti().penaltisPorMinuto()) {
			cobrarPenalti(sortearQuemDomina());
		} else if (aleatorio.nextDouble() < parametros.chancePorMinuto()) {
			finalizar(sortearQuemDomina());
		}
		// só sorteia quando há beneficiado, para não mudar as partidas neutras de uma mesma semente
		Lado beneficiado = ladoCom(Sorte.BENEFICIADO);
		if (beneficiado != null
				&& aleatorio.nextDouble() < parametros.chancePorMinuto() * parametros.sorte().chancesExtras()) {
			finalizar(beneficiado);
		}
		// com um a mais, mais lances (rev. 99); só sorteia depois de uma expulsão, para não mudar as outras partidas
		int aMaisNaCasa = expulsos(Lado.CASA) - expulsos(Lado.FORA);
		if (aMaisNaCasa != 0 && parametros.disciplina().chancesExtrasPorExpulsao() > 0 && aleatorio.nextDouble()
				< parametros.chancePorMinuto() * parametros.disciplina().chancesExtrasPorExpulsao() * Math.abs(aMaisNaCasa)) {
			finalizar(aMaisNaCasa > 0 ? Lado.FORA : Lado.CASA);
		}
		verificarCartao(Lado.CASA);
		verificarCartao(Lado.FORA);
		verificarLesao(Lado.CASA);
		verificarLesao(Lado.FORA);
		if (minuto == fim && condicoes.decisivo().isPresent() && placarSomadoEmpatado()) {
			if (prorrogacao) {
				penaltis = disputarPenaltis();
			} else {
				prorrogacao = true;
				fim += JogoDecisivo.MINUTOS_PRORROGACAO;
			}
		}
		return List.copyOf(eventos.subList(antes, eventos.size()));
	}

	public boolean terminada() {
		return minuto >= fim;
	}

	/** A partida foi para a prorrogação (SPEC §8.2). */
	public boolean prorrogacao() {
		return prorrogacao;
	}

	/** Minuto em que a partida termina: 90, ou 120 depois de ir para a prorrogação. */
	public int minutoFinal() {
		return fim;
	}

	public int minuto() {
		return minuto;
	}

	public ResultadoPartida resultado() {
		return new ResultadoPartida(golsCasa, golsFora, eventos, prorrogacao, Optional.ofNullable(penaltis));
	}

	private Lado sortearQuemDomina() {
		boolean campoNeutro = condicoes.decisivo().map(JogoDecisivo::campoNeutro).orElse(false);
		double meioCasa = potencia(forcaSetor(casa, Posicao.MEIO_CAMPISTA)) * (campoNeutro ? 1 : parametros.fatorMando())
				* fatorDominio(Lado.CASA) * ajuste(casa).meio();
		double meioFora = potencia(forcaSetor(fora, Posicao.MEIO_CAMPISTA)) * fatorDominio(Lado.FORA) * ajuste(fora).meio();
		double chanceCasa = meioCasa + meioFora == 0 ? 0.5 : meioCasa / (meioCasa + meioFora);
		chanceCasa = comExpulsoes(chanceCasa, expulsos(Lado.CASA) - expulsos(Lado.FORA), parametros.disciplina());
		return aleatorio.nextDouble() < chanceCasa ? Lado.CASA : Lado.FORA;
	}

	/**
	 * Expulsão (SPEC §7.4, rev. 98): além do buraco no setor, o adversário fica com 50% a mais da chance
	 * de ter a jogada, por expulso a mais: 50/50 vira 75/25, 60/40 vira 90/10, com teto de 99/1.
	 *
	 * @param chanceCasa chance do mandante ficar com a jogada pelo meio
	 * @param aMaisNaCasa expulsos do mandante menos os do visitante
	 * @return a chance do mandante com a expulsão
	 */
	static double comExpulsoes(double chanceCasa, int aMaisNaCasa, ParametrosDisciplina disciplina) {
		if (aMaisNaCasa == 0) {
			return chanceCasa;
		}
		double fator = Math.pow(1 + disciplina.bonusDominioPorExpulsao(), Math.abs(aMaisNaCasa));
		if (aMaisNaCasa > 0) {
			double fora = 1 - chanceCasa;
			return 1 - Math.max(fora, Math.min(disciplina.dominioMaximo(), fora * fator));
		}
		return Math.max(chanceCasa, Math.min(disciplina.dominioMaximo(), chanceCasa * fator));
	}

	/**
	 * Trava das goleadas com expulso (SPEC §7.4, rev. 114): com jogador a mais e 2 gols ou mais de vantagem,
	 * a conversão cai a cada gol de vantagem além do primeiro. Sem expulso a mais vale 1, e o motor não muda.
	 */
	private double travaComExpulsao(Lado lado) {
		int vantagem = gols(lado) - gols(lado.adversario());
		if (expulsos(lado.adversario()) <= expulsos(lado) || vantagem < 2) {
			return 1;
		}
		return Math.pow(parametros.disciplina().travaGoleadaComExpulsao(), vantagem - 1);
	}

	/** Expulsos por cartão até agora; quem sai lesionado sem substituto não conta. */
	private int expulsos(Lado lado) {
		return (int) eventos.stream().filter(e -> e.lado() == lado && e.tipo().expulsao()).count();
	}

	private void finalizar(Lado lado) {
		TimeEmCampo ataque = time(lado);
		TimeEmCampo defesa = time(lado.adversario());
		Jogador finalizador = sortearFinalizador(ataque);

		double pressao = disputa(forcaSetor(ataque, Posicao.ATACANTE), forcaSetor(defesa, Posicao.DEFENSOR));
		double duelo = disputa(forcaEfetiva(lado, finalizador.forca()), forcaEfetiva(lado.adversario(), defesa.forcaGoleiro()));
		double antiGoleada = Math.pow(parametros.fatorPorGolMarcado(), gols(lado));
		double conversao = Math.clamp(parametros.conversaoBase() * pressao * duelo * antiGoleada,
				parametros.conversaoMinima(), parametros.conversaoMaxima()) * bonusFinalizacao(finalizador)
				* abertura(ataque) * abertura(defesa) * ajuste(ataque).ataque() / ajuste(defesa).defesa()
				* travaComExpulsao(lado);

		registrarFinalizacao(lado, finalizador, conversao, false);
	}

	private void cobrarPenalti(Lado lado) {
		Jogador batedor = time(lado).batedorPenalti(aleatorio);
		registrarFinalizacao(lado, batedor, conversaoPenalti(lado, batedor), true);
	}

	private double conversaoPenalti(Lado lado, Jogador batedor) {
		TimeEmCampo defesa = time(lado.adversario());
		double conversao = defesa.goleiro().asterisco() && !defesa.goleiroImprovisado()
				? parametros.asterisco().conversaoPenaltiGoleiroAsterisco()
				: parametros.penalti().conversao();
		return Math.min(1, conversao * bonusFinalizacao(batedor));
	}

	private boolean placarSomadoEmpatado() {
		JogoDecisivo decisivo = condicoes.decisivo().orElseThrow();
		return golsCasa + decisivo.golsCasaAntes() == golsFora + decisivo.golsForaAntes();
	}

	/**
	 * Disputa de pênaltis com a mesma conversão do pênalti da partida. O primeiro a bater é sorteado;
	 * cada lado bate na ordem de {@link #ordemDaDisputa}, recomeçando quando todos já bateram.
	 */
	private DisputaPenaltis disputarPenaltis() {
		Lado primeiro = aleatorio.nextBoolean() ? Lado.CASA : Lado.FORA;
		Map<Lado, List<Jogador>> ordem = new EnumMap<>(Lado.class);
		ordem.put(primeiro, ordemDaDisputa(primeiro));
		ordem.put(primeiro.adversario(), ordemDaDisputa(primeiro.adversario()));
		Map<Lado, Integer> cobradas = new EnumMap<>(Map.of(Lado.CASA, 0, Lado.FORA, 0));
		Map<Lado, Integer> gols = new EnumMap<>(Map.of(Lado.CASA, 0, Lado.FORA, 0));
		List<DisputaPenaltis.Cobranca> cobrancas = new ArrayList<>();
		while (true) {
			for (Lado lado : List.of(primeiro, primeiro.adversario())) {
				List<Jogador> batedores = ordem.get(lado);
				Jogador batedor = batedores.get(cobradas.get(lado) % batedores.size());
				boolean gol = aleatorio.nextDouble() < conversaoPenalti(lado, batedor);
				cobrancas.add(new DisputaPenaltis.Cobranca(lado, batedor, gol));
				cobradas.merge(lado, 1, Integer::sum);
				if (gol) {
					gols.merge(lado, 1, Integer::sum);
				}
				if (disputaDecidida(cobradas, gols)) {
					return new DisputaPenaltis(cobrancas);
				}
			}
		}
	}

	/**
	 * Nas cinco iniciais, decidida quando um lado não alcança o outro nem convertendo as que faltam;
	 * depois, quando os dois bateram o mesmo número de vezes e um tem mais gols.
	 */
	private static boolean disputaDecidida(Map<Lado, Integer> cobradas, Map<Lado, Integer> gols) {
		int cobradasCasa = cobradas.get(Lado.CASA);
		int cobradasFora = cobradas.get(Lado.FORA);
		int golsCasa = gols.get(Lado.CASA);
		int golsFora = gols.get(Lado.FORA);
		int inicial = DisputaPenaltis.COBRANCAS_INICIAIS;
		if (cobradasCasa < inicial || cobradasFora < inicial) {
			int faltamCasa = Math.max(0, inicial - cobradasCasa);
			int faltamFora = Math.max(0, inicial - cobradasFora);
			return golsCasa > golsFora + faltamFora || golsFora > golsCasa + faltamCasa;
		}
		return cobradasCasa == cobradasFora && golsCasa != golsFora;
	}

	/**
	 * Ordem dos batedores na disputa (SPEC §12): o batedor escolhido pelo treinador, se está em campo;
	 * depois os jogadores de linha do mais forte ao mais fraco, com o asterisco à frente no empate e o
	 * sorteio entre os iguais; o goleiro por último.
	 */
	private List<Jogador> ordemDaDisputa(Lado lado) {
		TimeEmCampo time = time(lado);
		List<Jogador> linha = new ArrayList<>(time.emCampo());
		linha.remove(time.goleiro());
		Collections.shuffle(linha, aleatorio);
		linha.sort(Comparator.comparingInt(Jogador::forca).reversed()
			.thenComparing(Jogador::asterisco, Comparator.reverseOrder()));
		time.batedorPreferidoEmCampo().ifPresent(preferido -> {
			linha.remove(preferido);
			linha.addFirst(preferido);
		});
		if (!linha.contains(time.goleiro())) {
			linha.add(time.goleiro());
		}
		return linha;
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
		eventos.add(penalti && tipo == TipoEvento.DEFESA
				? EventoPartida.penaltiDefendido(minuto, lado, finalizador, time(lado.adversario()).goleiro())
				: new EventoPartida(minuto, lado, tipo, finalizador, penalti));
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
			TreinadorMaquina.aposGoleiroExpulso(time, condicoes.estrangeiros()).ifPresent(t -> registrarSubstituicao(lado, t.sai(), t.entra()));
		}
	}

	private void verificarLesao(Lado lado) {
		ParametrosLesao parametrosLesao = parametros.lesao();
		TimeEmCampo time = time(lado);
		List<Jogador> emCampo = time.emCampo();
		if (lesoes.getOrDefault(lado, 0) >= condicoes.lesoesPermitidas(lado, parametrosLesao.limitePorTemporada())
				|| emCampo.size() < MINIMO_EM_CAMPO_PARA_CARTAO
				|| aleatorio.nextDouble() >= parametrosLesao.lesoesPorMinuto()) {
			return;
		}
		Jogador lesionado = Sorteio.ponderado(emCampo, j -> parametrosLesao.peso(j.temperamento()), aleatorio);
		lesoes.merge(lado, 1, Integer::sum);
		eventos.add(EventoPartida.lesao(minuto, lado, lesionado, parametrosLesao.sortear(aleatorio)));
		if (ladosDaMaquina.contains(lado)) {
			tirarLesionado(lado, lesionado);
		} else {
			lesionadoAguardando.put(lado, lesionado);
		}
	}

	/** Troca automática do lesionado; sem troca possível, ele sai e o time fica com um a menos. */
	private void tirarLesionado(Lado lado, Jogador lesionado) {
		TimeEmCampo time = time(lado);
		TreinadorMaquina.aposLesao(time, lesionado, condicoes.estrangeiros())
			.ifPresentOrElse(t -> registrarSubstituicao(lado, t.sai(), t.entra()), () -> time.expulsar(lesionado));
	}

	private Jogador sortearFinalizador(TimeEmCampo time) {
		PesosFinalizador pesos = parametros.pesosFinalizador().get(time.formacao());
		// setores sem jogadores (após expulsões) ficam de fora do sorteio
		List<Posicao> setores = Arrays.stream(Posicao.values())
			.filter(p -> !time.jogadoresNa(p).isEmpty() && pesos.peso(p) > 0)
			.toList();
		Posicao setor = Sorteio.ponderado(setores, pesos::peso, aleatorio);
		double fator = parametros.asterisco().fatorEscolhaFinalizador();
		return Sorteio.ponderado(time.jogadoresNa(setor), j -> j.asterisco() ? j.forca() * fator : j.forca(),
				aleatorio);
	}

	/** Asterisco e bônus de artilharia somam, cada um, {@code bonusConversao} (SPEC §5.2, §5.3). */
	/** Jogo aberto pelos atacantes em campo (SPEC §7.2, rev. 107): vale para o ataque e para a defesa do time. */
	private double abertura(TimeEmCampo time) {
		return parametros.abertura(time.jogadoresNa(Posicao.ATACANTE).size());
	}

	/** Bônus de equilíbrio do esquema escolhido (SPEC §7.2, rev. 113). */
	private AjusteFormacao ajuste(TimeEmCampo time) {
		return parametros.ajuste(time.formacao());
	}

	private double bonusFinalizacao(Jogador jogador) {
		double bonus = parametros.asterisco().bonusConversao();
		int bonificacoes = (jogador.asterisco() ? 1 : 0) + (condicoes.bonusExtra().contains(jogador.id()) ? 1 : 0);
		return 1 + bonus * bonificacoes;
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
		double media = forcaEfetiva(time == casa ? Lado.CASA : Lado.FORA, time.soma(posicao) / quantidade);
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

	private double fatorDominio(Lado lado) {
		double sorte = condicoes.sorteDe(lado) == Sorte.PREJUDICADO ? parametros.sorte().fatorDominioPrejudicado() : 1;
		ParametrosAsterisco asterisco = parametros.asterisco();
		long excedentes = time(lado).emCampo().stream().filter(Jogador::asterisco).count()
				- asterisco.limiteSemPenalidade();
		return excedentes > 0 ? sorte * Math.pow(1 - asterisco.penalidadeDominioPorExcedente(), excedentes) : sorte;
	}

	private Lado ladoCom(Sorte procurada) {
		for (Lado lado : Lado.values()) {
			if (condicoes.sorteDe(lado) == procurada) {
				return lado;
			}
		}
		return null;
	}

	private TimeEmCampo time(Lado lado) {
		return lado == Lado.CASA ? casa : fora;
	}

	private int gols(Lado lado) {
		return lado == Lado.CASA ? golsCasa : golsFora;
	}
}
