# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Candidato** (usuário externo): procura vagas, mantém um currículo único, candidata-se e acompanha o andamento. Usa principalmente pelo **celular**: o portal do candidato é mobile-first.
- **Profissional de RH** (usuário interno): cadastra e encerra vagas, consulta inscritos, atualiza etapas, revisa documentos e a triagem assistida por IA. Usa principalmente no **desktop**; o painel precisa continuar utilizável no celular.
- **Administrador**: previsto na modelagem, sem área implementada (login mostra "ainda não disponível").

## Product Purpose

Sistema de Gerenciamento de Vagas (Projeto Integrador, ADS — IFRS Campus Erechim, 2026). Digitaliza recrutamento e seleção: tira o processo de planilhas e e-mails, centraliza informações e melhora a comunicação entre empresa e candidato. Sucesso = candidato se candidata e entende em que etapa está sem precisar perguntar; RH gerencia vagas e inscritos num só lugar.

## Positioning

Um único currículo reaproveitado em todas as candidaturas, acompanhamento de status visível ao candidato em cada etapa (inscrito → em análise → entrevista → aprovado/reprovado) e triagem por IA apresentada sempre como recomendação revisada por uma pessoa, nunca como decisão automática (RN04).

## Operating Context

- Fluxo: RH cadastra vaga → candidato vê e se candidata (status `inscrito`) → RH consulta inscritos e pode pedir triagem por IA → RH atualiza status → se aprovado, candidato recebe pedido de documentos.
- Produção: https://upteam.duckdns.org/ (frontend estático servido pelo nginx, API em `/api`).
- Documentação de requisitos, regras e casos de uso em `modelagem/`.

## Capabilities and Constraints

- Ligado ao backend real: login (`POST /auth/login`), cadastro (`POST /usuarios`), vagas (`GET/POST/PUT /vagas`, encerrar = `PUT status=encerrada`, sem DELETE), currículo (`GET /curriculos/usuario/{id}`, `POST /curriculos`, `PUT /curriculos/{id}`). Essa comunicação deve ser preservada em qualquer redesenho.
- Ainda mockado (localStorage) por falta de backend: candidatura, lista de candidatos por vaga, documentos, notificações. Triagem por IA é só visual.
- Não há sessão/JWT: o usuário logado é passado explicitamente em cada chamada; F5 volta ao login.
- Recuperação de senha (RF03) não existe.
- Documentos: PDF ou DOCX até 5 MB (RNF09).
- Stack: React 19 + TypeScript + Vite, Tailwind 4, componentes shadcn/ui (Radix); telas em `frontend/src/pages/`, orquestradas por `frontend/src/App.tsx`.

## Brand Commitments

- Nome do produto: **TeamUp** (substitui o provisório "Vagas+").
- Verde é a cor de identidade (pedido explícito do usuário).
- Tema claro e escuro, alternável pelo usuário; o padrão segue a preferência do sistema operacional.
- Aparência própria, profissional, sem cara de template genérico.
- Idioma da interface: português do Brasil.

## Evidence on Hand

- Nenhum dado de uso, depoimento, cliente ou métrica real. **Não inventar números** na interface (os antigos "18 vagas abertas / 3 etapas / 100% revisão humana" foram removidos a pedido do usuário).
- Dados de demonstração do mock (candidatos, documentos, notificações) são sintéticos.

## Product Principles

1. O candidato sempre sabe em que etapa está e qual é o próximo passo.
2. A decisão é humana: IA aparece como apoio, com o critério visível, nunca como veredito.
3. Um currículo, muitas candidaturas: reduzir retrabalho do candidato.
4. Honestidade de dados: nada de números ou provas inventadas; o que é demonstração fica claro.

## Accessibility & Inclusion

LGPD: dados pessoais do candidato só aparecem para quem precisa (currículo não tem listagem geral). Sem requisito formal de WCAG registrado; manter contraste e foco visíveis em ambos os temas.
