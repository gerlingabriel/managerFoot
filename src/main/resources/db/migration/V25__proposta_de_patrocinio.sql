-- Propostas de patrocínio do clube do usuário (SPEC §9.6, modelo 9a, rev. 55 e 57). No início de cada
-- temporada o clube de um usuário recebe a oferta com o valor da tabela; a escolha paga a parte à vista
-- (lançamento PATROCINIO na rodada 0), e as propostas por vitória e por acesso pagam o resto depois
-- (PATROCINIO na rodada da vitória ou na última). Sem escolha, a rodada 1 não começa. Os clubes da
-- máquina recebem a proposta fixa sem oferta. Jogos salvos antes desta migração já receberam a fixa.
CREATE TABLE jogo_patrocinio (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clube_id     BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    temporada_id BIGINT NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    tabela       BIGINT NOT NULL CHECK (tabela > 0),
    jogos        INT    NOT NULL CHECK (jogos > 0),
    escolha      TEXT CHECK (escolha IN ('FIXA', 'POR_VITORIA', 'SE_SUBIR')),
    CONSTRAINT uq_jogo_patrocinio UNIQUE (clube_id, temporada_id)
);

CREATE INDEX ix_jogo_patrocinio_temporada ON jogo_patrocinio (temporada_id);
