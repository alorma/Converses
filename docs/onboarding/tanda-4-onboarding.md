# Tanda 4 · Onboarding animat

Referència visual: llenç «Converses», secció **«Onboarding · 3 passos»**:

- **0a** · Recorda cada conversa.
- **0b** · Tot en una cronologia.
- **0c** · Només per a tu.

Hi ha versió en clar i en fosc. Al llenç, tocar la il·lustració repeteix l'animació.

Aquesta tanda és independent de les altres. Si la tanda 2 encara no s'ha fet, la il·lustració del pas 2 ja fa servir els tipus nous (Luna · Jo · Apunt): és només un dibuix i no depèn del codi de la cronologia.

## Objectiu

Una benvinguda de 3 pantalles que es mostra **només el primer cop** que s'obre l'app, amb animacions curtes a cada il·lustració. En acabar, porta al llistat buit (que ja té «Afegeix la primera persona»).

---

## 1. Flux i persistència

- Nova clau de DataStore: `onboarding_done` (booleà, per defecte `false`), dins de `SettingsRepository` / `AppSettings`.
- `MainActivity` decideix la destinació inicial:
  - si `onboarding_done == false` → `OnboardingRoute`;
  - si no → `PeopleRoute`, com ara.
- Cal llegir la clau abans del primer frame, igual que el tema, perquè no parpellegi el llistat.
- **Omet** (a dalt a la dreta, als passos 1 i 2) i **Comença** (pas 3):
  1. desen `onboarding_done = true`;
  2. substitueixen la ruta per `PeopleRoute`, sense que es pugui tornar enrere a l'onboarding.
- Enrere del sistema:
  - al pas 2 o 3, torna al pas anterior;
  - al pas 1, surt de l'app sense marcar-lo com a fet.
- Si l'usuari ja té persones (per exemple, després d'una restauració), es marca com a fet i no es mostra.

## 2. Estructura de la pantalla

`ui/onboarding/OnboardingScreen.kt`:

- `HorizontalPager` de 3 pàgines, que permet lliscar.
- Barra superior de 64 dp: **Omet** com a `TextButton`, en `onSurfaceVariant`, 15/700. No surt al pas 3.
- **Il·lustració:** zona de 380 dp d'alt, amb 20 dp de marge lateral.
- **Text:** 20 dp per sota, amb 28 dp de marge lateral:
  - títol `display` de 34 sp / 800, amb amplada 118 i mida òptica 72;
  - cos de 17 sp en `onSurfaceVariant`, amb interlineat de 1,45;
  - 12 dp entre títol i cos.
- **Peu**, a 32 dp de la vora inferior, amb `navigationBarsPadding`:
  - **indicador de pàgina:** tres punts de 8 dp, separats 6 dp; l'actiu s'allarga fins a 28 dp en `primary`, amb una animació de 350 ms amb rebot; els altres van en `outlineVariant`;
  - **botó:** pastilla `primary` de 56 dp, «Següent →» als passos 1 i 2 i «Comença» al pas 3.

Tema: el de l'app (monocrom amb color dinàmic). Els avatars fan servir els pastels fixos.

## 3. Animacions

### Regles comunes

- Cada il·lustració té el seu composable a `ui/onboarding/OnboardingIllustrations.kt`.
- L'animació comença quan la pàgina queda **assentada** (`pagerState.settledPage == index`).
- Es **repeteix** quan es torna a la pàgina i quan es toca la il·lustració. Per fer-ho, cal un `replayKey` dins de la clau d'un `LaunchedEffect`, que reinicia els `Animatable` i torna a llançar la seqüència.
- Cada element té el seu `Animatable` (escala, rotació, desplaçament i opacitat via `graphicsLayer`) i un retard propi dins del mateix `LaunchedEffect`, llançat amb `launch { delay(ms); anim.animateTo(…) }`.
- **Rebots:** `spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)`, o `keyframes` quan cal un rebot concret (escala 0 → 1,12 → 0,96 → 1).
- **Moviment reduït:** si `Settings.Global.ANIMATOR_DURATION_SCALE == 0` (o el sistema demana menys animació), es mostra directament l'estat final, sense moviments infinits.
- Les il·lustracions són decoratives: `contentDescription = null` i `clearAndSetSemantics {}`.
- El títol i el cos entren amb fade + desplaçament de 16 dp cap amunt (500 ms, ease-out): el títol a 150 ms i el cos a 300 ms.

### Pas 1 · Recorda cada conversa

Il·lustració sobre un llenç de 350 × 380 dp:

| Element | Mida i posició | Animació | Temps (inici · durada) |
| --- | --- | --- | --- |
| Bafarada (`MaterialShapes.Cookie9Sided`, `primary`) | 160 dp, centrada a (95, 95) | Escala 0 → 1 amb rebot i rotació −40° → 0° | 100 ms · 900 ms |
| Línia 1 de text (`onPrimary`, 10 dp, 100 %) | dins la bafarada, 40 dp de marge | Amplada 0 → 100 % | 800 ms · 500 ms |
| Línia 2 (70 %) | | Amplada 0 → 70 % | 1050 ms · 400 ms |
| 5 avatars (formes i pastels diferents, amb inicials) | vegeu la taula de sota | Surten del centre: desplaçament (dx, dy) → 0, escala 0 → 1,15 → 1, rotació −90° → 0° | 550 ms + i × 140 ms · 900 ms |
| Flotació dels avatars | | `infiniteTransition`: translateY 0 ↔ −7 dp, ease-in-out | des de 1600 ms + i × 200 ms; període 3,0 / 3,6 / 4,2 s |
| Gir lent de la forma | | Rotació infinita de la forma; les inicials giren al revés i queden dretes | període 18 + i × 4 s |

| i | Forma | Pastel | Inicials | x, y | Mida |
| --- | --- | --- | --- | --- | --- |
| 0 | Cookie9Sided | Menta | JP | 18, 40 | 72 |
| 1 | Clover4Leaf | Préssec | LF | 250, 20 | 88 |
| 2 | Sunny | Cel | R | 270, 250 | 64 |
| 3 | Flower | Rosa | MS | 40, 260 | 84 |
| 4 | Cookie9Sided | Llimona | A | 160, 300 | 48 |

`dx, dy` = centre de la bafarada (175, 190) − centre de l'avatar.

### Pas 2 · Tot en una cronologia

- Targeta `surfaceContainerLowest`, radi 32 i 20/16 dp de padding, a 70 dp del capdamunt. Entra amb fade + pujada (500 ms).
- Dins hi ha tres files amb el component de cronologia real (`TimelineRow` amb el marcador de 32 dp):

| Fila (i) | Marcador | Etiqueta | Text |
| --- | --- | --- | --- |
| 0 | Avatar Lavanda «L» (Cookie9Sided) | LUNA · 21:25 · DISCORD | La Nyra deu diners al gremi. |
| 1 | Cercle `primary` amb `person` | JO · 21:40 · DISCORD | En Kael va perdre el germà. |
| 2 | Quadrat de vora `outline` amb `edit_note` | APUNT · AHIR · EN PERSONA | Partida a la jornada de jocs. |

Seqüència per fila, amb `base = 500 ms + i × 750 ms`:

| Element | Animació | Inici · durada |
| --- | --- | --- |
| Marcador | Escala 0 → 1,25 → 1 | base · 450 ms |
| Targeta de l'entrada | Fade + desplaçament de 28 dp des de la dreta | base + 150 ms · 450 ms |
| Línia vertical | `scaleY` 0 → 1, amb origen a dalt | base + 350 ms · 500 ms |
| Text | «Escriptura»: `drawWithContent` + `clipRect` d'amplada 0 → 100 %, lineal | base + 350 ms · 700 ms |

### Pas 3 · Només per a tu

| Element | Animació | Inici · durada |
| --- | --- | --- |
| Avatar de 180 dp (Cookie9Sided, Lavanda) amb `lock` farcit | Pop amb rebot | 100 ms · 800 ms |
| Botó d'empremta de 80 dp (`primary`, vora de 6 dp del color de fons), a baix a la dreta de l'avatar | Pop amb rebot | 500 ms · 600 ms |
| Anell de pols (vora de 3 dp `primary`) | Escala 1 → 1,9 i opacitat 0,55 → 0, **2 cops** | 900 ms · 1000 ms cada cop |
| Línia d'escaneig (3 dp, `onPrimary`, amb resplendor) | Recorre l'empremta de dalt a baix, **2 cops** | 1100 ms · 700 ms cada cop |
| Botó → **verd** | Fons `primary` → `success`, contingut → `onSuccess` | 2400 ms · 350 ms |
| `fingerprint` → `check` | La primera surt (escala 0,6, opacitat 0) i el `check` entra amb rebot | 2400 ms · 200 ms / 2550 ms · 350 ms |
| Cercle de fons de 280 dp (`successContainer`) | Escala 0,2 → 1 i opacitat 0 → 1 | 2500 ms · 700 ms |
| `lock` → `lock_open` + rebot de l'avatar (1 → 1,08 → 0,97 → 1) | Crossfade | 2600 ms · 250 ms / 2800 ms · 400 ms |
| Pastilles «Sense núvol» (`cloud_off`) i «Sense comptes» (`no_accounts`) | Fade + pujada | 3100 ms · 500 ms |
| Botó **Comença** | Pop | 3200 ms · 500 ms |

Al pas 3, el botó **Comença** no s'activa fins que acaba l'animació (o es pot prémer abans i cancel·la la resta).

### Colors d'èxit (nous, al `designsystem`)

És l'únic verd de l'app i només s'usa aquí. Van fixos, fora de l'esquema dinàmic, com l'error:

| Token | Clar | Fosc |
| --- | --- | --- |
| `success` | `#1B9247` | `#5AC576` |
| `onSuccess` | `#FFFFFF` | `#00220A` |
| `successContainer` | `#D1F2D7` | `#133C1F` |

## 4. Textos (`strings.xml`)

| Clau | ca | es | en |
| --- | --- | --- | --- |
| `onb_skip` | Omet | Omitir | Skip |
| `onb_next` | Següent | Siguiente | Next |
| `onb_start` | Comença | Empezar | Get started |
| `onb_1_title` | Recorda cada conversa | Recuerda cada conversación | Remember every conversation |
| `onb_1_body` | Què et va explicar? Què li vas dir tu? Guarda-ho persona a persona i no ho tornis a barrejar mai més. | ¿Qué te contó? ¿Qué le dijiste tú? Guárdalo persona a persona y no lo vuelvas a mezclar nunca más. | What did they tell you? What did you say? Keep it person by person and never mix it up again. |
| `onb_2_title` | Tot en una cronologia | Todo en una cronología | All in one timeline |
| `onb_2_body` | Cada entrada diu qui ho ha dit: tu, l'altra persona o un apunt teu. Per xat, per xarxes o en persona, tot queda en ordre. | Cada entrada dice quién lo dijo: tú, la otra persona o un apunte tuyo. Por chat, redes o en persona, todo queda en orden. | Every entry shows who said it: you, them or a note of yours. By chat, social media or face to face, it all stays in order. |
| `onb_3_title` | Només per a tu | Solo para ti | Just for you |
| `onb_3_body` | Les notes es queden al mòbil. Si vols, protegeix una persona amb l'empremta i ningú més la podrà obrir. | Las notas se quedan en el móvil. Si quieres, protege a una persona con la huella y nadie más podrá abrirla. | Your notes stay on your phone. Protect anyone with your fingerprint so no one else can open them. |
| `onb_no_cloud` | Sense núvol | Sin nube | No cloud |
| `onb_no_accounts` | Sense comptes | Sin cuentas | No accounts |
| `onb_page` | Pas %1$d de %2$d | Paso %1$d de %2$d | Step %1$d of %2$d |

## 5. Accessibilitat

- Il·lustracions decoratives: TalkBack només llegeix el títol i el cos de cada pas.
- Indicador de pàgina: `onb_page` com a `stateDescription`, i cada punt clicable amb «Pas N».
- Mida mínima de 48 dp per a Omet i per als punts (l'àrea clicable, no el dibuix).
- Amb la font gran, el text pot créixer: el contingut ha de poder fer scroll per sobre del peu sense tapar-se.

## Pla en commits

1. `onboarding_done` a `SettingsRepository` i ruta inicial condicional a `MainActivity`.
2. `OnboardingScreen`: pager, textos, punts, botons, Omet/Comença i enrere. Encara amb les il·lustracions estàtiques en l'estat final.
3. Tokens `success*` al `designsystem`.
4. Animació del pas 1.
5. Animació del pas 2 (amb `TimelineRow`).
6. Animació del pas 3.
7. Repetició en tocar, moviment reduït, previews (estat final de cada pas, en clar i en fosc) i tests.

## Proves

- [ ] Primera obertura: surt l'onboarding; després de **Comença** o **Omet**, no torna a sortir encara que es tanqui l'app.
- [ ] Enrere al pas 1 surt de l'app i l'onboarding torna a sortir la propera vegada.
- [ ] Lliscar entre pàgines llança l'animació de la pàgina d'arribada, no la de la pàgina que es deixa.
- [ ] Tocar la il·lustració la repeteix des del principi.
- [ ] Amb les animacions del sistema desactivades, es veu l'estat final i no hi ha moviments infinits.
- [ ] Clar i fosc; verd d'èxit llegible en tots dos.
- [ ] ca, es i en amb la font gran: res es talla ni queda tapat pel peu.
- [ ] Rendiment: les animacions infinites del pas 1 s'aturen en sortir de la pàgina (sense `recomposition` contínua; fer servir `graphicsLayer` amb lambda).
