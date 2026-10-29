package br.com.managerfoot.economia;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;

/**
 * Prêmio da liga no fim da temporada e patrocínio no início (SPEC §8.1 e §9.6, rev. 45 e 46). O prêmio do
 * campeão é o preço de jogadores com asterisco do teto de força da divisão; o 2º e o 3º recebem uma
 * fração; o patrocínio é uma fração do prêmio do campeão que cai a cada posição na temporada anterior. Em
 * centavos de Real.
 */
public final class Premiacao {

	private final ParametrosPremiacao parametros;
	private final PrecoJogador preco;

	public Premiacao(ParametrosPremiacao parametros, PrecoJogador preco) {
		this.parametros = parametros;
		this.preco = preco;
	}

	public static Premiacao padrao() {
		return new Premiacao(ParametrosPremiacao.padrao(), PrecoJogador.padrao());
	}

	/**
	 * Prêmio do campeão.
	 *
	 * @param forcaReferencia força dos jogadores que o prêmio compra: o teto de força da divisão
	 *        ({@link br.com.managerfoot.temporada.Divisao#limiteForca()})
	 */
	public long premioDoCampeao(int forcaReferencia) {
		// jogador nacional: o país do jogador é o da liga
		Jogador referencia = new Jogador(0, "referência", Posicao.ATACANTE, forcaReferencia, Temperamento.NORMAL, true,
				"BRA");
		return preco.preco(referencia, "BRA", referencia.forca()) * parametros.jogadoresNoPremio();
	}

	/** Prêmio pela posição final na liga; do 4º em diante, nada. */
	public long premio(int posicao, int forcaReferencia) {
		long campeao = premioDoCampeao(forcaReferencia);
		return switch (posicao) {
			case 1 -> campeao;
			case 2 -> preco.redondo(Math.round(campeao * parametros.segundoLugar()));
			case 3 -> preco.redondo(Math.round(campeao * parametros.terceiroLugar()));
			default -> 0;
		};
	}

	/**
	 * Patrocínio da temporada pela posição na anterior: o 1º recebe a fração cheia, o 2º um pouco menos
	 * e cada posição seguinte uma porcentagem menos que a anterior.
	 */
	public long patrocinio(int posicaoAnterior, int clubes, int forcaReferencia) {
		if (posicaoAnterior < 1 || posicaoAnterior > clubes) {
			throw new IllegalArgumentException("posição " + posicaoAnterior + " fora de 1 a " + clubes);
		}
		return preco.redondo(Math.round(cheio(forcaReferencia) * fator(posicaoAnterior)));
	}

	/** Patrocínio da primeira temporada: igual para todos, a média do que as posições receberiam. */
	public long patrocinioInicial(int clubes, int forcaReferencia) {
		double soma = 0;
		for (int posicao = 1; posicao <= clubes; posicao++) {
			soma += fator(posicao);
		}
		return preco.redondo(Math.round(cheio(forcaReferencia) * soma / clubes));
	}

	private double fator(int posicao) {
		return posicao == 1 ? 1
				: (1 - parametros.patrocinioSegundo()) * Math.pow(1 - parametros.patrocinioPorPosicao(), posicao - 2);
	}

	private double cheio(int forcaReferencia) {
		return premioDoCampeao(forcaReferencia) * parametros.patrocinioSobrePremio();
	}
}
