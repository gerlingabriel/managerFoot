package br.com.managerfoot.motor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Formacao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

class EscalacaoMaquinaTest {

	private final EscalacaoMaquina maquina = new EscalacaoMaquina(ParametrosMotor.padrao());
	private long proximoId = 1;

	@Test
	void plantelPadraoComMeiasFortesEscolheFormacaoComMaisMeias() {
		List<Jogador> plantel = new ArrayList<>();
		plantel.addAll(jogadores(Posicao.GOLEIRO, 30, 30, 30));
		plantel.addAll(jogadores(Posicao.DEFENSOR, 20, 20, 20, 20, 20, 20, 20));
		plantel.addAll(jogadores(Posicao.MEIO_CAMPISTA, 40, 40, 40, 40, 40, 40, 40));
		plantel.addAll(jogadores(Posicao.ATACANTE, 10, 10, 10, 10, 10, 10));

		Escalacao escalacao = maquina.escalar(plantel);

		assertThat(escalacao.formacao()).isEqualTo(Formacao.F_3_6_1);
		assertThat(escalacao.reservas()).hasSize(Escalacao.MAXIMO_RESERVAS);
		assertThat(escalacao.reservas().getFirst().posicao()).isEqualTo(Posicao.GOLEIRO);
	}

	@Test
	void escolheOsMaisFortesDeCadaPosicaoEEmpateFicaComAPrimeiraFormacao() {
		List<Jogador> plantel = new ArrayList<>();
		plantel.addAll(jogadores(Posicao.GOLEIRO, 25, 20));
		plantel.addAll(jogadores(Posicao.DEFENSOR, 25, 25, 25, 25, 25, 5));
		plantel.addAll(jogadores(Posicao.MEIO_CAMPISTA, 25, 25, 25, 25, 25, 5));
		plantel.addAll(jogadores(Posicao.ATACANTE, 25, 25, 25, 25, 5));

		Escalacao escalacao = maquina.escalar(plantel);

		assertThat(escalacao.formacao()).isEqualTo(Formacao.F_4_4_2);
		assertThat(escalacao.titulares()).allMatch(j -> j.forca() == 25);
	}

	@Test
	void naoPassaDeTresAsteriscosQuandoHaTroca() {
		List<Jogador> plantel = new ArrayList<>(jogadores(Posicao.GOLEIRO, 28, 20));
		plantel.addAll(jogadores(Posicao.DEFENSOR, 25, 25, 25, 25, 25));
		plantel.addAll(jogadores(Posicao.MEIO_CAMPISTA, 25, 25, 25, 25, 25));
		plantel.add(asterisco(Posicao.ATACANTE, 40));
		plantel.add(asterisco(Posicao.ATACANTE, 39));
		plantel.add(asterisco(Posicao.MEIO_CAMPISTA, 38));
		plantel.add(asterisco(Posicao.MEIO_CAMPISTA, 30));
		plantel.addAll(jogadores(Posicao.ATACANTE, 20, 20));

		Escalacao escalacao = maquina.escalar(plantel);

		assertThat(escalacao.titulares().stream().filter(Jogador::asterisco)).hasSize(3);
		// 4-4-2 com 4 asteriscos: sai o que custa menos força, o meia de 30, e entra um meia de 25
		assertThat(escalacao.formacao()).isEqualTo(Formacao.F_4_4_2);
		assertThat(escalacao.titulares()).extracting(Jogador::forca).doesNotContain(30).contains(40, 39, 38);
	}

	@Test
	void resultadoNaoDependeDaOrdemDoPlantel() {
		List<Jogador> plantel = new ArrayList<>();
		plantel.addAll(jogadores(Posicao.GOLEIRO, 30, 22, 18));
		plantel.addAll(jogadores(Posicao.DEFENSOR, 31, 27, 25, 25, 20, 18, 12));
		plantel.addAll(jogadores(Posicao.MEIO_CAMPISTA, 35, 29, 29, 24, 21, 17, 15));
		plantel.addAll(jogadores(Posicao.ATACANTE, 38, 33, 26, 19, 16, 10));
		List<Jogador> embaralhado = new ArrayList<>(plantel);
		Collections.shuffle(embaralhado, new Random(3));

		assertThat(maquina.escalar(embaralhado)).isEqualTo(maquina.escalar(plantel));
	}

	@Test
	void semGoleiroODefensorMaisFracoVaiProGolComMetadeDaForca() {
		List<Jogador> plantel = new ArrayList<>(jogadores(Posicao.DEFENSOR, 30, 30, 30, 30, 30, 21));
		plantel.addAll(jogadores(Posicao.MEIO_CAMPISTA, 30, 30, 30, 30, 30));
		plantel.addAll(jogadores(Posicao.ATACANTE, 30, 30, 30));
		long idDoMaisFraco = plantel.get(5).id();

		Escalacao escalacao = maquina.escalar(plantel);

		Jogador goleiro = escalacao.jogadoresNa(Posicao.GOLEIRO).getFirst();
		assertThat(goleiro.id()).isEqualTo(idDoMaisFraco);
		assertThat(goleiro.forca()).isEqualTo(11);
	}

	@Test
	void faltandoJogadorDeLinhaOMaisForteQueSobrouCompletaOSetor() {
		// só 2 defensores: nenhuma formação fecha; a 3-x-x falta 1 defensor
		List<Jogador> plantel = new ArrayList<>(jogadores(Posicao.GOLEIRO, 30, 10));
		plantel.addAll(jogadores(Posicao.DEFENSOR, 30, 30));
		plantel.addAll(jogadores(Posicao.MEIO_CAMPISTA, 30, 30, 30, 30, 30, 30, 22));
		plantel.addAll(jogadores(Posicao.ATACANTE, 30, 30, 30));

		Escalacao escalacao = maquina.escalar(plantel);

		assertThat(escalacao.titulares()).hasSize(11);
		assertThat(escalacao.jogadoresNa(Posicao.DEFENSOR)).hasSize(escalacao.formacao().quantidade(Posicao.DEFENSOR));
		assertThat(escalacao.reservas()).noneMatch(r -> escalacao.titulares().stream().anyMatch(t -> t.id() == r.id()));
	}

	@Test
	void menosDeOnzeDisponiveisERecusado() {
		List<Jogador> plantel = new ArrayList<>(jogadores(Posicao.GOLEIRO, 30));
		plantel.addAll(jogadores(Posicao.DEFENSOR, 30, 30, 30, 30, 30, 30, 30, 30, 30));

		assertThatIllegalStateException().isThrownBy(() -> maquina.escalar(plantel)).withMessageContaining("há 10");
	}

	private List<Jogador> jogadores(Posicao posicao, int... forcas) {
		List<Jogador> lista = new ArrayList<>();
		for (int forca : forcas) {
			long id = proximoId++;
			lista.add(new Jogador(id, posicao.name() + id, posicao, forca, Temperamento.NORMAL, "BRA"));
		}
		return lista;
	}

	private Jogador asterisco(Posicao posicao, int forca) {
		return jogadores(posicao, forca).getFirst().comAsterisco(true);
	}
}
