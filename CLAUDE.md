# Converses

App Android per recordar què he dit, què m'han dit i què ha passat amb cada persona (xat, xarxes o en persona).

## Referències (font de veritat)

- `docs/plan.md`: especificació completa (stack, tema, components, pantalles, model de dades, ordre d'implementació).
- `docs/*.png`: captures del disseny (Material 3 Expressive, 8 pantalles). Usar-les per a mides, colors, cantonades i jerarquia visual.

## Stack

Kotlin, Jetpack Compose, Material 3 Expressive (`material3` 1.4+), MaterialKolor, Room amb KSP, Hilt, Navigation Compose.

## Normes

- Treballar per passos seguint "Ordre d'implementació" de `docs/plan.md`. No avançar al pas següent sense que l'usuari ho validi.
- Abans d'afegir una llibreria, comprovar a la documentació oficial la seva versió estable actual.
- Concentrar tots els `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` al mòdul `designsystem`; les pantalles no els repeteixen.
- Textos de la UI sempre a `strings.xml`: anglès per defecte (`values/`), amb català (`values-ca/`) i castellà (`values-es/`).
- Gradle amb version catalog (`gradle/libs.versions.toml`).
