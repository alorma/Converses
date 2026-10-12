# Converses per a Wear OS — Fases

Context, abast, arquitectura, decisions (P0–P16) i riscos són a `docs/wear/README.md`; les captures de referència, a `docs/wear/screenshots/`. Cada fase acaba amb alguna cosa que es pot provar; no es comença la següent sense validar l'anterior.

| Fase | Títol | Depèn de | Estat |
| --- | --- | --- | --- |
| 0 | Decisions i investigació | — | Feta, amb 3 pendents que no bloquegen (català i mecanisme de dictat: abans de la fase 6; AVD i Data Layer: fases 1–2) |
| 1 | Mòdul `:wear`, tema i previews amb dades fictícies | 0 | Feta |
| 2 | Contracte i sincronització mòbil → rellotge | 0 | Feta |
| 3 | Pantalles de lectura: Persones, Cronologia, Sense mòbil | 1, 2 | Feta |
| 4 | Més entrades sota demanda | 3 | Feta |
| 5 | Bloqueig de l'app i persones protegides | 0 (P0, P10), 3, 4 | Feta |
| 6 | Dictat | 1 | Feta |
| 7 | Qui, Desat i nota rellotge → mòbil amb cua | 2, 3, 6 | Feta |
| 8 | Estats d'error i casos límit | 5, 7 | Feta |
| 9 | Qualitat, privacitat i accessibilitat | 8 | Pendent |
| 10 | CI i publicació | 9 | Pendent |
| 11 | Més endavant | 10 | Idees |

---

Com es treballa cada fase (tasques, commits, validació, proposta d'inici): `.claude/rules/phased-work.md`.

---

## Fase 0 · Decisions i investigació

Sense codi de producte.

- [x] **P0:** aprovació al mòbil amb empremta i notificació com a camí (decidit); revisat amb el disseny: sense credencial al rellotge i una aprovació nova a cada obertura.
- [x] Decidir si la vigència és un ajust (Ajustos → Seguretat) o queda fixa a 5 min (P11): es va fixar a 5 min, i després **es descarta**: aprovació nova a cada obertura.
- [x] Demanar el disseny de les pantalles noves (P12): rebut el 12 d'oct. de 2026 (`docs/wear/handoff/`).
- [x] Tancar P1, P6, P7, P9, P11, P14 i P15 (vegeu `README.md`).
- [x] Consultar la documentació oficial: Wear Compose Material 3 i navegació, Data Layer (`play-services-wearable`), `EdgeButton`, `TransformingLazyColumn`, reconeixement de veu i requisits de Play per a apps de Wear. Versions anotades al `README.md` i afegides al version catalog.
- [x] **Versionat:** s'amplia `alorma/VersionPlugin` (1.1.0, encara no publicada) amb `appVersion { formFactor = FormFactor.WEAR }`: enum PHONE=0 (per defecte), WEAR=2, TV=4, AUTOMOTIVE=6, i `versionCode` = base + offset (1.2.3 → 10020031; WEAR → 10020033). `:app` no canvia. Document inicial: `~/projects/personal/VersionPlugin/docs/version-code-offset.md` (parlava de `versionCodeOffset`; el disseny final és `formFactor`). **Resolt a la fase 1** (1.1.0 publicada i integrada).
- [x] `minSdk` de `:wear`: **30** (Wear OS 3+).
- [x] **P10:** comprovat amb una app de prova descartable a l'emulador Wear OS 7 (API 37), `minSdk` 30 acordat. Resultats: sense bloqueig `isDeviceSecure=false`, el credencial intent és `null` i **no es pot crear una clau que exigeixi autenticació** (cal el bloqueig de pantalla); amb PIN, `isDeviceSecure=true`, el credencial intent apunta als ajustos del rellotge, la clau es crea, i el xifratge queda **bloquejat** (`UserNotAuthenticatedException`) un cop caducada la finestra d'autenticació (provat a 40 s amb una finestra de 30 s); en treure el PIN, **la clau desapareix sola**. Limitacions: és un emulador (`insideSecureHardware=false`, Keystore per programari) i no hi ha prova a l'API 30; cal repetir-ho al rellotge real a la fase 9. Conclusió: s'adopta el xifratge amb Keystore lligat a autenticació.
- [ ] Comprovar si el reconeixedor de veu del rellotge suporta **català** (P5); si no, avisar abans de cercar alternatives. **Pendent:** la documentació no ho confirma; cal rellotge real (abans de la fase 6).
- [ ] Triar el mecanisme de dictat (`SpeechRecognizer` amb parcials o `ACTION_RECOGNIZE_SPEECH`) i explicar-ne el motiu. **Pendent**, depèn de la prova anterior; plantejament: `SpeechRecognizer` amb parcials, pla B `ACTION_RECOGNIZE_SPEECH`.
- [ ] Preparar emuladors: parell Wear + telèfon amb Google Play. Fet: telèfon (Android 17) i `Wear_OS_XL_Round` ja **emparellats**. Pendent: AVD Large Round i Small Round (fase 1) i comprovar que la Data Layer hi funciona (fase 2).
- [x] Confirmar com es publica una app de Wear emparellada a la mateixa fitxa de Play: investigat; els punts no verificats (mateixa release, GPP, formulari de dades) passen a la fase 10.

**Sortida:** decisions escrites i versions fixades.

## Fase 1 · Mòdul `:wear`, tema i previews

- [x] Afegir `:wear` (i `:shared` si es confirma) a `settings.gradle.kts` i al catàleg de versions. — `5708737` (`:shared` queda per a la fase 2)
- [x] Mateix `applicationId` que `:app`, amb sufix `.dev` i signatura de debug com al mòbil; manifest amb `type.watch` i `standalone = false`; `allowBackup=false` i regles de backup. — `5708737`
- [x] **Versionat:** `VersionPlugin` 1.1.0 (ja publicada) aplicat a `:wear` amb `appVersion { formFactor = FormFactor.WEAR }`, sense import. Comprovat: `:wear` 10060012 i `:app` 10060010 (1.6.1 snapshot).
- [x] `ConversesWearTheme` amb els tokens del brief (negre pur, `surface-card`, `surface-card-high`, `primary`, `on-surface`…, avatars pastel i tinta `#1E1D24`) i Roboto Flex (subconjunt llatí, com al mòbil). — `f5bcfcc`
- [x] Components: llista agrupada (separació 2, radi 24 fora i 6 dins), fila de persona, entrada de cronologia (radi 18), grup connectat del «Qui», Galeta d'avatar. — `components/` (previews a la tasca 5)
- [x] Les 9 pantalles com a previews amb dades fictícies, a Large i Small Round, i captures de preview comparades amb `docs/wear/screenshots/` — `ui/` i `WearScreenshotTests` (18 captures de referència a `wear/src/screenshotTestDebug/reference`);.
- [x] Textos a `strings.xml` en en/ca/es; reutilitzar els del mòbil (Jo, Apunt, Desa, grups de canal) i afegir només els nous.
- [x] ktlint al mòdul; `testDebugUnitTest`, `ktlintCheck` i els APK actuals sense trencar-se.

**Sortida:** les 9 pantalles es veuen a les previews i s'instal·la un APK de debug a l'emulador.

## Fase 2 · Contracte i sincronització mòbil → rellotge

La part més delicada: privacitat.

Decidit en proposar la fase (12 d'oct. de 2026): les persones **arxivades no van al rellotge**; el text de les entrades es **talla a ~280 caràcters**; `:shared` és el **domini compartit** (enums d'avatar i entrada, contracte i constants).

- [x] **Domini compartit:** mòdul `:shared` (Kotlin/JVM pur) amb `AvatarColor` (ARGB), `AvatarShape`, `ChannelType` i `EntryKind`; `:designsystem` i `:wear` només hi afegeixen la conversió a `Color`; el rellotge perd la seva còpia d'`AvatarColor`. Schema de Room intacte. — `d32269a`
- [x] Contracte (a `:shared`): models d'instantani amb **versió de protocol**, serialització kotlinx.serialization; `sanitized()` aplica la privacitat en codificar i en descodificar; text tallat a 280. — `3dce9ef`, `866cfed` (la versió s'ha d'escriure sempre: trobat a l'emulador)
- [x] `WearSnapshotPublisher` al mòbil: construeix l'instantani des de Room i el publica amb `DataClient` (darrere de `WearLink`); debounce, no republica si res no ha canviat, republica en connectar-se. — `a5ead83`
- [x] **Només es publica si hi ha rellotge** (decidit el 12 d'oct. de 2026): «hi ha rellotge» = algun node amb la capacitat `converses_watch` (encara que no sigui a prop) o algun node connectat; sense rellotge no es publica res i s'esborra l'instantani de la Data Layer. Play Services no té *listener* de nodes: l'estat es reavalua en arrencar i en cada canvi de capacitat. **Provat als emuladors:** amb el rellotge apagat i el mòbil reiniciat no es deixa res a la Data Layer (en tornar a encendre'l no reapareix la còpia antiga), i en connectar-se el rellotge el mòbil publica un instantani nou sense reiniciar l'app. Pendent amb un rellotge real (fase 9). Els tests de JVM cobreixen sense rellotge, apareix, desapareix i torna.
- [x] Contingut: persones no arxivades (`id`, nom, color i forma d'avatar, `protected`, hora de l'última entrada), **5 últimes entrades** per persona no protegida, i l'**estat del Bloqueig de l'app**. — `5b8ebaf`
- [x] **Filtre de protegides al mòbil:** només `id`, nom, color i `protected = true`; cap entrada, canal ni hora (consulta SQL, constructor i `sanitized()`). — `5b8ebaf`
- [x] Capacitats `converses_phone` (mòbil) i `converses_watch` (rellotge). — `a5ead83`
- [x] Còpia local al rellotge (DataStore a `files/`, fora del backup) amb l'hora de l'última sincronització, i `SnapshotListenerService`. Sense xifrar: és de la fase 5. — `b98f8c6`
- [x] Tests de JVM: protegida sense contingut, passa a protegida i perd les entrades, límit de 5, ordre, text tallat, versió, bloqueig, debounce i errors d'enviament (`:shared` 12, `:app` 18, `:wear` 7).
- [x] Room: **no es toca** (només una consulta nova de DAO; `app/schemas` intacte). Cap migració.
- [x] **Emulador** (telèfon + Wear XL Round, dades d'exemple): el rellotge rep i desa l'instantani; la protegida (Luna Roja) arriba només amb nom, color i forma; en tornar a obrir el mòbil, es republica.

**Sortida:** amb dades reals del mòbil, el rellotge rep i desa un instantani correcte; els tests de privacitat passen.

## Fase 3 · Pantalles de lectura

- [x] Persones (1) i Persones sense mòbil (8), amb «Dades de fa N min» (o «Dades d'ara mateix» i hores). Les protegides només mostren nom i cadenat; el toc no fa res fins a la fase 5. — `877e978`, `ac82280`, `1b86288`
- [x] Cronologia (2) amb l'`EdgeButton` «Nota» (sense destí fins a la fase 6/7) — `ac82280`: comprovar P2 i, si cal, forçar que sigui sempre visible. **P2 ja comprovat a la fase 1:** el d'`ScreenScaffold` només apareix en arribar al final de la llista; el mòdul usa `PinnedEdgeButton` (sempre visible, amb degradat).
- [x] Cronologia buida (P3): només el botó «Nota» gran al centre; sense disseny, pendent de confirmar. — `ac82280`
- [x] Detecció de «mòbil no connectat» amb `NodeClient`/`CapabilityClient`: als emuladors les capacitats no es propaguen, així que també compten els nodes connectats. — `d1bca50`, `eb13d67`
- [x] Navegació amb Navigation 3 (`NavDisplay` + `rememberSwipeDismissableSceneStrategy`), decidida a la fase 0. — `1b6af5e`, `ac82280`
- [x] ViewModels amb tests de JVM (`SnapshotMappingTest`, `ViewModelsTest`); captures de preview noves (cronologia buida i sense dades). — `1577d2d`, `877e978`, `ac82280`

**Sortida:** navegar Persones → Cronologia amb dades reals; estat sense mòbil correcte.

## Fase 4 · Més entrades sota demanda

- [x] Contracte a `:shared` (`/converses/entries`, petició amb cursor `occurredAt`+`id`, resposta amb `hasMore`/`denied`, pàgines de 20) i tests del codec. — `5b8eb25`
- [x] Mòbil: `WearEntriesService` + `WearEntriesResponder` (`MessageClient.sendRequest`/`onRequest`); consulta paginada al DAO (sense canvi d'esquema, sense migració). Rebutja protegides, arxivades i inexistents; tests de JVM. — `3f52dfa`
- [x] Rellotge: `MoreEntriesSource` (`GmsMoreEntriesSource`, temps màxim de 8 s). — `f788b51`
- [x] `TimelineViewModel`: les entrades extra viuen **només en memòria** i s'obliden en sortir; mòbil inaccessible: la llista s'acaba sense error (P13); límit de memòria (P7: 100 extra per persona); tests de JVM (paginació, final de llista, mòbil absent, límit, sense duplicats, res al disc). — `f660f2d`
- [x] UI: en acostar-se al final es demana la pàgina següent; indicador de càrrega al final (P16) amb captura de referència. Validat a l'emulador (Marta Soler amb 65 entrades: carrega fins a l'última i s'atura sense error). — `42a61d5`

Validada per Bernat. El cas del mòbil realment inaccessible només està cobert per tests de JVM.

**Sortida:** una persona amb moltes entrades les va carregant mentre es fa scroll.

## Fase 5 · Bloqueig de l'app i persones protegides

Depèn de **P0** i **P10**.

**Contracte**
- [x] `:shared`: `WearApproval` (petició, cancel·lació, ack, resultat amb entrades i testimoni de nota, `sanitized()` i codec) i tests (9). Camins `/converses/approval`, `/cancel` i `/result`; resultat del mòbil al rellotge per `MessageClient`. — `61546d6`

**Bloqueig de l'app**
- [x] Llegir l'estat del bloqueig de l'instantani; si no es pot verificar, assumir **actiu**. — `a8d5178` (`AppLockGate`)
- [x] Si és actiu i el rellotge no és segur (`isDeviceSecure`): pantalla 9 («Cal un bloqueig», «Configura», que obre els ajustos de seguretat si el sistema ho permet). No mostra ni desa cap entrada. — `a8d5178`
- [x] Còpia local xifrada amb Keystore (AES-GCM, clau `setUnlockedDeviceRequired`: il·legible amb el rellotge bloquejat; si no es pot xifrar es conserva la còpia anterior; si no es pot desxifrar es tracta com si no hi fos). La còpia antiga en clar s'esborra en arrencar. Tests de JVM amb un xifrat de mentida (4 nous) i **emulador**: es desa xifrat a `files/datastore` i es llegeix. Pendent amb rellotge real (fase 9): comportament de la clau en bloquejar el rellotge i en treure el PIN. — `b933332`
- [x] Esborrar la còpia si es treu el bloqueig del rellotge, en obrir l'app (en tornar a primer pla) i a cada sincronització. — `a8d5178`. **Emulador** (telèfon + Wear XL Round): amb el bloqueig actiu i sense PIN es veu la pantalla 9 i la còpia s'esborra; «Configura» obre Seguretat → Bloqueig de pantalla; amb PIN, l'app s'obre; en treure'l, torna la pantalla 9; amb el bloqueig desactivat, tot torna. Tests de JVM (`AppLockGateTest`, 6; `WearSnapshotStoreTest`, 2 nous).
- [x] La cua de pendents segueix les mateixes regles: **s'esborra** (decidit el 12 d'oct. de 2026: les notes es perden, no es xifren) quan cal bloqueig i el rellotge no en té, en sincronitzar o en intentar entregar-la (`enforceLock`). «Més entrades» ja viu només en memòria i no es pot arribar a la pantalla amb la pantalla 9. Tests de JVM (3 nous a `WearSnapshotStoreTest`, 1 a `NoteOutboxTest`). — `9c14da8`

**Persones protegides**
- [x] **Disseny (P12):** rebut (pantalles 7, 10–12 al rellotge i 13–16 al mòbil).
- [x] Rellotge: toc a una protegida → pantalla 7 «Confirma-ho al mòbil» (compte enrere de 60 s, «Cancel·la»), 10 «Denegat», 11 «Ha caducat» (amb «Reintenta») i 12 «No disponible sense el mòbil»; `ApprovalViewModel` (10 tests de JVM), `GmsApprovalClient` i `ApprovalResultListenerService`; sense credencial al rellotge i sense finestra de vigència: una aprovació nova a cada obertura (P0, P11). Captures de referència a Large i Small Round (8, comparades amb el disseny; a Small Round es compacta la galeta perquè hi càpiga). La pantalla 7 antiga («Desbloqueja») i el seu test s'eliminen. — `a65f9bb`
- [x] **Aprovació al mòbil (P0), canvis decidits amb Bernat:** amb l'app en primer pla s'obre **directament el `BiometricPrompt`** (pantalla 15), sobre una finestra transparent i sense notificació; en segon pla, notificació genèrica (13) i, en tocar-la, el mateix prompt. Només si l'usuari el cancel·la es veu la pantalla 14 («Confirma» reobre el prompt, «Denega» denega). Després d'aprovar la pantalla es tanca sola: **no hi ha pantalla 16 «Fet»**. El títol del prompt du el **nom del rellotge** (`Node.displayName`, amb «El rellotge» si no es coneix) i el **nom de la persona**: «{rellotge} vol desbloquejar {persona}»; la notificació continua sense nom (R1). Si el rellotge cancel·la, o la petició caduca, amb el prompt o la pantalla oberts, el mòbil els tanca sol (el diàleg del sistema es cancel·la explícitament). Provat a l'emulador. Tests de JVM (`details`, cancel·lació i caducitat). — `48c33f3`, `b9db5c0`
- [x] Mòbil: petició caducada oberta des de la notificació, o cancel·lada al rellotge amb el prompt obert: tanca la pantalla amb un missatge breu («No s'ha pogut completar la petició»), sense nom. — `747e2fa`
- [x] Mòbil: `WearApprovalService` + `WearApprovalCoordinator` (petició d'un sol ús, caducitat de 60 s validada pel mòbil, màxim 3 pendents i 5 per minut, ack immediat, cancel·lació, resultat al rellotge per `MessageClient`, sense registrar contingut) i `ApprovalTokens` (testimoni de 5 min per persona, en memòria). Tests de JVM (14 + 3). — `be87ace`
- [x] Mòbil, UI: `ApprovalActivity` (`BiometricPrompt` reutilitzat; sense nom ni contingut; si la petició ja no és vàlida, tanca amb el missatge «No s'ha pogut completar la petició»), `ApprovalViewModel` (9 tests de JVM), notificació genèrica real (`AndroidApprovalNotifier`, caduca amb la petició), `StatusBadge` a `:designsystem`, textos en en/ca/es i permís `POST_NOTIFICATIONS`, que es demana en obrir l'app si hi ha rellotge emparellat. **Provat a l'emulador (telèfon + Wear XL Round):** amb l'app oberta el prompt surt directament; en cancel·lar-lo apareix la pantalla 14 i «Confirma» el reobre; amb el PIN, la pantalla es tanca i el rellotge obre la cronologia; amb l'app en segon pla surt la notificació genèrica. **Pendent:** referència de la captura `approvalConfirm` (etiqueta `update-screenshots` a la PR). — `747e2fa`, `48c33f3`
- [x] Testimoni de vigència curta (5 min) per a notes a protegides: emès (`be87ace`), el mòbil rebutja les notes a protegides sense un testimoni vàlid, vigent i d'aquella persona (`WearNote.approvalToken`, camp opcional; `WearNoteReceiver`; 3 tests nous al mòbil i 2 a `:shared`) — `9e92302`.
- [x] Mostrar les entrades **només en memòria** (`ProtectedSessions`): s'esborren en sortir de la persona i amb l'app en segon pla (`MainActivity.onStop`); comprovat a l'emulador que no queda al disc el text d'una protegida i que en tornar de segon pla la protegida torna a estar tancada. Tests de JVM (4). — `a65f9bb`
- [x] Sense mòbil: pantalla 12 «No disponible sense el mòbil» (preview i test del ViewModel; no provada a l'emulador amb el mòbil realment absent). — `a65f9bb`
- [x] Gravar una nota a una protegida només dins una sessió aprovada. **Decidit amb Bernat:** la nota **no passa per la cua** (que és al disc, en clar): s'envia directament al mòbil amb el testimoni, només en memòria; si el mòbil no hi és es veu la pantalla 12, si el rebutja (testimoni caducat) la 11, i la nota **no es desa enlloc** (cal dictar-la de nou). Les notes normals continuen anant a la cua. Tests de JVM (4 nous a `WhoViewModelTest`). **Pendent amb rellotge real:** dictat de punta a punta (l'emulador no genera text). La cronologia d'una protegida no mostra la nota nova fins a tornar-la a obrir. — `17f499f`
- [x] Al mòbil: servir les entrades d'una protegida **només amb una aprovació biomètrica vàlida** (P0): `WearEntriesResponder` la nega sempre i només `WearApprovalCoordinator.approve` les envia, després del `BiometricPrompt`. — `be87ace`
- [x] Tests de JVM: res d'una protegida al disc ni a la còpia, esborrat en sortir, estat del bloqueig, assumir actiu; una petició sense aprovació, caducada o repetida no serveix res; una nota a protegida sense testimoni vàlid es rebutja. (`:shared` 36, `:app` 152 i `:wear` 88 a la darrera execució completa, amb ktlint i `validateDebugScreenshotTest` del rellotge en verd.)
- [x] **Emulador de punta a punta** (telèfon + Wear XL Round), amb els APK finals: aprovar amb l'app oberta (prompt directe, la pantalla es tanca, el rellotge obre la cronologia sense rastre al disc), aprovar des de la notificació genèrica (app en segon pla), cancel·lar el prompt (apareix «Confirma» / «Denega» i «Confirma» el reobre), «Denega» (rellotge: «Denegat»), caducar (rellotge: «Ha caducat», el mòbil tanca el prompt; «Reintenta» fa una petició nova i s'aprova), cancel·lar al rellotge (el mòbil tanca el prompt) i passar a segon pla (la protegida torna a estar tancada). **No provat:** mòbil realment absent (pantalla 12, només tests de JVM), dictat i nota a una protegida de punta a punta, i tot el que depèn d'un rellotge real (biometria, clau Keystore en bloquejar-lo i treure el PIN → fase 9).

**Sortida:** amb el bloqueig actiu i rellotge sense PIN, només es veu la pantalla 9; amb PIN, una protegida s'obre i no queda rastre en sortir.

Validada per Bernat. Provada a l'emulador (telèfon + Wear XL Round); el rellotge físic no s'ha provat: biometria real, clau Keystore en bloquejar el rellotge i en treure el PIN, i dictat i nota a una protegida de punta a punta queden per a la fase 9. Falta la referència de la captura `approvalConfirm` de l'app (etiqueta `update-screenshots` a la PR).

## Fase 6 · Dictat

- [x] `SpeechSource` (`SpeechRecognizer` amb parcials, `ACTION_RECOGNIZE_SPEECH` com a pla B no ha calt) i `DictationViewModel`; tests de JVM. — `9dea534`
- [x] Pantalla 3 amb text parcial en directe i «Acaba»; el botó «Nota» de la cronologia hi porta. — `92ab57a`
- [x] Permís `RECORD_AUDIO` (flux de petició; si el sistema ja no mostra el diàleg, «Permet» obre els ajustos de l'app). — `9dea534`, `92ab57a`
- [x] Ones lligades a l'amplitud del micròfon; quietes si s'han desactivat les animacions del sistema. — `92ab57a`
- [x] Idioma el de l'app (ca/es/en); el català depèn del reconeixedor del rellotge (P5, sense confirmar). — `92ab57a`
- [x] Estats d'error i permís denegat (P4): sense veu, sense reconeixedor, micròfon, error genèric i permís, amb captures. — `92ab57a`

Validada per Bernat. Provada a l'emulador Wear (permís, escolta, error i reintent); «Acaba» deixa la transcripció a la pantalla i la fase 7 la passarà a «Qui». El català no s'ha comprovat.

**Sortida:** dictar una frase i veure'n la transcripció al rellotge.

## Fase 7 · Qui, Desat i nota rellotge → mòbil

- [x] «Qui» (4): Jo / {persona} / Apunt amb els mateixos textos que `whoLabel(...)`; per defecte la persona; «Desa». — `6cd9421`
- [x] Cua de pendents amb UUID per nota, a DataStore (`files/`, fora del backup), i entrega amb **WorkManager 2.12.0** (sobreviu al tancament de l'app; *backoff* exponencial de 30 s; es reprogramen en arrencar i quan torna el mòbil). Les notes queden a la cua fins que el mòbil les confirma o les rebutja (P14). — `300204a`
- [x] Enviament amb `MessageClient.sendRequest` a `/converses/note`, amb contracte a `:shared` (`WearNote`, text de fins a 4.000 caràcters: no es talla com les entrades, es rebutja si hi passa). — `9de440e`, `300204a`
- [x] `WearNoteService` + `WearNoteReceiver` al mòbil: valida que la persona existeixi, no sigui arxivada ni protegida, desa com una entrada nova i ignora duplicats per UUID (registre de les últimes 500 id a DataStore). **Canal (P9):** el de la nota ràpida, és a dir, l'últim usat amb la persona o el primer. **Cap canvi d'esquema de Room: sense migració.** — `584f415`
- [x] Desat (5) i Desat sense mòbil (6): es tanca sola als 2 s i torna a la Cronologia. «Sense mòbil» es mostra si en desar el mòbil no era accessible; l'esborrat de la cua passa quan el mòbil confirma. — `6cd9421`
- [x] Haptic `Confirm` en desar (P6), pel sistema de feedback de Compose (el rellotge no té cap interruptor propi de l'app). — `6cd9421`
- [x] Tests de JVM: contracte (5), mòbil (5: desa, canal, duplicat, protegida/arxivada/inexistent, il·legible), cua i reintents (7) i «Qui» (6). Captures de referència intactes (`validateDebugScreenshotTest`).
- [x] **Emulador** (telèfon + Wear XL Round): amb el mòbil a prop, la nota arriba al mòbil i el rellotge torna a la cronologia; amb el mòbil sense resposta la nota queda a la cua i s'entrega sola en ~30 s en tornar, sense duplicats (4 «Desa» = 4 entrades). El dictat real no es pot fer a l'emulador (no genera text): per provar «Qui» s'ha arrencat temporalment a `WhoRoute` amb text fix (canvi no commitejat). **Pendent amb rellotge real:** dictat de punta a punta i «Sense mòbil» real.
- Les notes a persones **protegides** no es poden crear (la fase 5 és pendent) i el mòbil les rebutja; el descart de notes a persones esborrades o fetes protegides és de la fase 8 (R3, R4).

Validada per Bernat amb les proves de l'emulador i els tests; el rellotge físic no s'ha pogut provar (no s'engega), així que el dictat real i «Sense mòbil» real queden per a quan es pugui (fase 9).

**Sortida:** apuntar una nota des del rellotge i veure-la al mòbil, amb i sense connexió.

## Fase 8 · Estats d'error i casos límit

- [x] Mòbil absent durant tota la sessió; rellotge reiniciat amb notes a la cua: ja cobert (cua a disc, reprogramació en arrencar, tests de `NoteOutboxTest`); test nou de reintent després de reinici. — `209742c`
- [x] Nota duplicada: la cua ignora UUID repetits i el mòbil també (registre de les últimes 500). — `209742c`
- [x] Persona que passa a protegida (R3) i persona esborrada (R4), **decidit amb Bernat: es descarta**. A cada sincronització la cua esborra les notes de persones protegides o que ja no hi són (`retainOnly`), perquè no quedi text al disc; si el mòbil rebutja una nota, també es descarta. Avís mínim a Persones («N notes descartades», toc per tancar; comptador persistent sense contingut ni persona), amb captura a Large i Small Round. — `209742c`, `7baa9df`
- [x] Reconeixedor sense resultat (P5): revisat, sense canvis (sense veu → «No s'ha sentit res», sense reconeixedor, micròfon i error genèric ja tenen pantalla). Un idioma no suportat cau a l'error genèric. **Pendent amb rellotge real (fase 9):** si el català hi és.
- [x] Bloqueig de l'app activat o desactivat mentre el rellotge és obert: `AppLockGate` reacciona al següent instantani i a `ON_RESUME`; test nou. — `5767fe3`. **Emulador** (telèfon + Wear XL Round, rellotge sense PIN): amb el rellotge obert a la llista, activar el Bloqueig de l'app al mòbil el porta sol a «Cal un bloqueig» i n'esborra la còpia; en desactivar-lo i posar PIN al rellotge, en tornar a obrir l'app torna la llista. **No provat a l'emulador:** l'avís «N notes descartades» (no es pot dictar; només tests de JVM i captura).

## Fase 9 · Qualitat, privacitat i accessibilitat

- [x] Repàs de privacitat de punta a punta: res de protegides al disc del rellotge (còpia, logs, backup, notificacions). Test que ho comprova (`PrivacyAuditTest`: un instantani amb contingut d'una protegida no arriba al fitxer ni amb un xifrat que deixi el text en clar, i el codi del rellotge no usa logs, notificacions ni altre emmagatzematge). **Emulador:** `files/` només té la còpia xifrada i la cua buida, la base de WorkManager no duu text de notes i les notes a protegides ja no passen per la cua (WhoViewModel). — `0a39811`
- [x] Còpia i cua fora del backup d'Android: `BackupRulesTest` comprova `allowBackup=false` i que els tres fitxers de regles exclouen tots els dominis. L'emulador Wear no té servei de backup (`dumpsys backup`), així que no s'ha provat en viu. — `1467c56`
- [x] Large i Small Round, mida de lletra gran, ambient, ca/es/en. Les captures ja cobreixen Large i Small Round (ca); a l'**emulador** (Wear XL Round) s'ha provat lletra a 1,3 (llista i cronologia sense talls) i l'app en en/ca/es (cadenes: 55 a cada idioma). **Ambient:** l'app no el gestiona (cap codi d'ambient): en passar a ambient el sistema la deixa i el contingut no queda a la pantalla; es deixa així per privacitat.
- [x] TalkBack al rellotge (parcial). Trobat i arreglat: una persona protegida només tenia el cadenat decoratiu (TalkBack no deia que ho fos) i les inicials de l'avatar es llegirien abans del nom; ara la fila diu «Persona protegida» (`stateDescription`, ca/es/en) i les inicials s'amaguen. L'avís «N notes descartades» té `Role.Button`. Validat amb l'arbre d'`uiautomator` i els tests. **No verificat:** la veu real de TalkBack (l'emulador no mostra el text parlat i el tutorial bloqueja la prova) → ho ha de mirar Bernat al rellotge real. — `fc1c5e7`
- [ ] Rellotge real: bateria, latència, desconnexions, bloqueig i desbloqueig reals. **Pendent, no es pot fer sense rellotge físic** (no s'engega). Llista per quan es pugui:
  - Clau Keystore: la còpia és il·legible amb el rellotge bloquejat i la clau desapareix en treure el PIN (P10, `minSdk` 30).
  - Biometria real a l'aprovació d'una protegida, i dictat + nota a una protegida de punta a punta.
  - Català al reconeixedor de veu i «Sense mòbil» real; desconnexió i reconnexió amb notes a la cua.
  - Release R8: dictat i entrega d'una nota, i aprovació (no es poden exercitar a l'emulador).
  - TalkBack: què diu la fila d'una persona protegida («Persona protegida») i que no repeteix les inicials.
  - Bateria i latència de la sincronització; ambient (l'app hauria de desaparèixer, sense deixar contingut a la pantalla).
- [x] Release amb R8: Koin, serialització i Data Layer. **Trobat:** `:wear:assembleRelease` no compilava (`WearScaffolds.kt` usa `ui-tooling-preview` i només hi era a debug); arreglat. **Emulador** (parella release mòbil + rellotge signats amb la clau de debug, ids sense `.dev`): els dos arrenquen sense crashes, el mòbil sincronitza una persona i una entrada nova i el rellotge les mostra (Data Layer, serialització, DataStore i desxifrat amb Keystore sota R8). Al mapping hi són el worker, els serveis i els codecs de protocol. **No provat sota R8:** dictat i entrega d'una nota del rellotge al mòbil, aprovació d'una protegida (necessita biometria) → rellotge real/fase 10. Koin no s'usa al rellotge. — `e0ff1ac`
- [x] Permisos: només els imprescindibles (`RECORD_AUDIO`). Revisat amb `aapt2 dump permissions` sobre l'APK de release: l'únic permís propi és `RECORD_AUDIO`; la resta (`WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`) els fusiona WorkManager i es mantenen perquè la cua de notes es reprogrami en reiniciar el rellotge. Sense `INTERNET`, ubicació ni notificacions.
- [x] Comparar la implementació amb `docs/wear/screenshots/` i anotar les diferències. Comparades la llista de persones (01) i la cronologia (02) a l'emulador (Wear XL Round, en-US):
  - **Capçalera d'entrada retallada:** al disseny és «JO · 21:40 · XAT»; amb hora de 12 h i canal llarg es talla («MARTA · 6:07 PM · IN P…») i el canal es perd. Més visible amb lletra gran. **Decidit amb Bernat: es deixa així.**
  - El títol de debug porta «(D)» (només debug) i les files de la llista tenen l'efecte d'escala de la vora de la pantalla; són coherents amb el disseny.
  - No comparades en viu: dictat (03), Qui (04), Desat (05–06), aprovació (07–12) i les del mòbil (13–16); les cobreixen les captures i les proves d'emulador de les fases 6–8.

Validada per Bernat amb les proves de l'emulador i els tests. Queda pendent només el rellotge real (llista a la tasca «Rellotge real»): no es pot provar fins que el rellotge s'engegui, i la veu real de TalkBack i les captures de la fila protegida (etiqueta `update-screenshots` a la PR) es miren llavors.

## Fase 10 · CI i publicació

- [x] `pr-build.yml`: tests, ktlint i APK de `:wear`. `ktlintCheck` i `testDebugUnitTest` ja cobrien `:wear`; s'hi afegeixen `:wear:assembleDebug` i `:wear:assembleRelease` (també a `build-apk.yml`). — `3bf36dd`, `99df544`
- [x] `screenshot-tests.yml`: referències de `:wear` (etiqueta `update-screenshots`). Sense canvis: `validateDebugScreenshotTest` ja inclou `:wear`, el glob de referències és multimòdul i el script de l'informe també.
- [x] `release.yml`: AAB del rellotge i Gradle Play Publisher. `:wear` signa amb la keystore de release i aplica el plugin; el workflow construeix els dos AAB, els puja com a artefactes i publica `:app` i `:wear` en una sola invocació de Gradle. **No verificat:** que comparteixin edició i un AAB no esborri l'altre de la pista (no s'ha pogut fer ni un `--dry-run` de `publish*`); es comprova amb la primera release a `internal`. Si no, afegir `retain` amb el `versionCode` de l'altre. **Pendent:** el flux de la Play Console (form factor Wear OS). — `99df544`
- [ ] **Política de privacitat i «Sense núvol» (P8):** el text de la landing el canvia Bernat. La fitxa de Play ja diu «dispositiu i rellotge emparellat».
- [ ] Fitxa de Play: text per a Wear OS en en/ca/es i 4 captures 720×720 per idioma a `wear-screenshots` (`4f7cab2`, `4f6aa1a`, `d88c412`). **Pendent:** comprovar que GPP les puja i el formulari de seguretat de les dades.
- [ ] Pista inicial `internal`, després `beta`.
- [ ] Notes de versió: proposar el punt; no es toquen sense acord.

## Fase 11 · Més endavant

- Tile i complicació de nota ràpida (no dissenyats).
- Respostes predefinides, o triar el canal des del rellotge.
- Notificacions des del mòbil (sense contingut sensible).
- Rellotge autònom amb base de dades pròpia.
