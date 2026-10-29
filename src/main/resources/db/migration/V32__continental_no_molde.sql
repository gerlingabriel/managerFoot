-- Competição continental no molde (SPEC §8.3.2, §8.3.6, rev. 72 e 73): o nome e as vagas por país de
-- cada região. Os países sem liga (molde_pais.nome_liga nulo) que têm vaga entram só com clubes,
-- gerados pelo banco padrão; suas ligas não são jogadas.
CREATE TABLE molde_continental (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    molde_id BIGINT NOT NULL REFERENCES molde (id) ON DELETE CASCADE,
    regiao   TEXT   NOT NULL CHECK (regiao IN ('AMERICA_DO_SUL', 'EUROPA')),
    nome     TEXT   NOT NULL CHECK (btrim(nome) <> ''),
    CONSTRAINT uq_molde_continental_regiao UNIQUE (molde_id, regiao)
);

-- Vagas de um país; um país com vagas e sem clubes deixa as vagas vazias (§8.3.2 itens 3 e 4).
-- ordem: a da tabela de vagas, para a tela mostrar como a SPEC.
CREATE TABLE molde_continental_vaga (
    continental_id BIGINT NOT NULL REFERENCES molde_continental (id) ON DELETE CASCADE,
    pais_codigo    TEXT   NOT NULL REFERENCES pais_fifa (codigo),
    vagas          INT    NOT NULL CHECK (vagas >= 1),
    ordem          INT    NOT NULL CHECK (ordem >= 0),
    PRIMARY KEY (continental_id, pais_codigo),
    CONSTRAINT uq_molde_continental_vaga_ordem UNIQUE (continental_id, ordem)
);
