#!/usr/bin/env bash
# Regenera res/font/roboto_flex.ttf a partir de la Roboto Flex variable completa (~1,7 MB):
# eixos limitats als que fa servir Type.kt (opsz 14–72, wght 400–800, wdth 100–118, la resta fixos)
# i només el llatí (en/ca/es). Ús: designsystem/fonts/subset.sh <RobotoFlex-original.ttf>
# Requereix: pip install fonttools brotli
set -euo pipefail
src="$1"
out="$(dirname "$0")/../src/main/res/font/roboto_flex.ttf"
tmp="$(mktemp -d)"
fonttools varLib.instancer "$src" opsz=14:72 wght=400:800 wdth=100:118 \
  GRAD=0 slnt=0 XOPQ=96 YOPQ=79 XTRA=468 YTUC=712 YTLC=514 YTAS=750 YTDE=-203 YTFI=738 -o "$tmp/inst.ttf"
pyftsubset "$tmp/inst.ttf" \
  --unicodes="U+0020-007E,U+00A0-00FF,U+0100-017F,U+0131,U+0152-0153,U+02C6,U+02DA,U+02DC,U+2013-2014,U+2018-201A,U+201C-201E,U+2022,U+2026,U+2030,U+2039-203A,U+20AC,U+2122,U+2190-2193,U+2212,U+00B7" \
  --layout-features='*' --no-hinting --desubroutinize --output-file="$out"
