-- Mundial de Clubes (SPEC §8.3.4, rev. 88): a região de cada país do molde e, no jogo, os clubes de fora que
-- concorrem a representar a sua região. Nula = o clube não concorre; jogos criados antes ficam sem Mundial.
ALTER TABLE molde_pais
    ADD COLUMN regiao TEXT CHECK (regiao IN ('AMERICA_DO_SUL', 'EUROPA', 'AMERICA_DO_NORTE', 'ASIA', 'AFRICA', 'OCEANIA'));

ALTER TABLE jogo_clube
    ADD COLUMN regiao_mundial TEXT CHECK (regiao_mundial IN ('AMERICA_DO_SUL', 'EUROPA', 'AMERICA_DO_NORTE', 'ASIA', 'AFRICA', 'OCEANIA')),
    ADD CONSTRAINT ck_jogo_clube_regiao_de_fora CHECK (regiao_mundial IS NULL OR pais_codigo IS NOT NULL);
CREATE INDEX ix_jogo_clube_regiao_mundial ON jogo_clube (jogo_id, regiao_mundial) WHERE regiao_mundial IS NOT NULL;
