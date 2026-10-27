package br.com.managerfoot.dominio;

/**
 * Clube no mínimo necessário à estrutura da liga (Etapa 2). Estádio, cores, país e finanças
 * entram nas etapas seguintes.
 *
 * @param forca força cadastrada (SPEC §4.3), na escala de {@link Jogador#FORCA_MINIMA} a
 *        {@link Jogador#FORCA_MAXIMA}
 */
public record Clube(long id, String nome, int forca) {

	public Clube {
		if (nome == null || nome.isBlank()) {
			throw new IllegalArgumentException("nome do clube é obrigatório");
		}
		if (forca < Jogador.FORCA_MINIMA || forca > Jogador.FORCA_MAXIMA) {
			throw new IllegalArgumentException("força de " + nome + " deve estar entre " + Jogador.FORCA_MINIMA + " e "
					+ Jogador.FORCA_MAXIMA + ", mas foi " + forca);
		}
	}
}
