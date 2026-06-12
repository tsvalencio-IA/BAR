#!/usr/bin/env bash
set -euo pipefail

RES_DIR="app/src/main/res"

if [[ ! -d "$RES_DIR" ]]; then
  echo "ERRO: diretório $RES_DIR não encontrado."
  exit 1
fi

echo "Validando arquivos de recursos Android..."

# Remove arquivos residuais gerados por upload via navegador, como nomes "1" e "2",
# que não são recursos Android válidos dentro das pastas mipmap.
find "$RES_DIR" -type f \
  \( -path '*/mipmap-*/*' -o -path '*/drawable-*/*' -o -path '*/drawable/*' \) \
  ! -name '*.png' ! -name '*.xml' ! -name '*.webp' ! -name '*.jpg' ! -name '*.jpeg' ! -name '*.gif' \
  -print -delete

# Remove resíduos comuns de sistema e upload.
find "$RES_DIR" -type f \
  \( -name '.DS_Store' -o -name 'Thumbs.db' -o -name 'desktop.ini' -o -name '*~' \) \
  -print -delete

# Falha de forma clara caso ainda exista arquivo inválido nas pastas mipmap.
INVALID="$(find "$RES_DIR" -type f -path '*/mipmap-*/*' ! -name '*.png' ! -name '*.xml' -print)"
if [[ -n "$INVALID" ]]; then
  echo "ERRO: ainda existem recursos mipmap inválidos:"
  echo "$INVALID"
  exit 1
fi

# Garante que todos os PNGs realmente tenham assinatura PNG.
while IFS= read -r -d '' file; do
  signature="$(od -An -tx1 -N8 "$file" | tr -d ' \n')"
  if [[ "$signature" != "89504e470d0a1a0a" ]]; then
    echo "ERRO: arquivo com extensão .png não é PNG válido: $file"
    exit 1
  fi
done < <(find "$RES_DIR" -type f -name '*.png' -print0)

echo "Recursos Android validados com sucesso."
