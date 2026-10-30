-- Mundial de Clubes na temporada (SPEC §8.3.4, rev. 88): os jogos reusam os confrontos da copa, marcados pela
-- competição, com partida de perna JOGO_UNICO. A rodada deles vem depois da última da liga. Temporadas
-- gravadas antes não têm Mundial.
ALTER TABLE jogo_copa_confronto
    DROP CONSTRAINT jogo_copa_confronto_competicao_check,
    ADD CONSTRAINT ck_jogo_copa_confronto_competicao CHECK (competicao IN ('COPA', 'CONTINENTAL', 'MUNDIAL'));

-- Etapas já jogadas, como etapas_continental_processadas: só avança uma por vez.
ALTER TABLE jogo_temporada
    ADD COLUMN etapas_mundial_processadas INT NOT NULL DEFAULT 0 CHECK (etapas_mundial_processadas >= 0);

ALTER TABLE jogo_lancamento
    DROP CONSTRAINT ck_jogo_lancamento_competicao,
    ADD CONSTRAINT ck_jogo_lancamento_competicao CHECK (competicao IN ('LIGA', 'COPA', 'CONTINENTAL', 'MUNDIAL'));

ALTER TABLE jogo_rodada_ao_vivo
    ADD COLUMN etapa_mundial INT CHECK (etapa_mundial >= 0),
    DROP CONSTRAINT ck_jogo_rodada_ao_vivo_etapa,
    ADD CONSTRAINT ck_jogo_rodada_ao_vivo_etapa CHECK (num_nonnulls(etapa_copa, etapa_continental, etapa_mundial) <= 1);

-- Campeão e vice, como jogo_campeao_continental.
CREATE TABLE jogo_campeao_mundial (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogo_id              BIGINT NOT NULL REFERENCES jogo (id) ON DELETE CASCADE,
    temporada            INT    NOT NULL CHECK (temporada >= 1),
    ano                  INT    NOT NULL,
    clube_id             BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    clube_nome           TEXT   NOT NULL,
    clube_pais           TEXT REFERENCES pais_fifa (codigo),
    vice_id              BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    vice_nome            TEXT   NOT NULL,
    vice_pais            TEXT REFERENCES pais_fifa (codigo),
    usuario_id           BIGINT REFERENCES usuario (id),
    treinador_maquina_id BIGINT REFERENCES jogo_treinador_maquina (id) ON DELETE SET NULL,
    vice_treinador       TEXT,
    vice_usuario_id      BIGINT REFERENCES usuario (id),
    CONSTRAINT uq_jogo_campeao_mundial UNIQUE (jogo_id, temporada)
);
