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
  - Debug: `TimberTracker` (logcat, etiqueta `Tracking`). Release: `FirebaseTracker`, buit fins a la fase 8. Cada *build type* té el seu `trackersModule`.
- Firebase és només una implementació més, i ni tan sols s'**inicialitza** si no hi ha consentiment:
  - Es treu la inicialització automàtica de Firebase (`FirebaseInitProvider` amb `tools:node="remove"` al manifest), i a més `firebase_analytics_collection_enabled` i `firebase_crashlytics_collection_enabled` queden a `false`.
  - `FirebaseApp.initializeApp` es crida només quan el consentiment és «Sí», a l'arrencada o en el moment d'activar-lo.
  - En desactivar-ho, s'apaga la recollida (`setAnalyticsCollectionEnabled(false)`, `setCrashlyticsCollectionEnabled(false)`), es descarten els pendents (`deleteUnsentReports()`, `resetAnalyticsData()`) i l'arrencada següent ja no inicialitza res.
  - Sense consentiment, `EventTracker` no crida cap implementació remota. A debug hi ha un tracker de logcat, que no surt del mòbil.
  - **No** es fa servir el *consent mode* de Google com a Caducity, perquè amb tot denegat continua enviant pings sense galetes.
- **Eina:** Firebase Analytics + Crashlytics (decidit el 03-11-2026). Abans d'afegir-les cal comprovar-ne la versió estable a la documentació oficial.
- **Què s'envia:** noms d'esdeveniment de pantalles i funcions, errors, el model de mòbil, la versió d'Android i la de l'app.
- **No s'envia mai:** text, noms de persones, ids, dades ni canals propis. Obrir una persona protegida compta igual que obrir-ne qualsevol altra.
- **Sense flag** (04-11-2026): sense consentiment no surt res, i fins a la fase 8 el tracker de release és buit.
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

1. Infraestructura agnòstica — validada el 04-11-2026:
   - [x] `Tracker`, `Screen`/`Event`/`Action` abstractes i `EventTracker` que filtra els remots pel consentiment — `06f24c2`
   - [x] `TimberTracker` a debug, `FirebaseTracker` buit a release — `1f8bb5a`
   - [x] Consentiment al DataStore i `ConsentManager` — `88685b3`
   - [x] `TrackScreen` — `c3d7499`
   - [x] Aquest README
Ordre decidit el 04-11-2026: tot el tracking es fa i es valida amb el `TimberTracker`; Firebase va al final.

2. Pantalles. `route.toScreen()` (`ui/tracking/Screens.kt`) i un sol `TrackScreen` a `AppNavigation`, no a cada pantalla.
   - Pantalles: `onboarding`, `people`, `archived`, `person`, `edit_person` (`mode` create/edit), `quick_note` (`with_person`, `source` app/share), `task` (`mode`), `paywall` (`highlight` benefici o `none`), `settings`, `custom_channels`.
   - `PersonLockedRoute` no es registra: diria que l'usuari té persones protegides. Després de desbloquejar compta com a `person`.
   - Els fulls i diàlegs dins d'una pantalla no són pantalles: van com a accions (fase 3).
   - [x] Aquest pla — `57e0926`
   - [x] `Screens.kt` (`object Screens`) i `toScreen()` amb tests de JVM — `d9656c3`
   - [x] `TrackScreen` a `AppNavigation`, validat a l'emulador — `12e0214`
3. Accions de l'usuari (`trackAction`), incloent obrir fulls i diàlegs, revisades una a una perquè no portin cap dada — validada el 04-11-2026.
   - `object Actions` (`ui/tracking/Actions.kt`): els paràmetres són enums i booleans, mai text. Un canal propi només diu `custom_channel=true`, mai quin.
   - No es registra el que ja és una pantalla (obrir una persona, el paywall, l'editor).
   - Es criden des del ViewModel si n'hi ha (tests de JVM amb un tracker de prova); obrir fulls i diàlegs, des de la pantalla amb estat amb `rememberActionTracker()`.
   - Catàleg (decidit el 04-11-2026):
     - Persones: `filter_toggled` (`channel`, `selected`).
     - Detall: `kind_filter_toggled` (`kind`), `entry_sheet_opened` (`mode`), `entry_saved` (`mode` new/new_as/edit/quick, `kind`, `channel`, `custom_channel`), `entry_menu_opened`, `entry_copied`, `entry_moved`, `entry_deleted`, `entry_restored`, `day_deleted`, `person_exported`, `person_archived` (`archived`), `task_done`, `task_reopened` (`source` undo/list).
     - Nota ràpida: `entry_saved` (`mode=quick`), `person_picker_opened`.
     - Editor: `person_saved` (`mode`), `person_protection_changed` (`enabled`, sense dir de qui: en una persona existent, quan l'interruptor es desa després de l'autenticació; en una de nova, en desar-la protegida), `person_deleted`, `avatar_sheet_opened`, `avatar_applied` (sense forma ni color).
     - Tasca: `task_saved` (`mode`, `has_due`), `task_deleted`.
     - Canals propis: `custom_channel_saved` (`mode`, `channel`), `custom_channel_deleted`.
     - Ajustos: `setting_changed` (`setting` theme/dynamic_color/person_color/haptics/app_lock/relock_after, `value`), `link_opened` (`target` language/security/privacy/feedback).
     - Paywall: `purchase_started`, `restore_tapped` (el resultat, a la fase 4).
     - Onboarding: `onboarding_finished` (`skipped`).
   - [x] `Actions.kt`, `rememberActionTracker()` i aquest catàleg — `812c33a`
   - [x] Llista, detall i nota ràpida — `709343c`
   - [x] Editor, tasca i canals propis — `b9d545f`
   - [x] Ajustos, paywall i onboarding — `c876e99`
4. Esdeveniments (`trackEvent`): coses que passen sense que l'usuari les demani — validada el 04-11-2026.
   - `object Events` (`data/tracking/Events.kt`, a `data/` perquè els emeten capes de dades). Valors tancats, com les accions.
   - Catàleg (decidit el 04-11-2026):
     - Paywall: `purchase_finished` (`result` purchased/pending/cancelled/offline/failed, `late`), `purchase_timed_out`, `restore_finished` (`result` restored/nothing/pending/offline/failed).
     - Pro: `pro_changed` (`active`), només quan arriba pel listener de RevenueCat, no per una compra o restauració del paywall.
     - Bloqueig de l'app: `app_locked` (`reason` cold_start/relock), `app_lock_disabled` (`reason` device_unprotected).
     - Importar: `text_import_empty` (`source` share/process_text).
   - No es registra: el desbloqueig d'una persona protegida, l'èxit o error del desbloqueig de l'app, ni les migracions (si fallen, fase 5).
   - `pro_changed` no compta el que arriba pel listener mentre hi ha una compra o restauració en curs: RevenueCat sovint envia el `CustomerInfo` abans que `purchase` torni.
   - Sense bloqueig de pantalla al mòbil, l'arrencada registra `app_locked` (cold_start) i just després `app_lock_disabled`: el bloqueig surt un instant.
   - [x] `Events.kt` i aquest catàleg — `af156fe`
   - [x] Paywall — `91bdbce`
   - [x] Pro pel listener — `b813b5d`, `1a5ddac`
   - [x] Bloqueig de l'app — `914d7e1`
   - [x] Importació buida — `19dc853`
   - [x] Validació a l'emulador amb logcat (compra, restauració, bloquejos, importacions)
5. Errors (`trackError`) — validada el 04-11-2026.
   - Criteri (decidit el 04-11-2026): la fase no canvia cap comportament, llevat del DataStore corrupte (més avall). Només s'informa dels errors que l'app ja s'empassa; el que fa petar l'app (migracions, exportar) ho recollirà Crashlytics a la fase 8.
   - `object ErrorSites` (`data/tracking/ErrorSites.kt`): noms tancats per al `where`, mai dades.
   - Catàleg:
     - Pro: `pro_price`, `pro_customer_info`, `pro_purchase`, `pro_restore`. Només quan el resultat és `Failed`: sense xarxa, cancel·lat o pendent no són errors (ja surten com a esdeveniments).
     - Obrir fora de l'app: `open_link` (Ajustos i política del paywall), `open_feedback`, `open_security_setup` (fallback a la configuració de seguretat).
   - DataStore corrupte (decidit el 04-11-2026): es buida i l'app arrenca amb els valors per defecte en comptes de petar cada cop. A `settings` vol dir onboarding i full de consentiment de nou i el bloqueig de l'app apagat (les persones protegides continuen demanant l'empremta). S'informa `datastore_settings`, `datastore_pro` i `datastore_flags` a través de `StartupErrors`, una cua que es buida cap a l'`EventTracker` en arrencar, perquè el handler salta mentre Koin encara crea el `ConsentManager`. A `settings` el consentiment també s'ha esborrat, així que aquest error no surt mai del mòbil.
   - `open_security_setup` només s'informa des d'Ajustos: des d'una persona protegida, la traça diria que n'hi ha.
   - [x] `ErrorSites.kt` i aquest catàleg — `ebe2e94`
   - [x] Pro (`RevenueCatGateway`) — `568912a`
   - [x] Obrir fora de l'app — `450eb57`
   - [x] DataStore corrupte — `3565ae7`
6. Configuració de l'`EventTracker`: propietats comunes a tots els esdeveniments, començant per `isPro`.
7. Full d'una sola vegada (10a, 10b) després de l'onboarding, amb un FireAndForget, i Ajustos › Privacitat (1c), amb els textos en ca/en/es. Captures i emulador.
8. Implementació Firebase amb la inicialització manual, i l'activació i desactivació en calent. Cal el `google-services.json`. Validació a l'emulador amb logcat (`adb shell setprop log.tag.FA VERBOSE`): sense consentiment, cap tràfic de Firebase.
