# Sincronització, fase 0: spike de Firebase. Guió

Un spike: codi que **es llença**. No porta tests, ni revisió, ni textos traduïts, i no es fusiona mai a `main`. L'únic que en queda és la secció «Resultat del spike» que s'afegeix a `docs/sync/disseny.md`.

**Disseny:** `docs/sync/disseny.md`, seccions «Firebase sense trencar la regla de l'analítica» i «Fases».

## Les preguntes que ha de respondre

1. Amb sessió iniciada i estadístiques en «No», surt alguna cosa cap a Analytics o Crashlytics? Si en surt, cal una segona instància de `FirebaseApp` amb nom, i el disseny canvia.
2. L'inici de sessió amb Google (Credential Manager) cap a Firebase Auth funciona amb el projecte `converses-72251` i l'empremta de signatura de debug i de Play?
3. App Check amb Play Integrity deixa passar l'app i rebutja una crida sense atestació? Com es treballa en debug i a l'emulador (proveïdor de debug)?
4. Quins servidors es contacten en total i quins identificadors nous surten del telèfon (per exemple, l'identificador de Firebase Installations)? Cal per a la taula de «Privacitat externa» i per a la política.
5. Quins permisos afegeixen les biblioteques al manifest fusionat?
6. Quines versions estables hi ha avui d'Auth, Firestore, App Check, Credential Manager i `googleid`, i són compatibles amb el `firebase-bom` del projecte (35.1.0) o cal pujar-lo?

## Abans de començar: només Bernat (consola)

Res d'això es pot fer des del codi, i el segon punt no es pot desfer.

- [ ] **Authentication:** activar el proveïdor de Google.
- [ ] **Firestore:** crear la base de dades i **triar-ne la regió** (no es pot canviar). El disseny proposa una regió europea; cal mirar si el projecte en té una per defecte.
- [ ] **App Check:** registrar l'app amb Play Integrity i afegir un token de debug per a l'emulador. Sense forçar-lo encara a Firestore; es força durant el pas 4.
- [ ] **Empremtes SHA-1 i SHA-256** de la clau de debug i de la clau de signatura de Play a la configuració de l'app, i baixar el `google-services.json` nou.
- [ ] **Regles de Firestore** mínimes per al spike: només `request.auth.uid == uid` llegeix i escriu sota `users/{uid}`.

## Passos

Tot en una branca `spike/sync-firebase`, en un emulador amb Google Play i un compte de proves.

- [ ] **1. Versions.** Mira a la documentació oficial les versions estables actuals de les biblioteques de la pregunta 6 i afegeix-les a `gradle/libs.versions.toml`. Apunta-les.
- [ ] **2. Manifest fusionat.** Compila i compara `app/build/intermediates/merged_manifests/debug/…/AndroidManifest.xml` amb el de `main`: permisos, `provider`s i `service`s nous. Apunta'ls.
- [ ] **3. Pantalla de debug.** Una entrada al menú de debug (`ConversesDebugMenu`) amb quatre botons: inicialitzar Firebase **sense** encendre la recollida (`FirebaseApp.initializeApp` i prou, sense passar per `FirebaseGateway.enable()`), iniciar sessió amb Google, escriure i llegir un document a `users/{uid}/spike/ping`, i tancar sessió.
- [ ] **4. App Check.** Força App Check a Firestore des de la consola i comprova que l'app continua escrivint i que una crida REST amb el token d'Auth però sense App Check és rebutjada.
- [ ] **5. Què surt del telèfon.** Amb el consentiment d'estadístiques en «No» i la sessió iniciada, fes servir l'app deu minuts (crea persones i entrades, canvia de pantalla, tanca-la i torna-la a obrir) i provoca un crash. Mira-ho de dues maneres:
  - **Registres:** `adb shell setprop log.tag.FA VERBOSE`, `log.tag.FA-SVC VERBOSE` i `log.tag.FirebaseCrashlytics DEBUG`, i `adb logcat` filtrant aquestes etiquetes. No hi ha d'haver cap pujada d'esdeveniments ni d'informes.
  - **Trànsit:** l'emulador darrere d'un proxy (mitmproxy) amb una configuració de xarxa de debug que accepti el seu certificat. Apunta **tots** els servidors contactats. No n'hi ha d'haver cap d'Analytics ni de Crashlytics.
- [ ] **6. La mateixa prova amb «Sí».** Per confirmar que el mètode de mesura veu el trànsit d'Analytics i de Crashlytics quan sí que n'hi ha. Sense això, un resultat net al pas 5 no demostra res.
- [ ] **7. De «Sí» a «No» amb sessió.** Canvia el consentiment i repeteix el pas 5: la recollida s'atura i Auth i Firestore continuen funcionant.

## Què se n'escriu

Una secció «Resultat del spike» a `docs/sync/disseny.md` amb:

- una sola instància de Firebase o dues, i per què;
- la llista de servidors i d'identificadors que surten del telèfon, per completar «Privacitat externa»;
- els permisos i components que afegeixen les biblioteques;
- les versions triades i si cal pujar el BOM;
- com es treballa amb App Check en debug i al CI;
- qualsevol sorpresa que canviï les fases 3 a 6.

Després s'esborra la branca, es treuen de la consola el document de prova i el token de debug si no s'ha de reaprofitar, i es planifiquen les fases 3 a 6.

## Si la resposta a la pregunta 1 és «sí que en surt»

No s'arregla dins del spike. Es prova l'alternativa del disseny (segona instància de `FirebaseApp` amb nom, només per a Auth, Firestore i App Check), es repeteixen els passos 5 a 7, i el resultat va al disseny abans de planificar res més.
