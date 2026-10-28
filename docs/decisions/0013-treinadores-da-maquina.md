# 0013 — Treinadores da máquina e histórico

- **Status:** Aceita (SPEC §5.4 e §8.1.1, rev. 54; modelo 8a aprovado)
- **Data:** 2026-10-28

## Contexto

- O ranking de treinadores conta todos os campeões da 1ª divisão, da máquina e usuários (rev. 42 e
  46), mas só os usuários existiam como treinadores.
- O usuário pediu um treinador por clube, obrigatório ao criar ou editar um clube.
- As demissões e os treinadores livres (§5.4) ainda estão em aberto.

## Decisão

- **Molde:** `molde_clube.treinador` obrigatório (V23). `MoldeRepositorio.adicionarClube` exige o
  nome; `trocarTreinador` é a edição. O banco padrão gera nome e sobrenome do idioma do país, com
  gerador próprio para não mudar os clubes e planteis já gerados. Clubes cadastrados antes recebem
  "Treinador do <clube>", e carregar o banco padrão de novo troca esse nome pelo gerado.
- **Jogo:** `jogo_treinador_maquina` (id, nome, clube), copiado do molde ao criar o jogo. Tabela
  própria, separada de `jogo_treinador` (os usuários), porque muitas consultas usam
  `jogo_treinador` para saber quem é humano, e porque as demissões vão mudar treinadores de clube.
  O clube de um usuário mantém o seu treinador da máquina, que não aparece enquanto o usuário
  estiver lá.
- **Campeões:** `jogo_campeao.treinador_maquina_id` quando nenhum usuário dirigia o clube; o ranking
  agrupa por usuário ou por treinador da máquina e mostra o clube atual de cada um.

## Consequências

- Sem demissões, o treinador da máquina fica no mesmo clube o jogo inteiro.
- Jogos salvos antes da V23 têm treinadores com o nome provisório.
- Ainda não há tela de edição do molde; a obrigatoriedade vale no repositório e no banco.
