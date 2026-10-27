package br.com.managerfoot.temporada;

/**
 * Divisão no formato da liga do molde ({@link FormatoLiga}), ainda sem clubes.
 *
 * @param prioridade 1 = liga principal; dada pelo editor, sem repetir nem pular
 * @param quantidadeClubes vagas da divisão
 * @param limiteForca teto de força dos jogadores na divisão (a principal é sempre 50)
 * @param rebaixados quantos descem; os promovidos vêm da divisão de cima
 */
public record FormatoDivisao(String nome, int prioridade, int quantidadeClubes, int limiteForca, int rebaixados) {
}
