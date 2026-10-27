package br.com.managerfoot.motor;

import static br.com.managerfoot.motor.EscalacoesDeTeste.comAsteriscos;
import static br.com.managerfoot.motor.EscalacoesDeTeste.escalacao;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;

/** Efeitos do asterisco e do bônus de artilharia (SPEC §5.2, §5.3), com sementes fixas. */
class AsteriscoTest {

	private static final int PARTIDAS = 4000;
	private final MotorPartida motor = new MotorPartida(ParametrosMotor.padrao());

	/** Gols de cada jogador do mandante em PARTIDAS jogos. */
	private Map<Long, Integer> golsPorJogador(Escalacao casa, Escalacao fora, CondicoesPartida condicoes) {
		Map<Long, Integer> gols = new java.util.HashMap<>();
		for (long s = 0; s < PARTIDAS; s++) {
			for (EventoPartida e : motor.simular(RegrasPartida.padrao(), casa, fora, condicoes, s).eventos()) {
				if (e.tipo() == TipoEvento.GOL && e.lado() == Lado.CASA && !e.penalti()) {
					gols.merge(e.jogador().id(), 1, Integer::sum);
				}
			}
		}
		return gols;
	}

	private static long primeiroAtacante(Escalacao e, int indice) {
		return e.jogadoresNa(Posicao.ATACANTE).get(indice).id();
	}

	@Test
	void atacanteComAsteriscoMarcaMaisQueOParceiroDeMesmaForca() {
		Escalacao casa = comAsteriscos(1, 25, 1);
		Map<Long, Integer> gols = golsPorJogador(casa, escalacao(Formacao.F_4_4_2, 25, 100), CondicoesPartida.NEUTRAS);

		double razao = (double) gols.get(primeiroAtacante(casa, 0)) / gols.get(primeiroAtacante(casa, 1));
		// 1,5 na escolha × 1,15 na conversão ≈ 1,7
		assertThat(razao).isBetween(1.45, 2.0);
	}

	@Test
	void bonusDeArtilhariaSomaAoAsterisco() {
		Escalacao casa = comAsteriscos(2, 25, 1);
		long comBonus = primeiroAtacante(casa, 0);
		long semBonus = primeiroAtacante(casa, 1);
		Map<Long, Integer> gols = golsPorJogador(casa, escalacao(Formacao.F_4_4_2, 25, 100),
				new CondicoesPartida(Map.of(), Set.of(comBonus)));

		double razao = (double) gols.get(comBonus) / gols.get(semBonus);
		// conversão 1,30 contra 1,15 ≈ 1,13
		assertThat(razao).isBetween(1.05, 1.25);
	}

	@Test
	void goleiroComAsteriscoDefendeMaisPenaltis() {
		Escalacao casa = escalacao(Formacao.F_4_4_2, 25, 1);
		Escalacao foraNormal = escalacao(Formacao.F_4_4_2, 25, 100);
		List<Jogador> titulares = new ArrayList<>(foraNormal.titulares());
		Jogador g = foraNormal.jogadoresNa(Posicao.GOLEIRO).getFirst();
		titulares.set(titulares.indexOf(g), new Jogador(g.id(), g.nome(), g.posicao(), g.forca(), g.temperamento(), true));
		Escalacao foraAsterisco = new Escalacao(Formacao.F_4_4_2, titulares);

		assertThat(conversaoPenaltisDoMandante(casa, foraNormal)).isBetween(0.71, 0.79);
		assertThat(conversaoPenaltisDoMandante(casa, foraAsterisco)).isBetween(0.61, 0.69);
	}

	private double conversaoPenaltisDoMandante(Escalacao casa, Escalacao fora) {
		int cobrados = 0;
		int convertidos = 0;
		for (long s = 0; s < 3 * PARTIDAS; s++) {
			for (EventoPartida e : motor.simular(casa, fora, s).eventos()) {
				if (e.penalti() && e.lado() == Lado.CASA) {
					cobrados++;
					convertidos += e.tipo() == TipoEvento.GOL ? 1 : 0;
				}
			}
		}
		return (double) convertidos / cobrados;
	}

	@Test
	void asteriscosAlemDoTerceiroQuaseNaoRendemPontos() {
		double tres = pontosPorJogo(comAsteriscos(3, 25, 1));
		double zero = pontosPorJogo(comAsteriscos(0, 25, 1));
		double seis = pontosPorJogo(comAsteriscos(6, 25, 1));

		assertThat(tres).isGreaterThan(zero + 0.05);
		assertThat(seis - tres).isBetween(-0.03, 0.015);
	}

	private double pontosPorJogo(Escalacao time) {
		Escalacao adversario = escalacao(Formacao.F_4_4_2, 25, 100);
		EstatisticasLote casa = EstatisticasLote.medir(motor, time, adversario, 0, 2 * PARTIDAS);
		EstatisticasLote fora = EstatisticasLote.medir(motor, adversario, time, 0, 2 * PARTIDAS);
		return (3.0 * casa.vitoriasCasa + casa.empates + 3.0 * fora.vitoriasFora + fora.empates) / (4.0 * PARTIDAS);
	}
}
