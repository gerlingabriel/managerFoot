-- Dono do jogo e treinador humano (SPEC §3.2, rev. 25). O treinador fica em tabela própria porque
-- o multiplayer e as demissões (§5.4) trarão vários treinadores por jogo e troca de clube.

-- Nulos só nos jogos criados antes desta migração.
-- semente_clube_inicial: nula quando o administrador escolheu o clube.
ALTER TABLE jogo
    ADD COLUMN dono_id               BIGINT REFERENCES usuario (id),
    ADD COLUMN semente_clube_inicial BIGINT;

-- alvo da chave composta de jogo_treinador: garante clube do mesmo jogo
ALTER TABLE jogo_clube ADD CONSTRAINT uq_jogo_clube_jogo UNIQUE (jogo_id, id);

CREATE TABLE jogo_treinador (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    jogo_id    BIGINT NOT NULL REFERENCES jogo (id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuario (id),
    clube_id   BIGINT NOT NULL,
    CONSTRAINT uq_jogo_treinador_usuario UNIQUE (jogo_id, usuario_id),
    CONSTRAINT uq_jogo_treinador_clube UNIQUE (clube_id),
    CONSTRAINT fk_jogo_treinador_clube_mesmo_jogo
        FOREIGN KEY (jogo_id, clube_id) REFERENCES jogo_clube (jogo_id, id) ON DELETE CASCADE
);
