-- Jogo em rede (SPEC §3.3, rev. 90 e 92; docs/decisions/0020). O jogo é criado como um jogo
-- individual do administrador e fica na sala até ele começar a temporada (comecou_em).
--   modo: SORTEIO, cada um recebe um clube sorteado ao entrar; ESCOLHA, cada um escolhe.
CREATE TABLE jogo_rede (
    jogo_id    BIGINT PRIMARY KEY REFERENCES jogo (id) ON DELETE CASCADE,
    modo       TEXT NOT NULL CHECK (modo IN ('SORTEIO', 'ESCOLHA')),
    comecou_em TIMESTAMPTZ
);

-- Links de convite. O token fica guardado como foi gerado porque o administrador volta a vê-lo na
-- sala para copiar. Gerar um link novo marca o anterior como trocado: um só vale por jogo.
CREATE TABLE jogo_rede_convite (
    token       TEXT PRIMARY KEY CHECK (length(token) BETWEEN 20 AND 100),
    jogo_id     BIGINT      NOT NULL REFERENCES jogo_rede (jogo_id) ON DELETE CASCADE,
    criado_em   TIMESTAMPTZ NOT NULL,
    vale_ate    TIMESTAMPTZ NOT NULL,
    trocado_em  TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_jogo_rede_convite_atual ON jogo_rede_convite (jogo_id) WHERE trocado_em IS NULL;

-- Quem está na sala, com ou sem clube (o clube fica em jogo_treinador). visto_em é a última
-- consulta do navegador: sem consulta por 30 s, o participante está sem conexão.
CREATE TABLE jogo_rede_participante (
    jogo_id    BIGINT      NOT NULL REFERENCES jogo_rede (jogo_id) ON DELETE CASCADE,
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id),
    entrou_em  TIMESTAMPTZ NOT NULL,
    visto_em   TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (jogo_id, usuario_id)
);

CREATE INDEX ix_jogo_rede_participante_usuario ON jogo_rede_participante (usuario_id);
