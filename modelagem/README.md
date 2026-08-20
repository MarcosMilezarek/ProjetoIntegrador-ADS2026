# Modelagem do Sistema — Índice para Agentes

Sistema de Gerenciamento de Vagas: um Portal do Candidato (cadastro de currículo, vagas, candidatura, acompanhamento, envio de documentos) e um Painel do RH (cadastro de vagas, gestão de candidatos, atualização de status, triagem assistida por IA).

## Como usar esta pasta

Antes de implementar uma funcionalidade, leia os arquivos relevantes **nesta ordem**:

1. **[glossario.md](glossario.md)** — siglas usadas no projeto (RF, RN, RNF, LGPD, ATS...).
2. **[atores.md](atores.md)** — perfis de usuário e o que cada um pode fazer.
3. **[requisitos-funcionais.md](requisitos-funcionais.md)** — **o que** o sistema deve fazer (RF01-RF16). Comece aqui para entender o escopo da funcionalidade pedida.
4. **[regras-de-negocio.md](regras-de-negocio.md)** — **como** as funcionalidades devem se comportar: validações e restrições (RN01-RN08). Leia sempre junto com os requisitos funcionais.
5. **[casos-de-uso.md](casos-de-uso.md)** — fluxo principal/alternativo de cada caso de uso, mais os diagramas de caso de uso.
6. **[requisitos-nao-funcionais.md](requisitos-nao-funcionais.md)** — performance, segurança, usabilidade etc (RNF01-RNF13). Consulte ao tomar decisões técnicas.
7. **[modelo-de-dados.md](modelo-de-dados.md)** — entidades conceituais e diagrama ER. O schema real está em [database/schema.sql](../database/schema.sql).
8. **[diagrama-de-classes.md](diagrama-de-classes.md)** — classes de domínio e relacionamentos.
9. **[diagrama-de-sequencia.md](diagrama-de-sequencia.md)** — ordem das interações entre ator, front-end, API, banco e serviços externos, por caso de uso.
10. **[diagrama-de-estados.md](diagrama-de-estados.md)** — estados e transições de Candidatura e Vaga.
11. **[prototipos-de-telas.md](prototipos-de-telas.md)** — protótipos de baixa fidelidade de cada tela.
12. **[arquitetura.md](arquitetura.md)** — separação front-end/API/banco/IA/notificações, módulos do sistema, integração com IA (OpenRouter) e fluxo operacional do processo seletivo.

Os diagramas (UML/ER/protótipos) são imagens em [imagens/](imagens/), referenciadas a partir do arquivo `.md` correspondente.

## Regra geral para agentes

- **Tarefa de frontend (tela/componente)**: `requisitos-funcionais.md` → `regras-de-negocio.md` → `casos-de-uso.md` → `prototipos-de-telas.md` (mockup da tela).
- **Tarefa de backend (endpoint/serviço)**: `requisitos-funcionais.md` → `regras-de-negocio.md` → `casos-de-uso.md` → `modelo-de-dados.md` → `diagrama-de-classes.md`.
- **Tarefa de banco de dados**: `modelo-de-dados.md` (e o [database/schema.sql](../database/schema.sql) para o estado atual do schema).
- **Tarefa envolvendo triagem por IA**: `arquitetura.md` (seção "Integração com IA" e "Critérios para triagem assistida por IA") → `regras-de-negocio.md` (RN04).
- **Dúvida sobre uma sigla**: `glossario.md`. **Dúvida sobre quem pode fazer o quê**: `atores.md`.

Cada arquivo cobre apenas o seu escopo — não duplique conteúdo entre eles. Se uma informação não estiver em nenhum arquivo, ela ainda não foi modelada; não invente requisito ou regra de negócio.
