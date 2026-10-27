# 0007 — Contas, senhas e link de cadastro

- **Status:** Aceita
- **Data:** 2026-10-27

## Contexto

- A SPEC §3.4 pede um administrador criado por script, um link de cadastro
  que transforma quem o usa em participante comum, e papéis.
- As regras do link e do login foram decididas na rev. 24: uso único, 7
  dias, e-mail e senha de 8 ou mais caracteres.
- As telas ainda não existem: os modelos precisam de aprovação antes.

## Opções consideradas

**Senhas:**
1. `spring-security-crypto` (só o BCrypt), sem o filtro web do Spring
   Security por enquanto.
2. Spring Security completo já agora.

**Token do link:**
1. Token aleatório de 32 bytes; o banco guarda só o hash SHA-256.
2. Guardar o token em texto.

**Script do administrador:**
1. A própria aplicação com `--criar-admin`, lendo e-mail, nome e senha de
   variáveis de ambiente.
2. Script SQL com hash calculado à mão.

## Decisão

- **Senhas com `spring-security-crypto` (BCrypt).** O Spring Security
  completo (login, sessão, CSRF) entra junto com as telas, quando houver
  rotas a proteger. Adicioná-lo antes bloquearia o health check sem ganho.
- **Token aleatório com hash SHA-256 no banco.** Quem lê o banco não
  consegue usar links pendentes. O token só aparece uma vez, ao ser gerado.
- **Script pela aplicação**, com a senha em variável de ambiente, nunca em
  argumento de linha de comando (fica no histórico do shell e na lista de
  processos) nem em log.
- Novo pacote **`contas`** para usuários, papéis e cadastro. Não é regra
  de jogo nem sessão em rede; o multiplayer vai referenciar os usuários.

## Consequências

- O uso único do link é garantido no banco: o cadastro marca o link como
  usado numa única instrução, que só tem efeito se ele ainda não foi usado e
  não expirou.
- O login ainda não tem tela nem sessão HTTP: `ContasServico.autenticar`
  verifica a senha e será usado pelo Spring Security quando ele entrar.
- Senhas com mais de 72 bytes são recusadas, porque o BCrypt só considera
  os primeiros 72.
