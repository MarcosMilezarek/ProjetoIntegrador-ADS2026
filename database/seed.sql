-- ---------------------------------------------------------------------------
-- Dados de teste do Sistema de Processo Seletivo (ambiente de escritório).
--
-- Gerado para o schema atual (migrations V1 a V6). NAO roda automaticamente: o Flyway
-- nao le este arquivo, justamente para nao injetar dados de teste em producao sem
-- intencao. Aplicar manualmente (a conexao precisa estar em utf8mb4):
--
--   mysql --default-character-set=utf8mb4 -u <usuario> -p selecao_rh < database/seed.sql
--
-- Senha de todos os usuarios de teste: senha123
-- Todos os emails terminam em @exemplo.test, e a limpeza abaixo se restringe a esse
-- dominio - contas reais nao sao tocadas. Pode ser reexecutado.
--
-- O que o seed cria:
--   * 1 administrador, 3 RH e 30 candidatos (um sem curriculo, um inativo e um bloqueado);
--   * 18 vagas de escritorio (14 abertas, 2 rascunhos e 2 encerradas);
--   * 52 candidaturas em todas as etapas: inscrito, em triagem, entrevista (com agenda e
--     presenca pendente ou confirmada), aprovado (com documentos em varias situacoes),
--     reprovado, cancelado e contratado (com funcionarios ativos e inativo);
--   * historico de etapas, documentos de contratacao, analises de IA e notificacoes
--     coerentes com cada passo (as recentes ficam nao lidas).
--
-- As datas sao relativas ao momento da execucao (@agora), em horario de Brasilia, que e como o
-- backend grava todas as colunas de data e hora. Os PDFs de exemplo dos curriculos e dos
-- documentos sao criados por database/seed-arquivos.sh.
-- ---------------------------------------------------------------------------

SET NAMES utf8mb4;
SET @agora = UTC_TIMESTAMP() - INTERVAL 3 HOUR;

-- ---------------------------------------------------------------
-- Limpeza do seed anterior (somente dominio @exemplo.test)
-- ---------------------------------------------------------------
-- candidatura em cascata remove documento, analise_ia, historico_status e funcionario
DELETE FROM candidatura
WHERE usuario_id IN (SELECT id FROM usuario WHERE email LIKE '%@exemplo.test')
   OR vaga_id IN (SELECT v.id FROM vaga v JOIN usuario u ON u.id = v.rh_id
                  WHERE u.email LIKE '%@exemplo.test');

-- curriculo em cascata remove formacao, experiencia e arquivo
DELETE FROM curriculo
WHERE usuario_id IN (SELECT id FROM usuario WHERE email LIKE '%@exemplo.test');

DELETE FROM vaga
WHERE rh_id IN (SELECT id FROM usuario WHERE email LIKE '%@exemplo.test');

-- usuario em cascata remove as notificacoes
DELETE FROM usuario WHERE email LIKE '%@exemplo.test';

-- ---------------------------------------------------------------
-- usuario (senha de todos: senha123)
-- ---------------------------------------------------------------
INSERT INTO usuario (nome, email, senha_hash, perfil, status, criado_em, atualizado_em) VALUES
  ('Administrador Geral', 'admin@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'administrador', 'ativo', @agora - INTERVAL 90 DAY, @agora - INTERVAL 90 DAY),
  ('Rita Nogueira', 'rita.rh@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'rh', 'ativo', @agora - INTERVAL 80 DAY, @agora - INTERVAL 80 DAY),
  ('Paulo Fontes', 'paulo.rh@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'rh', 'ativo', @agora - INTERVAL 80 DAY, @agora - INTERVAL 80 DAY),
  ('Marina Castro', 'marina.rh@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'rh', 'ativo', @agora - INTERVAL 80 DAY, @agora - INTERVAL 80 DAY),
  ('Ana Souza', 'ana.souza@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 75 DAY, @agora - INTERVAL 75 DAY),
  ('Bruno Lima', 'bruno.lima@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 74 DAY, @agora - INTERVAL 74 DAY),
  ('Carla Mendes', 'carla.mendes@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 73 DAY, @agora - INTERVAL 73 DAY),
  ('Diego Rocha', 'diego.rocha@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 72 DAY, @agora - INTERVAL 72 DAY),
  ('Elisa Prado', 'elisa.prado@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'inativo', @agora - INTERVAL 71 DAY, @agora - INTERVAL 71 DAY),
  ('Felipe Andrade', 'felipe.andrade@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 70 DAY, @agora - INTERVAL 70 DAY),
  ('Gabriela Torres', 'gabriela.torres@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 69 DAY, @agora - INTERVAL 69 DAY),
  ('Henrique Duarte', 'henrique.duarte@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 68 DAY, @agora - INTERVAL 68 DAY),
  ('Isabela Ferraz', 'isabela.ferraz@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 67 DAY, @agora - INTERVAL 67 DAY),
  ('João Pedro Martins', 'joao.martins@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 66 DAY, @agora - INTERVAL 66 DAY),
  ('Karina Alves', 'karina.alves@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 65 DAY, @agora - INTERVAL 65 DAY),
  ('Lucas Barbosa', 'lucas.barbosa@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 64 DAY, @agora - INTERVAL 64 DAY),
  ('Mariana Cardoso', 'mariana.cardoso@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 63 DAY, @agora - INTERVAL 63 DAY),
  ('Nicolas Teixeira', 'nicolas.teixeira@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 62 DAY, @agora - INTERVAL 62 DAY),
  ('Olívia Ramos', 'olivia.ramos@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 61 DAY, @agora - INTERVAL 61 DAY),
  ('Pedro Henrique Gomes', 'pedro.gomes@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 60 DAY, @agora - INTERVAL 60 DAY),
  ('Renata Siqueira', 'renata.siqueira@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 59 DAY, @agora - INTERVAL 59 DAY),
  ('Sérgio Vasconcelos', 'sergio.vasconcelos@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 58 DAY, @agora - INTERVAL 58 DAY),
  ('Tatiane Moreira', 'tatiane.moreira@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 57 DAY, @agora - INTERVAL 57 DAY),
  ('Ulisses Pinheiro', 'ulisses.pinheiro@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 56 DAY, @agora - INTERVAL 56 DAY),
  ('Vanessa Cunha', 'vanessa.cunha@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 55 DAY, @agora - INTERVAL 55 DAY),
  ('Wesley Nunes', 'wesley.nunes@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 54 DAY, @agora - INTERVAL 54 DAY),
  ('Yasmin Carvalho', 'yasmin.carvalho@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 53 DAY, @agora - INTERVAL 53 DAY),
  ('Rafael Monteiro', 'rafael.monteiro@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 52 DAY, @agora - INTERVAL 52 DAY),
  ('Beatriz Azevedo', 'beatriz.azevedo@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 51 DAY, @agora - INTERVAL 51 DAY),
  ('Cauã Ribeiro', 'caua.ribeiro@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 50 DAY, @agora - INTERVAL 50 DAY),
  ('Débora Freitas', 'debora.freitas@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 49 DAY, @agora - INTERVAL 49 DAY),
  ('Eduardo Salgado', 'eduardo.salgado@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 48 DAY, @agora - INTERVAL 48 DAY),
  ('Fernanda Lacerda', 'fernanda.lacerda@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'ativo', @agora - INTERVAL 47 DAY, @agora - INTERVAL 47 DAY),
  ('Otávio Brandão', 'otavio.brandao@exemplo.test', '$2a$10$FpVlSt1I0U8FPwN0p8xm4OxaOhblnHKxhZdG68hCtWNIyaVcl6f/q', 'candidato', 'bloqueado', @agora - INTERVAL 46 DAY, @agora - INTERVAL 46 DAY);

-- ids usados nas demais tabelas
SELECT id INTO @u_admin FROM usuario WHERE email = 'admin@exemplo.test';
SELECT id INTO @u_rita FROM usuario WHERE email = 'rita.rh@exemplo.test';
SELECT id INTO @u_paulo FROM usuario WHERE email = 'paulo.rh@exemplo.test';
SELECT id INTO @u_marina FROM usuario WHERE email = 'marina.rh@exemplo.test';
SELECT id INTO @u_ana FROM usuario WHERE email = 'ana.souza@exemplo.test';
SELECT id INTO @u_bruno FROM usuario WHERE email = 'bruno.lima@exemplo.test';
SELECT id INTO @u_carla FROM usuario WHERE email = 'carla.mendes@exemplo.test';
SELECT id INTO @u_diego FROM usuario WHERE email = 'diego.rocha@exemplo.test';
SELECT id INTO @u_elisa FROM usuario WHERE email = 'elisa.prado@exemplo.test';
SELECT id INTO @u_felipe FROM usuario WHERE email = 'felipe.andrade@exemplo.test';
SELECT id INTO @u_gabriela FROM usuario WHERE email = 'gabriela.torres@exemplo.test';
SELECT id INTO @u_henrique FROM usuario WHERE email = 'henrique.duarte@exemplo.test';
SELECT id INTO @u_isabela FROM usuario WHERE email = 'isabela.ferraz@exemplo.test';
SELECT id INTO @u_joao FROM usuario WHERE email = 'joao.martins@exemplo.test';
SELECT id INTO @u_karina FROM usuario WHERE email = 'karina.alves@exemplo.test';
SELECT id INTO @u_lucas FROM usuario WHERE email = 'lucas.barbosa@exemplo.test';
SELECT id INTO @u_mariana FROM usuario WHERE email = 'mariana.cardoso@exemplo.test';
SELECT id INTO @u_nicolas FROM usuario WHERE email = 'nicolas.teixeira@exemplo.test';
SELECT id INTO @u_olivia FROM usuario WHERE email = 'olivia.ramos@exemplo.test';
SELECT id INTO @u_pedro FROM usuario WHERE email = 'pedro.gomes@exemplo.test';
SELECT id INTO @u_renata FROM usuario WHERE email = 'renata.siqueira@exemplo.test';
SELECT id INTO @u_sergio FROM usuario WHERE email = 'sergio.vasconcelos@exemplo.test';
SELECT id INTO @u_tatiane FROM usuario WHERE email = 'tatiane.moreira@exemplo.test';
SELECT id INTO @u_ulisses FROM usuario WHERE email = 'ulisses.pinheiro@exemplo.test';
SELECT id INTO @u_vanessa FROM usuario WHERE email = 'vanessa.cunha@exemplo.test';
SELECT id INTO @u_wesley FROM usuario WHERE email = 'wesley.nunes@exemplo.test';
SELECT id INTO @u_yasmin FROM usuario WHERE email = 'yasmin.carvalho@exemplo.test';
SELECT id INTO @u_rafael FROM usuario WHERE email = 'rafael.monteiro@exemplo.test';
SELECT id INTO @u_beatriz FROM usuario WHERE email = 'beatriz.azevedo@exemplo.test';
SELECT id INTO @u_caua FROM usuario WHERE email = 'caua.ribeiro@exemplo.test';
SELECT id INTO @u_debora FROM usuario WHERE email = 'debora.freitas@exemplo.test';
SELECT id INTO @u_eduardo FROM usuario WHERE email = 'eduardo.salgado@exemplo.test';
SELECT id INTO @u_fernanda FROM usuario WHERE email = 'fernanda.lacerda@exemplo.test';
SELECT id INTO @u_otavio FROM usuario WHERE email = 'otavio.brandao@exemplo.test';

-- ---------------------------------------------------------------
-- vaga (ambiente de escritorio)
-- ---------------------------------------------------------------
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_rita, 'Assistente Administrativo', 'Apoio às rotinas do escritório: recepção de documentos, controle de agenda da diretoria, pedidos de material, protocolo e organização de arquivos físicos e digitais.', 'Ensino médio completo (superior em andamento é diferencial), Pacote Office, boa comunicação escrita, organização.', 'São Paulo - SP', 'presencial', 'clt', 'aberta', DATE(@agora) + INTERVAL 20 DAY, @agora - INTERVAL 43200 MINUTE, @agora - INTERVAL 43200 MINUTE);
SET @v_adm = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_rita, 'Analista Financeiro Pleno', 'Fluxo de caixa, conciliação bancária, projeções orçamentárias e relatórios gerenciais para a diretoria. Contato diário com contabilidade e fornecedores.', 'Superior em Administração, Economia ou Ciências Contábeis, Excel avançado, 3 anos em rotinas financeiras, ERP.', 'São Paulo - SP', 'hibrido', 'clt', 'aberta', DATE(@agora) + INTERVAL 25 DAY, @agora - INTERVAL 50400 MINUTE, @agora - INTERVAL 50400 MINUTE);
SET @v_fin = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_paulo, 'Analista de Departamento Pessoal', 'Admissões e demissões, folha de pagamento, férias, benefícios e obrigações acessórias (eSocial, FGTS, INSS). Atendimento às dúvidas dos colaboradores.', 'Experiência com folha de pagamento e eSocial, conhecimento da legislação trabalhista, Excel intermediário.', 'Campinas - SP', 'presencial', 'clt', 'aberta', DATE(@agora) + INTERVAL 18 DAY, @agora - INTERVAL 40320 MINUTE, @agora - INTERVAL 40320 MINUTE);
SET @v_dp = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_paulo, 'Recepcionista Corporativa', 'Recepção de clientes e visitantes, controle de acesso, atendimento telefônico, agendamento de salas de reunião e apoio à equipe administrativa.', 'Ensino médio completo, boa dicção e postura profissional, experiência em recepção ou atendimento, inglês básico.', 'São Paulo - SP', 'presencial', 'clt', 'aberta', DATE(@agora) + INTERVAL 14 DAY, @agora - INTERVAL 57600 MINUTE, @agora - INTERVAL 57600 MINUTE);
SET @v_recep = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_marina, 'Auxiliar de Contabilidade', 'Classificação e lançamento de documentos contábeis, conciliações, apoio no fechamento mensal e na entrega de obrigações fiscais.', 'Cursando Ciências Contábeis, noções de escrituração contábil, Excel intermediário, atenção a detalhes.', 'Curitiba - PR', 'presencial', 'clt', 'aberta', DATE(@agora) + INTERVAL 30 DAY, @agora - INTERVAL 37440 MINUTE, @agora - INTERVAL 37440 MINUTE);
SET @v_contab = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_rita, 'Analista de Contas a Pagar e Receber', 'Programação de pagamentos, emissão de boletos e notas, controle de inadimplência, conferência de lançamentos e relatórios semanais.', 'Experiência em contas a pagar e receber, Excel, noções de fluxo de caixa e de ERP.', 'Belo Horizonte - MG', 'hibrido', 'clt', 'aberta', DATE(@agora) + INTERVAL 22 DAY, @agora - INTERVAL 28800 MINUTE, @agora - INTERVAL 28800 MINUTE);
SET @v_cpr = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_marina, 'Assistente de Recrutamento e Seleção', 'Divulgação de vagas, triagem de currículos, agendamento e condução de entrevistas iniciais, contato com candidatos e apoio na admissão.', 'Superior em Psicologia, Administração ou RH (cursando ou completo), boa comunicação, empatia e organização.', 'Campinas - SP', 'hibrido', 'clt', 'aberta', DATE(@agora) + INTERVAL 16 DAY, @agora - INTERVAL 25920 MINUTE, @agora - INTERVAL 25920 MINUTE);
SET @v_rs = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_paulo, 'Estagiário(a) de Administração', 'Apoio às áreas administrativa e financeira: organização de documentos, planilhas de controle, atendimento interno e suporte em processos de compras.', 'Cursando Administração ou áreas afins (a partir do 2º semestre), Excel básico, vontade de aprender. Jornada de 6 horas.', 'São Paulo - SP', 'presencial', 'estagio', 'aberta', DATE(@agora) + INTERVAL 12 DAY, @agora - INTERVAL 21600 MINUTE, @agora - INTERVAL 21600 MINUTE);
SET @v_est = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_rita, 'Secretária Executiva Bilíngue', 'Gestão da agenda e das viagens da diretoria, redação de e-mails e atas em português e inglês, organização de reuniões e relacionamento com clientes.', 'Inglês fluente, experiência como secretária ou assistente de diretoria, discrição, domínio do Pacote Office.', 'São Paulo - SP', 'presencial', 'clt', 'aberta', DATE(@agora) + INTERVAL 28 DAY, @agora - INTERVAL 31680 MINUTE, @agora - INTERVAL 31680 MINUTE);
SET @v_sec = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_marina, 'Analista de Compras', 'Cotações e negociação com fornecedores, emissão de pedidos, acompanhamento de entregas e manutenção do cadastro de fornecedores.', 'Experiência em compras ou suprimentos, Excel intermediário, boa negociação, conhecimento de ERP.', 'Curitiba - PR', 'hibrido', 'clt', 'aberta', DATE(@agora) + INTERVAL 21 DAY, @agora - INTERVAL 17280 MINUTE, @agora - INTERVAL 17280 MINUTE);
SET @v_comp = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_paulo, 'Coordenador(a) Administrativo', 'Liderança da equipe administrativa (6 pessoas), gestão de contratos de serviços, orçamento do escritório e melhoria dos processos internos.', 'Superior completo, 5 anos em rotinas administrativas, 2 anos liderando equipes, indicadores e gestão de contratos.', 'Campinas - SP', 'presencial', 'clt', 'aberta', DATE(@agora) + INTERVAL 35 DAY, @agora - INTERVAL 5760 MINUTE, @agora - INTERVAL 5760 MINUTE);
SET @v_coord = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_marina, 'Assistente Comercial', 'Elaboração de propostas e contratos, atualização do CRM, suporte à equipe de vendas e acompanhamento de pedidos até a entrega.', 'Ensino médio completo, Excel intermediário, experiência com CRM é diferencial, boa comunicação.', 'Belo Horizonte - MG', 'hibrido', 'clt', 'aberta', DATE(@agora) + INTERVAL 17 DAY, @agora - INTERVAL 4320 MINUTE, @agora - INTERVAL 4320 MINUTE);
SET @v_com = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_rita, 'Auxiliar de Arquivo e Documentação', 'Digitalização, indexação e organização do arquivo morto da empresa durante projeto de seis meses.', 'Ensino médio completo, organização, atenção a detalhes, disponibilidade imediata.', 'São Paulo - SP', 'presencial', 'temporario', 'aberta', DATE(@agora) + INTERVAL 10 DAY, @agora - INTERVAL 2880 MINUTE, @agora - INTERVAL 2880 MINUTE);
SET @v_arq = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_paulo, 'Analista Fiscal', 'Apuração de impostos (ICMS, ISS, PIS e COFINS), conferência de notas fiscais, SPED e atendimento a auditorias, em regime de prestação de serviço.', 'Experiência com apuração fiscal e SPED, conhecimento de Lucro Presumido e Real, CNPJ ativo.', 'Remoto', 'remoto', 'pj', 'aberta', DATE(@agora) + INTERVAL 26 DAY, @agora - INTERVAL 23040 MINUTE, @agora - INTERVAL 23040 MINUTE);
SET @v_fisc = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_marina, 'Analista de Facilities', 'Gestão de contratos de limpeza, segurança e manutenção, controle de chamados e organização do espaço do escritório.', 'Experiência em facilities ou serviços gerais, gestão de fornecedores, Pacote Office.', 'São Paulo - SP', 'presencial', 'clt', 'rascunho', NULL, @agora - INTERVAL 1440 MINUTE, @agora - INTERVAL 1440 MINUTE);
SET @v_fac = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_rita, 'Gerente Administrativo-Financeiro', 'Responsável pelas áreas administrativa, financeira e de RH: orçamento anual, relatórios para os sócios e gestão da equipe de 12 pessoas.', 'Superior completo, pós-graduação desejável, 8 anos na área, experiência em gestão de equipes e em planejamento financeiro.', 'São Paulo - SP', 'hibrido', 'clt', 'rascunho', NULL, @agora - INTERVAL 1440 MINUTE, @agora - INTERVAL 1440 MINUTE);
SET @v_ger = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_rita, 'Assistente de Faturamento', 'Emissão de notas fiscais, conferência de pedidos e envio de faturas aos clientes. Vaga preenchida.', 'Ensino médio completo, Excel, experiência em faturamento.', 'São Paulo - SP', 'presencial', 'clt', 'encerrada', DATE(@agora) - INTERVAL 8 DAY, @agora - INTERVAL 100800 MINUTE, @agora - INTERVAL 100800 MINUTE);
SET @v_fatur = LAST_INSERT_ID();
INSERT INTO vaga (rh_id, titulo, descricao, requisitos, local, modalidade, tipo_contrato, status, prazo, criado_em, atualizado_em) VALUES
  (@u_paulo, 'Auxiliar Administrativo Temporário', 'Reforço da equipe administrativa durante o fechamento do semestre. Vaga encerrada.', 'Ensino médio completo, disponibilidade imediata.', 'Campinas - SP', 'presencial', 'temporario', 'encerrada', DATE(@agora) - INTERVAL 12 DAY, @agora - INTERVAL 86400 MINUTE, @agora - INTERVAL 86400 MINUTE);
SET @v_aux = LAST_INSERT_ID();

-- ---------------------------------------------------------------
-- curriculo, formacao, experiencia e arquivo (Diego Rocha fica sem curriculo)
-- ---------------------------------------------------------------
-- Ana Souza
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_ana, '1996-03-14', 'feminino', 'São Paulo', 'SP', '(11) 93637-3318', NULL, 'Pacote Office, Excel intermediário, organização de arquivos, atendimento telefônico, controle de agenda', 'Excel Intermediário - Fundação Bradesco', 'Profissional com experiência em rotinas administrativas: controle de documentos, agenda, atendimento a fornecedores e apoio à diretoria. Organizada e proativa.', @agora - INTERVAL 20 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Tecnologia em Processos Gerenciais', 'Centro Universitário UNA', '2014-02-01', '2018-12-01'),
  (@cur, 'Pós-graduação em Gestão Empresarial', 'Centro Universitário Senac', '2020-03-01', '2021-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Auxiliar Administrativo', 'Indústria Metalúrgica Brasul', '2016-07-01', '2020-04-01', 0, 'Apoio às rotinas do escritório: protocolo, arquivo, controle de materiais e atendimento a fornecedores.'),
  (@cur, 'Assistente Administrativo', 'Comercial Atlântico', '2020-05-01', '2022-08-01', 0, 'Controle de contratos e documentos, agenda da gerência, emissão de relatórios e conferência de pedidos.'),
  (@cur, 'Recepcionista', 'Instituto Educar Mais', '2022-09-01', NULL, 1, 'Recepção de visitantes, triagem de ligações e agendamento de reuniões.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Ana Souza.pdf', 'seed-cv-ana.pdf', 'application/pdf', 656, @agora - INTERVAL 26 DAY);

-- Bruno Lima
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_bruno, '1992-07-02', 'masculino', 'Campinas', 'SP', '(19) 99466-2365', 'https://www.linkedin.com/in/bruno-lima', 'Pacote Office, Excel intermediário, organização de arquivos, atendimento telefônico, controle de agenda', 'Excel Intermediário - Fundação Bradesco
Rotinas Administrativas - Senac', 'Profissional com experiência em rotinas administrativas: controle de documentos, agenda, atendimento a fornecedores e apoio à diretoria. Organizada e proativa.', @agora - INTERVAL 27 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Administração', 'Universidade Paulista (UNIP)', '2010-02-01', '2014-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Auxiliar Administrativo', 'Construtora Vale Verde', '2012-05-01', '2017-06-01', 0, 'Apoio às rotinas do escritório: protocolo, arquivo, controle de materiais e atendimento a fornecedores.'),
  (@cur, 'Assistente Administrativo', 'Grupo Alvorada Serviços', '2017-07-01', '2021-12-01', 0, 'Controle de contratos e documentos, agenda da gerência, emissão de relatórios e conferência de pedidos.'),
  (@cur, 'Recepcionista', 'Distribuidora Santa Clara', '2022-01-01', '2026-05-01', 0, 'Recepção de visitantes, triagem de ligações e agendamento de reuniões.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Bruno Lima.pdf', 'seed-cv-bruno.pdf', 'application/pdf', 656, @agora - INTERVAL 27 DAY);

-- Carla Mendes
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_carla, '1989-11-21', 'feminino', 'Curitiba', 'PR', '(41) 94069-7280', NULL, 'Excel avançado, fluxo de caixa, conciliação bancária, ERP (Totvs, Omie), relatórios gerenciais', 'Finanças para Não Financeiros - FGV', 'Profissional da área financeira com vivência em fluxo de caixa, conciliações e relatórios gerenciais. Foco em precisão e prazos.', @agora - INTERVAL 17 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Ciências Contábeis', 'Universidade Paulista (UNIP)', '2007-02-01', '2011-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente Financeiro', 'Grupo Alvorada Serviços', '2009-02-01', '2015-11-01', 0, 'Contas a pagar e receber, conciliação bancária e apoio no fechamento mensal.'),
  (@cur, 'Analista Financeiro', 'Instituto Educar Mais', '2015-12-01', '2020-04-01', 0, 'Fluxo de caixa diário, projeções e relatórios de resultado para a diretoria.'),
  (@cur, 'Auxiliar Financeiro', 'Clínica Bem Viver', '2020-05-01', NULL, 1, 'Lançamentos, emissão de boletos e controle de inadimplência.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Carla Mendes.pdf', 'seed-cv-carla.pdf', 'application/pdf', 656, @agora - INTERVAL 19 DAY);

-- Diego Rocha: sem curriculo (exercita a tela de primeiro cadastro)

-- Elisa Prado
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_elisa, '1994-05-09', 'feminino', 'São Paulo', 'SP', '(11) 96537-5804', NULL, 'Pacote Office, Excel intermediário, organização de arquivos, atendimento telefônico, controle de agenda', 'Excel Intermediário - Fundação Bradesco', 'Profissional com experiência em rotinas administrativas: controle de documentos, agenda, atendimento a fornecedores e apoio à diretoria. Organizada e proativa.', @agora - INTERVAL 10 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Gestão Empresarial', 'Universidade São Judas', '2012-02-01', '2016-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Auxiliar Administrativo', 'Distribuidora Santa Clara', '2014-01-01', '2018-08-01', 0, 'Apoio às rotinas do escritório: protocolo, arquivo, controle de materiais e atendimento a fornecedores.'),
  (@cur, 'Assistente Administrativo', 'Instituto Educar Mais', '2018-09-01', '2026-08-01', 0, 'Controle de contratos e documentos, agenda da gerência, emissão de relatórios e conferência de pedidos.');

-- Felipe Andrade
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_felipe, '1998-01-30', 'masculino', 'Belo Horizonte', 'MG', '(31) 95095-1953', 'https://www.linkedin.com/in/felipe-andrade', 'Pacote Office, Excel intermediário, organização de arquivos, atendimento telefônico, controle de agenda', 'Excel Intermediário - Fundação Bradesco', 'Profissional com experiência em rotinas administrativas: controle de documentos, agenda, atendimento a fornecedores e apoio à diretoria. Organizada e proativa.', @agora - INTERVAL 26 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Administração', 'Universidade Federal do Paraná', '2016-02-01', '2020-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Auxiliar Administrativo', 'Logística Rota Sul', '2018-04-01', '2023-06-01', 0, 'Apoio às rotinas do escritório: protocolo, arquivo, controle de materiais e atendimento a fornecedores.'),
  (@cur, 'Assistente Administrativo', 'Imobiliária Horizonte', '2023-07-01', NULL, 1, 'Controle de contratos e documentos, agenda da gerência, emissão de relatórios e conferência de pedidos.');

-- Gabriela Torres
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_gabriela, '2000-08-17', 'feminino', 'São Paulo', 'SP', '(11) 97433-5227', 'https://www.linkedin.com/in/gabriela-torres', 'Triagem de currículos, entrevista por competências, divulgação de vagas, LinkedIn Recruiter, testes de seleção', 'Recrutamento e Seleção - Senac', 'Profissional de Recrutamento e Seleção com boa escuta e comunicação. Experiência em triagem, entrevistas e apoio à admissão.', @agora - INTERVAL 19 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Gestão de Recursos Humanos', 'Universidade Federal do Paraná', '2018-02-01', '2022-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Estagiária de RH', 'Instituto Educar Mais', '2020-05-01', '2023-04-01', 0, 'Triagem de currículos, agendamento de entrevistas e apoio em processos seletivos.'),
  (@cur, 'Assistente de Recrutamento', 'Grupo Alvorada Serviços', '2023-05-01', NULL, 1, 'Divulgação de vagas, entrevistas iniciais e acompanhamento de candidatos.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Gabriela Torres.pdf', 'seed-cv-gabriela.pdf', 'application/pdf', 656, @agora - INTERVAL 16 DAY);

-- Henrique Duarte
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_henrique, '1985-04-25', 'masculino', 'Campinas', 'SP', '(19) 97668-4548', NULL, 'Excel avançado, fluxo de caixa, conciliação bancária, ERP (Totvs, Omie), relatórios gerenciais', 'Finanças para Não Financeiros - FGV
Excel Avançado - Impacta', 'Profissional da área financeira com vivência em fluxo de caixa, conciliações e relatórios gerenciais. Foco em precisão e prazos.', @agora - INTERVAL 22 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Ciências Contábeis', 'Universidade São Judas', '2003-02-01', '2007-12-01'),
  (@cur, 'Pós-graduação em Gestão de Pessoas', 'Estácio', '2009-03-01', '2010-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente Financeiro', 'Seguradora Nova Aliança', '2005-04-01', '2010-12-01', 0, 'Contas a pagar e receber, conciliação bancária e apoio no fechamento mensal.'),
  (@cur, 'Analista Financeiro', 'Grupo Alvorada Serviços', '2011-01-01', '2019-08-01', 0, 'Fluxo de caixa diário, projeções e relatórios de resultado para a diretoria.'),
  (@cur, 'Auxiliar Financeiro', 'Indústria Metalúrgica Brasul', '2019-09-01', '2026-05-01', 0, 'Lançamentos, emissão de boletos e controle de inadimplência.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Henrique Duarte.pdf', 'seed-cv-henrique.pdf', 'application/pdf', 656, @agora - INTERVAL 15 DAY);

-- Isabela Ferraz
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_isabela, '1994-10-06', 'feminino', 'Curitiba', 'PR', '(41) 95908-7727', NULL, 'Triagem de currículos, entrevista por competências, divulgação de vagas, LinkedIn Recruiter, testes de seleção', 'Recrutamento e Seleção - Senac
Entrevista por Competências - Fundação Bradesco', 'Profissional de Recrutamento e Seleção com boa escuta e comunicação. Experiência em triagem, entrevistas e apoio à admissão.', @agora - INTERVAL 39 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Administração', 'Centro Universitário UNA', '2012-02-01', '2016-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Estagiária de RH', 'Instituto Educar Mais', '2014-07-01', '2017-11-01', 0, 'Triagem de currículos, agendamento de entrevistas e apoio em processos seletivos.'),
  (@cur, 'Assistente de Recrutamento', 'Grupo Alvorada Serviços', '2017-12-01', '2021-09-01', 0, 'Divulgação de vagas, entrevistas iniciais e acompanhamento de candidatos.'),
  (@cur, 'Analista de Recrutamento e Seleção', 'Construtora Vale Verde', '2021-10-01', NULL, 1, 'Condução de processos seletivos de ponta a ponta e relação com gestores.');

-- João Pedro Martins
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_joao, '2003-12-03', 'masculino', 'São Paulo', 'SP', '(11) 95480-7571', 'https://www.linkedin.com/in/joao-martins', 'Excel básico, Word, PowerPoint, comunicação, trabalho em equipe', 'Excel Básico - Fundação Bradesco', 'Estudante em busca de oportunidade de estágio para aplicar a teoria e aprender a rotina de um escritório. Dedicação e vontade de crescer.', @agora - INTERVAL 21 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Administração', 'Centro Universitário UNA', '2023-02-01', NULL);

-- Karina Alves
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_karina, '1990-06-19', 'feminino', 'Belo Horizonte', 'MG', '(31) 97988-7422', NULL, 'Folha de pagamento, eSocial, legislação trabalhista, admissões e rescisões, benefícios, Excel', 'eSocial na Prática - Sescon', 'Profissional de Departamento Pessoal com experiência em folha, admissões e obrigações acessórias. Atendimento cuidadoso às dúvidas dos colaboradores.', @agora - INTERVAL 34 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Psicologia', 'Universidade Positivo', '2008-02-01', '2012-12-01'),
  (@cur, 'Pós-graduação em Gestão de Pessoas', 'Universidade Positivo', '2014-03-01', '2015-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Auxiliar de Departamento Pessoal', 'Imobiliária Horizonte', '2010-02-01', '2019-05-01', 0, 'Admissões, controle de ponto e conferência de benefícios.'),
  (@cur, 'Assistente de Departamento Pessoal', 'Distribuidora Santa Clara', '2019-06-01', '2026-08-01', 0, 'Cálculo de folha, férias e rescisões, envio de eventos ao eSocial.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Karina Alves.pdf', 'seed-cv-karina.pdf', 'application/pdf', 656, @agora - INTERVAL 34 DAY);

-- Lucas Barbosa
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_lucas, '1997-02-11', 'masculino', 'São Paulo', 'SP', '(11) 97979-6294', 'https://www.linkedin.com/in/lucas-barbosa', 'Folha de pagamento, eSocial, legislação trabalhista, admissões e rescisões, benefícios, Excel', 'eSocial na Prática - Sescon
Rotinas de Departamento Pessoal - Senac', 'Profissional de Departamento Pessoal com experiência em folha, admissões e obrigações acessórias. Atendimento cuidadoso às dúvidas dos colaboradores.', @agora - INTERVAL 27 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Administração', 'Universidade Positivo', '2015-02-01', '2019-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Auxiliar de Departamento Pessoal', 'Indústria Metalúrgica Brasul', '2017-05-01', '2022-06-01', 0, 'Admissões, controle de ponto e conferência de benefícios.'),
  (@cur, 'Assistente de Departamento Pessoal', 'Clínica Bem Viver', '2022-07-01', '2026-04-01', 0, 'Cálculo de folha, férias e rescisões, envio de eventos ao eSocial.');

-- Mariana Cardoso
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_mariana, '1993-09-28', 'feminino', 'Campinas', 'SP', '(19) 97214-8511', NULL, 'Folha de pagamento, eSocial, legislação trabalhista, admissões e rescisões, benefícios, Excel', 'eSocial na Prática - Sescon', 'Profissional de Departamento Pessoal com experiência em folha, admissões e obrigações acessórias. Atendimento cuidadoso às dúvidas dos colaboradores.', @agora - INTERVAL 26 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Administração', 'Centro Universitário UNA', '2011-02-01', '2015-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Auxiliar de Departamento Pessoal', 'Logística Rota Sul', '2013-07-01', '2018-11-01', 0, 'Admissões, controle de ponto e conferência de benefícios.'),
  (@cur, 'Assistente de Departamento Pessoal', 'Indústria Metalúrgica Brasul', '2018-12-01', NULL, 1, 'Cálculo de folha, férias e rescisões, envio de eventos ao eSocial.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Mariana Cardoso.pdf', 'seed-cv-mariana.pdf', 'application/pdf', 656, @agora - INTERVAL 35 DAY);

-- Nicolas Teixeira
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_nicolas, '1987-03-08', 'masculino', 'Curitiba', 'PR', '(41) 96708-6918', 'https://www.linkedin.com/in/nicolas-teixeira', 'Cotações, negociação, cadastro de fornecedores, ERP, Excel intermediário, controle de estoque', 'Gestão de Compras - FGV
Negociação - Sebrae', 'Profissional de suprimentos com experiência em cotações, negociação e relacionamento com fornecedores. Orientado a resultado e economia.', @agora - INTERVAL 25 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Gestão Comercial', 'FATEC', '2005-02-01', '2009-12-01'),
  (@cur, 'Pós-graduação em Controladoria', 'Universidade Federal do Paraná', '2011-03-01', '2012-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente de Compras', 'Indústria Metalúrgica Brasul', '2007-03-01', '2012-06-01', 0, 'Cotações, pedidos e acompanhamento de entregas.'),
  (@cur, 'Comprador', 'Logística Rota Sul', '2012-07-01', '2019-01-01', 0, 'Negociação com fornecedores, contratos de fornecimento e redução de custos.'),
  (@cur, 'Auxiliar de Suprimentos', 'Construtora Vale Verde', '2019-02-01', '2026-04-01', 0, 'Cadastro de fornecedores, conferência de notas e controle de estoque.');

-- Olívia Ramos
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_olivia, '1999-07-22', 'feminino', 'São Paulo', 'SP', '(11) 98736-3601', 'https://www.linkedin.com/in/olivia-ramos', 'Atendimento ao público, central telefônica, controle de acesso, agenda de salas, inglês básico', 'Atendimento ao Cliente - Senac', 'Profissional de atendimento com boa comunicação e postura corporativa. Experiência em recepção de clientes e apoio administrativo.', @agora - INTERVAL 29 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Administração', 'Universidade Paulista (UNIP)', '2017-02-01', '2021-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Recepcionista', 'Grupo Alvorada Serviços', '2019-07-01', '2026-07-01', 0, 'Recepção de clientes, controle de visitantes e atendimento telefônico.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Olívia Ramos.pdf', 'seed-cv-olivia.pdf', 'application/pdf', 656, @agora - INTERVAL 22 DAY);

-- Pedro Henrique Gomes
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_pedro, '1991-12-14', 'masculino', 'Belo Horizonte', 'MG', '(31) 95343-8380', 'https://www.linkedin.com/in/pedro-gomes', 'Elaboração de propostas, CRM (RD Station, Pipedrive), Excel, atendimento ao cliente, pós-venda', 'Vendas Consultivas - Sebrae
CRM na Prática - RD University', 'Profissional comercial com experiência em propostas, CRM e suporte a vendas. Boa comunicação e foco no cliente.', @agora - INTERVAL 33 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Administração', 'Universidade Paulista (UNIP)', '2009-02-01', '2013-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente Comercial', 'Imobiliária Horizonte', '2011-03-01', '2015-09-01', 0, 'Propostas, atualização do CRM e acompanhamento de pedidos.'),
  (@cur, 'Vendedor Interno', 'Comercial Atlântico', '2015-10-01', '2022-03-01', 0, 'Prospecção ativa, atendimento a leads e fechamento de pedidos por telefone.'),
  (@cur, 'Auxiliar de Vendas', 'Grupo Alvorada Serviços', '2022-04-01', NULL, 1, 'Apoio à equipe comercial e controle de pedidos.');

-- Renata Siqueira
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_renata, '1986-01-05', 'feminino', 'São Paulo', 'SP', '(11) 97279-1001', 'https://www.linkedin.com/in/renata-siqueira', 'Excel avançado, fluxo de caixa, conciliação bancária, ERP (Totvs, Omie), relatórios gerenciais', 'Finanças para Não Financeiros - FGV', 'Profissional da área financeira com vivência em fluxo de caixa, conciliações e relatórios gerenciais. Foco em precisão e prazos.', @agora - INTERVAL 6 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Administração', 'Universidade Federal do Paraná', '2004-02-01', '2008-12-01'),
  (@cur, 'Pós-graduação em Controladoria', 'Universidade Positivo', '2010-03-01', '2011-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente Financeiro', 'Transportes Planalto', '2006-04-01', '2013-03-01', 0, 'Contas a pagar e receber, conciliação bancária e apoio no fechamento mensal.'),
  (@cur, 'Analista Financeiro', 'Logística Rota Sul', '2013-04-01', '2019-05-01', 0, 'Fluxo de caixa diário, projeções e relatórios de resultado para a diretoria.'),
  (@cur, 'Auxiliar Financeiro', 'Escritório Contábil Prisma', '2019-06-01', NULL, 1, 'Lançamentos, emissão de boletos e controle de inadimplência.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Renata Siqueira.pdf', 'seed-cv-renata.pdf', 'application/pdf', 656, @agora - INTERVAL 19 DAY);

-- Sérgio Vasconcelos
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_sergio, '1980-08-31', 'masculino', 'Campinas', 'SP', '(19) 99414-6487', 'https://www.linkedin.com/in/sergio-vasconcelos', 'Liderança de equipes, gestão de contratos, orçamento, indicadores, melhoria de processos, Excel avançado', 'Gestão de Equipes - FGV
Gestão de Contratos - Fundação Getulio Vargas', 'Profissional com mais de dez anos em rotinas administrativas, os últimos anos liderando equipes e contratos de serviço. Foco em processos e pessoas.', @agora - INTERVAL 25 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Administração', 'Centro Universitário Senac', '1998-02-01', '2002-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente Administrativo', 'Grupo Alvorada Serviços', '2000-05-01', '2008-12-01', 0, 'Rotinas administrativas e apoio à gerência.'),
  (@cur, 'Supervisor Administrativo', 'Indústria Metalúrgica Brasul', '2009-01-01', '2019-11-01', 0, 'Supervisão de equipe de 4 pessoas, compras e contratos de serviços.'),
  (@cur, 'Coordenador Administrativo', 'Escritório Contábil Prisma', '2019-12-01', NULL, 1, 'Gestão de equipe de 8 pessoas, orçamento do escritório e fornecedores.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Sérgio Vasconcelos.pdf', 'seed-cv-sergio.pdf', 'application/pdf', 656, @agora - INTERVAL 39 DAY);

-- Tatiane Moreira
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_tatiane, '1995-04-16', 'feminino', 'Curitiba', 'PR', '(41) 99869-6869', 'https://www.linkedin.com/in/tatiane-moreira', 'Escrituração contábil, conciliações, SPED, Excel intermediário, sistemas Domínio e Alterdata', 'Contabilidade Básica - Sebrae', 'Profissional contábil com experiência em lançamentos, conciliações e obrigações acessórias. Atenção a detalhes e comprometimento com os prazos.', @agora - INTERVAL 6 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Ciências Contábeis', 'Universidade Federal do Paraná', '2013-02-01', '2017-12-01'),
  (@cur, 'Pós-graduação em Gestão Empresarial', 'Centro Universitário Senac', '2019-03-01', '2020-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Auxiliar Contábil', 'Transportes Planalto', '2015-02-01', '2017-10-01', 0, 'Classificação de documentos, lançamentos e conciliações bancárias.'),
  (@cur, 'Assistente Contábil', 'Imobiliária Horizonte', '2017-11-01', '2022-03-01', 0, 'Escrituração, apoio no fechamento mensal e entrega de obrigações acessórias.'),
  (@cur, 'Analista Fiscal', 'Seguradora Nova Aliança', '2022-04-01', NULL, 1, 'Apuração de impostos, SPED e conferência de notas fiscais de entrada e saída.');

-- Ulisses Pinheiro
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_ulisses, '1988-10-12', 'masculino', 'São Paulo', 'SP', '(11) 99089-3590', 'https://www.linkedin.com/in/ulisses-pinheiro', 'Folha de pagamento, eSocial, legislação trabalhista, admissões e rescisões, benefícios, Excel', 'eSocial na Prática - Sescon
Rotinas de Departamento Pessoal - Senac', 'Profissional de Departamento Pessoal com experiência em folha, admissões e obrigações acessórias. Atendimento cuidadoso às dúvidas dos colaboradores.', @agora - INTERVAL 36 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Psicologia', 'Universidade São Judas', '2006-02-01', '2010-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Auxiliar de Departamento Pessoal', 'Grupo Alvorada Serviços', '2008-06-01', '2013-06-01', 0, 'Admissões, controle de ponto e conferência de benefícios.'),
  (@cur, 'Assistente de Departamento Pessoal', 'Comercial Atlântico', '2013-07-01', '2019-10-01', 0, 'Cálculo de folha, férias e rescisões, envio de eventos ao eSocial.'),
  (@cur, 'Analista de Departamento Pessoal', 'Construtora Vale Verde', '2019-11-01', NULL, 1, 'Fechamento da folha de 300 colaboradores e atendimento a auditorias trabalhistas.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Ulisses Pinheiro.pdf', 'seed-cv-ulisses.pdf', 'application/pdf', 656, @agora - INTERVAL 18 DAY);

-- Vanessa Cunha
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_vanessa, '1992-02-27', 'feminino', 'Belo Horizonte', 'MG', '(31) 98908-3266', NULL, 'Atendimento ao público, central telefônica, controle de acesso, agenda de salas, inglês básico', 'Atendimento ao Cliente - Senac
Inglês Básico - Wizard', 'Profissional de atendimento com boa comunicação e postura corporativa. Experiência em recepção de clientes e apoio administrativo.', @agora - INTERVAL 38 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Administração', 'Universidade Paulista (UNIP)', '2010-02-01', '2014-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Recepcionista', 'Logística Rota Sul', '2012-01-01', '2018-05-01', 0, 'Recepção de clientes, controle de visitantes e atendimento telefônico.'),
  (@cur, 'Atendente', 'Escritório Contábil Prisma', '2018-06-01', '2026-08-01', 0, 'Atendimento presencial e por telefone, registro de chamados e encaminhamentos.');

-- Wesley Nunes
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_wesley, '2004-05-20', 'masculino', 'São Paulo', 'SP', '(11) 97478-1705', 'https://www.linkedin.com/in/wesley-nunes', 'Excel básico, Word, PowerPoint, comunicação, trabalho em equipe', 'Excel Básico - Fundação Bradesco', 'Estudante em busca de oportunidade de estágio para aplicar a teoria e aprender a rotina de um escritório. Dedicação e vontade de crescer.', @agora - INTERVAL 40 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Ciências Contábeis', 'FATEC', '2023-02-01', NULL);

-- Yasmin Carvalho
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_yasmin, '1998-11-09', 'feminino', 'Campinas', 'SP', '(19) 97069-3580', 'https://www.linkedin.com/in/yasmin-carvalho', 'Gestão de agenda, viagens corporativas, redação em português e inglês, Pacote Office, atas de reunião', 'Secretariado Executivo - Senac', 'Secretária com experiência em gestão de agenda, viagens e comunicação com clientes, inclusive em inglês. Discrição e organização.', @agora - INTERVAL 11 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Letras - Inglês', 'Universidade São Judas', '2016-02-01', '2020-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente de Diretoria', 'Escritório Contábil Prisma', '2018-04-01', '2021-01-01', 0, 'Agenda, viagens e preparação de reuniões da diretoria.'),
  (@cur, 'Secretária Executiva', 'Logística Rota Sul', '2021-02-01', '2023-11-01', 0, 'Apoio direto a dois diretores, comunicação com clientes e redação de atas.'),
  (@cur, 'Recepcionista Bilíngue', 'Clínica Bem Viver', '2023-12-01', '2026-08-01', 0, 'Recepção de clientes estrangeiros e apoio administrativo.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Yasmin Carvalho.pdf', 'seed-cv-yasmin.pdf', 'application/pdf', 656, @agora - INTERVAL 15 DAY);

-- Rafael Monteiro
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_rafael, '1990-06-04', 'masculino', 'Curitiba', 'PR', '(41) 97512-4148', NULL, 'Escrituração contábil, conciliações, SPED, Excel intermediário, sistemas Domínio e Alterdata', 'Contabilidade Básica - Sebrae
SPED Fiscal - CRC', 'Profissional contábil com experiência em lançamentos, conciliações e obrigações acessórias. Atenção a detalhes e comprometimento com os prazos.', @agora - INTERVAL 32 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Tecnologia em Gestão Financeira', 'Universidade Federal do Paraná', '2008-02-01', '2012-12-01'),
  (@cur, 'Pós-graduação em Controladoria', 'Universidade Positivo', '2014-03-01', '2015-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Auxiliar Contábil', 'Indústria Metalúrgica Brasul', '2010-05-01', '2019-03-01', 0, 'Classificação de documentos, lançamentos e conciliações bancárias.'),
  (@cur, 'Assistente Contábil', 'Seguradora Nova Aliança', '2019-04-01', NULL, 1, 'Escrituração, apoio no fechamento mensal e entrega de obrigações acessórias.');

-- Beatriz Azevedo
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_beatriz, '1997-03-23', 'feminino', 'São Paulo', 'SP', '(11) 99618-5775', NULL, 'Gestão de agenda, viagens corporativas, redação em português e inglês, Pacote Office, atas de reunião', 'Secretariado Executivo - Senac
Inglês Avançado - Cultura Inglesa', 'Secretária com experiência em gestão de agenda, viagens e comunicação com clientes, inclusive em inglês. Discrição e organização.', @agora - INTERVAL 9 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Secretariado Executivo', 'Centro Universitário Senac', '2015-02-01', '2019-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente de Diretoria', 'Clínica Bem Viver', '2017-05-01', '2022-04-01', 0, 'Agenda, viagens e preparação de reuniões da diretoria.'),
  (@cur, 'Secretária Executiva', 'Instituto Educar Mais', '2022-05-01', NULL, 1, 'Apoio direto a dois diretores, comunicação com clientes e redação de atas.');

-- Cauã Ribeiro
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_caua, '2001-09-15', 'masculino', 'Belo Horizonte', 'MG', '(31) 99216-4260', 'https://www.linkedin.com/in/caua-ribeiro', 'Elaboração de propostas, CRM (RD Station, Pipedrive), Excel, atendimento ao cliente, pós-venda', 'Vendas Consultivas - Sebrae
CRM na Prática - RD University', 'Profissional comercial com experiência em propostas, CRM e suporte a vendas. Boa comunicação e foco no cliente.', @agora - INTERVAL 7 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Gestão Comercial', 'Centro Universitário Senac', '2019-02-01', '2023-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente Comercial', 'Logística Rota Sul', '2021-01-01', NULL, 1, 'Propostas, atualização do CRM e acompanhamento de pedidos.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Cauã Ribeiro.pdf', 'seed-cv-caua.pdf', 'application/pdf', 656, @agora - INTERVAL 23 DAY);

-- Débora Freitas
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_debora, '1984-12-01', 'feminino', 'Campinas', 'SP', '(19) 99229-4026', 'https://www.linkedin.com/in/debora-freitas', 'Gestão de agenda, viagens corporativas, redação em português e inglês, Pacote Office, atas de reunião', 'Secretariado Executivo - Senac', 'Secretária com experiência em gestão de agenda, viagens e comunicação com clientes, inclusive em inglês. Discrição e organização.', @agora - INTERVAL 31 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Letras - Inglês', 'PUC-Campinas', '2002-02-01', '2006-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente de Diretoria', 'Escritório Contábil Prisma', '2004-04-01', '2012-05-01', 0, 'Agenda, viagens e preparação de reuniões da diretoria.'),
  (@cur, 'Secretária Executiva', 'Grupo Alvorada Serviços', '2012-06-01', '2020-06-01', 0, 'Apoio direto a dois diretores, comunicação com clientes e redação de atas.'),
  (@cur, 'Recepcionista Bilíngue', 'Transportes Planalto', '2020-07-01', NULL, 1, 'Recepção de clientes estrangeiros e apoio administrativo.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Débora Freitas.pdf', 'seed-cv-debora.pdf', 'application/pdf', 656, @agora - INTERVAL 12 DAY);

-- Eduardo Salgado
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_eduardo, '1993-05-18', 'masculino', 'São Paulo', 'SP', '(11) 99807-1566', 'https://www.linkedin.com/in/eduardo-salgado', 'Excel avançado, fluxo de caixa, conciliação bancária, ERP (Totvs, Omie), relatórios gerenciais', 'Finanças para Não Financeiros - FGV
Excel Avançado - Impacta', 'Profissional da área financeira com vivência em fluxo de caixa, conciliações e relatórios gerenciais. Foco em precisão e prazos.', @agora - INTERVAL 29 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Ciências Econômicas', 'Universidade Paulista (UNIP)', '2011-02-01', '2015-12-01'),
  (@cur, 'Pós-graduação em Gestão de Pessoas', 'Universidade Positivo', '2017-03-01', '2018-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente Financeiro', 'Distribuidora Santa Clara', '2013-06-01', '2019-06-01', 0, 'Contas a pagar e receber, conciliação bancária e apoio no fechamento mensal.'),
  (@cur, 'Analista Financeiro', 'Instituto Educar Mais', '2019-07-01', '2026-05-01', 0, 'Fluxo de caixa diário, projeções e relatórios de resultado para a diretoria.');

-- Fernanda Lacerda
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_fernanda, '1991-08-07', 'feminino', 'Curitiba', 'PR', '(41) 93075-6869', 'https://www.linkedin.com/in/fernanda-lacerda', 'Escrituração contábil, conciliações, SPED, Excel intermediário, sistemas Domínio e Alterdata', 'Contabilidade Básica - Sebrae', 'Profissional contábil com experiência em lançamentos, conciliações e obrigações acessórias. Atenção a detalhes e comprometimento com os prazos.', @agora - INTERVAL 18 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Ciências Contábeis', 'PUC-Campinas', '2009-02-01', '2013-12-01'),
  (@cur, 'Pós-graduação em Gestão Empresarial', 'Universidade São Judas', '2015-03-01', '2016-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Auxiliar Contábil', 'Indústria Metalúrgica Brasul', '2011-01-01', '2016-09-01', 0, 'Classificação de documentos, lançamentos e conciliações bancárias.'),
  (@cur, 'Assistente Contábil', 'Instituto Educar Mais', '2016-10-01', '2021-11-01', 0, 'Escrituração, apoio no fechamento mensal e entrega de obrigações acessórias.'),
  (@cur, 'Analista Fiscal', 'Grupo Alvorada Serviços', '2021-12-01', NULL, 1, 'Apuração de impostos, SPED e conferência de notas fiscais de entrada e saída.');
INSERT INTO curriculo_arquivo (curriculo_id, nome_original, nome_armazenado, content_type, tamanho_bytes, enviado_em) VALUES
  (@cur, 'Currículo - Fernanda Lacerda.pdf', 'seed-cv-fernanda.pdf', 'application/pdf', 656, @agora - INTERVAL 21 DAY);

-- Otávio Brandão
INSERT INTO curriculo (usuario_id, data_nascimento, sexo, cidade, uf, numero_contato, perfil_linkedin, competencias, certificacoes, resumo, atualizado_em) VALUES
  (@u_otavio, '1989-01-26', 'masculino', 'Campinas', 'SP', '(19) 97627-2749', NULL, 'Cotações, negociação, cadastro de fornecedores, ERP, Excel intermediário, controle de estoque', 'Gestão de Compras - FGV', 'Profissional de suprimentos com experiência em cotações, negociação e relacionamento com fornecedores. Orientado a resultado e economia.', @agora - INTERVAL 28 DAY);
SET @cur = LAST_INSERT_ID();
INSERT INTO curriculo_formacao (curriculo_id, curso, instituicao, data_inicio, data_termino) VALUES
  (@cur, 'Gestão Comercial', 'Universidade São Judas', '2007-02-01', '2011-12-01'),
  (@cur, 'Pós-graduação em Controladoria', 'Universidade Federal do Paraná', '2013-03-01', '2014-12-01');
INSERT INTO curriculo_experiencia (curriculo_id, cargo, empresa, data_contratacao, data_demissao, trabalho_atual, descricao_atividades) VALUES
  (@cur, 'Assistente de Compras', 'Logística Rota Sul', '2009-03-01', '2016-05-01', 0, 'Cotações, pedidos e acompanhamento de entregas.'),
  (@cur, 'Comprador', 'Imobiliária Horizonte', '2016-06-01', '2026-04-01', 0, 'Negociação com fornecedores, contratos de fornecimento e redução de custos.');

-- ---------------------------------------------------------------
-- candidatura + historico_status + documento + funcionario + analise_ia
-- Cada bloco: candidato -> vaga (etapa atual)
-- ---------------------------------------------------------------
SET @ent_ana_adm = TIMESTAMP(DATE(@agora + INTERVAL 2 DAY), '14:00:00');
-- Ana Souza -> Assistente Administrativo (entrevista)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_ana, @v_adm, 'entrevista', @ent_ana_adm, 'confirmado', @agora - INTERVAL 7737 MINUTE, @agora - INTERVAL 13119 MINUTE);
SET @cd_ana_adm = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_ana_adm, NULL, 'inscrito', @u_ana, 'Candidatura registrada pelo portal.', @agora - INTERVAL 13119 MINUTE),
  (@cd_ana_adm, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 11210 MINUTE),
  (@cd_ana_adm, 'em_triagem', 'entrevista', @u_rita, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_ana_adm, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 7840 MINUTE);
INSERT INTO analise_ia (candidatura_id, pontuacao, resumo, data_analise) VALUES (@cd_ana_adm, 82.00, 'Boa aderência: experiência com rotinas de escritório e Excel intermediário.', @agora - INTERVAL 7840 MINUTE);

SET @ent_bruno_adm = TIMESTAMP(DATE(@agora - INTERVAL 17 DAY), '10:30:00');
-- Bruno Lima -> Assistente Administrativo (aprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_bruno, @v_adm, 'aprovado', @ent_bruno_adm, 'confirmado', @agora - INTERVAL 29283 MINUTE, @agora - INTERVAL 34807 MINUTE);
SET @cd_bruno_adm = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_bruno_adm, NULL, 'inscrito', @u_bruno, 'Candidatura registrada pelo portal.', @agora - INTERVAL 34807 MINUTE),
  (@cd_bruno_adm, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 32786 MINUTE),
  (@cd_bruno_adm, 'em_triagem', 'entrevista', @u_rita, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_bruno_adm, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 29456 MINUTE),
  (@cd_bruno_adm, 'entrevista', 'aprovado', @u_rita, 'Aprovado após a entrevista.', @agora - INTERVAL 22245 MINUTE);
INSERT INTO documento (candidatura_id, tipo, formato, status, arquivo_url, tamanho_bytes, data_envio) VALUES
  (@cd_bruno_adm, 'rg', 'pdf', 'aprovado', 'seed-doc-bruno_adm-rg.pdf', 656, @agora - INTERVAL 21607 MINUTE),
  (@cd_bruno_adm, 'cpf', 'pdf', 'aprovado', 'seed-doc-bruno_adm-cpf.pdf', 656, @agora - INTERVAL 21523 MINUTE),
  (@cd_bruno_adm, 'ctps', 'pdf', 'aprovado', 'seed-doc-bruno_adm-ctps.pdf', 656, @agora - INTERVAL 21485 MINUTE),
  (@cd_bruno_adm, 'titulo_eleitor', 'pdf', 'aprovado', 'seed-doc-bruno_adm-titulo_eleitor.pdf', 656, @agora - INTERVAL 21441 MINUTE),
  (@cd_bruno_adm, 'comprovante_residencia', 'pdf', 'pendente', 'seed-doc-bruno_adm-comprovante_residencia.pdf', 656, @agora - INTERVAL 21322 MINUTE),
  (@cd_bruno_adm, 'comprovante_escolaridade', 'pdf', 'aprovado', 'seed-doc-bruno_adm-comprovante_escolaridade.pdf', 656, @agora - INTERVAL 21209 MINUTE),
  (@cd_bruno_adm, 'foto_3x4', 'pdf', 'recusado', 'seed-doc-bruno_adm-foto_3x4.pdf', 656, @agora - INTERVAL 21126 MINUTE),
  (@cd_bruno_adm, 'pis_pasep', 'pdf', 'aprovado', 'seed-doc-bruno_adm-pis_pasep.pdf', 656, @agora - INTERVAL 21081 MINUTE),
  (@cd_bruno_adm, 'certidao_nascimento_casamento', 'pdf', 'aprovado', 'seed-doc-bruno_adm-certidao_nascimento_casamento.pdf', 656, @agora - INTERVAL 21209 MINUTE),
  (@cd_bruno_adm, 'dados_bancarios', 'pdf', 'aprovado', 'seed-doc-bruno_adm-dados_bancarios.pdf', 656, @agora - INTERVAL 20879 MINUTE);

-- Felipe Andrade -> Assistente Administrativo (em_triagem)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_felipe, @v_adm, 'em_triagem', NULL, NULL, NULL, @agora - INTERVAL 7469 MINUTE);
SET @cd_felipe_adm = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_felipe_adm, NULL, 'inscrito', @u_felipe, 'Candidatura registrada pelo portal.', @agora - INTERVAL 7469 MINUTE),
  (@cd_felipe_adm, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 5460 MINUTE);
INSERT INTO analise_ia (candidatura_id, pontuacao, resumo, data_analise) VALUES (@cd_felipe_adm, 67.00, 'Perfil júnior com boa formação; pouca vivência em controle de documentos.', @agora - INTERVAL 5460 MINUTE);

-- Gabriela Torres -> Assistente Administrativo (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_gabriela, @v_adm, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 1650 MINUTE);
SET @cd_gabriela_adm = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_gabriela_adm, NULL, 'inscrito', @u_gabriela, 'Candidatura registrada pelo portal.', @agora - INTERVAL 1650 MINUTE);

-- João Pedro Martins -> Assistente Administrativo (reprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_joao, @v_adm, 'reprovado', NULL, NULL, NULL, @agora - INTERVAL 23155 MINUTE);
SET @cd_joao_adm = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_joao_adm, NULL, 'inscrito', @u_joao, 'Candidatura registrada pelo portal.', @agora - INTERVAL 23155 MINUTE),
  (@cd_joao_adm, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 21118 MINUTE),
  (@cd_joao_adm, 'em_triagem', 'reprovado', @u_rita, 'Perfil mais indicado para estágio.', @agora - INTERVAL 17385 MINUTE);

SET @ent_carla_fin = TIMESTAMP(DATE(@agora - INTERVAL 27 DAY), '10:00:00');
-- Carla Mendes -> Analista Financeiro Pleno (contratado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_carla, @v_fin, 'contratado', @ent_carla_fin, 'confirmado', @agora - INTERVAL 43820 MINUTE, @agora - INTERVAL 49342 MINUTE);
SET @cd_carla_fin = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_carla_fin, NULL, 'inscrito', @u_carla, 'Candidatura registrada pelo portal.', @agora - INTERVAL 49342 MINUTE),
  (@cd_carla_fin, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 47353 MINUTE),
  (@cd_carla_fin, 'em_triagem', 'entrevista', @u_rita, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_carla_fin, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 44159 MINUTE),
  (@cd_carla_fin, 'entrevista', 'aprovado', @u_rita, 'Aprovado após a entrevista.', @agora - INTERVAL 36948 MINUTE),
  (@cd_carla_fin, 'aprovado', 'contratado', @u_rita, 'Contratação registrada pelo RH.', @agora - INTERVAL 25364 MINUTE);
INSERT INTO funcionario (candidatura_id, status, data_contratacao) VALUES (@cd_carla_fin, 'ativo', @agora - INTERVAL 25364 MINUTE);
INSERT INTO documento (candidatura_id, tipo, formato, status, arquivo_url, tamanho_bytes, data_envio) VALUES
  (@cd_carla_fin, 'rg', 'pdf', 'aprovado', 'seed-doc-carla_fin-rg.pdf', 656, @agora - INTERVAL 32564 MINUTE),
  (@cd_carla_fin, 'cpf', 'pdf', 'aprovado', 'seed-doc-carla_fin-cpf.pdf', 656, @agora - INTERVAL 32519 MINUTE),
  (@cd_carla_fin, 'ctps', 'pdf', 'aprovado', 'seed-doc-carla_fin-ctps.pdf', 656, @agora - INTERVAL 32474 MINUTE),
  (@cd_carla_fin, 'titulo_eleitor', 'pdf', 'aprovado', 'seed-doc-carla_fin-titulo_eleitor.pdf', 656, @agora - INTERVAL 32429 MINUTE),
  (@cd_carla_fin, 'comprovante_residencia', 'pdf', 'aprovado', 'seed-doc-carla_fin-comprovante_residencia.pdf', 656, @agora - INTERVAL 32384 MINUTE),
  (@cd_carla_fin, 'comprovante_escolaridade', 'pdf', 'aprovado', 'seed-doc-carla_fin-comprovante_escolaridade.pdf', 656, @agora - INTERVAL 32339 MINUTE),
  (@cd_carla_fin, 'foto_3x4', 'pdf', 'aprovado', 'seed-doc-carla_fin-foto_3x4.pdf', 656, @agora - INTERVAL 32294 MINUTE),
  (@cd_carla_fin, 'pis_pasep', 'pdf', 'aprovado', 'seed-doc-carla_fin-pis_pasep.pdf', 656, @agora - INTERVAL 32249 MINUTE),
  (@cd_carla_fin, 'certidao_nascimento_casamento', 'pdf', 'aprovado', 'seed-doc-carla_fin-certidao_nascimento_casamento.pdf', 656, @agora - INTERVAL 32204 MINUTE),
  (@cd_carla_fin, 'dados_bancarios', 'pdf', 'aprovado', 'seed-doc-carla_fin-dados_bancarios.pdf', 656, @agora - INTERVAL 32159 MINUTE);

SET @ent_henrique_fin = TIMESTAMP(DATE(@agora + INTERVAL 1 DAY), '10:30:00');
-- Henrique Duarte -> Analista Financeiro Pleno (entrevista)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_henrique, @v_fin, 'entrevista', @ent_henrique_fin, 'pendente', NULL, @agora - INTERVAL 14814 MINUTE);
SET @cd_henrique_fin = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_henrique_fin, NULL, 'inscrito', @u_henrique, 'Candidatura registrada pelo portal.', @agora - INTERVAL 14814 MINUTE),
  (@cd_henrique_fin, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 12927 MINUTE),
  (@cd_henrique_fin, 'em_triagem', 'entrevista', @u_rita, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_henrique_fin, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 9466 MINUTE);

-- Isabela Ferraz -> Analista Financeiro Pleno (em_triagem)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_isabela, @v_fin, 'em_triagem', NULL, NULL, NULL, @agora - INTERVAL 8764 MINUTE);
SET @cd_isabela_fin = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_isabela_fin, NULL, 'inscrito', @u_isabela, 'Candidatura registrada pelo portal.', @agora - INTERVAL 8764 MINUTE),
  (@cd_isabela_fin, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 6851 MINUTE);

SET @ent_renata_fin = TIMESTAMP(DATE(@agora - INTERVAL 19 DAY), '15:00:00');
-- Renata Siqueira -> Analista Financeiro Pleno (aprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_renata, @v_fin, 'aprovado', @ent_renata_fin, 'confirmado', @agora - INTERVAL 32200 MINUTE, @agora - INTERVAL 37603 MINUTE);
SET @cd_renata_fin = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_renata_fin, NULL, 'inscrito', @u_renata, 'Candidatura registrada pelo portal.', @agora - INTERVAL 37603 MINUTE),
  (@cd_renata_fin, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 35680 MINUTE),
  (@cd_renata_fin, 'em_triagem', 'entrevista', @u_rita, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_renata_fin, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 32387 MINUTE),
  (@cd_renata_fin, 'entrevista', 'aprovado', @u_rita, 'Aprovado após a entrevista.', @agora - INTERVAL 24985 MINUTE);
INSERT INTO documento (candidatura_id, tipo, formato, status, arquivo_url, tamanho_bytes, data_envio) VALUES
  (@cd_renata_fin, 'rg', 'pdf', 'aprovado', 'seed-doc-renata_fin-rg.pdf', 656, @agora - INTERVAL 24404 MINUTE),
  (@cd_renata_fin, 'cpf', 'pdf', 'aprovado', 'seed-doc-renata_fin-cpf.pdf', 656, @agora - INTERVAL 24235 MINUTE),
  (@cd_renata_fin, 'ctps', 'pdf', 'aprovado', 'seed-doc-renata_fin-ctps.pdf', 656, @agora - INTERVAL 24147 MINUTE),
  (@cd_renata_fin, 'titulo_eleitor', 'pdf', 'aprovado', 'seed-doc-renata_fin-titulo_eleitor.pdf', 656, @agora - INTERVAL 24105 MINUTE),
  (@cd_renata_fin, 'comprovante_residencia', 'pdf', 'aprovado', 'seed-doc-renata_fin-comprovante_residencia.pdf', 656, @agora - INTERVAL 24145 MINUTE),
  (@cd_renata_fin, 'comprovante_escolaridade', 'pdf', 'aprovado', 'seed-doc-renata_fin-comprovante_escolaridade.pdf', 656, @agora - INTERVAL 24007 MINUTE),
  (@cd_renata_fin, 'foto_3x4', 'pdf', 'aprovado', 'seed-doc-renata_fin-foto_3x4.pdf', 656, @agora - INTERVAL 24095 MINUTE),
  (@cd_renata_fin, 'pis_pasep', 'pdf', 'aprovado', 'seed-doc-renata_fin-pis_pasep.pdf', 656, @agora - INTERVAL 23911 MINUTE),
  (@cd_renata_fin, 'certidao_nascimento_casamento', 'pdf', 'aprovado', 'seed-doc-renata_fin-certidao_nascimento_casamento.pdf', 656, @agora - INTERVAL 23846 MINUTE),
  (@cd_renata_fin, 'dados_bancarios', 'pdf', 'aprovado', 'seed-doc-renata_fin-dados_bancarios.pdf', 656, @agora - INTERVAL 24197 MINUTE);

SET @ent_sergio_fin = TIMESTAMP(DATE(@agora - INTERVAL 17 DAY), '11:00:00');
-- Sérgio Vasconcelos -> Analista Financeiro Pleno (reprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_sergio, @v_fin, 'reprovado', @ent_sergio_fin, 'confirmado', @agora - INTERVAL 26693 MINUTE, @agora - INTERVAL 32160 MINUTE);
SET @cd_sergio_fin = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_sergio_fin, NULL, 'inscrito', @u_sergio, 'Candidatura registrada pelo portal.', @agora - INTERVAL 32160 MINUTE),
  (@cd_sergio_fin, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 30122 MINUTE),
  (@cd_sergio_fin, 'em_triagem', 'entrevista', @u_rita, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_sergio_fin, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 26759 MINUTE),
  (@cd_sergio_fin, 'entrevista', 'reprovado', @u_rita, 'Pretensão salarial acima da faixa da vaga.', @agora - INTERVAL 22916 MINUTE);

SET @ent_karina_dp = TIMESTAMP(DATE(@agora + INTERVAL 3 DAY), '09:00:00');
-- Karina Alves -> Analista de Departamento Pessoal (entrevista)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_karina, @v_dp, 'entrevista', @ent_karina_dp, 'pendente', NULL, @agora - INTERVAL 11815 MINUTE);
SET @cd_karina_dp = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_karina_dp, NULL, 'inscrito', @u_karina, 'Candidatura registrada pelo portal.', @agora - INTERVAL 11815 MINUTE),
  (@cd_karina_dp, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 9908 MINUTE),
  (@cd_karina_dp, 'em_triagem', 'entrevista', @u_paulo, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_karina_dp, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 6598 MINUTE);

-- Lucas Barbosa -> Analista de Departamento Pessoal (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_lucas, @v_dp, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 3373 MINUTE);
SET @cd_lucas_dp = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_lucas_dp, NULL, 'inscrito', @u_lucas, 'Candidatura registrada pelo portal.', @agora - INTERVAL 3373 MINUTE);

-- Mariana Cardoso -> Analista de Departamento Pessoal (em_triagem)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_mariana, @v_dp, 'em_triagem', NULL, NULL, NULL, @agora - INTERVAL 8738 MINUTE);
SET @cd_mariana_dp = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_mariana_dp, NULL, 'inscrito', @u_mariana, 'Candidatura registrada pelo portal.', @agora - INTERVAL 8738 MINUTE),
  (@cd_mariana_dp, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 6810 MINUTE);
INSERT INTO analise_ia (candidatura_id, pontuacao, resumo, data_analise) VALUES (@cd_mariana_dp, 88.00, 'Experiência sólida em folha e eSocial, alinhada ao que a vaga pede.', @agora - INTERVAL 6810 MINUTE);

SET @ent_ulisses_dp = TIMESTAMP(DATE(@agora - INTERVAL 13 DAY), '14:30:00');
-- Ulisses Pinheiro -> Analista de Departamento Pessoal (aprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_ulisses, @v_dp, 'aprovado', @ent_ulisses_dp, 'confirmado', @agora - INTERVAL 23303 MINUTE, @agora - INTERVAL 29182 MINUTE);
SET @cd_ulisses_dp = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_ulisses_dp, NULL, 'inscrito', @u_ulisses, 'Candidatura registrada pelo portal.', @agora - INTERVAL 29182 MINUTE),
  (@cd_ulisses_dp, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 27195 MINUTE),
  (@cd_ulisses_dp, 'em_triagem', 'entrevista', @u_paulo, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_ulisses_dp, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 23872 MINUTE),
  (@cd_ulisses_dp, 'entrevista', 'aprovado', @u_paulo, 'Aprovado após a entrevista.', @agora - INTERVAL 16595 MINUTE);
INSERT INTO documento (candidatura_id, tipo, formato, status, arquivo_url, tamanho_bytes, data_envio) VALUES
  (@cd_ulisses_dp, 'rg', 'pdf', 'aprovado', 'seed-doc-ulisses_dp-rg.pdf', 656, @agora - INTERVAL 15957 MINUTE),
  (@cd_ulisses_dp, 'cpf', 'pdf', 'aprovado', 'seed-doc-ulisses_dp-cpf.pdf', 656, @agora - INTERVAL 15925 MINUTE),
  (@cd_ulisses_dp, 'ctps', 'pdf', 'aprovado', 'seed-doc-ulisses_dp-ctps.pdf', 656, @agora - INTERVAL 15739 MINUTE),
  (@cd_ulisses_dp, 'titulo_eleitor', 'pdf', 'pendente', 'seed-doc-ulisses_dp-titulo_eleitor.pdf', 656, @agora - INTERVAL 15657 MINUTE),
  (@cd_ulisses_dp, 'comprovante_residencia', 'pdf', 'pendente', 'seed-doc-ulisses_dp-comprovante_residencia.pdf', 656, @agora - INTERVAL 15658 MINUTE),
  (@cd_ulisses_dp, 'foto_3x4', 'pdf', 'aprovado', 'seed-doc-ulisses_dp-foto_3x4.pdf', 656, @agora - INTERVAL 15498 MINUTE);

SET @ent_olivia_recep = TIMESTAMP(DATE(@agora - INTERVAL 26 DAY), '14:00:00');
-- Olívia Ramos -> Recepcionista Corporativa (contratado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_olivia, @v_recep, 'contratado', @ent_olivia_recep, 'confirmado', @agora - INTERVAL 41797 MINUTE, @agora - INTERVAL 47572 MINUTE);
SET @cd_olivia_recep = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_olivia_recep, NULL, 'inscrito', @u_olivia, 'Candidatura registrada pelo portal.', @agora - INTERVAL 47572 MINUTE),
  (@cd_olivia_recep, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 45457 MINUTE),
  (@cd_olivia_recep, 'em_triagem', 'entrevista', @u_paulo, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_olivia_recep, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 42137 MINUTE),
  (@cd_olivia_recep, 'entrevista', 'aprovado', @u_paulo, 'Aprovado após a entrevista.', @agora - INTERVAL 34852 MINUTE),
  (@cd_olivia_recep, 'aprovado', 'contratado', @u_paulo, 'Contratação registrada pelo RH.', @agora - INTERVAL 23315 MINUTE);
INSERT INTO funcionario (candidatura_id, status, data_contratacao) VALUES (@cd_olivia_recep, 'ativo', @agora - INTERVAL 23315 MINUTE);
INSERT INTO documento (candidatura_id, tipo, formato, status, arquivo_url, tamanho_bytes, data_envio) VALUES
  (@cd_olivia_recep, 'rg', 'pdf', 'aprovado', 'seed-doc-olivia_recep-rg.pdf', 656, @agora - INTERVAL 30515 MINUTE),
  (@cd_olivia_recep, 'cpf', 'pdf', 'aprovado', 'seed-doc-olivia_recep-cpf.pdf', 656, @agora - INTERVAL 30470 MINUTE),
  (@cd_olivia_recep, 'ctps', 'pdf', 'aprovado', 'seed-doc-olivia_recep-ctps.pdf', 656, @agora - INTERVAL 30425 MINUTE),
  (@cd_olivia_recep, 'titulo_eleitor', 'pdf', 'aprovado', 'seed-doc-olivia_recep-titulo_eleitor.pdf', 656, @agora - INTERVAL 30380 MINUTE),
  (@cd_olivia_recep, 'comprovante_residencia', 'pdf', 'aprovado', 'seed-doc-olivia_recep-comprovante_residencia.pdf', 656, @agora - INTERVAL 30335 MINUTE),
  (@cd_olivia_recep, 'comprovante_escolaridade', 'pdf', 'aprovado', 'seed-doc-olivia_recep-comprovante_escolaridade.pdf', 656, @agora - INTERVAL 30290 MINUTE),
  (@cd_olivia_recep, 'foto_3x4', 'pdf', 'aprovado', 'seed-doc-olivia_recep-foto_3x4.pdf', 656, @agora - INTERVAL 30245 MINUTE),
  (@cd_olivia_recep, 'pis_pasep', 'pdf', 'aprovado', 'seed-doc-olivia_recep-pis_pasep.pdf', 656, @agora - INTERVAL 30200 MINUTE),
  (@cd_olivia_recep, 'certidao_nascimento_casamento', 'pdf', 'aprovado', 'seed-doc-olivia_recep-certidao_nascimento_casamento.pdf', 656, @agora - INTERVAL 30155 MINUTE),
  (@cd_olivia_recep, 'dados_bancarios', 'pdf', 'aprovado', 'seed-doc-olivia_recep-dados_bancarios.pdf', 656, @agora - INTERVAL 30110 MINUTE);

SET @ent_yasmin_recep = TIMESTAMP(DATE(@agora - INTERVAL 1 DAY), '15:00:00');
-- Yasmin Carvalho -> Recepcionista Corporativa (entrevista)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_yasmin, @v_recep, 'entrevista', @ent_yasmin_recep, 'confirmado', @agora - INTERVAL 5132 MINUTE, @agora - INTERVAL 10532 MINUTE);
SET @cd_yasmin_recep = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_yasmin_recep, NULL, 'inscrito', @u_yasmin, 'Candidatura registrada pelo portal.', @agora - INTERVAL 10532 MINUTE),
  (@cd_yasmin_recep, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 8522 MINUTE),
  (@cd_yasmin_recep, 'em_triagem', 'entrevista', @u_paulo, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_yasmin_recep, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 5268 MINUTE);

-- Beatriz Azevedo -> Recepcionista Corporativa (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_beatriz, @v_recep, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 4595 MINUTE);
SET @cd_beatriz_recep = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_beatriz_recep, NULL, 'inscrito', @u_beatriz, 'Candidatura registrada pelo portal.', @agora - INTERVAL 4595 MINUTE);

-- Vanessa Cunha -> Recepcionista Corporativa (cancelado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_vanessa, @v_recep, 'cancelado', NULL, NULL, NULL, @agora - INTERVAL 21842 MINUTE);
SET @cd_vanessa_recep = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_vanessa_recep, NULL, 'inscrito', @u_vanessa, 'Candidatura registrada pelo portal.', @agora - INTERVAL 21842 MINUTE),
  (@cd_vanessa_recep, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 19698 MINUTE),
  (@cd_vanessa_recep, 'em_triagem', 'cancelado', @u_paulo, 'Candidata desistiu do processo.', @agora - INTERVAL 15113 MINUTE);

-- Nicolas Teixeira -> Auxiliar de Contabilidade (em_triagem)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_nicolas, @v_contab, 'em_triagem', NULL, NULL, NULL, @agora - INTERVAL 7449 MINUTE);
SET @cd_nicolas_contab = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_nicolas_contab, NULL, 'inscrito', @u_nicolas, 'Candidatura registrada pelo portal.', @agora - INTERVAL 7449 MINUTE),
  (@cd_nicolas_contab, 'inscrito', 'em_triagem', @u_marina, 'Currículo em análise pelo RH.', @agora - INTERVAL 5493 MINUTE);

SET @ent_tatiane_contab = TIMESTAMP(DATE(@agora + INTERVAL 4 DAY), '16:00:00');
-- Tatiane Moreira -> Auxiliar de Contabilidade (entrevista)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_tatiane, @v_contab, 'entrevista', @ent_tatiane_contab, 'pendente', NULL, @agora - INTERVAL 15980 MINUTE);
SET @cd_tatiane_contab = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_tatiane_contab, NULL, 'inscrito', @u_tatiane, 'Candidatura registrada pelo portal.', @agora - INTERVAL 15980 MINUTE),
  (@cd_tatiane_contab, 'inscrito', 'em_triagem', @u_marina, 'Currículo em análise pelo RH.', @agora - INTERVAL 13926 MINUTE),
  (@cd_tatiane_contab, 'em_triagem', 'entrevista', @u_marina, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_tatiane_contab, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 10536 MINUTE);

-- Rafael Monteiro -> Auxiliar de Contabilidade (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_rafael, @v_contab, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 2936 MINUTE);
SET @cd_rafael_contab = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_rafael_contab, NULL, 'inscrito', @u_rafael, 'Candidatura registrada pelo portal.', @agora - INTERVAL 2936 MINUTE);

SET @ent_fernanda_contab = TIMESTAMP(DATE(@agora - INTERVAL 12 DAY), '13:30:00');
-- Fernanda Lacerda -> Auxiliar de Contabilidade (aprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_fernanda, @v_contab, 'aprovado', @ent_fernanda_contab, 'confirmado', @agora - INTERVAL 21927 MINUTE, @agora - INTERVAL 27621 MINUTE);
SET @cd_fernanda_contab = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_fernanda_contab, NULL, 'inscrito', @u_fernanda, 'Candidatura registrada pelo portal.', @agora - INTERVAL 27621 MINUTE),
  (@cd_fernanda_contab, 'inscrito', 'em_triagem', @u_marina, 'Currículo em análise pelo RH.', @agora - INTERVAL 25630 MINUTE),
  (@cd_fernanda_contab, 'em_triagem', 'entrevista', @u_marina, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_fernanda_contab, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 22162 MINUTE),
  (@cd_fernanda_contab, 'entrevista', 'aprovado', @u_marina, 'Aprovado após a entrevista.', @agora - INTERVAL 14708 MINUTE);
INSERT INTO documento (candidatura_id, tipo, formato, status, arquivo_url, tamanho_bytes, data_envio) VALUES
  (@cd_fernanda_contab, 'rg', 'pdf', 'pendente', 'seed-doc-fernanda_contab-rg.pdf', 656, @agora - INTERVAL 14061 MINUTE),
  (@cd_fernanda_contab, 'cpf', 'pdf', 'pendente', 'seed-doc-fernanda_contab-cpf.pdf', 656, @agora - INTERVAL 14005 MINUTE),
  (@cd_fernanda_contab, 'dados_bancarios', 'pdf', 'pendente', 'seed-doc-fernanda_contab-dados_bancarios.pdf', 656, @agora - INTERVAL 13989 MINUTE);

SET @ent_pedro_cpr = TIMESTAMP(DATE(@agora + INTERVAL 5 DAY), '11:00:00');
-- Pedro Henrique Gomes -> Analista de Contas a Pagar e Receber (entrevista)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_pedro, @v_cpr, 'entrevista', @ent_pedro_cpr, 'pendente', NULL, @agora - INTERVAL 13038 MINUTE);
SET @cd_pedro_cpr = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_pedro_cpr, NULL, 'inscrito', @u_pedro, 'Candidatura registrada pelo portal.', @agora - INTERVAL 13038 MINUTE),
  (@cd_pedro_cpr, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 11123 MINUTE),
  (@cd_pedro_cpr, 'em_triagem', 'entrevista', @u_rita, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_pedro_cpr, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 7726 MINUTE);

-- Eduardo Salgado -> Analista de Contas a Pagar e Receber (em_triagem)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_eduardo, @v_cpr, 'em_triagem', NULL, NULL, NULL, @agora - INTERVAL 5841 MINUTE);
SET @cd_eduardo_cpr = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_eduardo_cpr, NULL, 'inscrito', @u_eduardo, 'Candidatura registrada pelo portal.', @agora - INTERVAL 5841 MINUTE),
  (@cd_eduardo_cpr, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 3730 MINUTE);

-- Karina Alves -> Analista de Contas a Pagar e Receber (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_karina, @v_cpr, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 1811 MINUTE);
SET @cd_karina_cpr = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_karina_cpr, NULL, 'inscrito', @u_karina, 'Candidatura registrada pelo portal.', @agora - INTERVAL 1811 MINUTE);

SET @ent_mariana_rs = TIMESTAMP(DATE(@agora + INTERVAL 2 DAY), '16:30:00');
-- Mariana Cardoso -> Assistente de Recrutamento e Seleção (entrevista)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_mariana, @v_rs, 'entrevista', @ent_mariana_rs, 'confirmado', @agora - INTERVAL 5793 MINUTE, @agora - INTERVAL 11643 MINUTE);
SET @cd_mariana_rs = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_mariana_rs, NULL, 'inscrito', @u_mariana, 'Candidatura registrada pelo portal.', @agora - INTERVAL 11643 MINUTE),
  (@cd_mariana_rs, 'inscrito', 'em_triagem', @u_marina, 'Currículo em análise pelo RH.', @agora - INTERVAL 9614 MINUTE),
  (@cd_mariana_rs, 'em_triagem', 'entrevista', @u_marina, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_mariana_rs, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 6360 MINUTE);

-- Gabriela Torres -> Assistente de Recrutamento e Seleção (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_gabriela, @v_rs, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 6237 MINUTE);
SET @cd_gabriela_rs = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_gabriela_rs, NULL, 'inscrito', @u_gabriela, 'Candidatura registrada pelo portal.', @agora - INTERVAL 6237 MINUTE);

-- Isabela Ferraz -> Assistente de Recrutamento e Seleção (reprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_isabela, @v_rs, 'reprovado', NULL, NULL, NULL, @agora - INTERVAL 18764 MINUTE);
SET @cd_isabela_rs = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_isabela_rs, NULL, 'inscrito', @u_isabela, 'Candidatura registrada pelo portal.', @agora - INTERVAL 18764 MINUTE),
  (@cd_isabela_rs, 'inscrito', 'reprovado', @u_marina, 'Experiência anterior não atende ao nível da vaga.', @agora - INTERVAL 15006 MINUTE);

-- João Pedro Martins -> Estagiário(a) de Administração (em_triagem)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_joao, @v_est, 'em_triagem', NULL, NULL, NULL, @agora - INTERVAL 8723 MINUTE);
SET @cd_joao_est = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_joao_est, NULL, 'inscrito', @u_joao, 'Candidatura registrada pelo portal.', @agora - INTERVAL 8723 MINUTE),
  (@cd_joao_est, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 6758 MINUTE);

SET @ent_wesley_est = TIMESTAMP(DATE(@agora + INTERVAL 1 DAY), '13:30:00');
-- Wesley Nunes -> Estagiário(a) de Administração (entrevista)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_wesley, @v_est, 'entrevista', @ent_wesley_est, 'pendente', NULL, @agora - INTERVAL 10344 MINUTE);
SET @cd_wesley_est = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_wesley_est, NULL, 'inscrito', @u_wesley, 'Candidatura registrada pelo portal.', @agora - INTERVAL 10344 MINUTE),
  (@cd_wesley_est, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 8425 MINUTE),
  (@cd_wesley_est, 'em_triagem', 'entrevista', @u_paulo, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_wesley_est, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 4990 MINUTE);

-- Cauã Ribeiro -> Estagiário(a) de Administração (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_caua, @v_est, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 3134 MINUTE);
SET @cd_caua_est = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_caua_est, NULL, 'inscrito', @u_caua, 'Candidatura registrada pelo portal.', @agora - INTERVAL 3134 MINUTE);

-- Yasmin Carvalho -> Secretária Executiva Bilíngue (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_yasmin, @v_sec, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 4559 MINUTE);
SET @cd_yasmin_sec = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_yasmin_sec, NULL, 'inscrito', @u_yasmin, 'Candidatura registrada pelo portal.', @agora - INTERVAL 4559 MINUTE);

-- Beatriz Azevedo -> Secretária Executiva Bilíngue (em_triagem)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_beatriz, @v_sec, 'em_triagem', NULL, NULL, NULL, @agora - INTERVAL 10369 MINUTE);
SET @cd_beatriz_sec = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_beatriz_sec, NULL, 'inscrito', @u_beatriz, 'Candidatura registrada pelo portal.', @agora - INTERVAL 10369 MINUTE),
  (@cd_beatriz_sec, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 8375 MINUTE);

-- Débora Freitas -> Secretária Executiva Bilíngue (reprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_debora, @v_sec, 'reprovado', NULL, NULL, NULL, @agora - INTERVAL 26212 MINUTE);
SET @cd_debora_sec = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_debora_sec, NULL, 'inscrito', @u_debora, 'Candidatura registrada pelo portal.', @agora - INTERVAL 26212 MINUTE),
  (@cd_debora_sec, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 24129 MINUTE),
  (@cd_debora_sec, 'em_triagem', 'reprovado', @u_rita, 'Inglês abaixo do exigido para a função.', @agora - INTERVAL 20369 MINUTE);

SET @ent_nicolas_comp = TIMESTAMP(DATE(@agora - INTERVAL 4 DAY), '09:30:00');
-- Nicolas Teixeira -> Analista de Compras (aprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_nicolas, @v_comp, 'aprovado', @ent_nicolas_comp, 'confirmado', @agora - INTERVAL 10201 MINUTE, @agora - INTERVAL 15918 MINUTE);
SET @cd_nicolas_comp = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_nicolas_comp, NULL, 'inscrito', @u_nicolas, 'Candidatura registrada pelo portal.', @agora - INTERVAL 15918 MINUTE),
  (@cd_nicolas_comp, 'inscrito', 'em_triagem', @u_marina, 'Currículo em análise pelo RH.', @agora - INTERVAL 13823 MINUTE),
  (@cd_nicolas_comp, 'em_triagem', 'entrevista', @u_marina, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_nicolas_comp, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 10480 MINUTE),
  (@cd_nicolas_comp, 'entrevista', 'aprovado', @u_marina, 'Aprovado após a entrevista.', @agora - INTERVAL 3067 MINUTE);

-- Rafael Monteiro -> Analista de Compras (em_triagem)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_rafael, @v_comp, 'em_triagem', NULL, NULL, NULL, @agora - INTERVAL 7238 MINUTE);
SET @cd_rafael_comp = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_rafael_comp, NULL, 'inscrito', @u_rafael, 'Candidatura registrada pelo portal.', @agora - INTERVAL 7238 MINUTE),
  (@cd_rafael_comp, 'inscrito', 'em_triagem', @u_marina, 'Currículo em análise pelo RH.', @agora - INTERVAL 5232 MINUTE);

-- Eduardo Salgado -> Analista de Compras (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_eduardo, @v_comp, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 2936 MINUTE);
SET @cd_eduardo_comp = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_eduardo_comp, NULL, 'inscrito', @u_eduardo, 'Candidatura registrada pelo portal.', @agora - INTERVAL 2936 MINUTE);

-- Henrique Duarte -> Coordenador(a) Administrativo (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_henrique, @v_coord, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 1589 MINUTE);
SET @cd_henrique_coord = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_henrique_coord, NULL, 'inscrito', @u_henrique, 'Candidatura registrada pelo portal.', @agora - INTERVAL 1589 MINUTE);

-- Sérgio Vasconcelos -> Coordenador(a) Administrativo (em_triagem)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_sergio, @v_coord, 'em_triagem', NULL, NULL, NULL, @agora - INTERVAL 3192 MINUTE);
SET @cd_sergio_coord = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_sergio_coord, NULL, 'inscrito', @u_sergio, 'Candidatura registrada pelo portal.', @agora - INTERVAL 3192 MINUTE),
  (@cd_sergio_coord, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 1030 MINUTE);
INSERT INTO analise_ia (candidatura_id, pontuacao, resumo, data_analise) VALUES (@cd_sergio_coord, 91.00, 'Liderança de equipes e gestão de contratos: perfil muito aderente à posição.', @agora - INTERVAL 1030 MINUTE);

-- Ulisses Pinheiro -> Coordenador(a) Administrativo (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_ulisses, @v_coord, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 1759 MINUTE);
SET @cd_ulisses_coord = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_ulisses_coord, NULL, 'inscrito', @u_ulisses, 'Candidatura registrada pelo portal.', @agora - INTERVAL 1759 MINUTE);

-- Cauã Ribeiro -> Assistente Comercial (em_triagem)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_caua, @v_com, 'em_triagem', NULL, NULL, NULL, @agora - INTERVAL 3071 MINUTE);
SET @cd_caua_com = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_caua_com, NULL, 'inscrito', @u_caua, 'Candidatura registrada pelo portal.', @agora - INTERVAL 3071 MINUTE),
  (@cd_caua_com, 'inscrito', 'em_triagem', @u_marina, 'Currículo em análise pelo RH.', @agora - INTERVAL 1184 MINUTE);

-- Felipe Andrade -> Assistente Comercial (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_felipe, @v_com, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 1775 MINUTE);
SET @cd_felipe_com = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_felipe_com, NULL, 'inscrito', @u_felipe, 'Candidatura registrada pelo portal.', @agora - INTERVAL 1775 MINUTE);

-- Pedro Henrique Gomes -> Assistente Comercial (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_pedro, @v_com, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 3022 MINUTE);
SET @cd_pedro_com = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_pedro_com, NULL, 'inscrito', @u_pedro, 'Candidatura registrada pelo portal.', @agora - INTERVAL 3022 MINUTE);

-- Wesley Nunes -> Auxiliar de Arquivo e Documentação (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_wesley, @v_arq, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 1485 MINUTE);
SET @cd_wesley_arq = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_wesley_arq, NULL, 'inscrito', @u_wesley, 'Candidatura registrada pelo portal.', @agora - INTERVAL 1485 MINUTE);

-- Vanessa Cunha -> Auxiliar de Arquivo e Documentação (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_vanessa, @v_arq, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 1631 MINUTE);
SET @cd_vanessa_arq = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_vanessa_arq, NULL, 'inscrito', @u_vanessa, 'Candidatura registrada pelo portal.', @agora - INTERVAL 1631 MINUTE);

-- Fernanda Lacerda -> Analista Fiscal (inscrito)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_fernanda, @v_fisc, 'inscrito', NULL, NULL, NULL, @agora - INTERVAL 4391 MINUTE);
SET @cd_fernanda_fisc = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_fernanda_fisc, NULL, 'inscrito', @u_fernanda, 'Candidatura registrada pelo portal.', @agora - INTERVAL 4391 MINUTE);

-- Tatiane Moreira -> Analista Fiscal (reprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_tatiane, @v_fisc, 'reprovado', NULL, NULL, NULL, @agora - INTERVAL 14497 MINUTE);
SET @cd_tatiane_fisc = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_tatiane_fisc, NULL, 'inscrito', @u_tatiane, 'Candidatura registrada pelo portal.', @agora - INTERVAL 14497 MINUTE),
  (@cd_tatiane_fisc, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 12490 MINUTE),
  (@cd_tatiane_fisc, 'em_triagem', 'reprovado', @u_paulo, 'Sem experiência com Lucro Real.', @agora - INTERVAL 8707 MINUTE);

SET @ent_lucas_fatur = TIMESTAMP(DATE(@agora - INTERVAL 55 DAY), '11:30:00');
-- Lucas Barbosa -> Assistente de Faturamento (contratado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_lucas, @v_fatur, 'contratado', @ent_lucas_fatur, 'confirmado', @agora - INTERVAL 83941 MINUTE, @agora - INTERVAL 89691 MINUTE);
SET @cd_lucas_fatur = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_lucas_fatur, NULL, 'inscrito', @u_lucas, 'Candidatura registrada pelo portal.', @agora - INTERVAL 89691 MINUTE),
  (@cd_lucas_fatur, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 87569 MINUTE),
  (@cd_lucas_fatur, 'em_triagem', 'entrevista', @u_rita, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_lucas_fatur, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 84207 MINUTE),
  (@cd_lucas_fatur, 'entrevista', 'aprovado', @u_rita, 'Aprovado após a entrevista.', @agora - INTERVAL 76745 MINUTE),
  (@cd_lucas_fatur, 'aprovado', 'contratado', @u_rita, 'Contratação registrada pelo RH.', @agora - INTERVAL 65100 MINUTE);
INSERT INTO funcionario (candidatura_id, status, data_contratacao) VALUES (@cd_lucas_fatur, 'inativo', @agora - INTERVAL 65100 MINUTE);
INSERT INTO documento (candidatura_id, tipo, formato, status, arquivo_url, tamanho_bytes, data_envio) VALUES
  (@cd_lucas_fatur, 'rg', 'pdf', 'aprovado', 'seed-doc-lucas_fatur-rg.pdf', 656, @agora - INTERVAL 72300 MINUTE),
  (@cd_lucas_fatur, 'cpf', 'pdf', 'aprovado', 'seed-doc-lucas_fatur-cpf.pdf', 656, @agora - INTERVAL 72255 MINUTE),
  (@cd_lucas_fatur, 'ctps', 'pdf', 'aprovado', 'seed-doc-lucas_fatur-ctps.pdf', 656, @agora - INTERVAL 72210 MINUTE),
  (@cd_lucas_fatur, 'titulo_eleitor', 'pdf', 'aprovado', 'seed-doc-lucas_fatur-titulo_eleitor.pdf', 656, @agora - INTERVAL 72165 MINUTE),
  (@cd_lucas_fatur, 'comprovante_residencia', 'pdf', 'aprovado', 'seed-doc-lucas_fatur-comprovante_residencia.pdf', 656, @agora - INTERVAL 72120 MINUTE),
  (@cd_lucas_fatur, 'comprovante_escolaridade', 'pdf', 'aprovado', 'seed-doc-lucas_fatur-comprovante_escolaridade.pdf', 656, @agora - INTERVAL 72075 MINUTE),
  (@cd_lucas_fatur, 'foto_3x4', 'pdf', 'aprovado', 'seed-doc-lucas_fatur-foto_3x4.pdf', 656, @agora - INTERVAL 72030 MINUTE),
  (@cd_lucas_fatur, 'pis_pasep', 'pdf', 'aprovado', 'seed-doc-lucas_fatur-pis_pasep.pdf', 656, @agora - INTERVAL 71985 MINUTE),
  (@cd_lucas_fatur, 'certidao_nascimento_casamento', 'pdf', 'aprovado', 'seed-doc-lucas_fatur-certidao_nascimento_casamento.pdf', 656, @agora - INTERVAL 71940 MINUTE),
  (@cd_lucas_fatur, 'dados_bancarios', 'pdf', 'aprovado', 'seed-doc-lucas_fatur-dados_bancarios.pdf', 656, @agora - INTERVAL 71895 MINUTE),
  (@cd_lucas_fatur, 'certificado_reservista', 'pdf', 'aprovado', 'seed-doc-lucas_fatur-certificado_reservista.pdf', 656, @agora - INTERVAL 71850 MINUTE);

-- Bruno Lima -> Assistente de Faturamento (reprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_bruno, @v_fatur, 'reprovado', NULL, NULL, NULL, @agora - INTERVAL 79461 MINUTE);
SET @cd_bruno_fatur = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_bruno_fatur, NULL, 'inscrito', @u_bruno, 'Candidatura registrada pelo portal.', @agora - INTERVAL 79461 MINUTE),
  (@cd_bruno_fatur, 'inscrito', 'em_triagem', @u_rita, 'Currículo em análise pelo RH.', @agora - INTERVAL 77445 MINUTE),
  (@cd_bruno_fatur, 'em_triagem', 'reprovado', @u_rita, 'Vaga preenchida por outro candidato.', @agora - INTERVAL 73736 MINUTE);

-- Renata Siqueira -> Assistente de Faturamento (cancelado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_renata, @v_fatur, 'cancelado', NULL, NULL, NULL, @agora - INTERVAL 75146 MINUTE);
SET @cd_renata_fatur = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_renata_fatur, NULL, 'inscrito', @u_renata, 'Candidatura registrada pelo portal.', @agora - INTERVAL 75146 MINUTE),
  (@cd_renata_fatur, 'inscrito', 'cancelado', @u_rita, 'Candidata aceitou outra proposta.', @agora - INTERVAL 70681 MINUTE);

SET @ent_debora_aux = TIMESTAMP(DATE(@agora - INTERVAL 48 DAY), '15:30:00');
-- Débora Freitas -> Auxiliar Administrativo Temporário (contratado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_debora, @v_aux, 'contratado', @ent_debora_aux, 'confirmado', @agora - INTERVAL 72015 MINUTE, @agora - INTERVAL 77828 MINUTE);
SET @cd_debora_aux = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_debora_aux, NULL, 'inscrito', @u_debora, 'Candidatura registrada pelo portal.', @agora - INTERVAL 77828 MINUTE),
  (@cd_debora_aux, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 75784 MINUTE),
  (@cd_debora_aux, 'em_triagem', 'entrevista', @u_paulo, CONCAT('Entrevista agendada para ', DATE_FORMAT(@ent_debora_aux, '%d/%m/%Y às %H:%i'), '.'), @agora - INTERVAL 72475 MINUTE),
  (@cd_debora_aux, 'entrevista', 'aprovado', @u_paulo, 'Aprovado após a entrevista.', @agora - INTERVAL 65217 MINUTE),
  (@cd_debora_aux, 'aprovado', 'contratado', @u_paulo, 'Contratação registrada pelo RH.', @agora - INTERVAL 53583 MINUTE);
INSERT INTO funcionario (candidatura_id, status, data_contratacao) VALUES (@cd_debora_aux, 'ativo', @agora - INTERVAL 53583 MINUTE);
INSERT INTO documento (candidatura_id, tipo, formato, status, arquivo_url, tamanho_bytes, data_envio) VALUES
  (@cd_debora_aux, 'rg', 'pdf', 'aprovado', 'seed-doc-debora_aux-rg.pdf', 656, @agora - INTERVAL 60783 MINUTE),
  (@cd_debora_aux, 'cpf', 'pdf', 'aprovado', 'seed-doc-debora_aux-cpf.pdf', 656, @agora - INTERVAL 60738 MINUTE),
  (@cd_debora_aux, 'ctps', 'pdf', 'aprovado', 'seed-doc-debora_aux-ctps.pdf', 656, @agora - INTERVAL 60693 MINUTE),
  (@cd_debora_aux, 'titulo_eleitor', 'pdf', 'aprovado', 'seed-doc-debora_aux-titulo_eleitor.pdf', 656, @agora - INTERVAL 60648 MINUTE),
  (@cd_debora_aux, 'comprovante_residencia', 'pdf', 'aprovado', 'seed-doc-debora_aux-comprovante_residencia.pdf', 656, @agora - INTERVAL 60603 MINUTE),
  (@cd_debora_aux, 'comprovante_escolaridade', 'pdf', 'aprovado', 'seed-doc-debora_aux-comprovante_escolaridade.pdf', 656, @agora - INTERVAL 60558 MINUTE),
  (@cd_debora_aux, 'foto_3x4', 'pdf', 'aprovado', 'seed-doc-debora_aux-foto_3x4.pdf', 656, @agora - INTERVAL 60513 MINUTE),
  (@cd_debora_aux, 'pis_pasep', 'pdf', 'aprovado', 'seed-doc-debora_aux-pis_pasep.pdf', 656, @agora - INTERVAL 60468 MINUTE),
  (@cd_debora_aux, 'certidao_nascimento_casamento', 'pdf', 'aprovado', 'seed-doc-debora_aux-certidao_nascimento_casamento.pdf', 656, @agora - INTERVAL 60423 MINUTE),
  (@cd_debora_aux, 'dados_bancarios', 'pdf', 'aprovado', 'seed-doc-debora_aux-dados_bancarios.pdf', 656, @agora - INTERVAL 60378 MINUTE);

-- Eduardo Salgado -> Auxiliar Administrativo Temporário (reprovado)
INSERT INTO candidatura (usuario_id, vaga_id, status, entrevista_em, presenca, presenca_confirmada_em, data_candidatura) VALUES
  (@u_eduardo, @v_aux, 'reprovado', NULL, NULL, NULL, @agora - INTERVAL 72101 MINUTE);
SET @cd_eduardo_aux = LAST_INSERT_ID();
INSERT INTO historico_status (candidatura_id, status_anterior, status_novo, usuario_id, observacao, data_alteracao) VALUES
  (@cd_eduardo_aux, NULL, 'inscrito', @u_eduardo, 'Candidatura registrada pelo portal.', @agora - INTERVAL 72101 MINUTE),
  (@cd_eduardo_aux, 'inscrito', 'em_triagem', @u_paulo, 'Currículo em análise pelo RH.', @agora - INTERVAL 70205 MINUTE),
  (@cd_eduardo_aux, 'em_triagem', 'reprovado', @u_paulo, 'Vaga preenchida por outro candidato.', @agora - INTERVAL 66332 MINUTE);

-- ---------------------------------------------------------------
-- notificacao (inseridas em ordem cronologica; as de mais de 4 dias ja foram lidas)
-- ---------------------------------------------------------------
INSERT INTO notificacao (usuario_id, tipo, referencia_id, titulo, mensagem, lida, criado_em) VALUES
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Nova candidatura', 'Lucas Barbosa se candidatou à vaga Assistente de Faturamento.', 1, @agora - INTERVAL 89691 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Sua candidatura mudou de etapa', 'Vaga Assistente de Faturamento: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 87569 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Entrevista agendada', CONCAT('Vaga Assistente de Faturamento: sua entrevista foi marcada para ', DATE_FORMAT(@ent_lucas_fatur, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 84207 MINUTE),
  (@u_rita, 'candidatura', @cd_bruno_fatur, 'Nova candidatura', 'Bruno Lima se candidatou à vaga Assistente de Faturamento.', 1, @agora - INTERVAL 79461 MINUTE),
  (@u_paulo, 'candidatura', @cd_debora_aux, 'Nova candidatura', 'Débora Freitas se candidatou à vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 77828 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_fatur, 'Sua candidatura mudou de etapa', 'Vaga Assistente de Faturamento: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 77445 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Sua candidatura mudou de etapa', 'Vaga Assistente de Faturamento: sua candidatura agora está em "Aprovado".', 1, @agora - INTERVAL 76745 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Sua candidatura mudou de etapa', 'Vaga Auxiliar Administrativo Temporário: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 75784 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fatur, 'Nova candidatura', 'Renata Siqueira se candidatou à vaga Assistente de Faturamento.', 1, @agora - INTERVAL 75146 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_fatur, 'Sua candidatura mudou de etapa', 'Agradecemos o seu interesse e o tempo dedicado à vaga Assistente de Faturamento. Desta vez seguiremos com outro perfil, mas o seu currículo continua no portal e você pode se candidatar às outras vagas abertas.', 1, @agora - INTERVAL 73736 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Entrevista agendada', CONCAT('Vaga Auxiliar Administrativo Temporário: sua entrevista foi marcada para ', DATE_FORMAT(@ent_debora_aux, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 72475 MINUTE),
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Documento recebido', 'Lucas Barbosa enviou RG para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 72300 MINUTE),
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Documento recebido', 'Lucas Barbosa enviou CPF para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 72255 MINUTE),
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Documento recebido', 'Lucas Barbosa enviou Carteira de Trabalho (CTPS) para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 72210 MINUTE),
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Documento recebido', 'Lucas Barbosa enviou Título de eleitor para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 72165 MINUTE),
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Documento recebido', 'Lucas Barbosa enviou Comprovante de residência para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 72120 MINUTE),
  (@u_paulo, 'candidatura', @cd_eduardo_aux, 'Nova candidatura', 'Eduardo Salgado se candidatou à vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 72101 MINUTE),
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Documento recebido', 'Lucas Barbosa enviou Comprovante de escolaridade para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 72075 MINUTE),
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Documento recebido', 'Lucas Barbosa enviou Foto 3x4 para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 72030 MINUTE),
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Documento recebido', 'Lucas Barbosa enviou PIS ou PASEP para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 71985 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Documento aprovado', 'Seu documento Comprovante de residência da vaga Assistente de Faturamento foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 71966 MINUTE),
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Documento recebido', 'Lucas Barbosa enviou Certidão de nascimento ou casamento para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 71940 MINUTE),
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Documento recebido', 'Lucas Barbosa enviou Dados bancários para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 71895 MINUTE),
  (@u_rita, 'candidatura', @cd_lucas_fatur, 'Documento recebido', 'Lucas Barbosa enviou Certificado de reservista para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 71850 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Documento aprovado', 'Seu documento RG da vaga Assistente de Faturamento foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 71751 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Documento aprovado', 'Seu documento Certidão de nascimento ou casamento da vaga Assistente de Faturamento foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 71691 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Documento aprovado', 'Seu documento CPF da vaga Assistente de Faturamento foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 71643 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Documento aprovado', 'Seu documento Dados bancários da vaga Assistente de Faturamento foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 71619 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Documento aprovado', 'Seu documento PIS ou PASEP da vaga Assistente de Faturamento foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 71426 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Documento aprovado', 'Seu documento Carteira de Trabalho (CTPS) da vaga Assistente de Faturamento foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 71422 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Documento aprovado', 'Seu documento Título de eleitor da vaga Assistente de Faturamento foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 71316 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Documento aprovado', 'Seu documento Comprovante de escolaridade da vaga Assistente de Faturamento foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 71272 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Documento aprovado', 'Seu documento Foto 3x4 da vaga Assistente de Faturamento foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 71205 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Documento aprovado', 'Seu documento Certificado de reservista da vaga Assistente de Faturamento foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 71148 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fatur, 'Sua candidatura mudou de etapa', 'A sua candidatura à vaga Assistente de Faturamento foi encerrada. Quando quiser, conheça as outras vagas abertas e candidate-se.', 1, @agora - INTERVAL 70681 MINUTE),
  (@u_eduardo, 'candidatura', @cd_eduardo_aux, 'Sua candidatura mudou de etapa', 'Vaga Auxiliar Administrativo Temporário: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 70205 MINUTE),
  (@u_eduardo, 'candidatura', @cd_eduardo_aux, 'Sua candidatura mudou de etapa', 'Agradecemos o seu interesse e o tempo dedicado à vaga Auxiliar Administrativo Temporário. Desta vez seguiremos com outro perfil, mas o seu currículo continua no portal e você pode se candidatar às outras vagas abertas.', 1, @agora - INTERVAL 66332 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Sua candidatura mudou de etapa', 'Vaga Auxiliar Administrativo Temporário: sua candidatura agora está em "Aprovado".', 1, @agora - INTERVAL 65217 MINUTE),
  (@u_lucas, 'candidatura', @cd_lucas_fatur, 'Contratação confirmada', 'Bem-vindo(a) à equipe! Você foi contratado(a) para a vaga Assistente de Faturamento.', 1, @agora - INTERVAL 65100 MINUTE),
  (@u_paulo, 'candidatura', @cd_debora_aux, 'Documento recebido', 'Débora Freitas enviou RG para a vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 60783 MINUTE);
INSERT INTO notificacao (usuario_id, tipo, referencia_id, titulo, mensagem, lida, criado_em) VALUES
  (@u_paulo, 'candidatura', @cd_debora_aux, 'Documento recebido', 'Débora Freitas enviou CPF para a vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 60738 MINUTE),
  (@u_paulo, 'candidatura', @cd_debora_aux, 'Documento recebido', 'Débora Freitas enviou Carteira de Trabalho (CTPS) para a vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 60693 MINUTE),
  (@u_paulo, 'candidatura', @cd_debora_aux, 'Documento recebido', 'Débora Freitas enviou Título de eleitor para a vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 60648 MINUTE),
  (@u_paulo, 'candidatura', @cd_debora_aux, 'Documento recebido', 'Débora Freitas enviou Comprovante de residência para a vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 60603 MINUTE),
  (@u_paulo, 'candidatura', @cd_debora_aux, 'Documento recebido', 'Débora Freitas enviou Comprovante de escolaridade para a vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 60558 MINUTE),
  (@u_paulo, 'candidatura', @cd_debora_aux, 'Documento recebido', 'Débora Freitas enviou Foto 3x4 para a vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 60513 MINUTE),
  (@u_paulo, 'candidatura', @cd_debora_aux, 'Documento recebido', 'Débora Freitas enviou PIS ou PASEP para a vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 60468 MINUTE),
  (@u_paulo, 'candidatura', @cd_debora_aux, 'Documento recebido', 'Débora Freitas enviou Certidão de nascimento ou casamento para a vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 60423 MINUTE),
  (@u_paulo, 'candidatura', @cd_debora_aux, 'Documento recebido', 'Débora Freitas enviou Dados bancários para a vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 60378 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Documento aprovado', 'Seu documento RG da vaga Auxiliar Administrativo Temporário foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 60355 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Documento aprovado', 'Seu documento Título de eleitor da vaga Auxiliar Administrativo Temporário foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 60272 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Documento aprovado', 'Seu documento Comprovante de residência da vaga Auxiliar Administrativo Temporário foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 60128 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Documento aprovado', 'Seu documento Carteira de Trabalho (CTPS) da vaga Auxiliar Administrativo Temporário foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 60017 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Documento aprovado', 'Seu documento CPF da vaga Auxiliar Administrativo Temporário foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 59996 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Documento aprovado', 'Seu documento Foto 3x4 da vaga Auxiliar Administrativo Temporário foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 59977 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Documento aprovado', 'Seu documento Dados bancários da vaga Auxiliar Administrativo Temporário foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 59875 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Documento aprovado', 'Seu documento Certidão de nascimento ou casamento da vaga Auxiliar Administrativo Temporário foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 59757 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Documento aprovado', 'Seu documento PIS ou PASEP da vaga Auxiliar Administrativo Temporário foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 59686 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Documento aprovado', 'Seu documento Comprovante de escolaridade da vaga Auxiliar Administrativo Temporário foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 59671 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_aux, 'Contratação confirmada', 'Bem-vindo(a) à equipe! Você foi contratado(a) para a vaga Auxiliar Administrativo Temporário.', 1, @agora - INTERVAL 53583 MINUTE),
  (@u_rita, 'candidatura', @cd_carla_fin, 'Nova candidatura', 'Carla Mendes se candidatou à vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 49342 MINUTE),
  (@u_paulo, 'candidatura', @cd_olivia_recep, 'Nova candidatura', 'Olívia Ramos se candidatou à vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 47572 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Sua candidatura mudou de etapa', 'Vaga Analista Financeiro Pleno: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 47353 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Sua candidatura mudou de etapa', 'Vaga Recepcionista Corporativa: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 45457 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Entrevista agendada', CONCAT('Vaga Analista Financeiro Pleno: sua entrevista foi marcada para ', DATE_FORMAT(@ent_carla_fin, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 44159 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Entrevista agendada', CONCAT('Vaga Recepcionista Corporativa: sua entrevista foi marcada para ', DATE_FORMAT(@ent_olivia_recep, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 42137 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fin, 'Nova candidatura', 'Renata Siqueira se candidatou à vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 37603 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Sua candidatura mudou de etapa', 'Vaga Analista Financeiro Pleno: sua candidatura agora está em "Aprovado".', 1, @agora - INTERVAL 36948 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Sua candidatura mudou de etapa', 'Vaga Analista Financeiro Pleno: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 35680 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Sua candidatura mudou de etapa', 'Vaga Recepcionista Corporativa: sua candidatura agora está em "Aprovado".', 1, @agora - INTERVAL 34852 MINUTE),
  (@u_rita, 'candidatura', @cd_bruno_adm, 'Nova candidatura', 'Bruno Lima se candidatou à vaga Assistente Administrativo.', 1, @agora - INTERVAL 34807 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Sua candidatura mudou de etapa', 'Vaga Assistente Administrativo: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 32786 MINUTE),
  (@u_rita, 'candidatura', @cd_carla_fin, 'Documento recebido', 'Carla Mendes enviou RG para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 32564 MINUTE),
  (@u_rita, 'candidatura', @cd_carla_fin, 'Documento recebido', 'Carla Mendes enviou CPF para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 32519 MINUTE),
  (@u_rita, 'candidatura', @cd_carla_fin, 'Documento recebido', 'Carla Mendes enviou Carteira de Trabalho (CTPS) para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 32474 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Documento aprovado', 'Seu documento RG da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 32432 MINUTE),
  (@u_rita, 'candidatura', @cd_carla_fin, 'Documento recebido', 'Carla Mendes enviou Título de eleitor para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 32429 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Entrevista agendada', CONCAT('Vaga Analista Financeiro Pleno: sua entrevista foi marcada para ', DATE_FORMAT(@ent_renata_fin, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 32387 MINUTE),
  (@u_rita, 'candidatura', @cd_carla_fin, 'Documento recebido', 'Carla Mendes enviou Comprovante de residência para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 32384 MINUTE),
  (@u_rita, 'candidatura', @cd_carla_fin, 'Documento recebido', 'Carla Mendes enviou Comprovante de escolaridade para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 32339 MINUTE);
INSERT INTO notificacao (usuario_id, tipo, referencia_id, titulo, mensagem, lida, criado_em) VALUES
  (@u_rita, 'candidatura', @cd_carla_fin, 'Documento recebido', 'Carla Mendes enviou Foto 3x4 para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 32294 MINUTE),
  (@u_rita, 'candidatura', @cd_carla_fin, 'Documento recebido', 'Carla Mendes enviou PIS ou PASEP para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 32249 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Documento aprovado', 'Seu documento Título de eleitor da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 32237 MINUTE),
  (@u_rita, 'candidatura', @cd_carla_fin, 'Documento recebido', 'Carla Mendes enviou Certidão de nascimento ou casamento para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 32204 MINUTE),
  (@u_rita, 'candidatura', @cd_sergio_fin, 'Nova candidatura', 'Sérgio Vasconcelos se candidatou à vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 32160 MINUTE),
  (@u_rita, 'candidatura', @cd_carla_fin, 'Documento recebido', 'Carla Mendes enviou Dados bancários para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 32159 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Documento aprovado', 'Seu documento Carteira de Trabalho (CTPS) da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 32095 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Documento aprovado', 'Seu documento Comprovante de residência da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 32045 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Documento aprovado', 'Seu documento CPF da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 31913 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Documento aprovado', 'Seu documento Dados bancários da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 31871 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Documento aprovado', 'Seu documento Comprovante de escolaridade da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 31753 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Documento aprovado', 'Seu documento PIS ou PASEP da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 31742 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Documento aprovado', 'Seu documento Foto 3x4 da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 31532 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Documento aprovado', 'Seu documento Certidão de nascimento ou casamento da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 31432 MINUTE),
  (@u_paulo, 'candidatura', @cd_olivia_recep, 'Documento recebido', 'Olívia Ramos enviou RG para a vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 30515 MINUTE),
  (@u_paulo, 'candidatura', @cd_olivia_recep, 'Documento recebido', 'Olívia Ramos enviou CPF para a vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 30470 MINUTE),
  (@u_paulo, 'candidatura', @cd_olivia_recep, 'Documento recebido', 'Olívia Ramos enviou Carteira de Trabalho (CTPS) para a vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 30425 MINUTE),
  (@u_paulo, 'candidatura', @cd_olivia_recep, 'Documento recebido', 'Olívia Ramos enviou Título de eleitor para a vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 30380 MINUTE),
  (@u_paulo, 'candidatura', @cd_olivia_recep, 'Documento recebido', 'Olívia Ramos enviou Comprovante de residência para a vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 30335 MINUTE),
  (@u_paulo, 'candidatura', @cd_olivia_recep, 'Documento recebido', 'Olívia Ramos enviou Comprovante de escolaridade para a vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 30290 MINUTE),
  (@u_paulo, 'candidatura', @cd_olivia_recep, 'Documento recebido', 'Olívia Ramos enviou Foto 3x4 para a vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 30245 MINUTE),
  (@u_paulo, 'candidatura', @cd_olivia_recep, 'Documento recebido', 'Olívia Ramos enviou PIS ou PASEP para a vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 30200 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Documento aprovado', 'Seu documento Carteira de Trabalho (CTPS) da vaga Recepcionista Corporativa foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 30175 MINUTE),
  (@u_paulo, 'candidatura', @cd_olivia_recep, 'Documento recebido', 'Olívia Ramos enviou Certidão de nascimento ou casamento para a vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 30155 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Documento aprovado', 'Seu documento RG da vaga Recepcionista Corporativa foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 30124 MINUTE),
  (@u_sergio, 'candidatura', @cd_sergio_fin, 'Sua candidatura mudou de etapa', 'Vaga Analista Financeiro Pleno: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 30122 MINUTE),
  (@u_paulo, 'candidatura', @cd_olivia_recep, 'Documento recebido', 'Olívia Ramos enviou Dados bancários para a vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 30110 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Documento aprovado', 'Seu documento Certidão de nascimento ou casamento da vaga Recepcionista Corporativa foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 29965 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Documento aprovado', 'Seu documento PIS ou PASEP da vaga Recepcionista Corporativa foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 29813 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Documento aprovado', 'Seu documento Dados bancários da vaga Recepcionista Corporativa foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 29658 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Documento aprovado', 'Seu documento Comprovante de residência da vaga Recepcionista Corporativa foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 29620 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Documento aprovado', 'Seu documento CPF da vaga Recepcionista Corporativa foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 29614 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Documento aprovado', 'Seu documento Comprovante de escolaridade da vaga Recepcionista Corporativa foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 29500 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Documento aprovado', 'Seu documento Título de eleitor da vaga Recepcionista Corporativa foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 29491 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Documento aprovado', 'Seu documento Foto 3x4 da vaga Recepcionista Corporativa foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 29487 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Entrevista agendada', CONCAT('Vaga Assistente Administrativo: sua entrevista foi marcada para ', DATE_FORMAT(@ent_bruno_adm, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 29456 MINUTE),
  (@u_paulo, 'candidatura', @cd_ulisses_dp, 'Nova candidatura', 'Ulisses Pinheiro se candidatou à vaga Analista de Departamento Pessoal.', 1, @agora - INTERVAL 29182 MINUTE),
  (@u_marina, 'candidatura', @cd_fernanda_contab, 'Nova candidatura', 'Fernanda Lacerda se candidatou à vaga Auxiliar de Contabilidade.', 1, @agora - INTERVAL 27621 MINUTE),
  (@u_ulisses, 'candidatura', @cd_ulisses_dp, 'Sua candidatura mudou de etapa', 'Vaga Analista de Departamento Pessoal: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 27195 MINUTE),
  (@u_sergio, 'candidatura', @cd_sergio_fin, 'Entrevista agendada', CONCAT('Vaga Analista Financeiro Pleno: sua entrevista foi marcada para ', DATE_FORMAT(@ent_sergio_fin, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 26759 MINUTE);
INSERT INTO notificacao (usuario_id, tipo, referencia_id, titulo, mensagem, lida, criado_em) VALUES
  (@u_rita, 'candidatura', @cd_debora_sec, 'Nova candidatura', 'Débora Freitas se candidatou à vaga Secretária Executiva Bilíngue.', 1, @agora - INTERVAL 26212 MINUTE),
  (@u_fernanda, 'candidatura', @cd_fernanda_contab, 'Sua candidatura mudou de etapa', 'Vaga Auxiliar de Contabilidade: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 25630 MINUTE),
  (@u_carla, 'candidatura', @cd_carla_fin, 'Contratação confirmada', 'Bem-vindo(a) à equipe! Você foi contratado(a) para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 25364 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Sua candidatura mudou de etapa', 'Vaga Analista Financeiro Pleno: sua candidatura agora está em "Aprovado".', 1, @agora - INTERVAL 24985 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fin, 'Documento recebido', 'Renata Siqueira enviou RG para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 24404 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fin, 'Documento recebido', 'Renata Siqueira enviou CPF para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 24235 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fin, 'Documento recebido', 'Renata Siqueira enviou Dados bancários para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 24197 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fin, 'Documento recebido', 'Renata Siqueira enviou Carteira de Trabalho (CTPS) para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 24147 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fin, 'Documento recebido', 'Renata Siqueira enviou Comprovante de residência para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 24145 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_sec, 'Sua candidatura mudou de etapa', 'Vaga Secretária Executiva Bilíngue: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 24129 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fin, 'Documento recebido', 'Renata Siqueira enviou Título de eleitor para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 24105 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fin, 'Documento recebido', 'Renata Siqueira enviou Foto 3x4 para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 24095 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Documento aprovado', 'Seu documento Dados bancários da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 24063 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fin, 'Documento recebido', 'Renata Siqueira enviou Comprovante de escolaridade para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 24007 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Documento aprovado', 'Seu documento Comprovante de residência da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 23962 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Documento aprovado', 'Seu documento RG da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 23936 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Documento aprovado', 'Seu documento Carteira de Trabalho (CTPS) da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 23914 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fin, 'Documento recebido', 'Renata Siqueira enviou PIS ou PASEP para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 23911 MINUTE),
  (@u_ulisses, 'candidatura', @cd_ulisses_dp, 'Entrevista agendada', CONCAT('Vaga Analista de Departamento Pessoal: sua entrevista foi marcada para ', DATE_FORMAT(@ent_ulisses_dp, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 23872 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Documento aprovado', 'Seu documento Comprovante de escolaridade da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 23854 MINUTE),
  (@u_rita, 'candidatura', @cd_renata_fin, 'Documento recebido', 'Renata Siqueira enviou Certidão de nascimento ou casamento para a vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 23846 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Documento aprovado', 'Seu documento CPF da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 23789 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Documento aprovado', 'Seu documento Título de eleitor da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 23727 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Documento aprovado', 'Seu documento PIS ou PASEP da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 23684 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Documento aprovado', 'Seu documento Foto 3x4 da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 23564 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fin, 'Documento aprovado', 'Seu documento Certidão de nascimento ou casamento da vaga Analista Financeiro Pleno foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 23338 MINUTE),
  (@u_olivia, 'candidatura', @cd_olivia_recep, 'Contratação confirmada', 'Bem-vindo(a) à equipe! Você foi contratado(a) para a vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 23315 MINUTE),
  (@u_rita, 'candidatura', @cd_joao_adm, 'Nova candidatura', 'João Pedro Martins se candidatou à vaga Assistente Administrativo.', 1, @agora - INTERVAL 23155 MINUTE),
  (@u_sergio, 'candidatura', @cd_sergio_fin, 'Sua candidatura mudou de etapa', 'Agradecemos o seu interesse e o tempo dedicado à vaga Analista Financeiro Pleno. Desta vez seguiremos com outro perfil, mas o seu currículo continua no portal e você pode se candidatar às outras vagas abertas.', 1, @agora - INTERVAL 22916 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Sua candidatura mudou de etapa', 'Vaga Assistente Administrativo: sua candidatura agora está em "Aprovado".', 1, @agora - INTERVAL 22245 MINUTE),
  (@u_fernanda, 'candidatura', @cd_fernanda_contab, 'Entrevista agendada', CONCAT('Vaga Auxiliar de Contabilidade: sua entrevista foi marcada para ', DATE_FORMAT(@ent_fernanda_contab, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 22162 MINUTE),
  (@u_paulo, 'candidatura', @cd_vanessa_recep, 'Nova candidatura', 'Vanessa Cunha se candidatou à vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 21842 MINUTE),
  (@u_rita, 'candidatura', @cd_bruno_adm, 'Documento recebido', 'Bruno Lima enviou RG para a vaga Assistente Administrativo.', 1, @agora - INTERVAL 21607 MINUTE),
  (@u_rita, 'candidatura', @cd_bruno_adm, 'Documento recebido', 'Bruno Lima enviou CPF para a vaga Assistente Administrativo.', 1, @agora - INTERVAL 21523 MINUTE),
  (@u_rita, 'candidatura', @cd_bruno_adm, 'Documento recebido', 'Bruno Lima enviou Carteira de Trabalho (CTPS) para a vaga Assistente Administrativo.', 1, @agora - INTERVAL 21485 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Documento aprovado', 'Seu documento RG da vaga Assistente Administrativo foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 21449 MINUTE),
  (@u_rita, 'candidatura', @cd_bruno_adm, 'Documento recebido', 'Bruno Lima enviou Título de eleitor para a vaga Assistente Administrativo.', 1, @agora - INTERVAL 21441 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Documento aprovado', 'Seu documento CPF da vaga Assistente Administrativo foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 21357 MINUTE),
  (@u_rita, 'candidatura', @cd_bruno_adm, 'Documento recebido', 'Bruno Lima enviou Comprovante de residência para a vaga Assistente Administrativo.', 1, @agora - INTERVAL 21322 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Documento aprovado', 'Seu documento Carteira de Trabalho (CTPS) da vaga Assistente Administrativo foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 21301 MINUTE);
INSERT INTO notificacao (usuario_id, tipo, referencia_id, titulo, mensagem, lida, criado_em) VALUES
  (@u_rita, 'candidatura', @cd_bruno_adm, 'Documento recebido', 'Bruno Lima enviou Comprovante de escolaridade para a vaga Assistente Administrativo.', 1, @agora - INTERVAL 21209 MINUTE),
  (@u_rita, 'candidatura', @cd_bruno_adm, 'Documento recebido', 'Bruno Lima enviou Certidão de nascimento ou casamento para a vaga Assistente Administrativo.', 1, @agora - INTERVAL 21209 MINUTE),
  (@u_rita, 'candidatura', @cd_bruno_adm, 'Documento recebido', 'Bruno Lima enviou Foto 3x4 para a vaga Assistente Administrativo.', 1, @agora - INTERVAL 21126 MINUTE),
  (@u_joao, 'candidatura', @cd_joao_adm, 'Sua candidatura mudou de etapa', 'Vaga Assistente Administrativo: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 21118 MINUTE),
  (@u_rita, 'candidatura', @cd_bruno_adm, 'Documento recebido', 'Bruno Lima enviou PIS ou PASEP para a vaga Assistente Administrativo.', 1, @agora - INTERVAL 21081 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Documento aprovado', 'Seu documento Título de eleitor da vaga Assistente Administrativo foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 21041 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Precisamos de um novo envio', 'Não conseguimos aprovar o documento Foto 3x4 da vaga Assistente Administrativo desta vez. Sem problema: envie uma nova versão na tela de Documentos e o RH analisa novamente.', 1, @agora - INTERVAL 21026 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Documento aprovado', 'Seu documento Comprovante de escolaridade da vaga Assistente Administrativo foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 20946 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Documento aprovado', 'Seu documento Certidão de nascimento ou casamento da vaga Assistente Administrativo foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 20882 MINUTE),
  (@u_rita, 'candidatura', @cd_bruno_adm, 'Documento recebido', 'Bruno Lima enviou Dados bancários para a vaga Assistente Administrativo.', 1, @agora - INTERVAL 20879 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Documento aprovado', 'Seu documento PIS ou PASEP da vaga Assistente Administrativo foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 20658 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_adm, 'Documento aprovado', 'Seu documento Dados bancários da vaga Assistente Administrativo foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 20546 MINUTE),
  (@u_debora, 'candidatura', @cd_debora_sec, 'Sua candidatura mudou de etapa', 'Agradecemos o seu interesse e o tempo dedicado à vaga Secretária Executiva Bilíngue. Desta vez seguiremos com outro perfil, mas o seu currículo continua no portal e você pode se candidatar às outras vagas abertas.', 1, @agora - INTERVAL 20369 MINUTE),
  (@u_vanessa, 'candidatura', @cd_vanessa_recep, 'Sua candidatura mudou de etapa', 'Vaga Recepcionista Corporativa: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 19698 MINUTE),
  (@u_marina, 'candidatura', @cd_isabela_rs, 'Nova candidatura', 'Isabela Ferraz se candidatou à vaga Assistente de Recrutamento e Seleção.', 1, @agora - INTERVAL 18764 MINUTE),
  (@u_joao, 'candidatura', @cd_joao_adm, 'Sua candidatura mudou de etapa', 'Agradecemos o seu interesse e o tempo dedicado à vaga Assistente Administrativo. Desta vez seguiremos com outro perfil, mas o seu currículo continua no portal e você pode se candidatar às outras vagas abertas.', 1, @agora - INTERVAL 17385 MINUTE),
  (@u_eduardo, 'candidatura', @cd_eduardo_aux, 'Vaga encerrada', 'A vaga Auxiliar Administrativo Temporário foi encerrada. Agradecemos a sua participação no processo. O seu currículo continua no portal: fique de olho nas novas vagas e candidate-se quando quiser.', 1, @agora - INTERVAL 17280 MINUTE),
  (@u_ulisses, 'candidatura', @cd_ulisses_dp, 'Sua candidatura mudou de etapa', 'Vaga Analista de Departamento Pessoal: sua candidatura agora está em "Aprovado".', 1, @agora - INTERVAL 16595 MINUTE),
  (@u_marina, 'candidatura', @cd_tatiane_contab, 'Nova candidatura', 'Tatiane Moreira se candidatou à vaga Auxiliar de Contabilidade.', 1, @agora - INTERVAL 15980 MINUTE),
  (@u_paulo, 'candidatura', @cd_ulisses_dp, 'Documento recebido', 'Ulisses Pinheiro enviou RG para a vaga Analista de Departamento Pessoal.', 1, @agora - INTERVAL 15957 MINUTE),
  (@u_paulo, 'candidatura', @cd_ulisses_dp, 'Documento recebido', 'Ulisses Pinheiro enviou CPF para a vaga Analista de Departamento Pessoal.', 1, @agora - INTERVAL 15925 MINUTE),
  (@u_marina, 'candidatura', @cd_nicolas_comp, 'Nova candidatura', 'Nicolas Teixeira se candidatou à vaga Analista de Compras.', 1, @agora - INTERVAL 15918 MINUTE),
  (@u_paulo, 'candidatura', @cd_ulisses_dp, 'Documento recebido', 'Ulisses Pinheiro enviou Carteira de Trabalho (CTPS) para a vaga Analista de Departamento Pessoal.', 1, @agora - INTERVAL 15739 MINUTE),
  (@u_paulo, 'candidatura', @cd_ulisses_dp, 'Documento recebido', 'Ulisses Pinheiro enviou Comprovante de residência para a vaga Analista de Departamento Pessoal.', 1, @agora - INTERVAL 15658 MINUTE),
  (@u_paulo, 'candidatura', @cd_ulisses_dp, 'Documento recebido', 'Ulisses Pinheiro enviou Título de eleitor para a vaga Analista de Departamento Pessoal.', 1, @agora - INTERVAL 15657 MINUTE),
  (@u_ulisses, 'candidatura', @cd_ulisses_dp, 'Documento aprovado', 'Seu documento RG da vaga Analista de Departamento Pessoal foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 15553 MINUTE),
  (@u_ulisses, 'candidatura', @cd_ulisses_dp, 'Documento aprovado', 'Seu documento Carteira de Trabalho (CTPS) da vaga Analista de Departamento Pessoal foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 15545 MINUTE),
  (@u_paulo, 'candidatura', @cd_ulisses_dp, 'Documento recebido', 'Ulisses Pinheiro enviou Foto 3x4 para a vaga Analista de Departamento Pessoal.', 1, @agora - INTERVAL 15498 MINUTE),
  (@u_ulisses, 'candidatura', @cd_ulisses_dp, 'Documento aprovado', 'Seu documento CPF da vaga Analista de Departamento Pessoal foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 15360 MINUTE),
  (@u_vanessa, 'candidatura', @cd_vanessa_recep, 'Sua candidatura mudou de etapa', 'A sua candidatura à vaga Recepcionista Corporativa foi encerrada. Quando quiser, conheça as outras vagas abertas e candidate-se.', 1, @agora - INTERVAL 15113 MINUTE),
  (@u_ulisses, 'candidatura', @cd_ulisses_dp, 'Documento aprovado', 'Seu documento Foto 3x4 da vaga Analista de Departamento Pessoal foi aprovado. Obrigado pelo envio!', 1, @agora - INTERVAL 15049 MINUTE),
  (@u_isabela, 'candidatura', @cd_isabela_rs, 'Sua candidatura mudou de etapa', 'Agradecemos o seu interesse e o tempo dedicado à vaga Assistente de Recrutamento e Seleção. Desta vez seguiremos com outro perfil, mas o seu currículo continua no portal e você pode se candidatar às outras vagas abertas.', 1, @agora - INTERVAL 15006 MINUTE),
  (@u_rita, 'candidatura', @cd_henrique_fin, 'Nova candidatura', 'Henrique Duarte se candidatou à vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 14814 MINUTE),
  (@u_fernanda, 'candidatura', @cd_fernanda_contab, 'Sua candidatura mudou de etapa', 'Vaga Auxiliar de Contabilidade: sua candidatura agora está em "Aprovado".', 1, @agora - INTERVAL 14708 MINUTE),
  (@u_paulo, 'candidatura', @cd_tatiane_fisc, 'Nova candidatura', 'Tatiane Moreira se candidatou à vaga Analista Fiscal.', 1, @agora - INTERVAL 14497 MINUTE),
  (@u_marina, 'candidatura', @cd_fernanda_contab, 'Documento recebido', 'Fernanda Lacerda enviou RG para a vaga Auxiliar de Contabilidade.', 1, @agora - INTERVAL 14061 MINUTE),
  (@u_marina, 'candidatura', @cd_fernanda_contab, 'Documento recebido', 'Fernanda Lacerda enviou CPF para a vaga Auxiliar de Contabilidade.', 1, @agora - INTERVAL 14005 MINUTE),
  (@u_marina, 'candidatura', @cd_fernanda_contab, 'Documento recebido', 'Fernanda Lacerda enviou Dados bancários para a vaga Auxiliar de Contabilidade.', 1, @agora - INTERVAL 13989 MINUTE),
  (@u_tatiane, 'candidatura', @cd_tatiane_contab, 'Sua candidatura mudou de etapa', 'Vaga Auxiliar de Contabilidade: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 13926 MINUTE),
  (@u_nicolas, 'candidatura', @cd_nicolas_comp, 'Sua candidatura mudou de etapa', 'Vaga Analista de Compras: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 13823 MINUTE);
INSERT INTO notificacao (usuario_id, tipo, referencia_id, titulo, mensagem, lida, criado_em) VALUES
  (@u_rita, 'candidatura', @cd_ana_adm, 'Nova candidatura', 'Ana Souza se candidatou à vaga Assistente Administrativo.', 1, @agora - INTERVAL 13119 MINUTE),
  (@u_rita, 'candidatura', @cd_pedro_cpr, 'Nova candidatura', 'Pedro Henrique Gomes se candidatou à vaga Analista de Contas a Pagar e Receber.', 1, @agora - INTERVAL 13038 MINUTE),
  (@u_henrique, 'candidatura', @cd_henrique_fin, 'Sua candidatura mudou de etapa', 'Vaga Analista Financeiro Pleno: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 12927 MINUTE),
  (@u_tatiane, 'candidatura', @cd_tatiane_fisc, 'Sua candidatura mudou de etapa', 'Vaga Analista Fiscal: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 12490 MINUTE),
  (@u_paulo, 'candidatura', @cd_karina_dp, 'Nova candidatura', 'Karina Alves se candidatou à vaga Analista de Departamento Pessoal.', 1, @agora - INTERVAL 11815 MINUTE),
  (@u_marina, 'candidatura', @cd_mariana_rs, 'Nova candidatura', 'Mariana Cardoso se candidatou à vaga Assistente de Recrutamento e Seleção.', 1, @agora - INTERVAL 11643 MINUTE),
  (@u_bruno, 'candidatura', @cd_bruno_fatur, 'Vaga encerrada', 'A vaga Assistente de Faturamento foi encerrada. Agradecemos a sua participação no processo. O seu currículo continua no portal: fique de olho nas novas vagas e candidate-se quando quiser.', 1, @agora - INTERVAL 11520 MINUTE),
  (@u_renata, 'candidatura', @cd_renata_fatur, 'Vaga encerrada', 'A vaga Assistente de Faturamento foi encerrada. Agradecemos a sua participação no processo. O seu currículo continua no portal: fique de olho nas novas vagas e candidate-se quando quiser.', 1, @agora - INTERVAL 11520 MINUTE),
  (@u_ana, 'candidatura', @cd_ana_adm, 'Sua candidatura mudou de etapa', 'Vaga Assistente Administrativo: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 11210 MINUTE),
  (@u_pedro, 'candidatura', @cd_pedro_cpr, 'Sua candidatura mudou de etapa', 'Vaga Analista de Contas a Pagar e Receber: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 11123 MINUTE),
  (@u_tatiane, 'candidatura', @cd_tatiane_contab, 'Entrevista agendada', CONCAT('Vaga Auxiliar de Contabilidade: sua entrevista foi marcada para ', DATE_FORMAT(@ent_tatiane_contab, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 10536 MINUTE),
  (@u_paulo, 'candidatura', @cd_yasmin_recep, 'Nova candidatura', 'Yasmin Carvalho se candidatou à vaga Recepcionista Corporativa.', 1, @agora - INTERVAL 10532 MINUTE),
  (@u_nicolas, 'candidatura', @cd_nicolas_comp, 'Entrevista agendada', CONCAT('Vaga Analista de Compras: sua entrevista foi marcada para ', DATE_FORMAT(@ent_nicolas_comp, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 10480 MINUTE),
  (@u_rita, 'candidatura', @cd_beatriz_sec, 'Nova candidatura', 'Beatriz Azevedo se candidatou à vaga Secretária Executiva Bilíngue.', 1, @agora - INTERVAL 10369 MINUTE),
  (@u_paulo, 'candidatura', @cd_wesley_est, 'Nova candidatura', 'Wesley Nunes se candidatou à vaga Estagiário(a) de Administração.', 1, @agora - INTERVAL 10344 MINUTE),
  (@u_karina, 'candidatura', @cd_karina_dp, 'Sua candidatura mudou de etapa', 'Vaga Analista de Departamento Pessoal: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 9908 MINUTE),
  (@u_mariana, 'candidatura', @cd_mariana_rs, 'Sua candidatura mudou de etapa', 'Vaga Assistente de Recrutamento e Seleção: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 9614 MINUTE),
  (@u_henrique, 'candidatura', @cd_henrique_fin, 'Entrevista agendada', CONCAT('Vaga Analista Financeiro Pleno: sua entrevista foi marcada para ', DATE_FORMAT(@ent_henrique_fin, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 9466 MINUTE),
  (@u_rita, 'candidatura', @cd_isabela_fin, 'Nova candidatura', 'Isabela Ferraz se candidatou à vaga Analista Financeiro Pleno.', 1, @agora - INTERVAL 8764 MINUTE),
  (@u_paulo, 'candidatura', @cd_mariana_dp, 'Nova candidatura', 'Mariana Cardoso se candidatou à vaga Analista de Departamento Pessoal.', 1, @agora - INTERVAL 8738 MINUTE),
  (@u_paulo, 'candidatura', @cd_joao_est, 'Nova candidatura', 'João Pedro Martins se candidatou à vaga Estagiário(a) de Administração.', 1, @agora - INTERVAL 8723 MINUTE),
  (@u_tatiane, 'candidatura', @cd_tatiane_fisc, 'Sua candidatura mudou de etapa', 'Agradecemos o seu interesse e o tempo dedicado à vaga Analista Fiscal. Desta vez seguiremos com outro perfil, mas o seu currículo continua no portal e você pode se candidatar às outras vagas abertas.', 1, @agora - INTERVAL 8707 MINUTE),
  (@u_yasmin, 'candidatura', @cd_yasmin_recep, 'Sua candidatura mudou de etapa', 'Vaga Recepcionista Corporativa: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 8522 MINUTE),
  (@u_wesley, 'candidatura', @cd_wesley_est, 'Sua candidatura mudou de etapa', 'Vaga Estagiário(a) de Administração: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 8425 MINUTE),
  (@u_beatriz, 'candidatura', @cd_beatriz_sec, 'Sua candidatura mudou de etapa', 'Vaga Secretária Executiva Bilíngue: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 8375 MINUTE),
  (@u_ana, 'candidatura', @cd_ana_adm, 'Entrevista agendada', CONCAT('Vaga Assistente Administrativo: sua entrevista foi marcada para ', DATE_FORMAT(@ent_ana_adm, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 7840 MINUTE),
  (@u_pedro, 'candidatura', @cd_pedro_cpr, 'Entrevista agendada', CONCAT('Vaga Analista de Contas a Pagar e Receber: sua entrevista foi marcada para ', DATE_FORMAT(@ent_pedro_cpr, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 7726 MINUTE),
  (@u_rita, 'candidatura', @cd_felipe_adm, 'Nova candidatura', 'Felipe Andrade se candidatou à vaga Assistente Administrativo.', 1, @agora - INTERVAL 7469 MINUTE),
  (@u_marina, 'candidatura', @cd_nicolas_contab, 'Nova candidatura', 'Nicolas Teixeira se candidatou à vaga Auxiliar de Contabilidade.', 1, @agora - INTERVAL 7449 MINUTE),
  (@u_marina, 'candidatura', @cd_rafael_comp, 'Nova candidatura', 'Rafael Monteiro se candidatou à vaga Analista de Compras.', 1, @agora - INTERVAL 7238 MINUTE),
  (@u_isabela, 'candidatura', @cd_isabela_fin, 'Sua candidatura mudou de etapa', 'Vaga Analista Financeiro Pleno: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 6851 MINUTE),
  (@u_mariana, 'candidatura', @cd_mariana_dp, 'Sua candidatura mudou de etapa', 'Vaga Analista de Departamento Pessoal: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 6810 MINUTE),
  (@u_joao, 'candidatura', @cd_joao_est, 'Sua candidatura mudou de etapa', 'Vaga Estagiário(a) de Administração: sua candidatura agora está em "Em análise".', 1, @agora - INTERVAL 6758 MINUTE),
  (@u_karina, 'candidatura', @cd_karina_dp, 'Entrevista agendada', CONCAT('Vaga Analista de Departamento Pessoal: sua entrevista foi marcada para ', DATE_FORMAT(@ent_karina_dp, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 6598 MINUTE),
  (@u_mariana, 'candidatura', @cd_mariana_rs, 'Entrevista agendada', CONCAT('Vaga Assistente de Recrutamento e Seleção: sua entrevista foi marcada para ', DATE_FORMAT(@ent_mariana_rs, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 1, @agora - INTERVAL 6360 MINUTE),
  (@u_marina, 'candidatura', @cd_gabriela_rs, 'Nova candidatura', 'Gabriela Torres se candidatou à vaga Assistente de Recrutamento e Seleção.', 1, @agora - INTERVAL 6237 MINUTE),
  (@u_rita, 'candidatura', @cd_eduardo_cpr, 'Nova candidatura', 'Eduardo Salgado se candidatou à vaga Analista de Contas a Pagar e Receber.', 1, @agora - INTERVAL 5841 MINUTE),
  (@u_yasmin, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5755 MINUTE),
  (@u_joao, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5754 MINUTE),
  (@u_gabriela, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5753 MINUTE);
INSERT INTO notificacao (usuario_id, tipo, referencia_id, titulo, mensagem, lida, criado_em) VALUES
  (@u_debora, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5752 MINUTE),
  (@u_rafael, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5750 MINUTE),
  (@u_renata, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5749 MINUTE),
  (@u_beatriz, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5748 MINUTE),
  (@u_henrique, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5747 MINUTE),
  (@u_tatiane, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5747 MINUTE),
  (@u_bruno, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5745 MINUTE),
  (@u_wesley, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5745 MINUTE),
  (@u_carla, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5744 MINUTE),
  (@u_ana, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5743 MINUTE),
  (@u_sergio, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5743 MINUTE),
  (@u_caua, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5739 MINUTE),
  (@u_pedro, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5736 MINUTE),
  (@u_eduardo, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5736 MINUTE),
  (@u_nicolas, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5734 MINUTE),
  (@u_diego, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5732 MINUTE),
  (@u_ulisses, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5729 MINUTE),
  (@u_felipe, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5728 MINUTE),
  (@u_karina, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5727 MINUTE),
  (@u_vanessa, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5727 MINUTE),
  (@u_mariana, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5724 MINUTE),
  (@u_olivia, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5724 MINUTE),
  (@u_fernanda, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5724 MINUTE),
  (@u_isabela, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5723 MINUTE),
  (@u_lucas, 'nova_vaga', @v_coord, 'Nova vaga publicada', 'A vaga Coordenador(a) Administrativo está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 5723 MINUTE),
  (@u_nicolas, 'candidatura', @cd_nicolas_contab, 'Sua candidatura mudou de etapa', 'Vaga Auxiliar de Contabilidade: sua candidatura agora está em "Em análise".', 0, @agora - INTERVAL 5493 MINUTE),
  (@u_felipe, 'candidatura', @cd_felipe_adm, 'Sua candidatura mudou de etapa', 'Vaga Assistente Administrativo: sua candidatura agora está em "Em análise".', 0, @agora - INTERVAL 5460 MINUTE),
  (@u_yasmin, 'candidatura', @cd_yasmin_recep, 'Entrevista agendada', CONCAT('Vaga Recepcionista Corporativa: sua entrevista foi marcada para ', DATE_FORMAT(@ent_yasmin_recep, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 0, @agora - INTERVAL 5268 MINUTE),
  (@u_rafael, 'candidatura', @cd_rafael_comp, 'Sua candidatura mudou de etapa', 'Vaga Analista de Compras: sua candidatura agora está em "Em análise".', 0, @agora - INTERVAL 5232 MINUTE),
  (@u_wesley, 'candidatura', @cd_wesley_est, 'Entrevista agendada', CONCAT('Vaga Estagiário(a) de Administração: sua entrevista foi marcada para ', DATE_FORMAT(@ent_wesley_est, '%d/%m/%Y às %H:%i'), ' (horário de Brasília).'), 0, @agora - INTERVAL 4990 MINUTE),
  (@u_paulo, 'candidatura', @cd_beatriz_recep, 'Nova candidatura', 'Beatriz Azevedo se candidatou à vaga Recepcionista Corporativa.', 0, @agora - INTERVAL 4595 MINUTE),
  (@u_rita, 'candidatura', @cd_yasmin_sec, 'Nova candidatura', 'Yasmin Carvalho se candidatou à vaga Secretária Executiva Bilíngue.', 0, @agora - INTERVAL 4559 MINUTE),
  (@u_paulo, 'candidatura', @cd_fernanda_fisc, 'Nova candidatura', 'Fernanda Lacerda se candidatou à vaga Analista Fiscal.', 0, @agora - INTERVAL 4391 MINUTE),
  (@u_gabriela, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4315 MINUTE),
  (@u_felipe, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4314 MINUTE),
  (@u_fernanda, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4314 MINUTE),
  (@u_bruno, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4312 MINUTE),
  (@u_olivia, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4312 MINUTE),
  (@u_henrique, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4311 MINUTE),
  (@u_carla, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4310 MINUTE);
INSERT INTO notificacao (usuario_id, tipo, referencia_id, titulo, mensagem, lida, criado_em) VALUES
  (@u_sergio, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4309 MINUTE),
  (@u_nicolas, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4307 MINUTE),
  (@u_yasmin, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4306 MINUTE),
  (@u_wesley, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4305 MINUTE),
  (@u_diego, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4303 MINUTE),
  (@u_mariana, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4303 MINUTE),
  (@u_caua, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4302 MINUTE),
  (@u_beatriz, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4299 MINUTE),
  (@u_karina, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4298 MINUTE),
  (@u_pedro, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4298 MINUTE),
  (@u_eduardo, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4297 MINUTE),
  (@u_vanessa, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4296 MINUTE),
  (@u_ana, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4293 MINUTE),
  (@u_isabela, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4292 MINUTE),
  (@u_joao, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4291 MINUTE),
  (@u_rafael, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4290 MINUTE),
  (@u_renata, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4289 MINUTE),
  (@u_tatiane, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4287 MINUTE),
  (@u_lucas, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4284 MINUTE),
  (@u_ulisses, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4281 MINUTE),
  (@u_debora, 'nova_vaga', @v_com, 'Nova vaga publicada', 'A vaga Assistente Comercial está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 4280 MINUTE),
  (@u_eduardo, 'candidatura', @cd_eduardo_cpr, 'Sua candidatura mudou de etapa', 'Vaga Analista de Contas a Pagar e Receber: sua candidatura agora está em "Em análise".', 0, @agora - INTERVAL 3730 MINUTE),
  (@u_paulo, 'candidatura', @cd_lucas_dp, 'Nova candidatura', 'Lucas Barbosa se candidatou à vaga Analista de Departamento Pessoal.', 0, @agora - INTERVAL 3373 MINUTE),
  (@u_paulo, 'candidatura', @cd_sergio_coord, 'Nova candidatura', 'Sérgio Vasconcelos se candidatou à vaga Coordenador(a) Administrativo.', 0, @agora - INTERVAL 3192 MINUTE),
  (@u_paulo, 'candidatura', @cd_caua_est, 'Nova candidatura', 'Cauã Ribeiro se candidatou à vaga Estagiário(a) de Administração.', 0, @agora - INTERVAL 3134 MINUTE),
  (@u_marina, 'candidatura', @cd_caua_com, 'Nova candidatura', 'Cauã Ribeiro se candidatou à vaga Assistente Comercial.', 0, @agora - INTERVAL 3071 MINUTE),
  (@u_nicolas, 'candidatura', @cd_nicolas_comp, 'Sua candidatura mudou de etapa', 'Vaga Analista de Compras: sua candidatura agora está em "Aprovado".', 0, @agora - INTERVAL 3067 MINUTE),
  (@u_marina, 'candidatura', @cd_pedro_com, 'Nova candidatura', 'Pedro Henrique Gomes se candidatou à vaga Assistente Comercial.', 0, @agora - INTERVAL 3022 MINUTE),
  (@u_marina, 'candidatura', @cd_rafael_contab, 'Nova candidatura', 'Rafael Monteiro se candidatou à vaga Auxiliar de Contabilidade.', 0, @agora - INTERVAL 2936 MINUTE),
  (@u_marina, 'candidatura', @cd_eduardo_comp, 'Nova candidatura', 'Eduardo Salgado se candidatou à vaga Analista de Compras.', 0, @agora - INTERVAL 2936 MINUTE),
  (@u_gabriela, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2873 MINUTE),
  (@u_joao, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2872 MINUTE),
  (@u_ana, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2871 MINUTE),
  (@u_beatriz, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2871 MINUTE),
  (@u_henrique, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2867 MINUTE),
  (@u_vanessa, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2867 MINUTE),
  (@u_eduardo, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2867 MINUTE),
  (@u_yasmin, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2866 MINUTE),
  (@u_renata, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2864 MINUTE),
  (@u_isabela, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2863 MINUTE);
INSERT INTO notificacao (usuario_id, tipo, referencia_id, titulo, mensagem, lida, criado_em) VALUES
  (@u_felipe, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2860 MINUTE),
  (@u_lucas, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2860 MINUTE),
  (@u_karina, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2856 MINUTE),
  (@u_bruno, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2855 MINUTE),
  (@u_diego, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2854 MINUTE),
  (@u_nicolas, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2854 MINUTE),
  (@u_pedro, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2853 MINUTE),
  (@u_tatiane, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2852 MINUTE),
  (@u_wesley, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2851 MINUTE),
  (@u_sergio, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2850 MINUTE),
  (@u_rafael, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2850 MINUTE),
  (@u_olivia, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2849 MINUTE),
  (@u_carla, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2848 MINUTE),
  (@u_mariana, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2847 MINUTE),
  (@u_caua, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2847 MINUTE),
  (@u_ulisses, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2845 MINUTE),
  (@u_fernanda, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2845 MINUTE),
  (@u_debora, 'nova_vaga', @v_arq, 'Nova vaga publicada', 'A vaga Auxiliar de Arquivo e Documentação está aberta. Veja os detalhes e candidate-se.', 0, @agora - INTERVAL 2844 MINUTE),
  (@u_rita, 'candidatura', @cd_karina_cpr, 'Nova candidatura', 'Karina Alves se candidatou à vaga Analista de Contas a Pagar e Receber.', 0, @agora - INTERVAL 1811 MINUTE),
  (@u_marina, 'candidatura', @cd_felipe_com, 'Nova candidatura', 'Felipe Andrade se candidatou à vaga Assistente Comercial.', 0, @agora - INTERVAL 1775 MINUTE),
  (@u_paulo, 'candidatura', @cd_ulisses_coord, 'Nova candidatura', 'Ulisses Pinheiro se candidatou à vaga Coordenador(a) Administrativo.', 0, @agora - INTERVAL 1759 MINUTE),
  (@u_rita, 'candidatura', @cd_gabriela_adm, 'Nova candidatura', 'Gabriela Torres se candidatou à vaga Assistente Administrativo.', 0, @agora - INTERVAL 1650 MINUTE),
  (@u_rita, 'candidatura', @cd_vanessa_arq, 'Nova candidatura', 'Vanessa Cunha se candidatou à vaga Auxiliar de Arquivo e Documentação.', 0, @agora - INTERVAL 1631 MINUTE),
  (@u_paulo, 'candidatura', @cd_henrique_coord, 'Nova candidatura', 'Henrique Duarte se candidatou à vaga Coordenador(a) Administrativo.', 0, @agora - INTERVAL 1589 MINUTE),
  (@u_rita, 'candidatura', @cd_wesley_arq, 'Nova candidatura', 'Wesley Nunes se candidatou à vaga Auxiliar de Arquivo e Documentação.', 0, @agora - INTERVAL 1485 MINUTE),
  (@u_caua, 'candidatura', @cd_caua_com, 'Sua candidatura mudou de etapa', 'Vaga Assistente Comercial: sua candidatura agora está em "Em análise".', 0, @agora - INTERVAL 1184 MINUTE),
  (@u_sergio, 'candidatura', @cd_sergio_coord, 'Sua candidatura mudou de etapa', 'Vaga Coordenador(a) Administrativo: sua candidatura agora está em "Em análise".', 0, @agora - INTERVAL 1030 MINUTE);

-- Conferencia rapida (pode ser removida)
SELECT status, COUNT(*) AS candidaturas FROM candidatura GROUP BY status ORDER BY FIELD(status, 'inscrito', 'em_triagem', 'entrevista', 'aprovado', 'reprovado', 'contratado', 'cancelado');
