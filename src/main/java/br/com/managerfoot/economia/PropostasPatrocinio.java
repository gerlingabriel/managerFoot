package br.com.managerfoot.economia;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Propostas de patrocínio do início do ano (SPEC §9.6, rev. 40 e 55; modelo 9a): fixa à vista pelo
 * valor da tabela; menor à vista mais um valor por vitória; menor à vista mais um bônus se o clube
 * subir (na 1ª divisão, se ficar entre os primeiros). Domínio puro; dinheiro em centavos de Real.
 *
 * @param porVitoriaAgora fração da tabela paga à vista na proposta por vitória
 * @param vitoriasEsperadas fração das rodadas que o clube médio vence (o por vitória fecha a tabela)
 * @param seSubirAgora fração da tabela paga à vista na proposta por acesso
 * @param bonusSeSubir bônus por acesso, em vezes a tabela
 * @param primeirosNaPrimeira na 1ª divisão, que não tem para onde subir, o bônus vale para estes
 *        primeiros colocados (rev. 57)
 */
public record PropostasPatrocinio(double porVitoriaAgora, double vitoriasEsperadas, double seSubirAgora,
		double bonusSeSubir, int primeirosNaPrimeira) {

	/** Patrocinadores fictícios; os nomes são só texto (CLAUDE.md §7). */
	private static final List<String> PATROCINADORES = List.of("Banco Serra Azul", "Rede Farol", "Cervejaria Alto Rio",
			"Seguros Horizonte", "Construtora Pedra Lisa", "Laticínios Vale Verde", "Transportes Rota do Sul",
			"Telefonia Aurora", "Café Morro Alto", "Energia Ventania", "Têxtil Fio de Ouro", "Moinho Três Pontes");

	public static PropostasPatrocinio padrao() {
		return new PropostasPatrocinio(0.6, 0.37, 0.5, 1.5, 3);
	}

	/** Se o clube ganha o bônus da proposta por acesso: subiu, ou na 1ª ficou entre os primeiros. */
	public boolean subiu(int prioridadeDivisao, int posicao, int promovidos) {
		return prioridadeDivisao == 1 ? posicao <= primeirosNaPrimeira : posicao <= promovidos;
	}

	/** Vitórias do clube médio em {@code jogos}, para mostrar o total da proposta por vitória. */
	public int vitoriasEsperadas(int jogos) {
		return (int) Math.round(jogos * vitoriasEsperadas);
	}

	/** Um patrocinador diferente para cada proposta, na ordem de {@link Tipo}, sorteado pela semente. */
	public List<String> patrocinadores(long semente) {
		List<String> nomes = new ArrayList<>(PATROCINADORES);
		SplittableRandom aleatorio = new SplittableRandom(semente);
		List<String> escolhidos = new ArrayList<>();
		for (int i = 0; i < Tipo.values().length; i++) {
			escolhidos.add(nomes.remove(aleatorio.nextInt(nomes.size())));
		}
		return escolhidos;
	}

	public enum Tipo {
		FIXA, POR_VITORIA, SE_SUBIR
	}

	/** Uma proposta: o que paga agora e o que pode pagar depois. */
	public record Proposta(Tipo tipo, long agora, long porVitoria, long seSubir) {

		public long total(int vitorias, boolean subiu) {
			return agora + porVitoria * vitorias + (subiu ? seSubir : 0);
		}
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
