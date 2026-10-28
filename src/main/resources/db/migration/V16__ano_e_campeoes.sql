-- Ano da temporada e histórico de campeões (SPEC §8.1.1, rev. 40).

-- A temporada N do jogo é o ano ano_inicial + N - 1. Jogos salvos antes desta migração começam em 2026.
ALTER TABLE jogo
    ADD COLUMN ano_inicial INT NOT NULL DEFAULT 2026 CHECK (ano_inicial BETWEEN 1850 AND 9999);

-- Campeão de cada divisão em cada temporada, gravado no fim da temporada. Nome do clube e da divisão
-- copiados para o histórico não mudar se a liga mudar depois. O treinador é o usuário que dirigia o
-- clube no fim da temporada; nulo para clube da máquina.
CREATE TABLE jogo_campeao (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogo_id            BIGINT NOT NULL REFERENCES jogo (id) ON DELETE CASCADE,
    temporada          INT    NOT NULL CHECK (temporada >= 1),
    ano                INT    NOT NULL,
    divisao_prioridade INT    NOT NULL CHECK (divisao_prioridade >= 1),
    divisao_nome       TEXT   NOT NULL,
    clube_id           BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    clube_nome         TEXT   NOT NULL,
    usuario_id         BIGINT REFERENCES usuario (id),
    CONSTRAINT uq_jogo_campeao UNIQUE (jogo_id, temporada, divisao_prioridade)
);

CREATE INDEX ix_jogo_campeao_usuario ON jogo_campeao (usuario_id) WHERE usuario_id IS NOT NULL;
