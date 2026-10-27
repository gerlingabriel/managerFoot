-- Jogadores do molde (SPEC §5.2), no plantel de um clube do molde.
-- O máximo de 25 por clube depende do conjunto e fica no MoldeRepositorio.
-- Gols e asterisco temporário do bônus de artilharia são do jogo em andamento (tabelas jogo_*).

-- Nacionalidade: código de 3 letras como molde_pais.codigo, sem chave estrangeira, porque o
-- jogador pode ser de um país sem liga no molde (ex.: POR). Quem é estrangeiro: SPEC §8.4.
-- asterisco: só o permanente (SPEC §5.3).
CREATE TABLE molde_jogador (
    id            BIGINT  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clube_id      BIGINT  NOT NULL REFERENCES molde_clube (id) ON DELETE CASCADE,
    nome          TEXT    NOT NULL CHECK (btrim(nome) <> ''),
    nacionalidade TEXT    NOT NULL CHECK (nacionalidade ~ '^[A-Z]{3}$'),
    posicao       TEXT    NOT NULL CHECK (posicao IN ('GOLEIRO', 'DEFENSOR', 'MEIO_CAMPISTA', 'ATACANTE')),
    forca         INT     NOT NULL CHECK (forca BETWEEN 1 AND 50),
    temperamento  TEXT    NOT NULL CHECK (temperamento IN ('CALMO', 'EDUCADO', 'NORMAL', 'ANSIOSO', 'VIOLENTO')),
    asterisco     BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX ix_molde_jogador_clube ON molde_jogador (clube_id);
