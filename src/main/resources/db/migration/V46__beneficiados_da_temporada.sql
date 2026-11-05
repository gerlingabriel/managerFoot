-- Sorte da temporada (SPEC §5.3.1, rev. 108): cada divisão tem dois beneficiados da máquina sorteados, mais os
-- clubes de usuário fracos, e nenhum prejudicado. Oculto dos participantes.
CREATE TABLE jogo_temporada_beneficiado (
    temporada_id BIGINT NOT NULL REFERENCES jogo_temporada (id) ON DELETE CASCADE,
    divisao_id   BIGINT NOT NULL REFERENCES jogo_divisao (id) ON DELETE CASCADE,
    clube_id     BIGINT NOT NULL REFERENCES jogo_clube (id) ON DELETE CASCADE,
    PRIMARY KEY (temporada_id, clube_id)
);

-- O sorteio antigo (um beneficiado e um prejudicado) continua aqui para as temporadas já começadas e os saves
-- exportados antes: sem linha na tabela nova, vale o beneficiado antigo até a próxima temporada.
ALTER TABLE jogo_temporada_divisao
    ALTER COLUMN beneficiado_id DROP NOT NULL,
    ALTER COLUMN prejudicado_id DROP NOT NULL;
