-- Patrocínio e prêmio da liga no extrato (SPEC §8.1 e §9.6, rev. 45 e 46). O patrocínio é lançado
-- na rodada 0 da temporada (o início); o prêmio, na última rodada da temporada encerrada. A chave
-- única (clube, temporada, rodada, tipo) impede pagar duas vezes.
ALTER TABLE jogo_lancamento DROP CONSTRAINT jogo_lancamento_tipo_check;
ALTER TABLE jogo_lancamento ADD CONSTRAINT jogo_lancamento_tipo_check
    CHECK (tipo IN ('BILHETERIA', 'CUSTO_DO_JOGO', 'DESLOCAMENTO', 'SALARIOS', 'PATROCINIO', 'PREMIO'));
ALTER TABLE jogo_lancamento DROP CONSTRAINT jogo_lancamento_rodada_check;
ALTER TABLE jogo_lancamento ADD CONSTRAINT jogo_lancamento_rodada_check CHECK (rodada >= 0);
