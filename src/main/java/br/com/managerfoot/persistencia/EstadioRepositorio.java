package br.com.managerfoot.persistencia;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.economia.AmpliacaoEstadio;
import br.com.managerfoot.economia.MercadoDaMaquina;
import br.com.managerfoot.economia.ParametrosPublico;
import br.com.managerfoot.economia.PublicoDoJogo;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;

/**
 * Estádio dos clubes do jogo (SPEC §9.2, rev. 56 e 57): obras de ampliação, a decisão da máquina de
 * ampliar, a ampliação pelo usuário (modelo 6c) e a posição final da temporada passada, que decide o
 * público das primeiras rodadas. A obra e a posição só rodam dentro do avanço da rodada e do fim de
 * temporada, que já garantem processamento único; a ampliação do usuário trava o jogo.
 */
@Repository
public class EstadioRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final CaixaRepositorio caixas;
	private final ParametrosPublico parametros = ParametrosPublico.padrao();
	private final PublicoDoJogo publico = PublicoDoJogo.padrao();
	private final AmpliacaoEstadio ampliacao = AmpliacaoEstadio.padrao();

	public EstadioRepositorio(JdbcClient jdbc, JogoRepositorio jogos, CaixaRepositorio caixas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.caixas = caixas;
	}

	/**
	 * Depois da rodada jogada: as obras andam uma rodada e as que terminam somam o bloco ao estádio
	 * (vale a partir da rodada seguinte); depois, cada clube da máquina numa divisão, sem obra, decide
	 * se amplia e paga na hora. Clube de usuário só amplia pela tela.
	 *
	 * @param planteis os planteis que jogaram a rodada
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void avancarObras(long jogoId, long temporadaId, int rodada, Map<Long, List<Jogador>> planteis) {
		jdbc.sql("""
				UPDATE jogo_clube
				SET estadio_capacidade = CASE WHEN obra_rodadas = 1 THEN estadio_capacidade + :bloco ELSE estadio_capacidade END,
				    obra_rodadas = nullif(obra_rodadas - 1, 0)
				WHERE jogo_id = :jogo AND obra_rodadas IS NOT NULL""")
			.param("bloco", parametros.bloco())
			.param("jogo", jogoId)
			.update();
		List<ClubeSemObra> candidatos = jdbc.sql("""
				SELECT c.id, c.estadio_capacidade, c.saldo FROM jogo_clube c
				WHERE c.jogo_id = :jogo AND c.divisao_id IS NOT NULL AND c.obra_rodadas IS NULL AND c.saldo IS NOT NULL
				  AND NOT EXISTS (SELECT 1 FROM jogo_treinador t WHERE t.clube_id = c.id)
				ORDER BY c.id""")
			.param("jogo", jogoId)
			.query((rs, n) -> new ClubeSemObra(rs.getLong("id"), rs.getInt("estadio_capacidade"), rs.getLong("saldo")))
			.list();
		for (ClubeSemObra c : candidatos) {
			List<Jogador> plantel = planteis.getOrDefault(c.id(), List.of());
			if (ampliacao.maquinaAmplia(c.estadio(), MercadoDaMaquina.forcaEmCampo(plantel), c.saldo(), plantel)) {
				caixas.lancarEstadio(temporadaId, rodada, c.id(), ampliacao.custo());
				jdbc.sql("UPDATE jogo_clube SET obra_rodadas = :rodadas WHERE id = :clube")
					.param("rodadas", parametros.rodadasDeObra())
					.param("clube", c.id())
					.update();
			}
		}
	}

	/**
	 * Grava a posição final de cada clube na divisão em que jogou a temporada que terminou. Quem vem da
	 * fila para uma divisão conta como o último dela (como no patrocínio, rev. 46); quem vai para a fila
	 * fica sem posição.
	 *
	 * @param finais classificação final por prioridade da divisão
	 * @param proxima a liga da temporada seguinte
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void gravarPosicoesAnteriores(long jogoId, Map<Integer, Classificacao> finais, ConfiguracaoLiga proxima) {
		jdbc.sql("UPDATE jogo_clube SET posicao_anterior = NULL, clubes_divisao_anterior = NULL WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.update();
		finais.values().forEach(classificacao -> {
			for (int i = 0; i < classificacao.linhas().size(); i++) {
				gravar(jogoId, classificacao.linhas().get(i).clube().id(), i + 1, classificacao.linhas().size());
			}
		});
		for (Divisao divisao : proxima.divisoes()) {
			int clubes = divisao.clubes().size();
			for (Clube clube : divisao.clubes()) {
				boolean jogou = finais.values().stream()
					.anyMatch(c -> c.linhas().stream().anyMatch(l -> l.clube().id() == clube.id()));
				if (!jogou) {
					gravar(jogoId, clube.id(), clubes, clubes);
				}
			}
		}
		for (Clube clube : proxima.filaDeEspera()) {
			jdbc.sql("UPDATE jogo_clube SET posicao_anterior = NULL, clubes_divisao_anterior = NULL WHERE id = :clube")
				.param("clube", clube.id())
				.update();
		}
	}

	/**
	 * A janela do estádio do clube do usuário (modelo 6c, rev. 55): o que falta para ampliar.
	 *
	 * @throws NoSuchElementException se o usuário não treina clube no jogo
	 */
	@Transactional(readOnly = true)
	public JanelaEstadio janela(long jogoId, long usuarioId) {
		long clube = clubeDoUsuario(jogoId, usuarioId);
		Situacao s = situacao(jogoId, clube);
		return new JanelaEstadio(s.estadio(), parametros.capacidadeMaxima(), parametros.bloco(), ampliacao.custo(),
				parametros.rodadasDeObra(), s.obraRodadas(), impedimento(s));
	}

	/**
	 * O usuário amplia o estádio do clube dele (SPEC §9.2, modelo 6c): paga um bloco na hora, na última
	 * rodada processada, e os lugares valem depois da obra. O servidor confere tudo.
	 *
	 * @throws NoSuchElementException se o usuário não treina clube no jogo
	 * @throws AmpliacaoRecusadaException se há obra, o estádio está no máximo, há rodada ao vivo ou
	 *         falta caixa
	 */
	@Transactional
	public void ampliar(long jogoId, long usuarioId) {
		jogos.travar(jogoId);
		long clube = clubeDoUsuario(jogoId, usuarioId);
		Situacao s = situacao(jogoId, clube);
		String impedimento = impedimento(s);
		if (impedimento != null) {
			throw new AmpliacaoRecusadaException(impedimento + ".");
		}
		caixas.abrirCaixas(jogoId);
		LinhaTemporada temporada = jdbc.sql("""
				SELECT id, rodadas_processadas FROM jogo_temporada WHERE jogo_id = :jogo ORDER BY numero DESC LIMIT 1""")
			.param("jogo", jogoId)
			.query((rs, n) -> new LinhaTemporada(rs.getLong("id"), rs.getInt("rodadas_processadas")))
			.single();
		caixas.lancarEstadio(temporada.id(), temporada.rodadas(), clube, ampliacao.custo());
		jdbc.sql("UPDATE jogo_clube SET obra_rodadas = :rodadas WHERE id = :clube")
			.param("rodadas", parametros.rodadasDeObra())
			.param("clube", clube)
			.update();
	}

	private long clubeDoUsuario(long jogoId, long usuarioId) {
		return jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
	}

	/** Estádio, obra, saldo e rodada ao vivo; antes do primeiro avanço, o estádio e o saldo iniciais. */
	private Situacao situacao(long jogoId, long clube) {
		List<Jogador> plantel = jogos.carregarPlanteis(jogoId).getOrDefault(clube, List.of());
		Situacao s = jdbc.sql("""
				SELECT c.forca, c.estadio_capacidade, c.obra_rodadas,
				       EXISTS (SELECT 1 FROM jogo_rodada_ao_vivo r WHERE r.jogo_id = c.jogo_id) AS ao_vivo
				FROM jogo_clube c WHERE c.id = :clube""")
			.param("clube", clube)
			.query((rs, n) -> new Situacao(rs.getObject("estadio_capacidade") == null
					? publico.estadioInicial(rs.getInt("forca")) : rs.getInt("estadio_capacidade"),
					(Integer) rs.getObject("obra_rodadas"), 0, rs.getBoolean("ao_vivo")))
			.single();
		return new Situacao(s.estadio(), s.obraRodadas(), caixas.saldo(clube, plantel), s.aoVivo());
	}

	/** Por que o botão fica desligado (modelo 6c); nulo se pode ampliar. */
	private String impedimento(Situacao s) {
		if (s.obraRodadas() != null) {
			return "Obra em andamento";
		}
		if (!ampliacao.cabe(s.estadio())) {
			return "Estádio no tamanho máximo";
		}
		if (s.aoVivo()) {
			return "Não dá para ampliar durante a rodada";
		}
		if (s.saldo() < ampliacao.custo()) {
			return "Caixa insuficiente";
		}
		return null;
	}

	private void gravar(long jogoId, long clubeId, int posicao, int clubes) {
		jdbc.sql("""
				UPDATE jogo_clube SET posicao_anterior = :posicao, clubes_divisao_anterior = :clubes
				WHERE id = :clube AND jogo_id = :jogo""")
			.param("posicao", posicao)
			.param("clubes", clubes)
			.param("clube", clubeId)
			.param("jogo", jogoId)
			.update();
	}

	private record ClubeSemObra(long id, int estadio, long saldo) {
	}

	private record Situacao(int estadio, Integer obraRodadas, long saldo, boolean aoVivo) {
	}

	private record LinhaTemporada(long id, int rodadas) {
	}

	/**
	 * Janela do estádio (modelo 6c).
	 *
	 * @param obraRodadas rodadas que faltam da obra; nulo sem obra
	 * @param custo preço de um bloco, em centavos de Real
	 * @param rodadasDeObra quanto dura uma obra
	 * @param impedimento por que não pode ampliar agora; nulo se pode
	 */
	public record JanelaEstadio(int capacidade, int limite, int bloco, long custo, int rodadasDeObra, Integer obraRodadas,
			String impedimento) {
	}
}
