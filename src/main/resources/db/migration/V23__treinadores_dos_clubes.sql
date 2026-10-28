-- Um treinador por clube (SPEC §5.4 e §8.1.1, rev. 42 e 54): obrigatório ao criar ou editar um clube
-- no molde. Clubes já cadastrados recebem um nome provisório; o banco padrão troca pelo gerado ao ser
-- carregado de novo.
ALTER TABLE molde_clube ADD COLUMN treinador TEXT;
UPDATE molde_clube SET treinador = 'Treinador do ' || nome;
ALTER TABLE molde_clube
    ALTER COLUMN treinador SET NOT NULL,
    ADD CONSTRAINT ck_molde_clube_treinador CHECK (btrim(treinador) <> '' AND length(treinador) <= 60);

-- Treinadores da máquina no jogo, copiados do molde. Tabela própria porque as demissões (§5.4) vão
-- trocar treinadores de clube; enquanto não existem, cada um fica no seu clube. O clube de um usuário
-- mantém o treinador da máquina, que só volta a aparecer se o usuário sair.
CREATE TABLE jogo_treinador_maquina (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogo_id  BIGINT NOT NULL REFERENCES jogo (id) ON DELETE CASCADE,
    nome     TEXT   NOT NULL CHECK (btrim(nome) <> '' AND length(nome) <= 60),
    clube_id BIGINT,
    CONSTRAINT uq_jogo_treinador_maquina_clube UNIQUE (clube_id),
    CONSTRAINT fk_jogo_treinador_maquina_clube_mesmo_jogo
        FOREIGN KEY (jogo_id, clube_id) REFERENCES jogo_clube (jogo_id, id) ON DELETE CASCADE
);

INSERT INTO jogo_treinador_maquina (jogo_id, nome, clube_id)
SELECT jogo_id, 'Treinador do ' || nome, id FROM jogo_clube ORDER BY id;

-- O campeão guarda o treinador da máquina quando nenhum usuário dirigia o clube (ranking, §8.1.1).
ALTER TABLE jogo_campeao
    ADD COLUMN treinador_maquina_id BIGINT REFERENCES jogo_treinador_maquina (id) ON DELETE SET NULL;
UPDATE jogo_campeao c SET treinador_maquina_id = t.id
FROM jogo_treinador_maquina t WHERE t.clube_id = c.clube_id AND c.usuario_id IS NULL;
