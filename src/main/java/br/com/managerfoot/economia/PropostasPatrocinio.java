package br.com.managerfoot.economia;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Propostas de patrocínio do início do ano (SPEC §9.6, modelo 14b, rev. 85): fixa à vista pelo valor da
 * tabela; menor à vista mais um bônus no fim do ano por uma meta de vitórias; ainda menor à vista mais
 * um bônus maior por uma meta de posição. As metas dependem da força do clube na divisão. As propostas
 * antigas, por vitória e por acesso (modelo 9a), ficam só para os contratos já assinados. Domínio puro;
 * dinheiro em centavos de Real.
 *
 * @param porVitoriaAgora fração da tabela paga à vista na proposta por vitória
 * @param vitoriasEsperadas fração das rodadas que o clube médio vence (o por vitória fecha a tabela)
 * @param seSubirAgora fração da tabela paga à vista na proposta por acesso
 * @param bonusSeSubir bônus por acesso, em vezes a tabela
 * @param primeirosNaPrimeira na 1ª divisão, que não tem para onde subir, o bônus vale para estes
 *        primeiros colocados (rev. 57)
 * @param metaVitoriasAgora fração da tabela à vista na proposta com meta de vitórias
 * @param metaVitoriasBonus bônus da meta de vitórias, em vezes a tabela
 * @param metaPosicaoAgora fração da tabela à vista na proposta com meta de posição
 * @param metaPosicaoBonus bônus da meta de posição, em vezes a tabela
 * @param fracaoFortes até esta fração dos clubes da divisão, por força, conta como dos mais fortes
 * @param fracaoMedios até esta fração, como do meio; depois, como dos mais fracos
 * @param vitoriasFortes fração dos jogos que a meta de vitórias pede aos mais fortes
 * @param vitoriasMedios a mesma fração para os do meio
 * @param vitoriasFracos a mesma fração para os mais fracos
 */
public record PropostasPatrocinio(double porVitoriaAgora, double vitoriasEsperadas, double seSubirAgora,
		double bonusSeSubir, int primeirosNaPrimeira, double metaVitoriasAgora, double metaVitoriasBonus,
		double metaPosicaoAgora, double metaPosicaoBonus, double fracaoFortes, double fracaoMedios, double vitoriasFortes,
		double vitoriasMedios, double vitoriasFracos) {

	/** Patrocinadores fictícios; os nomes são só texto (CLAUDE.md §7). */
	private static final List<String> PATROCINADORES = List.of("Banco Serra Azul", "Rede Farol", "Cervejaria Alto Rio",
			"Seguros Horizonte", "Construtora Pedra Lisa", "Laticínios Vale Verde", "Transportes Rota do Sul",
			"Telefonia Aurora", "Café Morro Alto", "Energia Ventania", "Têxtil Fio de Ouro", "Moinho Três Pontes");

	public static PropostasPatrocinio padrao() {
		// metas simuladas por força (docs/simulacoes/estadio-publico-patrocinio.md): 22, 19 e 14 vitórias em 38 jogos
		return new PropostasPatrocinio(0.6, 0.37, 0.5, 1.5, 3, 0.6, 0.9, 0.4, 2.0, 0.2, 0.5, 22.0 / 38, 19.0 / 38, 14.0 / 38);
	}

	/** Se o clube ganha o bônus da proposta por acesso: subiu, ou na 1ª ficou entre os primeiros. */
	public boolean subiu(int prioridadeDivisao, int posicao, int promovidos) {
		return prioridadeDivisao == 1 ? posicao <= primeirosNaPrimeira : posicao <= promovidos;
	}

	/** Vitórias do clube médio em {@code jogos}, para mostrar o total da proposta por vitória. */
	public int vitoriasEsperadas(int jogos) {
		return (int) Math.round(jogos * vitoriasEsperadas);
	}

	/** Três patrocinadores diferentes, na ordem de {@link Tipo#lugar()}, sorteados pela semente. */
	public List<String> patrocinadores(long semente) {
		List<String> nomes = new ArrayList<>(PATROCINADORES);
		SplittableRandom aleatorio = new SplittableRandom(semente);
		List<String> escolhidos = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			escolhidos.add(nomes.remove(aleatorio.nextInt(nomes.size())));
		}
		return escolhidos;
	}

	public enum Tipo {
		FIXA(0), POR_VITORIA(1), SE_SUBIR(2), META_VITORIAS(1), META_POSICAO(2);

		private final int lugar;

		Tipo(int lugar) {
			this.lugar = lugar;
		}

		/** Posição na caixa, que decide o nome do patrocinador. */
		public int lugar() {
			return lugar;
		}
	}

	/** Uma proposta: o que paga agora e o que pode pagar depois. */
	public record Proposta(Tipo tipo, long agora, long porVitoria, long bonus) {

		public long total(int vitorias, boolean cumpriu) {
			return agora + porVitoria * vitorias + (cumpriu ? bonus : 0);
		}
	}

	/** O que as propostas com meta pedem na liga. */
	public record Metas(int vitorias, int posicao) {
	}

	/**
	 * As metas de um clube.
	 *
	 * @param ordemDeForca lugar do clube na divisão pela força, a partir de 1
	 * @param clubes clubes da divisão
	 * @param jogos jogos de cada clube na liga
	 */
	public Metas metas(int ordemDeForca, int clubes, int jogos) {
		if (ordemDeForca <= Math.round(clubes * fracaoFortes)) {
			return new Metas((int) Math.round(jogos * vitoriasFortes), 1);
		}
		if (ordemDeForca <= Math.round(clubes * fracaoMedios)) {
			return new Metas((int) Math.round(jogos * vitoriasMedios), 3);
		}
		return new Metas((int) Math.round(jogos * vitoriasFracos), Math.max(1, clubes / 2));
	}

	/** As três propostas da caixa (modelo 14b, rev. 85). */
	public List<Proposta> propostasComMeta(long tabela) {
		return List.of(new Proposta(Tipo.FIXA, tabela, 0, 0),
				new Proposta(Tipo.META_VITORIAS, redondo(tabela * metaVitoriasAgora), 0, redondo(tabela * metaVitoriasBonus)),
				new Proposta(Tipo.META_POSICAO, redondo(tabela * metaPosicaoAgora), 0, redondo(tabela * metaPosicaoBonus)));
	}

	/**
	 * @param tabela patrocínio da tabela pela posição anterior (§9.6, rev. 46)
	 * @param rodadas rodadas da divisão na temporada
	 */
	public List<Proposta> propostas(long tabela, int rodadas) {
		long agoraPorVitoria = redondo(tabela * porVitoriaAgora);
		// por vitória em múltiplos de R$ 100: na 3ª divisão o valor fica perto de R$ 1.000
		long porVitoria = Math.round((tabela - agoraPorVitoria) / Math.max(1, rodadas * vitoriasEsperadas) / 100_00) * 100_00;
		return List.of(new Proposta(Tipo.FIXA, tabela, 0, 0), new Proposta(Tipo.POR_VITORIA, agoraPorVitoria, porVitoria, 0),
				new Proposta(Tipo.SE_SUBIR, redondo(tabela * seSubirAgora), 0, redondo(tabela * bonusSeSubir)));
	}

	/** Em múltiplos de R$ 1.000. */
	private static long redondo(double centavos) {
		return Math.round(centavos / 1_000_00) * 1_000_00;
	}
}
