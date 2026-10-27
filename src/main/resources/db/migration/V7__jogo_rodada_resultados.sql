-- Avanço de rodada (SPEC §15, 3c.3; rev. 22): placares e gols no calendário, cartões,
-- suspensões e moral. A classificação é recalculada dos placares, sem tabela própria.

-- Rodadas já jogadas; o avanço só grava a rodada seguinte (processamento único).
ALTER TABLE jogo_temporada
    ADD COLUMN rodadas_processadas INT NOT NULL DEFAULT 0 CHECK (rodadas_processadas >= 0);

-- Placar nulo = partida ainda não jogada; semente reproduz a partida.
ALTER TABLE jogo_partida
    ADD COLUMN gols_mandante  INT CHECK (gols_mandante >= 0),
    ADD COLUMN gols_visitante INT CHECK (gols_visitante >= 0),
    ADD COLUMN semente        BIGINT,
    ADD CONSTRAINT ck_jogo_partida_placar_completo CHECK (
        (gols_mandante IS NULL AND gols_visitante IS NULL AND semente IS NULL)
        OR (gols_mandante IS NOT NULL AND gols_visitante IS NOT NULL AND semente IS NOT NULL));

-- Eventos públicos da partida: gols e cartões (SPEC §7.4: histórico de cartões do jogador).
-- clube_id é o clube do jogador; os gols da temporada e da carreira saem desta tabela.
CREATE TABLE jogo_evento (
    id         BIGINT  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    partida_id BIGINT  NOT NULL REFERENCES jogo_partida (id) ON DELETE CASCADE,
    minuto     INT     NOT NULL CHECK (minuto >= 0),
    clube_id   BIGINT  NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    jogador_id BIGINT  NOT NULL REFERENCES jogo_jogador (id) ON DELETE CASCADE,
    tipo       TEXT    NOT NULL CHECK (tipo IN ('GOL', 'CARTAO_AMARELO', 'SEGUNDO_AMARELO', 'CARTAO_VERMELHO')),
    penalti    BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX ix_jogo_evento_partida ON jogo_evento (partida_id);
CREATE INDEX ix_jogo_evento_jogador ON jogo_evento (jogador_id);

-- Disciplina (SPEC §7.4): amarelos acumulados na temporada e jogos de suspensão a cumprir.
ALTER TABLE jogo_jogador
    ADD COLUMN amarelos  INT NOT NULL DEFAULT 0 CHECK (amarelos >= 0),
    ADD COLUMN suspensao INT NOT NULL DEFAULT 0 CHECK (suspensao >= 0);

-- Moral do clube (SPEC §6.2); nulo = moral inicial dos parâmetros, ainda sem jogo.
ALTER TABLE jogo_clube
    ADD COLUMN moral INT;
