-- Sorte da temporada (SPEC §5.3.1, rev. 109): o prejudicado volta, sorteado só entre os clubes da máquina. A tabela
-- da V46 guarda os dois: prejudicado = false é beneficiado. O nome fica, para os saves exportados desde a V46 abrirem.
ALTER TABLE jogo_temporada_beneficiado
    ADD COLUMN prejudicado BOOLEAN NOT NULL DEFAULT false;
