package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.bancopadrao.Regiao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.persistencia.BancoEditavel.ClubeEditavel;
import br.com.managerfoot.persistencia.BancoEditavel.Continental;
import br.com.managerfoot.persistencia.BancoEditavel.Pais;
import br.com.managerfoot.persistencia.JogosDoTreinadorRepositorio.NovoJogo;
import br.com.managerfoot.temporada.FormatoDivisao;
import br.com.managerfoot.temporada.FormatoLiga;

/**
 * Banco editável no banco de dados (SPEC §3.5, rev. 89): baixar o banco padrão como arquivo e criar um jogo de um
 * arquivo enviado. O arquivo vira um molde só durante a criação do jogo e é apagado em seguida: o servidor não guarda
 * o banco da pessoa, só o jogo criado dele.
 */
@Repository
public class BancoEditavelRepositorio {

	private static final String NOME_MOLDE_TEMPORARIO = "Banco enviado";

	private final JdbcClient jdbc;
	private final MoldeRepositorio moldes;
	private final BancoPadraoRepositorio bancoPadrao;
	private final JogosDoTreinadorRepositorio jogos;

	public BancoEditavelRepositorio(JdbcClient jdbc, MoldeRepositorio moldes, BancoPadraoRepositorio bancoPadrao,
			JogosDoTreinadorRepositorio jogos) {
		this.jdbc = jdbc;
		this.moldes = moldes;
		this.bancoPadrao = bancoPadrao;
		this.jogos = jogos;
	}

	/** O banco padrão inteiro, como arquivo para editar. */
	@Transactional(readOnly = true)
	public byte[] baixarBancoPadrao() {
		long molde = bancoPadrao.moldeId().orElseThrow(() -> new NoSuchElementException("o banco padrão não foi carregado"));
		return carregar(molde).paraArquivo();
	}

	/** O molde como banco editável, com os países na ordem em que foram gravados. */
	BancoEditavel carregar(long moldeId) {
		record Linha(long id, String codigo, String nome, String regiao, String liga) {
		}
		List<Linha> linhas = jdbc.sql("SELECT id, codigo, nome, regiao, nome_liga FROM molde_pais WHERE molde_id = :molde ORDER BY id")
			.param("molde", moldeId)
			.query((rs, n) -> new Linha(rs.getLong("id"), rs.getString("codigo"), rs.getString("nome"), rs.getString("regiao"),
					rs.getString("nome_liga")))
			.list();
		List<Pais> paises = new ArrayList<>();
		for (Linha linha : linhas) {
			Map<Long, String> treinadores = moldes.treinadores(linha.id());
			List<ClubeEditavel> clubes = moldes.carregarClubes(linha.id()).stream()
				.map(c -> new ClubeEditavel(c, treinadores.get(c.id()), moldes.carregarPlantel(c.id())))
				.toList();
			paises.add(new Pais(linha.codigo(), linha.nome(), linha.regiao() == null ? null : Regiao.valueOf(linha.regiao()),
					linha.liga() == null ? Optional.empty() : Optional.of(moldes.carregarFormato(linha.id())), clubes));
		}
		List<Continental> continentais = moldes.continentais(moldeId).stream()
			.map(c -> new Continental(Regiao.valueOf(c.regiao()), c.nome(), c.vagas()))
			.toList();
		return new BancoEditavel(paises, continentais);
	}

	/**
	 * Lê e confere o arquivo enviado.
	 *
	 * @throws BancoRecusadoException com todos os problemas
	 */
	@Transactional(readOnly = true)
	public BancoEditavel ler(byte[] arquivo) {
		Set<String> catalogo = new HashSet<>(jdbc.sql("SELECT codigo FROM pais_fifa").query(String.class).list());
		return BancoEditavel.ler(arquivo, catalogo::contains);
	}

	/**
	 * Cria o jogo com o clube sorteado, como no banco padrão, a partir do campeonato do arquivo.
	 *
	 * @throws BancoRecusadoException se o arquivo não passa na validação
	 * @throws NoSuchElementException se o arquivo não tem liga desse país
	 */
	@Transactional
	public NovoJogo criarJogo(byte[] arquivo, String codigoPais, long usuarioId) {
		BancoEditavel banco = ler(arquivo);
		if (banco.pais(codigoPais).flatMap(Pais::liga).isEmpty()) {
			throw new NoSuchElementException("o arquivo não tem liga do país " + codigoPais);
		}
		long molde = moldes.criarMolde(NOME_MOLDE_TEMPORARIO);
		long paisMolde = gravar(molde, banco, codigoPais);
		NovoJogo novo = jogos.criarComSorteio(paisMolde, usuarioId);
		// o molde só serve à cópia; o jogo fica com molde_id nulo, como os jogos de um banco padrão substituído
		jdbc.sql("DELETE FROM molde WHERE id = :molde").param("molde", molde).update();
		return novo;
	}

	/** Grava o banco no molde e devolve o id do país jogado. */
	private long gravar(long molde, BancoEditavel banco, String codigoPais) {
		long jogado = -1;
		for (Pais pais : banco.paises()) {
			long id = moldes.criarPais(molde, pais.codigo(), pais.nome());
			if (pais.regiao() != null) {
				moldes.definirRegiao(id, pais.regiao().name());
			}
			if (pais.liga().isPresent()) {
				FormatoLiga liga = pais.liga().get();
				moldes.criarLiga(id, liga.nome());
				for (FormatoDivisao divisao : liga.divisoes()) {
					moldes.adicionarDivisao(id, divisao.nome(), divisao.quantidadeClubes(), divisao.limiteForca(), divisao.rebaixados());
				}
			}
			for (ClubeEditavel clube : pais.clubes()) {
				Clube gravado = moldes.adicionarClube(id, clube.clube(), clube.treinador());
				moldes.adicionarJogadores(gravado.id(), clube.jogadores());
			}
			if (pais.codigo().equals(codigoPais)) {
				jogado = id;
			}
		}
		for (Continental continental : banco.continentais()) {
			long id = moldes.criarContinental(molde, continental.regiao().name(), continental.nome());
			continental.vagas().forEach((codigo, vagas) -> moldes.adicionarVagas(id, codigo, vagas));
		}
		return jogado;
	}
}
