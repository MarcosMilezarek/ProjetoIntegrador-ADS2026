# Database

Scripts SQL do MySQL do sistema de seleção e recrutamento.

| Arquivo | Para que serve |
| --- | --- |
| `schema.sql` | Schema inicial (espelha a migration V1). O schema vigente é versionado pelo Flyway em `backend/src/main/resources/db/migration`. |
| `seed.sql` | Dados de teste de um ambiente de escritório (usuários, vagas, currículos, candidaturas em todas as etapas, documentos, agenda, funcionários e notificações). Não roda sozinho. |
| `seed-arquivos.sh` | Cria os PDFs de exemplo que o seed referencia (currículos e documentos de contratação). |

Como aplicar o seed está no [README do projeto](../README.md#dados-de-teste-opcional). Senha dos usuários de teste: `senha123`.

O seed pode ser reexecutado: ele começa removendo só os dados do domínio `@exemplo.test`. Todas as datas são relativas ao momento da execução, para que a agenda e os prazos façam sentido no dia em que for aplicado.

Fuso horário: o driver JDBC (`serverTimezone=America/Sao_Paulo`) grava as colunas de data e hora em horário de Brasília, e o seed segue a mesma convenção.
