-- Leilões da máquina antes da rodada (SPEC §9.4, rev. 41 a 45; modelo 7b; docs/decisions/0012).
-- Abertos quando o treinador confirma a tática da rodada; resolvidos depois dos lances dele.
-- rodada é a que vai ser jogada. A chave única impede leiloar o mesmo jogador duas vezes antes da
-- mesma rodada (sem lance, ele só volta depois que a rodada terminar).
CREATE TABLE jogo_leilao (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    temporada_id BIGINT  NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    rodada       INT     NOT NULL CHECK (rodada >= 1),
    jogador_id   BIGINT  NOT NULL REFERENCES jogo_jogador (id) ON DELETE CASCADE,
    vendedor_id  BIGINT  NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    lance_minimo BIGINT  NOT NULL CHECK (lance_minimo >= 0),
    situacao     TEXT    NOT NULL DEFAULT 'ABERTO'
        CHECK (situacao IN ('ABERTO', 'VENDIDO', 'SEM_LANCE', 'CANCELADO')),
    comprador_id BIGINT REFERENCES jogo_clube (id) ON DELETE CASCADE,
    valor        BIGINT CHECK (valor >= 0),
    empate       BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_jogo_leilao UNIQUE (temporada_id, rodada, jogador_id),
    CONSTRAINT ck_jogo_leilao_venda CHECK ((situacao = 'VENDIDO') = (comprador_id IS NOT NULL AND valor IS NOT NULL))
);

CREATE INDEX ix_jogo_leilao_rodada ON jogo_leilao (temporada_id, rodada);

-- Lances que valeram, dos treinadores e da máquina: um por clube em cada leilão.
CREATE TABLE jogo_leilao_lance (
    leilao_id BIGINT NOT NULL REFERENCES jogo_leilao (id) ON DELETE CASCADE,
    clube_id  BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    valor     BIGINT NOT NULL CHECK (valor >= 0),
    PRIMARY KEY (leilao_id, clube_id)
);
