# 0003 — Uso do repositório de referência do Elifoot 98

- **Status:** Aceita
- **Data:** 2026-10-26

## Contexto

O repositório `trsthales/desvendando-elifoot-98` traz pseudocódigo
reconstruído por engenharia reversa e declara finalidade educacional. O
Elifoot 98 pertence a André Elias.

## Decisão

- Usar a referência como **descrição de mecânicas**, não como código-fonte.
  Não copiar o pseudocódigo. Reimplementar em Java a partir das regras
  documentadas em [`docs/referencia-elifoot98.md`](../referencia-elifoot98.md).
- Constantes vindas da referência (6000/7000, pesos de disciplina etc.)
  entram como **parâmetros configuráveis** com valor inicial, nunca fixas no
  código.
- Quando a referência for inconsistente ou omissa, a regra vira "Em aberto"
  na SPEC até o usuário decidir.
- Não usar nomes, marcas ou arquivos (`.EFT`, `.SAV`) do jogo original.

## Consequências

- O balanceamento parte de valores conhecidos, mas pode divergir deles.
- Economia, mercado e asterisco não têm base na referência e precisam de
  modelo próprio.
