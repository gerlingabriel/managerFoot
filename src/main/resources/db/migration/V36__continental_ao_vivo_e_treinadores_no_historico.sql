-- Etapa da continental ao vivo (SPEC §8.3.6, rev. 79), na mesma tela da rodada: rodada é a da liga
-- depois da qual a etapa é jogada e etapa_continental, o índice dela no quadro. Nulo nos outros casos.
ALTER TABLE jogo_rodada_ao_vivo
    ADD COLUMN etapa_continental INT CHECK (etapa_continental >= 0),
    ADD CONSTRAINT ck_jogo_rodada_ao_vivo_etapa CHECK (etapa_copa IS NULL OR etapa_continental IS NULL);

-- Quadros do Histórico (modelo 13a, rev. 79): o treinador embaixo do vice e do 3º, com o nome copiado
-- como o do clube; o usuário, para marcar "(você)". Temporadas já encerradas ficam sem eles.
ALTER TABLE jogo_campeao
    ADD COLUMN vice_treinador      TEXT,
    ADD COLUMN vice_usuario_id     BIGINT REFERENCES usuario (id),
    ADD COLUMN terceiro_treinador  TEXT,
    ADD COLUMN terceiro_usuario_id BIGINT REFERENCES usuario (id);
ALTER TABLE jogo_campeao_copa
    ADD COLUMN vice_treinador  TEXT,
    ADD COLUMN vice_usuario_id BIGINT REFERENCES usuario (id);
ALTER TABLE jogo_campeao_continental
    ADD COLUMN vice_treinador  TEXT,
    ADD COLUMN vice_usuario_id BIGINT REFERENCES usuario (id);
