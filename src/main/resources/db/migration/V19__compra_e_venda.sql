-- Compra direta (SPEC §9.4, rev. 49): o comprador lança COMPRA e o vendedor VENDA, com o jogador.
-- A rodada é a última processada da temporada (0 antes da primeira). A chave única passa a incluir
-- o jogador, para várias compras na mesma rodada, sem repetir a mesma.
ALTER TABLE jogo_lancamento DROP CONSTRAINT jogo_lancamento_tipo_check;
ALTER TABLE jogo_lancamento ADD CONSTRAINT jogo_lancamento_tipo_check
    CHECK (tipo IN ('BILHETERIA', 'CUSTO_DO_JOGO', 'DESLOCAMENTO', 'SALARIOS', 'PATROCINIO', 'PREMIO', 'COMPRA',
                    'VENDA'));
ALTER TABLE jogo_lancamento
    ADD COLUMN jogador_id BIGINT REFERENCES jogo_jogador (id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_jogo_lancamento_jogador CHECK ((tipo IN ('COMPRA', 'VENDA')) = (jogador_id IS NOT NULL));
ALTER TABLE jogo_lancamento DROP CONSTRAINT uq_jogo_lancamento;
ALTER TABLE jogo_lancamento ADD CONSTRAINT uq_jogo_lancamento
    UNIQUE NULLS NOT DISTINCT (clube_id, temporada_id, rodada, tipo, jogador_id);
