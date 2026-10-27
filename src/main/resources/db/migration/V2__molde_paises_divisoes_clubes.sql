-- Molde editável (docs/decisions/0006): países, liga nacional, divisões e clubes.
-- Cada jogo começará de uma cópia do molde (tabelas jogo_*, em migrações futuras).
-- Regras que dependem do conjunto (15 a 20 clubes, promovidos = rebaixados da divisão de cima,
-- clubes suficientes) ficam no ValidadorLiga: o editor pode guardar uma liga ainda incompleta.

CREATE TABLE molde (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome      TEXT        NOT NULL CHECK (btrim(nome) <> ''),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Um país tem uma liga nacional; o nome da liga fica no próprio país.
-- Código de 3 letras maiúsculas (provisório: padrão ISO 3166-1 alfa-3, ex.: BRA, ARG).
CREATE TABLE molde_pais (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    molde_id  BIGINT NOT NULL REFERENCES molde (id) ON DELETE CASCADE,
    codigo    TEXT   NOT NULL CHECK (codigo ~ '^[A-Z]{3}$'),
    nome      TEXT   NOT NULL CHECK (btrim(nome) <> ''),
    nome_liga TEXT CHECK (btrim(nome_liga) <> ''),
    CONSTRAINT uq_molde_pais_codigo UNIQUE (molde_id, codigo)
);

-- SPEC §4.2: prioridade 1 = liga principal; força de 1 a 50.
CREATE TABLE molde_divisao (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pais_id      BIGINT NOT NULL REFERENCES molde_pais (id) ON DELETE CASCADE,
    nome         TEXT   NOT NULL CHECK (btrim(nome) <> ''),
    prioridade   INT    NOT NULL CHECK (prioridade >= 1),
    limite_forca INT    NOT NULL CHECK (limite_forca BETWEEN 1 AND 50),
    promovidos   INT    NOT NULL CHECK (promovidos >= 0),
    rebaixados   INT    NOT NULL CHECK (rebaixados >= 0),
    CONSTRAINT uq_molde_divisao_prioridade UNIQUE (pais_id, prioridade),
    -- alvo da chave composta de molde_clube: garante divisão do mesmo país do clube
    CONSTRAINT uq_molde_divisao_pais UNIQUE (pais_id, id)
);

-- Clube do país sem divisão (divisao_id nulo) está na fila de espera da liga (SPEC §4.1).
-- Escudo: modelo de ModeloEscudo e duas cores #RRGGBB diferentes (SPEC §10.2).
CREATE TABLE molde_clube (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pais_id         BIGINT NOT NULL REFERENCES molde_pais (id) ON DELETE CASCADE,
    divisao_id      BIGINT,
    nome            TEXT   NOT NULL CHECK (btrim(nome) <> ''),
    forca           INT    NOT NULL CHECK (forca BETWEEN 1 AND 50),
    escudo_modelo   TEXT   NOT NULL CHECK (escudo_modelo IN (
        'METADE_VERTICAL', 'METADE_HORIZONTAL', 'LISTRAS_VERTICAIS', 'LISTRAS_HORIZONTAIS',
        'CORTE_DIAGONAL', 'FAIXA_DIAGONAL', 'FAIXA_HORIZONTAL', 'QUARTOS', 'CRUZ', 'LISO_COM_BORDA')),
    escudo_cor_1    TEXT   NOT NULL CHECK (escudo_cor_1 ~ '^#[0-9A-F]{6}$'),
    escudo_cor_2    TEXT   NOT NULL CHECK (escudo_cor_2 ~ '^#[0-9A-F]{6}$'),
    CONSTRAINT ck_molde_clube_cores_diferentes CHECK (escudo_cor_1 <> escudo_cor_2),
    CONSTRAINT fk_molde_clube_divisao_mesmo_pais
        FOREIGN KEY (pais_id, divisao_id) REFERENCES molde_divisao (pais_id, id)
);

CREATE INDEX ix_molde_clube_pais ON molde_clube (pais_id);
CREATE INDEX ix_molde_clube_divisao ON molde_clube (divisao_id);
