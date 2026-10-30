-- Clubes de fora do campeonato deixam uns dois jogadores à venda (SPEC §9.4, rev. 84). Nos jogos já
-- criados, a escolha é fixa pelo id do jogador; nas temporadas seguintes, MercadoRepositorio sorteia.
UPDATE jogo_jogador SET a_venda = true
WHERE id IN (
    SELECT x.id FROM (
        SELECT j.id, row_number() OVER (PARTITION BY j.clube_id ORDER BY md5(j.id::text)) AS n
        FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id
        WHERE c.pais_codigo IS NOT NULL
    ) x WHERE x.n <= 2
);
