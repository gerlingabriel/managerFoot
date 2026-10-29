-- Copa nacional (SPEC §8.2, rev. 69 e 70): confrontos da chave sorteada no início da temporada e as
-- partidas deles no mesmo calendário da liga, para gols, cartões e lesões contarem em todas as
-- competições. Temporadas gravadas antes desta migração não têm copa.

-- Um confronto da fase; mandante joga a ida em casa (na final, campo neutro). Os vencedores das
-- ordens 2k e 2k+1 se enfrentam na fase seguinte.
CREATE TABLE jogo_copa_confronto (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    temporada_id BIGINT NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    fase         TEXT   NOT NULL CHECK (fase IN ('DEZESSEIS_AVOS', 'OITAVAS', 'QUARTAS', 'SEMIFINAL', 'FINAL')),
    ordem        INT    NOT NULL CHECK (ordem >= 0),
    mandante_id  BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    visitante_id BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    CONSTRAINT ck_jogo_copa_confronto_clubes CHECK (mandante_id <> visitante_id),
    CONSTRAINT uq_jogo_copa_confronto UNIQUE (temporada_id, fase, ordem)
);

-- Partida da liga: divisão e sem confronto. Partida da copa: confronto e perna, sem divisão; a rodada
-- é a da liga depois da qual ela é jogada. O placar inclui a prorrogação; os pênaltis ficam à parte.
ALTER TABLE jogo_partida
    DROP CONSTRAINT uq_jogo_partida_confronto,
    ALTER COLUMN divisao_id DROP NOT NULL,
    ADD COLUMN confronto_id       BIGINT REFERENCES jogo_copa_confronto (id) ON DELETE CASCADE,
    ADD COLUMN perna              TEXT CHECK (perna IN ('IDA', 'VOLTA', 'JOGO_UNICO')),
    ADD COLUMN prorrogacao        BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN penaltis_mandante  INT CHECK (penaltis_mandante >= 0),
    ADD COLUMN penaltis_visitante INT CHECK (penaltis_visitante >= 0),
    ADD CONSTRAINT ck_jogo_partida_competicao CHECK ((divisao_id IS NULL) = (confronto_id IS NOT NULL)),
    ADD CONSTRAINT ck_jogo_partida_perna CHECK ((confronto_id IS NULL) = (perna IS NULL)),
    ADD CONSTRAINT ck_jogo_partida_penaltis CHECK (
        (penaltis_mandante IS NULL AND penaltis_visitante IS NULL)
        OR (penaltis_mandante IS NOT NULL AND penaltis_visitante IS NOT NULL
            AND penaltis_mandante <> penaltis_visitante AND prorrogacao));

-- Na liga, cada par joga uma vez em cada casa; na copa, uma partida por perna do confronto.
CREATE UNIQUE INDEX uq_jogo_partida_liga ON jogo_partida (temporada_id, mandante_id, visitante_id)
    WHERE confronto_id IS NULL;
CREATE UNIQUE INDEX uq_jogo_partida_perna ON jogo_partida (confronto_id, perna) WHERE confronto_id IS NOT NULL;

-- Etapas do quadro da copa já jogadas (Copa.quadro); como rodadas_processadas, só avança uma por vez.
ALTER TABLE jogo_temporada
    ADD COLUMN etapas_copa_processadas INT NOT NULL DEFAULT 0 CHECK (etapas_copa_processadas >= 0);
