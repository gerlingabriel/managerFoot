package br.com.managerfoot.motor;

import java.util.List;

import br.com.managerfoot.dominio.Jogador;

/**
 * Disputa de pênaltis depois da prorrogação (SPEC §8.2): cinco cobranças alternadas para cada lado,
 * encerrada assim que um não alcança mais o outro, e depois cobranças alternadas até um converter e
 * o outro não. Os gols da disputa não contam para os artilheiros (SPEC §5.2).
 *
 * @param cobrancas em ordem
 */
public record DisputaPenaltis(List<Cobranca> cobrancas) {

	/** Cobranças de cada lado antes das alternadas. */
	public static final int COBRANCAS_INICIAIS = 5;

	/** Uma cobrança da disputa. */
	public record Cobranca(Lado lado, Jogador batedor, boolean gol) {
	}

	public DisputaPenaltis {
		cobrancas = List.copyOf(cobrancas);
	}

	public int gols(Lado lado) {
		return (int) cobrancas.stream().filter(c -> c.lado() == lado && c.gol()).count();
	}

	public Lado vencedor() {
		int casa = gols(Lado.CASA);
		int fora = gols(Lado.FORA);
		if (casa == fora) {
			throw new IllegalStateException("disputa sem vencedor: " + casa + "x" + fora);
		}
		return casa > fora ? Lado.CASA : Lado.FORA;
	}
}
