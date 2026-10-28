package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

class PublicoDoJogoTest {

	private final PublicoDoJogo publico = PublicoDoJogo.padrao();

	@Test
	void liderContraOSegundoEncheOMeioFicaPelaMetadeEOFundoBemAbaixoMesmoContraOLider() {
		int forca = 40;
		int estadio = publico.estadioInicial(forca);
		double torcida = publico.torcida(forca);
		assertThat(publico.ocupacao(1, 2, 20, 20, 38) * torcida / estadio).isGreaterThanOrEqualTo(1.0);
		// na força cadastrada; com a evolução a força em campo cai e a simulação dá perto de 47%
		assertThat(publico.ocupacao(10, 15, 20, 20, 38) * torcida / estadio).isBetween(0.4, 0.7);
		assertThat(publico.ocupacao(20, 1, 20, 20, 38) * torcida / estadio).isLessThan(0.3);
	}

	@Test
	void ninguemComecaNoMaximoEOEstadioGrandeNuncaEnche() {
		assertThat(publico.estadioInicial(Jogador.FORCA_MAXIMA)).isLessThan(ParametrosPublico.padrao().capacidadeMaxima());
		// clube de força 30 com o estádio máximo: mesmo líder contra o 2º na última rodada
		for (int s = 0; s < 200; s++) {
			int pagantes = publico.publico(80_000, 30, 1, 2, 20, 38, 38, new SplittableRandom(s));
			assertThat(pagantes).isLessThan(40_000);
		}
	}

	@Test
	void noComecoDaTemporadaATabelaValeMenos() {
		assertThat(publico.ocupacao(20, 1, 20, 1, 38)).isEqualTo(ParametrosPublico.padrao().ocupacaoInicio());
		assertThat(publico.ocupacao(1, 2, 20, 1, 38)).isEqualTo(ParametrosPublico.padrao().ocupacaoInicio());
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

	private static List<Jogador> plantel(int forca) {
		List<Jogador> p = new ArrayList<>();
		for (long id = 1; id <= 23; id++) {
			p.add(new Jogador(id, "J" + id, Posicao.MEIO_CAMPISTA, forca, Temperamento.NORMAL, "BRA"));
		}
		return p;
	}
}
