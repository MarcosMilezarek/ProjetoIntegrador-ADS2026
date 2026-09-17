# Contexto do Frontend

> Histórico do que já foi desenvolvido e em que etapa o frontend está. Atualize este arquivo
> sempre que uma feature nova for concluída, para quem retomar o trabalho (humano ou IA) não
> precisar reconstruir o contexto do zero. Última atualização: 2026-09-17.

## Stack

- React 19 + TypeScript + Vite
- Tailwind CSS 4 + shadcn/ui (componentes em `src/components/ui/`, baseados em Radix)
- Sem gerenciador de estado externo (só `useState`/`useEffect` locais em `App.tsx`)
- `npm install && npm run dev` (ver `frontend/README.md`)

## Estado atual: protótipo funcional de UI, com dados mockados

O frontend inteiro está em um único arquivo, [`src/App.tsx`](src/App.tsx) (~260 linhas densas),
com todas as telas do Portal do Candidato e do Painel do RH. Login e cadastro já falam com o
backend real; **o restante das telas ainda roda sobre dados fictícios em `localStorage`**, não
sobre a API.

### O que já está ligado ao backend real (`http://localhost:8080/api`)
- Tela de login (`Login` em `App.tsx`) → `authService.login` ([`src/types/auth-service.ts`](src/types/auth-service.ts)) → `POST /api/auth/login`
- Tela de cadastro de candidato (`Signup`) → `authService.cadastrar` → `POST /api/usuarios`
- Essas duas chamadas sempre usam `VITE_API_URL` (default `http://localhost:8080/api`), independente da flag de mock abaixo.

### O que ainda roda 100% mockado (localStorage, sem tocar o backend)
Controlado por [`src/services/portal-service.ts`](src/services/portal-service.ts): por padrão
(`VITE_USE_MOCK_API` ausente ou diferente de `"false"`) usa `mockPortalService`, que lê/escreve
em `localStorage` (chave `vagas-plus-mock-db`) com dados de exemplo (`seedJobs`, `seedProfile`,
`seedApplications` etc.). Isso cobre:
- Listagem e detalhe de vagas, candidatar-se
- Currículo do candidato (tela "Meu currículo")
- Minhas candidaturas / acompanhamento de status
- Documentos (upload pelo candidato, revisão pelo RH)
- Notificações
- Painel do RH: CRUD de vagas, listagem de candidatos por vaga, revisão de documentos

### Gap conhecido: `restPortalService` não bate com a API real
Existe um stub em [`src/services/rest-portal-service.ts`](src/services/rest-portal-service.ts)
pensado para logo substituir o mock (ativado com `VITE_USE_MOCK_API=false`), mas ele foi escrito
**antes** do backend de Vaga/Currículo existir e usa rotas/formatos que não existem no backend
atual:

| `rest-portal-service.ts` (hoje) | Backend real |
| --- | --- |
| `GET/POST/PUT/DELETE /jobs` | `GET/POST/PUT /vagas` (sem DELETE, campos diferentes: `VagaRequest`/`VagaResponse`) |
| `GET/PUT /candidate/profile` | `POST/GET/PUT /curriculos` (+ precisa de `usuarioId`, não existe "candidate/profile" implícito por sessão) |
| `/candidate/applications`, `/applications/{id}/status` | não existe ainda no backend (candidatura não foi implementada) |
| `/candidate/documents`, `/documents/{id}/review` | não existe ainda no backend |
| `/jobs/{id}/candidates` | não existe ainda no backend |
| `/notifications` | não existe no backend |

Ou seja: religar o portal ao backend real vai exigir reescrever `rest-portal-service.ts` (rotas,
payloads e também os tipos em `src/types/domain.ts`, que hoje são livres/fictícios e não
correspondem aos DTOs Java) — e só é possível para Vaga e Currículo por enquanto; o resto depende
de o backend ganhar candidatura/documentos/notificações primeiro.

## Telas implementadas (todas em `App.tsx`)

- **Login** / **Signup** — reais (ver acima)
- **Portal do candidato**: Vagas (lista + filtro + busca), Detalhe da vaga + candidatura, Sucesso
  pós-candidatura, Meu currículo (edição), Minhas candidaturas (acompanhamento), Documentos (envio)
- **Painel do RH**: Gerenciar vagas (lista + criar/editar via dialog + excluir), Candidatos por vaga
  (com "banner" de triagem por IA — visual apenas, sem IA real), Documentos (revisão aprovar/rejeitar)

## O que ainda NÃO existe no frontend

- Nenhuma integração real com Vaga/Currículo (só mock, apesar do backend já existir — ver gap acima)
- Triagem por IA é só um elemento visual estático, sem chamada nenhuma
- Recuperação de senha (RF03) não tem tela
- Área administrativa (perfil `administrador`) — login mostra mensagem "ainda não disponível" e para aí

## Próximo passo natural

Ligar `rest-portal-service.ts` às rotas reais de `/vagas` e `/curriculos` (ajustando tipos em
`domain.ts` para bater com `VagaResponse`/`CurriculoResponse`), mantendo o resto em mock até o
backend cobrir candidatura/documentos/notificações.
