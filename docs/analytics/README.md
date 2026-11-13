# Estadístiques d'ús (opt-in)

Per a tota l'app, **no és Pro**. Firebase Analytics i Crashlytics, darrere d'un sistema de tracking propi i d'un consentiment que per defecte és «No». Política: `converses.app/privacy` (repo ConversesLanding).

## Privacitat

- **No s'envia res fins al «Sí».** Sense triar o amb «No», Firebase **ni s'inicialitza**: el `FirebaseInitProvider` és fora del manifest i la recollida d'Analytics i Crashlytics està apagada per defecte. No es fa servir el *consent mode* de Google, perquè amb tot denegat continua enviant pings.
- **S'envia:** noms de pantalles, accions i esdeveniments amb valors tancats; errors i crashes **sense el `message`** de l'excepció; si l'usuari és Pro (`is_pro`); el model, la versió d'Android i la de l'app; l'identificador d'instal·lació i l'IP (Analytics en dedueix el país o la regió).
- **No s'envia mai:** text, noms de persones, ids, canals propis ni cap dada. Obrir una persona protegida compta igual que qualsevol altra, i `PersonLockedRoute` no es registra.
- **Sense identificador publicitari:** el manifest treu `AD_ID`, `ACCESS_ADSERVICES_*` i l'*install referrer*, i apaga la personalització d'anuncis i les pantalles automàtiques.
- **Retenció a Firebase:** 2 mesos (dades d'esdeveniment i d'usuari); Crashlytics, 90 dies.
- En dir «No» després d'un «Sí», s'apaga la recollida, es descarten els pendents (`deleteUnsentReports()`, `resetAnalyticsData()`) i l'arrencada següent ja no inicialitza res.

## Consentiment

- Es desa al DataStore (`analyticsConsent`; `null` = no ha triat). `ConsentManager` l'exposa.
- **Full d'una sola vegada:** surt a People mentre el consentiment és `null` i l'onboarding està fet (també als usuaris que actualitzen). Mai amb un intent de compartir, sobre el paywall ni sobre el bloqueig de l'app. Tancar-lo sense «D'acord» desa «No»: el consentiment només es dona confirmant-lo.
- **Ajustos › Privacitat:** interruptor «Estadístiques d'ús», s'aplica a l'instant.
- **Debug:** menú de debug › Usage statistics › «Ask for consent again» torna el consentiment a `null`.

## Arquitectura

- `data/tracking`: l'app només coneix `EventTracker`, un `Tracker` compost amb `trackScreen`, `trackEvent`, `trackAction` i `trackError(Throwable, where)`. `Screen`, `Event` i `Action` porten `eventName`, `eventVersion` i `eventProperties`.
- Cada `Tracker` diu si és remot. Els locals (`TimberTracker`, només a debug, etiqueta `Tracking`) reben sempre. Els remots (`FirebaseTracker`, a debug i release) només amb el «Sí».
- **Cua de pendents:** mentre el consentiment és `null`, el que aniria als remots es guarda en memòria (màxim 100, sense disc). Amb «Sí» s'envia en ordre, amb «No» es buida. Així l'onboarding d'un usuari nou compta si després diu «Sí».
- **Propietats comunes:** `EventTracker.setCommonProperty` les afegeix a tot (guanya la de l'esdeveniment si coincideixen). Ara només `is_pro`, que manté `ProTrackingProperty`; als errors van com a *custom keys* de Crashlytics.
- **Firebase:** `FirebaseController` escolta el consentiment i, a través de `FirebaseGateway`, inicialitza o apaga Firebase. `FirebaseTracker` també l'activa abans d'enviar, perquè la cua es pot buidar abans que el controller reaccioni. `SanitizingExceptionHandler` es posa al damunt del handler de Crashlytics perquè els crashes fatals també surtin sense `message`.
- **Errors de l'arrencada:** `StartupErrors` és una cua que es buida cap a l'`EventTracker` quan Koin ja l'ha creat (el DataStore corrupte salta abans).

## Catàlegs

Valors tancats, mai text. Per afegir-ne un, segueix el patró del fitxer.

- **Pantalles** (`ui/tracking/Screens.kt`, un sol `TrackScreen` a `AppNavigation`): `onboarding`, `people`, `archived`, `person`, `edit_person` (`mode`), `quick_note` (`with_person`, `source`), `task` (`mode`), `paywall` (`highlight`: `none`, `avatar`… o `sync`, quan ve de Sincronització), `settings`, `custom_channels`, `sync`, `sync_words` (`mode`: `activation` o `review`), `sync_enter_words`. Els fulls i diàlegs no són pantalles, són accions.
- **Accions** (`ui/tracking/Actions.kt`): des del ViewModel si n'hi ha; obrir fulls i diàlegs, des de la pantalla amb `rememberActionTracker()`. No es registra el que ja és una pantalla.
  - Persones: `filter_toggled`.
  - Detall: `kind_filter_toggled`, `entry_sheet_opened`, `entry_saved` (`mode`, `kind`, `channel`, `custom_channel`), `entry_menu_opened`, `entry_copied`, `entry_moved`, `entry_deleted`, `entry_restored`, `day_deleted`, `person_exported`, `person_archived`, `task_done`, `task_reopened`.
  - Nota ràpida: `entry_saved` (`mode=quick`), `person_picker_opened`.
  - Editor: `person_saved`, `person_protection_changed`, `person_deleted`, `avatar_sheet_opened`, `avatar_applied`.
  - Tasques i canals propis: `task_saved`, `task_deleted`, `custom_channel_saved`, `custom_channel_deleted`.
  - Ajustos: `setting_changed` (`setting`, `value`; inclou `analytics`), `link_opened`.
  - Paywall i onboarding: `purchase_started`, `restore_tapped`, `onboarding_finished`.
  - Sincronització (darrere del flag `sync`; mai el compte ni les paraules): `sync_sign_in_started`, `sync_sign_in_finished` (`result`: `activating`, `needs_key`, `cancelled`, `offline`, `failed`), `sync_activation_finished` (`result`: `done`, `already_active`, `offline`, `failed`), `sync_activation_cancelled` (només si hi havia una activació a mitges), `sync_words_copied`, `sync_words_submitted` (`result`: `accepted`, `wrong`, `invalid`, `offline`, `failed`), `sync_signed_out` (`from`: `ready` o `needs_key`), `sync_upload_finished` i `sync_download_finished` (`result`: `done`, `offline`, `failed`; mai recomptes), `sync_merge_shown` (el diàleg de fusió, sense el recompte d'entrades).
- **Esdeveniments** (`data/tracking/Events.kt`): `purchase_finished`, `purchase_timed_out`, `restore_finished`, `pro_changed` (només del listener de RevenueCat, fora d'una compra en curs), `app_locked`, `app_lock_disabled`, `text_import_empty`. No es registra el desbloqueig de persones protegides ni el de l'app.
- **Errors** (`data/tracking/ErrorSites.kt`, el `where`): només els que l'app ja s'empassa. `pro_price`, `pro_customer_info`, `pro_purchase`, `pro_restore` (només `Failed`), `open_link`, `open_feedback`, `open_security_setup` (només des d'Ajustos), `datastore_settings`, `datastore_pro`, `datastore_flags`, `datastore_sync`, `datastore_sync_device` (el DataStore corrupte es buida i l'app arrenca amb els valors per defecte), `sync_sign_in`, `sync_account`, `sync_key_store`.

## Firebase i CI

- Projecte `converses-72251`, apps `com.alorma.contactnotes` i `com.alorma.contactnotes.dev`.
- `app/google-services.json` és fora del repo. A la CI el crea cada workflow a partir del secret `GOOGLE_SERVICES_JSON_BASE64` (secret d'Actions i de Dependabot).
- Per veure-ho a l'emulador: `adb shell setprop log.tag.FA VERBOSE`, `log.tag.FA-SVC VERBOSE`, `log.tag.FirebaseCrashlytics DEBUG` i, per a DebugView, `debug.firebase.analytics.app com.alorma.contactnotes.dev`.

## Limitacions conegudes

- **`screen_view` amb el «Sí» en calent:** si Firebase s'inicialitza amb l'app ja oberta, Analytics es pensa que és en segon pla i rebutja els `screen_view` fins que l'app torna a primer pla. Es perden les pantalles d'aquella sessió (incloses les de la cua); els esdeveniments, les accions i els errors sí que arriben. Acceptat: no hi ha API pública per evitar-ho sense inicialitzar Firebase abans del consentiment.
- Sense bloqueig de pantalla al mòbil, l'arrencada registra `app_locked` i just després `app_lock_disabled`.
- A release, el nom de la classe dels errors propis de l'app surt ofuscat (el *stack trace* sí que es desofusca).
