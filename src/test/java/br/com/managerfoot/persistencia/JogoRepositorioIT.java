package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.contas.AcessoNegadoException;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Escudo;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.ModeloEscudo;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;

/** Migrações V5 e V9 e {@link JogoRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JogoRepositorioIT {

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JdbcClient jdbc;

	long molde;
	long brasil;
	ConfiguracaoLiga ligaMolde;

	@BeforeEach
	void criaMoldeComPlanteis() {
		molde = moldes.criarMolde("Padrão");
		brasil = moldes.criarPais(molde, "BRA", "Brasil");
		ligaMolde = moldes.criarLiga(brasil, ligaDeDuasDivisoes("BR"));
		moldes.adicionarJogadores(primeiroClube(ligaMolde).id(), plantel("Titular"));
		moldes.adicionarJogadores(ligaMolde.filaDeEspera().getFirst().id(), plantel("Fila"));
	}

	@Test
	void jogoCopiaLigaEPlanteisComIdsNovos() {
		long jogo = criar(brasil);

		ConfiguracaoLiga copia = jogos.carregarLiga(jogo);
		assertThat(semIds(copia)).isEqualTo(semIds(ligaMolde));
		assertThat(primeiroClube(copia).id()).isNotEqualTo(primeiroClube(ligaMolde).id());

		Map<Long, List<Jogador>> planteis = jogos.carregarPlanteis(jogo);
		assertThat(planteis).hasSize(34);
		List<Jogador> copiados = planteis.get(primeiroClube(copia).id());
		assertThat(copiados).usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
			.containsExactlyElementsOf(moldes.carregarPlantel(primeiroClube(ligaMolde).id()));
		assertThat(planteis.get(copia.filaDeEspera().getFirst().id())).hasSize(23);
		assertThat(planteis.get(copia.divisoes().get(1).clubes().getFirst().id())).isEmpty();
		assertThat(jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = ?").param(jogo).query(String.class).single())
			.isEqualTo("BRA");
	}

	@Test
	void editarOuApagarOMoldeNaoAlteraOJogo() {
		long jogo = criar(brasil);
		ConfiguracaoLiga antes = jogos.carregarLiga(jogo);
		Map<Long, List<Jogador>> planteisAntes = jogos.carregarPlanteis(jogo);

		jdbc.sql("UPDATE molde_clube SET forca = 1, nome = 'Editado' WHERE pais_id = ?").param(brasil).update();
		jdbc.sql("UPDATE molde_jogador SET forca = 1").update();
		assertThat(jogos.carregarLiga(jogo)).isEqualTo(antes);

		jdbc.sql("DELETE FROM molde WHERE id = ?").param(molde).update();
		assertThat(jogos.carregarLiga(jogo)).isEqualTo(antes);
		assertThat(jogos.carregarPlanteis(jogo)).isEqualTo(planteisAntes);
		assertThat(jdbc.sql("SELECT molde_id FROM jogo WHERE id = ?").param(jogo).query(Long.class).optional())
			.isEmpty();
	}

	@Test
	void soOPaisEscolhidoECopiado() {
		long argentina = moldes.criarPais(molde, "ARG", "Argentina");
		moldes.criarLiga(argentina, ligaDeDuasDivisoes("AR"));

		long jogo = criar(argentina);

		assertThat(primeiroClube(jogos.carregarLiga(jogo)).nome()).startsWith("AR");
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_clube WHERE jogo_id = ?").param(jogo).query(Long.class).single())
			.isEqualTo(34);
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id "
				+ "WHERE c.jogo_id = ?").param(jogo).query(Long.class).single()).isZero();
	}

	@Test
	void doisJogosDoMesmoMoldeSaoIndependentes() {
		long primeiro = criar(brasil);
		long segundo = criar(brasil);
		ConfiguracaoLiga ligaSegundo = jogos.carregarLiga(segundo);

		jdbc.sql("DELETE FROM jogo WHERE id = ?").param(primeiro).update();

		assertThat(jogos.carregarLiga(segundo)).isEqualTo(ligaSegundo);
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_clube WHERE jogo_id = ?").param(primeiro).query(Long.class)
			.single()).isZero();
	}

	@Test
	void ligaInvalidaERecusadaComOsProblemasESemGravarNada() {
		long chile = moldes.criarPais(molde, "CHI", "Chile");
		moldes.criarLiga(chile, new ConfiguracaoLiga("Pequena",
				List.of(new Divisao("Única", 1, 50, 0, 0, clubes("CL", 3, 30))), List.of()));
		long jogosAntes = contarJogos();

		assertThatThrownBy(() -> criar(chile)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Única");
		assertThat(contarJogos()).isEqualTo(jogosAntes);
	}

	@Test
	void paisSemLigaOuInexistenteERecusado() {
		long semLiga = moldes.criarPais(molde, "URU", "Uruguai");

		assertThatThrownBy(() -> criar(semLiga)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("URU");
		assertThatThrownBy(() -> criar(-1)).isInstanceOf(NoSuchElementException.class);
		assertThatThrownBy(() -> jogos.carregarLiga(-1)).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void participanteRecebeUmDosQuatroMaisFracosDaUltimaDivisao() {
		Usuario participante = LigaDeTeste.participante(contas);
		Set<String> sorteados = new HashSet<>();
		for (long semente = 0; semente < 12; semente++) {
			long jogo = jogos.criarJogoComClubeSorteado(brasil, participante.id(), semente);
			long clube = jogos.clubeDoTreinador(jogo, participante.id()).orElseThrow();
			sorteados.add(nomeDoClube(clube));
			assertThat(jdbc.sql("SELECT dono_id FROM jogo WHERE id = ?").param(jogo).query(Long.class).single())
				.isEqualTo(participante.id());
			assertThat(jdbc.sql("SELECT semente_clube_inicial FROM jogo WHERE id = ?").param(jogo).query(Long.class)
				.single()).isEqualTo(semente);
		}
		assertThat(sorteados).isSubsetOf("BR Segunda 1", "BR Segunda 2", "BR Segunda 3", "BR Segunda 4")
			.hasSizeGreaterThan(1);
	}

	@Test
	void mesmaSementeSorteiaOMesmoClube() {
		Usuario participante = LigaDeTeste.participante(contas);
		long primeiro = jogos.criarJogoComClubeSorteado(brasil, participante.id(), 42);
		long segundo = jogos.criarJogoComClubeSorteado(brasil, participante.id(), 42);
		assertThat(nomeDoClube(jogos.clubeDoTreinador(primeiro, participante.id()).orElseThrow()))
			.isEqualTo(nomeDoClube(jogos.clubeDoTreinador(segundo, participante.id()).orElseThrow()));
	}

	@Test
	void administradorEscolheClubeDeQualquerDivisao() {
		Usuario admin = LigaDeTeste.administrador(contas);
		Clube listrado = primeiroClube(ligaMolde);

		long jogo = jogos.criarJogoComClubeEscolhido(brasil, admin.id(), listrado.id());

		long clube = jogos.clubeDoTreinador(jogo, admin.id()).orElseThrow();
		assertThat(clube).isEqualTo(primeiroClube(jogos.carregarLiga(jogo)).id());
		assertThat(jdbc.sql("SELECT semente_clube_inicial FROM jogo WHERE id = ?").param(jogo).query(Long.class)
			.optional()).isEmpty();
		assertThat(jogos.clubeDoTreinador(jogo, LigaDeTeste.participante(contas).id())).isEmpty();
	}

	@Test
	void administradorTambemPodePedirOSorteio() {
		Usuario admin = LigaDeTeste.administrador(contas);
		long jogo = jogos.criarJogoComClubeSorteado(brasil, admin.id(), 3);
		assertThat(nomeDoClube(jogos.clubeDoTreinador(jogo, admin.id()).orElseThrow())).startsWith("BR Segunda");
	}

	@Test
	void escolhaSoDoAdministradorESoDeClubeDeDivisaoDoPais() {
		Usuario admin = LigaDeTeste.administrador(contas);
		Usuario participante = LigaDeTeste.participante(contas);
		long argentina = moldes.criarPais(molde, "ARG", "Argentina");
		ConfiguracaoLiga ligaArgentina = moldes.criarLiga(argentina, ligaDeDuasDivisoes("AR"));
		long jogosAntes = contarJogos();

		assertThatThrownBy(() -> jogos.criarJogoComClubeEscolhido(brasil, participante.id(),
				primeiroClube(ligaMolde).id())).isInstanceOf(AcessoNegadoException.class);
		assertThatThrownBy(() -> jogos.criarJogoComClubeEscolhido(brasil, admin.id(),
				ligaMolde.filaDeEspera().getFirst().id())).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("divisão");
		assertThatThrownBy(() -> jogos.criarJogoComClubeEscolhido(brasil, admin.id(),
				primeiroClube(ligaArgentina).id())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> jogos.criarJogoComClubeSorteado(brasil, -1, 1))
			.isInstanceOf(NoSuchElementException.class);
		assertThat(contarJogos()).isEqualTo(jogosAntes);
	}

	@Test
	void clubeTemNoMaximoUmTreinadorHumano() {
		Usuario participante = LigaDeTeste.participante(contas);
		Usuario outro = LigaDeTeste.participante(contas);
		long jogo = criar(brasil);
		long clube = jogos.clubeDoTreinador(jogo, dono(jogo)).orElseThrow();

		assertThatThrownBy(() -> jdbc.sql("INSERT INTO jogo_treinador (jogo_id, usuario_id, clube_id) VALUES (?, ?, ?)")
			.params(jogo, participante.id(), clube).update()).isInstanceOf(DataIntegrityViolationException.class);
		long outroJogo = criar(brasil);
		long clubeDeOutroJogo = jogos.clubeDoTreinador(outroJogo, dono(outroJogo)).orElseThrow();
		assertThatThrownBy(() -> jdbc.sql("INSERT INTO jogo_treinador (jogo_id, usuario_id, clube_id) VALUES (?, ?, ?)")
			.params(jogo, outro.id(), clubeDeOutroJogo).update()).isInstanceOf(DataIntegrityViolationException.class);
	}

	private long criar(long pais) {
		return jogos.criarJogoComClubeSorteado(pais, LigaDeTeste.participante(contas).id(), 1);
	}

	private long dono(long jogo) {
		return jdbc.sql("SELECT dono_id FROM jogo WHERE id = ?").param(jogo).query(Long.class).single();
	}

	private String nomeDoClube(long clube) {
		return jdbc.sql("SELECT nome FROM jogo_clube WHERE id = ?").param(clube).query(String.class).single();
	}

	private long contarJogos() {
		return jdbc.sql("SELECT count(*) FROM jogo").query(Long.class).single();
	}

	private static Clube primeiroClube(ConfiguracaoLiga liga) {
		return liga.divisoes().getFirst().clubes().getFirst();
	}

	/** A liga com todos os ids de clube zerados, para comparar molde e cópia. */
	private static ConfiguracaoLiga semIds(ConfiguracaoLiga liga) {
		return new ConfiguracaoLiga(liga.nome(),
				liga.divisoes().stream().map(d -> new Divisao(d.nome(), d.prioridade(), d.limiteForca(), d.promovidos(),
						d.rebaixados(), d.clubes().stream().map(JogoRepositorioIT::semId).toList())).toList(),
				liga.filaDeEspera().stream().map(JogoRepositorioIT::semId).toList());
	}

	private static Clube semId(Clube c) {
		return new Clube(0, c.nome(), c.forca(), c.escudo());
	}

	/**
	 * 1ª com 16 clubes e 2 rebaixados, 2ª com 16 clubes (forças 10 a 25, nessa ordem), 2 promovidos e
	 * 2 rebaixados, fila com 2.
	 */
	private static ConfiguracaoLiga ligaDeDuasDivisoes(String prefixo) {
		List<Clube> primeira = new java.util.ArrayList<>(clubes(prefixo + " Primeira", 16, 40));
		primeira.set(0, new Clube(0, prefixo + " Listrado", 40,
				new Escudo(ModeloEscudo.LISTRAS_VERTICAIS, "#111827", "#F4F1E6")));
		return new ConfiguracaoLiga("Campeonato " + prefixo,
				List.of(new Divisao("Série A", 1, 50, 0, 2, primeira),
						new Divisao("Série B", 2, 30, 2, 2, IntStream.rangeClosed(1, 16)
							.mapToObj(i -> new Clube(0, prefixo + " Segunda " + i, 9 + i))
							.toList())),
				clubes(prefixo + " Fila", 2, 15));
	}

	private static List<Clube> clubes(String prefixo, int quantidade, int forca) {
		return IntStream.rangeClosed(1, quantidade).mapToObj(i -> new Clube(0, prefixo + " " + i, forca)).toList();
	}

	/** 23 jogadores com posições, temperamentos e nacionalidades variados; os 3 primeiros com asterisco. */
	private static List<Jogador> plantel(String prefixo) {
		Posicao[] posicoes = Posicao.values();
		Temperamento[] temperamentos = Temperamento.values();
		String[] paises = { "BRA", "POR", "ARG", "ENG" };
		return IntStream.range(0, 23)
			.mapToObj(i -> new Jogador(0, prefixo + " " + i, posicoes[i % posicoes.length], 10 + i,
					temperamentos[i % temperamentos.length], i < 3, paises[i % paises.length]))
			.toList();
	}
}
