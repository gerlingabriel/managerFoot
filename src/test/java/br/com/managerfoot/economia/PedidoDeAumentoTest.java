package br.com.managerfoot.economia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.economia.PedidoDeAumento.Candidato;

class PedidoDeAumentoTest {

	private final PedidoDeAumento pedido = PedidoDeAumento.padrao();

	@Test
	void soPedeDepoisDeSubirOIntervaloAcimaDaForcaDoSalario() {
		assertThat(pedido.pede(new Candidato(jogador(1, 14), 10))).isFalse();
		assertThat(pedido.pede(new Candidato(jogador(1, 15), 10))).isTrue();
		// força abaixo da do salário: o usuário paga mais e administra (rev. 58)
		assertThat(pedido.pede(new Candidato(jogador(1, 8), 10))).isFalse();
	}

	@Test
	void pedeOQueMaisSubiuENoEmpateODeMenorId() {
		Candidato cinco = new Candidato(jogador(3, 20), 15);
		Candidato seis = new Candidato(jogador(7, 21), 15);
		Candidato seisOutro = new Candidato(jogador(5, 31), 25);
		Candidato quatro = new Candidato(jogador(1, 19), 15);

		assertThat(pedido.quemPede(List.of(cinco, seis, quatro, seisOutro))).contains(seisOutro);
		assertThat(pedido.quemPede(List.of(quatro))).isEmpty();
	}

	private static Jogador jogador(long id, int forca) {
		return new Jogador(id, "J" + id, Posicao.MEIO_CAMPISTA, forca, Temperamento.NORMAL, "BRA");
	}
}
