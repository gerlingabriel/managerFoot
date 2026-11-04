-- Meta do patrocínio paga assim que fica garantida (SPEC §9.6, rev. 97; modelo 20a). A rodada da liga em
-- que o bônus foi pago; nulo enquanto não foi. O fim da temporada só paga as metas ainda não pagas.
ALTER TABLE jogo_patrocinio ADD COLUMN meta_paga_rodada INT CHECK (meta_paga_rodada >= 1);
