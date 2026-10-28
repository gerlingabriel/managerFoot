-- Som dos gols ligado ou desligado (SPEC §10.2 e §10.4), escolhido no quadro da rodada e mantido
-- para as próximas, como a velocidade.
ALTER TABLE jogo_treinador ADD COLUMN som_gols BOOLEAN NOT NULL DEFAULT true;
