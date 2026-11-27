# UI «Expressive» · Fase 1: base del sistema de disseny — pla d'implementació

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tenir a `:designsystem` les peces que faran servir les fases 2–7: el tema crema, `PersonPalette`, els rols de tipografia i els botons amb forma i morph. Cap pantalla de `:app` no canvia encara.

**Architecture:** Un paquet nou, `com.alorma.contactnotes.designsystem.expressive`. `ExpressiveTheme` embolica un contingut amb un `MaterialExpressiveTheme` de colors crema, i un `CompositionLocal` exposa els tokens que el `ColorScheme` no té. `PersonPalette` és una taula fixa (sense MaterialKolor) per a cada `AvatarColor`, en clar i en fosc. Els botons amb forma fan servir `MaterialShapes` i un `Morph` cap al cercle.

**Tech Stack:** Kotlin, Compose, `material3` 1.6.0-alpha01 (`MaterialShapes`, `MotionScheme`, `TextAutoSize`), `androidx.graphics:graphics-shapes`, JUnit 4, Compose Preview Screenshot Testing.

**Spec:** `docs/superpowers/specs/2026-11-27-expressive-ui-design.md`. Els valors exactes surten de `docs/superpowers/specs/expressive-ui/INSTRUCCIONS.md` (seccions 2–5).

## Global Constraints

- Tots els `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` es queden a `:designsystem`.
- Cap canvi a `:app`, ni a Room, ni als ViewModels, en aquesta fase.
- Codi en anglès i KDoc en català, com la resta del mòdul. ktlint net (`./gradlew ktlintCheck`).
- Els noms dels mètodes dels screenshot tests van en camelCase, sense `_`.
- Branca `expressive-ui`. Un commit per tasca, i només quan la validació passa.

## Review Focus

1. **Colors derivats** (Préssec, Llimona i Sand en fosc, Sand en clar): han de complir AA (≥ 4,5) a cada parella de text. El test de contrast de la tasca 2 ho comprova per als 7 colors.
2. **«Color de la persona» apagat** (`LocalPersonColorTheme` = false): la paleta passa a ser neutra (superfície `#EFEBE0`, text tinta) i no la de l'avatar. El test de la tasca 2 hi afegeix `neutralPalette`.
3. **Nom llarg amb text gran:** el títol ample ha de saltar de línia (com a màxim 2) i encongir-se fins a 28 sp, sense tallar-se. Té una preview amb `fontScale = 2f` a la tasca 3.
4. **«Elimina animacions» activat:** els botons amb forma no fan morph. Es comprova a l'emulador a la tasca 4.
5. **Objectiu tàctil d'un botó petit:** un `ShapeButton` de menys de 48 dp continua tenint un objectiu tàctil de 48 dp, amb `minimumInteractiveComponentSize()`.

---

### Task 1: Colors de l'app i `ExpressiveTheme`

**Files:**
- Create: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/ExpressiveColors.kt`
- Create: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/ExpressiveTheme.kt`
- Test: `designsystem/src/test/java/com/alorma/contactnotes/designsystem/expressive/ExpressiveColorsContrastTest.kt` i el helper `Contrast.kt` al mateix paquet

**Interfaces:**
- Produces:
  - `data class ExpressiveColors(val background: Color, val ink: Color, val inkSecondary: Color, val neutral: Color, val accent: Color, val onAccent: Color)`
  - `val LightExpressiveColors`, `val DarkExpressiveColors`
  - `val LocalExpressiveColors: ProvidableCompositionLocal<ExpressiveColors>`
  - `object Expressive { val colors: ExpressiveColors @Composable get() }`
  - `@Composable fun ExpressiveTheme(content: @Composable () -> Unit)`
  - Les funcions de contrast es posen en un helper intern de test, `contrast(a: Color, b: Color): Double`, i la tasca 2 també el fa servir. Ha d'anar a `designsystem/src/test/.../expressive/Contrast.kt`.

Valors (secció 2 de la proposta):

| Token | Clar | Fosc |
| --- | --- | --- |
| background | `#FBF8F1` | `#14130F` |
| ink | `#1C1B17` | `#F1EDE2` |
| inkSecondary | `#5A574D` | `#B8B2A3` |
| neutral | `#EFEBE0` | `#2A2822` |
| accent | `#F2E8A6` | `#E4D77A` |
| onAccent | `#1C1B17` | `#1C1B17` |

- [ ] **Step 1: Write the failing test** `ExpressiveColorsContrastTest.textIsReadableOnEverySurface`. Per a clar i fosc, comprova que `contrast(...) >= 4.5` per a cadascuna d'aquestes parelles:
  - `ink` sobre `background`;
  - `ink` sobre `neutral`;
  - `inkSecondary` sobre `background`;
  - `onAccent` sobre `accent`.

  El missatge diu quina parella i quin mode falla.
- [ ] **Step 2:** `./gradlew :designsystem:testDebugUnitTest --tests '*ExpressiveColorsContrastTest'`. Expected: FAIL (no compila).
- [ ] **Step 3: Implement** `ExpressiveColors.kt` amb els valors de la taula.
- [ ] **Step 4: Implement `ExpressiveTheme`.** Llegeix `isDarkTheme` i proveeix `LocalExpressiveColors`. Embolica el contingut en un `MaterialExpressiveTheme` que fa servir:
  - `motionScheme = MotionScheme.expressive()`;
  - la tipografia i les formes actuals (`MaterialTheme.typography`, `MaterialTheme.shapes`);
  - `MaterialTheme.colorScheme.copy(...)` amb aquest mapatge:
    - `background` i `surface`: background;
    - `onBackground` i `onSurface`: ink;
    - `onSurfaceVariant`: inkSecondary;
    - tots els `surfaceContainer*` i `surfaceVariant`: neutral;
    - `primary`: ink, i `onPrimary`: background;
    - `primaryContainer` i `secondaryContainer`: accent;
    - `onPrimaryContainer` i `onSecondaryContainer`: onAccent.

  Els colors d'error no es toquen. No fa servir color dinàmic.
- [ ] **Step 5:** el mateix test, ara PASS. També `./gradlew :designsystem:ktlintCheck`.
- [ ] **Step 6: Commit** amb el missatge «Add the expressive app colors and theme».

### Task 2: `PersonPalette`

**Files:**
- Create: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/PersonPalette.kt`
- Test: `designsystem/src/test/java/com/alorma/contactnotes/designsystem/expressive/PersonPaletteContrastTest.kt`

**Interfaces:**
- Consumes: `ExpressiveColors`, `LocalPersonColorTheme` i `isDarkTheme` (tots dos ja existeixen a `designsystem.theme`).
- Produces:
  - `data class PersonPalette(val surface: Color, val avatar: Color, val accent: Color, val detailBackground: Color, val featured: Color, val onFeatured: Color)`:
    - `accent` és el «text fosc» en clar i el color de l'avatar en fosc; serveix per al nom, les overlines i el botó «Nova entrada»;
    - `featured` i `onFeatured` són la targeta destacada.
  - `fun AvatarColor.palette(isDark: Boolean): PersonPalette`
  - `fun neutralPalette(isDark: Boolean): PersonPalette`: el que es fa servir amb «Color de la persona» apagat. Fa servir la superfície neutra i la tinta de la tasca 1.
  - `@Composable fun rememberPersonPalette(color: AvatarColor?): PersonPalette`: dona `neutralPalette` si `color == null` o si `LocalPersonColorTheme` és fals.

**Clar (secció 4 de la proposta):** `featured = accent` (el text fosc) i `onFeatured = #FFFFFF`.

| Color | surface | avatar | accent | detailBackground |
| --- | --- | --- | --- | --- |
| Mint | `#E2F2E8` | `#C2E8D0` | `#245C3F` | `#F1F8F3` |
| Lavender | `#ECE7F8` | `#D9CFF5` | `#4F3E92` | `#F6F3FC` |
| Peach | `#FBE7DC` | `#FFD3BA` | `#8A3E22` | `#FDF4EE` |
| Sky | `#E0EDF8` | `#C6DFF5` | `#24557F` | `#F2F7FC` |
| Rose | `#F9E3EA` | `#F5CADA` | `#8C3456` | `#FCF2F5` |
| Lemon | `#F5F0CF` | `#EEE39C` | `#5E5410` | `#FBF9EA` |
| Sand | derivat | derivat | derivat | derivat |

**Fosc:**
- `avatar` és el mateix que en clar.
- `accent = avatar`.
- `featured = avatar`, i `onFeatured` és un to molt fosc del mateix color (`#10261A` en Mint).
- `detailBackground` és un to entre `#14130F` i `surface`.
- Superfícies donades per la proposta: Mint `#1F3328`, Lavender `#2A2540`, Sky `#1C2B3A` i Rose `#3A2129`.
- La resta (Peach, Lemon, Sand) i tots els `onFeatured` i `detailBackground` foscos es deriven amb la mateixa lluminositat que els donats.

Sand en clar parteix de l'avatar actual (`#E8DDCB`) i de la llavor `#86704C`. Cada valor derivat porta el comentari `// derivat` al costat.

- [ ] **Step 1: Write the failing test** `PersonPaletteContrastTest.textIsReadableForEveryPersonColor`. Per a cada `AvatarColor.entries` (i també per a `neutralPalette`), en clar i en fosc, comprova que el contrast és ≥ 4,5 en aquestes parelles:
  - `accent` sobre `surface`;
  - `accent` sobre `detailBackground`;
  - `onFeatured` sobre `featured`;
  - `Expressive ink` del mode corresponent sobre `surface` (és el text de les files).
- [ ] **Step 2: Write the failing test** `PersonPaletteContrastTest.givenValuesMatchTheProposal`. Comprova que `AvatarColor.Mint.palette(false)` és igual a la fila Mint de la taula de clar, que el `surface` fosc de Mint és `#1F3328` i que l'`onFeatured` fosc de Mint és `#10261A`.
- [ ] **Step 3:** `./gradlew :designsystem:testDebugUnitTest --tests '*PersonPaletteContrastTest'`. Expected: FAIL (no compila).
- [ ] **Step 4: Implement** `PersonPalette.kt` amb un `when` exhaustiu sobre `AvatarColor`, perquè un color nou no compili sense paleta. Ajusta els valors derivats fins que el test passi.
- [ ] **Step 5:** el test, ara PASS. També `ktlintCheck`.
- [ ] **Step 6: Commit** amb el missatge «Add the expressive person palette».

### Task 3: Tipografia ampla

**Files:**
- Create: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/ExpressiveType.kt`
- Create: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/ExpressivePreviews.kt`, amb les previews de tot el paquet
- Modify: `designsystem/src/screenshotTest/kotlin/com/alorma/contactnotes/designsystem/ComponentsScreenshotTests.kt`

**Interfaces:**
- Produces:
  - `object ExpressiveType { fun display(size: TextUnit): TextStyle; fun quote(size: TextUnit): TextStyle; val overline: TextStyle; val body: TextStyle }`
  - `@Composable fun WideTitle(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, maxSize: TextUnit = 44.sp, minSize: TextUnit = 28.sp)`: fa servir `display(maxSize)`, `maxLines = 2` i `autoSize = TextAutoSize.StepBased(minSize, maxSize)`.

Valors (secció 3 de la proposta). Totes les famílies fan servir `R.font.roboto_flex`, amb el mateix patró que `robotoFlex()` a `theme/Type.kt`. Fes-lo `internal` i reutilitza'l, no el copiïs.

- **display:**
  - `FontVariation`: `width(151f)`, `weight(900)` i `opticalSizing(144.sp)`;
  - `letterSpacing = (-0.6).sp`;
  - `lineHeight ≈ size × 1.05`.
- **quote:** `weight(600)` i `letterSpacing = (-0.3).sp`.
- **overline:**
  - 12 sp, `weight(700)`, `letterSpacing = 1.1.sp`;
  - el text el passa a majúscules qui el crida (`uppercase()`), no l'estil.
- **body:** `weight(400)`, 16 sp.

- [ ] **Step 1: Add previews** a `ExpressivePreviews.kt`:
  - `ExpressiveTypePreview`, dins d'`ExpressiveTheme`: «Converses» en display 34, «fa 2 h» en display 44, una cita en 27 i «JORDI PUIG · XAT» en overline;
  - `WideTitleLongNamePreview`: un `WideTitle` de 44 sp amb «Maria Montserrat Puig i Ferrer» en una amplada de 360 dp.
- [ ] **Step 2: Add screenshot tests** a `ComponentsScreenshotTests`:
  - `expressiveType`, amb `@ComponentPreviews`;
  - `wideTitleLongName`, amb `@ComponentPreviews` i una anotació nova, `@Preview(name = "Light huge font", locale = "en", fontScale = 2f)`, que s'ha de definir al costat de `LargeFontComponentPreviews` com a `HugeFontComponentPreviews`.
- [ ] **Step 3: Implement** `ExpressiveType.kt`.
- [ ] **Step 4:** `./gradlew :designsystem:updateDebugScreenshotTest --tests '*ComponentsScreenshotTests.wideTitleLongName' --tests '*ComponentsScreenshotTests.expressiveType'`, i obre les PNG de `designsystem/src/screenshotTestDebug/reference/`. Expected:
  - el display es veu ample i molt gruixut, no amb el pes per defecte;
  - amb text a 200 %, el nom llarg ocupa com a màxim 2 línies i no es talla.
- [ ] **Step 5:** `./gradlew :designsystem:validateDebugScreenshotTest` i `ktlintCheck`. Expected: PASS.
- [ ] **Step 6: Commit** amb les referències noves i el missatge «Add the expressive typography roles».

### Task 4: Botons amb forma i morph

**Files:**
- Create: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/MorphShape.kt`
- Create: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/ShapeButton.kt`
- Create: `designsystem/src/main/java/com/alorma/contactnotes/designsystem/expressive/ReduceMotion.kt`
- Modify: `ExpressivePreviews.kt` i `ComponentsScreenshotTests.kt`
- Modify: `app/src/main/java/com/alorma/contactnotes/ui/onboarding/OnboardingScreen.kt:136`, perquè faci servir `rememberReduceMotion()` en lloc de la seva lectura pròpia. És l'únic canvi a `:app` i elimina el duplicat.

**Interfaces:**
- Produces:
  - `class MorphShape(private val morph: Morph, private val progress: Float) : Shape`. Escala el camí de `Morph` (en coordenades 0..1) a la mida que rep.
  - `@Composable fun rememberReduceMotion(): Boolean`. És cert si `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`.
  - `ShapeButton`:

    ```kotlin
    @Composable fun ShapeButton(
      onClick: () -> Unit,
      shape: RoundedPolygon,
      size: Dp,
      containerColor: Color,
      contentColor: Color,
      contentDescription: String?,
      modifier: Modifier = Modifier,
      selected: Boolean = false,
      content: @Composable () -> Unit,
    )
    ```

  Comportament:
  - En prémer (`collectIsPressedAsState`), o mentre `selected` és cert, fa morph de `shape` cap a `MaterialShapes.Circle` amb `MaterialTheme.motionScheme.fastSpatialSpec()`.
  - Amb `rememberReduceMotion()`, salta directament a la forma final, sense animar.
  - El retall es fa amb `graphicsLayer { clip = true; this.shape = ... }`.
  - El botó porta `Role.Button`, la `contentDescription` (quan no és null) i `minimumInteractiveComponentSize()`.

- [ ] **Step 1: Add previews.** `ShapeButtonPreview`, dins d'`ExpressiveTheme`, mostra:
  - una galeta `Cookie9Sided` de 80 dp amb `edit` en `#8A3E22`;
  - una de 52 dp en accent amb `person_add`;
  - un `Clover4Leaf` de 92 dp `selected = true` (es veu en forma de cercle);
  - una de 48 dp neutra.

  Afegeix-hi el screenshot test `shapeButton` (`@ComponentPreviews`).
- [ ] **Step 2: Implement** `ReduceMotion.kt`, `MorphShape.kt` i `ShapeButton.kt`, i canvia `OnboardingScreen` perquè faci servir `rememberReduceMotion()`.
- [ ] **Step 3:** `./gradlew :designsystem:updateDebugScreenshotTest --tests '*ComponentsScreenshotTests.shapeButton'` i mira la PNG. Expected: les formes surten senceres i centrades, i el Clover seleccionat surt en forma de cercle.
- [ ] **Step 4: Comprova a l'emulador.** Posa la preview `ShapeButtonPreview` en una pantalla de debug temporal, o fes servir «Run Preview» a l'emulador.
  - En prémer, la galeta es transforma en cercle i torna enrere.
  - Amb «Elimina animacions» activat, no hi ha transició.
  - La pantalla temporal no s'ha de quedar al commit.
- [ ] **Step 5:** `./gradlew ktlintCheck testDebugUnitTest :designsystem:validateDebugScreenshotTest`. Expected: PASS.
- [ ] **Step 6: Commit** amb el missatge «Add shape buttons that morph when pressed».

---

## Fases següents (un pla per fase, quan s'hi arribi)

2. Llistat (14a, 14e) · 3. Targeta destacada · 4. Detall (14b) · 5. Entrada (14c, `EntryRoute`) · 6. Nota ràpida (14d, `EntryForm`) · 7. Moviment i accessibilitat · 8. Opcional: propera tasca.
