-- Jogo em andamento (docs/decisions/0006): cópia da liga de um país do molde, que evolui sem
-- alterar o molde. Os outros países só entram com as competições continentais (Etapa 6).
-- Temporada, calendário e resultados entram em migrações seguintes.

-- molde_id só registra a origem: apagar o molde não apaga o jogo.
CREATE TABLE jogo (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    molde_id    BIGINT REFERENCES molde (id) ON DELETE SET NULL,
    pais_codigo TEXT        NOT NULL REFERENCES pais_fifa (codigo),
    nome_liga   TEXT        NOT NULL CHECK (btrim(nome_liga) <> ''),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE jogo_divisao (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogo_id      BIGINT NOT NULL REFERENCES jogo (id) ON DELETE CASCADE,
    nome         TEXT   NOT NULL CHECK (btrim(nome) <> ''),
    prioridade   INT    NOT NULL CHECK (prioridade >= 1),
    limite_forca INT    NOT NULL CHECK (limite_forca BETWEEN 1 AND 50),
    promovidos   INT    NOT NULL CHECK (promovidos >= 0),
    rebaixados   INT    NOT NULL CHECK (rebaixados >= 0),
    CONSTRAINT uq_jogo_divisao_prioridade UNIQUE (jogo_id, prioridade),
    -- alvo da chave composta de jogo_clube: garante divisão do mesmo jogo do clube
    CONSTRAINT uq_jogo_divisao_jogo UNIQUE (jogo_id, id)
);

-- Clube sem divisão (divisao_id nulo) está na fila de espera (SPEC §4.1).
CREATE TABLE jogo_clube (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogo_id         BIGINT NOT NULL REFERENCES jogo (id) ON DELETE CASCADE,
    divisao_id      BIGINT,
    nome            TEXT   NOT NULL CHECK (btrim(nome) <> ''),
    forca           INT    NOT NULL CHECK (forca BETWEEN 1 AND 50),
    escudo_modelo   TEXT   NOT NULL CHECK (escudo_modelo IN (
        'METADE_VERTICAL', 'METADE_HORIZONTAL', 'LISTRAS_VERTICAIS', 'LISTRAS_HORIZONTAIS',
        'CORTE_DIAGONAL', 'FAIXA_DIAGONAL', 'FAIXA_HORIZONTAL', 'QUARTOS', 'CRUZ', 'LISO_COM_BORDA')),
    escudo_cor_1    TEXT   NOT NULL CHECK (escudo_cor_1 ~ '^#[0-9A-F]{6}$'),
    escudo_cor_2    TEXT   NOT NULL CHECK (escudo_cor_2 ~ '^#[0-9A-F]{6}$'),
    CONSTRAINT ck_jogo_clube_cores_diferentes CHECK (escudo_cor_1 <> escudo_cor_2),
    CONSTRAINT fk_jogo_clube_divisao_mesmo_jogo
        FOREIGN KEY (jogo_id, divisao_id) REFERENCES jogo_divisao (jogo_id, id)
);

CREATE INDEX ix_jogo_clube_jogo ON jogo_clube (jogo_id);
CREATE INDEX ix_jogo_clube_divisao ON jogo_clube (divisao_id);

-- asterisco: o permanente; o temporário do bônus de artilharia é da temporada.
CREATE TABLE jogo_jogador (
    id            BIGINT  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clube_id      BIGINT  NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    nome          TEXT    NOT NULL CHECK (btrim(nome) <> ''),
    nacionalidade TEXT    NOT NULL REFERENCES pais_fifa (codigo),
    posicao       TEXT    NOT NULL CHECK (posicao IN ('GOLEIRO', 'DEFENSOR', 'MEIO_CAMPISTA', 'ATACANTE')),
    forca         INT     NOT NULL CHECK (forca BETWEEN 1 AND 50),
    temperamento  TEXT    NOT NULL CHECK (temperamento IN ('CALMO', 'EDUCADO', 'NORMAL', 'ANSIOSO', 'VIOLENTO')),
    asterisco     BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX ix_jogo_jogador_clube ON jogo_jogador (clube_id);
