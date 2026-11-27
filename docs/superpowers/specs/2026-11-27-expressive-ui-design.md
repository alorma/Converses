# UI «Expressive»: disseny

Data: 2026-11-27. Branca: `expressive-ui`.

La proposta visual original és a `expressive-ui/INSTRUCCIONS.md` i `expressive-ui/pantalles.html` (taulers 14a–14f).
Mana per a colors, mides, formes i textos. Aquest document diu què se'n fa i què canvia respecte d'ella.

## Intenció

Fer les pantalles principals més «expressive» (crema, tipografia ampla Roboto Flex, formes de `MaterialShapes` amb morph,
color de cada persona) i decidir, provant-ho en Bernat mateix, si es queda. Només ho prova en Bernat, amb builds de debug.

## Decisions (diferències amb la proposta)

- **Sense feature flag.** Es treballa a la branca `expressive-ui` i les pantalles es modifiquen directament. No hi ha
  paquet `ui.expressive` ni versions duplicades. Si no convenç, la branca es descarta.
- **Sense canvis de dades:** cap ViewModel nou, cap canvi a Room. Els `UiState` actuals alimenten les pantalles noves.
  Única excepció possible: la fase 8 (propera tasca), una consulta de només lectura al repositori.
- **`:designsystem`** guarda la base nova al paquet `designsystem.expressive`: colors (crema, tinta, accent, neutre),
  `PersonPalette`, rols de tipografia i botons amb forma i morph. Tots els opt-ins hi continuen.
- **`PersonPalette` amb els 7 `AvatarColor`.** La proposta en dona 6. Sand els deriva el constructor amb la mateixa lògica
  i amb contrast AA comprovat.
- **Tema només a 4 pantalles:** llistat (i Arxivades, que és la mateixa pantalla), detall, entrada i nota ràpida. Ajustos,
  Pro, onboarding, sincronització i la resta es queden com estan.
- **`EntryForm` és l'únic formulari d'entrada:** el redisseny de la nota ràpida (14d) també canvia «Nova entrada» i «Edita
  entrada».
- **14c** és una ruta nova (`EntryRoute`) que substitueix el full d'accions de l'entrada.
- **Pantalles grans:** les pantalles noves han de funcionar als panells i diàlegs actuals (≥ 600 dp) sense trencar-se, però
  no es poleixen.

## Fases

Cada fase es valida (ktlint, tests, emulador) i acaba en commits petits.

1. **Base del sistema de disseny:** colors, `PersonPalette` (clara i fosca, contrast AA), tipografia, botons amb forma i
   morph, previews.
2. **Llistat (14a, 14e):** capçalera, xips, grups per dates, files, barra flotant amb el FAB a `AppShell`.
3. **Targeta destacada** (sense la tasca): la persona de l'entrada més recent i no protegida.
4. **Detall (14b):** capçalera amb Clover, accions, recomptes, dades, cronologia en bombolles.
5. **Entrada (14c):** `EntryRoute`.
6. **Nota ràpida (14d):** `EntryForm` amb selector Qui, morph i «Desa».
7. **Moviment i accessibilitat:** «Elimina animacions», text al 200 %, rendiment amb 200 persones, captures regenerades.
8. **Opcional:** propera tasca a la targeta destacada.

## Restriccions que no canvien

- Persones protegides: cap dada abans de desbloquejar (targeta destacada inclosa); `FLAG_SECURE` on toqui.
- Textos a `strings.xml` en en, ca i es. Scaffold a cada pantalla, sense insets manuals.
- Sincronització: si alguna fase escriu, segella amb `stamped(clock())`. En principi cap fase escriu res de nou.

## Queda per al constructor

Mides i valors exactes de cada peça (a la proposta), com repartir els components i l'animació concreta de cada morph.
