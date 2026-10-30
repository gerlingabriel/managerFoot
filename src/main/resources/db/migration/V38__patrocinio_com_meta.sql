-- Propostas de patrocínio com meta (SPEC §9.6, modelo 14b, rev. 85). A meta depende da força do clube
-- e fica gravada ao assinar. Contratos por vitória e por acesso já assinados seguem valendo até o fim
-- da temporada deles.
ALTER TABLE jogo_patrocinio
    DROP CONSTRAINT jogo_patrocinio_escolha_check,
    ADD CONSTRAINT jogo_patrocinio_escolha_check
        CHECK (escolha IN ('FIXA', 'POR_VITORIA', 'SE_SUBIR', 'META_VITORIAS', 'META_POSICAO')),
    ADD COLUMN meta_vitorias INT CHECK (meta_vitorias > 0),
    ADD COLUMN meta_posicao  INT CHECK (meta_posicao > 0);
