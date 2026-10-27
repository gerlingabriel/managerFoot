-- Temporada do jogo em andamento e os sorteios do seu início (Temporada.iniciar): calendário,
-- sorte e azar por divisão e bônus de artilharia. Tudo é gravado, e não recalculado pela semente,
-- para que uma mudança de algoritmo não altere jogos já salvos.
-- Resultados e gols entram na próxima migração (avanço de rodada).

-- semente: a dos sorteios e a do desempate por sorteio da classificação (SPEC §8.1, rev. 12).
CREATE TABLE jogo_temporada (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogo_id     BIGINT      NOT NULL REFERENCES jogo (id) ON DELETE CASCADE,
    numero      INT         NOT NULL CHECK (numero >= 1),
    semente     BIGINT      NOT NULL,
    iniciada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- impede iniciar a mesma temporada duas vezes
    CONSTRAINT uq_jogo_temporada_numero UNIQUE (jogo_id, numero)
);

-- Sorte e azar da divisão na temporada (SPEC §5.3.1). Oculto dos participantes.
CREATE TABLE jogo_temporada_divisao (
    temporada_id   BIGINT NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    divisao_id     BIGINT NOT NULL REFERENCES jogo_divisao (id) ON DELETE CASCADE,
    beneficiado_id BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    prejudicado_id BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    PRIMARY KEY (temporada_id, divisao_id),
    CONSTRAINT ck_jogo_temporada_divisao_sorte CHECK (beneficiado_id <> prejudicado_id)
);

-- Calendário da liga (SPEC §8.1): cada par joga uma vez em cada casa. A folga (número ímpar de
-- clubes) é quem da divisão não aparece na rodada.
CREATE TABLE jogo_partida (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    temporada_id BIGINT NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    divisao_id   BIGINT NOT NULL REFERENCES jogo_divisao (id) ON DELETE CASCADE,
    rodada       INT    NOT NULL CHECK (rodada >= 1),
    mandante_id  BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    visitante_id BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    CONSTRAINT ck_jogo_partida_clubes_diferentes CHECK (mandante_id <> visitante_id),
    CONSTRAINT uq_jogo_partida_confronto UNIQUE (temporada_id, mandante_id, visitante_id)
);

CREATE INDEX ix_jogo_partida_rodada ON jogo_partida (temporada_id, rodada);

-- Bônus de artilharia da temporada (SPEC §5.2): extra = já tinha asterisco e ganha o bônus que
-- soma ao do asterisco; senão, ganha o asterisco temporário.
CREATE TABLE jogo_bonus_artilharia (
    temporada_id BIGINT  NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    divisao_id   BIGINT  NOT NULL REFERENCES jogo_divisao (id) ON DELETE CASCADE,
    jogador_id   BIGINT  NOT NULL REFERENCES jogo_jogador (id) ON DELETE CASCADE,
    extra        BOOLEAN NOT NULL,
    PRIMARY KEY (temporada_id, jogador_id)
);
