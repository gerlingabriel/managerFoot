-- Jogo em rede (SPEC §3.3, rev. 102): com o jogo parado, cada treinador que abre uma janela (o próprio
-- time ou outro clube) entra na parada. O jogo volta quando todos fecham ou quando acaba o tempo de quem
-- parou primeiro (jogo_rodada_ao_vivo.pausa_ate).
CREATE TABLE jogo_pausa_participante (
    jogo_id    BIGINT      NOT NULL REFERENCES jogo_rodada_ao_vivo (jogo_id) ON DELETE CASCADE,
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id),
    desde      TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (jogo_id, usuario_id)
);
