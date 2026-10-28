# 0009 — Login com sessão, telas de entrada e link pelo terminal

- **Status:** Aceita
- **Data:** 2026-10-28

## Contexto

- A 3f.1 abre as telas: entrar, cadastro pelo link e sair (SPEC §3.4,
  modelo aprovado em §10.5, rev. 28). O ADR 0007 deixou o Spring Security
  completo para este momento.
- O jogo será testado primeiro só na máquina do usuário; publicação em
  domínio e rede ficam para depois.
- Ainda não há tela do administrador para gerar links de cadastro.

## Opções consideradas

**Sessão:**
1. Sessão HTTP do servidor (padrão do Spring Security), com CSRF nos
   formulários.
2. Token (JWT) no navegador.

**HTMX:**
1. Webjar servido pela aplicação.
2. CDN.

**Gerar link sem tela:**
1. Opção `--gerar-link-cadastro=<e-mail do administrador>`, mostrando o link
   no terminal.
2. Esperar a tela do administrador.

## Decisão

- **Sessão HTTP com CSRF.** As telas são Thymeleaf renderizadas no
  servidor; JWT não traz ganho e complica sair. O usuário fica na sessão
  como `Usuario` (sem senha), com o papel como `ROLE_<PAPEL>`. A senha
  continua verificada por `ContasRepositorio.autenticar`.
- **Abertos sem login:** `/entrar`, `/cadastro`, `/css/**`, `/webjars/**`,
  `/actuator/health` e `/error`. O resto exige login.
- **Cadastro já entra:** troca o id da sessão e grava o usuário nela.
- **HTMX por webjar**, sem dependência de CDN. As fontes (Barlow) vêm do
  Google Fonts, como no modelo; sem internet o navegador usa a fonte do
  sistema.
- **O convite vai na URL** (`/cadastro?convite=…`). Para ele não vazar para
  o Google Fonts pelo cabeçalho Referer, as páginas mandam
  `Referrer-Policy: same-origin`.
- **Link pelo terminal**, impresso na saída padrão e não no log, porque
  quem tem o link cria uma conta. O endereço base vem de `URL_BASE`
  (padrão `http://localhost:8080`).

## Consequências

- Erros de cadastro aparecem embaixo de cada campo (`RegrasConta.problemasPorCampo`);
  link inválido e e-mail em uso têm exceções próprias
  (`LinkInvalidoException`, `EmailEmUsoException`), ainda subclasses de
  `IllegalArgumentException`.
- Erro no cadastro não gasta o link.
- Depois de errar a senha, o e-mail digitado não volta preenchido.
- Sem limite de tentativas de login por enquanto: aceitável para uso
  local, precisa ser decidido antes de publicar em rede.
- O início depois de entrar é provisório (nome e Sair); as opções do
  início (§10.5, rev. 29) entram na 3f.2.
