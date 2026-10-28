# 0008 — Banco padrão gerado por código e carregado por comando

- **Status:** Aceita
- **Data:** 2026-10-27

## Contexto

- A SPEC §3.5 e §4.1 (rev. 30 e 31) pedem um banco padrão com 6 países,
  formatos reais adaptados (`docs/banco-padrao-formatos.md`) e nomes
  fictícios.
- São 360 clubes e 8.280 jogadores. Sem esses dados não dá para criar jogo.
- Os números da geração (faixas de força, asteriscos, temperamento) ainda
  podem mudar depois que o usuário testar.

## Opções consideradas

1. **Migração Flyway com os dados em SQL:** cerca de 8 mil `INSERT`s num
   arquivo. Mudar um número exigiria gerar o arquivo de novo e criar outra
   migração, porque uma migração aplicada não é editada.
2. **Gerador em código, com semente fixa, e comando
   `--carregar-banco-padrao`:** o mesmo padrão do `--criar-admin` (0007).
3. **Gerar automaticamente ao subir a aplicação:** esconde uma escrita
   grande no banco dentro da inicialização.

## Decisão

Opção 2.

- O gerador fica no pacote `bancopadrao`, como domínio puro, sem Spring nem
  banco, e é testado sem container.
- A mesma semente gera o mesmo banco. Cada país tem sua própria sequência,
  para que acrescentar um país não mude os outros.
- `BancoPadraoRepositorio` grava tudo numa transação, num molde chamado
  "Banco padrão". Se esse molde já existe, nada é gravado. Um advisory lock
  do PostgreSQL faz duas cargas simultâneas esperarem uma pela outra.
- O pacote `bancopadrao` se soma aos da decisão 0005.

## Consequências

- Para mudar a geração, basta ajustar `ParametrosBancoPadrao` e carregar de
  novo num banco sem o molde. Jogos já criados não mudam, porque cada jogo
  é uma cópia (0006).
- Recarregar por cima de um molde existente não é possível. Quando o editor
  existir, as mudanças do usuário serão feitas no molde, não no gerador.
- A identificação do molde pelo nome é provisória. Quando houver molde
  pessoal (SPEC §3.5, em aberto), o molde padrão pode ganhar uma marcação
  própria.
