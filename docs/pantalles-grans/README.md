# Pantalles grans: tauleta i plegable

Document de treball mentre la feina està en curs. Cada sessió fa un pas, el marca com a fet i apunta aquí el que la
següent ha de saber. Quan s'acabi l'últim pas, es reescriu com a resum del que existeix i per què, i se n'esborren els
passos (la història queda a git).

## Referència de disseny

- `disseny/pantalles.html`: les pantalles (12a–12e). Cal obrir-la al navegador amb connexió (fonts de Google).
- `disseny/INSTRUCCIONS.md`: les mides, els textos i les regles, tal com van arribar de Claude Design (10-11-2026).

**El que mana és el resultat** (com es veu i com es comporta), no la part tècnica de les instruccions: Claude Design
no coneix el codi. On aquest document i les instruccions es contradiuen en el «com», val aquest document.

## Per què

- Que l'app compleixi els criteris de pantalles grans de Google Play i es vegi pensada per a tauleta.
- Aprendre a fer disseny adaptatiu amb Compose: passos petits, cadascun provat a l'emulador.
- Deixar la base per quan hi hagi compte d'usuari i còpia al núvol (diversos dispositius). Ara no se'n fa res.

## Què es fa i què no

**Sí**

- A partir de 600 dp: rail, llistat i detall de costat, detall a dues columnes a partir de 840 dp, formularis en
  diàleg, plegable alineat amb la frontissa, teclat i ratolí.
- Multifinestra i finestres lliures (ChromeOS, escriptori de tauleta) funcionen.

**No**

- **El mòbil no canvia.** Per sota de 600 dp tot queda idèntic; les captures de referència actuals no s'han de
  regenerar per culpa d'aquesta feina. Si una canvia, és un error.
- **Mode taula (12e):** fora. Un plegable mig obert es comporta com obert del tot. Apuntat a `docs/backlog.md`.
- **Valoració dins l'app** (secció 7 de les instruccions): l'app no en té, no aplica.
- Compte d'usuari, núvol, base de dades: res. No hi ha cap canvi de Room ni cap dada nova que surti del telèfon.

**Faria que fos un fracàs, encara que funcioni**

- Que una persona protegida ensenyi cap dada al panell de detall abans de validar la biometria.
- Que en girar, redimensionar, plegar o desplegar es perdi la persona oberta, l'scroll o el text que s'escrivia.
- Tenir dos sistemes de navegació convivint.

## Decisions tècniques

Preses per Claude, no per en Bernat, tret que digui el contrari.

- **Mida de la finestra:** `currentWindowAdaptiveInfo()` i les classes d'amplada (compacta, mitjana, expandida). Mai
  per model de dispositiu ni per orientació.
- **Navegació:** tot continua sobre Navigation 3 i `TopLevelBackStack`. El llistat i detall es fa amb les «scenes»
  adaptatives de Navigation 3 (`ListDetailSceneStrategy`, artefacte `adaptive-navigation3`) i els diàlegs amb
  `DialogSceneStrategy`, no amb `NavigableListDetailPaneScaffold` ni `NavigationSuiteScaffold`, que porten navegador
  propi. Les rutes no canvien: `PeopleRoute`/`ArchivedRoute` són el panell de llistat; `PersonRoute` i
  `PersonLockedRoute`, el de detall. Abans d'afegir res, comprovar les versions estables i els noms a la documentació
  oficial; si l'artefacte no fos estable o no servís, parar i dir-ho.
- **Rail:** component propi a `:designsystem` (`NavigationRail` de Material 3 amb les mides del disseny), dins
  d'`AppShell`, que tria entre barra flotant i rail. `AppShell` continua sent l'únic lloc de la navegació principal.
- **Rail i Ajustos:** en tauleta el rail es veu a Persones, Arxivades i Ajustos (i Canals propis), amb Ajustos marcat
  com a actiu. Ajustos continua sent una ruta apilada, no una pestanya.
- **Plegable obert:** la barra flotant i el FAB es dibuixen dins del panell del llistat. Cal actualitzar la regla del
  `CLAUDE.md` que diu que la barra només viu al FAB slot d'`AppShell`.
- **Colors:** el panell de detall continua amb `PersonTheme` (color de l'avatar); el rail i el llistat, amb el tema de
  l'app. Els tokens `pri`, `card-hi`… del disseny es tradueixen als rols de Material del tema, no a colors fixos.
- **Persona protegida amb dos panells:** `PersonLockedRoute` ocupa el panell de detall; si es cancel·la, es treu de la
  pila i queda «Tria una persona». `FLAG_SECURE` es manté mentre hi hagi dades d'una persona protegida a la pantalla.
- **Estadístiques d'ús:** cap esdeveniment nou. El seguiment de pantalles segueix sortint d'`AppNavigation` amb la
  ruta de dalt de la pila; l'estat buit «Tria una persona» no és cap pantalla. Comprovar que amb dos panells no
  s'envia res duplicat ni es registra `PersonLockedRoute`.
- **Proves:** la lògica nova (què es mostra a cada amplada, pila amb dos panells) va en funcions pures amb tests JVM.
  Cada pantalla principal té previews i captures noves a mida de tauleta i plegable; les de mòbil no es toquen.

## Passos

Un pas per sessió, en ordre. Cada pas: `./gradlew ktlintCheck`, `./gradlew testDebugUnitTest`,
`./gradlew validateDebugScreenshotTest` (el mòbil no ha de canviar) i prova a l'emulador (Pixel Tablet vertical i
horitzontal; Pixel Fold quan toqui; el redimensionable per a les transicions d'amplada). Commit només amb el vistiplau
d'en Bernat.

- [x] **1. Base i rail.** Dependències adaptatives al catàleg de versions. Classe d'amplada disponible per a les
  pantalles. Rail de 96 dp (botó «Nota ràpida» a dalt, Persones, Arxivades, Ajustos) a partir de 600 dp en lloc de la
  barra flotant. Ajustos, Canals propis i Pro en columna centrada de 640 dp com a màxim. Fulls inferiors amb amplada
  màxima de 640 dp. Revisar el manifest (`resizeableActivity`, cap orientació bloquejada).
- [ ] **2. Llistat i detall.** Dos panells: llistat de 300 dp (mitjana) o 380 dp (expandida) i detall en un panell amb
  fons i radi 28. Capçalera del llistat i xips en `FlowRow`. Persona oberta marcada (`selected` a la semàntica).
  Estat buit «Tria una persona» (textos nous en en/ca/es, secció 8). Detall sense fletxa enrere. Triar una altra
  persona substitueix el detall, no l'apila. Persones protegides. Passar de dos panells a un (i al revés) conserva la
  selecció: amb persona oberta es veu el detall amb fletxa enrere; sense, el llistat.
- [ ] **3. Detall a dues columnes** (expandida, 12a). Capçalera amb avatar de 96 dp, `edit` i el split button
  «Nota ▾» a la dreta (ja no flota). Dades i Pendents en 280 dp a l'esquerra, Cronologia a la dreta. En amplada
  mitjana (12c): una columna, avatar de 80 dp i el split button flotant dins del panell.
- [ ] **4. Diàlegs.** A partir de 600 dp, Nova/Editar persona, Nota ràpida (12b), Nova tasca i Pro són diàlegs de
  560 dp (radi 28, scrim, Esc tanca). La Nota ràpida ve amb la persona oberta ja triada. El text escrit sobreviu a
  girar i redimensionar.
- [ ] **5. Plegable** (12d). Amb un `FoldingFeature` vertical que separa, la divisió s'alinea amb la frontissa: llistat
  a l'esquerra, detall a la dreta, sense rail, barra flotant i FAB dins del llistat, res interactiu a menys de 24 dp
  de la frontissa. Plegar i desplegar conserva persona, scroll i text. Sense `FoldingFeature`, disposició de tauleta
  vertical.
- [ ] **6. Teclat i ratolí.** Fletxes amunt i avall pel llistat, Intro obre, Ctrl+N nota ràpida, Esc tanca diàlegs,
  hover a tot el que es pot tocar.
- [ ] **7. Tancament.** Revisió sencera a tauleta, plegable i finestra lliure. Actualitzar `CLAUDE.md` (arquitectura
  i regles de l'`AppShell`). Proposar la línia de les notes de versió. Valorar regenerar el Baseline Profile (la
  navegació haurà canviat). Condensar aquest document.

## Notes entre sessions

**Després del pas 1**

- Versions comprovades a la documentació oficial (10-11-2026): `androidx.compose.material3.adaptive` 1.3.0 estable, amb
  `adaptive-navigation3` inclòs. De moment només s'ha afegit `adaptive` (per a `currentWindowAdaptiveInfo()`); la resta
  s'afegeix al pas que la faci servir. `NavDisplay` rep les escenes amb `sceneStrategies` (una llista).
- `ui/navigation/ScreenWidth.kt`: `ScreenWidth`, `currentScreenWidth()` i `shellNavigationFor(ruta, amplada)`, que
  decideix si `AppShell` dibuixa la barra flotant, el rail o res. Ara el rail s'amaga a les pantalles modals (nota
  ràpida, formularis, Pro) perquè encara són de pantalla completa; quan passin a diàleg (pas 4) s'ha de quedar a sota.
- El rail és `ConversesNavigationRail` (`:designsystem`), fet amb `WideNavigationRail` de Material 3 Expressive, que ja
  té les mides del disseny (96 dp, indicador de 56 × 32). Fa servir les icones del mòbil (`Groups`, `Archive`,
  `EditNote`) i no les del disseny (`group`, `inventory_2`, `edit_square`), perquè mòbil i tauleta siguin iguals.
- `Modifier.readableWidth()` (`:designsystem`) centra una columna de 640 dp com a màxim; al mòbil no fa res.
- Fulls inferiors: `ModalBottomSheet` ja limita l'amplada a 640 dp per defecte; no ha calgut tocar res.
- Manifest: no ha calgut tocar res (cap orientació bloquejada; `resizeableActivity` ja és `true` per defecte).
- Captures de tauleta: anotacions `TabletPortraitPreviews` i `TabletLandscapePreviews` a 160 dpi. Amb la densitat per
  defecte fan 2100 × 3360 px i la validació es queda sense memòria.
- `TopLevelBackStack.navigateSingle` obre Ajustos sense apilar-ne una segona còpia (el rail es veu des d'Ajustos).
- **Pendent de decidir amb en Bernat:** l'etiqueta BETA del llistat. El disseny diu que no surt enlloc, però treure-la
  canvia el mòbil; no s'ha tocat.
