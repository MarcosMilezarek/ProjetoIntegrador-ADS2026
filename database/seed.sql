-- ---------------------------------------------------------------------------
-- Dados de teste do Sistema de Processo Seletivo.
--
-- Cobre todas as tabelas do schema (V1 + V2). NAO roda automaticamente: o
-- Flyway nao le este arquivo, justamente para nao injetar dados de teste em
-- producao sem intencao. Aplicar manualmente:
--
--   mysql -u <usuario> -p selecao_rh < database/seed.sql
--
-- Senha de todos os usuarios de teste: senha123
-- Todos os emails terminam em @exemplo.test, e a limpeza abaixo se restringe
-- a esse dominio - contas reais nao sao tocadas.
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------
-- Limpeza do seed anterior (somente dominio @exemplo.test)
-- ---------------------------------------------------------------
-- candidatura em cascata remove documento, analise_ia e historico_status
DELETE FROM candidatura
WHERE usuario_id IN (SELECT id FROM usuario WHERE email LIKE '%@exemplo.test')
   OR vaga_id IN (SELECT v.id FROM vaga v JOIN usuario u ON u.id = v.rh_id
                  WHERE u.email LIKE '%@exemplo.test');

-- curriculo em cascata remove formacao, experiencia e arquivo
DELETE FROM curriculo
WHERE usuario_id IN (SELECT id FROM usuario WHERE email LIKE '%@exemplo.test');

DELETE FROM vaga
WHERE rh_id IN (SELECT id FROM usuario WHERE email LIKE '%@exemplo.test');

DELETE FROM usuario WHERE email LIKE '%@exemplo.test';

-- ---------------------------------------------------------------
-- usuario (senha de todos: senha123)
-- ---------------------------------------------------------------
INSERT INTO usuario (nome, email, senha_hash, perfil, status) VALUES
  ('Administrador Geral', 'admin@exemplo.test',
   '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'administrador', 'ativo'),
  ('Rita Nogueira',       'rita.rh@exemplo.test',
   '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'rh', 'ativo'),
  ('Paulo Fontes',        'paulo.rh@exemplo.test',
   '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'rh', 'ativo'),
  ('Ana Souza',           'ana.candidata@exemplo.test',
   '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo'),
  ('Bruno Lima',          'bruno.candidato@exemplo.test',
   '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo'),
  ('Carla Mendes',        'carla.candidata@exemplo.test',
   '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo'),
  -- sem curriculo: exercita a tela de "primeiro cadastro"
  ('Diego Rocha',         'diego.candidato@exemplo.test',
   '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo'),
  -- conta inativa: exercita o bloqueio de login
  ('Elisa Prado',         'elisa.inativa@exemplo.test',
   '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'inativo');

-- ---------------------------------------------------------------
-- vaga
-- ---------------------------------------------------------------
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo) VALUES
  ((SELECT id FROM usuario WHERE email = 'rita.rh@exemplo.test'),
   'Desenvolvedor(a) Backend Java',
   'Manutencao e evolucao de APIs REST do produto de recrutamento.',
   'Java 21, Spring Boot, MySQL, testes automatizados', 'Campinas - SP',
   'hibrido', 'clt', 'aberta', DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY)),
  ((SELECT id FROM usuario WHERE email = 'rita.rh@exemplo.test'),
   'Desenvolvedor(a) Frontend React',
   'Construcao das telas do portal do candidato e do painel do RH.',
   'React, TypeScript, Tailwind CSS', 'Remoto',
   'remoto', 'clt', 'aberta', DATE_ADD(CURRENT_DATE, INTERVAL 45 DAY)),
  ((SELECT id FROM usuario WHERE email = 'paulo.rh@exemplo.test'),
   'Estagio em Analise de Dados',
   'Apoio na construcao de relatorios e indicadores de processo seletivo.',
   'Excel, SQL basico, cursando ensino superior', 'Sao Paulo - SP',
   'presencial', 'estagio', 'aberta', DATE_ADD(CURRENT_DATE, INTERVAL 15 DAY)),
  ((SELECT id FROM usuario WHERE email = 'paulo.rh@exemplo.test'),
   'Analista de RH Junior',
   'Conducao de entrevistas e triagem de curriculos.',
   'Experiencia com recrutamento e selecao', 'Campinas - SP',
   'presencial', 'clt', 'rascunho', NULL),
  ((SELECT id FROM usuario WHERE email = 'rita.rh@exemplo.test'),
   'Tech Lead (encerrada)',
   'Vaga encerrada, mantida para historico conforme RN05.',
   'Lideranca tecnica de squad', 'Remoto',
   'remoto', 'pj', 'encerrada', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY));

-- ---------------------------------------------------------------
-- curriculo (dados pessoais + contato + conteudo)
-- ---------------------------------------------------------------
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin,
                        competencias, certificacoes, resumo) VALUES
  ((SELECT id FROM usuario WHERE email = 'ana.candidata@exemplo.test'),
   '1998-04-12', 'feminino', 'Campinas', 'SP', '19999990001', 'https://linkedin.com/in/ana-souza',
   'Java, Spring Boot, MySQL, Git',
   'AWS Cloud Practitioner (2024), Scrum Foundation PSPO I (2023)',
   'Desenvolvedora backend com foco em APIs REST e bancos relacionais.'),
  ((SELECT id FROM usuario WHERE email = 'bruno.candidato@exemplo.test'),
   '1995-11-03', 'masculino', 'Sao Paulo', 'SP', '11988880002', 'https://linkedin.com/in/bruno-lima',
   'React, TypeScript, Tailwind CSS, Vite',
   NULL,
   'Desenvolvedor frontend com experiencia em design systems e acessibilidade.'),
  ((SELECT id FROM usuario WHERE email = 'carla.candidata@exemplo.test'),
   '2003-07-22', 'nao_informado', 'Jundiai', 'SP', '11977770003', NULL,
   'SQL, Power BI, Excel avancado',
   'Google Data Analytics (2025)',
   'Estudante de ADS buscando primeira oportunidade em dados.');

-- ---------------------------------------------------------------
-- curriculo_formacao
-- ---------------------------------------------------------------
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  ((SELECT c.id FROM curriculo c JOIN usuario u ON u.id = c.usuario_id WHERE u.email = 'ana.candidata@exemplo.test'),
   'Analise e Desenvolvimento de Sistemas', 'Fatec Campinas', '2017-02-01', '2019-12-15'),
  ((SELECT c.id FROM curriculo c JOIN usuario u ON u.id = c.usuario_id WHERE u.email = 'ana.candidata@exemplo.test'),
   'Pos-graduacao em Engenharia de Software', 'PUC Minas', '2021-03-01', '2022-08-30'),
  ((SELECT c.id FROM curriculo c JOIN usuario u ON u.id = c.usuario_id WHERE u.email = 'bruno.candidato@exemplo.test'),
   'Ciencia da Computacao', 'Universidade de Sao Paulo', '2014-02-01', '2018-12-10'),
  -- curso em andamento: data_termino nula
  ((SELECT c.id FROM curriculo c JOIN usuario u ON u.id = c.usuario_id WHERE u.email = 'carla.candidata@exemplo.test'),
   'Analise e Desenvolvimento de Sistemas', 'Fatec Jundiai', '2024-02-01', NULL);

-- ---------------------------------------------------------------
-- curriculo_experiencia
-- ---------------------------------------------------------------
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao,
                                    trabalho_atual, descricao_atividades) VALUES
  -- emprego atual: data_demissao obrigatoriamente nula (chk_experiencia_trabalho_atual)
  ((SELECT c.id FROM curriculo c JOIN usuario u ON u.id = c.usuario_id WHERE u.email = 'ana.candidata@exemplo.test'),
   'Desenvolvedora Backend Pleno', 'Acme Software', '2022-03-01', NULL, TRUE,
   'Desenvolvimento e manutencao de APIs REST em Spring Boot; cobertura de testes com JUnit.'),
  ((SELECT c.id FROM curriculo c JOIN usuario u ON u.id = c.usuario_id WHERE u.email = 'ana.candidata@exemplo.test'),
   'Desenvolvedora Backend Junior', 'Tech Solutions', '2020-01-15', '2022-02-28', FALSE,
   'Integracoes com servicos de terceiros e rotinas de importacao de dados.'),
  ((SELECT c.id FROM curriculo c JOIN usuario u ON u.id = c.usuario_id WHERE u.email = 'bruno.candidato@exemplo.test'),
   'Desenvolvedor Frontend Senior', 'Studio Web', '2021-06-01', NULL, TRUE,
   'Lideranca do design system e migracao do legado para React com TypeScript.'),
  ((SELECT c.id FROM curriculo c JOIN usuario u ON u.id = c.usuario_id WHERE u.email = 'carla.candidata@exemplo.test'),
   'Jovem Aprendiz - Administrativo', 'Distribuidora Central', '2022-02-01', '2023-08-31', FALSE,
   'Apoio na rotina administrativa e construcao de planilhas de acompanhamento.');

-- ---------------------------------------------------------------
-- curriculo_arquivo
-- Atencao: o binario fica em disco, em ${app.upload.dir}/curriculo/.
-- Para o download funcionar, crie o arquivo correspondente:
--   mkdir -p <app.upload.dir>/curriculo
--   printf '%%PDF-1.4 curriculo de exemplo' > <app.upload.dir>/curriculo/seed-ana.pdf
-- ---------------------------------------------------------------
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes) VALUES
  ((SELECT c.id FROM curriculo c JOIN usuario u ON u.id = c.usuario_id WHERE u.email = 'ana.candidata@exemplo.test'),
   'curriculo-ana-souza.pdf', 'seed-ana.pdf', 'application/pdf', 29);

-- ---------------------------------------------------------------
-- candidatura (RN01: um candidato nao se inscreve duas vezes na mesma vaga)
-- ---------------------------------------------------------------
INSERT INTO candidatura (usuario_id, vaga_id, status, data_candidatura) VALUES
  ((SELECT id FROM usuario WHERE email = 'ana.candidata@exemplo.test'),
   (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Backend Java'),
   'entrevista', DATE_SUB(NOW(), INTERVAL 12 DAY)),
  ((SELECT id FROM usuario WHERE email = 'ana.candidata@exemplo.test'),
   (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Frontend React'),
   'inscrito', DATE_SUB(NOW(), INTERVAL 3 DAY)),
  ((SELECT id FROM usuario WHERE email = 'bruno.candidato@exemplo.test'),
   (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Frontend React'),
   'em_triagem', DATE_SUB(NOW(), INTERVAL 6 DAY)),
  ((SELECT id FROM usuario WHERE email = 'bruno.candidato@exemplo.test'),
   (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Backend Java'),
   'reprovado', DATE_SUB(NOW(), INTERVAL 20 DAY)),
  ((SELECT id FROM usuario WHERE email = 'carla.candidata@exemplo.test'),
   (SELECT id FROM vaga WHERE titulo = 'Estagio em Analise de Dados'),
   'aprovado', DATE_SUB(NOW(), INTERVAL 8 DAY));

-- ---------------------------------------------------------------
-- documento (RN03/RNF09: PDF ou DOCX, ate 5MB)
-- ---------------------------------------------------------------
INSERT INTO documento (candidatura_id, tipo, formato, arquivo_url, tamanho_bytes) VALUES
  ((SELECT id FROM candidatura
    WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'ana.candidata@exemplo.test')
      AND vaga_id = (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Backend Java')),
   'RG', 'pdf', 'https://storage.exemplo.test/documentos/ana-rg.pdf', 184320),
  ((SELECT id FROM candidatura
    WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'ana.candidata@exemplo.test')
      AND vaga_id = (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Backend Java')),
   'Comprovante de escolaridade', 'pdf', 'https://storage.exemplo.test/documentos/ana-diploma.pdf', 402944),
  ((SELECT id FROM candidatura
    WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'carla.candidata@exemplo.test')
      AND vaga_id = (SELECT id FROM vaga WHERE titulo = 'Estagio em Analise de Dados')),
   'Declaracao de matricula', 'docx', 'https://storage.exemplo.test/documentos/carla-matricula.docx', 96256);

-- ---------------------------------------------------------------
-- analise_ia (RF15: triagem assistida, pontuacao 0-100)
-- ---------------------------------------------------------------
INSERT INTO analise_ia (candidatura_id, pontuacao, resumo) VALUES
  ((SELECT id FROM candidatura
    WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'ana.candidata@exemplo.test')
      AND vaga_id = (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Backend Java')),
   92.50, 'Forte aderencia: experiencia atual em Spring Boot e MySQL, alem de testes automatizados.'),
  ((SELECT id FROM candidatura
    WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'bruno.candidato@exemplo.test')
      AND vaga_id = (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Frontend React')),
   88.00, 'Aderencia alta em React e TypeScript; sem experiencia relatada com testes end-to-end.'),
  ((SELECT id FROM candidatura
    WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'bruno.candidato@exemplo.test')
      AND vaga_id = (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Backend Java')),
   34.75, 'Perfil predominantemente frontend; pouca evidencia de Java ou banco relacional.');

-- ---------------------------------------------------------------
-- historico_status (RF08/RF13: trilha de mudancas de status)
-- ---------------------------------------------------------------
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  ((SELECT id FROM candidatura
    WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'ana.candidata@exemplo.test')
      AND vaga_id = (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Backend Java')),
   NULL, 'inscrito', (SELECT id FROM usuario WHERE email = 'ana.candidata@exemplo.test'),
   'Candidatura registrada pelo portal.', DATE_SUB(NOW(), INTERVAL 12 DAY)),
  ((SELECT id FROM candidatura
    WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'ana.candidata@exemplo.test')
      AND vaga_id = (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Backend Java')),
   'inscrito', 'em_triagem', (SELECT id FROM usuario WHERE email = 'rita.rh@exemplo.test'),
   'Selecionada na triagem automatica (92,5 pontos).', DATE_SUB(NOW(), INTERVAL 9 DAY)),
  ((SELECT id FROM candidatura
    WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'ana.candidata@exemplo.test')
      AND vaga_id = (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Backend Java')),
   'em_triagem', 'entrevista', (SELECT id FROM usuario WHERE email = 'rita.rh@exemplo.test'),
   'Entrevista tecnica agendada.', DATE_SUB(NOW(), INTERVAL 4 DAY)),
  ((SELECT id FROM candidatura
    WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'bruno.candidato@exemplo.test')
      AND vaga_id = (SELECT id FROM vaga WHERE titulo = 'Desenvolvedor(a) Backend Java')),
   'em_triagem', 'reprovado', (SELECT id FROM usuario WHERE email = 'rita.rh@exemplo.test'),
   'Perfil mais aderente a vagas de frontend.', DATE_SUB(NOW(), INTERVAL 18 DAY)),
  ((SELECT id FROM candidatura
    WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'carla.candidata@exemplo.test')
      AND vaga_id = (SELECT id FROM vaga WHERE titulo = 'Estagio em Analise de Dados')),
   'entrevista', 'aprovado', (SELECT id FROM usuario WHERE email = 'paulo.rh@exemplo.test'),
   'Aprovada para o programa de estagio.', DATE_SUB(NOW(), INTERVAL 2 DAY));
