-- Caixa da rodada (SPEC §9.1 e §9.2, rev. 37). Dinheiro em centavos de Real.

-- Capacidade e saldo nulos = caixa ainda não aberto: o avanço da rodada abre com a capacidade
-- inicial e o saldo inicial calculados pelo domínio (economia.CaixaDaRodada), também nos jogos
-- salvos antes desta migração. O saldo pode ficar negativo (SPEC §9.3).
ALTER TABLE jogo_clube
    ADD COLUMN estadio_capacidade INT CHECK (estadio_capacidade > 0),
    ADD COLUMN saldo              BIGINT,
    ADD CONSTRAINT ck_jogo_clube_caixa_aberto CHECK ((estadio_capacidade IS NULL) = (saldo IS NULL));

-- Pagantes do jogo; nulo nas partidas jogadas antes desta migração.
ALTER TABLE jogo_partida
    ADD COLUMN publico INT CHECK (publico >= 0);

-- Extrato: um lançamento de cada tipo por clube e rodada, para a rodada não ser lançada duas vezes.
CREATE TABLE jogo_lancamento (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clube_id     BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    temporada_id BIGINT NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    rodada       INT    NOT NULL CHECK (rodada >= 1),
    tipo         TEXT   NOT NULL CHECK (tipo IN ('BILHETERIA', 'CUSTO_DO_JOGO', 'DESLOCAMENTO', 'SALARIOS')),
    valor        BIGINT NOT NULL,
    CONSTRAINT uq_jogo_lancamento UNIQUE (clube_id, temporada_id, rodada, tipo)
);

CREATE INDEX ix_jogo_lancamento_temporada ON jogo_lancamento (temporada_id);
