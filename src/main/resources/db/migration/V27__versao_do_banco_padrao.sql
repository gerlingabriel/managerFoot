-- Versão do banco padrão gravada no molde (SPEC rev. 60, ADR 0008): quando o gerador muda (2 divisões
-- e 20 jogadores na rev. 60), a carga apaga o molde de versão antiga e grava o novo. Jogos já criados
-- não mudam: são cópias, e jogo.molde_id fica nulo (ON DELETE SET NULL). Nula = molde comum.
ALTER TABLE molde
    ADD COLUMN versao_banco_padrao INT CHECK (versao_banco_padrao >= 1);

-- O banco padrão carregado até a rev. 59 (3 divisões, 23 jogadores) é a versão 1.
UPDATE molde SET versao_banco_padrao = 1 WHERE nome = 'Banco padrão';
