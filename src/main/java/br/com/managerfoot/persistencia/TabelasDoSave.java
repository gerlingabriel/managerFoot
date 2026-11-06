package br.com.managerfoot.persistencia;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O que entra no arquivo de save (SPEC §3.5), na ordem de gravação: cada tabela só referencia as anteriores. Os ids
 * do arquivo nunca são usados no banco: cada referência daqui é trocada pelo id novo. {@code SaveRepositorioIT} confere
 * esta lista com o esquema, para uma migração nova não ficar fora do save sem aviso.
 */
final class TabelasDoSave {

	/** As da rodada ao vivo só existem durante ela, e o save a recusa; as da rede porque jogo em rede não baixa arquivo. */
	static final Set<String> FORA_DO_SAVE = Set.of("jogo_rodada_ao_vivo", "jogo_substituicao_ao_vivo", "jogo_rede",
			"jogo_rede_convite", "jogo_rede_participante", "jogo_rede_lance", "jogo_pausa_participante");

	private static final String JOGO = "jogo";
	private static final String DIVISAO = "jogo_divisao";
	private static final String CLUBE = "jogo_clube";
	private static final String JOGADOR = "jogo_jogador";
	private static final String TEMPORADA = "jogo_temporada";
	private static final String CONFRONTO = "jogo_copa_confronto";
	private static final String PARTIDA = "jogo_partida";
	private static final String MAQUINA = "jogo_treinador_maquina";
	private static final String LEILAO = "jogo_leilao";

	static final List<Tabela> TODAS = List.of(
			new Tabela(JOGO, "id", List.of("id"), null, null, Map.of(), Set.of("dono_id"), Set.of("molde_id")),
			gerada(DIVISAO, "jogo_id", JOGO, Map.of("jogo_id", JOGO)),
			gerada(CLUBE, "jogo_id", JOGO, Map.of("jogo_id", JOGO, "divisao_id", DIVISAO)),
			gerada(JOGADOR, "clube_id", CLUBE, Map.of("clube_id", CLUBE)),
			gerada(TEMPORADA, "jogo_id", JOGO, Map.of("jogo_id", JOGO)),
			composta("jogo_temporada_divisao", List.of("temporada_id", "divisao_id"), "temporada_id", TEMPORADA,
					Map.of("temporada_id", TEMPORADA, "divisao_id", DIVISAO, "beneficiado_id", CLUBE, "prejudicado_id", CLUBE)),
			composta("jogo_temporada_beneficiado", List.of("temporada_id", "clube_id"), "temporada_id", TEMPORADA,
					Map.of("temporada_id", TEMPORADA, "divisao_id", DIVISAO, "clube_id", CLUBE)),
			gerada(CONFRONTO, "temporada_id", TEMPORADA, Map.of("temporada_id", TEMPORADA, "mandante_id", CLUBE, "visitante_id", CLUBE)),
			gerada(PARTIDA, "temporada_id", TEMPORADA, Map.of("temporada_id", TEMPORADA, "divisao_id", DIVISAO, "mandante_id", CLUBE,
					"visitante_id", CLUBE, "confronto_id", CONFRONTO)),
			composta("jogo_bonus_artilharia", List.of("temporada_id", "jogador_id"), "temporada_id", TEMPORADA,
					Map.of("temporada_id", TEMPORADA, "divisao_id", DIVISAO, "jogador_id", JOGADOR)),
			composta("jogo_bonus_treinador_sorteio", List.of("temporada_id", "clube_id"), "temporada_id", TEMPORADA,
					Map.of("temporada_id", TEMPORADA, "clube_id", CLUBE)),
			composta("jogo_bonus_treinador", List.of("temporada_id", "jogador_id"), "temporada_id", TEMPORADA,
					Map.of("temporada_id", TEMPORADA, "jogador_id", JOGADOR, "clube_id", CLUBE)),
			gerada("jogo_evento", "partida_id", PARTIDA, Map.of("partida_id", PARTIDA, "clube_id", CLUBE, "jogador_id", JOGADOR)),
			new Tabela("jogo_treinador", "id", List.of("id"), "jogo_id", JOGO, Map.of("jogo_id", JOGO, "clube_id", CLUBE),
					Set.of("usuario_id"), Set.of()),
			gerada(MAQUINA, "jogo_id", JOGO, Map.of("jogo_id", JOGO, "clube_id", CLUBE)),
			composta("jogo_escalacao", List.of("clube_id"), "clube_id", CLUBE, Map.of("clube_id", CLUBE, "batedor_id", JOGADOR)),
			composta("jogo_escalacao_jogador", List.of("clube_id", "jogador_id"), "clube_id", CLUBE,
					Map.of("clube_id", CLUBE, "jogador_id", JOGADOR)),
			gerada("jogo_lancamento", "temporada_id", TEMPORADA, Map.of("clube_id", CLUBE, "temporada_id", TEMPORADA, "jogador_id", JOGADOR)),
			campeao("jogo_campeao", Set.of("usuario_id", "vice_usuario_id", "terceiro_usuario_id"),
					Map.of("jogo_id", JOGO, "clube_id", CLUBE, "vice_id", CLUBE, "terceiro_id", CLUBE, "treinador_maquina_id", MAQUINA)),
			campeao("jogo_campeao_copa"),
			campeao("jogo_campeao_continental"),
			campeao("jogo_campeao_mundial"),
			gerada(LEILAO, "temporada_id", TEMPORADA, Map.of("temporada_id", TEMPORADA, "jogador_id", JOGADOR, "vendedor_id", CLUBE,
					"comprador_id", CLUBE)),
			composta("jogo_leilao_lance", List.of("leilao_id", "clube_id"), "leilao_id", LEILAO, Map.of("leilao_id", LEILAO, "clube_id", CLUBE)),
			gerada("jogo_patrocinio", "temporada_id", TEMPORADA, Map.of("clube_id", CLUBE, "temporada_id", TEMPORADA)),
			new Tabela("jogo_troca_treinador", "id", List.of("id"), "jogo_id", JOGO,
					Map.of("jogo_id", JOGO, "temporada_id", TEMPORADA, "clube_id", CLUBE, "outro_clube_id", CLUBE),
					Set.of("convidado_id", "demitido_id"), Set.of()),
			composta("jogo_continental", List.of("jogo_id"), "jogo_id", JOGO, Map.of("jogo_id", JOGO)),
			composta("jogo_continental_vaga", List.of("jogo_id", "pais_codigo"), "jogo_id", JOGO, Map.of("jogo_id", JOGO)),
			composta("jogo_continental_grupo", List.of("temporada_id", "clube_id"), "temporada_id", TEMPORADA,
					Map.of("temporada_id", TEMPORADA, "clube_id", CLUBE)));

	private TabelasDoSave() {
	}

	private static Tabela gerada(String nome, String colunaDoPai, String pai, Map<String, String> referencias) {
		return new Tabela(nome, "id", List.of("id"), colunaDoPai, pai, referencias, Set.of(), Set.of());
	}

	private static Tabela composta(String nome, List<String> ordem, String colunaDoPai, String pai, Map<String, String> referencias) {
		return new Tabela(nome, null, ordem, colunaDoPai, pai, referencias, Set.of(), Set.of());
	}

	private static Tabela campeao(String nome) {
		return campeao(nome, Set.of("usuario_id", "vice_usuario_id"),
				Map.of("jogo_id", JOGO, "clube_id", CLUBE, "vice_id", CLUBE, "treinador_maquina_id", MAQUINA));
	}

	private static Tabela campeao(String nome, Set<String> usuarios, Map<String, String> referencias) {
		return new Tabela(nome, "id", List.of("id"), "jogo_id", JOGO, referencias, usuarios, Set.of());
	}

	/** A tabela pelo nome; nulo se ela não faz parte do save. */
	static Tabela buscar(String nome) {
		return TODAS.stream().filter(t -> t.nome().equals(nome)).findFirst().orElse(null);
	}

	/**
	 * Uma tabela do save.
	 *
	 * @param chaveGerada coluna com o id gerado pelo banco; nula se a chave é feita de referências
	 * @param ordem colunas que deixam as linhas sempre na mesma ordem
	 * @param colunaDoPai coluna que liga a linha ao jogo, direto ou pela tabela {@code pai}; nula só no próprio jogo
	 * @param referencias coluna com id de outra tabela do save e qual é a tabela
	 * @param usuarios colunas com id de conta: valem para quem envia o arquivo, nunca como vieram
	 * @param omitidas colunas que não vão para o arquivo
	 */
	record Tabela(String nome, String chaveGerada, List<String> ordem, String colunaDoPai, String pai,
			Map<String, String> referencias, Set<String> usuarios, Set<String> omitidas) {

		/** Condição SQL das linhas de um jogo, com o parâmetro {@code :jogo}. Só usa nomes fixos desta classe. */
		String filtro() {
			if (pai == null) {
				return "id = :jogo";
			}
			if (pai.equals(JOGO)) {
				return colunaDoPai + " = :jogo";
			}
			return colunaDoPai + " IN (SELECT id FROM " + pai + " WHERE " + buscar(pai).filtro() + ")";
		}
	}
}
