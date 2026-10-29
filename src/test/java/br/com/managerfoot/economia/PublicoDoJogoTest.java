package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

class PublicoDoJogoTest {

	private static final OptionalDouble SEM = OptionalDouble.empty();

	private final PublicoDoJogo publico = PublicoDoJogo.padrao();

	@Test
	void liderContraOSegundoEncheOMeioFicaPelaMetadeEOFundoBemAbaixoMesmoContraOLider() {
		int forca = 40;
		int estadio = publico.estadioInicial(forca);
		double torcida = publico.torcida(forca);
		assertThat(publico.ocupacao(1, 2, 20, 20, 38, SEM, SEM) * torcida / estadio).isGreaterThanOrEqualTo(1.0);
		// na força cadastrada; com a evolução a força em campo cai e a simulação dá perto de 47%
		assertThat(publico.ocupacao(10, 15, 20, 20, 38, SEM, SEM) * torcida / estadio).isBetween(0.4, 0.7);
		assertThat(publico.ocupacao(20, 1, 20, 20, 38, SEM, SEM) * torcida / estadio).isLessThan(0.3);
	}

	@Test
	void ninguemComecaNoMaximoEOEstadioGrandeNuncaEnche() {
		assertThat(publico.estadioInicial(Jogador.FORCA_MAXIMA)).isLessThan(ParametrosPublico.padrao().capacidadeMaxima());
		// clube de força 30 com o estádio máximo: mesmo líder contra o 2º na última rodada
		for (int s = 0; s < 200; s++) {
			int pagantes = publico.publico(80_000, 30, 1, 2, 20, 38, 38, SEM, SEM, new SplittableRandom(s));
			assertThat(pagantes).isLessThan(40_000);
		}
	}

	@Test
	void naPrimeiraTemporadaOComecoNaoDependeDaTabela() {
		assertThat(publico.ocupacao(20, 1, 20, 1, 38, SEM, SEM)).isEqualTo(ParametrosPublico.padrao().ocupacaoInicio());
		assertThat(publico.ocupacao(1, 2, 20, 1, 38, SEM, SEM)).isEqualTo(ParametrosPublico.padrao().ocupacaoInicio());
	}

	@Test
	void noComecoDaTemporadaValeAPosicaoFinalDaPassadaEDaRodada5EmDianteATabela() {
		OptionalDouble campeao = OptionalDouble.of(PublicoDoJogo.relativa(1, 20));
		OptionalDouble lanterna = OptionalDouble.of(PublicoDoJogo.relativa(20, 20));
		// rodada 1: o campeão passado lota mais que o lanterna passado, qualquer que seja a tabela
		double campeaoEmCasa = publico.ocupacao(20, 19, 20, 1, 38, campeao, lanterna);
		double lanternaEmCasa = publico.ocupacao(1, 2, 20, 1, 38, lanterna, campeao);
		assertThat(campeaoEmCasa).isGreaterThan(lanternaEmCasa);
		assertThat(campeaoEmCasa).isEqualTo(ParametrosPublico.padrao().ocupacaoLider());
		// rodada 5: só a tabela atual
		assertThat(publico.ocupacao(20, 19, 20, 5, 38, campeao, lanterna))
			.isEqualTo(publico.ocupacao(20, 19, 20, 5, 38, lanterna, campeao));
		// no meio do caminho, entre os dois
		assertThat(publico.ocupacao(20, 19, 20, 3, 38, campeao, lanterna)).isBetween(publico.ocupacao(20, 19, 20, 5, 38, SEM, SEM),
				campeaoEmCasa);
	}

	@Test
	void maquinaAmpliaQuandoATorcidaCresceuENaoSemCaixa() {
		AmpliacaoEstadio ampliacao = AmpliacaoEstadio.padrao();
		List<Jogador> plantel = plantel(45);
		long rico = 100 * ampliacao.custo();
		// estádio pequeno para a torcida de força 45
		assertThat(ampliacao.maquinaAmplia(20_000, 45, rico, plantel)).isTrue();
		assertThat(ampliacao.maquinaAmplia(20_000, 45, ampliacao.custo(), plantel)).isFalse();
		// já do tamanho da torcida e sem dinheiro sobrando: não amplia
		assertThat(ampliacao.maquinaAmplia(publico.torcida(45), 45, 2 * ampliacao.custo(), plantel)).isFalse();
		// no máximo nunca
		assertThat(ampliacao.maquinaAmplia(80_000, 50, rico, plantel)).isFalse();
	}

	@Test
	void propostasDePatrocinioPagamATabelaNoClubeMedio() {
		PropostasPatrocinio propostas = PropostasPatrocinio.padrao();
		List<PropostasPatrocinio.Proposta> lista = propostas.propostas(200_000_00L, 38);
		assertThat(lista.get(0).total(0, false)).isEqualTo(200_000_00L);
		int medio = (int) Math.round(38 * propostas.vitoriasEsperadas());
		assertThat(lista.get(1).total(medio, false)).isBetween(190_000_00L, 210_000_00L);
		assertThat(lista.get(2).total(0, false)).isEqualTo(100_000_00L);
		assertThat(lista.get(2).total(0, true)).isEqualTo(400_000_00L);
	}

	@Test
	void bonusDeAcessoNaPrimeiraValeParaOsTresPrimeirosEOsPatrocinadoresNaoSeRepetem() {
		PropostasPatrocinio propostas = PropostasPatrocinio.padrao();
		assertThat(propostas.subiu(1, 3, 0)).isTrue();
		assertThat(propostas.subiu(1, 4, 0)).isFalse();
		assertThat(propostas.subiu(2, 2, 2)).isTrue();
		assertThat(propostas.subiu(2, 3, 2)).isFalse();
		assertThat(propostas.patrocinadores(7)).hasSize(3).doesNotHaveDuplicates().isEqualTo(propostas.patrocinadores(7));
	}

	private static List<Jogador> plantel(int forca) {
		List<Jogador> p = new ArrayList<>();
		for (long id = 1; id <= 23; id++) {
			p.add(new Jogador(id, "J" + id, Posicao.MEIO_CAMPISTA, forca, Temperamento.NORMAL, "BRA"));
		}
		return p;
	}
}
