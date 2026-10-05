# Contexto do Backend

> **Retrato histórico de 2026-10-02.** A fonte de referência do contexto deste projeto passou a ser o vault do
> Obsidian do mantenedor (pasta `Projeto-Integrador`), e este arquivo não é mais atualizado: pode estar defasado.
> Em divergência, vale o código.

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
- As conexões ficam em memória (`NotificacaoService`), uma por aba, **separadas em cada processo**. O backend local e o da VPS usam o mesmo MySQL (o local liga pelo túnel), então uma notificação criada por um deles não passava pela memória do outro e o site não recebia em tempo real o que o backend local gravou (e vice-versa). Descoberto em 2026-10-02: 25 das 51 ações que geraram notificações na VPS não tinham requisição no log do nginx, ou seja, vieram de um backend local pelo túnel.
- **Sincronizador** (`NotificacaoService.sincronizar()`, a cada 1s): cada instância consulta `notificacao` por ids novos (`id > ultimoIdVisto`, cursor iniciado no maior id existente ao subir) e envia aos conectados dela as que **outra** instância gravou. As que a própria instância criou já saem pelo envio direto depois do commit e são puladas (mapa `criadasAqui`, preenchido antes do commit e limpo após 5 min), então nada chega em dobro. Latência: ~60 ms para o que nasce na instância e até ~1s para o que vem de outra. Limitação conhecida: uma linha com id menor que confirma depois de outra com id maior que o sincronizador já viu não é empurrada por ele (só ocorre entre instâncias, com transações concorrentes); ela continua na listagem e entra na ressincronização que o cliente faz ao reconectar.
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

Se o `git pull` da VM reclamar de `frontend/package-lock.json` (o `npm install` de lá reescreve esse arquivo), rode `git checkout -- frontend/package-lock.json` antes do pull; é só um arquivo gerado, sem alteração que valha manter.

## Publicação do JWT

A autenticação JWT foi publicada na VPS em 2026-10-01, junto com o frontend que envia o token. `JWT_SECRET` está no `backend.service`. Antes dessa publicação, o jar e o `dist` anteriores foram guardados em `/opt/backup` (`backend-antes-jwt.jar`, `dist-antes-jwt` e `sha-antes-jwt.txt`), caso seja preciso voltar atrás.

## Publicação das notificações, entrevista e tipos de documento

Publicada na VPS em 2026-10-01 (backend e frontend juntos), com as migrations V3 a V5 aplicadas no banco de produção. Antes, foram guardados em `/opt/backup`: `selecao_rh-antes-v5.sql` (dump do banco), `backend-antes-v5.jar`, `dist-antes-v5`, `uploads-antes-v5.tgz` e `sha-antes-v5.txt`. Para voltar atrás é preciso restaurar o dump junto com o jar, porque o jar antigo não funciona com o schema V5.

## Publicação da presença, agenda, revisão de documentos e funcionários

Publicada na VPS em 2026-10-02 (commits `cf4ef57` do backend e `8df4498` do frontend, migration V6). A V6 já estava aplicada no banco de produção desde a manhã do mesmo dia (13:45 no relógio do servidor), porque o backend local subiu pelo túnel antes da publicação, o caso que o aviso em "Como rodar" descreve. Como ela só adiciona colunas com valor padrão e uma tabela, o backend antigo continuou funcionando nesse intervalo. Antes do deploy foram guardados em `/opt/backup`: `selecao_rh-antes-v6.sql`, `backend-antes-v6.jar`, `dist-antes-v6`, `uploads-antes-v6.tgz` e `sha-antes-v6.txt`. Como o schema já era o V6 quando o backup foi feito, o jar `antes-v6` também roda com ele.

Teste funcional em produção depois do deploy (JWT, perfis, candidatura, entrevista, presença, agenda, revisão de documentos, contratação, funcionário e notificações em tempo real, inclusive o SSE atravessando o nginx): sem falhas. Ficaram no banco os registros de teste `e2e.*` (usuários, a vaga "Vaga E2E v6", a candidatura contratada e o funcionário dela, já inativado).

## Publicação do sincronizador de notificações e do novo seed

Publicada na VPS em 2026-10-02 (commits `51c2cc7` do backend, `bfe031c` do seed e `a8b8698` da documentação). Só o backend foi rebuildado; o frontend não mudou. Antes do deploy foram guardados em `/opt/backup`: `selecao_rh-antes-seed.sql` (dump com os dados de teste anteriores), `backend-antes-sync.jar`, `uploads-antes-seed.tgz` e `sha-antes-sync.txt` (commit anterior da VM). O banco de produção foi zerado (`TRUNCATE` de todas as tabelas, menos `flyway_schema_history`, com os ids reiniciados) e semeado com `database/seed.sql`, com o backend parado; os PDFs de exemplo foram criados por `database/seed-arquivos.sh`. Os arquivos de upload antigos continuam em `/opt/app/uploads` como órfãos (também estão no `tgz` de backup). Para voltar atrás: restaurar o dump (`sudo mysql selecao_rh < /opt/backup/selecao_rh-antes-seed.sql`) e, se preciso, o jar antigo, que roda com o mesmo schema V6.

Validação antes de publicar, numa réplica descartável na própria VM (jar real, banco `selecao_rh_scratch` separado e nginx temporário com a mesma configuração e TLS): o evento chega em ~60 ms por Tomcat, nginx e nginx com TLS, e se mantém por 11 minutos com os pings; o nginx **não** era a causa. A causa foi reproduzida com duas instâncias do backend sobre o mesmo banco (a ação feita pela instância B não chegava ao candidato conectado na A em 6s) e, com o sincronizador, passou a chegar em ~0,7s, sem duplicar. Teste automatizado: `NotificacaoIntegracaoTest` (`notificacaoGravadaPorOutraInstanciaTambemChegaNaConexao` e `notificacaoCriadaAquiNaoChegaEmDobroDepoisDoSincronizador`).

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

**Backend local e site compartilham o banco e, desde 2026-10-02, as notificações em tempo real** (sincronizador de 1s, ver "Notificações em tempo real"). Para o backend local receber também o que o site gravou, ele precisa estar numa versão com o sincronizador (commit `51c2cc7` ou posterior): reinicie-o depois de atualizar.

**Atenção ao rodar uma migration nova pelo túnel**: o Flyway aplica no banco de produção assim que o backend local sobe. Se o código ainda não foi publicado na VM, o backend de produção passa a rodar com um schema que ele não conhece (foi o caso da V5, que renomeia `documento.tipo`). Teste migrations novas num MySQL descartável e só depois publique.

Com o túnel aberto, `application-local.properties` aponta para `jdbc:mysql://localhost:3306/selecao_rh`
com o usuário/senha do banco da VM. Sem o túnel (ou apontando para um host inexistente) a aplicação
**não sobe**: o Flyway falha no startup ao tentar a conexão.

Também é preciso o segredo do JWT: `JWT_SECRET` no ambiente ou `app.jwt.secret` no
`application-local.properties` (mínimo 32 caracteres). Sem ele a aplicação não sobe.

## Dados de teste (seed)

`database/seed.sql` (schema V6) popula todas as tabelas com um **ambiente de escritório**: 1 administrador, 3 RH, 30 candidatos (um sem currículo, um inativo e um bloqueado) e 18 vagas (14 abertas, 2 rascunhos, 2 encerradas) de áreas administrativas: assistente administrativo, analista financeiro, departamento pessoal, recepção, contabilidade, contas a pagar e receber, recrutamento e seleção, estágio, secretária bilíngue, compras, coordenação, comercial, arquivo, fiscal, facilities e gerência. São 52 candidaturas em todas as etapas (16 inscrito, 10 em análise, 8 entrevista, 5 aprovado, 7 reprovado, 4 contratado, 2 cancelado), com histórico de etapas, agenda (entrevistas futuras com presença pendente ou confirmada e uma de ontem), documentos em todas as situações (pendente, aprovado, recusado; um aprovado já com os 10 exigidos aprovados, pronto para "Contratar"), 4 funcionários (3 ativos e 1 inativo), análises de IA e 347 notificações coerentes com cada passo (as dos últimos 4 dias ficam não lidas). Senha de todos os usuários de teste: `senha123`; todos os e-mails terminam em `@exemplo.test` (RH: `rita.rh`, `paulo.rh`, `marina.rh`; administrador: `admin`).

Não roda automaticamente — o Flyway não lê esse arquivo, para não injetar dados de teste em produção
sem intenção. Para aplicar (a conexão precisa ser `utf8mb4`, por causa dos acentos):

```bash
mysql --default-character-set=utf8mb4 -u <usuario> -p selecao_rh < database/seed.sql
APP_UPLOAD_DIR=/opt/app/uploads bash database/seed-arquivos.sh   # PDFs de exemplo (opcional)
```

Pode ser reexecutado: o script começa removendo o seed anterior, com escopo restrito ao domínio
`@exemplo.test` (contas reais não são afetadas). Todas as datas são relativas ao momento da execução
(`@agora`), então a agenda e os prazos fazem sentido no dia em que o seed é aplicado. **Convenção de
horário**: o driver JDBC (`serverTimezone=America/Sao_Paulo`) grava todas as colunas de data e hora em
horário de Brasília, inclusive as que o código trata como UTC (`entrevista_em`, `presenca_confirmada_em`); o
seed segue a mesma convenção (`UTC_TIMESTAMP() - INTERVAL 3 HOUR`) e a API converte na leitura.

`database/seed-arquivos.sh` cria os PDFs de exemplo (15 currículos e 70 documentos) com os nomes que o seed
referencia em `$APP_UPLOAD_DIR/curriculo` e `$APP_UPLOAD_DIR/documento`; sem eles o seed funciona, mas o
download desses arquivos responde 404.

Para **zerar o banco antes de semear** (por exemplo, trocar os dados de teste antigos por estes) não há
script no repositório, de propósito: é uma operação destrutiva e pontual. Faça o dump antes e use
`SET FOREIGN_KEY_CHECKS = 0; TRUNCATE TABLE ...;` em todas as tabelas menos `flyway_schema_history`.
