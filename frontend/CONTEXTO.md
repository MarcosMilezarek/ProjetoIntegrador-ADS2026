# Contexto do Frontend

> Histórico do que já foi desenvolvido e em que etapa o frontend está. Atualize este arquivo
> sempre que uma feature nova for concluída, para quem retomar o trabalho (humano ou IA) não
> precisar reconstruir o contexto do zero. Última atualização: 2026-09-29.

## Visual (redesign de 2026-09-29)

- Marca **TeamUp** (substitui "Vagas+"). Direção visual "Liquid Glass" em verde: fundo branco
  com halos verdes desfocados, superfícies de vidro translúcido, navbar de vidro flutuante,
  orbe verde em CSS no login, Fustat (títulos) + Inter (corpo). Sistema documentado em
  [`../DESIGN.md`](../DESIGN.md); contexto de produto em [`../PRODUCT.md`](../PRODUCT.md).
- Tema claro/escuro: padrão segue o sistema; o usuário pode fixar claro ou escuro (salvo em
  `localStorage`, chave `teamup-theme`). Script em `index.html` aplica o tema antes da pintura.
- Todos os estilos estão em `src/styles.css` (tokens em `:root` e `.dark`).
- Mudanças de UX: removidos controles sem função (abas Candidato/RH do login, "Manter
  conectado", "Esqueci minha senha", telefone/cidade do cadastro que não eram enviados);
  erros de ações viram aviso em vez de falhar em silêncio; falha no carregamento inicial mostra
  "Tentar novamente" (não fica mais preso em "Carregando"); ações pós-login não recarregam a
  tela inteira; encerrar vaga pede confirmação; áreas ainda mockadas mostram
  "Dados de demonstração"; celular tem barra de abas inferior e tabelas viram fichas.

## Stack

- React 19 + TypeScript + Vite
- Tailwind CSS 4 + shadcn/ui (componentes em `src/components/ui/`, baseados em Radix)
- Sem gerenciador de estado externo (só `useState`/`useEffect` locais em `App.tsx`)
- `npm install && npm run dev` (ver `frontend/README.md`)

## Estado atual: Vaga e Currículo ligados ao backend real; o resto ainda é mock

O frontend inteiro está em um único arquivo, [`src/App.tsx`](src/App.tsx), com todas as telas do
Portal do Candidato e do Painel do RH.

### O que já está ligado ao backend real (`http://localhost:8080/api`)
- Login (`Login`) → `authService.login` → `POST /api/auth/login`; cadastro (`Signup`) →
  `authService.cadastrar` → `POST /api/usuarios`. Sempre usam `VITE_API_URL`, independente da
  flag de mock abaixo.
- **Vagas**: listar/criar/editar/encerrar. `portalService.getJobs/saveJob/closeJob` →
  [`src/services/rest-portal-service.ts`](src/services/rest-portal-service.ts) →
  `GET/POST/PUT /vagas` (não existe `DELETE`; "encerrar" é um `PUT` com `status: 'encerrada'`).
- **Currículo**: `portalService.getProfile/updateProfile` → `GET /curriculos/usuario/{usuarioId}`
  (404 tratado como "candidato ainda sem currículo") e `POST /curriculos` (primeiro salvamento) ou
  `PUT /curriculos/{id}` (edições seguintes — usa o **id do currículo**, não o `usuarioId`).
  > **Pendência (2026-09-29): o contrato do currículo mudou no backend e o front está defasado.**
  > `formacao` e `experiencias` não são mais texto livre: viraram listas estruturadas
  > (`formacoes[]` com curso/instituição/dataInicio/dataTermino e `experiencias[]` com
  > cargo/empresa/dataContratacao/dataDemissao/trabalhoAtual/descricaoAtividades). O currículo
  > também passou a ter dados pessoais (dataNascimento, sexo, cidade, uf), contato
  > (numeroContato, perfilLinkedin), `certificacoes`, `idade` (calculada, somente leitura) e
  > upload de PDF (`POST /curriculos/{id}/arquivo`, multipart, campo `arquivo`).
  > **Hoje o salvamento do currículo responde 400 em produção** (verificado em 2026-09-29):
  > o front envia `experiencias` como string e a API espera uma lista, o que quebra a
  > desserialização (`Requisição malformada ou com valores inválidos.`). Já `formacao`, que virou
  > campo desconhecido, é apenas ignorado. A tela "Meu currículo" e os tipos em `domain.ts`
  > precisam ser refeitos para o novo formato antes de voltar a funcionar (ver
  > `backend/CONTEXTO.md` para o contrato completo).
- Essas duas últimas só ficam ativas com `VITE_USE_MOCK_API=false` (ver composição híbrida
  abaixo); o identificador do usuário logado (`{id, nome, email, perfil}`) é guardado em
  `currentUser` (estado do `App`) e passado explicitamente a cada chamada, já que não há sessão
  real no backend.

### O que ainda roda 100% mockado (localStorage, sem tocar o backend)
[`src/services/portal-service.ts`](src/services/portal-service.ts) exporta `portalService` como
uma composição: com `VITE_USE_MOCK_API=false`, `getJobs/saveJob/closeJob/getProfile/updateProfile`
vão para `restPortalService` e o restante continua em `mockPortalService` (localStorage, chave
`vagas-plus-mock-db`). Sem a flag (padrão), tudo é mock. Continuam só mockados, pois o backend
ainda não implementa:
- Candidatar-se e acompanhar candidaturas (`apply`, `getApplications`, `updateApplicationStatus`)
- Listagem de candidatos por vaga (`getCandidates`, no painel do RH)
- Documentos (upload pelo candidato, revisão pelo RH)
- Notificações

## Telas implementadas (todas em `App.tsx`)

- **Login** / **Signup** — reais (ver acima)
- **Portal do candidato**: Vagas (lista + filtro por modalidade + busca por título; só mostra
  `status === 'aberta'`), Detalhe da vaga + candidatura (mock), Sucesso pós-candidatura, Meu
  currículo (edição — real), Minhas candidaturas (mock), Documentos (envio, mock)
- **Painel do RH**: Gerenciar vagas (lista + criar/editar via dialog + encerrar — real; abas
  Abertas/Rascunhos/Encerradas/Todas), Candidatos por vaga (mock, com "banner" de triagem por IA —
  visual apenas, sem IA real), Documentos (revisão aprovar/rejeitar, mock)

## O que ainda NÃO existe no frontend

- Triagem por IA é só um elemento visual estático, sem chamada nenhuma
- Recuperação de senha (RF03) não tem tela
- Área administrativa (perfil `administrador`) — login mostra mensagem "ainda não disponível" e para aí
- Fluxo de "publicar rascunho" — uma vaga criada pelo diálogo "Nova vaga" já nasce com
  `status: 'aberta'`; não há UI para criar como rascunho e publicar depois
- Sessão persistida entre reloads — um F5 sempre volta para a tela de login

## Próximo passo natural

Ligar candidatura, documentos e notificações assim que o backend implementar esses endpoints,
seguindo o mesmo padrão usado para Vaga/Currículo (composição em `portal-service.ts`, tipos
alinhados aos DTOs reais).
