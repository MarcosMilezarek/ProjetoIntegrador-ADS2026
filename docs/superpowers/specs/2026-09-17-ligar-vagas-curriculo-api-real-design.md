# Ligar Vagas e Currículo à API real — Design

> Data: 2026-09-17. Autor: frontend-developer (assistido por Claude). Status: aprovado.

## Contexto

O frontend (`frontend/src/App.tsx` + `src/services/portal-service.ts`) hoje roda inteiramente
sobre dados mockados em `localStorage`, exceto login/cadastro. O backend já tem `Vaga` e
`Curriculo` implementados (`/vagas`, `/curriculos`), mas `rest-portal-service.ts` foi escrito
antes desses endpoints existirem e não bate com o formato real. Esta spec cobre ligar **vagas** e
**currículo** à API real, mantendo candidatura, documentos e notificações mockados (o backend
ainda não os implementa).

Ver `frontend/CONTEXTO.md` (gap conhecido) e os DTOs reais em
`backend/src/main/java/.../dto/vaga/**` e `.../dto/curriculo/**`.

## Decisões (confirmadas com o usuário)

1. **Identidade do usuário**: guardar `{id, nome, email, perfil}` do `LoginResponse` em estado do
   `App` (`currentUser`), sem persistir em `localStorage`. Um F5 continua voltando pro login (sem
   mudança de comportamento nesse ponto).
2. **Excluir vaga → Encerrar vaga**: não existe `DELETE /vagas`, só `PUT` com `status=encerrada`.
   A ação "Excluir" do painel do RH vira "Encerrar vaga".
3. **Status da vaga**: `JobStatus` passa a ser `'rascunho' | 'aberta' | 'encerrada'` (os três
   status reais do backend), abandonando o conceito de `'closing'` que não existe.
4. **Campos do currículo sem correspondência no backend** (telefone, cidade, completude do
   perfil): removidos da tela "Meu currículo". Nome e e-mail passam a vir do usuário logado
   (`currentUser`), não do currículo.
5. **Campos da vaga sem correspondência no backend** (`department`, `tags`, `benefits`,
   `applicants`): removidos do tipo `Job` e das telas (lista, detalhe, dialog de criar/editar,
   tabela do RH).

## Decisões adicionais (tomadas durante a exploração do backend, para aprovação)

- `PUT /curriculos/{id}` usa o **id do currículo**, não o `usuarioId`. `GET
  /curriculos/usuario/{id}` retorna 404 para quem ainda não tem currículo. Portanto
  `CandidateProfile` guarda um `id` opcional (id do currículo, não do usuário); `updateProfile`
  decide `POST` (sem id ainda) vs `PUT` (id existente).
- O backend tem um campo `resumo` em `Curriculo` sem nenhuma UI hoje. Adiciono um campo "Resumo
  profissional" (textarea) em "Meu currículo" para não perder esse dado.
- Nova vaga criada pelo diálogo "Nova vaga" é enviada com `status: 'aberta'` (publicada na hora),
  para preservar o comportamento atual — a UI não tem um fluxo de "publicar rascunho" e adicionar
  um não está no escopo pedido. Editar uma vaga existente sempre reenvia o `status` atual (a
  edição não mexe em status; só a ação "Encerrar" muda status).
- `api-client.ts` hoje mostra erros de validação do backend como texto bruto (`{mensagem,
  campos}`). Reaproveito a extração de mensagem que `auth-service.ts` já tem, para erros de
  vaga/currículo aparecerem legíveis.

## Tipos (`src/types/domain.ts`)

```ts
export type JobStatus = 'rascunho' | 'aberta' | 'encerrada';

export type Job = {
  id: string;
  title: string;
  city: string;
  workModel: 'Presencial' | 'Híbrido' | 'Remoto';
  contract: 'CLT' | 'Estágio' | 'PJ' | 'Temporário';
  publishedAt: string;       // criadoEm formatado
  closesAt?: string;         // prazo (opcional)
  status: JobStatus;
  description: string;
  requirements: string[];    // requisitos, uma linha por item (join/split por \n)
};

export type NewJobInput = Omit<Job, 'id' | 'publishedAt' | 'status'>;

export type CandidateProfile = {
  id?: string;           // id do currículo (ausente = ainda não existe)
  education: string;     // formacao
  experience: string;    // experiencias
  skills: string[];      // competencias, join/split por vírgula
  resumo: string;
  updatedAt?: string;    // atualizadoEm formatado
};

export type Candidate = {
  id: string;
  name: string;
  email: string;
  applicationId: string;
  submittedAt: string;
  match: number;
  status: ApplicationStatus;
};
```

`Application`, `CandidateDocument`, `NotificationItem`, `ApplicationStatus`, `DocumentStatus`
permanecem inalterados (continuam mockados).

## Camada de serviço

`PortalService` (interface em `portal-service.ts`):

- `deleteJob(id)` → **`closeJob(id): Promise<Job>`**.
- `getProfile()` → **`getProfile(usuarioId: string): Promise<CandidateProfile>`**.
- `updateProfile(profile)` → **`updateProfile(usuarioId: string, profile: CandidateProfile):
  Promise<CandidateProfile>`**.
- Demais métodos (`getApplications`, `apply`, `getCandidates`, `updateApplicationStatus`,
  `getDocuments`, `uploadDocument`, `reviewDocument`, `getNotifications`,
  `markNotificationsRead`) mantêm assinatura atual.

`mockPortalService`:
- `closeJob`: muda `status` da vaga pra `'encerrada'` em vez de remover do array.
- `getProfile`/`updateProfile`: ignoram `usuarioId` (perfil único mockado, como hoje), mas
  retornam/aceitam o novo formato de `CandidateProfile` (sem nome/email/telefone/cidade). Dados
  internos do mock (nome/email usados em `getCandidates`) passam a viver numa estrutura interna
  própria do arquivo, não mais no tipo público `CandidateProfile`.

`restPortalService` (reescrito):
- `getJobs`: `GET /vagas` → mapeia `VagaResponse[]` para `Job[]`.
- `saveJob`: `POST /vagas` (sem id) ou `PUT /vagas/{id}` (com id, reenviando o `status` atual)
  → mapeia request/response.
- `closeJob`: `GET /vagas/{id}` seguido de `PUT /vagas/{id}` com `status: 'encerrada'` e os
  demais campos inalterados.
- `getProfile(usuarioId)`: `GET /curriculos/usuario/{usuarioId}`; trata 404 como "sem currículo"
  (retorna objeto vazio com `id` ausente).
- `updateProfile(usuarioId, profile)`: `POST /curriculos` (se `profile.id` ausente) ou
  `PUT /curriculos/{profile.id}`.
- Mapeamento de enums: `modalidade` (`presencial|remoto|hibrido`) ↔ `workModel`
  (`Presencial|Remoto|Híbrido`); `tipoContrato` (`clt|pj|estagio|temporario`) ↔ `contract`
  (`CLT|PJ|Estágio|Temporário`); `status` (`rascunho|aberta|encerrada`) é idêntico em ambos os
  lados (mesmos valores, já em minúsculas — sem tradução necessária).

`portalService` (composição, em vez de switch total):

```ts
export const portalService: PortalService = import.meta.env.VITE_USE_MOCK_API === 'false'
  ? { ...mockPortalService, getJobs: restPortalService.getJobs, saveJob: restPortalService.saveJob,
      closeJob: restPortalService.closeJob, getProfile: restPortalService.getProfile,
      updateProfile: restPortalService.updateProfile }
  : mockPortalService;
```

## `api-client.ts`

Extrair a função `mensagemDeErro` (hoje duplicável de `auth-service.ts`) para um lugar
compartilhado, ou replicar a mesma lógica dentro de `apiClient`, para que erros 400 de
vaga/currículo mostrem a mensagem do backend em vez do JSON cru.

## Telas (`App.tsx`)

- **`App`**: novo estado `currentUser: {id, nome, email, perfil} | null`, setado no `onAccess` do
  login. Passado para `ResumePage` (nome/e-mail) e usado nas chamadas de `getProfile`/
  `updateProfile`/`saveJob` (rhId ao criar vaga).
- **`Login`**: `onAccess` passa a receber a conta inteira, não só o `role`.
- **`JobsPage`**: remove filtro de área/departamento; busca só por título; filtra
  `status === 'aberta'`.
- **`JobDetail`**: breadcrumb sem departamento; card lateral sem linha de "Perfil completo".
- **`ResumePage`**: remove campos Telefone/Cidade e o card "Perfil completo"; nome/e-mail vêm de
  `currentUser` (somente leitura); adiciona campo "Resumo profissional".
- **`HrJobsPage`**: nova aba "Rascunhos"; tabela sem colunas Departamento/Candidatos; ação
  "Encerrar vaga" no lugar de "Excluir vaga"; criação de vaga usa `rhId: currentUser.id` e
  `status: 'aberta'`.
- **`JobDialog`**: remove campos Departamento, Tecnologias/palavras-chave e Benefícios.

## Fora de escopo

- Candidatura, documentos e notificações continuam 100% mockados (backend não os implementa
  ainda).
- Nenhuma persistência de sessão entre reloads (F5 volta pro login, como hoje).
- Nenhum fluxo de "publicar rascunho" na UI (vaga nova já nasce `aberta`).
