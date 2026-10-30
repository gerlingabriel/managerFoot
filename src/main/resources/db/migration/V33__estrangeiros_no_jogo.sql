-- Competição continental no jogo (SPEC §8.3.6, rev. 72 a 74): na criação, o jogo copia do molde a
-- competição da região do país jogado e os clubes estrangeiros que podem ocupar as vagas. Jogos criados
-- antes desta migração ficam sem continental.

-- País do clube estrangeiro; nulo = clube do país jogado. O estrangeiro não tem divisão nem caixa, não
-- evolui e não entra no mercado: só joga a continental.
ALTER TABLE jogo_clube
    ADD COLUMN pais_codigo TEXT REFERENCES pais_fifa (codigo),
    ADD CONSTRAINT ck_jogo_clube_estrangeiro_sem_divisao CHECK (pais_codigo IS NULL OR divisao_id IS NULL);
CREATE INDEX ix_jogo_clube_estrangeiro ON jogo_clube (jogo_id, pais_codigo) WHERE pais_codigo IS NOT NULL;

-- Cópia do nome e das vagas: o jogo não depende do molde, que pode ser apagado ou editado.
CREATE TABLE jogo_continental (
    jogo_id BIGINT PRIMARY KEY REFERENCES jogo (id) ON DELETE CASCADE,
    regiao  TEXT   NOT NULL CHECK (regiao IN ('AMERICA_DO_SUL', 'EUROPA')),
    nome    TEXT   NOT NULL CHECK (btrim(nome) <> '')
);

CREATE TABLE jogo_continental_vaga (
    jogo_id     BIGINT NOT NULL REFERENCES jogo_continental (jogo_id) ON DELETE CASCADE,
    pais_codigo TEXT   NOT NULL REFERENCES pais_fifa (codigo),
    vagas       INT    NOT NULL CHECK (vagas >= 1),
    ordem       INT    NOT NULL CHECK (ordem >= 0),
    PRIMARY KEY (jogo_id, pais_codigo),
    CONSTRAINT uq_jogo_continental_vaga_ordem UNIQUE (jogo_id, ordem)
);
