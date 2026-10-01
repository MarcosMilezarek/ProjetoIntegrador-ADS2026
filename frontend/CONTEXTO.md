# Contexto do Frontend

> Histórico do que já foi desenvolvido e em que etapa o frontend está. Atualize este arquivo
> sempre que uma feature nova for concluída, para quem retomar o trabalho (humano ou IA) não
> precisar reconstruir o contexto do zero. Última atualização: 2026-10-01.

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
  tela inteira; encerrar vaga pede confirmação; celular tem barra de abas inferior e tabelas viram fichas.

## Stack

- React 19 + TypeScript + Vite
- Tailwind CSS 4 + shadcn/ui (componentes em `src/components/ui/`, baseados em Radix)
- Sem gerenciador de estado externo (só `useState`/`useEffect` locais em `App.tsx`)
- `npm install && npm run dev` (ver `frontend/README.md`)

## Estado atual: tudo ligado ao backend real, exceto notificações (2026-10-01)

O frontend inteiro está em um único arquivo, [`src/App.tsx`](src/App.tsx), com todas as telas do
Portal do Candidato e do Painel do RH. A API é a descrita em `backend/CONTEXTO.md` (seção "Rotas",
com papel e regra de propriedade de cada endpoint).

> **Publicação:** esta versão envia o token JWT em toda chamada. O backend com JWT (commits
> `f09ceff` a `e952761`) e este frontend precisam ir para a VPS juntos: com o backend novo e o
> frontend antigo o site para (tudo responde 401), e o contrário também não funciona.

### Sessão (JWT)
- `POST /auth/login` devolve `{id, nome, email, perfil, token}`. O token fica só em memória
  (`setAuthToken` em [`src/lib/api-client.ts`](src/lib/api-client.ts)); recarregar a página volta ao
  login, como antes. O `apiClient` manda `Authorization: Bearer <token>` em toda chamada. O login e
  o cadastro (`auth-service.ts`) continuam sem token.
- **401** em qualquer chamada autenticada: o `apiClient` chama o handler registrado com
  `setUnauthorizedHandler`, o `App` encerra a sessão (`signOut`) e o login mostra "Sua sessão
  expirou. Entre novamente." Um 401 que chega depois de o usuário já ter saído é ignorado.
- **403**: não desloga. Ações mostram a `mensagem` do backend no aviso (toast); o carregamento dos
  inscritos mostra a mensagem na própria tela, com "Tentar novamente".
- Perfis: `candidato` entra no portal; `rh` e `administrador` entram no painel do RH. O
  administrador tem a aba extra **Configurações** (esconder a aba é só conveniência: o backend
  responde 403 a quem não é administrador).
- O corpo das requisições não leva mais `usuarioId` (currículo) nem `rhId` (vaga): quem age vem do token.
- O `refresh` do `App` carrega por papel. Candidato: vagas, currículo, candidaturas, documentos e
  notificações. RH e administrador: só vagas e documentos (currículo e `/candidaturas/minhas`
  respondem 403 para eles). Os inscritos são carregados na tela de candidatos e os usuários na
  tela de Configurações. As telas de candidaturas e documentos recarregam os dados ao abrir.

### O que está ligado ao backend real
Tudo passa por [`src/services/rest-portal-service.ts`](src/services/rest-portal-service.ts).
- **Login e cadastro**: `authService` (`POST /auth/login`, `POST /usuarios`). Sempre usam
  `VITE_API_URL`, independente da flag de mock.
- **Vagas**: `GET/POST/PUT /vagas` (não existe `DELETE`; "encerrar" é um `PUT` com
  `status: 'encerrada'`). O backend filtra: o RH vê só as suas, o administrador vê todas.
- **Currículo**: `GET /curriculos/usuario/{id}` (404 vira "ainda sem currículo"), `POST /curriculos`,
  `PUT /curriculos/{id}` (usa o id do currículo), PDF em `POST/GET /curriculos/{id}/arquivo`.
  Listas de experiências e formações, contato, dados pessoais e certificações; erros 400 por campo
  voltam marcados na tela.
- **Candidatura (candidato)**: `apply` é `POST /candidaturas {vagaId}`; `getApplications` é
  `GET /candidaturas/minhas` (traz `vagaTitulo` e `vagaStatus`, então a lista não depende de achar a
  vaga em `jobs`). Os 409 (vaga fechada, sem currículo, candidatura repetida) aparecem como aviso.
- **Etapas**: mapa backend para tela em `rest-portal-service.ts` (`statusFromApi`/`statusToApi`):
  inscrito=Inscrito, em_triagem=Em análise, entrevista=Entrevista, aprovado=Aprovado,
  reprovado=Não selecionado, contratado=Contratado, cancelado=Cancelado. Contratado e Cancelado
  só vêm da API (o painel não os oferece) e a tela os exibe normalmente.
- **Painel do RH, candidatos por vaga**: `getCandidates` é `GET /vagas/{id}/candidaturas`;
  `updateApplicationStatus` é `PUT /candidaturas/{id}/status`. O menu oferece Ver currículo, Mover
  para análise, Chamar para entrevista, Aprovar e Não selecionar (com confirmação); a opção igual à
  etapa atual fica desabilitada. "Ver currículo" abre um diálogo somente leitura
  (`GET /curriculos/usuario/{candidatoId}`) com botão para baixar o PDF.
- **Documentos por candidato**: não há lista de documentos pedidos nem revisão; cada documento é um
  arquivo enviado pelo candidato numa candidatura `aprovado` ou `contratado`.
  Candidato: `GET /documentos` (os seus) e formulário de envio (`POST /candidaturas/{id}/documentos`,
  multipart `arquivo` e `tipo`; PDF ou DOCX, 5 MB, tipo livre com sugestões). RH e administrador:
  `GET /documentos` agrupado por candidato, com Baixar. O botão "Meus documentos" na tela de
  candidaturas abre a tela do candidato mesmo sem aprovação (estado vazio explica a regra).
- **Downloads** (`GET /documentos/{id}/arquivo`, `GET /curriculos/{id}/arquivo`): como blob pelo
  `apiClient` (um link direto não leva o token). O nome do arquivo é montado no cliente
  (`${tipo}.${formato}` no documento, `arquivo.nomeOriginal` no currículo).
- **Configurações (administrador)**: `GET /usuarios` (a tela mostra só `rh` e `administrador`),
  `POST /usuarios` com token (perfil `rh` ou `administrador`, senha de no mínimo 6),
  `PUT /usuarios/{id}` (editar, trocar perfil e status, bloquear e desbloquear),
  `DELETE /usuarios/{id}` (com confirmação; 409 quando há vagas ou candidaturas, e a mensagem do
  backend sugere bloquear). Na linha do próprio administrador, perfil, status, bloquear e excluir
  ficam desabilitados (o backend responde 403). A tela avisa que mudanças de perfil e status só
  valem no próximo login da pessoa (o token dura 8 horas).

### O que ainda roda 100% mockado (localStorage, sem tocar o backend)
- **Notificações** (`getNotifications`, `markNotificationsRead`). O sino do candidato mostra dados
  de exemplo; o backend ainda não tem o endpoint.

### Modo mock
[`src/services/portal-service.ts`](src/services/portal-service.ts) exporta `portalService`: com
`VITE_USE_MOCK_API=false` os métodos da API real substituem os do mock; sem a flag (padrão) tudo é
mock, com as mesmas assinaturas. O mock guarda tudo em `localStorage` (chave
`vagas-plus-mock-db-v2`) e inclui candidaturas, documentos e usuários de exemplo. O login e o
cadastro sempre falam com a API, então até o mock precisa de um backend para entrar.

### Como testar localmente
- `frontend/.env.development` (versionado) aponta `VITE_API_URL=/api`, que o Vite repassa para a
  VPS (`vite.config.ts`). Enquanto a VPS não tiver o backend com JWT, teste com um backend local:
  crie `frontend/.env.development.local` (ignorado pelo git) com
  `VITE_API_URL=http://localhost:8081/api`.
- O backend local precisa de um MySQL próprio com o schema do Flyway e o `database/seed.sql`
  (senha dos usuários de teste `senha123`, e-mails `@exemplo.test`), e de `JWT_SECRET`. O CORS
  padrão do backend libera `http://localhost:5173`.
- Para a candidata Ana aparecer aprovada na vaga Backend Java (o seed a deixa em entrevista), mude
  a etapa dela pelo painel do RH (rita.rh) ou por `PUT /candidaturas/1/status`.

## Telas implementadas (todas em `App.tsx`)

- **Login** / **Signup**: reais.
- **Portal do candidato**: Vagas (lista, filtro por modalidade e busca; só `aberta`), Detalhe da vaga
  e candidatura, Sucesso, Meu currículo, Minhas candidaturas (status real) e Documentos (envio e
  lista dos próprios).
- **Painel do RH**: Vagas (criar, editar, encerrar), Candidatos por vaga (inscritos reais, etapas,
  currículo), Documentos agrupados por candidato e, só para o administrador, Configurações.

## O que ainda NÃO existe no frontend

- Triagem por IA (RF15): o backend não tem análise real. A coluna Aderência e o aviso de triagem
  saíram da tela de candidatos; voltam quando existir. Um texto do passo a passo da tela
  "Candidatura enviada" ainda cita "triagem assistida" e deveria ser revisto.
- Recuperação de senha (RF03) não tem tela.
- Solicitação de documentos pelo RH (RF14) e revisão (aprovar ou pedir ajuste) de documento.
- Atalho `GET /documentos?candidatoId=` a partir da tela de candidatos: o endpoint existe, mas a tela
  de documentos já agrupa por candidato e não usa o filtro.
- Fluxo de "publicar rascunho": uma vaga criada pelo diálogo "Nova vaga" já nasce com
  `status: 'aberta'`.
- Sessão persistida entre reloads: um F5 sempre volta para a tela de login.
- Renovação do token: ao expirar (8 horas), a pessoa entra de novo.
