-- Trocas de treinador pela moral (SPEC §5.4, rev. 59 a 62): quem é demitido chama o treinador de um
-- clube inferior de moral verde. Cada linha é uma demissão resolvida ou em andamento:
--   TROCA     os dois clubes trocaram de treinador (outro_clube_id = quem cedeu);
--   CONVITE   o treinador de outro_clube_id é usuário e ainda não respondeu (modelo 11a); a rodada espera;
--   RECUSADO  o usuário recusou; a vez passou ao seguinte, numa linha nova;
--   CONTINUA  o usuário demitido continua porque ninguém pôde vir (modelo 11b).
-- demitido_id: o usuário demitido, que vê o aviso (11b) até aviso_visto.
CREATE TABLE jogo_troca_treinador (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogo_id        BIGINT  NOT NULL REFERENCES jogo (id) ON DELETE CASCADE,
    temporada_id   BIGINT  NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    rodada         INT     NOT NULL CHECK (rodada >= 1),
    clube_id       BIGINT  NOT NULL,
    outro_clube_id BIGINT,
    convidado_id   BIGINT REFERENCES usuario (id),
    demitido_id    BIGINT REFERENCES usuario (id),
    situacao       TEXT    NOT NULL CHECK (situacao IN ('TROCA', 'CONVITE', 'RECUSADO', 'CONTINUA')),
    aviso_visto    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_jogo_troca_clube FOREIGN KEY (jogo_id, clube_id) REFERENCES jogo_clube (jogo_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_jogo_troca_outro FOREIGN KEY (jogo_id, outro_clube_id) REFERENCES jogo_clube (jogo_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_jogo_troca_outro CHECK ((situacao = 'CONTINUA') = (outro_clube_id IS NULL)),
    CONSTRAINT ck_jogo_troca_convite CHECK (situacao NOT IN ('CONVITE', 'RECUSADO') OR convidado_id IS NOT NULL)
);

-- no máximo um convite aberto por jogo (no jogo individual há um usuário; a rodada espera a resposta)
CREATE UNIQUE INDEX uq_jogo_troca_convite_aberto ON jogo_troca_treinador (jogo_id) WHERE situacao = 'CONVITE';
CREATE INDEX ix_jogo_troca_rodada ON jogo_troca_treinador (temporada_id, rodada);
