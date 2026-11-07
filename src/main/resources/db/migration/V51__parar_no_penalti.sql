-- Caixa "Pênaltis a favor" da janela do jogo (SPEC §12, rev. 133): o jogo para no pênalti a favor do clube do
-- treinador para ele escolher quem bate. Começa marcada.
ALTER TABLE jogo_treinador
    ADD COLUMN parar_no_penalti boolean NOT NULL DEFAULT true;
