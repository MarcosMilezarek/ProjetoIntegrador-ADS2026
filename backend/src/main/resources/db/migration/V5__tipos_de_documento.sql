-- O tipo de documento deixa de ser texto livre e passa a ser uma lista fechada
-- (enum Documento.Tipo no backend). Nenhum registro e apagado:
--   * o texto digitado antes fica em tipo_informado;
--   * tipo recebe o codigo da lista quando o texto corresponde a um item;
--   * o que nao corresponde fica com tipo nulo e continua listado como enviado.
-- A collation da tabela ignora maiusculas e acentos, por isso a comparacao abaixo
-- usa os textos sem acento.

ALTER TABLE documento CHANGE COLUMN tipo tipo_informado VARCHAR(100) NULL;

ALTER TABLE documento ADD COLUMN tipo ENUM('rg', 'cpf', 'ctps', 'titulo_eleitor', 'comprovante_residencia',
    'comprovante_escolaridade', 'foto_3x4', 'pis_pasep', 'certidao_nascimento_casamento', 'dados_bancarios',
    'certificado_reservista') NULL AFTER candidatura_id;

UPDATE documento SET tipo = 'rg'
WHERE TRIM(tipo_informado) IN ('RG', 'Identidade', 'Carteira de identidade', 'Registro geral');
UPDATE documento SET tipo = 'cpf'
WHERE TRIM(tipo_informado) IN ('CPF');
UPDATE documento SET tipo = 'ctps'
WHERE TRIM(tipo_informado) IN ('CTPS', 'Carteira de trabalho', 'Carteira de trabalho (CTPS)');
UPDATE documento SET tipo = 'titulo_eleitor'
WHERE TRIM(tipo_informado) IN ('Titulo de eleitor', 'Titulo eleitoral');
UPDATE documento SET tipo = 'comprovante_residencia'
WHERE TRIM(tipo_informado) IN ('Comprovante de residencia', 'Comprovante de endereco');
UPDATE documento SET tipo = 'comprovante_escolaridade'
WHERE TRIM(tipo_informado) IN ('Comprovante de escolaridade', 'Diploma', 'Diploma ou declaracao de matricula',
    'Declaracao de matricula', 'Historico escolar', 'Certificado de conclusao');
UPDATE documento SET tipo = 'foto_3x4'
WHERE TRIM(tipo_informado) IN ('Foto 3x4', 'Foto');
UPDATE documento SET tipo = 'pis_pasep'
WHERE TRIM(tipo_informado) IN ('PIS', 'PASEP', 'PIS/PASEP', 'PIS ou PASEP', 'NIS');
UPDATE documento SET tipo = 'certidao_nascimento_casamento'
WHERE TRIM(tipo_informado) IN ('Certidao de nascimento', 'Certidao de casamento', 'Certidao de nascimento ou casamento');
UPDATE documento SET tipo = 'dados_bancarios'
WHERE TRIM(tipo_informado) IN ('Dados bancarios', 'Comprovante de conta bancaria');
UPDATE documento SET tipo = 'certificado_reservista'
WHERE TRIM(tipo_informado) IN ('Certificado de reservista', 'Reservista');

-- Um documento por tipo em cada candidatura. Se dois textos antigos caem no mesmo tipo,
-- o envio mais recente fica com o tipo e os anteriores voltam a ficar sem tipo
-- (o registro e o arquivo continuam la).
UPDATE documento antigo
    JOIN documento recente
        ON recente.candidatura_id = antigo.candidatura_id
       AND recente.tipo = antigo.tipo
       AND recente.id > antigo.id
SET antigo.tipo = NULL;

ALTER TABLE documento ADD UNIQUE KEY uk_documento_candidatura_tipo (candidatura_id, tipo);
