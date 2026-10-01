# Contexto do Backend

> Histórico do que já foi desenvolvido e em que etapa o backend está. Atualize este arquivo
> sempre que uma feature nova for concluída, para quem retomar o trabalho (humano ou IA) não
> precisar reconstruir o contexto do zero. Última atualização: 2026-10-01.

## Stack

- Spring Boot 4.1.0 / Spring Framework 7 / Java 21 (roda em JVM 25 em desenvolvimento)
- Maven Wrapper (`./mvnw.cmd` no Windows, `./mvnw` no Git Bash)
- MySQL local na VPS (ver "Infraestrutura / Deploy" abaixo) em produção, H2 em memória nos testes
- Flyway para migrations (`src/main/resources/db/migration`)
- MapStruct para conversão entity → DTO
- Lombok (`@Getter`)
- Spring Security como resource server OAuth2: JWT HS256 emitido pelo próprio backend (Nimbus); senha com BCrypt

## Padrão arquitetural (seguir em toda feature nova)

É um monólito modular, não microserviços físicos (ver `modelagem/arquitetura.md`). Desde 2026-09-30 os
pacotes são organizados **por domínio, com as camadas dentro de cada domínio**:

```
com.rh.recrutamento.backend
  auth/         login, JWT, SegurancaConfig (regras por papel), UsuarioLogado
  usuario/      contas de acesso e Configurações (gestão de usuários pelo administrador)
  vaga/         vagas
  curriculo/    dados do candidato (currículo, formação, experiência, PDF)
  candidatura/  inscrição em vaga, etapas do processo seletivo e histórico
  documento/    documentos de contratação
  notificacao/  notificações em tempo real (SSE) e guardadas no banco
  comum/        erros (GlobalExceptionHandler), CORS, armazenamento de arquivos em disco
```

Dentro de cada domínio, a mesma cadeia de camadas (ex.: `usuario/controller/UsuarioController.java`):

```
entity → repository → dto/{request,response} → mapper (MapStruct) → service → controller
```

Erros de negócio são subclasses de `NegocioException` e centralizados em `GlobalExceptionHandler`
(`@RestControllerAdvice`), cada uma mapeada para um HTTP status (404/409/401/403 etc.).

Testes: unitário de service com Mockito (mapper real, `*MapperImpl`, não mockado) +
`@WebMvcTest`/`MockMvc` para o controller. Toda feature nova deve sair com os dois. Os testes seguem os
mesmos pacotes; os de integração (`integracao/`, sem mocks e com JWT real) cruzam domínios. Para
autenticar num `@WebMvcTest`, importe `SegurancaConfig` e `CorsConfig` e use os atalhos de
`Autenticacao` (`comoCandidato`, `comoRh`, `comoAdministrador`).

## O que já foi desenvolvido

### Autenticação, autorização e usuários (RF01, RF02; RF03 pendente)
- `Usuario`: nome, email, senhaHash (BCrypt), perfil (`candidato`/`rh`/`administrador`), status (`ativo`/`inativo`/`bloqueado`)
- **JWT stateless** (2026-09-30): `POST /api/auth/login` devolve `{id, nome, email, perfil, token}`. O token (HS256, validade de 8h) leva o id do usuário em `sub` e o perfil na claim `perfil`, que vira o papel `ROLE_<perfil>`. As demais rotas exigem `Authorization: Bearer <token>`; sem token ou com token inválido ou expirado, 401.
- **A identidade vem do token, nunca do corpo**: `rhId` (vagas) e `usuarioId` (currículo) saíram das requisições. Nos controllers, `@AuthenticationPrincipal Jwt` vira `UsuarioLogado(id, perfil)`, que é passado aos services.
- **Duas camadas de regra**: o papel é imposto no filtro (`SegurancaConfig`); a propriedade (cada um só vê o que é seu) é verificada nos services, que respondem `AcessoNegadoException` (403).
- Perfil e status vão no token: uma mudança feita em Configurações só vale no próximo login (ou quando o token expirar).
- `JWT_SECRET` é obrigatório (mínimo 32 caracteres) e não tem valor padrão: sem ele a aplicação não sobe.
- O administrador tem os poderes do RH (vagas e candidatos de todas as vagas) e, além disso, as Configurações.
- RF03 (recuperar senha) **não foi implementado**.

#### Rotas (prefixo `/api`)

| Rota | Acesso | Papéis | Regra de propriedade |
| --- | --- | --- | --- |
| `POST /auth/login` | pública | todos | não se aplica |
| `GET /auth/me` | protegida | todos | devolve o próprio usuário (restaura a sessão ao recarregar a página) |
| `POST /usuarios` | pública | todos | sem token de administrador só cria `candidato`; o administrador cria qualquer perfil |
| `GET /usuarios` | protegida | administrador | não se aplica |
| `GET /usuarios/{id}` | protegida | todos | o próprio usuário ou o administrador |
| `PUT /usuarios/{id}` | protegida | administrador | não altera o próprio perfil ou status |
| `DELETE /usuarios/{id}` | protegida | administrador | não exclui a própria conta; com vínculos, 409 |
| `GET /vagas` | protegida | todos | candidato: tudo menos rascunho; RH: só as suas; administrador: todas |
| `GET /vagas/{id}` | protegida | todos | candidato: rascunho responde 404; RH: só as suas (403); administrador: todas |
| `POST /vagas` | protegida | rh, administrador | o responsável é quem está logado |
| `PUT /vagas/{id}` | protegida | rh, administrador | RH só as suas |
| `POST /curriculos` | protegida | candidato | o dono é quem está logado |
| `GET /curriculos/{id}`, `GET /curriculos/usuario/{usuarioId}`, `GET /curriculos/{id}/arquivo` | protegida | todos | o dono; o RH de uma vaga em que o candidato se inscreveu; o administrador |
| `PUT /curriculos/{id}`, `POST /curriculos/{id}/arquivo` | protegida | candidato | só o dono |
| `POST /candidaturas` | protegida | candidato | o candidato é quem está logado |
| `GET /candidaturas/minhas` | protegida | candidato | só as próprias |
| `GET /vagas/{vagaId}/candidaturas` | protegida | rh, administrador | RH responsável pela vaga |
| `PUT /candidaturas/{id}/status` | protegida | rh, administrador | RH responsável pela vaga |
| `PUT /candidaturas/{id}/entrevista` | protegida | rh, administrador | RH responsável pela vaga |
| `POST /candidaturas/{id}/documentos` | protegida | candidato | só na própria candidatura |
| `GET /candidaturas/{id}/documentos` | protegida | todos | o dono; o RH da vaga; o administrador |
| `GET /documentos/tipos` | protegida | todos | não se aplica |
| `GET /documentos` | protegida | todos | candidato: os seus; RH: os dos candidatos das suas vagas; administrador: todos |
| `GET /documentos/{id}/arquivo` | protegida | todos | o dono; o RH da vaga; o administrador |
| `GET /notificacoes`, `GET /notificacoes/stream` | protegida | todos | só as próprias (o destinatário vem do token) |
| `PUT /notificacoes/lidas`, `PUT /notificacoes/{id}/lida` | protegida | todos | só as próprias |

### Vagas — RF10, RF11, RN02
- `Vaga` com `@ManyToOne` para `Usuario rh`
- `VagaController` — `POST/GET/GET-{id}/PUT` em `/api/vagas` (sem DELETE: RN05 exige manter histórico de vaga encerrada; "excluir" = `PUT` com `status=encerrada`)
- RN02: só `rh` e `administrador` criam e editam vagas (filtro de segurança). O responsável é quem está logado; o service ainda confere o perfil no banco (um token anterior a uma troca de perfil gera `RhInvalidoException`, 403). O RH só vê e edita as próprias vagas (RN07)

### Currículo — RF04, RF05 (reestruturado em 2026-09-29, migration V2)
- **A tabela `candidato` deixou de existir.** Eram três tabelas 1:1 para a mesma pessoa (`usuario` → `candidato` → `curriculo`) e os nomes não se distinguiam. Hoje: **`usuario` = conta de acesso, `curriculo` = dados do candidato**, com FK direta para `usuario`. `candidatura.candidato_id` virou `candidatura.usuario_id`.
- `Curriculo`: dados pessoais (data de nascimento, sexo, cidade, UF), contato (`numeroContato`, `perfilLinkedin`), `competencias`, `certificacoes` (opcional, fora da formação acadêmica) e `resumo`
- `CurriculoFormacao` (1:N): curso, instituição, data de início, data de término (nula = curso em andamento)
- `CurriculoExperiencia` (1:N): cargo, empresa, data de contratação, data de demissão, `trabalhoAtual`, descrição das atividades. **Quando `trabalhoAtual` é `true`, a data de demissão é gravada como nula** — normalizada no construtor da entidade e garantida no banco pela constraint `chk_experiencia_trabalho_atual`
- `CurriculoArquivo` (1:1): metadados do PDF. O binário fica em disco (`app.upload.dir`, padrão `./uploads`), com nome gerado por UUID — o nome enviado pelo cliente nunca entra no caminho do arquivo. Limite de 5MB e somente `application/pdf`
- **`idade` não é persistida**: é derivada de `dataNascimento` no mapper, para não desatualizar com o tempo
- `CurriculoController` — `POST /api/curriculos`, `GET /api/curriculos/{id}`, `GET /api/curriculos/usuario/{usuarioId}`, `PUT /api/curriculos/{id}`, `POST /api/curriculos/{id}/arquivo` (multipart, campo `arquivo`), `GET /api/curriculos/{id}/arquivo` (download). Sem DELETE e sem listagem geral — currículo é dado pessoal
- No `PUT`, as listas de formação e experiência **substituem** as atuais (o cliente envia o estado final)
- Regra 1:1 (um currículo por candidato) validada com `CurriculoJaExisteException` (409); o dono é o candidato logado, conferido no banco (sem `perfil=candidato`, `CandidatoInvalidoException`, 403); arquivo inválido gera `ArquivoInvalidoException` (400) e estouro do limite de multipart, 413
- **Bug corrigido junto**: salvar o primeiro currículo de um candidato retornava 500 (`AssertionFailure: null identifier`). `Candidato` tinha id atribuído, então o `save()` do Spring Data caía em `merge()` em vez de `persist()`, e o merge de uma entidade `@MapsId` nova quebra dentro do Hibernate. Com a remoção de `Candidato` o problema deixou de existir; `CurriculoIntegracaoTest` cobre a regressão sem mocks

### Candidatura e painel do RH (RF07, RF08, RF12, RF13; RN01, RN05, RN06, RN07)
- Usa as tabelas `candidatura` e `historico_status` que já existiam (sem migration).
- `POST /api/candidaturas` `{vagaId}`: só vaga `aberta` (RN05), só com currículo cadastrado (UC04) e uma vez por vaga (RN01); as violações respondem 409. Grava o primeiro registro do histórico (`inscrito`).
- `GET /api/candidaturas/minhas`: o candidato acompanha as suas (RN06). Cada item traz a vaga (título e status) e o status da candidatura.
- `GET /api/vagas/{vagaId}/candidaturas`: inscritos da vaga, com nome e e-mail do candidato. O currículo de cada um sai de `GET /api/curriculos/usuario/{candidatoId}`.
- `PUT /api/candidaturas/{id}/status` `{status, observacao?}`: muda a etapa e grava em `historico_status` quem mudou, de onde para onde e a observação (mudar para o mesmo status não gera histórico).
- **Etapas** (enum que já existia no banco): `inscrito`, `em_triagem`, `entrevista`, `aprovado`, `reprovado`, `contratado`, `cancelado`. A ordem não é imposta, para o RH poder corrigir um passo; o histórico guarda a trilha.
- **Agendamento de entrevista** (2026-10-01, migration V4): `PUT /api/candidaturas/{id}/entrevista` `{dataHora}` em ISO 8601 com fuso (ex. `2026-10-15T14:30:00-03:00`). Precisa ser no futuro (senão 400). Grava em `candidatura.entrevista_em` **em UTC**, leva a candidatura para `entrevista` pelo mesmo fluxo de etapa (com histórico) e notifica o candidato com a data e a hora no horário de Brasília (`America/Sao_Paulo`). As respostas de candidatura trazem `entrevistaEm` em UTC (ex. `2026-10-15T17:30:00Z`); o frontend converte para exibir. Não há reagendamento nem cancelamento próprios: chamar de novo grava a data nova.
- Candidatar avisa o RH da vaga; mudar a etapa avisa o candidato (ver Notificações).

### Documentos de contratação (RF09; RN03, RN06, RN07, RN08)
- `arquivo_url` guarda o **nome gerado em disco** (`$APP_UPLOAD_DIR/documento/<uuid>.<pdf|docx>`), não uma URL pública: o download sempre passa pela API.
- **Lista fechada de tipos** (2026-10-01, migration V5): enum `Documento.Tipo`, exposto em `GET /api/documentos/tipos` (`codigo`, `nome`, `obrigatorio`, `condicao`). Obrigatórios: RG, CPF, CTPS, título de eleitor, comprovante de residência, comprovante de escolaridade, foto 3x4, PIS ou PASEP, certidão de nascimento ou casamento e dados bancários. Condicional: certificado de reservista (homens de 18 a 45 anos), que não entra no total exigido.
- `POST /api/candidaturas/{id}/documentos` (multipart: `arquivo` e `tipo` com o **código** da lista, ex. `rg`): só o dono da candidatura e só com status `aprovado` ou `contratado` (RN03, senão 409); tipo fora da lista, 400; PDF ou DOCX até 5MB (RN08, senão 400). **Um por tipo em cada candidatura** (`uk_documento_candidatura_tipo`): reenviar o mesmo tipo substitui o arquivo anterior.
- `GET /api/candidaturas/{id}/documentos`: quadro com `enviados`, `pendentes`, `enviadosExigidos` e `totalExigidos`. O candidato e o RH da vaga veem o mesmo quadro.
- Na saída, `tipo` é o nome para exibir e `tipoCodigo` o código da lista. Envios antigos de texto livre: a V5 guardou o texto em `tipo_informado` e preencheu `tipo` quando havia correspondência; o que não correspondeu ficou com `tipo` nulo (`tipoCodigo` nulo na API) e continua listado em `enviados`.
- `GET /api/documentos[?candidatoId=]`: separado por candidato. O candidato vê os seus; o RH vê os dos candidatos das suas vagas; o administrador vê todos. Cada item traz `candidatoId`, `candidatoNome` e a vaga, para o frontend agrupar por candidato.
- `GET /api/documentos/{id}/arquivo`: download para o dono, o RH da vaga ou o administrador.
- O armazenamento em disco (`comum/service/ArquivoStorage`) é o mesmo do PDF do currículo, uma pasta por domínio; arquivo ausente em disco responde 404.

### Notificações em tempo real (2026-10-01, migration V3)
- Tabela `notificacao` (destinatário, título, mensagem, lida, data). Geradas pelo backend: candidatura nova avisa o RH da vaga; documento enviado avisa o RH da vaga; mudança de etapa e entrevista agendada avisam o candidato.
- Entrega por **SSE**: `GET /api/notificacoes/stream` (`text/event-stream`). Evento `conectado` ao abrir, evento `notificacao` (JSON igual ao da listagem) a cada aviso e um comentário `:ping` a cada 25s para o nginx não cortar a conexão. A notificação só é enviada depois do commit da ação.
- **O token vai no cabeçalho `Authorization`, nunca na URL**: o frontend lê o stream com `fetch` (não com `EventSource`, que não manda cabeçalho). A conexão fecha quando o token expira; o cliente reconecta e, com token vencido, recebe 401.
- As conexões ficam em memória (`NotificacaoService`), uma por aba. Serve para uma instância só do backend, que é o caso da VPS.
- `GET /api/notificacoes` lista as próprias (mais novas primeiro); `PUT /api/notificacoes/{id}/lida` marca uma; `PUT /api/notificacoes/lidas` marca todas. O contador de não lidas é calculado no cliente.

### Configurações (administrador)
- Gestão de usuários com os endpoints de `/api/usuarios`, restritos ao administrador no backend: cadastrar (`POST`, qualquer perfil), listar (`GET`), alterar dados, perfil e status (`PUT`) e excluir (`DELETE`).
- "Permissões" = perfil (`rh` ou `administrador`) e status (`ativo`, `inativo`, `bloqueado`). Não há tabela de permissões finas.
- Proteções: o administrador não altera o próprio perfil ou status nem exclui a própria conta (evita ficar sem ninguém para gerir usuários). Excluir usuário com vagas ou candidaturas responde 409; o caminho é bloquear.

## O que ainda NÃO existe

- Solicitação de documentos pelo RH (RF14) e revisão (aprovar ou recusar) de documento enviado
- Triagem assistida por IA (RF15): integração com OpenRouter prevista em `modelagem/arquitetura.md`
- Relatórios básicos (RF16)
- Recuperação de senha (RF03)
- Revogação de token (logout no servidor): o JWT vale até expirar

## Infraestrutura / Deploy (VPS Google Cloud)

Site em produção: **https://upteam.duckdns.org/** (API em `/api`, ex. `https://upteam.duckdns.org/api/vagas`)

- **VM**: `cloudvmads`, zona `southamerica-east1-c`, projeto `project-9558c67f-ba71-45f5-82b`, IP estático `35.215.251.22`
- **SSH**: `gcloud compute ssh --zone "southamerica-east1-c" cloudvmads --project "project-9558c67f-ba71-45f5-82b"` (precisa do Google Cloud SDK instalado e autenticado — `gcloud auth login`)
- **Domínio**: DuckDNS (`upteam.duckdns.org` apontando pro IP estático; antes era `piads2026-rh.duckdns.org`, que deixou de existir em 2026-09-29) + certificado Let's Encrypt via Certbot (renovação automática). Ao trocar de domínio: ajustar `server_name` em `/etc/nginx/sites-available/app`, emitir o certificado (`certbot --nginx -d <dominio> --redirect`) e atualizar `CORS_ALLOWED_ORIGINS` no `backend.service` — sem o CORS o cadastro no site dá 403
- **Serviços na VM**:
  - MySQL local (só aceita conexão de `localhost`/`127.0.0.1`, não exposto na internet) — banco `selecao_rh`, usuário `appuser`
  - Backend: serviço systemd `backend` (`/etc/systemd/system/backend.service`), roda o jar de `/opt/app/backend/target/backend-0.0.1-SNAPSHOT.jar` com `--spring.profiles.active=local`, reinicia sozinho se cair
  - Frontend: build estático (`npm run build`) em `/opt/app/frontend/dist`, servido pelo nginx
  - Nginx: serve o frontend e faz proxy de `/api/` pro backend (porta 8080 interna); HTTP redireciona pra HTTPS. Config em `/etc/nginx/sites-available/app` (fora do git). O `location /api/` tem `client_max_body_size 6m;` (o padrão de 1MB do nginx barraria o upload de PDF de até 5MB com 413) — se recriar a VM, reaplicar
- **Credenciais do banco na VM**: em `/opt/app/backend/src/main/resources/application-local.properties` (gitignored, carregado via `spring.config.import=optional:classpath:application-local.properties`) — **atenção**: por ser carregado via `classpath:`, esse arquivo fica embutido dentro do `.jar`; editar o arquivo sozinho não basta, é preciso rebuildar (`./mvnw clean package`) pra pegar a mudança
- **CORS de produção**: variável de ambiente `CORS_ALLOWED_ORIGINS` setada no `backend.service` (`Environment=`), não no arquivo de properties — hoje só tem `https://upteam.duckdns.org`
- **Uploads (PDF do currículo)**: variável `APP_UPLOAD_DIR` no `backend.service`, apontando para `/opt/app/uploads` (fora do diretório do build, para o rebuild não apagar os arquivos). Sem essa variável o padrão é `./uploads`, relativo ao diretório de trabalho do serviço
- **Documentos de contratação**: mesma variável, subpasta `documento` (`/opt/app/uploads/documento`)
- **JWT**: variável `JWT_SECRET` no `backend.service` (`Environment=JWT_SECRET=...`; gere com `openssl rand -base64 48`). Obrigatória desde 2026-09-30: sem ela o backend não sobe. Trocar o segredo invalida todos os tokens emitidos (todos precisam logar de novo)

### Workflow de deploy

**As alterações no servidor só devem acontecer depois que o código for enviado (`git push`) para o GitHub** — a VM sempre atualiza a partir do repositório remoto (`https://github.com/MarcosMilezarek/ProjetoIntegrador-ADS2026`, público), nunca de arquivos copiados diretamente do PC local.

Sequência pra atualizar a VM após um push:

```bash
gcloud compute ssh --zone "southamerica-east1-c" cloudvmads --project "project-9558c67f-ba71-45f5-82b" --command "cd /opt/app && git pull && cd backend && ./mvnw -q -DskipTests clean package && sudo systemctl restart backend && cd ../frontend && npm install --no-audit --no-fund && npm run build"
```

Se o histórico do GitHub for reescrito (force push), o `git pull` da VM falha. Nesse caso use `git fetch origin && git reset --hard origin/main` e depois `chmod +x backend/mvnw`, porque o reset devolve o `mvnw` sem permissão de execução (a VM mantém essa permissão só localmente).

## Publicação do JWT

A autenticação JWT foi publicada na VPS em 2026-10-01, junto com o frontend que envia o token. `JWT_SECRET` está no `backend.service`. Antes dessa publicação, o jar e o `dist` anteriores foram guardados em `/opt/backup` (`backend-antes-jwt.jar`, `dist-antes-jwt` e `sha-antes-jwt.txt`), caso seja preciso voltar atrás.

## Como rodar

```bash
cd backend
./mvnw.cmd test          # roda a suíte completa (H2, sem precisar do MySQL)
./mvnw.cmd spring-boot:run
```

A senha do banco real (VPS) fica só em `application-local.properties` (gitignored) — nunca commitar.

**O MySQL do projeto é sempre o da VPS** (não há banco local). Como ele só aceita conexão de
`localhost` na VM, para rodar o backend na sua máquina contra ele é preciso abrir um túnel SSH antes:

```bash
gcloud compute ssh --zone "southamerica-east1-c" cloudvmads --project "project-9558c67f-ba71-45f5-82b" -- -L 3306:localhost:3306
```

**Atenção ao rodar uma migration nova pelo túnel**: o Flyway aplica no banco de produção assim que o backend local sobe. Se o código ainda não foi publicado na VM, o backend de produção passa a rodar com um schema que ele não conhece (foi o caso da V5, que renomeia `documento.tipo`). Teste migrations novas num MySQL descartável e só depois publique.

Com o túnel aberto, `application-local.properties` aponta para `jdbc:mysql://localhost:3306/selecao_rh`
com o usuário/senha do banco da VM. Sem o túnel (ou apontando para um host inexistente) a aplicação
**não sobe**: o Flyway falha no startup ao tentar a conexão.

Também é preciso o segredo do JWT: `JWT_SECRET` no ambiente ou `app.jwt.secret` no
`application-local.properties` (mínimo 32 caracteres). Sem ele a aplicação não sobe.

## Dados de teste (seed)

`database/seed.sql` popula todas as tabelas com dados de teste (5 vagas, 8 usuários, currículos
completos, candidaturas em vários status, documentos, análises de IA e histórico). Senha de todos os
usuários de teste: `senha123`; todos os e-mails terminam em `@exemplo.test`.

Não roda automaticamente — o Flyway não lê esse arquivo, para não injetar dados de teste em produção
sem intenção. Para aplicar:

```bash
mysql -u <usuario> -p selecao_rh < database/seed.sql
```

Pode ser reexecutado: o script começa removendo o seed anterior, com escopo restrito ao domínio
`@exemplo.test` (contas reais não são afetadas). O único ponto de atenção é `curriculo_arquivo`: a
linha aponta para `seed-ana.pdf` em `$APP_UPLOAD_DIR/curriculo/`, que precisa existir em disco para
o download funcionar (o próprio arquivo tem a instrução do `printf`). Os três `documento` do seed
também só referenciam nomes em `$APP_UPLOAD_DIR/documento/`, sem arquivo real: o download deles
responde 404.
