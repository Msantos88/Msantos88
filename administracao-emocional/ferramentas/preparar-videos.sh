#!/usr/bin/env bash
# Converte os vídeos .mov em MP4 leves para o site e gera a imagem de capa de cada um.
# Uso:  bash preparar-videos.sh  PASTA_COM_OS_MOV  PASTA_DE_SAIDA
# Exemplo: bash preparar-videos.sh ~/Downloads/Videos ./saida
# Requer ffmpeg (Mac: brew install ffmpeg). Ordem dos arquivos = ordem alfabética (Vídeo 1, 2, 3).
set -euo pipefail
ENTRADA="${1:?Informe a pasta com os vídeos .mov}"
SAIDA="${2:?Informe a pasta de saída}"
mkdir -p "$SAIDA"
n=1
find "$ENTRADA" -maxdepth 1 -type f \( -iname '*.mov' -o -iname '*.mp4' \) | sort | while IFS= read -r f; do
  out="$SAIDA/video-$n.mp4"
  echo "[$n] $f"
  # H.264 + AAC, até 1280 px no lado maior, otimizado para abrir rápido na web
  ffmpeg -nostdin -y -loglevel error -i "$f" \
    -vf "scale='min(1280,iw)':'min(1280,ih)':force_original_aspect_ratio=decrease,scale=trunc(iw/2)*2:trunc(ih/2)*2" \
    -c:v libx264 -crf 27 -preset slow -pix_fmt yuv420p -c:a aac -b:a 128k -movflags +faststart "$out"
  # imagem de capa (1 segundo de vídeo)
  ffmpeg -nostdin -y -loglevel error -ss 1 -i "$out" -frames:v 1 -q:v 4 "$SAIDA/video-$n.jpg"
  echo "    -> $(du -h "$out" | cut -f1)"
  n=$((n+1))
done
echo "Pronto. Envie video-N.mp4 e video-N.jpg para a pasta assets/videos/ do site."
