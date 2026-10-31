# 0018 — Arquivo de save em JSON

- **Status:** Aceita (SPEC §3.5, rev. 89)
- **Data:** 2026-10-31

## Contexto

- A SPEC define que o usuário baixa o jogo em JSON com versão de formato, pode apagá-lo do servidor e envia o arquivo
  de volta, que é revalidado porque pode ter sido alterado.
- O estado do jogo está em 28 tabelas `jogo_*`, ligadas por chaves estrangeiras e ids gerados pelo banco. Novas
  migrações costumam acrescentar colunas e tabelas.
- O arquivo vem de fora: não dá para confiar nos ids dele. Um id aceito como veio poderia apontar para o jogo de outra
  pessoa.

## Opções consideradas

1. **Copiar as tabelas pela lista explícita, trocando os ids ao enviar** (escolhida).
2. **Modelo próprio do save, com objetos do domínio.** Isola o arquivo do esquema, mas exige escrever e manter o
   mapeamento de dezenas de campos de estado, e um campo esquecido só apareceria num jogo já carregado.
3. **Assinar o arquivo e recusar qualquer alteração.** Decidir isso cabe ao usuário (§3.5) e não dispensa a validação:
   a chave ficaria no servidor, e um arquivo de outra instalação não abriria.

## Decisão

- `TabelasDoSave` lista as tabelas na ordem de gravação, as colunas com id de outra tabela e as com id de conta.
- Ao enviar, todo id do arquivo vira um id novo, e toda referência precisa apontar para um registro do próprio arquivo.
  Colunas de conta passam a ser de quem enviou. `jogo.molde_id` não vai para o arquivo, porque o molde só é usado ao
  criar o jogo.
- Nomes de tabela e coluna do arquivo só chegam ao SQL se existirem na lista e no esquema; os valores vão por parâmetro.
- A importação roda numa transação só. Depois de gravar, abre a liga e a próxima etapa do jogo; qualquer recusa desfaz tudo.
- `jogo_rodada_ao_vivo` e `jogo_substituicao_ao_vivo` ficam fora, e a rodada ao vivo não baixa.

## Consequências

- Uma coluna nova opcional ou com valor padrão continua aceitando arquivos antigos. Uma coluna nova obrigatória, ou que
  mude o sentido de um dado, exige subir `SaveRepositorio.FORMATO` e converter os arquivos antigos.
- `SaveRepositorioIT` compara a lista com o esquema: tabela ou referência nova fora da lista quebra o teste.
- Valores alterados de propósito, mas dentro das restrições, são aceitos (Provisório, §3.5).
- O limite de envio é de 100 MB. Acima dele, o envio falha sem mensagem própria.
