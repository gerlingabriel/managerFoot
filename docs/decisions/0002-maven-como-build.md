# 0002 — Maven como ferramenta de build

- **Status:** Aceita
- **Data:** 2026-10-26

## Contexto

O repositório não tinha ferramenta de build. O CLAUDE.md §13 permite Maven
como padrão simples em repositório novo, desde que a escolha seja
registrada. A SPEC §16.1 deixava Maven/Gradle sem aprovação explícita.

## Opções consideradas

- **Maven:** convenção rígida, `pom.xml` declarativo, amplo suporte no
  Spring Initializr e nas IDEs.
- **Gradle:** mais flexível e rápido em builds grandes, porém com DSL mais
  complexa.

## Decisão

Maven, com Maven Wrapper (`mvnw`) commitado para fixar a versão.

Testes unitários e de integração ficam separados:
- unitários com o Surefire (`mvn test`);
- integração com Testcontainers via Failsafe (`mvn verify`), usando o sufixo
  `*IT.java`.

## Consequências

- O projeto não depende de uma instalação local de Maven.
- Uma troca futura para Gradle exige uma nova decisão que substitua esta.
