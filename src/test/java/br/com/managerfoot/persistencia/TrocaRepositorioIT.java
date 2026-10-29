package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Moral;

/** Migração V28 e {@link TrocaRepositorio} contra PostgreSQL real (SPEC §5.4, rev. 62). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TrocaRepositorioIT {

	@Autowired
	TrocaRepositorio trocas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	EscalacaoRepositorio escalacoes;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	TransactionTemplate transacao;

	long jogo;
	long temporada;
	long usuario;
	long doUsuario;
	List<Long> serieA;

	@BeforeEach
	void criaJogo() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		temporada = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = ?").param(jogo).query(Long.class).single();
		usuario = jdbc.sql("SELECT usuario_id FROM jogo_treinador WHERE jogo_id = ?").param(jogo).query(Long.class).single();
		doUsuario = jogos.clubeDoTreinador(jogo, usuario).orElseThrow();
		serieA = jdbc.sql("""
				SELECT c.id FROM jogo_clube c JOIN jogo_divisao d ON d.id = c.divisao_id
				WHERE c.jogo_id = ? AND d.prioridade = 1 ORDER BY c.id""").param(jogo).query(Long.class).list();
		// todos no amarelo: só quem o teste põe no verde pode ser chamado
		jdbc.sql("UPDATE jogo_clube SET moral = 10 WHERE jogo_id = ?").param(jogo).update();
	}

	@Test
	void demitidoDaMaquinaTrocaComOTreinadorDoInferiorDeMoralVerde() {
		long demite = serieA.get(0);
		long cede = serieA.get(1);
		forca(demite, 40);
		forca(cede, 34);
		moral(demite, 3);
		moral(cede, 20);
		String treinadorDemitido = treinador(demite);
		String treinadorChamado = treinador(cede);

		demitir(demite, 5);

		assertThat(treinador(demite)).isEqualTo(treinadorChamado);
		assertThat(treinador(cede)).isEqualTo(treinadorDemitido);
		assertThat(moralDe(demite)).isEqualTo(22);
		assertThat(moralDe(cede)).isEqualTo(22);
		assertThat(situacoes()).containsExactly("TROCA");
		assertThat(trocas.algumConvitePendente(jogo)).isFalse();
	}

	@Test
	void quemSaiDoVermelhoNoJogoNaoEhDemitidoENinguemDistanteDemaisEhChamado() {
		long demite = serieA.get(0);
		long distante = serieA.get(1);
		forca(demite, 48);
		forca(distante, 30);
		moral(distante, 20);
		moral(demite, 8);

		// estava no vermelho e voltou ao amarelo: salvo
		demitir(demite, 5);
		assertThat(situacoes()).isEmpty();

		// demitido, mas o único de moral verde está 18 pontos abaixo: ninguém vem e ele continua
		moral(demite, 3);
		demitir(demite, 5);
		assertThat(situacoes()).isEmpty();
		assertThat(moralDe(demite)).isEqualTo(3);
	}

	@Test
	void usuarioConvidadoAceitaEAssumeOClubeQueDemitiu() {
		long demite = serieA.get(0);
		forca(demite, 40);
		forca(doUsuario, 33);
		moral(demite, 3);
		moral(doUsuario, 20);
		String demitido = treinador(demite);
		String escondido = treinador(doUsuario);

		demitir(demite, 5);

		assertThat(trocas.algumConvitePendente(jogo)).isTrue();
		TrocaRepositorio.Convite convite = trocas.convitePendente(jogo, usuario).orElseThrow();
		assertThat(convite.convite().clube().id()).isEqualTo(demite);
		assertThat(convite.hoje().clube().id()).isEqualTo(doUsuario);
		assertThat(convite.treinadorDemitido()).isEqualTo(demitido);

		assertThat(trocas.aceitar(jogo, usuario, convite.id())).isTrue();
		assertThat(trocas.aceitar(jogo, usuario, convite.id())).isFalse();

		assertThat(jogos.clubeDoTreinador(jogo, usuario)).hasValue(demite);
		// o demitido vai para o clube antigo do usuário; o treinador escondido do usuário vai junto com ele
		assertThat(treinador(doUsuario)).isEqualTo(demitido);
		assertThat(treinador(demite)).isEqualTo(escondido);
		assertThat(moralDe(demite)).isEqualTo(22);
		assertThat(escalacoes.carregar(demite)).isPresent();
		assertThat(escalacoes.carregar(doUsuario)).isEmpty();
		assertThat(contar("SELECT count(*) FROM jogo_jogador WHERE clube_id = ? AND forca_salario IS NULL", demite)).isZero();
		assertThat(contar("SELECT count(*) FROM jogo_jogador WHERE clube_id = ? AND forca_salario IS NOT NULL", doUsuario))
			.isZero();
		assertThat(trocas.algumConvitePendente(jogo)).isFalse();
	}

	@Test
	void usuarioQueRecusaPassaAVezAoSeguinte() {
		long demite = serieA.get(0);
		long seguinte = serieA.get(1);
		forca(demite, 40);
		forca(doUsuario, 36);
		forca(seguinte, 33);
		moral(demite, 3);
		moral(doUsuario, 20);
		moral(seguinte, 20);
		String treinadorSeguinte = treinador(seguinte);

		demitir(demite, 5);
		long convite = trocas.convitePendente(jogo, usuario).orElseThrow().id();

		assertThat(trocas.recusar(jogo, usuario, convite)).isTrue();

		assertThat(jogos.clubeDoTreinador(jogo, usuario)).hasValue(doUsuario);
		assertThat(treinador(demite)).isEqualTo(treinadorSeguinte);
		assertThat(situacoes()).containsExactlyInAnyOrder("RECUSADO", "TROCA");
		assertThat(trocas.algumConvitePendente(jogo)).isFalse();
	}

	@Test
	void usuarioDemitidoTrocaComAMaquinaOuContinuaEVeOAviso() {
		long cede = serieA.get(0);
		forca(doUsuario, 30);
		forca(cede, 25);
		moral(doUsuario, 3);
		String treinadorDaMaquina = treinador(cede);

		// ninguém de moral verde: continua, com aviso
		demitir(doUsuario, 5);
		TrocaRepositorio.Aviso continua = trocas.avisoPendente(jogo, usuario).orElseThrow();
		assertThat(continua.novo()).isEmpty();
		assertThat(jogos.clubeDoTreinador(jogo, usuario)).hasValue(doUsuario);
		trocas.marcarAvisoVisto(jogo, usuario, continua.id());
		assertThat(trocas.avisoPendente(jogo, usuario)).isEmpty();

		// com um inferior de moral verde: troca
		moral(cede, 20);
		demitir(doUsuario, 5);
		TrocaRepositorio.Aviso aviso = trocas.avisoPendente(jogo, usuario).orElseThrow();
		assertThat(aviso.novo().orElseThrow().clube().id()).isEqualTo(cede);
		assertThat(aviso.treinadorQueChega()).isEqualTo(treinadorDaMaquina);
		assertThat(jogos.clubeDoTreinador(jogo, usuario)).hasValue(cede);
		assertThat(treinador(doUsuario)).isEqualTo(treinadorDaMaquina);
	}

	/** O clube estava com moral {@code antes} e jogou a rodada; a moral de depois é a gravada. */
	private void demitir(long clube, int antes) {
		transacao.executeWithoutResult(s -> trocas.demitirDepoisDaRodada(jogo, temporada, 1, Map.of(clube, new Moral(antes)),
				Set.of(clube)));
	}

	private void forca(long clube, int forca) {
		jdbc.sql("UPDATE jogo_jogador SET forca = ? WHERE clube_id = ?").param(forca).param(clube).update();
	}

	private void moral(long clube, int moral) {
		jdbc.sql("UPDATE jogo_clube SET moral = ? WHERE id = ?").param(moral).param(clube).update();
	}

	private int moralDe(long clube) {
		return jdbc.sql("SELECT moral FROM jogo_clube WHERE id = ?").param(clube).query(Integer.class).single();
	}

	private String treinador(long clube) {
		return jdbc.sql("SELECT nome FROM jogo_treinador_maquina WHERE clube_id = ?").param(clube).query(String.class)
			.single();
	}

	private List<String> situacoes() {
		return jdbc.sql("SELECT situacao FROM jogo_troca_treinador WHERE jogo_id = ? ORDER BY id").param(jogo)
			.query(String.class).list();
	}

	private long contar(String sql, long clube) {
		return jdbc.sql(sql).param(clube).query(Long.class).single();
	}
}
