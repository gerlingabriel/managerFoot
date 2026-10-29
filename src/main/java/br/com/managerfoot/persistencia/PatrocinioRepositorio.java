package br.com.managerfoot.persistencia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.economia.PropostasPatrocinio;
import br.com.managerfoot.economia.PropostasPatrocinio.Proposta;
import br.com.managerfoot.economia.PropostasPatrocinio.Tipo;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ProcessamentoRodada;

/**
 * Patrocínio escolhido pelo usuário (SPEC §9.6, modelo 9a, rev. 55 e 57): a oferta criada no início
 * da temporada ({@link CaixaRepositorio#pagarPatrocinio}), a escolha, que paga a parte à vista, e os
 * pagamentos seguintes: por vitória no avanço da rodada e o bônus de acesso no fim da temporada.
 */
@Repository
public class PatrocinioRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final CaixaRepositorio caixas;
	private final PropostasPatrocinio propostas = PropostasPatrocinio.padrao();

	public PatrocinioRepositorio(JdbcClient jdbc, JogoRepositorio jogos, CaixaRepositorio caixas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.caixas = caixas;
	}

	/**
	 * A caixa de propostas do clube do usuário, se ele ainda não escolheu nesta temporada.
	 *
	 * @throws NoSuchElementException se o usuário não treina clube no jogo
	 */
	@Transactional(readOnly = true)
	public Optional<CaixaPatrocinio> pendente(long jogoId, long usuarioId) {
		long clube = clubeDoUsuario(jogoId, usuarioId);
		return oferta(jogoId, clube).filter(o -> o.escolha() == null).map(o -> {
			List<String> nomes = propostas.patrocinadores(o.id());
			List<Proposta> lista = propostas.propostas(o.tabela(), o.jogos());
			List<PropostaNaCaixa> naCaixa = lista.stream()
				.map(p -> new PropostaNaCaixa(p, nomes.get(p.tipo().ordinal())))
				.toList();
			return new CaixaPatrocinio(jogos.ano(jogoId, o.temporada()), o.clube(), o.divisao(), o.prioridade() == 1,
					o.temporada() == 1, propostas.vitoriasEsperadas(o.jogos()), propostas.primeirosNaPrimeira(),
					Moeda.doPais(o.pais()), naCaixa);
		});
	}

	/**
	 * O usuário assina uma das propostas: paga a parte à vista na rodada 0. Um segundo pedido não paga
	 * de novo.
	 *
	 * @return se a escolha foi gravada agora
	 * @throws NoSuchElementException se o usuário não treina clube no jogo
	 */
	@Transactional
	public boolean assinar(long jogoId, long usuarioId, Tipo tipo) {
		jogos.travar(jogoId);
		long clube = clubeDoUsuario(jogoId, usuarioId);
		Optional<Oferta> oferta = oferta(jogoId, clube).filter(o -> o.escolha() == null);
		if (oferta.isEmpty()) {
			return false;
		}
		Oferta o = oferta.get();
		jdbc.sql("UPDATE jogo_patrocinio SET escolha = :escolha WHERE id = :id AND escolha IS NULL")
			.param("escolha", tipo.name())
			.param("id", o.id())
			.update();
		Proposta proposta = proposta(o.tabela(), o.jogos(), tipo);
		caixas.lancarPatrocinio(o.temporadaId(), 0, clube, proposta.agora());
		return true;
	}

	/** Proposta por vitória: paga cada vitória da rodada jogada. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void pagarVitorias(long temporadaId, int rodada, List<ProcessamentoRodada.PartidaJogada> partidas) {
		Map<Long, Proposta> contratos = contratos(temporadaId, Tipo.POR_VITORIA);
		if (contratos.isEmpty()) {
			return;
		}
		for (ProcessamentoRodada.PartidaJogada p : partidas) {
			int casa = p.resultado().golsCasa();
			int fora = p.resultado().golsFora();
			if (casa == fora) {
				continue;
			}
			long vencedor = casa > fora ? p.jogo().mandante().id() : p.jogo().visitante().id();
			Proposta proposta = contratos.get(vencedor);
			if (proposta != null && proposta.porVitoria() > 0) {
				caixas.lancarPatrocinio(temporadaId, rodada, vencedor, proposta.porVitoria());
			}
		}
	}

	/**
	 * Proposta por acesso: paga o bônus na última rodada da temporada que termina a quem subiu (na 1ª
	 * divisão, a quem ficou entre os primeiros).
	 *
	 * @param finais classificação final por prioridade da divisão
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void pagarBonusDeAcesso(long jogoId, Map<Integer, Classificacao> finais) {
		Temporada t = jdbc.sql("""
				SELECT id, rodadas_processadas FROM jogo_temporada WHERE jogo_id = :jogo ORDER BY numero DESC LIMIT 1""")
			.param("jogo", jogoId)
			.query((rs, n) -> new Temporada(rs.getLong("id"), rs.getInt("rodadas_processadas")))
			.single();
		Map<Long, Proposta> contratos = contratos(t.id(), Tipo.SE_SUBIR);
		if (contratos.isEmpty()) {
			return;
		}
		Map<Integer, Integer> promovidos = new HashMap<>();
		jogos.carregarLiga(jogoId).divisoes().forEach(d -> promovidos.put(d.prioridade(), d.promovidos()));
		finais.forEach((prioridade, classificacao) -> {
			for (int i = 0; i < classificacao.linhas().size(); i++) {
				long clube = classificacao.linhas().get(i).clube().id();
				Proposta proposta = contratos.get(clube);
				if (proposta != null && propostas.subiu(prioridade, i + 1, promovidos.get(prioridade))) {
					caixas.lancarPatrocinio(t.id(), t.rodadas(), clube, proposta.seSubir());
				}
			}
		});
	}

	private Map<Long, Proposta> contratos(long temporadaId, Tipo tipo) {
		Map<Long, Proposta> contratos = new HashMap<>();
		jdbc.sql("SELECT clube_id, tabela, jogos FROM jogo_patrocinio WHERE temporada_id = :temporada AND escolha = :escolha")
			.param("temporada", temporadaId)
			.param("escolha", tipo.name())
			.query(rs -> {
				contratos.put(rs.getLong("clube_id"), proposta(rs.getLong("tabela"), rs.getInt("jogos"), tipo));
			});
		return contratos;
	}

	private Proposta proposta(long tabela, int jogosNaTemporada, Tipo tipo) {
		return propostas.propostas(tabela, jogosNaTemporada).stream().filter(p -> p.tipo() == tipo).findFirst().orElseThrow();
	}

	private long clubeDoUsuario(long jogoId, long usuarioId) {
		return jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
	}

	/** A oferta do clube na temporada atual; vazia em jogos salvos antes da V25 e na fila de espera. */
	private Optional<Oferta> oferta(long jogoId, long clube) {
		return jdbc.sql("""
				SELECT p.id, p.tabela, p.jogos, p.escolha, t.id AS temporada_id, t.numero, c.nome, d.nome AS divisao,
				       d.prioridade, j.pais_codigo
				FROM jogo_patrocinio p
				JOIN jogo_temporada t ON t.id = p.temporada_id
				JOIN jogo_clube c ON c.id = p.clube_id
				JOIN jogo_divisao d ON d.id = c.divisao_id
				JOIN jogo j ON j.id = t.jogo_id
				WHERE p.clube_id = :clube AND t.jogo_id = :jogo
				  AND t.numero = (SELECT max(numero) FROM jogo_temporada WHERE jogo_id = :jogo)""")
			.param("clube", clube)
			.param("jogo", jogoId)
			.query((rs, n) -> new Oferta(rs.getLong("id"), rs.getLong("tabela"), rs.getInt("jogos"), rs.getString("escolha"),
					rs.getLong("temporada_id"), rs.getInt("numero"), rs.getString("nome"), rs.getString("divisao"),
					rs.getInt("prioridade"), rs.getString("pais_codigo")))
			.optional();
	}

	private record Oferta(long id, long tabela, int jogos, String escolha, long temporadaId, int temporada, String clube,
			String divisao, int prioridade, String pais) {
	}

	private record Temporada(long id, int rodadas) {
	}

	/**
	 * A caixa de propostas do início do ano (modelo 9a).
	 *
	 * @param temporada ano da temporada
	 * @param primeiraDivisao o bônus de acesso vale para os {@code primeirosNaPrimeira} colocados
	 * @param primeiraTemporada o valor da tabela é o mesmo para todos da divisão
	 * @param vitoriasEsperadas vitórias do clube médio, para o exemplo da proposta por vitória
	 */
	public record CaixaPatrocinio(int temporada, String clube, String divisao, boolean primeiraDivisao,
			boolean primeiraTemporada, int vitoriasEsperadas, int primeirosNaPrimeira, Moeda moeda,
			List<PropostaNaCaixa> propostas) {

		public CaixaPatrocinio {
			propostas = List.copyOf(propostas);
		}
	}

	/** Uma proposta com o nome do patrocinador. */
	public record PropostaNaCaixa(Proposta proposta, String patrocinador) {
	}
}
