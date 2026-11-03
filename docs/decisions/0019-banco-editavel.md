# 0019 — Banco editável por arquivo

- **Status:** Aceita (SPEC §3.5, rev. 89)
- **Data:** 2026-11-03

## Contexto

- Cada pessoa quer mudar nomes de times e jogadores sem o servidor guardar o banco dela, e o jogo não pode
  distribuir nomes que infrinjam direito autoral.
- O jogo é criado copiando um molde (`docs/decisions/0006`). Toda a criação (distribuição das divisões,
  continental, Mundial, treinadores) lê o molde.

## Opções consideradas

1. **Molde temporário:** gravar o arquivo como molde, criar o jogo como sempre e apagar o molde na mesma
   transação (escolhida).
2. **Criar o jogo direto do arquivo, sem molde.** Exigiria uma segunda versão de toda a cópia do molde para o jogo,
   que teria de acompanhar cada mudança da primeira.
3. **Guardar o banco da pessoa no servidor.** Contraria o pedido do usuário; fica como a opção "molde pessoal" da §3.5.

## Decisão

- O arquivo é JSON com `tipo: "banco"` e versão de formato, organizado por país, clube e jogador (`BancoEditavel`).
- A leitura junta todos os problemas, com o país, o clube e o jogador de cada um, e recusa o arquivo se houver algum.
- O arquivo aceito fica na sessão. Ao criar o jogo ele é lido de novo, gravado como molde, usado por
  `JogosDoTreinadorRepositorio.criarComSorteio` e apagado; o jogo fica com `molde_id` nulo.

## Consequências

- A criação do jogo é a mesma para o banco padrão e para o arquivo; uma regra nova da criação vale para os dois.
- Criar um jogo de um arquivo grande grava e apaga alguns milhares de jogadores numa transação: leva alguns segundos.
- O banco com nomes reais que o usuário pediu fica fora do repositório, que é público.
