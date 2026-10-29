-- Público pela posição e obra do estádio (SPEC §9.2, rev. 56 e 57).

-- Obra em andamento: rodadas que faltam para os lugares novos valerem; nulo sem obra. Uma obra por
-- vez, de um bloco.
-- Posição final da temporada passada na divisão em que o clube jogou, para o público das primeiras
-- rodadas; nula na 1ª temporada e nos jogos salvos antes desta migração (vale a ocupação de início).
-- Quem veio da fila conta como o último da divisão em que entra.
-- O estádio dos jogos salvos não muda (rev. 57): o estádio inicial novo vale só para caixas abertos
-- daqui em diante.
ALTER TABLE jogo_clube
    ADD COLUMN obra_rodadas            INT CHECK (obra_rodadas > 0),
    ADD COLUMN posicao_anterior        INT CHECK (posicao_anterior >= 1),
    ADD COLUMN clubes_divisao_anterior INT,
    ADD CONSTRAINT ck_jogo_clube_posicao_anterior CHECK (
        (posicao_anterior IS NULL) = (clubes_divisao_anterior IS NULL)
        AND (posicao_anterior IS NULL OR posicao_anterior <= clubes_divisao_anterior));

-- Pagamento da ampliação do estádio.
ALTER TABLE jogo_lancamento DROP CONSTRAINT jogo_lancamento_tipo_check;
ALTER TABLE jogo_lancamento ADD CONSTRAINT jogo_lancamento_tipo_check
    CHECK (tipo IN ('BILHETERIA', 'CUSTO_DO_JOGO', 'DESLOCAMENTO', 'SALARIOS', 'PATROCINIO', 'PREMIO', 'COMPRA',
                    'VENDA', 'ESTADIO'));
