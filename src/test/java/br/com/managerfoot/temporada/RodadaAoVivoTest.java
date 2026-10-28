package br.com.managerfoot.temporada;

import static br.com.managerfoot.temporada.ProcessamentoRodadaTest.PLANTEIS;
import static br.com.managerfoot.temporada.ProcessamentoRodadaTest.TEMPORADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.motor.EscalacaoMaquina;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.motor.Partida;
import br.com.managerfoot.motor.TipoEvento;
import br.com.managerfoot.temporada.RodadaAoVivo.PartidaDaRodada;

class RodadaAoVivoTest {

	private final ProcessamentoRodada processamento = ProcessamentoRodada.padrao();
	private final Clube doUsuario = TEMPORADA.divisoes().getFirst().calendario().rodadas().getFirst().jogos().getFirst()
		.mandante();
	private final Map<Long, Escalacao> escalacoes = Map.of(doUsuario.id(),
			new EscalacaoMaquina(ParametrosMotor.padrao()).escalar(PLANTEIS.get(doUsuario.id())));

	@Test
	void partidaRefeitaAteUmMinutoEhOComecoDaPartidaInteira() {
		RodadaAoVivo rodada = preparar();
		PartidaDaRodada partida = rodada.partidas().getLast();

		List<EventoPartida> ate45 = rodada.eventosAte(partida, 45);
		List<EventoPartida> inteira = rodada.noMinuto(partida, 90).resultado().eventos();

		assertThat(rodada.noMinuto(partida, 45).minuto()).isEqualTo(45);
		assertThat(ate45).allMatch(e -> e.minuto() <= 45);
		assertThat(inteira.subList(0, ate45.size())).isEqualTo(ate45);
		assertThat(rodada.finalizar(Map.of())).isEqualTo(rodada.finalizar(Map.of()));
	}

	@Test
	void substituicaoMudaSoAPartidaDoUsuario() {
		RodadaAoVivo rodada = preparar();
		PartidaDaRodada doClube = partidaDoUsuario(rodada);
		Partida aos30 = rodada.noMinuto(doClube, 30);
		Jogador sai = aos30.emCampo(Lado.CASA).stream().filter(j -> j.posicao() != Posicao.GOLEIRO)
			.findFirst().orElseThrow();
		Jogador entra = aos30.reservasDisponiveis(Lado.CASA).stream().filter(j -> j.posicao() == sai.posicao()).findFirst()
			.orElseThrow();

		RodadaAoVivo.Substituicao troca = rodada.substituir(doClube, 30, Lado.CASA, sai.id(), entra.id());
		RodadaAoVivo comTroca = rodada.com(troca);

		assertThat(troca.minuto()).isEqualTo(30);
		assertThat(comTroca.noMinuto(doClube, 31).emCampo(Lado.CASA)).contains(entra).doesNotContain(sai);
		assertThat(comTroca.noMinuto(doClube, 90).resultado().eventos())
			.anyMatch(e -> e.tipo() == TipoEvento.SUBSTITUICAO && e.minuto() == 30);
		ProcessamentoRodada.Resultado antes = rodada.finalizar(Map.of());
		ProcessamentoRodada.Resultado depois = comTroca.finalizar(Map.of());
		for (int i = 0; i < antes.partidas().size(); i++) {
			if (antes.partidas().get(i).jogo().mandante().id() != doUsuario.id()) {
				assertThat(depois.partidas().get(i)).isEqualTo(antes.partidas().get(i));
			}
		}
	}

	@Test
	void substituicaoRecusadaComMotivo() {
		RodadaAoVivo rodada = preparar();
		PartidaDaRodada doClube = partidaDoUsuario(rodada);
		Partida aos10 = rodada.noMinuto(doClube, 10);
		long emCampo = aos10.emCampo(Lado.CASA).getLast().id();
		long reserva = aos10.reservasDisponiveis(Lado.CASA).getFirst().id();

		assertThatIllegalArgumentException().isThrownBy(() -> rodada.substituir(doClube, 10, Lado.FORA,
				aos10.emCampo(Lado.FORA).getLast().id(), aos10.reservasDisponiveis(Lado.FORA).getFirst().id()))
			.withMessage("Só o treinador do clube faz substituições.");
		assertThatIllegalArgumentException().isThrownBy(() -> rodada.substituir(doClube, 10, Lado.CASA, reserva, emCampo))
			.withMessage("Quem sai precisa estar em campo.");
		assertThatIllegalArgumentException().isThrownBy(() -> rodada.substituir(doClube, 90, Lado.CASA, emCampo, reserva))
			.withMessage("A partida já terminou.");
	}

	@Test
	void previaDaFormacaoMudaOSetorSemGravar() {
		RodadaAoVivo rodada = preparar();
		PartidaDaRodada doClube = partidaDoUsuario(rodada);
		Partida aos20 = rodada.noMinuto(doClube, 20);
		Formacao antes = aos20.formacaoAtual(Lado.CASA);
		// a primeira troca entre setores de linha diferentes que resulta numa formação da lista
		for (Jogador sai : aos20.emCampo(Lado.CASA)) {
			for (Jogador entra : aos20.reservasDisponiveis(Lado.CASA)) {
				if (sai.posicao() == Posicao.GOLEIRO || entra.posicao() == Posicao.GOLEIRO || sai.posicao() == entra.posicao()) {
					continue;
				}
				int[] quantidades = { 0, antes.quantidade(Posicao.DEFENSOR), antes.quantidade(Posicao.MEIO_CAMPISTA),
						antes.quantidade(Posicao.ATACANTE) };
				quantidades[sai.posicao().ordinal()]--;
				quantidades[entra.posicao().ordinal()]++;
				Optional<Formacao> esperada = Formacao.com(quantidades[1], quantidades[2], quantidades[3]);
				if (esperada.isPresent()) {
					assertThat(rodada.formacaoCom(doClube, 20, Lado.CASA, sai.id(), entra.id())).isEqualTo(esperada.get());
					assertThat(rodada.noMinuto(doClube, 21).emCampo(Lado.CASA)).contains(sai);
					return;
				}
			}
		}
		throw new AssertionError("nenhuma troca de setor válida a partir de " + antes.rotulo());
	}

	@Test
	void rodadaRapidaEhARodadaAoVivoSemSubstituicoes() {
		assertThat(processamento.jogar(TEMPORADA, 2, PLANTEIS, Disciplina.vazia(), Map.of(), escalacoes))
			.isEqualTo(processamento.preparar(TEMPORADA, 2, PLANTEIS, Disciplina.vazia(), escalacoes, List.of())
				.finalizar(Map.of()));
	}

	private RodadaAoVivo preparar() {
		return processamento.preparar(TEMPORADA, 1, PLANTEIS, Disciplina.vazia(), escalacoes, List.of());
	}

	private PartidaDaRodada partidaDoUsuario(RodadaAoVivo rodada) {
		return rodada.partidas().stream().filter(p -> p.jogo().mandante().id() == doUsuario.id()).findFirst().orElseThrow();
	}
}
