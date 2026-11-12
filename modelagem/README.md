# Modelagem do Sistema

Sistema de Gerenciamento de Vagas: um Portal do Candidato (cadastro de currículo, vagas, candidatura, acompanhamento, envio de documentos) e um Painel do RH (cadastro de vagas, gestão de candidatos, atualização de status, triagem assistida por IA).

## Conteúdo

1. **[glossario.md](glossario.md)**: siglas usadas no projeto (RF, RN, RNF, LGPD, ATS...).
2. **[atores.md](atores.md)**: perfis de usuário e o que cada um pode fazer.
3. **[requisitos-funcionais.md](requisitos-funcionais.md)**: o que o sistema deve fazer (RF01 a RF16).
4. **[regras-de-negocio.md](regras-de-negocio.md)**: como as funcionalidades devem se comportar, com validações e restrições (RN01 a RN08).
5. **[casos-de-uso.md](casos-de-uso.md)**: fluxo principal e alternativo de cada caso de uso, com os diagramas de caso de uso.
6. **[requisitos-nao-funcionais.md](requisitos-nao-funcionais.md)**: desempenho, segurança, usabilidade e outros (RNF01 a RNF13).
7. **[modelo-de-dados.md](modelo-de-dados.md)**: entidades conceituais e diagrama ER. O schema inicial está em [database/schema.sql](../database/schema.sql), e o vigente nas migrações do Flyway em `backend/src/main/resources/db/migration`.
8. **[diagrama-de-classes.md](diagrama-de-classes.md)**: classes de domínio e relacionamentos.
9. **[diagrama-de-sequencia.md](diagrama-de-sequencia.md)**: ordem das interações entre ator, front-end, API, banco e serviços externos, por caso de uso.
10. **[diagrama-de-estados.md](diagrama-de-estados.md)**: estados e transições de Candidatura e Vaga.
11. **[prototipos-de-telas.md](prototipos-de-telas.md)**: protótipos de baixa fidelidade de cada tela.
12. **[arquitetura.md](arquitetura.md)**: separação entre front-end, API, banco, IA e notificações, módulos do sistema, integração com a OpenRouter e fluxo operacional do processo seletivo.

Os diagramas (UML, ER e protótipos) são imagens em [imagens/](imagens/), referenciadas a partir do arquivo `.md` correspondente.

## Por onde começar

- **Escopo do sistema:** requisitos funcionais, regras de negócio e casos de uso.
- **Telas:** casos de uso e protótipos de telas.
- **Backend e banco:** modelo de dados, diagrama de classes e diagramas de sequência.
- **Triagem por IA:** seções "Integração com IA" e "Critérios para triagem assistida por IA" em [arquitetura.md](arquitetura.md), junto com a RN04 em [regras-de-negocio.md](regras-de-negocio.md).
