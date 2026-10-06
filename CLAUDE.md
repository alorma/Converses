# Converses

App Android per recordar què he dit, què m'han dit i què ha passat amb cada persona (xat, xarxes o en persona).

## Referències (font de veritat)

- `docs/plan.md`: estat de l'app i del pla (stack, tema, components, pantalles, model de dades, decisions i pendent). Mantenir-lo actualitzat en acabar cada tasca.

## Stack

Kotlin, Jetpack Compose, Material 3 Expressive (`material3` 1.5 alpha: a la 1.4 estable les API d'Expressive són internes), MaterialKolor, Room amb KSP, Koin, Navigation 3, DataStore.

## Normes

- Treballar per passos petits i validar-los abans de continuar; l'estat i el pendent són a `docs/plan.md`.
- Abans d'afegir una llibreria, comprovar a la documentació oficial la seva versió estable actual.
- Concentrar tots els `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` al mòdul `designsystem`; les pantalles no els repeteixen.
- Textos de la UI sempre a `strings.xml`: anglès per defecte (`values/`), amb català (`values-ca/`) i castellà (`values-es/`).
- Gradle amb version catalog (`gradle/libs.versions.toml`).
