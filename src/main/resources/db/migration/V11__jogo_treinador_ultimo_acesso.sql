-- Último acesso do treinador ao jogo (SPEC §10.5, rev. 29): ordena e informa a lista "Voltar a
-- um jogo gravado". Por treinador, porque em rede cada um tem o seu.
ALTER TABLE jogo_treinador
    ADD COLUMN ultimo_acesso_em TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE INDEX ix_jogo_treinador_usuario ON jogo_treinador (usuario_id, ultimo_acesso_em DESC);
