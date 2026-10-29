-- Salário gravado e pedido de aumento (SPEC §9.5, rev. 58, modelo 10a).

-- Força em que o salário do jogador foi fixado. Nula = o salário segue a força atual, como nos clubes
-- da máquina, que readequam sempre. Nos clubes de usuário fica gravada no último contrato (início do
-- jogo, compra ou aumento aceito): se a força cai, o salário não cai; se sobe bastante, ele pede aumento.
-- aumento_temporada/aumento_rodada: quando o último pedido foi respondido (um por rodada no clube; quem
-- respondeu não pede de novo na temporada).
ALTER TABLE jogo_jogador
    ADD COLUMN forca_salario     INT CHECK (forca_salario > 0),
    ADD COLUMN aumento_temporada INT CHECK (aumento_temporada >= 1),
    ADD COLUMN aumento_rodada    INT CHECK (aumento_rodada >= 0),
    ADD CONSTRAINT ck_jogo_jogador_aumento CHECK ((aumento_temporada IS NULL) = (aumento_rodada IS NULL));

-- Jogos salvos: os jogadores dos clubes de usuário ficam com o salário da força de agora.
UPDATE jogo_jogador j SET forca_salario = j.forca
FROM jogo_treinador t WHERE t.clube_id = j.clube_id;
