package br.com.managerfoot.motor;

/**
 * Ajuste de equilíbrio de uma formação (SPEC §7.2, rev. 113). Cada fator vale 1 sem ajuste e
 * multiplica o efeito indicado; vem do esquema escolhido, não dos jogadores, então continua valendo
 * depois de expulsões e substituições.
 *
 * @param meio multiplica a força do meio-campo na disputa de quem fica com a jogada
 * @param ataque multiplica a conversão das finalizações do próprio time
 * @param defesa divide a conversão das finalizações do adversário contra o time
 */
public record AjusteFormacao(double meio, double ataque, double defesa) {

	public static final AjusteFormacao NENHUM = new AjusteFormacao(1, 1, 1);

	public AjusteFormacao {
		if (meio <= 0 || ataque <= 0 || defesa <= 0) {
			throw new IllegalArgumentException("os fatores do ajuste da formação devem ser positivos");
		}
	}
}
