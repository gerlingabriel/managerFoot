-- Mercado (SPEC §9.4, rev. 43): jogador anunciado para compra direta. A máquina deixa 5 ou 6 à
-- venda por clube, sorteados (economia.MercadoDaMaquina); clube de treinador humano não tem anúncio
-- automático. Jogos salvos antes desta migração ganham os anúncios no próximo avanço de rodada.
ALTER TABLE jogo_jogador
    ADD COLUMN a_venda BOOLEAN NOT NULL DEFAULT false;
