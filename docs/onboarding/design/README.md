# Referència de disseny (llenç «Converses»)

Fitxers HTML exportats del llenç de disseny, **només com a referència de lectura**.
No s'obren sols al navegador: depenen del runtime del llenç (`support.js`), que no s'inclou.

Com llegir-los:
- L'HTML de dins de `<x-dc>` és el layout, amb els estils en línia: mides, padding, radis i gaps en px (= dp).
- Els colors són variables CSS (`--pri`, `--pri-c`, `--card`, `--on-var`…) que corresponen als rols de Material 3:
  `--bg` surface · `--card` surfaceContainerLowest · `--card-hi` surfaceContainerHigh · `--s-high` surfaceContainerHigh (diàlegs) ·
  `--s-highest` surfaceContainerHighest · `--on` onSurface · `--on-var` onSurfaceVariant · `--outline` outline · `--outline-var` outlineVariant ·
  `--pri` primary · `--on-pri` onPrimary · `--pri-c` primaryContainer · `--on-pri-c` onPrimaryContainer · `--err*` error.
  Els valors concrets es generen a `themeVars` / `personTheme` (dins del `<script>`); a l'app els dona MaterialKolor.
- `{{...}}`, `<sc-for>` i `<sc-if>` són plantilles; el mètode `extra()` del `<script>` té les dades i l'estat (textos, opcions, temps d'animació).
- Formes dels avatars: `shape(size, n, amp)` → `MaterialShapes` (9 = Cookie9Sided, 4 = Clover4Leaf, 6 = Flower, 12 = Sunny).
- Animacions (tanda 4): `@keyframes ob-*` a la capçalera i els `animation:` en línia (durada, retard, corba).

Si hi ha cap diferència, mana el document `docs/tanda-N-*.md`.
