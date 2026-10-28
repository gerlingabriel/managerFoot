package br.com.managerfoot.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.bancopadrao.GeradorBancoPadrao.ClubeGerado;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao.PaisGerado;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.temporada.FormatoDivisao;

/**
 * Grava o banco padrão gerado como um molde (docs/decisions/0008). Carregar de novo não duplica:
 * se o molde do banco padrão já existe, nada é gravado.
 */
@Repository
public class BancoPadraoRepositorio {

	/** Nome do molde que guarda o banco padrão. */
	public static final String NOME_MOLDE = "Banco padrão";

	private final JdbcClient jdbc;
	private final MoldeRepositorio moldes;

	public BancoPadraoRepositorio(JdbcClient jdbc, MoldeRepositorio moldes) {
		this.jdbc = jdbc;
		this.moldes = moldes;
	}

	/** Resultado da carga: o id do molde e se ele foi criado agora. */
	public record Carga(long moldeId, boolean criado) {
	}

	/** Grava os países gerados num molde novo, ou devolve o que já existe. */
	@Transactional
	public Carga carregar(List<PaisGerado> paises) {
		// duas cargas ao mesmo tempo esperam uma pela outra e a segunda encontra o molde pronto
		jdbc.sql("SELECT 1 FROM pg_advisory_xact_lock(hashtext('managerfoot.banco-padrao'))").query(Integer.class).single();
		Optional<Long> existente = moldeId();
		if (existente.isPresent()) {
			return new Carga(existente.get(), false);
		}
		long molde = moldes.criarMolde(NOME_MOLDE);
		for (PaisGerado gerado : paises) {
			long pais = moldes.criarPais(molde, gerado.pais().codigo(), gerado.pais().nome());
			moldes.criarLiga(pais, gerado.pais().formato().nome());
			for (FormatoDivisao divisao : gerado.pais().formato().divisoes()) {
				moldes.adicionarDivisao(pais, divisao.nome(), divisao.quantidadeClubes(), divisao.limiteForca(),
						divisao.rebaixados());
			}
			for (ClubeGerado clube : gerado.clubes()) {
				Clube gravado = moldes.adicionarClube(pais, clube.clube());
				moldes.adicionarJogadores(gravado.id(), clube.jogadores());
			}
		}
		return new Carga(molde, true);
	}

	/** Id do molde do banco padrão, se já foi carregado. */
	@Transactional(readOnly = true)
	public Optional<Long> moldeId() {
		return jdbc.sql("SELECT id FROM molde WHERE nome = :nome ORDER BY id LIMIT 1")
			.param("nome", NOME_MOLDE)
			.query(Long.class)
			.optional();
	}
}
