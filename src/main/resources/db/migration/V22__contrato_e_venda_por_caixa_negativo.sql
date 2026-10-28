-- Caixa negativo do treinador (SPEC §9.3 e §9.4, rev. 53): um jogador sem contrato renovado vai a
-- leilão sem lance mínimo. contrato_temporada é o número da temporada em que o contrato foi renovado
-- (por enquanto, só na compra); o contrato vale até o fim dela, e no início da seguinte o jogador
-- fica sem contrato. Nulo: nunca renovado.
ALTER TABLE jogo_jogador ADD COLUMN contrato_temporada INT CHECK (contrato_temporada >= 1);

-- O leilão do início da temporada, antes do patrocínio, fica na rodada 0, como o patrocínio.
ALTER TABLE jogo_leilao DROP CONSTRAINT jogo_leilao_rodada_check;
ALTER TABLE jogo_leilao ADD CONSTRAINT jogo_leilao_rodada_check CHECK (rodada >= 0);
