#!/bin/bash
# Cria os PDFs de exemplo referenciados por database/seed.sql (curriculos e documentos de contratacao).
# Sem eles o seed funciona, mas o download desses arquivos responde 404.
#
# Uso (APP_UPLOAD_DIR e o mesmo diretorio do backend; na VPS, /opt/app/uploads):
#   APP_UPLOAD_DIR=/opt/app/uploads bash database/seed-arquivos.sh
set -euo pipefail
DIR="${APP_UPLOAD_DIR:-./uploads}"
mkdir -p "$DIR/curriculo" "$DIR/documento"
BASE="$(mktemp)"
trap 'rm -f "$BASE"' EXIT
base64 -d > "$BASE" <<'PDF'
JVBERi0xLjQKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4KZW5kb2JqCjIgMCBvYmoKPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFszIDAgUl0gL0NvdW50IDEgPj4KZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL1BhZ2UgL1BhcmVudCAyIDAgUiAvTWVkaWFCb3ggWzAgMCAzNjAgMTYwXSAvQ29udGVudHMgNCAwIFIgL1Jlc291cmNlcyA8PCAvRm9udCA8PCAvRjEgNSAwIFIgPj4gPj4gPj4KZW5kb2JqCjQgMCBvYmoKPDwgL0xlbmd0aCAxMTEgPj4Kc3RyZWFtCkJUIC9GMSAxMyBUZiAyNCA5NiBUZCAoRG9jdW1lbnRvIGRlIGV4ZW1wbG8pIFRqIDAgLTIyIFRkIC9GMSAxMCBUZiAoRGFkbyBmaWN0aWNpbyBkbyBhbWJpZW50ZSBkZSB0ZXN0ZXMuKSBUaiBFVAplbmRzdHJlYW0KZW5kb2JqCjUgMCBvYmoKPDwgL1R5cGUgL0ZvbnQgL1N1YnR5cGUgL1R5cGUxIC9CYXNlRm9udCAvSGVsdmV0aWNhID4+CmVuZG9iagp4cmVmCjAgNgowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDAwMDkgMDAwMDAgbiAKMDAwMDAwMDA1OCAwMDAwMCBuIAowMDAwMDAwMTE1IDAwMDAwIG4gCjAwMDAwMDAyNDEgMDAwMDAgbiAKMDAwMDAwMDQwMyAwMDAwMCBuIAp0cmFpbGVyCjw8IC9TaXplIDYgL1Jvb3QgMSAwIFIgPj4Kc3RhcnR4cmVmCjQ3MwolJUVPRgo=
PDF

for nome in \
  seed-cv-ana.pdf \
  seed-cv-bruno.pdf \
  seed-cv-carla.pdf \
  seed-cv-gabriela.pdf \
  seed-cv-henrique.pdf \
  seed-cv-karina.pdf \
  seed-cv-mariana.pdf \
  seed-cv-olivia.pdf \
  seed-cv-renata.pdf \
  seed-cv-sergio.pdf \
  seed-cv-ulisses.pdf \
  seed-cv-yasmin.pdf \
  seed-cv-caua.pdf \
  seed-cv-debora.pdf \
  seed-cv-fernanda.pdf; do
  cp "$BASE" "$DIR/curriculo/$nome"
done

for nome in \
  seed-doc-bruno_adm-rg.pdf \
  seed-doc-bruno_adm-cpf.pdf \
  seed-doc-bruno_adm-ctps.pdf \
  seed-doc-bruno_adm-titulo_eleitor.pdf \
  seed-doc-bruno_adm-comprovante_residencia.pdf \
  seed-doc-bruno_adm-comprovante_escolaridade.pdf \
  seed-doc-bruno_adm-foto_3x4.pdf \
  seed-doc-bruno_adm-pis_pasep.pdf \
  seed-doc-bruno_adm-certidao_nascimento_casamento.pdf \
  seed-doc-bruno_adm-dados_bancarios.pdf \
  seed-doc-carla_fin-rg.pdf \
  seed-doc-carla_fin-cpf.pdf \
  seed-doc-carla_fin-ctps.pdf \
  seed-doc-carla_fin-titulo_eleitor.pdf \
  seed-doc-carla_fin-comprovante_residencia.pdf \
  seed-doc-carla_fin-comprovante_escolaridade.pdf \
  seed-doc-carla_fin-foto_3x4.pdf \
  seed-doc-carla_fin-pis_pasep.pdf \
  seed-doc-carla_fin-certidao_nascimento_casamento.pdf \
  seed-doc-carla_fin-dados_bancarios.pdf \
  seed-doc-renata_fin-rg.pdf \
  seed-doc-renata_fin-cpf.pdf \
  seed-doc-renata_fin-ctps.pdf \
  seed-doc-renata_fin-titulo_eleitor.pdf \
  seed-doc-renata_fin-comprovante_residencia.pdf \
  seed-doc-renata_fin-comprovante_escolaridade.pdf \
  seed-doc-renata_fin-foto_3x4.pdf \
  seed-doc-renata_fin-pis_pasep.pdf \
  seed-doc-renata_fin-certidao_nascimento_casamento.pdf \
  seed-doc-renata_fin-dados_bancarios.pdf \
  seed-doc-ulisses_dp-rg.pdf \
  seed-doc-ulisses_dp-cpf.pdf \
  seed-doc-ulisses_dp-ctps.pdf \
  seed-doc-ulisses_dp-titulo_eleitor.pdf \
  seed-doc-ulisses_dp-comprovante_residencia.pdf \
  seed-doc-ulisses_dp-foto_3x4.pdf \
  seed-doc-olivia_recep-rg.pdf \
  seed-doc-olivia_recep-cpf.pdf \
  seed-doc-olivia_recep-ctps.pdf \
  seed-doc-olivia_recep-titulo_eleitor.pdf \
  seed-doc-olivia_recep-comprovante_residencia.pdf \
  seed-doc-olivia_recep-comprovante_escolaridade.pdf \
  seed-doc-olivia_recep-foto_3x4.pdf \
  seed-doc-olivia_recep-pis_pasep.pdf \
  seed-doc-olivia_recep-certidao_nascimento_casamento.pdf \
  seed-doc-olivia_recep-dados_bancarios.pdf \
  seed-doc-fernanda_contab-rg.pdf \
  seed-doc-fernanda_contab-cpf.pdf \
  seed-doc-fernanda_contab-dados_bancarios.pdf \
  seed-doc-lucas_fatur-rg.pdf \
  seed-doc-lucas_fatur-cpf.pdf \
  seed-doc-lucas_fatur-ctps.pdf \
  seed-doc-lucas_fatur-titulo_eleitor.pdf \
  seed-doc-lucas_fatur-comprovante_residencia.pdf \
  seed-doc-lucas_fatur-comprovante_escolaridade.pdf \
  seed-doc-lucas_fatur-foto_3x4.pdf \
  seed-doc-lucas_fatur-pis_pasep.pdf \
  seed-doc-lucas_fatur-certidao_nascimento_casamento.pdf \
  seed-doc-lucas_fatur-dados_bancarios.pdf \
  seed-doc-lucas_fatur-certificado_reservista.pdf \
  seed-doc-debora_aux-rg.pdf \
  seed-doc-debora_aux-cpf.pdf \
  seed-doc-debora_aux-ctps.pdf \
  seed-doc-debora_aux-titulo_eleitor.pdf \
  seed-doc-debora_aux-comprovante_residencia.pdf \
  seed-doc-debora_aux-comprovante_escolaridade.pdf \
  seed-doc-debora_aux-foto_3x4.pdf \
  seed-doc-debora_aux-pis_pasep.pdf \
  seed-doc-debora_aux-certidao_nascimento_casamento.pdf \
  seed-doc-debora_aux-dados_bancarios.pdf; do
  cp "$BASE" "$DIR/documento/$nome"
done

echo "PDFs de exemplo criados em $DIR ($(ls "$DIR/curriculo" | grep -c '^seed-') curriculos, $(ls "$DIR/documento" | grep -c '^seed-') documentos)."
