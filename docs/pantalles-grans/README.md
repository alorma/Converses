# Pantalles grans: tauleta i plegable

Com s'adapta l'app a les pantalles grans i per què. El pas a pas de com es va fer és a l'historial de git
(novembre de 2026).

## Referència de disseny

- `disseny/pantalles.html`: les pantalles (12a–12e). Cal obrir-la al navegador amb connexió (fonts de Google). La
  línia discontínua «frontissa» és una anotació, no es dibuixa.
- `disseny/INSTRUCCIONS.md`: les mides, els textos i les regles, tal com van arribar de Claude Design (10-11-2026).

El que mana és el resultat (com es veu i com es comporta), no la part tècnica de les instruccions: Claude Design no
coneix el codi. On aquest document i les instruccions es contradiuen en el «com», val aquest document.

## Per què

- Que l'app compleixi els criteris de pantalles grans de Google Play i es vegi pensada per a tauleta.
- Deixar la base per quan hi hagi compte d'usuari i còpia al núvol (diversos dispositius).

## Què fa

La disposició depèn de l'amplada de la finestra, mai del model de dispositiu ni de l'orientació. Una tauleta en
multifinestra o un plegable tancat poden ser «mòbil».

| Finestra | Navegació | Llistat i detall | Formularis |
| --- | --- | --- | --- |
| Menys de 600 dp (mòbil, plegable tancat) | Barra flotant | Un panell | Pantalla completa |
| 600–839 dp (tauleta vertical) | Rail | Llistat de 300 dp i detall | Diàleg de 560 dp |
| 840 dp o més (tauleta horitzontal) | Rail | Llistat de 380 dp i detall | Diàleg de 560 dp |
| Amb frontissa vertical (plegable obert) | Barra flotant dins del llistat | Partits per la frontissa | Diàleg de 560 dp |

- **El mòbil no canvia.** Per sota de 600 dp tot és com abans; les captures de referència del mòbil no s'han de
  regenerar per culpa de les pantalles grans.
- **Rail:** botó de nota ràpida a dalt (en lloc del FAB) i Persones, Arxivades i Ajustos. Es veu a les pestanyes, amb
  una persona oberta i a Ajustos (on marca Ajustos), i es queda sota els diàlegs. No surt a l'onboarding ni en un
  import des d'una altra app.
- **Llistat i detall:** la persona oberta queda marcada al llistat (`selected` a la semàntica) i el detall va dins
  d'un panell arrodonit, sense fletxa enrere. Triar una altra persona la substitueix; enrere torna a «Tria una
  persona». En arrencar no se n'obre cap. Els xips de filtre passen a dues línies en lloc de fer scroll.
- **Detall a dues columnes:** quan el panell de detall fa 600 dp o més (en una tauleta de 1280 dp sí; amb una
  finestra de 840 dp encara no). La capçalera és fixa amb l'avatar de 96 dp, el recompte per «Qui», editar i el botó
  «Nota ▾», que deixa de flotar. «Dades» i «Pendents» van en 280 dp i la cronologia a la resta; a la columna estreta,
  «Nova tasca» es queda en la icona.
- **Formularis en diàleg:** nota ràpida, nova/editar persona, tasca i Pro. Esc i enrere els tanquen; tocar fora no,
  perquè es perdria el text. La nota ràpida oberta amb una persona al detall ja ve amb ella triada. Un import des
  d'una altra app (la pila és només la nota ràpida) es queda a pantalla completa.
- **Plegable obert:** sense rail. El llistat fa fins a la frontissa i el detall, la resta, amb 24 dp sense res
  interactiu a cada costat. La barra flotant i el botó de nota ràpida van a baix del llistat. Mig obert es comporta
  igual. Ajustos hi és a pantalla completa amb fletxa enrere, com al mòbil.
- **Canvis de mida:** girar, redimensionar, plegar o desplegar conserva la persona oberta, l'scroll i el text que
  s'escrivia. Amb una persona oberta, en passar a un panell es veu el detall amb fletxa enrere.
- **Columnes de lectura:** Ajustos, Canals propis, Pro i l'onboarding queden en una columna centrada de 640 dp. Els
  fulls inferiors ja es limiten a 640 dp.
- **Teclat:** fletxes amunt i avall pel llistat, Intro obre la persona, Ctrl+N obre la nota ràpida allà on se'n veu
  el botó, Esc tanca els diàlegs.
- **Persones protegides:** amb dos panells, la pantalla de bloqueig ocupa el panell de detall i no es carrega res
  fins que la biometria va bé; si es cancel·la, torna a «Tria una persona».

## Com està fet

- **Una sola navegació.** Tot continua sobre Navigation 3 i `TopLevelBackStack`; les rutes i la pila són les mateixes
  a qualsevol amplada, i per això l'estat sobreviu als canvis de mida. `NavDisplay` rep dues estratègies d'escena:
  `ListDetailSceneStrategy` (de `adaptive-navigation3`) per al llistat i el detall, i `DialogSceneStrategy` (de
  Navigation 3) per als formularis. No es fan servir `NavigableListDetailPaneScaffold` ni `NavigationSuiteScaffold`,
  que porten navegador propi.
- **Qui decideix els dos panells** és la directiva (`calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth`).
  `twoPane` en surt i arriba a les pantalles com a `inPane`, perquè pantalles i escena no es puguin contradir.
- **`BackNavigationBehavior.PopLatest`** és necessari: amb el valor per defecte, enrere amb dos panells tancava l'app
  amb la persona oberta.
- **`TopLevelBackStack`:** `openPerson` substitueix la persona oberta en lloc d'apilar-la, `openPersonId` és la fila
  marcada i `navigateSingle` obre Ajustos des del rail sense apilar-ne una segona còpia.
- **`AppShell`** continua sent l'únic lloc de la navegació principal; `shellNavigationFor(pila, amplada, book)`
  decideix si dibuixa la barra flotant, el rail o res. El contingut es crida sempre des del mateix lloc, igual que
  `DetailPanel` i `FormDialogFrame`, perquè les pantalles no perdin l'estat en canviar de disposició.
- **El rail** és `ConversesNavigationRail` (`:designsystem`), fet amb `WideNavigationRail` de Material 3 Expressive,
  que ja té les mides del disseny (96 dp, indicador de 56 × 32).
- **Plegable («llibre»):** es detecta per la frontissa vertical de `windowPosture.hingeList`, encara que sigui plana i
  d'amplada 0 (Pixel 9 Pro Fold). Per amplada (852 dp) seria una tauleta horitzontal.
- **Diàlegs i `FLAG_SECURE`:** el diàleg és una finestra a part i el flag de l'activitat no el cobreix si es demana
  amb el diàleg ja obert (triar una persona protegida a la nota ràpida). `FormDialogFrame` el posa a la finestra del
  diàleg seguint `SecureWindow.isRequested`. No serveix canviar `DialogProperties`: `NavDisplay` es queda amb la
  primera escena superposada d'una ruta i n'ignora els canvis.
- **Ctrl+N** s'escolta amb `ViewCompat.addOnUnhandledKeyEventListener`: només arriba si cap camp s'ha quedat la tecla,
  i amb un diàleg obert no arriba perquè és una altra finestra.
- **Fletxes al llistat:** les files ja són enfocables; la capçalera porta `focusProperties { down = ... }` perquè la
  fletxa avall entri a la primera fila en lloc de saltar als botons flotants.
- **Estadístiques d'ús:** cap esdeveniment nou. El seguiment de pantalles segueix sortint d'`AppNavigation` amb la
  ruta de dalt de la pila; «Tria una persona» no és cap pantalla. No surt cap dada nova del telèfon.

## Com es prova

- Emuladors: `37_Pixel_Tablet` i `37_Pixel_Fold`. El plegable té dues pantalles: `screencap` necessita `-d <id>`
  (`adb shell dumpsys SurfaceFlinger --display-id`). Plegar i desplegar: `adb emu fold` / `adb emu unfold`; mig
  obert: `adb emu sensor set hinge-angle0 100`.
- Estrènyer la finestra d'una tauleta: `adb shell wm size 1100x1600` i `adb shell wm size reset`.
- Una persona protegida es veu en negre a les captures (pantalla segura): es comprova amb `uiautomator dump`, i el
  flag de les finestres amb `adb shell dumpsys window windows` (activitat i diàleg han de dir `SECURE`).
- Teclat: `adb shell input keycombination 113 42` (Ctrl+N), `keyevent 20`/`19` (fletxes), `66` (Intro), `111` (Esc).
- Captures de tauleta i plegable: a 160 dpi (`TabletPortraitPreviews`, `TabletLandscapePreviews`,
  `DetailPanePreviews`, `FoldablePreviews`). Amb la densitat per defecte la validació es queda sense memòria. Els
  diàlegs no tenen captura: són finestres del sistema.

## Diferències amb el disseny

Decisions conscients; cap és un error pendent.

- **Mode taula (12e):** no es fa. És a `docs/backlog.md`.
- **Icones del rail:** les del mòbil (`Groups`, `Archive`, `EditNote`), no les del disseny (`group`, `inventory_2`,
  `edit_square`), perquè mòbil i tauleta siguin iguals.
- **Capçalera del llistat:** manté el botó d'afegir persona; el disseny hi posa un menú de tres punts sense dir què
  hi va.
- **«Tria una persona»:** la galeta és la de la resta d'estats buits (de color, 120 dp); el disseny la vol neutra i
  de 96 dp.
- **Fila seleccionada:** `primaryContainer` del tema, que en el tema grafit és fosc.
- **Botó «Nota ▾» a la capçalera:** manté l'alçada del mòbil; el disseny el vol de 48 dp.
- **Diàlegs:** alts (fins a 720 dp) encara que el formulari sigui curt, perquè les pantalles són `Scaffold`; en un
  plegable surten centrats i travessen la frontissa. La nota ràpida diu «AMB QUI», sense «· LA PERSONA OBERTA».
- **Onboarding:** el disseny no en porta. La columna centrada és la solució mínima; un onboarding pensat per a
  tauleta demanaria un disseny propi.
- **Valoració dins l'app:** les instruccions en parlen, però l'app no en té.
- **Etiqueta BETA:** treta a tot arreu, també al mòbil; és l'única excepció a «el mòbil no canvia».

## No comprovat

- Hover amb ratolí: és el que porten per defecte els components de Material; no s'ha vist amb un ratolí.
- El focus de teclat es marca amb el ressalt per defecte, que és subtil.
- Finestres lliures (ChromeOS, escriptori de tauleta): només s'ha provat canviant la mida de la finestra.
- Una persona protegida i Ajustos al plegable; el diàleg de tasca; compartir text des d'una altra app en tauleta.
