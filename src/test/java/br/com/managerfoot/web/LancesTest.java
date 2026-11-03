package br.com.managerfoot.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;
import br.com.managerfoot.motor.TipoEvento;

class LancesTest {

	private final Lances lances = new Lances();
	private final Jogador batedor = new Jogador(1, "Moreira", Posicao.ATACANTE, 30, Temperamento.NORMAL, "BRA");
	private final Jogador goleiro = new Jogador(2, "Castanheira", Posicao.GOLEIRO, 28, Temperamento.NORMAL, "BRA");

	@Test
	void penaltiDefendidoFicaComOGoleiroDoOutroLado() {
		EventoPartida defendido = new EventoPartida(41, Lado.CASA, TipoEvento.DEFESA, batedor, true, null, null, goleiro);

		assertThat(lances.texto(defendido)).isEqualTo("Castanheira defende pênalti 41'");
		assertThat(lances.nome(defendido)).isEqualTo("Castanheira defende pênalti de Moreira");
		assertThat(lances.daCasa(defendido)).isFalse();
		assertThat(lances.icone(defendido)).isEqualTo("defesa");
		assertThat(lances.descricao(defendido)).isEqualTo("Pênalti defendido por Castanheira");
	}

	@Test
	void golDePenaltiContinuaComOBatedor() {
		EventoPartida gol = new EventoPartida(41, Lado.FORA, TipoEvento.GOL, batedor, true);

		assertThat(lances.texto(gol)).isEqualTo("Moreira 41' (pên.)");
		assertThat(lances.nome(gol)).isEqualTo("Moreira (pên.)");
		assertThat(lances.daCasa(gol)).isFalse();
		assertThat(lances.icone(gol)).isEqualTo("gol");
		assertThat(lances.descricao(gol)).isEqualTo("Gol de Moreira");
	}
}
