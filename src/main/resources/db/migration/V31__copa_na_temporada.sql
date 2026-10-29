-- Lançamentos da copa (SPEC §8.2, rev. 70): bilheteria, custo, deslocamento, patrocínio por vitória e
-- prêmio da copa têm a mesma rodada da liga depois da qual a etapa é jogada. A competição entra na
-- chave para os da copa não somarem nem baterem com os da rodada da liga.
ALTER TABLE jogo_lancamento
    ADD COLUMN competicao TEXT NOT NULL DEFAULT 'LIGA' CHECK (competicao IN ('LIGA', 'COPA'));
ALTER TABLE jogo_lancamento DROP CONSTRAINT uq_jogo_lancamento;
ALTER TABLE jogo_lancamento ADD CONSTRAINT uq_jogo_lancamento
    UNIQUE NULLS NOT DISTINCT (clube_id, temporada_id, rodada, tipo, jogador_id, competicao);

-- Campeão e vice da copa de cada temporada (SPEC §8.1.1 e §8.2, rev. 70), gravados ao fim da final,
-- para a coluna Copa da aba Histórico; o título não conta no ranking de treinadores. Nomes copiados
-- como em jogo_campeao; o treinador é o usuário ou, sem usuário, o da máquina.
CREATE TABLE jogo_campeao_copa (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogo_id              BIGINT NOT NULL REFERENCES jogo (id) ON DELETE CASCADE,
    temporada            INT    NOT NULL CHECK (temporada >= 1),
    ano                  INT    NOT NULL,
    clube_id             BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    clube_nome           TEXT   NOT NULL,
    vice_id              BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    vice_nome            TEXT   NOT NULL,
    usuario_id           BIGINT REFERENCES usuario (id),
    treinador_maquina_id BIGINT REFERENCES jogo_treinador_maquina (id) ON DELETE SET NULL,
    CONSTRAINT uq_jogo_campeao_copa UNIQUE (jogo_id, temporada)
);

-- Etapa da copa ao vivo (rev. 70): a mesma tela da rodada; rodada é a da liga depois da qual a etapa
-- é jogada e etapa_copa, o índice dela no quadro. Nulo numa rodada da liga.
ALTER TABLE jogo_rodada_ao_vivo
    ADD COLUMN etapa_copa INT CHECK (etapa_copa >= 0);
