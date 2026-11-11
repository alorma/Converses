# Sincronització, fase 3: compte i clau. Pla d'implementació

> **Per a qui l'executi amb agents:** primer la base comuna (tasca 0), en sèrie. Després les tasques A, B, C i D en paral·lel, cadascuna al seu worktree sobre `sync-fase-3`. Cada tasca només toca els fitxers que té assignats.

**Objectiu:** iniciar sessió amb Google, generar la clau al primer dispositiu i ensenyar-ne les paraules, donar la clau amb les paraules en un dispositiu nou, i guardar-la al dispositiu. Encara no es puja ni es baixa cap registre: això és la fase 4.

**Disseny:** `docs/sync/disseny.md` (seccions «Claus», «Compte» i «Firebase sense trencar la regla de l'analítica») i `docs/sync/disseny/INSTRUCCIONS.md` (taulers 13a, 13b, 13d, 13e, 13f, 13i, 13j, 13o, 13r i 0c).

## Què hi entra i què no

| Hi entra | Queda per a |
| --- | --- |
| Instància `sync` de Firebase, Auth amb Credential Manager, App Check | |
| Document del compte amb `wrappedKey`, `format` i `createdAt` | `plan`, `devices`, `lastSyncAt`: fases 4 i 5 |
| Clau al dispositiu (Keystore, `noBackupFilesDir`) | |
| Sense sessió (13d), empremta (13e), paraules (13f) | Pujada (13g) i «Sincronització activada» (13h): fase 4 |
| Clau en un dispositiu nou escrivint les paraules (13i, 13j) | QR (13k, 13p): fase 3b **(B, 11 de novembre de 2026)** |
| | Places (13l), fusió (13m): fases 4 i 5 |
| | «No tinc les paraules» (13n): «Comença de nou» necessita el motor, fase 4 |
| Amb sessió (13o), reduïda: compte, paraules i tancar la sessió | Estat («Al dia»): fase 4. Dispositius: fase 5. Esborrar el compte: fase 6 |
| Indicador (13a), rail (13r), fila d'Ajustos (13b), onboarding (0c) | Dispositiu tret (13c, 13o2): fase 5 |

Tot va darrere de `SyncFlag` (`data/flags`, apagat per defecte). Amb el flag apagat l'app és exactament la d'avui: sense indicador, sense fila a Ajustos, l'onboarding amb els textos actuals i Firebase `sync` sense inicialitzar.

## Privacitat externa

Amb el flag apagat no surt res de nou. Amb el flag encès, i només si l'usuari inicia sessió (opcional):

| Dada | A qui |
| --- | --- |
| Correu, nom i foto del compte de Google, UID | Firebase Auth |
| Clau de dades embolcallada, versió del format, data de creació | Firestore |
| Atestació (Play Integrity, o el token de debug) | App Check |

Permís nou al manifest fusionat: `READ_GSERVICES` (de reCAPTCHA, dependència de Firebase Auth), ja vist al spike. La política de privacitat i el formulari de Play es toquen a la fase 7, abans de treure el flag; queda pendent mesurar els servidors contactats.

## Decisions preses en aquest pla

- **(B)** Sense QR en aquesta fase. Les targetes i files que hi porten no es dibuixen.
- **(C)** Tancar la sessió entra ara en la seva forma mínima (tanca Auth i esborra la clau local): sense això no es pot provar el segon dispositiu. Alliberar la plaça arriba amb la fase 5.
- **(C)** L'avatar de l'indicador és la inicial del nom sobre `primaryContainer`, no la foto de Google: carregar-la demana una biblioteca d'imatges nova i una connexió a un servidor més. Es decideix a part.
- **(C)** La data de l'última revisió de les paraules és de cada dispositiu (DataStore `sync`).
- **(C)** La clau del Keystore que xifra el fitxer no demana autenticació de l'usuari: el motor l'ha de poder llegir sol. L'empremta es demana a la UI, i el repositori no dona les paraules si abans no s'han desbloquejat (`unlockWords`), tampoc després de restaurar el procés.
- **(C)** Firebase `sync` només s'inicialitza si el dispositiu té una sessió marcada al DataStore o quan l'usuari toca «Continua amb Google».
- **(C)** Un procés mort a mitja activació (paraules a la vista, res pujat) deixa una sessió sense clau ni document: en arrencar, es tanca.
- **(C)** Crear el document del compte és una transacció que falla si ja existeix: dos dispositius que s'activen alhora no es trepitgen la clau.

## Contracte (tasca 0)

`data/sync`:

| Peça | Què és |
| --- | --- |
| `SyncFlag` | El flag |
| `SyncAccount(uid, name, email)` | La sessió. El `toString()` no porta ni nom ni correu |
| `AuthGateway` | `currentAccount()`, `signIn(activity)`, `signOut()` |
| `AccountRemote` | `load(uid)` i `create(uid, account)` del document del compte |
| `SyncKeyStore` | `load()`, `save(keys)`, `clear()` de `StoredKeys(uid, secret, dataKey)` |
| `SyncLocalStore` | Marca de sessió i data de revisió de les paraules |
| `SyncRepository` | L'estat (`SignedOut`, `Checking`, `Activating`, `NeedsKey`, `Ready`) i les accions |

Falsos a `app/src/test/.../testing`: `FakeAuthGateway`, `FakeAccountRemote`, `FakeSyncKeyStore`, `FakeSyncLocalStore`.

Rutes: `SyncRoute`, `SecurityWordsRoute(activation)` i `EnterWordsRoute`. Les dues últimes són formularis (`isFormRoute()`): diàlegs de 560 dp en pantalla gran.

## Què cal mirar a la revisió

1. **Tancar les paraules abans de «Continua»** tanca la sessió i no deixa cap document al núvol.
2. **Paraules d'un altre compte o mal ordenades:** «no obren les dades d'aquest compte», i el dispositiu es queda a `NeedsKey`.
3. **Sense connexió en iniciar sessió o en confirmar:** cap estat a mitges; es torna a `SignedOut` o es queda a `Activating`.
4. **Amb el «No» a les estadístiques**, iniciar sessió no engega Analytics ni Crashlytics; amb el «Sí» i `sync` present, l'app no peta.
5. **Les pantalles amb paraules** (13f, 13j) surten en negre a les recents, i restaurar el procés a 13f no les ensenya sense empremta.
6. **Amb el flag apagat**, cap canvi visible i cap captura de pantalla existent canvia.

---

### Tasca 0: base comuna (en sèrie)

- [ ] Dependències al catàleg i a `app/build.gradle.kts`: `firebase-auth`, `firebase-firestore`, `firebase-appcheck-playintegrity`, `firebase-appcheck-debug` (només debug), `androidx.credentials` 1.6.0 (`credentials` i `credentials-play-services-auth`) i `googleid` 1.2.1.
- [ ] `SyncFlag`, registrat a Koin amb `bind FeatureFlag::class`.
- [ ] El contracte de `data/sync`, `SyncRepository` amb el seu test i els falsos.
- [ ] Implementacions d'Android buides però segures (no fan res i no peten), perquè el graf de Koin sigui complet a cada worktree.
- [ ] Rutes, `isFormRoute()`, `toScreen()` (`sync`, `sync_words`, `sync_enter_words`) i entrades a `AppNavigation`.
- [ ] `ui/sync`: `SyncScreen` i `SyncViewModel`, amb un fitxer buit per a cada contingut.
- [ ] Els textos de la secció 13 d'`INSTRUCCIONS.md` que fa servir aquesta fase, en els tres idiomes i amb el prefix `sync_`. Els de les pantalles que encara no existeixen arriben amb la seva fase. Textos nous, que no són al disseny: `sync_state_on`, `sync_indicator_on`, `sync_indicator_key`, `sync_error_offline` i `sync_error_failed`.

### Tasca A: Firebase, compte i magatzem de la clau

**Fitxers:** `data/sync/SyncFirebase.kt`, `FirebaseAuthGateway.kt`, `FirestoreAccountRemote.kt`, `AndroidSyncKeyStore.kt`, `data/tracking/FirebaseGateway.kt`, `di/` (també a `debug` i `release` per al proveïdor d'App Check).

- [ ] Instància `sync` (`FirebaseOptions.fromResource`), amb App Check instal·lat abans de fer-la servir: Play Integrity en release, el proveïdor de debug en debug.
- [ ] `FirebaseAuthGateway`: Credential Manager amb `GetSignInWithGoogleOption` i `default_web_client_id`, cap a `FirebaseAuth.getInstance(app)`. Cancel·lar no és un error.
- [ ] `FirestoreAccountRemote`: `load` des del servidor (no de la memòria cau) i `create` en una transacció que falla si el document ja hi és.
- [ ] `AndroidSyncKeyStore`: fitxer a `noBackupFilesDir`, xifrat amb una clau AES-GCM de l'Android Keystore. El format del fitxer és una classe JVM pura amb el seu test; un fitxer que no es pot llegir és com si no hi fos.
- [ ] `AndroidFirebaseGateway` mira `FirebaseApp.DEFAULT_APP_NAME`, a `enable()` i a `disable()`.
- [ ] Llocs nous a `ErrorSites` (`sync_sign_in`, `sync_account`, `sync_key_store`), sense `message`.

### Tasca B: sense sessió, activació i amb sessió

**Fitxers:** `ui/sync/SyncScreen.kt`, `SyncViewModel.kt`, `SyncSignedOutContent.kt`, `SyncActivatingContent.kt`, `SyncReadyContent.kt`, `SecurityWordsScreen.kt` i els seus tests, previews i captures.

- [ ] 13d: galeta, títol, tres files, botó de Google i nota al peu.
- [ ] 13e: `promptUnlock` amb `sync_bio_title` i `sync_bio_words`. Cancel·lar-lo a l'activació tanca la sessió.
- [ ] 13f: graella de 2 × 6, «Copia» (porta-retalls sensible, s'esborra al cap d'1 minut), nota, i el peu amb la casella i «Continua» només a l'activació. `FLAG_SECURE`.
- [ ] 13o reduïda: compte, «Paraules de seguretat» amb la data de revisió, «Tanca la sessió» amb la seva confirmació, i el peu.

### Tasca C: clau en un dispositiu nou

**Fitxers:** `ui/sync/SyncNeedsKeyContent.kt`, `EnterWordsScreen.kt`, `EnterWordsViewModel.kt` i els seus tests, previews i captures; `Bip39` si cal una funció de suggeriments.

- [ ] 13i: només la targeta «Escriu les paraules», la fila del compte amb «Canvia» (tanca la sessió).
- [ ] 13j: 12 camps en 2 columnes, «Enganxa» (`Bip39.split`), suggeriments, teclat sense aprenentatge, paraules desconegudes en `error`, i la nota de «no obren aquest compte». `FLAG_SECURE`.

### Tasca D: indicador, Ajustos i onboarding

**Fitxers:** `ui/sync/SyncIndicator.kt`, `ui/people`, `ui/settings`, `ui/onboarding`, `ui/navigation/AppShell.kt`, `ui/AppNavigation.kt` (només les crides noves) i `:designsystem` (el rail).

- [ ] 13a: indicador a la dreta de `more_vert`, amb els estats sense sessió, amb sessió i «falten les paraules».
- [ ] 13r: a la tauleta, a baix del rail amb l'etiqueta «Compte», i la capçalera sense indicador.
- [ ] 13b: secció «Compte» sota la targeta de Pro, amb la fila «Sincronització».
- [ ] 0c: «Ja tinc dades al núvol», els dos xips i el text nous.
- [ ] Tot això, només amb `SyncFlag` encès.

### Després de fusionar

- [ ] `./gradlew ktlintCheck testDebugUnitTest :app:assembleDebug` net.
- [ ] Prova a l'emulador amb el flag encès: activar, tancar la sessió, tornar a entrar amb les paraules, paraules equivocades.
- [ ] `docs/sync/disseny.md`: secció «Construït (fase 3)».
