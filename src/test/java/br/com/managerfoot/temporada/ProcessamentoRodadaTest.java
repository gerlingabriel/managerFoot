package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.TipoEvento;

class ProcessamentoRodadaTest {

	static final ConfiguracaoLiga LIGA = new ConfiguracaoLiga("Nacional",
			List.of(new Divisao("A", 1, 50, 0, 2, clubes(1, 16, 35)), new Divisao("B", 2, 30, 2, 2, clubes(101, 15, 22))),
			clubes(201, 2, 15));
	static final Map<Long, List<Jogador>> PLANTEIS = planteis();
	static final Temporada TEMPORADA = Temporada.iniciar(1, LIGA, PLANTEIS, 99);

	private final ProcessamentoRodada processamento = ProcessamentoRodada.padrao();

	@Test
	void rodadaJogaTodasAsDivisoesDaMaisBaixaParaAPrincipal() {
		ProcessamentoRodada.Resultado r = processamento.jogar(TEMPORADA, 1, PLANTEIS, Disciplina.vazia(), Map.of());

		assertThat(r.partidas()).hasSize(8 + 7);
		assertThat(r.partidas().getFirst().prioridade()).isEqualTo(2);
		assertThat(r.partidas().getLast().prioridade()).isEqualTo(1);
		assertThat(r.partidas()).allMatch(p -> p.jogo().rodada() == 1);
		assertThat(r.moral()).hasSize(16 + 14);
	}

	@Test
	void mesmoEstadoMesmaRodada() {
		assertThat(processamento.jogar(TEMPORADA, 3, PLANTEIS, Disciplina.vazia(), Map.of()))
			.isEqualTo(processamento.jogar(TEMPORADA, 3, PLANTEIS, Disciplina.vazia(), Map.of()));
	}

	@Test
	void suspensoNaoJogaECumpreASuspensao() {
		Jogo jogo = TEMPORADA.divisoes().getFirst().calendario().rodadas().getFirst().jogos().getFirst();
		Jogador artilheiro = PLANTEIS.get(jogo.mandante().id()).getLast();
		Disciplina suspenso = new Disciplina(Map.of(), Map.of(artilheiro.id(), 1));

		ProcessamentoRodada.Resultado r = processamento.jogar(TEMPORADA, 1, PLANTEIS, suspenso, Map.of());

		assertThat(r.partidas().stream().flatMap(p -> p.resultado().eventos().stream()))
			.noneMatch(e -> e.jogador().id() == artilheiro.id());
		assertThat(r.disciplina().suspenso(artilheiro.id())).isFalse();
	}

	@Test
	void clubeDoTreinadorJogaComAEscalacaoDele() {
		Clube clube = LIGA.divisoes().getFirst().clubes().getFirst();
		List<Jogador> plantel = PLANTEIS.get(clube.id());
		// os mais fracos de cada posição num 5-4-1, para não coincidir com a escolha da máquina
		Escalacao escolhida = new Escalacao(Formacao.F_5_4_1, List.of(
				maisFracos(plantel, Posicao.GOLEIRO, 1), maisFracos(plantel, Posicao.DEFENSOR, 5),
				maisFracos(plantel, Posicao.MEIO_CAMPISTA, 4), maisFracos(plantel, Posicao.ATACANTE, 1))
			.stream().flatMap(List::stream).toList());
		Set<Long> escalados = escolhida.titulares().stream().map(Jogador::id).collect(Collectors.toSet());
		Set<Long> doClube = plantel.stream().map(Jogador::id).collect(Collectors.toSet());

		List<EventoPartida> eventos = new ArrayList<>();
		for (int rodada = 1; rodada <= 15; rodada++) {
			processamento.jogar(TEMPORADA, rodada, PLANTEIS, Disciplina.vazia(), Map.of(), Map.of(clube.id(), escolhida))
				.partidas().forEach(p -> eventos.addAll(p.resultado().eventos()));
		}

		List<Long> doClubeNosEventos = eventos.stream().map(e -> e.jogador().id()).filter(doClube::contains).toList();
		assertThat(doClubeNosEventos).isNotEmpty().allMatch(escalados::contains);
	}

	@Test
	void escalacaoDoTreinadorComSuspensoERecusada() {
		Clube clube = LIGA.divisoes().getFirst().clubes().getFirst();
		Escalacao escolhida = new EscalacaoMaquina(ParametrosMotor.padrao()).escalar(PLANTEIS.get(clube.id()));
		Disciplina suspenso = new Disciplina(Map.of(), Map.of(escolhida.titulares().getLast().id(), 1));

		assertThatIllegalStateException()
			.isThrownBy(() -> processamento.jogar(TEMPORADA, 1, PLANTEIS, suspenso, Map.of(), Map.of(clube.id(), escolhida)))
			.withMessageContaining("suspenso");
	}

	@Test
	void rodadaInexistenteERecusada() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> processamento.jogar(TEMPORADA, 99, PLANTEIS, Disciplina.vazia(), Map.of()));
	}

	@Test
	void clubeComDezDisponiveisJogaComUmAMenos() {
		Map<Long, List<Jogador>> curtos = new HashMap<>(PLANTEIS);
		Jogo jogo = TEMPORADA.divisoes().getFirst().calendario().rodadas().getFirst().jogos().getFirst();
		curtos.put(jogo.mandante().id(), PLANTEIS.get(jogo.mandante().id()).subList(0, 10));

		RodadaAoVivo.PartidaDaRodada partida = processamento
			.preparar(TEMPORADA, 1, curtos, Disciplina.vazia(), Map.of(), List.of())
			.partidas()
			.stream()
			.filter(p -> p.jogo().equals(jogo))
			.findFirst()
			.orElseThrow();
		assertThat(partida.casa().titulares()).hasSize(10);
	}

	@Test
	void clubeSemOMinimoParaComecarFalhaComONomeDoClube() {
		Map<Long, List<Jogador>> curtos = new HashMap<>(PLANTEIS);
		Jogo jogo = TEMPORADA.divisoes().getFirst().calendario().rodadas().getFirst().jogos().getFirst();
		curtos.put(jogo.mandante().id(), PLANTEIS.get(jogo.mandante().id()).subList(0, 6));

		assertThatIllegalStateException()
			.isThrownBy(() -> processamento.jogar(TEMPORADA, 1, curtos, Disciplina.vazia(), Map.of()))
			.withMessageContaining(jogo.mandante().nome());
	}

	@Test
	void temporadaInteiraTemPlacaresPlausiveisESuspensoes() {
		Disciplina disciplina = Disciplina.vazia();
		Map<Long, Moral> moral = Map.of();
		List<ProcessamentoRodada.PartidaJogada> todas = new ArrayList<>();
		int rodadas = TEMPORADA.divisoes().stream().mapToInt(d -> d.calendario().rodadas().size()).max().orElseThrow();
		int suspensosEmAlgumaRodada = 0;
		for (int rodada = 1; rodada <= rodadas; rodada++) {
			ProcessamentoRodada.Resultado r = processamento.jogar(TEMPORADA, rodada, PLANTEIS, disciplina, moral);
			todas.addAll(r.partidas());
			disciplina = r.disciplina();
			moral = r.moral();
			suspensosEmAlgumaRodada += disciplina.suspensoes().size();
		}

		assertThat(todas).hasSize(16 * 15 + 15 * 14);
		double golsPorJogo = todas.stream().mapToInt(p -> p.resultado().golsCasa() + p.resultado().golsFora()).average()
			.orElseThrow();
		assertThat(golsPorJogo).isBetween(1.5, 4.0);
		assertThat(suspensosEmAlgumaRodada).isPositive();
		long cartoes = todas.stream().flatMap(p -> p.resultado().eventos().stream()).map(EventoPartida::tipo)
			.filter(t -> t == TipoEvento.CARTAO_AMARELO).count();
		assertThat(cartoes).isPositive();
	}

	private static List<Jogador> maisFracos(List<Jogador> plantel, Posicao posicao, int quantidade) {
		return plantel.stream().filter(j -> j.posicao() == posicao).sorted(Comparator.comparingInt(Jogador::forca))
			.limit(quantidade).toList();
	}

	private static List<Clube> clubes(int primeiroId, int quantidade, int forca) {
		return IntStream.range(primeiroId, primeiroId + quantidade).mapToObj(id -> new Clube(id, "Clube " + id, forca))
			.toList();
	}

	/** Elenco padrão de 23 (3 goleiros, 7 defensores, 7 meias, 6 atacantes) para cada clube. */
	private static Map<Long, List<Jogador>> planteis() {
		SplittableRandom aleatorio = new SplittableRandom(7);
		Temperamento[] temperamentos = Temperamento.values();
		Map<Long, List<Jogador>> planteis = new HashMap<>();
		List<Clube> todos = new ArrayList<>(LIGA.filaDeEspera());
		LIGA.divisoes().forEach(d -> todos.addAll(d.clubes()));
		for (Clube clube : todos) {
			List<Jogador> plantel = new ArrayList<>();
			int[] quantidades = { 3, 7, 7, 6 };
			for (Posicao posicao : Posicao.values()) {
				for (int i = 0; i < quantidades[posicao.ordinal()]; i++) {
					long id = clube.id() * 100 + plantel.size();
					int forca = Math.clamp(clube.forca() + aleatorio.nextInt(-5, 6), 1, 50);
					plantel.add(new Jogador(id, "J" + id, posicao, forca, temperamentos[aleatorio.nextInt(5)],
							aleatorio.nextInt(10) == 0, "BRA"));
				}
			}
			planteis.put(clube.id(), plantel);
		}
		return planteis;
	}
}
