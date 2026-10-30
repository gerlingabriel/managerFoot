package br.com.managerfoot.temporada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import br.com.managerfoot.bancopadrao.Regiao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escudo;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.ModeloEscudo;

class MundialTest {

	static final Escudo ESCUDO = new Escudo(ModeloEscudo.values()[0], "#FFFFFF", "#000000");
	static final Clube CAMPEAO = clube(1, "Campeão");
	static final Clube EUROPEU = clube(2, "Europeu");
	static final Clube ASIATICO = clube(3, "Asiático");
	static final Clube AFRICANO = clube(4, "Africano");
	static final Clube DA_OCEANIA = clube(5, "Da Oceania");
	static final Clube NORTE_AMERICANO = clube(6, "Norte-americano");

	static Clube clube(long id, String nome) {
		return new Clube(id, nome, 30, ESCUDO);
	}

	static Map<Regiao, List<Clube>> candidatos() {
		Map<Regiao, List<Clube>> mapa = new EnumMap<>(Regiao.class);
		mapa.put(Regiao.EUROPA, List.of(EUROPEU));
		mapa.put(Regiao.ASIA, List.of(ASIATICO));
		mapa.put(Regiao.AFRICA, List.of(AFRICANO));
		mapa.put(Regiao.OCEANIA, List.of(DA_OCEANIA));
		mapa.put(Regiao.AMERICA_DO_NORTE, List.of(NORTE_AMERICANO));
		return mapa;
	}

	static Optional<Mundial> sortear(Map<Regiao, List<Clube>> candidatos, long semente) {
		return Mundial.sortear(CAMPEAO, Regiao.AMERICA_DO_SUL, candidatos, new HashMap<Long, List<Jogador>>(), 3,
				new SplittableRandom(semente));
	}

	static PlacarCopa placar(int a, int b) {
		return new PlacarCopa(a, b, false, Optional.empty());
	}

	@Test
	void europaPegaOSorteadoEAsAmericasSeEnfrentam() {
		Mundial mundial = sortear(candidatos(), 7).orElseThrow();
		assertThat(mundial.semifinais().get(0).primeiro()).isEqualTo(EUROPEU);
		assertThat(mundial.semifinais().get(0).segundo()).isIn(ASIATICO, AFRICANO, DA_OCEANIA);
		assertThat(mundial.semifinais().get(1).primeiro()).isEqualTo(NORTE_AMERICANO);
		assertThat(mundial.semifinais().get(1).segundo()).isEqualTo(CAMPEAO);
		assertThat(mundial.decisao()).isEmpty();
	}

	@Test
	void asTresRegioesSaemNoSorteioEAMesmaSementeRepete() {
		java.util.Set<Clube> saidos = new java.util.HashSet<>();
		for (long semente = 0; semente < 60; semente++) {
			saidos.add(sortear(candidatos(), semente).orElseThrow().semifinais().get(0).segundo());
		}
		assertThat(saidos).containsExactlyInAnyOrder(ASIATICO, AFRICANO, DA_OCEANIA);
		assertThat(sortear(candidatos(), 11)).isEqualTo(sortear(candidatos(), 11));
	}

	@Test
	void campeaoDaEuropaRepresentaAEuropa() {
		Map<Regiao, List<Clube>> candidatos = candidatos();
		candidatos.remove(Regiao.EUROPA);
		candidatos.put(Regiao.AMERICA_DO_SUL, List.of(clube(7, "Sul-americano")));
		Mundial mundial = Mundial.sortear(CAMPEAO, Regiao.EUROPA, candidatos, Map.of(), 3, new SplittableRandom(1)).orElseThrow();
		assertThat(mundial.semifinais().get(0).primeiro()).isEqualTo(CAMPEAO);
		assertThat(mundial.semifinais().get(1).segundo().nome()).isEqualTo("Sul-americano");
	}

	@Test
	void semClubeDeUmaRegiaoNaoHaMundial() {
		Map<Regiao, List<Clube>> semNorte = candidatos();
		semNorte.remove(Regiao.AMERICA_DO_NORTE);
		assertThat(sortear(semNorte, 1)).isEmpty();
		Map<Regiao, List<Clube>> semSorteadas = candidatos();
		List.of(Regiao.ASIA, Regiao.AFRICA, Regiao.OCEANIA).forEach(semSorteadas::remove);
		assertThat(sortear(semSorteadas, 1)).isEmpty();
	}

	@Test
	void regiaoSorteadaSemClubesCedeOLugarAsOutras() {
		Map<Regiao, List<Clube>> candidatos = candidatos();
		candidatos.remove(Regiao.ASIA);
		candidatos.remove(Regiao.OCEANIA);
		for (long semente = 0; semente < 20; semente++) {
			assertThat(sortear(candidatos, semente).orElseThrow().semifinais().get(0).segundo()).isEqualTo(AFRICANO);
		}
	}

	@Test
	void vencedoresDasSemifinaisVaoAFinalEOCampeaoSaiDela() {
		Mundial mundial = sortear(candidatos(), 7).orElseThrow();
		Clube adversario = mundial.semifinais().get(0).segundo();
		mundial = mundial.comPlacar(FaseCopa.SEMIFINAL, 0, placar(0, 2));
		assertThatThrownBy(mundial::comFinal).isInstanceOf(IllegalStateException.class);
		mundial = mundial.comPlacar(FaseCopa.SEMIFINAL, 1,
				new PlacarCopa(1, 1, true, Optional.of(new PlacarCopa.Penaltis(3, 4)))).comFinal();
		Mundial.Confronto decisao = mundial.decisao().orElseThrow();
		assertThat(decisao.primeiro()).isEqualTo(adversario);
		assertThat(decisao.segundo()).isEqualTo(CAMPEAO);
		assertThat(mundial.campeao()).isEmpty();
		mundial = mundial.comPlacar(FaseCopa.FINAL, 0, placar(1, 3));
		assertThat(mundial.campeao()).contains(CAMPEAO);
		assertThat(mundial.vice()).contains(adversario);
	}

	@Test
	void naoAceitaEmpateSemPenaltisNemPlacarRepetido() {
		Mundial mundial = sortear(candidatos(), 7).orElseThrow();
		assertThatThrownBy(() -> mundial.comPlacar(FaseCopa.SEMIFINAL, 0, placar(1, 1)))
			.isInstanceOf(IllegalArgumentException.class);
		Mundial jogado = mundial.comPlacar(FaseCopa.SEMIFINAL, 0, placar(1, 0));
		assertThatThrownBy(() -> jogado.comPlacar(FaseCopa.SEMIFINAL, 0, placar(2, 0)))
			.isInstanceOf(IllegalStateException.class);
	}
}
