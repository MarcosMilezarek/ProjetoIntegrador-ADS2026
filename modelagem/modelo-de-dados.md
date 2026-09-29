# Modelo de Dados

O schema real e atualizado do banco (tipos, enums, chaves, índices) está em [database/schema.sql](../database/schema.sql) — use-o como fonte de verdade para implementação. As tabelas abaixo são o modelo conceitual.

> **Nota de revisão (2026-09-29):** a entidade `Candidato`, que era uma extensão 1:1 de `Usuário`,
> foi removida. Havia três tabelas 1:1 para a mesma pessoa (`usuario` → `candidato` → `curriculo`),
> o que tornava os nomes indistinguíveis entre si. Os dados pessoais e de contato passaram para
> `Currículo`, que agora referencia `Usuário` diretamente. Hoje a leitura é direta:
> **Usuário = conta de acesso; Currículo = dados do candidato.**

## Entidades principais

| Entidade | Campos principais | Finalidade |
| ----- | ----- | ----- |
| Usuário | id, nome, e-mail, senha_hash, perfil, status | Conta de acesso. Representa candidatos, profissionais de RH e administradores. |
| Currículo | id, usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competências, certificações, resumo | Dados pessoais, contato e conteúdo profissional do candidato (1:1 com Usuário). |
| Formação (currículo) | id, curriculo_id, curso, instituição, data_início, data_término | Formação acadêmica; um currículo tem várias. `data_término` nula = curso em andamento. |
| Experiência (currículo) | id, curriculo_id, cargo, empresa, data_contratação, data_demissão, trabalho_atual, descrição_atividades | Experiência profissional; um currículo tem várias. `data_demissão` é sempre nula quando `trabalho_atual` é verdadeiro. |
| Arquivo do currículo | id, curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em | Metadados do PDF anexado (1:1 com Currículo). O binário fica em disco, não no banco. |
| Vaga | id, título, descrição, requisitos, local, modalidade, tipo, status | Representa uma oportunidade cadastrada pelo RH. |
| Candidatura | id, usuario_id, vaga_id, status, data_candidatura | Relaciona candidato e vaga, registrando o andamento do processo. |
| Documento | id, candidatura_id, tipo, arquivo_url, data_envio | Registra documentos enviados pelo candidato. |
| Análise IA | id, candidatura_id, pontuação, resumo, data_análise | Armazena o resultado da triagem assistida por IA. |
| Histórico de status | id, candidatura_id, status_anterior, status_novo, usuário, data | Mantém rastreabilidade das mudanças do processo. |

## Diagrama ER

> **Atenção:** a imagem abaixo ainda mostra o modelo antigo (com a tabela `candidato` e o currículo
> de campos livres). Precisa ser regerada para refletir a tabela acima.

![Diagrama ER do banco de dados](imagens/figura-15-diagrama-er.png)
