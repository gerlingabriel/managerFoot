package br.com.managerfoot.motor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.EscalacaoDoTreinador;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.LimiteEstrangeiros;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

/** Limite de estrangeiros em campo na liga e na copa nacional (SPEC §8.4, rev. 130). */
class LimiteEstrangeirosTest {

	private static final Optional<LimiteEstrangeiros> CINCO = Optional.of(new LimiteEstrangeiros("BRA", 5));

	private final EscalacaoMaquina maquina = new EscalacaoMaquina(ParametrosMotor.padrao());
	private long proximoId = 1;

	@Test
	void contaQuemEEstrangeiroNaLigaMenosOsEquiparados() {
		LimiteEstrangeiros limite = CINCO.orElseThrow();
		assertThat(limite.conta(jogador(Posicao.ATACANTE, 20, "ARG"))).isTrue();
		assertThat(limite.conta(jogador(Posicao.ATACANTE, 20, "POR"))).as("português no Brasil").isFalse();
		assertThat(limite.conta(jogador(Posicao.ATACANTE, 20, "BRA"))).isFalse();
		assertThat(new LimiteEstrangeiros("ESP", 5).conta(jogador(Posicao.ATACANTE, 20, "FRA"))).as("UE na Espanha").isFalse();
		assertThatIllegalArgumentException().isThrownBy(() -> new LimiteEstrangeiros("BRA", 12));
		assertThatIllegalArgumentException().isThrownBy(() -> new LimiteEstrangeiros("BRA", -1));
		assertThat(limite.problema(6)).isEqualTo("6 estrangeiros em campo: o limite é 5");
	}

	@Test
	void aMaquinaTrocaOsEstrangeirosExcedentesPeloMelhorNacionalDaPosicao() {
		List<Jogador> plantel = new ArrayList<>(jogadores(Posicao.GOLEIRO, "BRA", 25, 20));
		plantel.addAll(jogadores(Posicao.DEFENSOR, "ARG", 35, 35, 34));
		plantel.addAll(jogadores(Posicao.DEFENSOR, "BRA", 25, 24, 23, 22));
		plantel.addAll(jogadores(Posicao.MEIO_CAMPISTA, "URU", 35, 33));
		plantel.addAll(jogadores(Posicao.MEIO_CAMPISTA, "BRA", 25, 24, 23, 22));
		plantel.addAll(jogadores(Posicao.ATACANTE, "COL", 36, 30));
		plantel.addAll(jogadores(Posicao.ATACANTE, "BRA", 26, 21));

		Escalacao semLimite = maquina.escalar(plantel);
		Escalacao comLimite = maquina.escalar(plantel, CINCO);

		LimiteEstrangeiros limite = CINCO.orElseThrow();
		assertThat(limite.contar(semLimite.titulares())).isGreaterThan(5);
		assertThat(limite.contar(comLimite.titulares())).isEqualTo(5);
		assertThat(comLimite.titulares()).hasSize(11).allMatch(j -> j.posicao() != Posicao.GOLEIRO || j.forca() == 25);
		// troca os que menos perdem: os estrangeiros mais fortes ficam
		assertThat(comLimite.titulares()).extracting(Jogador::forca).contains(36, 35);
		assertThat(comLimite.reservas()).as("o banco não tem limite").anyMatch(limite::conta);
	}

	@Test
	void semNacionalNaPosicaoAMaquinaUsaUmNacionalDeOutraPosicaoDeLinha() {
		List<Jogador> plantel = new ArrayList<>(jogadores(Posicao.GOLEIRO, "BRA", 25));
		plantel.addAll(jogadores(Posicao.DEFENSOR, "BRA", 25, 25, 25, 25, 24));
		plantel.addAll(jogadores(Posicao.MEIO_CAMPISTA, "BRA", 25, 25, 25, 25, 24));
		plantel.addAll(jogadores(Posicao.ATACANTE, "ARG", 40, 40, 40, 40, 40, 40, 40));

		Escalacao escalacao = maquina.escalar(plantel, Optional.of(new LimiteEstrangeiros("BRA", 1)));

		assertThat(new LimiteEstrangeiros("BRA", 1).contar(escalacao.titulares())).isLessThanOrEqualTo(1);
		assertThat(escalacao.titulares()).hasSize(11);
	}

	@Test
	void naContinentalENoMundialNaoHaLimite() {
		List<Jogador> plantel = new ArrayList<>(jogadores(Posicao.GOLEIRO, "ARG", 25));
		plantel.addAll(jogadores(Posicao.DEFENSOR, "ARG", 25, 25, 25, 25));
		plantel.addAll(jogadores(Posicao.MEIO_CAMPISTA, "ARG", 25, 25, 25, 25));
		plantel.addAll(jogadores(Posicao.ATACANTE, "ARG", 25, 25));
		plantel.addAll(jogadores(Posicao.DEFENSOR, "BRA", 10));

		assertThat(CINCO.orElseThrow().contar(maquina.escalar(plantel, Optional.empty()).titulares())).isEqualTo(11);
	}

	@Test
	void aSubstituicaoQuePassaDoLimiteERecusadaEAQueTrocaEstrangeiroPorEstrangeiroNao() {
		List<Jogador> titulares = time442("ARG", 5);
		Jogador argentinoReserva = jogador(Posicao.ATACANTE, 20, "ARG");
		Jogador brasileiroReserva = jogador(Posicao.ATACANTE, 20, "BRA");
		Partida partida = partida(titulares, List.of(argentinoReserva, brasileiroReserva), Set.of(Lado.CASA));
		Jogador nacional = titulares.stream().filter(j -> j.nacionalidade().equals("BRA") && j.posicao() == Posicao.ATACANTE)
			.findFirst().orElseThrow();
		Jogador estrangeiro = titulares.stream().filter(j -> j.nacionalidade().equals("ARG") && j.posicao() != Posicao.GOLEIRO)
			.findFirst().orElseThrow();

		assertThatIllegalArgumentException().isThrownBy(() -> partida.substituir(Lado.CASA, nacional, argentinoReserva))
			.withMessage("Já há 5 estrangeiros em campo, o limite. Para entrar um estrangeiro, tire outro estrangeiro.");
		partida.substituir(Lado.CASA, estrangeiro, argentinoReserva);
		assertThat(partida.emCampo(Lado.CASA)).contains(argentinoReserva);
	}

	@Test
	void aMaquinaNaoTrocaOLesionadoPorEstrangeiroAlemDoLimite() {
		List<Jogador> titulares = time442("ARG", 5);
		Jogador nacional = titulares.stream().filter(j -> j.nacionalidade().equals("BRA") && j.posicao() == Posicao.ATACANTE)
			.findFirst().orElseThrow();
		Jogador argentinoReserva = jogador(Posicao.ATACANTE, 30, "ARG");
		Jogador brasileiroReserva = jogador(Posicao.ATACANTE, 10, "BRA");
		TimeEmCampo time = new TimeEmCampo(new Escalacao(Formacao.F_4_4_2, titulares, List.of(argentinoReserva, brasileiroReserva)),
				0.5, 5);

		assertThat(TreinadorMaquina.aposLesao(time, nacional, CINCO)).map(TreinadorMaquina.Troca::entra)
			.contains(brasileiroReserva);
		assertThat(TreinadorMaquina.aposLesao(time, nacional, Optional.empty())).map(TreinadorMaquina.Troca::entra)
			.contains(argentinoReserva);
	}

	@Test
	void aEscalacaoDoTreinadorAcimaDoLimiteNaoJogaEATaticaCompletaRespeitandoOLimite() {
		List<Jogador> plantel = new ArrayList<>(time442("ARG", 6));
		EscalacaoDoTreinador escalacao = new EscalacaoDoTreinador(Formacao.F_4_4_2,
				Set.copyOf(plantel.stream().map(Jogador::id).toList()), Set.of(), null);

		assertThat(escalacao.problemas(plantel, id -> false, id -> false, CINCO))
			.containsExactly("6 estrangeiros em campo: o limite é 5");
		assertThat(escalacao.problemas(plantel, id -> false, id -> false, Optional.empty())).isEmpty();

		Jogador brasileiro = jogador(Posicao.DEFENSOR, 10, "BRA");
		plantel.add(brasileiro);
		EscalacaoDoTreinador completada = escalacao.vazia().completada(plantel, id -> false, 3, id -> 0, CINCO);
		assertThat(completada.problemas(plantel, id -> false, id -> false, CINCO)).isEmpty();
		assertThat(completada.titulares()).contains(brasileiro.id());
	}

	/** 4-4-2 com {@code estrangeiros} de fora (do gol para a frente) e o resto brasileiro. */
	private List<Jogador> time442(String pais, int estrangeiros) {
		List<Jogador> time = new ArrayList<>();
		int n = 0;
		for (Posicao posicao : Posicao.values()) {
			for (int i = 0; i < Formacao.F_4_4_2.quantidade(posicao); i++) {
				time.add(jogador(posicao, 25, n++ < estrangeiros ? pais : "BRA"));
			}
		}
		return time;
	}

	private Partida partida(List<Jogador> titulares, List<Jogador> reservas, Set<Lado> doUsuario) {
		CondicoesPartida condicoes = CondicoesPartida.NEUTRAS.comEstrangeiros(CINCO);
		return new Partida(ParametrosMotor.padrao(), RegrasPartida.padrao(), new Escalacao(Formacao.F_4_4_2, titulares, reservas),
				new Escalacao(Formacao.F_4_4_2, time442("BRA", 0), List.of()), doUsuario, condicoes, new SplittableRandom(1));
	}

	private List<Jogador> jogadores(Posicao posicao, String pais, int... forcas) {
		List<Jogador> lista = new ArrayList<>();
		for (int forca : forcas) {
			lista.add(jogador(posicao, forca, pais));
		}
		return lista;
	}

	private Jogador jogador(Posicao posicao, int forca, String pais) {
		long id = proximoId++;
		return new Jogador(id, posicao.name() + id, posicao, forca, Temperamento.NORMAL, pais);
	}
}
