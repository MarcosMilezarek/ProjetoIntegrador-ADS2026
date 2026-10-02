# Database

Scripts SQL do MySQL do sistema de seleção e recrutamento.

| Arquivo | Para que serve |
| --- | --- |
| `schema.sql` | Schema inicial (espelha a migration V1). O schema vigente é versionado pelo Flyway em `backend/src/main/resources/db/migration`. |
| `seed.sql` | Dados de teste de um ambiente de escritório (usuários, vagas, currículos, candidaturas em todas as etapas, documentos, agenda, funcionários e notificações). Não roda sozinho. |
| `seed-arquivos.sh` | Cria os PDFs de exemplo que o seed referencia (currículos e documentos de contratação). |

Como aplicar o seed, o que ele cria e o aviso sobre o fuso horário estão em
[`backend/CONTEXTO.md`](../backend/CONTEXTO.md), na seção "Dados de teste (seed)". Senha dos usuários de teste: `senha123`.
