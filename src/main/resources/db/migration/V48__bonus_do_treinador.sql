-- Bônus do treinador usuário (SPEC §5.3.2, rev. 121): pela força em campo do clube na hora do sorteio, alguns
-- jogadores ganham força extra em campo e o asterisco temporário até o fim da temporada.

-- Clubes que já sortearam na temporada: o sorteio é um só por clube e temporada (o clube que o usuário deixou mantém
-- os sorteados; o clube novo sorteia).
CREATE TABLE jogo_bonus_treinador_sorteio (
    temporada_id BIGINT NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    clube_id     BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    PRIMARY KEY (temporada_id, clube_id)
);

-- Sorteados: extra = já tinha asterisco e ganha o bônus extra de finalização em vez do asterisco temporário.
CREATE TABLE jogo_bonus_treinador (
    temporada_id BIGINT  NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    jogador_id   BIGINT  NOT NULL REFERENCES jogo_jogador (id) ON DELETE CASCADE,
    clube_id     BIGINT  NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    forca_extra  INT     NOT NULL CHECK (forca_extra > 0),
    extra        BOOLEAN NOT NULL,
    PRIMARY KEY (temporada_id, jogador_id),
    FOREIGN KEY (temporada_id, clube_id) REFERENCES jogo_bonus_treinador_sorteio (temporada_id, clube_id) ON DELETE CASCADE
);
