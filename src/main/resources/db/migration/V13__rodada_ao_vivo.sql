-- Rodada ao vivo (docs/decisions/0010; SPEC rev. 35). O relógio anda pelo tempo: o progresso
-- gravado (em minutos de jogo, somando as divisões já exibidas) vale no instante andando_desde;
-- nulo é pausado. As partidas são refeitas pela semente da rodada e pelas substituições.
-- A linha some quando a rodada é gravada.

-- Velocidade escolhida pelo treinador, mantida para as próximas rodadas.
ALTER TABLE jogo_treinador
    ADD COLUMN velocidade TEXT NOT NULL DEFAULT 'NORMAL' CHECK (velocidade IN ('LENTA', 'NORMAL', 'RAPIDA'));

-- Uma rodada ao vivo por jogo: a chave primária impede iniciar duas.
CREATE TABLE jogo_rodada_ao_vivo (
    jogo_id       BIGINT PRIMARY KEY REFERENCES jogo (id) ON DELETE CASCADE,
    temporada_id  BIGINT           NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    rodada        INT              NOT NULL CHECK (rodada >= 1),
    progresso     DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (progresso >= 0),
    andando_desde TIMESTAMPTZ
);

-- Substituições do treinador, na ordem em que foram feitas. mandante_id identifica a partida.
CREATE TABLE jogo_substituicao_ao_vivo (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogo_id     BIGINT NOT NULL REFERENCES jogo_rodada_ao_vivo (jogo_id) ON DELETE CASCADE,
    mandante_id BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    minuto      INT    NOT NULL CHECK (minuto >= 0),
    lado        TEXT   NOT NULL CHECK (lado IN ('CASA', 'FORA')),
    sai_id      BIGINT NOT NULL REFERENCES jogo_jogador (id) ON DELETE CASCADE,
    entra_id    BIGINT NOT NULL REFERENCES jogo_jogador (id) ON DELETE CASCADE
);

CREATE INDEX ix_jogo_substituicao_ao_vivo_jogo ON jogo_substituicao_ao_vivo (jogo_id, id);
