# Contexto do Backend

> Histórico do que já foi desenvolvido e em que etapa o backend está. Atualize este arquivo
> sempre que uma feature nova for concluída, para quem retomar o trabalho (humano ou IA) não
> precisar reconstruir o contexto do zero. Última atualização: 2026-09-17.

## Stack

- Spring Boot 4.1.0 / Spring Framework 7 / Java 21 (roda em JVM 25 em desenvolvimento)
- Maven Wrapper (`./mvnw.cmd` no Windows, `./mvnw` no Git Bash)
- MySQL (Azure) em produção, H2 em memória nos testes
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

### Currículo — RF04, RF05
- `Curriculo` (formação, experiências, competências, resumo) + `Candidato` — entidade mínima interna, extensão 1:1 de `Usuario` (chave compartilhada via `@MapsId`), autoprovisionada no primeiro cadastro de currículo. Não tem controller/DTO próprios: os campos de perfil da tabela `candidato` (telefone, cidade, UF, data de nascimento, LinkedIn) não foram pedidos ainda e não são expostos.
- `CurriculoController` — `POST /api/curriculos`, `GET /api/curriculos/{id}`, `GET /api/curriculos/usuario/{usuarioId}`, `PUT /api/curriculos/{id}` (sem DELETE, sem listagem geral — currículo é dado pessoal)
- Regra 1:1 (um currículo por candidato) validada com `CurriculoJaExisteException` (409); `usuarioId` precisa ser `perfil=candidato`, senão `CandidatoInvalidoException` (403)

## O que ainda NÃO existe

- Candidatura a vaga (RF07, RN01, UC04) — vincular candidato a vaga, impedir duplicidade
- Acompanhamento de status da candidatura pelo candidato (RF08)
- Envio/gestão de documentos (RF09, RN03, RNF09 — PDF/DOCX até 5MB)
- Painel do RH: listar candidatos inscritos por vaga (RF12), atualizar status do candidato (RF13), solicitar documentos (RF14)
- Triagem assistida por IA (RF15) — integração com OpenRouter prevista em `modelagem/arquitetura.md`
- Relatórios básicos (RF16)
- Autenticação real (sessão, JWT ou equivalente) e autorização por perfil
- Endpoints próprios para o perfil complementar do candidato (tabela `candidato`)

## Pendências conhecidas

- **Deploy real bloqueado**: o usuário `appuser` do MySQL Azure não tem privilégio `CREATE`, então o Flyway nunca rodou contra o banco de produção — só foi validado via H2 nos testes automatizados. Precisa de alguém com acesso ao Azure para conceder o privilégio (fora do meu acesso).

## Como rodar

```bash
cd backend
./mvnw.cmd test          # roda a suíte completa (H2, sem precisar do MySQL)
./mvnw.cmd spring-boot:run
```

A senha do banco real (Azure) fica só em `application-local.properties` (gitignored) — nunca commitar.
