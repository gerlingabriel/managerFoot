-- Escalação do treinador humano (SPEC §7.2 e §10.3, rev. 34): formação, titulares, banco e
-- batedor de pênalti. Pode ficar incompleta enquanto o treinador monta o time; a validade
-- (formação, até 7 reservas, suspensos) é conferida pelo domínio antes da rodada.
-- Clubes sem linha aqui são escalados pela máquina.

-- alvo da chave composta: o jogador escalado é do próprio clube
ALTER TABLE jogo_jogador ADD CONSTRAINT uq_jogo_jogador_clube UNIQUE (clube_id, id);

CREATE TABLE jogo_escalacao (
    clube_id   BIGINT PRIMARY KEY REFERENCES jogo_clube (id) ON DELETE CASCADE,
    formacao   TEXT   NOT NULL CHECK (formacao IN (
        'F_4_4_2', 'F_3_5_2', 'F_4_3_3', 'F_3_4_3', 'F_4_2_4', 'F_5_4_1', 'F_4_5_1', 'F_3_6_1')),
    batedor_id BIGINT,
    CONSTRAINT fk_jogo_escalacao_batedor
        FOREIGN KEY (clube_id, batedor_id) REFERENCES jogo_jogador (clube_id, id)
);

-- Quem não aparece aqui está fora do banco. A chave primária impede o mesmo jogador duas vezes.
CREATE TABLE jogo_escalacao_jogador (
    clube_id   BIGINT NOT NULL REFERENCES jogo_escalacao (clube_id) ON DELETE CASCADE,
    jogador_id BIGINT NOT NULL,
    papel      TEXT   NOT NULL CHECK (papel IN ('TITULAR', 'RESERVA')),
    PRIMARY KEY (clube_id, jogador_id),
    CONSTRAINT fk_jogo_escalacao_jogador_mesmo_clube
        FOREIGN KEY (clube_id, jogador_id) REFERENCES jogo_jogador (clube_id, id) ON DELETE CASCADE
);
