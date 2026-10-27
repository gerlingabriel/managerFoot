package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.contas.AcessoNegadoException;
import br.com.managerfoot.contas.Papel;
import br.com.managerfoot.contas.TokenCadastro;
import br.com.managerfoot.contas.Usuario;

/** Migração V8 e {@link ContasRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ContasRepositorioIT {

	private static final AtomicInteger SEQUENCIA = new AtomicInteger();
	private static final String SENHA = "senha-forte-1";

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JdbcClient jdbc;

	Usuario admin;

	@BeforeEach
	void criaAdministrador() {
		admin = contas.criarAdministrador(email("admin"), "Admin", SENHA);
	}

	@Test
	void administradorEntraComEmailEmQualquerCaixaESoComASenhaCerta() {
		assertThat(admin.papel()).isEqualTo(Papel.ADMINISTRADOR);
		assertThat(contas.autenticar("  " + admin.email().toUpperCase() + " ", SENHA)).contains(admin);
		assertThat(contas.autenticar(admin.email(), "senha-errada")).isEmpty();
		assertThat(contas.autenticar("ninguem@exemplo.com", SENHA)).isEmpty();
		assertThat(jdbc.sql("SELECT senha_hash FROM usuario WHERE id = ?").param(admin.id()).query(String.class).single())
			.doesNotContain(SENHA);
	}

	@Test
	void linkCadastraUmParticipanteEUmaVezSo() {
		String token = contas.gerarLinkCadastro(admin.id());

		Usuario ana = contas.cadastrar(token, email("ana"), "Ana", SENHA);

		assertThat(ana.papel()).isEqualTo(Papel.PARTICIPANTE);
		assertThat(contas.autenticar(ana.email(), SENHA)).contains(ana);
		assertThatThrownBy(() -> contas.cadastrar(token, email("bia"), "Bia", SENHA))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("não vale mais");
		// o banco guarda só o hash do token
		assertThat(jdbc.sql("SELECT count(*) FROM link_cadastro WHERE token_hash = ?").param(token).query(Long.class)
			.single()).isZero();
		assertThat(jdbc.sql("SELECT usado_por FROM link_cadastro WHERE token_hash = ?")
			.param(TokenCadastro.hash(token)).query(Long.class).single()).isEqualTo(ana.id());
	}

	@Test
	void linkExpiradoOuInventadoNaoCadastra() {
		String token = contas.gerarLinkCadastro(admin.id());
		jdbc.sql("UPDATE link_cadastro SET criado_em = now() - interval '8 days', expira_em = now() - interval '1 day' "
				+ "WHERE token_hash = ?").param(TokenCadastro.hash(token)).update();

		assertThatThrownBy(() -> contas.cadastrar(token, email("ana"), "Ana", SENHA))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> contas.cadastrar(TokenCadastro.gerar(), email("bia"), "Bia", SENHA))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> contas.cadastrar(null, email("bia"), "Bia", SENHA))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void linkGeradoValeSeteDias() {
		String token = contas.gerarLinkCadastro(admin.id());

		Long segundos = jdbc.sql("SELECT extract(epoch FROM expira_em - criado_em)::bigint FROM link_cadastro "
				+ "WHERE token_hash = ?").param(TokenCadastro.hash(token)).query(Long.class).single();
		assertThat(segundos).isEqualTo(7 * 24 * 3600L);
	}

	@Test
	void cadastroRecusadoNaoGastaOLink() {
		String token = contas.gerarLinkCadastro(admin.id());

		assertThatThrownBy(() -> contas.cadastrar(token, admin.email().toUpperCase(), "Cópia", SENHA))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Já existe uma conta");
		assertThatThrownBy(() -> contas.cadastrar(token, "sem-arroba", " ", "123"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("e-mail válido")
			.hasMessageContaining("nome")
			.hasMessageContaining("8 caracteres");

		assertThat(contas.cadastrar(token, email("ana"), "Ana", SENHA).papel()).isEqualTo(Papel.PARTICIPANTE);
	}

	@Test
	void cadastrosSimultaneosComOMesmoLinkCriamUmaContaSo() throws Exception {
		String token = contas.gerarLinkCadastro(admin.id());
		CountDownLatch largada = new CountDownLatch(1);
		List<Callable<Boolean>> tarefas = new ArrayList<>();
		for (String nome : List.of("ana", "bia")) {
			String email = email(nome);
			tarefas.add(() -> {
				largada.await();
				try {
					contas.cadastrar(token, email, nome, SENHA);
					return true;
				}
				catch (IllegalArgumentException e) {
					return false;
				}
			});
		}
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			List<Future<Boolean>> futuros = List.of(executor.submit(tarefas.get(0)), executor.submit(tarefas.get(1)));
			largada.countDown();
			List<Boolean> sucessos = new ArrayList<>();
			for (Future<Boolean> f : futuros) {
				sucessos.add(f.get());
			}
			assertThat(sucessos).containsExactlyInAnyOrder(true, false);
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	void soAdministradorGeraLinkERedefineSenha() {
		Usuario ana = contas.cadastrar(contas.gerarLinkCadastro(admin.id()), email("ana"), "Ana", SENHA);

		assertThatThrownBy(() -> contas.gerarLinkCadastro(ana.id())).isInstanceOf(AcessoNegadoException.class);
		assertThatThrownBy(() -> contas.redefinirSenha(ana.id(), admin.id(), "outra-senha"))
			.isInstanceOf(AcessoNegadoException.class);

		contas.redefinirSenha(admin.id(), ana.id(), "nova-senha-9");

		assertThat(contas.autenticar(ana.email(), "nova-senha-9")).contains(ana);
		assertThat(contas.autenticar(ana.email(), SENHA)).isEmpty();
	}

	private static String email(String nome) {
		return nome + SEQUENCIA.incrementAndGet() + "-" + System.nanoTime() + "@exemplo.com";
	}
}
