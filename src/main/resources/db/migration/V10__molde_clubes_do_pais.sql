-- Formato da liga no molde (SPEC §4.1, rev. 27): o clube pertence só ao país; a divisão diz
-- quantos clubes tem e quantos rebaixa. Os clubes são distribuídos pela força ao criar o jogo.

-- quantidade_clubes: herdada da contagem atual; a faixa de 15 a 20 fica no ValidadorLiga.
ALTER TABLE molde_divisao ADD COLUMN quantidade_clubes INT;
UPDATE molde_divisao d
SET quantidade_clubes = greatest(1, (SELECT count(*) FROM molde_clube c WHERE c.divisao_id = d.id));
ALTER TABLE molde_divisao
    ALTER COLUMN quantidade_clubes SET NOT NULL,
    ADD CONSTRAINT ck_molde_divisao_quantidade CHECK (quantidade_clubes >= 1);

-- Promovidos = rebaixados da divisão de cima: calculados, não guardados.
ALTER TABLE molde_divisao DROP COLUMN promovidos;

DROP INDEX ix_molde_clube_divisao;
ALTER TABLE molde_clube DROP COLUMN divisao_id;
ALTER TABLE molde_divisao DROP CONSTRAINT uq_molde_divisao_pais;

-- Excluir uma divisão renumera as de baixo num só UPDATE: a unicidade é conferida no fim da
-- transação, para não colidir no meio da renumeração.
ALTER TABLE molde_divisao DROP CONSTRAINT uq_molde_divisao_prioridade;
ALTER TABLE molde_divisao ADD CONSTRAINT uq_molde_divisao_prioridade UNIQUE (pais_id, prioridade)
    DEFERRABLE INITIALLY DEFERRED;

-- A semente do jogo agora sorteia também a distribuição dos clubes, e vale para o clube escolhido.
ALTER TABLE jogo RENAME COLUMN semente_clube_inicial TO semente_criacao;
