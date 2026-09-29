# Contexto do Backend

> Histórico do que já foi desenvolvido e em que etapa o backend está. Atualize este arquivo
> sempre que uma feature nova for concluída, para quem retomar o trabalho (humano ou IA) não
> precisar reconstruir o contexto do zero. Última atualização: 2026-09-29.

## Stack

- Spring Boot 4.1.0 / Spring Framework 7 / Java 21 (roda em JVM 25 em desenvolvimento)
- Maven Wrapper (`./mvnw.cmd` no Windows, `./mvnw` no Git Bash)
- MySQL local na VPS (ver "Infraestrutura / Deploy" abaixo) em produção, H2 em memória nos testes
- Flyway para migrations (`src/main/resources/db/migration`)
- MapStruct para conversão entity → DTO
- Lombok (`@Getter`) + `spring-security-crypto` só para o `BCryptPasswordEncoder` (não há Spring Security completo)

## Padrão arquitetural (seguir em toda feature nova)

É um monólito modular, não microserviços físicos (ver `modelagem/arquitetura.md`). Cada feature segue
a mesma cadeia de camadas:

```
entity → repository → dto/<feature>/{request,response} → mapper (MapStruct) → service → controller
```

Erros de negócio são subclasses de `NegocioException` e centralizados em `GlobalExceptionHandler`
(`@RestControllerAdvice`), cada uma mapeada para um HTTP status (404/409/401/403 etc.).

Testes: unitário de service com Mockito (mapper real, `*MapperImpl`, não mockado) +
`@WebMvcTest`/`MockMvc` para o controller. Toda feature nova deve sair com os dois.

## O que já foi desenvolvido

### Autenticação e Usuários — RF01, RF02, RF03 (parcial)
- `Usuario`: nome, email, senhaHash (BCrypt), perfil (`candidato`/`rh`/`administrador`), status (`ativo`/`inativo`/`bloqueado`)
- `AuthController` — `POST /api/auth/login`
- `UsuarioController` — CRUD completo em `/api/usuarios` (POST, GET lista, GET/{id}, PUT, DELETE)
- **Não há sessão/JWT/Spring Security real** — apesar de um commit antigo citar "JWT", só existe o encoder de senha. Todo endpoint está aberto; "quem está fazendo a ação" é sempre passado explícito no corpo da requisição.
- RF03 (recuperar senha) **não foi implementado**.

### Vagas — RF10, RF11, RN02
- `Vaga` com `@ManyToOne` para `Usuario rh`
- `VagaController` — `POST/GET/GET-{id}/PUT` em `/api/vagas` (sem DELETE: RN05 exige manter histórico de vaga encerrada; "excluir" = `PUT` com `status=encerrada`)
- RN02 (só RH gerencia vaga) validada no service: o `rhId` do corpo precisa apontar para um `Usuario` com `perfil=rh`, senão `RhInvalidoException` (403)

### Currículo — RF04, RF05 (reestruturado em 2026-09-29, migration V2)
- **A tabela `candidato` deixou de existir.** Eram três tabelas 1:1 para a mesma pessoa (`usuario` → `candidato` → `curriculo`) e os nomes não se distinguiam. Hoje: **`usuario` = conta de acesso, `curriculo` = dados do candidato**, com FK direta para `usuario`. `candidatura.candidato_id` virou `candidatura.usuario_id`.
- `Curriculo`: dados pessoais (data de nascimento, sexo, cidade, UF), contato (`numeroContato`, `perfilLinkedin`), `competencias`, `certificacoes` (opcional, fora da formação acadêmica) e `resumo`
- `CurriculoFormacao` (1:N): curso, instituição, data de início, data de término (nula = curso em andamento)
- `CurriculoExperiencia` (1:N): cargo, empresa, data de contratação, data de demissão, `trabalhoAtual`, descrição das atividades. **Quando `trabalhoAtual` é `true`, a data de demissão é gravada como nula** — normalizada no construtor da entidade e garantida no banco pela constraint `chk_experiencia_trabalho_atual`
- `CurriculoArquivo` (1:1): metadados do PDF. O binário fica em disco (`app.upload.dir`, padrão `./uploads`), com nome gerado por UUID — o nome enviado pelo cliente nunca entra no caminho do arquivo. Limite de 5MB e somente `application/pdf`
- **`idade` não é persistida**: é derivada de `dataNascimento` no mapper, para não desatualizar com o tempo
- `CurriculoController` — `POST /api/curriculos`, `GET /api/curriculos/{id}`, `GET /api/curriculos/usuario/{usuarioId}`, `PUT /api/curriculos/{id}`, `POST /api/curriculos/{id}/arquivo` (multipart, campo `arquivo`), `GET /api/curriculos/{id}/arquivo` (download). Sem DELETE e sem listagem geral — currículo é dado pessoal
- No `PUT`, as listas de formação e experiência **substituem** as atuais (o cliente envia o estado final)
- Regra 1:1 (um currículo por candidato) validada com `CurriculoJaExisteException` (409); `usuarioId` precisa ser `perfil=candidato`, senão `CandidatoInvalidoException` (403); arquivo inválido gera `ArquivoInvalidoException` (400) e estouro do limite de multipart, 413
- **Bug corrigido junto**: salvar o primeiro currículo de um candidato retornava 500 (`AssertionFailure: null identifier`). `Candidato` tinha id atribuído, então o `save()` do Spring Data caía em `merge()` em vez de `persist()`, e o merge de uma entidade `@MapsId` nova quebra dentro do Hibernate. Com a remoção de `Candidato` o problema deixou de existir; `CurriculoIntegracaoTest` cobre a regressão sem mocks

## O que ainda NÃO existe

- Candidatura a vaga (RF07, RN01, UC04) — vincular candidato a vaga, impedir duplicidade
- Acompanhamento de status da candidatura pelo candidato (RF08)
- Envio/gestão de documentos (RF09, RN03, RNF09 — PDF/DOCX até 5MB)
- Painel do RH: listar candidatos inscritos por vaga (RF12), atualizar status do candidato (RF13), solicitar documentos (RF14)
- Triagem assistida por IA (RF15) — integração com OpenRouter prevista em `modelagem/arquitetura.md`
- Relatórios básicos (RF16)
- Autenticação real (sessão, JWT ou equivalente) e autorização por perfil

## Infraestrutura / Deploy (VPS Google Cloud)

Site em produção: **https://piads2026-rh.duckdns.org/** (API em `/api`, ex. `https://piads2026-rh.duckdns.org/api/vagas`)

- **VM**: `cloudvmads`, zona `southamerica-east1-c`, projeto `project-9558c67f-ba71-45f5-82b`, IP estático `35.215.251.22`
- **SSH**: `gcloud compute ssh --zone "southamerica-east1-c" cloudvmads --project "project-9558c67f-ba71-45f5-82b"` (precisa do Google Cloud SDK instalado e autenticado — `gcloud auth login`)
- **Domínio**: DuckDNS (`piads2026-rh.duckdns.org` apontando pro IP estático) + certificado Let's Encrypt via Certbot (renovação automática)
- **Serviços na VM**:
  - MySQL local (só aceita conexão de `localhost`/`127.0.0.1`, não exposto na internet) — banco `selecao_rh`, usuário `appuser`
  - Backend: serviço systemd `backend` (`/etc/systemd/system/backend.service`), roda o jar de `/opt/app/backend/target/backend-0.0.1-SNAPSHOT.jar` com `--spring.profiles.active=local`, reinicia sozinho se cair
  - Frontend: build estático (`npm run build`) em `/opt/app/frontend/dist`, servido pelo nginx
  - Nginx: serve o frontend e faz proxy de `/api/` pro backend (porta 8080 interna); HTTP redireciona pra HTTPS
- **Credenciais do banco na VM**: em `/opt/app/backend/src/main/resources/application-local.properties` (gitignored, carregado via `spring.config.import=optional:classpath:application-local.properties`) — **atenção**: por ser carregado via `classpath:`, esse arquivo fica embutido dentro do `.jar`; editar o arquivo sozinho não basta, é preciso rebuildar (`./mvnw clean package`) pra pegar a mudança
- **CORS de produção**: variável de ambiente `CORS_ALLOWED_ORIGINS` setada no `backend.service` (`Environment=`), não no arquivo de properties — hoje só tem `https://piads2026-rh.duckdns.org`
- **Uploads (PDF do currículo)**: variável `APP_UPLOAD_DIR` no `backend.service`, apontando para `/opt/app/uploads` (fora do diretório do build, para o rebuild não apagar os arquivos). Sem essa variável o padrão é `./uploads`, relativo ao diretório de trabalho do serviço

### Workflow de deploy

**As alterações no servidor só devem acontecer depois que o código for enviado (`git push`) para o GitHub** — a VM sempre atualiza a partir do repositório remoto (`https://github.com/MarcosMilezarek/ProjetoIntegrador-ADS2026`, público), nunca de arquivos copiados diretamente do PC local.

Sequência pra atualizar a VM após um push:

```bash
gcloud compute ssh --zone "southamerica-east1-c" cloudvmads --project "project-9558c67f-ba71-45f5-82b" --command "cd /opt/app && git pull && cd backend && ./mvnw -q -DskipTests clean package && sudo systemctl restart backend && cd ../frontend && npm install --no-audit --no-fund && npm run build"
```

## Pendências conhecidas

- Nenhuma pendência de deploy no momento — Flyway roda normalmente contra o MySQL local da VPS.

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

Com o túnel aberto, `application-local.properties` aponta para `jdbc:mysql://localhost:3306/selecao_rh`
com o usuário/senha do banco da VM. Sem o túnel (ou apontando para um host inexistente) a aplicação
**não sobe**: o Flyway falha no startup ao tentar a conexão.

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
o download funcionar (o próprio arquivo tem a instrução do `printf`).
