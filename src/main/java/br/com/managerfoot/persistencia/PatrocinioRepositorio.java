package br.com.managerfoot.persistencia;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.economia.MetaPatrocinio;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.economia.PropostasPatrocinio;
import br.com.managerfoot.economia.PropostasPatrocinio.Proposta;
import br.com.managerfoot.economia.PropostasPatrocinio.Tipo;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.LinhaClassificacao;
import br.com.managerfoot.temporada.ProcessamentoRodada;

/**
 * Patrocínio escolhido pelo usuário (SPEC §9.6, modelo 9a, rev. 55 e 57): a oferta criada no início
 * da temporada ({@link CaixaRepositorio#pagarPatrocinio}), a escolha, que paga a parte à vista, e os
 * pagamentos seguintes: o bônus das metas no fim da temporada (rev. 85) e, nos contratos antigos, por
 * vitória no avanço da rodada e o bônus de acesso no fim da temporada.
 */
@Repository
public class PatrocinioRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final CaixaRepositorio caixas;
	private final ClassificacaoRepositorio classificacoes;
	private final TemporadaRepositorio temporadas;
	private final PropostasPatrocinio propostas = PropostasPatrocinio.padrao();

	public PatrocinioRepositorio(JdbcClient jdbc, JogoRepositorio jogos, CaixaRepositorio caixas,
			ClassificacaoRepositorio classificacoes, TemporadaRepositorio temporadas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.caixas = caixas;
		this.classificacoes = classificacoes;
		this.temporadas = temporadas;
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
			List<PropostaNaCaixa> naCaixa = propostas.propostasComMeta(o.tabela()).stream()
				.map(p -> new PropostaNaCaixa(p, nomes.get(p.tipo().lugar())))
				.toList();
			int clubes = clubesDaDivisao(jogoId, clube).size();
			int ordem = ordemDeForca(jogoId, clube);
			return new CaixaPatrocinio(jogos.ano(jogoId, o.temporada()), o.clube(), o.divisao(), ordem,
					propostas.metas(ordem, clubes, o.jogos()), Moeda.doPais(o.pais()), naCaixa);
		});
	}

	private List<Long> clubesDaDivisao(long jogoId, long clube) {
		return jogos.carregarLiga(jogoId).divisoes().stream()
			.map(d -> d.clubes().stream().map(Clube::id).toList())
			.filter(ids -> ids.contains(clube))
			.findFirst().orElseThrow();
	}

	/** Lugar do clube na divisão pela força em campo no início da temporada, a partir de 1; empate pelo id. */
	private int ordemDeForca(long jogoId, long clube) {
		return 1 + clubesDaDivisao(jogoId, clube).stream()
			.sorted(Comparator.<Long>comparingDouble(caixas::forcaEmCampo).reversed().thenComparing(Comparator.naturalOrder()))
			.toList().indexOf(clube);
	}

	/** Nome do patrocinador que o clube do usuário assinou nesta temporada (modelo 14a, rev. 83). */
	@Transactional(readOnly = true)
	public Optional<String> patrocinador(long jogoId, long usuarioId) {
		return oferta(jogoId, clubeDoUsuario(jogoId, usuarioId)).filter(o -> o.escolha() != null)
			.map(o -> propostas.patrocinadores(o.id()).get(Tipo.valueOf(o.escolha()).lugar()));
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
		// as propostas antigas não aparecem mais na caixa: pedir uma delas é pedido inválido
		Proposta proposta = propostas.propostasComMeta(o.tabela()).stream().filter(p -> p.tipo() == tipo).findFirst()
			.orElseThrow(() -> new NoSuchElementException("proposta " + tipo + " não está na caixa"));
		PropostasPatrocinio.Metas metas = propostas.metas(ordemDeForca(jogoId, clube), clubesDaDivisao(jogoId, clube).size(), o.jogos());
		jdbc.sql("""
				UPDATE jogo_patrocinio SET escolha = :escolha, meta_vitorias = :vitorias, meta_posicao = :posicao
				WHERE id = :id AND escolha IS NULL""")
			.param("escolha", tipo.name())
			.param("vitorias", metas.vitorias())
			.param("posicao", metas.posicao())
			.param("id", o.id())
			.update();
		caixas.lancarPatrocinio(o.temporadaId(), 0, clube, proposta.agora());
		return true;
	}

	/** Proposta por vitória: paga cada vitória da rodada jogada. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void pagarVitorias(long temporadaId, int rodada, List<ProcessamentoRodada.PartidaJogada> partidas) {
		pagarVitorias(temporadaId, rodada, partidas, Competicao.LIGA);
	}

	/**
	 * Como {@link #pagarVitorias(long, int, List)}, na competição das partidas. Na copa vale o placar da
	 * partida com a prorrogação; a disputa de pênaltis não conta como vitória.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void pagarVitorias(long temporadaId, int rodada, List<ProcessamentoRodada.PartidaJogada> partidas,
			Competicao competicao) {
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
				caixas.lancarPatrocinio(temporadaId, rodada, vencedor, proposta.porVitoria(), competicao);
			}
		}
	}

	/**
	 * Bônus do fim do ano, na última rodada da temporada que termina: das propostas com meta, a quem
	 * cumpriu a meta gravada; da proposta antiga por acesso, a quem subiu (na 1ª divisão, a quem ficou
	 * entre os primeiros).
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
		pagarMetas(t, finais);
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
					caixas.lancarPatrocinio(t.id(), t.rodadas(), clube, proposta.bonus());
				}
			}
		});
	}

	private void pagarMetas(Temporada t, Map<Integer, Classificacao> finais) {
		// a meta já paga quando ficou garantida (rev. 97) não paga de novo
		jdbc.sql("""
				SELECT clube_id, tabela, escolha, meta_vitorias, meta_posicao FROM jogo_patrocinio
				WHERE temporada_id = :temporada AND escolha IN ('META_VITORIAS', 'META_POSICAO') AND meta_paga_rodada IS NULL""")
			.param("temporada", t.id())
			.query((rs, n) -> new ContratoComMeta(rs.getLong("clube_id"), rs.getLong("tabela"), Tipo.valueOf(rs.getString("escolha")),
					rs.getInt("meta_vitorias"), rs.getInt("meta_posicao")))
			.list()
			.forEach(c -> finais.values().forEach(classificacao -> {
				for (int i = 0; i < classificacao.linhas().size(); i++) {
					LinhaClassificacao linha = classificacao.linhas().get(i);
					if (linha.clube().id() == c.clube()
							&& (c.tipo() == Tipo.META_VITORIAS ? linha.vitorias() >= c.vitorias() : i + 1 <= c.posicao())) {
						long bonus = propostas.propostasComMeta(c.tabela()).stream().filter(p -> p.tipo() == c.tipo()).findFirst()
							.orElseThrow().bonus();
						caixas.lancarPatrocinio(t.id(), t.rodadas(), c.clube(), bonus);
					}
				}
			}));
	}

	private record ContratoComMeta(long clube, long tabela, Tipo tipo, int vitorias, int posicao) {
	}

	/**
	 * Depois de cada rodada da liga (SPEC §9.6, rev. 97; modelo 20a): paga na hora a meta que ficou
	 * garantida e marca a rodada, para não pagar de novo no fim da temporada.
	 *
	 * @param temporadaId a linha de {@code jogo_temporada} de {@code temporada}
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void pagarMetasGarantidas(br.com.managerfoot.temporada.Temporada temporada, long temporadaId, int rodada) {
		List<ContratoAberto> abertos = contratosComMeta(temporadaId, "AND p.meta_paga_rodada IS NULL AND c.divisao_id IS NOT NULL");
		if (abertos.isEmpty()) {
			return;
		}
		Map<Integer, Classificacao> atuais = classificacoes.calcular(temporada, temporadaId, Integer.MAX_VALUE);
		for (ContratoAberto c : abertos) {
			Classificacao classificacao = atuais.get(c.prioridade());
			if (classificacao == null || classificacao.linhas().stream().noneMatch(l -> l.clube().id() == c.clube().id())) {
				continue;
			}
			if (c.meta().situacao(classificacao, c.clube().id(), c.jogos()).estado() == MetaPatrocinio.Estado.GARANTIDA) {
				caixas.lancarPatrocinio(temporadaId, rodada, c.clube().id(), c.meta().bonus());
				jdbc.sql("UPDATE jogo_patrocinio SET meta_paga_rodada = :rodada WHERE id = :id")
					.param("rodada", rodada)
					.param("id", c.id())
					.update();
			}
		}
	}

	/**
	 * A meta do patrocínio do usuário, para o topo do plantel e Finanças (modelo 20a). Vazio se o
	 * patrocínio não tem meta (ou ainda não foi escolhido) ou se o clube não está numa divisão.
	 *
	 * @throws NoSuchElementException se o usuário não treina clube no jogo
	 */
	@Transactional(readOnly = true)
	public Optional<MetaNaTela> meta(long jogoId, long usuarioId) {
		long clube = clubeDoUsuario(jogoId, usuarioId);
		Optional<ContratoAberto> contrato = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :jogo ORDER BY numero DESC LIMIT 1")
			.param("jogo", jogoId)
			.query(Long.class)
			.optional()
			.flatMap(t -> contratosComMeta(t, "AND p.clube_id = " + clube + " AND c.divisao_id IS NOT NULL").stream().findFirst());
		if (contrato.isEmpty()) {
			return Optional.empty();
		}
		ContratoAberto c = contrato.get();
		br.com.managerfoot.temporada.Temporada temporada = temporadas.carregarAtual(jogoId);
		Classificacao classificacao = classificacoes.calcular(temporada, c.temporadaId(), Integer.MAX_VALUE).get(c.prioridade());
		if (classificacao == null || classificacao.linhas().stream().noneMatch(l -> l.clube().id() == c.clube().id())) {
			return Optional.empty();
		}
		MetaPatrocinio.Situacao situacao = c.meta().situacao(classificacao, c.clube().id(), c.jogos());
		if (c.pagaNaRodada() != null) {
			// já paga: fica garantida mesmo que a classificação mude depois
			situacao = new MetaPatrocinio.Situacao(MetaPatrocinio.Estado.GARANTIDA, 100, situacao.vitorias(), situacao.jogos(),
					situacao.restantes(), situacao.posicao(), situacao.pontosAtras());
		}
		return Optional.of(new MetaNaTela(propostas.patrocinadores(c.id()).get(c.meta().tipo().lugar()), c.meta(), situacao,
				c.pagaNaRodada(), Moeda.doPais(c.pais())));
	}

	/**
	 * A meta na tela.
	 *
	 * @param pagaNaRodada rodada em que o bônus entrou no caixa; nula se ainda não entrou
	 */
	public record MetaNaTela(String patrocinador, MetaPatrocinio meta, MetaPatrocinio.Situacao situacao, Integer pagaNaRodada,
			Moeda moeda) {
	}

	private List<ContratoAberto> contratosComMeta(long temporadaId, String condicao) {
		return jdbc.sql("""
				SELECT p.id AS contrato_id, p.tabela, p.jogos, p.escolha, p.meta_vitorias, p.meta_posicao, p.meta_paga_rodada,
				       d.prioridade, j.pais_codigo, c.id, c.nome, c.forca, c.escudo_modelo, c.escudo_cor_1, c.escudo_cor_2
				FROM jogo_patrocinio p
				JOIN jogo_clube c ON c.id = p.clube_id
				JOIN jogo_divisao d ON d.id = c.divisao_id
				JOIN jogo_temporada t ON t.id = p.temporada_id
				JOIN jogo j ON j.id = t.jogo_id
				WHERE p.temporada_id = :temporada AND p.escolha IN ('META_VITORIAS', 'META_POSICAO') """ + condicao)
			.param("temporada", temporadaId)
			.query((rs, n) -> {
				Tipo tipo = Tipo.valueOf(rs.getString("escolha"));
				long bonus = propostas.propostasComMeta(rs.getLong("tabela")).stream().filter(x -> x.tipo() == tipo).findFirst()
					.orElseThrow().bonus();
				MetaPatrocinio meta = new MetaPatrocinio(tipo, tipo == Tipo.META_VITORIAS ? rs.getInt("meta_vitorias") : rs.getInt("meta_posicao"),
						bonus);
				return new ContratoAberto(rs.getLong("contrato_id"), temporadaId, meta, rs.getInt("jogos"), rs.getInt("prioridade"),
						TabelasLiga.clube(rs), (Integer) rs.getObject("meta_paga_rodada"), rs.getString("pais_codigo"));
			})
			.list();
	}

	private record ContratoAberto(long id, long temporadaId, MetaPatrocinio meta, int jogos, int prioridade, Clube clube,
			Integer pagaNaRodada, String pais) {
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
	 * A caixa de propostas do início do ano (modelo 14b).
	 *
	 * @param temporada ano da temporada
	 * @param ordemDeForca lugar do clube na divisão pela força, a partir de 1
	 * @param metas o que as propostas com meta pedem a este clube
	 */
	public record CaixaPatrocinio(int temporada, String clube, String divisao, int ordemDeForca, PropostasPatrocinio.Metas metas,
			Moeda moeda, List<PropostaNaCaixa> propostas) {

		public CaixaPatrocinio {
			propostas = List.copyOf(propostas);
		}
	}

	/** Uma proposta com o nome do patrocinador. */
	public record PropostaNaCaixa(Proposta proposta, String patrocinador) {
	}
}
