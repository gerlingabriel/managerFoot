package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.contas.AcessoNegadoException;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.multiplayer.ModoRede;
import br.com.managerfoot.multiplayer.Sala.Participante;
import br.com.managerfoot.multiplayer.Sala.Situacao;
import br.com.managerfoot.persistencia.SalaRepositorio.Convite;
import br.com.managerfoot.persistencia.SalaRepositorio.VisaoSala;

/** Migração V41 e {@link SalaRepositorio}: sala, convite, entrada, escolha, tirar e começar (SPEC §3.3, rev. 92). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SalaRepositorioIT {

	private static final Instant AGORA = Instant.parse("2026-11-03T20:00:00Z");
	private static final Duration TRINTA = Duration.ofSeconds(30);

	@Autowired
	SalaRepositorio salas;

	@Autowired
	JogosDoTreinadorRepositorio jogosDoTreinador;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	RodadaAoVivoRepositorio aoVivo;

	@Autowired
	SaveRepositorio saves;

	@Autowired
	JdbcClient jdbc;

	long pais;
	Usuario admin;
	Usuario marcos;
	Usuario lucia;

	@BeforeEach
	void criarPaisEContas() {
		pais = LigaDeTeste.criarPais(moldes);
		admin = LigaDeTeste.administrador(contas);
		marcos = LigaDeTeste.participante(contas);
		lucia = LigaDeTeste.participante(contas);
	}

	@Test
	void jogoEmRedeFicaNaSalaSemPatrocinioESoOAdministradorDentro() {
		long jogo = jogosDoTreinador.criarEmRedeComSorteio(pais, admin.id(), AGORA).jogoId();

		assertThat(salas.naSala(jogo)).isTrue();
		VisaoSala v = salas.visao(jogo, admin.id()).orElseThrow();
		assertThat(v.administrador()).isTrue();
		assertThat(v.sala().modo()).isEqualTo(ModoRede.SORTEIO);
		assertThat(v.sala().participantes()).singleElement().satisfies(p -> {
			assertThat(p.administrador()).isTrue();
			assertThat(p.clube()).isPresent();
		});
		assertThat(v.convite()).hasValueSatisfying(c -> assertThat(c.valeAte()).isEqualTo(AGORA.plus(Duration.ofDays(7))));
		// patrocínio e salários só ao começar
		assertThat(contar("SELECT count(*) FROM jogo_patrocinio p JOIN jogo_clube c ON c.id = p.clube_id WHERE c.jogo_id = :jogo", jogo)).isZero();
		assertThat(salas.visao(jogo, marcos.id())).isEmpty();
	}

	@Test
	void participanteNaoCriaJogoEmRede() {
		assertThatThrownBy(() -> jogosDoTreinador.criarEmRedeComSorteio(pais, marcos.id(), AGORA))
			.isInstanceOf(AcessoNegadoException.class);
	}

	@Test
	void sorteioDaClubeLivreNaUltimaDivisaoAoEntrarERepetirSoVoltaASala() {
		long jogo = jogosDoTreinador.criarEmRedeComSorteio(pais, admin.id(), AGORA).jogoId();
		String token = salas.visao(jogo, admin.id()).orElseThrow().convite().orElseThrow().token();

		assertThat(salas.convite(token, marcos.id(), AGORA)).hasValueSatisfying(c -> {
			assertThat(c).isInstanceOf(Convite.Valido.class);
			assertThat(((Convite.Valido) c).grupoSorteio()).isEqualTo(5);
			assertThat(((Convite.Valido) c).naSala()).containsExactly("Admin");
		});
		assertThat(salas.entrar(token, marcos.id(), AGORA.plusSeconds(1))).isEqualTo(jogo);
		assertThat(salas.entrar(token, marcos.id(), AGORA.plusSeconds(2))).isEqualTo(jogo);

		long clubeAdmin = jogos.clubeDoTreinador(jogo, admin.id()).orElseThrow();
		long clubeMarcos = jogos.clubeDoTreinador(jogo, marcos.id()).orElseThrow();
		assertThat(clubeMarcos).isNotEqualTo(clubeAdmin);
		assertThat(jogos.carregarLiga(jogo).divisoes().getLast().clubes()).extracting(Clube::id).contains(clubeMarcos);
		assertThat(salas.convite(token, marcos.id(), AGORA)).containsInstanceOf(Convite.JaNaSala.class);
		assertThat(salas.visao(jogo, marcos.id()).orElseThrow().convite()).as("só o administrador vê o link").isEmpty();
	}

	@Test
	void modoEscolhaEntraSemClubeEClubeOcupadoFicaBloqueado() {
		long jogo = criarComEscolha();
		String token = token(jogo);
		long clubeAdmin = jogos.clubeDoTreinador(jogo, admin.id()).orElseThrow();

		salas.entrar(token, marcos.id(), AGORA);
		assertThat(jogos.clubeDoTreinador(jogo, marcos.id())).isEmpty();
		assertThat(jogosDoTreinador.salasSemClube(marcos.id())).singleElement()
			.satisfies(s -> assertThat(s.jogoId()).isEqualTo(jogo));
		assertThat(situacao(jogo, marcos, AGORA)).isEqualTo(Situacao.ESCOLHENDO);

		assertThatThrownBy(() -> salas.escolher(jogo, marcos.id(), clubeAdmin))
			.isInstanceOf(SalaRecusadaException.class).hasMessageContaining("já tem treinador");
		long fila = jogos.carregarLiga(jogo).filaDeEspera().getFirst().id();
		assertThatThrownBy(() -> salas.escolher(jogo, marcos.id(), fila))
			.isInstanceOf(SalaRecusadaException.class).hasMessageContaining("divisão");

		long livre = jogos.carregarLiga(jogo).divisoes().getFirst().clubes().stream().mapToLong(Clube::id)
			.filter(id -> id != clubeAdmin).findFirst().orElseThrow();
		salas.escolher(jogo, marcos.id(), livre);
		assertThat(jogos.clubeDoTreinador(jogo, marcos.id())).hasValue(livre);
		assertThat(situacao(jogo, marcos, AGORA)).isEqualTo(Situacao.PRONTO);
		assertThat(jogosDoTreinador.listar(marcos.id())).singleElement().satisfies(j -> {
			assertThat(j.rede()).hasValueSatisfying(r -> {
				assertThat(r.naSala()).isTrue();
				assertThat(r.souAdministrador()).isFalse();
				assertThat(r.treinadores()).isEqualTo(2);
				assertThat(r.administrador()).isEqualTo("Admin");
			});
		});
		assertThatThrownBy(() -> salas.escolher(jogo, marcos.id(), livre)).isInstanceOf(SalaRecusadaException.class);
	}

	@Test
	void linkNovoCancelaOAnteriorEVencidoNaoEntra() {
		long jogo = criarComEscolha();
		String antigo = token(jogo);
		String novo = salas.novoConvite(jogo, admin.id(), AGORA.plusSeconds(60));

		assertThat(novo).isNotEqualTo(antigo);
		assertThat(salas.convite(antigo, marcos.id(), AGORA)).hasValueSatisfying(c -> assertThat(((Convite.Invalido) c).motivo())
			.isEqualTo(Convite.Motivo.TROCADO));
		assertThatThrownBy(() -> salas.entrar(antigo, marcos.id(), AGORA)).isInstanceOf(SalaRecusadaException.class);
		Instant depois = AGORA.plusSeconds(60).plus(Duration.ofDays(7));
		assertThat(salas.convite(novo, marcos.id(), depois)).hasValueSatisfying(c -> assertThat(((Convite.Invalido) c).motivo())
			.isEqualTo(Convite.Motivo.VENCEU));
		assertThat(salas.convite("nao-existe-este-token-de-convite", marcos.id(), AGORA)).isEmpty();
		assertThatThrownBy(() -> salas.novoConvite(jogo, marcos.id(), AGORA)).isInstanceOf(AcessoNegadoException.class);
	}

	@Test
	void administradorTiraAlguemEOClubeVoltaAMaquina() {
		long jogo = jogosDoTreinador.criarEmRedeComSorteio(pais, admin.id(), AGORA).jogoId();
		salas.entrar(token(jogo), marcos.id(), AGORA);
		long clube = jogos.clubeDoTreinador(jogo, marcos.id()).orElseThrow();

		assertThatThrownBy(() -> salas.tirar(jogo, marcos.id(), admin.id())).isInstanceOf(AcessoNegadoException.class);
		assertThatThrownBy(() -> salas.tirar(jogo, admin.id(), admin.id())).isInstanceOf(SalaRecusadaException.class);
		salas.tirar(jogo, admin.id(), marcos.id());

		assertThat(salas.visao(jogo, marcos.id())).isEmpty();
		assertThat(salas.ocupados(jogo)).doesNotContain(clube);
		assertThat(contar("SELECT count(*) FROM jogo_escalacao WHERE clube_id = :jogo", clube)).isZero();
	}

	@Test
	void comecaSoComTodosProntosEFixaPatrocinioDeTodosOsUsuarios() {
		long jogo = criarComEscolha();
		salas.entrar(token(jogo), marcos.id(), AGORA);
		salas.entrar(token(jogo), lucia.id(), AGORA);
		long livre = jogos.carregarLiga(jogo).divisoes().getLast().clubes().getFirst().id();
		salas.escolher(jogo, marcos.id(), livre);

		assertThatThrownBy(() -> salas.comecar(jogo, marcos.id(), AGORA, TRINTA)).isInstanceOf(AcessoNegadoException.class);
		assertThatThrownBy(() -> salas.comecar(jogo, admin.id(), AGORA, TRINTA))
			.isInstanceOf(SalaRecusadaException.class).hasMessageContaining("Participante escolher o clube");
		salas.tirar(jogo, admin.id(), lucia.id());
		// Marcos sem consultar há mais de 30 s
		assertThatThrownBy(() -> salas.comecar(jogo, admin.id(), AGORA.plusSeconds(31), TRINTA))
			.isInstanceOf(SalaRecusadaException.class).hasMessageContaining("voltar");
		assertThatThrownBy(() -> aoVivo.iniciar(jogo, admin.id(), 1, AGORA)).isInstanceOf(DecisaoPendenteException.class)
			.hasMessageContaining("ainda não começou");

		salas.marcarVisto(jogo, admin.id(), AGORA.plusSeconds(31));
		salas.marcarVisto(jogo, marcos.id(), AGORA.plusSeconds(31));
		salas.comecar(jogo, admin.id(), AGORA.plusSeconds(31), TRINTA);
		salas.comecar(jogo, admin.id(), AGORA.plusSeconds(32), TRINTA);

		assertThat(salas.naSala(jogo)).isFalse();
		// a sorte da 1ª temporada é refeita com os dois treinadores nos clubes (SPEC §5.3.1, rev. 108)
		List<Long> deUsuario = List.of(jogos.clubeDoTreinador(jogo, admin.id()).orElseThrow(), livre);
		for (long clube : deUsuario) {
			long divisao = contar("SELECT divisao_id FROM jogo_clube WHERE id = :jogo", clube);
			List<Long> beneficiados = jdbc.sql("SELECT clube_id FROM jogo_temporada_beneficiado WHERE divisao_id = :divisao")
				.param("divisao", divisao).query(Long.class).list();
			assertThat(beneficiados.stream().filter(b -> !deUsuario.contains(b))).hasSize(2);
			double media = jdbc.sql("SELECT avg(forca) FROM jogo_jogador WHERE clube_id = :clube").param("clube", clube)
				.query(Double.class).single();
			assertThat(beneficiados.contains(clube)).as("clube %d com média %.1f", clube, media).isEqualTo(media < 40);
		}
		List<Long> comPatrocinio = jdbc.sql("""
				SELECT p.clube_id FROM jogo_patrocinio p JOIN jogo_clube c ON c.id = p.clube_id WHERE c.jogo_id = :jogo""")
			.param("jogo", jogo).query(Long.class).list();
		assertThat(comPatrocinio).containsExactlyInAnyOrder(jogos.clubeDoTreinador(jogo, admin.id()).orElseThrow(), livre);
		assertThat(contar("SELECT count(*) FROM jogo_jogador WHERE clube_id = :jogo AND forca_salario IS NULL", livre)).isZero();
		assertThat(contar("SELECT count(*) FROM jogo_jogador WHERE clube_id = :jogo AND a_venda", livre)).isZero();
		assertThat(salas.convite(token(jogo), lucia.id(), AGORA)).hasValueSatisfying(c -> assertThat(((Convite.Invalido) c).motivo())
			.isEqualTo(Convite.Motivo.COMECOU));
		assertThatThrownBy(() -> salas.tirar(jogo, admin.id(), marcos.id())).isInstanceOf(SalaRecusadaException.class);
	}

	@Test
	void jogoEmRedeNaoBaixaArquivoESoOAdministradorApaga() {
		long jogo = jogosDoTreinador.criarEmRedeComSorteio(pais, admin.id(), AGORA).jogoId();
		salas.entrar(token(jogo), marcos.id(), AGORA);

		assertThatThrownBy(() -> saves.exportar(jogo, admin.id())).isInstanceOf(SaveRecusadoException.class);
		assertThat(saves.apagar(jogo, marcos.id())).isFalse();
		assertThat(saves.apagar(jogo, admin.id())).isTrue();
		assertThat(contar("SELECT count(*) FROM jogo_rede_participante WHERE jogo_id = :jogo", jogo)).isZero();
	}

	private long criarComEscolha() {
		long semente = 5;
		long clubeMolde = jogos.distribuicao(pais, semente).divisoes().getFirst().clubes().getFirst().id();
		return jogosDoTreinador.criarEmRedeComEscolha(pais, admin.id(), semente, clubeMolde, AGORA);
	}

	private String token(long jogo) {
		return jdbc.sql("SELECT token FROM jogo_rede_convite WHERE jogo_id = :jogo ORDER BY criado_em DESC LIMIT 1")
			.param("jogo", jogo).query(String.class).single();
	}

	private Situacao situacao(long jogo, Usuario usuario, Instant agora) {
		Participante p = salas.visao(jogo, admin.id()).orElseThrow().sala().participantes().stream()
			.filter(x -> x.usuarioId() == usuario.id()).findFirst().orElseThrow();
		return p.situacao(agora, TRINTA);
	}

	private long contar(String sql, long id) {
		return jdbc.sql(sql).param("jogo", id).query(Long.class).single();
	}
}
