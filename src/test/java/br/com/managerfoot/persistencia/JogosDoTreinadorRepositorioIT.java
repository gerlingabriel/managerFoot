package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio.JogoGravado;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio.NovoJogo;

/** Migração V11 e {@link JogosDoTreinadorRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JogosDoTreinadorRepositorioIT {

	@Autowired
	JogosDoTreinadorRepositorio repositorio;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	JdbcClient jdbc;

	@Test
	void jogoNovoJaTemATemporada1EOClubeSaiDosCandidatosDaUltimaDivisao() {
		long pais = LigaDeTeste.criarPais(moldes);
		Usuario usuario = LigaDeTeste.participante(contas);

		NovoJogo novo = repositorio.criarComSorteio(pais, usuario.id(), 42);

		long clube = jogos.clubeDoTreinador(novo.jogoId(), usuario.id()).orElseThrow();
		assertThat(novo.candidatos()).extracting(Clube::id).contains(clube);
		assertThat(novo.candidatos()).hasSizeGreaterThanOrEqualTo(4);
		assertThat(jogos.carregarLiga(novo.jogoId()).divisoes().getLast().clubes()).containsAll(novo.candidatos());
		assertThat(rodadas.totalDeRodadas(novo.jogoId())).isPositive();

		assertThat(repositorio.listar(usuario.id())).singleElement().satisfies(j -> {
			assertThat(j.jogoId()).isEqualTo(novo.jogoId());
			assertThat(j.clube().id()).isEqualTo(clube);
			assertThat(j.pais()).isEqualTo("Brasil");
			assertThat(j.divisao()).isEqualTo("Série B");
			assertThat(j.ano()).isEqualTo(2026);
			assertThat(j.rodada()).isEqualTo(1);
		});
	}

	@Test
	void mesmaSementeMesmoClube() {
		long pais = LigaDeTeste.criarPais(moldes);
		Usuario usuario = LigaDeTeste.participante(contas);

		String primeiro = nomeDoClube(repositorio.criarComSorteio(pais, usuario.id(), 7), usuario);
		String segundo = nomeDoClube(repositorio.criarComSorteio(pais, usuario.id(), 7), usuario);

		assertThat(segundo).isEqualTo(primeiro);
	}

	@Test
	void listaPeloUltimoAcessoERodadaAvancaComOJogo() {
		long pais = LigaDeTeste.criarPais(moldes);
		Usuario usuario = LigaDeTeste.participante(contas);
		long antigo = repositorio.criarComSorteio(pais, usuario.id(), 1).jogoId();
		long recente = repositorio.criarComSorteio(pais, usuario.id(), 2).jogoId();
		jdbc.sql("UPDATE jogo_treinador SET ultimo_acesso_em = now() - interval '1 day' WHERE jogo_id = :jogo")
			.param("jogo", antigo)
			.update();
		assertThat(repositorio.listar(usuario.id())).extracting(JogoGravado::jogoId).containsExactly(recente, antigo);

		rodadas.avancar(antigo, 1);
		assertThat(repositorio.abrir(antigo, usuario.id())).hasValueSatisfying(j -> assertThat(j.rodada()).isEqualTo(2));

		assertThat(repositorio.listar(usuario.id())).extracting(JogoGravado::jogoId).containsExactly(antigo, recente);
	}

	@Test
	void naoAbreJogoDeOutroUsuarioNemInexistente() {
		long pais = LigaDeTeste.criarPais(moldes);
		Usuario dono = LigaDeTeste.participante(contas);
		Usuario outro = LigaDeTeste.participante(contas);
		long jogo = repositorio.criarComSorteio(pais, dono.id(), 3).jogoId();

		assertThat(repositorio.abrir(jogo, outro.id())).isEmpty();
		assertThat(repositorio.abrir(Long.MAX_VALUE, dono.id())).isEmpty();
		assertThat(repositorio.listar(outro.id())).isEmpty();
	}

	private String nomeDoClube(NovoJogo novo, Usuario usuario) {
		return repositorio.abrir(novo.jogoId(), usuario.id()).orElseThrow().clube().nome();
	}
}
