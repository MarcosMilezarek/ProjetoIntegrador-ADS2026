# Contexto do Frontend

> Histórico do que já foi desenvolvido e em que etapa o frontend está. Atualize este arquivo
> sempre que uma feature nova for concluída, para quem retomar o trabalho (humano ou IA) não
> precisar reconstruir o contexto do zero. Última atualização: 2026-10-02.

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

## Estado atual: tudo ligado ao backend real (2026-10-01)

O frontend inteiro está em um único arquivo, [`src/App.tsx`](src/App.tsx), com todas as telas do
Portal do Candidato e do Painel do RH. A API é a descrita em `backend/CONTEXTO.md` (seção "Rotas",
com papel e regra de propriedade de cada endpoint).

> **Publicação:** o JWT já está na VPS. Sessão salva (`GET /auth/me`), notificações em tempo real,
> agendamento de entrevista e tipos de documento dependem das migrations V3 a V5 e das rotas novas
> do backend de 2026-10-01: backend e frontend devem ir juntos. Com o frontend novo e o backend
> antigo, o F5 derruba a sessão (`/auth/me` não existe) e o sino e os documentos falham.

### Sessão (JWT)
- `POST /auth/login` devolve `{id, nome, email, perfil, token}`. O `App` grava
  `{token, id, nome, email, perfil}` em `localStorage`, chave `upteam.sessao`
  (`readSession`/`writeSession` em `App.tsx`), e chama `setAuthToken` em
  [`src/lib/api-client.ts`](src/lib/api-client.ts). O `apiClient` manda `Authorization: Bearer <token>`
  em toda chamada. O login e o cadastro (`auth-service.ts`) continuam sem token.
- **F5 (restaurar a sessão):** se houver sessão salva, o `App` mostra a `LoadingScreen` (nunca o login),
  define o token e chama `GET /auth/me` (`authService.usuarioAtual`). Sucesso: restaura o usuário e a
  tela da URL. 401, 403 ou 404, ou um perfil diferente do salvo: apaga a sessão e volta ao login com
  "Sua sessão expirou. Entre novamente." Falha de rede: a `LoadingScreen` mostra o erro com "Tentar
  novamente" e "Voltar ao login" (a sessão é mantida).
- **Tela na URL (hash):** `#/candidato/{vagas|vaga/{id}|curriculo|candidaturas|documentos}` e
  `#/rh/{vagas|candidatos[/{vagaId}]|agenda|documentos|funcionarios|configuracoes}`. O hash é escrito a cada navegação (o
  botão voltar do navegador funciona) e lido ao restaurar. Hash de outro perfil, inexistente, ou
  `configuracoes` para quem não é administrador: cai na tela inicial do perfil. A tela "Candidatura
  enviada" usa o hash de Candidaturas.
- **401** em qualquer chamada autenticada: o `apiClient` chama o handler registrado com
  `setUnauthorizedHandler`, o `App` encerra a sessão (`signOut`, que também apaga a chave salva e o
  hash) e o login mostra o aviso. Um 401 que chega depois de o usuário já ter saído é ignorado.
- **403**: não desloga. Ações mostram a `mensagem` do backend no aviso (toast); o carregamento dos
  inscritos mostra a mensagem na própria tela, com "Tentar novamente".
- Perfis: `candidato` entra no portal; `rh` e `administrador` entram no painel do RH. O
  administrador tem a aba extra **Configurações** (esconder a aba é só conveniência: o backend
  responde 403 a quem não é administrador).
- O corpo das requisições não leva `usuarioId` (currículo) nem `rhId` (vaga): quem age vem do token.
- O `refresh` do `App` carrega por papel. Candidato: vagas, currículo, candidaturas e notificações.
  RH e administrador: vagas, documentos e notificações (currículo e `/candidaturas/minhas` respondem
  403 para eles). Os inscritos são carregados na tela de candidatos, os usuários na de Configurações e
  os quadros de documentos dentro das telas que os mostram. As telas de candidaturas e documentos
  recarregam os dados ao abrir.

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
  `updateApplicationStatus` é `PUT /candidaturas/{id}/status`. O menu oferece Ver currículo, Ver
  documentos, Mover para análise, Chamar para entrevista (ou Reagendar), Aprovar e Não selecionar (com
  confirmação). "Ver currículo" abre um diálogo somente leitura (`GET /curriculos/usuario/{candidatoId}`)
  com botão para baixar o PDF.
- **Entrevista:** "Chamar para entrevista" abre um diálogo com `datetime-local` (`min` = agora em
  Brasília) e envia `PUT /candidaturas/{id}/entrevista {dataHora: "<valor>:00-03:00"}` (horário de
  Brasília, sem horário de verão). O 400 de `campos.dataHora` aparece no campo. As listas trazem
  `entrevistaEm` em UTC (`interviewAt` nos tipos `Application` e `Candidate`), exibido com
  `formatDateTime` (`Intl.DateTimeFormat` pt-BR, fuso `America/Sao_Paulo`) na tela do candidato e na
  lista do RH. Chamar de novo grava a data nova (o item vira "Reagendar entrevista").
- **Documentos de contratação:** lista fechada de tipos em `GET /documentos/tipos` (`DocumentType`),
  que alimenta o `Select` do formulário; o upload (`POST /candidaturas/{id}/documentos`, multipart
  `arquivo` e `tipo`) leva o **código** do tipo (ex. `rg`), PDF ou DOCX até 5 MB, e reenviar o mesmo
  tipo substitui o anterior. O quadro de cada candidatura aprovada ou contratada vem de
  `GET /candidaturas/{id}/documentos` (`DocumentBoard`): contador "X de Y obrigatórios enviados",
  lista "Enviados" (Baixar e Substituir) e "Pendentes" (selo "Condicional" e o texto da condição);
  envio antigo sem código mostra o selo "tipo antigo" e não tem Substituir. Depois de cada envio o
  quadro recarrega. O RH abre o mesmo quadro (somente leitura) pelo menu do candidato, "Ver documentos".
  A página Documentos do RH e do administrador segue agrupada por candidato, em acordeão (`GET /documentos`).
  O botão "Meus documentos" da tela de candidaturas abre a tela do candidato mesmo sem aprovação (o
  estado vazio explica a regra).
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

### Presença, agenda, revisão de documentos e contratação (2026-10-02)
- **Presença (candidato):** `CandidaturaResponse` traz `presenca` (`pendente`, `confirmado` ou nulo) e
  `presencaConfirmadaEm` (UTC). Em Minhas candidaturas, com a etapa Entrevista e data marcada, `pendente` mostra
  "Confirmar presença" (`PUT /candidaturas/{id}/entrevista/presenca`, e a candidatura na tela vira a da resposta) e
  `confirmado` mostra "Presença confirmada em {data e hora de Brasília}". Remarcar volta para `pendente`.
- **Aba Agenda (RH):** `GET /agenda[?status=pendente|confirmado]`, filtro Todos, Confirmados e Pendentes. É um
  calendário (`Calendar` do shadcn, em `src/components/ui/calendar.tsx`, que usa `react-day-picker` com o locale
  `ptBR`): o dia com entrevista ganha um ponto (amarelo se alguma presença está pendente, verde se todas
  confirmaram) e o dia escolhido lista hora, candidato, vaga e o selo Confirmado ou Pendente. A entrevista é ligada
  ao dia pela data em Brasília (`brasiliaDay`), não pela do navegador. Sem escolha, abre no próximo dia com
  entrevista. Recarrega a cada notificação em tempo real.
- **Aba Documentos (RH):** acordeão (`Accordion` do shadcn, em `src/components/ui/accordion.tsx`, sobre o Radix) com
  um item por candidato: o cabeçalho traz nome, vagas, quantidade de documentos e quantos estão para analisar, e os
  documentos ficam recuados dentro dele. Vários itens podem ficar abertos e o estado aberto sobrevive à revisão.
- **Seletor de vagas (RH, aba Candidatos):** não lista vagas `encerrada`, exceto a que está aberta no momento (aberta
  pelo botão da tela de vagas), para o campo não ficar vazio. Sem vaga escolhida, a aba abre na primeira não encerrada.
- **Menu do topo (RH):** com 5 ou 6 abas, entre 768px e 1239px ele perde os ícones e o nome ao lado do avatar e rola
  na horizontal se ainda faltar espaço (abaixo de 768px vale a barra inferior).
- **Revisão de documentos:** `DocumentoResponse.status` (`pendente`, `aprovado`, `recusado`) vira selo em toda linha de
  documento (candidato e RH). O RH vê Aprovar e Recusar (`PUT /documentos/{id}/aprovar` e `/recusar`) no quadro de "Ver
  documentos" e na aba Documentos. O candidato vê a orientação no arquivo recusado e usa Substituir (reenviar volta a
  `pendente`).
- **Contratar:** item do menu do candidato (`POST /candidaturas/{id}/contratar`). Habilitado só com a candidatura
  `aprovado` e, para cada tipo obrigatório de `GET /documentos/tipos`, um documento da candidatura com status `aprovado`
  (usa os `GET /documentos` que o `App` já carrega). Desabilitado, mostra o motivo no próprio item. O servidor valida de
  novo e a `mensagem` do 409 aparece no aviso. No 201 o candidato sai da lista local (a API também deixou de devolver
  contratados) e a aba Funcionários, que carrega ao abrir, já o mostra.
- **Aba Funcionários (RH):** `GET /funcionarios`, perfil em `GET /funcionarios/{id}` (dados da contratação e documentos
  com selo e download) e Inativar com confirmação (`PUT /funcionarios/{id}/inativar`, troca só a linha na lista).
  `dataContratacao` vem sem fuso; a tela mostra só a data.
- Os selos e as ações novas dependem das rotas novas do backend: com o backend antigo, as abas Agenda e Funcionários
  mostram o erro com "Tentar novamente".

### Notificações (candidato, RH e administrador)
- `GET /notificacoes` (mais novas primeiro) vira `NotificationItem` (`tipo`, `titulo`, `mensagem`, `lida`). As de tipo
  `candidatura` aparecem com tom verde (`data-type` no item); as de `nova_vaga` seguem o estilo padrão. `referenciaId`
  chega da API mas a tela ainda não o usa.
  O sino aparece nos dois painéis; o contador é a quantidade com `read === false`. Fechar o sino
  marca todas (`PUT /notificacoes/lidas`, 204); clicar numa notificação marca só ela
  (`PUT /notificacoes/{id}/lida`). A tela marca na hora e a API confirma depois.
- **Tempo real (SSE):** com sessão ativa, o `App` chama `portalService.subscribeNotifications`, que
  lê `GET /notificacoes/stream` com `fetch` (o `apiClient` devolve a `Response` com `as: 'stream'`),
  com o token no cabeçalho e nunca na URL (não usa `EventSource`). O leitor separa os blocos por linha
  em branco, lê `event:` e `data:` e ignora as linhas que começam com `:` (ping de 25 s). Evento
  `conectado`: recarrega a lista. Evento `notificacao`: insere no topo se o id ainda não existe, sobe
  `liveVersion` (a lista de inscritos e os quadros de documentos abertos recarregam) e chama `refresh()`.
- Reconexão quando o fluxo cai: espera crescente de 1, 2, 5, 10 e 30 s (a última se repete), zerada ao
  receber `conectado`. Resposta 401 para o laço (o `apiClient` já derruba a sessão). Sair da conta
  aborta o fluxo com `AbortController`.

### Modo mock
[`src/services/portal-service.ts`](src/services/portal-service.ts) exporta `portalService`: com
`VITE_USE_MOCK_API=false` os métodos da API real substituem os do mock; sem a flag (padrão) tudo é
mock, com as mesmas assinaturas. O mock guarda tudo em `localStorage` (chave
`vagas-plus-mock-db-v4`, que subiu da v3 porque o formato mudou) e inclui candidaturas, documentos (com a mesma lista
de tipos e status), usuários, funcionários e notificações de exemplo, sem tempo real. Presença, agenda, revisão e
contratação também existem no mock, com as mesmas regras e mensagens de 409. O login e o cadastro sempre falam com a API, então até o
mock precisa de um backend para entrar.

### Como testar localmente
- `frontend/.env.development` (versionado) aponta `VITE_API_URL=/api`, que o Vite repassa para a
  VPS (`vite.config.ts`). Para outro backend, use `frontend/.env.development.local` (ignorado pelo
  git) com `VITE_API_URL=http://localhost:PORTA/api`; uma variável de ambiente do processo também
  vale.
- O backend local precisa de um MySQL com o schema do Flyway (V1 a V5) e o `database/seed.sql`
  (senha dos usuários de teste `senha123`, e-mails `@exemplo.test`), e de `JWT_SECRET`. O CORS
  padrão libera `http://localhost:5173`; outras origens vão em `CORS_ALLOWED_ORIGINS`.
- Para testar notificações em tempo real são necessários dois navegadores (ou perfis) independentes,
  porque a sessão salva fica no `localStorage` da origem: um como RH e outro como candidato.
- O seed deixa a Ana em entrevista na vaga Backend Java; para ela aparecer aprovada (documentos),
  mude a etapa pelo painel do RH (rita.rh) ou por `PUT /candidaturas/1/status`.

## Telas implementadas (todas em `App.tsx`)

- **Login** / **Signup**: reais.
- **Portal do candidato**: Vagas (lista, filtro por modalidade e busca; só `aberta`), Detalhe da vaga
  e candidatura, Sucesso, Meu currículo, Minhas candidaturas (status real e data da entrevista) e
  Documentos (formulário com lista de tipos e quadro de enviados e pendentes por candidatura aprovada).
- **Painel do RH**: Vagas (criar, editar, encerrar), Candidatos por vaga (inscritos reais, etapas,
  entrevista com data e hora, currículo, quadro de documentos), Documentos agrupados por candidato e,
  só para o administrador, Configurações. Sino de notificações em todas as telas.

## O que ainda NÃO existe no frontend

- Triagem por IA (RF15): o backend não tem análise real. A coluna Aderência e o aviso de triagem
  saíram da tela de candidatos; voltam quando existir. Um texto do passo a passo da tela
  "Candidatura enviada" ainda cita "triagem assistida" e deveria ser revisto.
- Recuperação de senha (RF03) não tem tela.
- Solicitação de documentos pelo RH (RF14). A revisão (aprovar ou recusar) já existe, sem campo para o motivo.
- Atalho `GET /documentos?candidatoId=` a partir da tela de candidatos: o endpoint existe, mas as
  telas já agrupam por candidato ou usam o quadro da candidatura.
- Fluxo de "publicar rascunho": uma vaga criada pelo diálogo "Nova vaga" já nasce com
  `status: 'aberta'`.
- Renovação do token: ao expirar (8 horas), a pessoa entra de novo.
- Cancelar entrevista: o backend só reagenda (chamar de novo grava a data nova).
