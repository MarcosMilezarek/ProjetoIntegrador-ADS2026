# Requisitos Não Funcionais

| Código | Requisito | Critério |
| ----- | ----- | ----- |
| RNF01 | Conformidade com a LGPD | O sistema deve tratar dados pessoais apenas para finalidades relacionadas ao processo seletivo e permitir solicitação de correção ou exclusão quando aplicável. |
| RNF02 | Controle de acesso | As funcionalidades devem ser liberadas conforme o perfil do usuário: candidato, RH ou administrador. |
| RNF03 | Comunicação segura | O tráfego entre cliente e servidor deve utilizar HTTPS em ambiente de produção. |
| RNF04 | Armazenamento de senhas | As senhas devem ser armazenadas utilizando hash seguro, sem gravação em texto puro. |
| RNF05 | Tempo de resposta das telas | As telas principais devem carregar em até 2 segundos em condições normais de rede e uso. |
| RNF06 | Tempo de resposta da IA | A triagem de um currículo deve retornar resultado em até 8 segundos em condições normais de processamento. |
| RNF07 | Disponibilidade | O sistema deve permanecer disponível durante todo o dia, salvo períodos de manutenção previamente informados. |
| RNF08 | Acessos simultâneos | A solução deve suportar ao menos 300 usuários simultâneos em cenário de uso comum. |
| RNF09 | Upload de arquivos | Currículos e documentos devem aceitar PDF e DOCX, com limite inicial de 5 MB por arquivo. |
| RNF10 | Usabilidade e acessibilidade | A interface deve ser simples, responsiva e seguir boas práticas de acessibilidade, incluindo contraste, rótulos claros e navegação objetiva. |
| RNF11 | Arquitetura web | O sistema deve separar front-end, back-end, banco de dados, armazenamento de arquivos e serviços externos por meio de APIs. |
| RNF12 | Auditoria | Ações relevantes, como alteração de status e acesso a documentos, devem ser registradas para rastreabilidade. |
| RNF13 | Backup | A base de dados e os arquivos enviados devem possuir rotina de backup adequada ao ambiente de implantação. |
