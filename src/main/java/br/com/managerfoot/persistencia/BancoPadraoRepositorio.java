package br.com.managerfoot.persistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.bancopadrao.ContinentalPadrao;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao.BancoGerado;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao.ClubeGerado;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao.EstrangeiroGerado;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao.PaisGerado;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.temporada.FormatoDivisao;

/**
 * Grava o banco padrão gerado como um molde (docs/decisions/0008). Carregar de novo não duplica:
 * se o molde do banco padrão já existe na versão atual, nada é gravado; se é de uma versão antiga
 * do gerador, ele é apagado e gravado de novo (jogos já criados são cópias e não mudam).
 */
@Repository
public class BancoPadraoRepositorio {

	/** Nome do molde que guarda o banco padrão. */
	public static final String NOME_MOLDE = "Banco padrão";
	/**
	 * Versão do gerador: muda quando o banco gerado muda (2 na rev. 60: 2 divisões e 20 jogadores; 3 na
	 * rev. 73: países sem liga e vagas da continental; 4 na rev. 87: estrangeiros nos planteis e mais asteriscos).
	 */
	public static final int VERSAO = 4;

	private final JdbcClient jdbc;
	private final MoldeRepositorio moldes;

	public BancoPadraoRepositorio(JdbcClient jdbc, MoldeRepositorio moldes) {
		this.jdbc = jdbc;
		this.moldes = moldes;
	}

	/**
	 * Resultado da carga: o id do molde e se ele foi criado agora.
	 *
	 * @param substituido havia um molde de versão antiga, que foi apagado
	 */
	public record Carga(long moldeId, boolean criado, boolean substituido) {
	}

	/** Grava o banco gerado num molde novo, ou devolve o que já existe na versão atual. */
	@Transactional
	public Carga carregar(BancoGerado banco) {
		List<PaisGerado> paises = banco.paises();
		// duas cargas ao mesmo tempo esperam uma pela outra e a segunda encontra o molde pronto
		jdbc.sql("SELECT 1 FROM pg_advisory_xact_lock(hashtext('managerfoot.banco-padrao'))").query(Integer.class).single();
		Optional<Long> existente = moldeId();
		if (existente.isPresent() && versao(existente.get()) >= VERSAO) {
			completarTreinadores(existente.get(), paises);
			return new Carga(existente.get(), false, false);
		}
		existente.ifPresent(antigo -> jdbc.sql("DELETE FROM molde WHERE id = :molde").param("molde", antigo).update());
		long molde = moldes.criarMolde(NOME_MOLDE);
		jdbc.sql("UPDATE molde SET versao_banco_padrao = :versao WHERE id = :molde")
			.param("versao", VERSAO)
			.param("molde", molde)
			.update();
		for (PaisGerado gerado : paises) {
			long pais = moldes.criarPais(molde, gerado.pais().codigo(), gerado.pais().nome());
			moldes.criarLiga(pais, gerado.pais().formato().nome());
			for (FormatoDivisao divisao : gerado.pais().formato().divisoes()) {
				moldes.adicionarDivisao(pais, divisao.nome(), divisao.quantidadeClubes(), divisao.limiteForca(),
						divisao.rebaixados());
			}
			for (ClubeGerado clube : gerado.clubes()) {
				Clube gravado = moldes.adicionarClube(pais, clube.clube(), clube.treinador());
				moldes.adicionarJogadores(gravado.id(), clube.jogadores());
			}
		}
		// sem liga: os clubes ficam no país e a liga não é criada (SPEC §8.3.3)
		for (EstrangeiroGerado gerado : banco.estrangeiros()) {
			long pais = moldes.criarPais(molde, gerado.pais().codigo(), gerado.pais().nome());
			for (ClubeGerado clube : gerado.clubes()) {
				Clube gravado = moldes.adicionarClube(pais, clube.clube(), clube.treinador());
				moldes.adicionarJogadores(gravado.id(), clube.jogadores());
			}
		}
		for (ContinentalPadrao continental : banco.continentais()) {
			long id = moldes.criarContinental(molde, continental.regiao().name(), continental.nome());
			continental.vagas().forEach((codigo, vagas) -> moldes.adicionarVagas(id, codigo, vagas));
		}
		return new Carga(molde, true, existente.isPresent());
	}

	private int versao(long moldeId) {
		return jdbc.sql("SELECT coalesce(versao_banco_padrao, 1) FROM molde WHERE id = :molde")
			.param("molde", moldeId)
			.query(Integer.class)
			.single();
	}

	/**
	 * Banco padrão carregado antes dos treinadores (V23): troca o nome provisório pelo gerado. O gerador
	 * tem semente fixa, então os clubes de cada país vêm na mesma ordem em que foram gravados.
	 */
	private void completarTreinadores(long moldeId, List<PaisGerado> paises) {
		for (PaisGerado gerado : paises) {
			Optional<Long> pais = jdbc.sql("SELECT id FROM molde_pais WHERE molde_id = :molde AND codigo = :codigo")
				.param("molde", moldeId)
				.param("codigo", gerado.pais().codigo())
				.query(Long.class)
				.optional();
			if (pais.isEmpty()) {
				continue;
			}
			List<Long> clubes = List.copyOf(moldes.treinadores(pais.get()).keySet());
			for (int i = 0; i < Math.min(clubes.size(), gerado.clubes().size()); i++) {
				jdbc.sql("UPDATE molde_clube SET treinador = :treinador WHERE id = :clube AND treinador = 'Treinador do ' || nome")
					.param("treinador", gerado.clubes().get(i).treinador())
					.param("clube", clubes.get(i))
					.update();
			}
		}
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
