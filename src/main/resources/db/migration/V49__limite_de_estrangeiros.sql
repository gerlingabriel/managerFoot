-- Limite de estrangeiros em campo na liga e na copa nacional (SPEC §8.4, rev. 130). O número é do país no banco
-- (molde) e o jogo guarda a cópia do momento em que foi criado. Os jogos e moldes que já existem ficam com o padrão, 5.
ALTER TABLE molde_pais
    ADD COLUMN limite_estrangeiros integer NOT NULL DEFAULT 5
        CONSTRAINT ck_molde_pais_limite_estrangeiros CHECK (limite_estrangeiros BETWEEN 0 AND 11);

ALTER TABLE jogo
    ADD COLUMN limite_estrangeiros integer NOT NULL DEFAULT 5
        CONSTRAINT ck_jogo_limite_estrangeiros CHECK (limite_estrangeiros BETWEEN 0 AND 11);
