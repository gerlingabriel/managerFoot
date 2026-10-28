-- Venda do usuário por leilão (SPEC §9.4, rev. 53): o treinador põe um jogador do seu plantel em
-- leilão pela ficha, com o lance mínimo que ele define; o leilão é resolvido na hora. O tipo separa
-- esses leilões dos que abrem ao confirmar a tática (RODADA), para um não impedir o outro. A chave
-- única continua valendo para os dois: o jogador só volta a leilão depois que a rodada terminar.
ALTER TABLE jogo_leilao
    ADD COLUMN tipo TEXT NOT NULL DEFAULT 'RODADA' CHECK (tipo IN ('RODADA', 'VENDA'));
