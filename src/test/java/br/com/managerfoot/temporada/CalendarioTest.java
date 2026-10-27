package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.managerfoot.dominio.Clube;

class CalendarioTest {

	private static List<Clube> clubes(int quantidade) {
		List<Clube> clubes = new ArrayList<>();
		for (int i = 1; i <= quantidade; i++) {
			clubes.add(new Clube(i, "Clube " + i, 30));
		}
		return clubes;
	}

	@ParameterizedTest
	@ValueSource(ints = { 15, 16, 17, 18, 19, 20 })
	void cadaClubeFazNMenos1JogosEmCasaENMenos1Fora(int n) {
		List<Clube> clubes = clubes(n);
		Calendario calendario = Calendario.turnoEReturno(clubes);

		for (Clube clube : clubes) {
			long emCasa = calendario.jogos().stream().filter(j -> j.mandante().equals(clube)).count();
			long fora = calendario.jogos().stream().filter(j -> j.visitante().equals(clube)).count();
			assertThat(emCasa).as("%s em casa", clube.nome()).isEqualTo(n - 1);
			assertThat(fora).as("%s fora", clube.nome()).isEqualTo(n - 1);
		}
		assertThat(calendario.jogos()).hasSize(n * (n - 1));
	}

	@Test
	void exemplosDoUsuario20Clubes38Jogos18Clubes34Jogos() {
		Calendario vinte = Calendario.turnoEReturno(clubes(20));
		Calendario dezoito = Calendario.turnoEReturno(clubes(18));

		assertThat(vinte.rodadas()).hasSize(38);
		assertThat(vinte.jogosDe(clubes(20).getFirst())).hasSize(38);
		assertThat(dezoito.rodadas()).hasSize(34);
		assertThat(dezoito.jogosDe(clubes(18).getFirst())).hasSize(34);
	}

	@ParameterizedTest
	@ValueSource(ints = { 15, 16, 17, 18, 19, 20 })
	void cadaParSeEnfrentaUmaVezEmCadaCasa(int n) {
		Calendario calendario = Calendario.turnoEReturno(clubes(n));
		Set<String> confrontos = new HashSet<>();
		for (Jogo jogo : calendario.jogos()) {
			assertThat(jogo.mandante()).isNotEqualTo(jogo.visitante());
			assertThat(confrontos.add(jogo.mandante().id() + ">" + jogo.visitante().id()))
				.as("confronto repetido com o mesmo mando: %s x %s", jogo.mandante().nome(), jogo.visitante().nome())
				.isTrue();
		}
		assertThat(confrontos).hasSize(n * (n - 1));
	}

	@ParameterizedTest
	@ValueSource(ints = { 15, 16, 17, 18, 19, 20 })
	void cadaClubeJogaNoMaximoUmaVezPorRodadaEOImparFolgaUmaVezPorTurno(int n) {
		List<Clube> clubes = clubes(n);
		Calendario calendario = Calendario.turnoEReturno(clubes);
		Map<Clube, Integer> folgas = new HashMap<>();

		for (Rodada rodada : calendario.rodadas()) {
			Set<Clube> jogaram = new HashSet<>();
			for (Jogo jogo : rodada.jogos()) {
				assertThat(jogaram.add(jogo.mandante())).isTrue();
				assertThat(jogaram.add(jogo.visitante())).isTrue();
				assertThat(jogo.rodada()).isEqualTo(rodada.numero());
			}
			rodada.folga().ifPresent(c -> {
				assertThat(jogaram).doesNotContain(c);
				folgas.merge(c, 1, Integer::sum);
			});
			assertThat(jogaram.size() + (rodada.folga().isPresent() ? 1 : 0)).isEqualTo(n);
		}
		if (n % 2 == 1) {
			assertThat(folgas).hasSize(n).allSatisfy((clube, vezes) -> assertThat(vezes).isEqualTo(2));
		} else {
			assertThat(folgas).isEmpty();
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { 15, 18, 20 })
	void returnoRepeteOTurnoComMandoInvertido(int n) {
		Calendario calendario = Calendario.turnoEReturno(clubes(n));
		int rodadasPorTurno = calendario.rodadas().size() / 2;

		for (int r = 0; r < rodadasPorTurno; r++) {
			Rodada ida = calendario.rodadas().get(r);
			Rodada volta = calendario.rodadas().get(r + rodadasPorTurno);
			assertThat(volta.numero()).isEqualTo(ida.numero() + rodadasPorTurno);
			assertThat(volta.folga()).isEqualTo(ida.folga());
			for (int j = 0; j < ida.jogos().size(); j++) {
				assertThat(volta.jogos().get(j).mandante()).isEqualTo(ida.jogos().get(j).visitante());
				assertThat(volta.jogos().get(j).visitante()).isEqualTo(ida.jogos().get(j).mandante());
			}
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { 16, 18, 20 })
	void turnoTemOMinimoTeoricoDeMandosRepetidos(int n) {
		List<Clube> clubes = clubes(n);
		Calendario calendario = Calendario.turnoEReturno(clubes);
		List<Rodada> turno = calendario.rodadas().subList(0, n - 1);

		int repeticoes = 0;
		for (Clube clube : clubes) {
			Boolean anteriorEmCasa = null;
			for (Rodada rodada : turno) {
				for (Jogo jogo : rodada.jogos()) {
					if (jogo.envolve(clube)) {
						boolean emCasa = jogo.mandante().equals(clube);
						if (anteriorEmCasa != null && anteriorEmCasa == emCasa) {
							repeticoes++;
						}
						anteriorEmCasa = emCasa;
					}
				}
			}
		}
		// em pontos corridos com N par, o mínimo possível é N-2 repetições
		assertThat(repeticoes).isEqualTo(n - 2);
	}

	@Test
	void mesmaOrdemDeClubesGeraOMesmoCalendario() {
		assertThat(Calendario.turnoEReturno(clubes(17))).isEqualTo(Calendario.turnoEReturno(clubes(17)));
	}

	@Test
	void recusaListaPequenaOuComRepetidos() {
		assertThatIllegalArgumentException().isThrownBy(() -> Calendario.turnoEReturno(clubes(1)))
			.withMessageContaining("ao menos 2 clubes");
		List<Clube> repetidos = new ArrayList<>(clubes(16));
		repetidos.set(5, repetidos.get(4));
		assertThatIllegalArgumentException().isThrownBy(() -> Calendario.turnoEReturno(repetidos))
			.withMessageContaining("repetidos");
	}
}
