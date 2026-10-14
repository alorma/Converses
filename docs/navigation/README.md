# Navegació comuna amb Caducity

Converses i Caducity han de navegar igual. Aquest pla porta Converses al comportament comú; el mirall és `docs/navigation/README.md` de Caducity. Ordre acordat: primer Caducity (fase K), després Converses (fase C), cadascuna en una sessió. Abans de començar la fase C, mirar com va quedar la fase K a Caducity i copiar-ne la solució.

Com es treballa cada fase (tasques, commits, validació): `.claude/rules/phased-work.md`.

## Problemes d'ara

- La toolbar flotant (Persones, Arxivades, Ajustos i el FAB de nota ràpida) viu dins de `PeopleListScreen`, al slot FAB de cada pantalla. Persones ↔ Arxivades és navegació entre destinacions principals, així que la barra es mou amb la pantalla a cada canvi en lloc de quedar-se quieta.
- La pila es manipula directament a `ui/AppNavigation.kt` (`backStack.add` / `removeLastOrNull`), sense el concepte de pestanya: Arxivades és una pantalla més que s'apila.
- Canviar Persones ↔ Arxivades fa la transició horitzontal d'una pantalla nova, no la d'un canvi de pestanya.

## Comportament comú (decidit)

1. **Una sola pila desable** (`rememberNavBackStack`). La primera pestanya (Persones) és la base.
2. **Les pestanyes no acumulen historial:** anar a Arxivades deixa `[Persones, Arxivades]`; anar a Persones deixa `[Persones]`. Les pantalles secundàries s'apilen a sobre. Ajustos és secundària, no pestanya.
3. **En canviar de pestanya es perd l'estat** de la que deixes (scroll, pantalles obertes a sobre). Decidit: són top level i tenen prioritat.
4. **Back:** treu la darrera entrada. A la base, amb una sola entrada, el back és del sistema (tanca l'app amb la seva animació).
5. **Barra fixa** en un `Scaffold` pare, fora de `NavDisplay` (Material 3 i la recepta de UI comuna de Navigation 3). Només visible quan la pantalla de dalt és una pestanya, amb animació d'entrada i sortida. El FAB va dins la barra.
6. **Transicions:** les actuals (`nativeTransition`, `predictiveBack`, vertical per a les modals) i fos en canviar de pestanya.
7. **Espai per a la barra constant** (`LocalFloatingBarPadding`) perquè les llistes no saltin.

La barra no mostra dades de cap persona: moure-la no afecta les persones protegides.

## Fase C · Converses

- [x] **C1.** `8567ab8` Treure la lògica de la pila de `ui/AppNavigation.kt` a una classe navegadora amb la mateixa API que la de Caducity (`navigate`, `switchTab`, `pop`, `resetTo`) sobre un `NavBackStack`. `PeopleRoute` i `ArchivedRoute` passen a ser `TopLevelRoute`; `SettingsRoute` és secundària.
  Validació: test JVM nou, mirall del de Caducity; `testDebugUnitTest`, `ktlintCheck`.
- [x] **C2.** `4032b86` Barra fixa: moure `ConversesFloatingToolbar` de `PeopleListScreen` a un shell al voltant de `NavDisplay`, amb `AnimatedVisibility` i `LocalFloatingBarPadding` en lloc dels `96.dp` fixos de la llista.
  Validació: actualitzar els screenshots de la llista (hi deixa de sortir la barra) i afegir-ne un del shell; emulador: Persones ↔ Arxivades, obrir una persona i tornar.
- [x] **C3.** `ca9dbf9` Fos en canviar de pestanya, amb les transicions finals de Caducity: entrada opaca, fade tardà en sortir, i el fos només com a metadata d'`ArchivedRoute` (només `transitionSpec`: amb `popTransitionSpec` el back fa un parpelleig). Pendent: sensació d'ús (Bernat).
  Validació: emulador; Bernat per a la sensació d'ús.
- [x] **C4.** `de18fea` Actualitzar la norma de `CLAUDE.md`: la toolbar de navegació va al shell, no al slot FAB de cada pantalla.
  Validació: revisió.

Execució: en sèrie, a la sessió principal, sense agents. Un commit per tasca.

Release notes: probablement sí, una línia (la barra ja no es mou en canviar entre Persones i Arxivades). Proposar-la en preparar la versió.
