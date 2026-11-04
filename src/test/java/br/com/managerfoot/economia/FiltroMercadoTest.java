package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.economia.FiltroMercado.Asterisco;
import br.com.managerfoot.economia.FiltroMercado.Oferta;
import br.com.managerfoot.economia.FiltroMercado.Ordem;

class FiltroMercadoTest {

	private static final Oferta MEIA_FORTE = oferta(1, "Ariel", Posicao.MEIO_CAMPISTA, 48, true, 276_250_00L, 6);
	private static final Oferta MEIA_BARATO = oferta(2, "Paz", Posicao.MEIO_CAMPISTA, 34, false, 61_200_00L, 1);
	private static final Oferta ATACANTE = oferta(3, "Valdez", Posicao.ATACANTE, 46, false, 240_500_00L, 9);

	@Test
	void padraoSoMostraQuemCabeNoCaixa() {
		FiltroMercado padrao = FiltroMercado.padrao();

		assertThat(padrao.aceita(MEIA_BARATO, 100_000_00L)).isTrue();
		assertThat(padrao.aceita(MEIA_FORTE, 100_000_00L)).isFalse();
		assertThat(new FiltroMercado(Set.of(), null, null, null, null, Asterisco.TODOS, Ordem.FORCA, false).aceita(MEIA_FORTE, 0))
			.isTrue();
	}

	@Test
	void posicaoForcaPrecoEAsterisco() {
		long caixa = 1_000_000_00L;
		FiltroMercado meias30a50 = new FiltroMercado(Set.of(Posicao.MEIO_CAMPISTA), 30, 50, null, 300_000_00L, Asterisco.TODOS, Ordem.FORCA, true);
		assertThat(List.of(MEIA_FORTE, MEIA_BARATO, ATACANTE).stream().filter(o -> meias30a50.aceita(o, caixa)))
			.containsExactly(MEIA_FORTE, MEIA_BARATO);

		FiltroMercado acimaDe40 = new FiltroMercado(Set.of(), 40, null, 100_000_00L, null, Asterisco.SEM, Ordem.FORCA, true);
		assertThat(List.of(MEIA_FORTE, MEIA_BARATO, ATACANTE).stream().filter(o -> acimaDe40.aceita(o, caixa))).containsExactly(ATACANTE);

		FiltroMercado soComAsterisco = new FiltroMercado(Set.of(), null, null, null, null, Asterisco.COM, Ordem.FORCA, false);
		assertThat(soComAsterisco.aceita(MEIA_FORTE, 0)).isTrue();
		assertThat(soComAsterisco.aceita(ATACANTE, 0)).isFalse();
	}

	@Test
	void ordemPorForcaPrecoOuGols() {
		List<Oferta> todas = List.of(MEIA_BARATO, ATACANTE, MEIA_FORTE);

		assertThat(todas.stream().sorted(filtro(Ordem.FORCA).comparador())).containsExactly(MEIA_FORTE, ATACANTE, MEIA_BARATO);
		assertThat(todas.stream().sorted(filtro(Ordem.PRECO).comparador())).containsExactly(MEIA_BARATO, ATACANTE, MEIA_FORTE);
		assertThat(todas.stream().sorted(filtro(Ordem.GOLS).comparador())).containsExactly(ATACANTE, MEIA_FORTE, MEIA_BARATO);
	}

	private static FiltroMercado filtro(Ordem ordem) {
		return new FiltroMercado(Set.of(), null, null, null, null, Asterisco.TODOS, ordem, false);
	}

	private static Oferta oferta(long id, String nome, Posicao posicao, int forca, boolean asterisco, long preco, int gols) {
		return new Oferta(new Jogador(id, nome, posicao, forca, Temperamento.NORMAL, asterisco, "BRA"), preco, gols);
	}
}
