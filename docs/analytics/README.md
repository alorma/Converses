# Estadístiques d'ús (opt-in)

Funció per a tota l'app, **no és Pro**. Disseny: lliurament de Claude Design del 03-11-2026, `~/Downloads/converses-analitica-avatar.zip`, `INSTRUCCIONS.md` §1 i `pantalles.html` (taulers 10a, 10b, 10c i 1c; taula de textos en ca/en/es a §1 «Textos»). Referència d'implementació: Caducity (`feature/tracking`, `feature/consent`).

Com es treballa cada fase: `.claude/rules/phased-work.md`.

## Decisions (03-11-2026)

- **Consentiment apagat per defecte.** No s'envia res fins que l'usuari tria «Sí».
- **Sistema agnòstic** (`data/tracking`). L'app només coneix `EventTracker`, un `Tracker` compost amb quatre mètodes:
  - `trackScreen(Screen)`: pantalles (`TrackScreen(screen)` a la pantalla amb estat, no al `...Content`).
  - `trackEvent(Event)`: coses que passen sense que l'usuari les demani.
  - `trackAction(Action)`: accions de l'usuari.
  - `trackError(Throwable, where)`: errors que no peten l'app; els trackers remots no envien el `message` de l'excepció.
  - `Screen`, `Event` i `Action` són abstractes i porten `eventName`, `eventVersion` i `eventProperties` (`Map<String, String>`). Cada esdeveniment concret és una subclasse que fa el seu mapping a partir dels seus paràmetres.
  - Cada `Tracker` diu si és remot (`isRemote`). `EventTracker` només crida els remots si `ConsentManager.hasConsent()`; els locals reben sempre.
  - Debug: `TimberTracker` (logcat, etiqueta `Tracking`). Release: `FirebaseTracker`, buit fins a la fase 2. Cada *build type* té el seu `trackersModule`.
- Firebase és només una implementació més, i ni tan sols s'**inicialitza** si no hi ha consentiment:
  - Es treu la inicialització automàtica de Firebase (`FirebaseInitProvider` amb `tools:node="remove"` al manifest), i a més `firebase_analytics_collection_enabled` i `firebase_crashlytics_collection_enabled` queden a `false`.
  - `FirebaseApp.initializeApp` es crida només quan el consentiment és «Sí», a l'arrencada o en el moment d'activar-lo.
  - En desactivar-ho, s'apaga la recollida (`setAnalyticsCollectionEnabled(false)`, `setCrashlyticsCollectionEnabled(false)`), es descarten els pendents (`deleteUnsentReports()`, `resetAnalyticsData()`) i l'arrencada següent ja no inicialitza res.
  - Sense consentiment, `EventTracker` no crida cap implementació remota. A debug hi ha un tracker de logcat, que no surt del mòbil.
  - **No** es fa servir el *consent mode* de Google com a Caducity, perquè amb tot denegat continua enviant pings sense galetes.
- **Eina:** Firebase Analytics + Crashlytics (decidit el 03-11-2026). Abans d'afegir-les cal comprovar-ne la versió estable a la documentació oficial.
- **Què s'envia:** noms d'esdeveniment de pantalles i funcions, errors, el model de mòbil, la versió d'Android i la de l'app.
- **No s'envia mai:** text, noms de persones, ids, dades ni canals propis. Obrir una persona protegida compta igual que obrir-ne qualsevol altra.
- **Sense flag** (04-11-2026): sense consentiment no surt res, i fins a la fase 2 el tracker de release és buit.
- El consentiment es desa al DataStore (`analyticsConsent`, `null` = no ha triat).

## Disseny

- **Full d'una sola vegada (10a, 10b):**
  - Surt just després de l'onboarding (decidit el 04-11-2026), un sol cop, amb un FireAndForget. Mai sobre el paywall o el desbloqueig. Sense comptador d'obertures.
  - No es pot descartar: si l'app es tanca sense triar, torna a sortir.
  - Capçalera: galeta de 56 dp (`primaryContainer`) amb `insights`, «Ajuda a millorar Converses» i el text.
  - Dues files agrupades: `check` amb «S'envia…» i `block` amb «No s'envia mai…».
  - `radiogroup` de dues opcions de 60 dp, sense cap opció triada al principi.
  - «D'acord» (56 dp) està desactivat fins que hi ha una tria; en tocar-lo, desa i tanca.
  - Al lliurament també surten l'avís de canals i el full de valoració, que són de Caducity i a Converses no existeixen.
- **Ajustos › Privacitat (1c):**
  - Secció nova entre Seguretat i Canals.
  - Fila de 72 dp amb `insights`, «Estadístiques d'ús» i un interruptor. Subtítol: «Activades · anònimes» o «Desactivades · no s'envia res».
  - Nota: «Mai s'envien persones, notes ni res del que escrius. Detalls a la Política de privacitat.».
  - S'aplica a l'instant.

## Pendent abans de publicar

- **Projecte de Firebase:** apps `com.alorma.contactnotes` i `com.alorma.contactnotes.dev`, el `google-services.json` (fora del repo, com a Caducity) i el secret `GOOGLE_SERVICES_JSON_BASE64` als workflows que compilen.
- La política de privacitat (`converses.app/privacy`, repo ConversesLanding) i el formulari de seguretat de dades de Play.

## Fases

1. Infraestructura agnòstica (pendent de la validació de Bernat):
   - [x] `Tracker`, `Screen`/`Event`/`Action` abstractes i `EventTracker` que filtra els remots pel consentiment — `06f24c2`
   - [x] `TimberTracker` a debug, `FirebaseTracker` buit a release — `1f8bb5a`
   - [x] Consentiment al DataStore i `ConsentManager` — `88685b3`
   - [x] `TrackScreen` — `c3d7499`
   - [x] Aquest README
2. Implementació Firebase amb la inicialització manual, i l'activació i desactivació en calent. Cal el `google-services.json`. Validació a l'emulador amb logcat (`adb shell setprop log.tag.FA VERBOSE`): sense consentiment, cap tràfic de Firebase.
3. Full d'una sola vegada (10a, 10b) després de l'onboarding, amb un FireAndForget, i Ajustos › Privacitat (1c), amb els textos en ca/en/es. Captures i emulador.
4. Esdeveniments de pantalles i accions principals, revisats un a un perquè no portin cap dada. Actualitzar la documentació.
