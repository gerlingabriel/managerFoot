-- Contas (SPEC §3.4, rev. 24; docs/decisions/0007): usuários com papel e links de cadastro.

-- E-mail guardado em minúsculas pelo serviço; o índice único garante uma conta por e-mail.
-- senha_hash: BCrypt; a senha em texto nunca é gravada.
CREATE TABLE usuario (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email      TEXT        NOT NULL CHECK (email = lower(email) AND email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'
                                           AND length(email) <= 254),
    nome       TEXT        NOT NULL CHECK (btrim(nome) <> '' AND length(nome) <= 60),
    senha_hash TEXT        NOT NULL,
    papel      TEXT        NOT NULL CHECK (papel IN ('ADMINISTRADOR', 'PARTICIPANTE')),
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuario_email UNIQUE (email)
);

-- Link de cadastro de uso único: só o hash SHA-256 (hex) do token é guardado.
-- usado_por/usado_em ficam nulos até o cadastro; os dois são preenchidos juntos.
CREATE TABLE link_cadastro (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_hash TEXT        NOT NULL CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    criado_por BIGINT      NOT NULL REFERENCES usuario (id),
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_em  TIMESTAMPTZ NOT NULL,
    usado_por  BIGINT REFERENCES usuario (id),
    usado_em   TIMESTAMPTZ,
    CONSTRAINT uq_link_cadastro_token UNIQUE (token_hash),
    CONSTRAINT ck_link_cadastro_uso CHECK ((usado_por IS NULL) = (usado_em IS NULL)),
    CONSTRAINT ck_link_cadastro_validade CHECK (expira_em > criado_em)
);
