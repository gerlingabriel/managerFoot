package br.com.managerfoot.persistencia;

import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.contas.AcessoNegadoException;
import br.com.managerfoot.contas.Papel;
import br.com.managerfoot.contas.RegrasConta;
import br.com.managerfoot.contas.Senhas;
import br.com.managerfoot.contas.TokenCadastro;
import br.com.managerfoot.contas.Usuario;

/**
 * Contas e cadastro por link (SPEC §3.4, rev. 24; docs/decisions/0007). Senhas e tokens nunca são
 * gravados nem registrados em texto.
 */
@Repository
public class ContasRepositorio {

	private final JdbcClient jdbc;
	private final Duration validadeLink;

	public ContasRepositorio(JdbcClient jdbc, @Value("${managerfoot.contas.validade-link}") Duration validadeLink) {
		if (validadeLink.isNegative() || validadeLink.isZero()) {
			throw new IllegalArgumentException("managerfoot.contas.validade-link deve ser positiva: " + validadeLink);
		}
		this.jdbc = jdbc;
		this.validadeLink = validadeLink;
	}

	/**
	 * Cria um administrador (usado pelo script de administração).
	 *
	 * @throws IllegalArgumentException se os dados forem inválidos ou o e-mail já tiver conta
	 */
	@Transactional
	public Usuario criarAdministrador(String email, String nome, String senha) {
		return inserir(email, nome, senha, Papel.ADMINISTRADOR);
	}

	/**
	 * Gera um link de cadastro de uso único e devolve o token, que não fica gravado.
	 *
	 * @throws AcessoNegadoException se quem pede não é administrador
	 */
	@Transactional
	public String gerarLinkCadastro(long administradorId) {
		exigirAdministrador(administradorId);
		String token = TokenCadastro.gerar();
		jdbc.sql("""
				INSERT INTO link_cadastro (token_hash, criado_por, expira_em)
				VALUES (:hash, :admin, now() + make_interval(secs => :segundos))""")
			.param("hash", TokenCadastro.hash(token))
			.param("admin", administradorId)
			.param("segundos", validadeLink.toSeconds())
			.update();
		return token;
	}

	/**
	 * Cadastra um participante pelo link e marca o link como usado, na mesma transação.
	 *
	 * @throws IllegalArgumentException se o link não vale (inexistente, usado ou expirado), se os
	 *         dados forem inválidos ou se o e-mail já tiver conta
	 */
	@Transactional
	public Usuario cadastrar(String token, String email, String nome, String senha) {
		// trava o link: dois cadastros simultâneos com o mesmo token não passam juntos
		Long linkId = token == null ? null : jdbc.sql("""
				SELECT id FROM link_cadastro
				WHERE token_hash = :hash AND usado_em IS NULL AND expira_em > now() FOR UPDATE""")
			.param("hash", TokenCadastro.hash(token))
			.query(Long.class)
			.optional()
			.orElse(null);
		if (linkId == null) {
			throw new IllegalArgumentException("Este link de cadastro não vale mais: já foi usado, expirou ou não existe.");
		}
		Usuario usuario = inserir(email, nome, senha, Papel.PARTICIPANTE);
		jdbc.sql("UPDATE link_cadastro SET usado_por = :usuario, usado_em = now() WHERE id = :link")
			.param("usuario", usuario.id())
			.param("link", linkId)
			.update();
		return usuario;
	}

	/** O usuário, se o e-mail e a senha conferem. Não revela se o e-mail existe. */
	@Transactional(readOnly = true)
	public Optional<Usuario> autenticar(String email, String senha) {
		Optional<Gravado> gravado = jdbc.sql("SELECT id, email, nome, papel, senha_hash FROM usuario WHERE email = :email")
			.param("email", RegrasConta.normalizarEmail(email))
			.query((rs, n) -> new Gravado(new Usuario(rs.getLong("id"), rs.getString("email"), rs.getString("nome"),
					Papel.valueOf(rs.getString("papel"))), rs.getString("senha_hash")))
			.optional();
		boolean confere = Senhas.confere(senha, gravado.map(Gravado::senhaHash).orElse(null));
		return confere ? gravado.map(Gravado::usuario) : Optional.empty();
	}

	/**
	 * Recuperação de senha pelo administrador (SPEC rev. 24).
	 *
	 * @throws AcessoNegadoException se quem pede não é administrador
	 * @throws NoSuchElementException se o usuário não existe
	 */
	@Transactional
	public void redefinirSenha(long administradorId, long usuarioId, String novaSenha) {
		exigirAdministrador(administradorId);
		int atualizados = jdbc.sql("UPDATE usuario SET senha_hash = :hash WHERE id = :usuario")
			.param("hash", Senhas.hash(novaSenha))
			.param("usuario", usuarioId)
			.update();
		if (atualizados == 0) {
			throw new NoSuchElementException("usuário " + usuarioId + " não existe");
		}
	}

	@Transactional(readOnly = true)
	public Optional<Usuario> buscar(long usuarioId) {
		return jdbc.sql("SELECT id, email, nome, papel FROM usuario WHERE id = :id")
			.param("id", usuarioId)
			.query((rs, n) -> new Usuario(rs.getLong("id"), rs.getString("email"), rs.getString("nome"),
					Papel.valueOf(rs.getString("papel"))))
			.optional();
	}

	private Usuario inserir(String email, String nome, String senha, Papel papel) {
		RegrasConta.exigirValidos(email, nome, senha);
		String normalizado = RegrasConta.normalizarEmail(email);
		try {
			long id = TabelasLiga.inserir(jdbc, """
					INSERT INTO usuario (email, nome, senha_hash, papel)
					VALUES (:email, :nome, :hash, :papel)""",
					Map.of("email", normalizado, "nome", nome.strip(), "hash", Senhas.hash(senha), "papel", papel.name()));
			return new Usuario(id, normalizado, nome.strip(), papel);
		}
		catch (DuplicateKeyException e) {
			throw new IllegalArgumentException("Já existe uma conta com este e-mail.", e);
		}
	}

	private void exigirAdministrador(long usuarioId) {
		if (buscar(usuarioId).map(Usuario::papel).orElse(null) != Papel.ADMINISTRADOR) {
			throw new AcessoNegadoException("só o administrador pode fazer isso");
		}
	}

	private record Gravado(Usuario usuario, String senhaHash) {
	}
}
