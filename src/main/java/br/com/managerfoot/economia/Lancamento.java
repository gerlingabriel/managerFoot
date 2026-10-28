package br.com.managerfoot.economia;

/** Entrada (positiva) ou saída (negativa) do caixa, em centavos de Real (SPEC §13, economia rastreável). */
public record Lancamento(TipoLancamento tipo, long valor) {
}
