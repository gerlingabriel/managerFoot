-- Histórico em quadros (SPEC §8.1.1, rev. 77): a liga mostra campeão, vice e 3º; a continental, campeão
-- e vice. Temporadas encerradas antes desta migração ficam sem vice e 3º da liga.
ALTER TABLE jogo_campeao
    ADD COLUMN vice_id       BIGINT REFERENCES jogo_clube (id) ON DELETE CASCADE,
    ADD COLUMN vice_nome     TEXT,
    ADD COLUMN terceiro_id   BIGINT REFERENCES jogo_clube (id) ON DELETE CASCADE,
    ADD COLUMN terceiro_nome TEXT;

-- Campeão e vice da continental, gravados ao fim da final (§8.3.6). O país vem preenchido no clube de
-- fora, que não tem treinador no jogo; só os treinadores nacionais entram no quadro de títulos.
CREATE TABLE jogo_campeao_continental (
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
    CONSTRAINT uq_jogo_campeao_continental UNIQUE (jogo_id, temporada)
);
