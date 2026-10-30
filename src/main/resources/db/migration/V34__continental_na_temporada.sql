-- Competição continental na temporada (SPEC §8.3.6, rev. 72 a 75): os grupos sorteados no início da
-- temporada e as partidas deles no mesmo calendário da liga e da copa; o mata-mata reusa os
-- confrontos da copa, marcados pela competição. Temporadas gravadas antes não têm continental.

-- Confronto de mata-mata da copa nacional ou da continental.
ALTER TABLE jogo_copa_confronto
    ADD COLUMN competicao TEXT NOT NULL DEFAULT 'COPA' CHECK (competicao IN ('COPA', 'CONTINENTAL')),
    DROP CONSTRAINT uq_jogo_copa_confronto,
    ADD CONSTRAINT uq_jogo_copa_confronto UNIQUE (temporada_id, competicao, fase, ordem);

-- Clube de um grupo; pote 0 é o dos mais fortes e dá a ordem do clube no grupo.
CREATE TABLE jogo_continental_grupo (
    temporada_id BIGINT NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    clube_id     BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    grupo        INT    NOT NULL CHECK (grupo >= 0),
    pote         INT    NOT NULL CHECK (pote >= 0),
    PRIMARY KEY (temporada_id, clube_id),
    CONSTRAINT uq_jogo_continental_grupo_pote UNIQUE (temporada_id, grupo, pote)
);

-- Partida de grupo: sem divisão e sem confronto; jogo_do_grupo é o 1º a 6º jogo do grupo, e a rodada é
-- a da liga depois da qual ele é jogado. A liga passa a ser reconhecida pela divisão.
ALTER TABLE jogo_partida
    ADD COLUMN grupo         INT CHECK (grupo >= 0),
    ADD COLUMN jogo_do_grupo INT CHECK (jogo_do_grupo >= 1),
    DROP CONSTRAINT ck_jogo_partida_competicao,
    ADD CONSTRAINT ck_jogo_partida_competicao CHECK (num_nonnulls(divisao_id, confronto_id, grupo) = 1),
    ADD CONSTRAINT ck_jogo_partida_grupo CHECK ((grupo IS NULL) = (jogo_do_grupo IS NULL));
DROP INDEX uq_jogo_partida_liga;
CREATE UNIQUE INDEX uq_jogo_partida_liga ON jogo_partida (temporada_id, mandante_id, visitante_id)
    WHERE divisao_id IS NOT NULL;
CREATE UNIQUE INDEX uq_jogo_partida_grupo ON jogo_partida (temporada_id, grupo, mandante_id, visitante_id)
    WHERE grupo IS NOT NULL;

-- Etapas da continental já jogadas, como etapas_copa_processadas: só avança uma por vez.
ALTER TABLE jogo_temporada
    ADD COLUMN etapas_continental_processadas INT NOT NULL DEFAULT 0 CHECK (etapas_continental_processadas >= 0);

-- Lançamentos da continental no extrato, separados dos da liga e da copa.
ALTER TABLE jogo_lancamento
    DROP CONSTRAINT jogo_lancamento_competicao_check,
    ADD CONSTRAINT ck_jogo_lancamento_competicao CHECK (competicao IN ('LIGA', 'COPA', 'CONTINENTAL'));
