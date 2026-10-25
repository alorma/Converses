# Converses i els contactes del dispositiu — Fases

Context, abast, arquitectura, decisions (C1–C11) i riscos són a `docs/contacts/README.md`. Cada fase acaba amb alguna cosa que es pot provar; no es comença la següent sense validar l'anterior.

| Fase | Títol | Depèn de | Estat |
| --- | --- | --- | --- |
| 0 | Decisions i investigació (Play, Contactes a l'emulador, disseny) | — | Fet (25-10-2026) |
| 1 | Flag, migració v3 i repositori | 0 | Feta i validada (25-10-2026) |
| 2 | Vincular i desvincular a l'editor; vincle al detall | 1, 0 (C8) | Feta i validada (25-10-2026) |
| 3 | Compte, sync adapter i obrir des de Contactes | 1 | Feta i validada (25-10-2026) |
| 4 | Reconciliació, permisos, disparadors i Ajustos › Contactes | 2, 3 | Feta i validada (25-10-2026) |
| 5 | Privacitat i casos límit | 4 | Feta i validada (25-10-2026) |
| 6 | Qualitat, textos, captures i Play | 5 | Pendent |
| 7 | Més endavant | 6 | Idees |

---

Com es treballa cada fase (tasques, commits, validació, proposta d'inici): `.claude/rules/phased-work.md`.

---

## Fase 0 · Decisions i investigació

Sense codi de producte. Prova de concepte a la branca `poc/contacts-row` (no es fusiona mai); resultats a `docs/contacts/handoff.md` d'aquella branca i resumits al `README.md`.

- [x] Política de Play vigent per a `READ_CONTACTS` i `WRITE_CONTACTS` — vegeu «Política» al `README.md`
- [x] Prova de concepte: compte, sync adapter, `contacts.xml` i files enganxades a un contacte; vistes i obertes a Google Contacts a la 37.1 i a la 37.2 — `21af28b`, `de6318b` (a `poc/contacts-row`)
- [x] Lookup key després de l'agregació: canvia, però l'antic continua resolent; el nom del contacte no canvia
- [x] People Directory (37.2) provat i descartat (C10)
- [x] Decidir C6, C7, C9, C10 i, en part, C11
- [x] Disseny de Claude Design (C8): lliurat el 25 d'oct., a `docs/contacts/disseny.md`
- [ ] Neteja: treure telèfons, correus i l'empresa «Converses» dels contactes de prova (compte de Google de Bernat); `clear` de la prova de concepte als dos emuladors; esborrar el worktree `../Converses-poc-contacts` quan ja no calgui

## Fase 1 · Flag, migració v3 i repositori

- [x] Decidir si es desa `contactName` (C11): sí
- [x] `ContactLinkFeature` (`key = "contact_link_enabled"`, `pro = true`) a `data/flags`, registrat a Koin (el menú de debug el llista sol) — `30d4f14`
- [x] `PersonEntity`: `contactId`, `contactLookupKey` i `contactName` nul·lables. `ConversesDatabase` a versió 3, `3.json` exportat, `Migration2To3` registrada a `di/AppModules.kt` — `c30b2dc`
- [x] Test de migració 2 → 3 i 1 → 3 amb `MigrationTestHelper` (persones, entrades i fets intactes; columnes noves `null`), passat a l'emulador 37.2 — `c30b2dc`
- [x] `ConversesRepository`: `linkContact` (també per refrescar el lookup key) i `unlinkContact`; tests de JVM amb `FakeDb` — `4bc819d`
- [x] El vincle no arriba a l'snapshot de Wear (test amb l'snapshot codificat, també per a protegides) — `197c5c7`. Auto encara no existeix. L'exportació (`PersonExporter`) escriu camp a camp i no inclou el vincle (revisat al codi; no té test de JVM perquè necessita `Context`).

**Validació:** `testDebugUnitTest`, test de migració (instrumentat, a l'emulador), `ktlintCheck`.
**Sortida:** la BBDD d'una v2 real s'actualitza sense perdre res; encara no es veu res a la UI.

## Fase 2 · Vincular a l'editor; vincle al detall

Segons `docs/contacts/disseny.md`. Decidit en començar (25-10-2026): el permís i l'explicació 5b es fan en aquesta fase, no a la 4, perquè l'editor no té sentit sense; a una persona existent, vincular i desvincular es desen a l'acte (com «Protegida»); a Nova persona, amb «Desa».

- [x] Desar la persona a l'editor conserva el vincle (`save()` no copiava les columnes del contacte) — `40ead27`
- [x] Estat del permís de Contactes darrere d'una interfície (fake als tests) i «explicació ja vista» a DataStore — `194d4b0`
- [x] `EditPersonViewModel`: estat de la secció Contacte (flag apagat, sense vincle, vinculat, sense permís), vincular amb el contacte triat i desvincular; tests de JVM — `1cc56a3`
- [x] Editor (Editar / Nova persona), només amb el flag: secció «Contacte» amb els tres estats, `PickContact`, «Dona permís» (ajustos de l'app si ja no es pot preguntar) i l'explicació 5b; textos en/ca/es, previews i captures — `b32009b`, `8c330f2`. Provat a l'emulador 37.2: 5b la primera vegada («Ara no» la torna a mostrar), rebutjar → «Sense permís», «Dona permís» torna a preguntar i després obre els ajustos de l'app, el permís donat des dels ajustos es nota en tornar, vincular i desvincular es desen a l'acte (també girant la pantalla), Nova persona desa el vincle amb «Desa», i amb el flag apagat no es veu res
- [x] `PersonDetailViewModel`: vincle resolt en obrir (vinculada, contacte esborrat; res sense permís ni amb el flag apagat), refresc del lookup key i el nom, «Desvincula»; tests de JVM amb `FakeContactsGateway` — `cb34439`
- [x] Detall: «Vinculada a {nom}» i «Obre a Contactes» (`ACTION_VIEW` amb el lookup URI); targeta «contacte esborrat» amb «Desvincula»; textos, previews i captures — `91b4290`
- [x] Validació a l'emulador 37.2: el vincle fet a l'editor surt al detall en tornar; la fila obre la fitxa a Google Contacts; amb un contacte local esborrat (creat i esborrat amb `adb shell content`, no al compte de Google) surt la targeta i «Desvincula» treu el vincle; amb el flag apagat no surt res i el vincle es conserva. La persona protegida bloquejada no s'ha provat: `PersonLockedScreen` no carrega res del vincle

**On s'ha quedat (25-10-2026):** fase validada per Bernat; la següent és la 4 (les files a Contactes arriben amb el reconciliador, no en vincular). La UI de l'editor és a `ui/edit/ContactSection.kt` (fila, full 5b i `openAppDetailsSettings()`, reutilitzable per a Ajustos › Contactes) i la del detall a `ui/person/DetailContactRow.kt`. El que ja hi ha:

- `ContactsPermission` (`isGranted()`, `permissions` = READ i WRITE) i `AppSettings.contactsExplained` amb `setContactsExplained`.
- `ContactsGateway.readPicked(uri)` llegeix el contacte de `PickContact` sense permís.
- `EditPersonUiState.contact` (`ContactLinkState`): `section` (`Hidden`, `NotLinked`, `Linked(name)`, `NoPermission`) i `linkStep` (`Explain` → 5b, `RequestPermission` → diàleg del sistema, `Pick` → selector). `NoPermission` = explicació vista i permís no donat. Amb vincle es mostra `Linked` encara que no hi hagi permís, perquè desvincular no en necessita.
- Accions a `EditPersonActions` (la UI encara no les crida): `onContactPicked(uri)`, `onUnlinkContact()`, `onContactsExplained()` («Continua» a la 5b) i `onContactsPermissionChanged()` (cridar-la després del diàleg i a `ON_RESUME`).
- Per a la UI: «Dona permís» obre els ajustos de l'app si l'explicació ja s'ha vist i `shouldShowRequestPermissionRationale` és fals; si no, torna a demanar el permís. «Ara no» a la 5b no marca l'explicació com a vista.

**Validació:** tests, `ktlintCheck`, captures revisades; emulador: vincular (Editar i Nova persona), obrir a Contactes, contacte esborrat i desvincular, sense permís, i amb el flag apagat o la persona bloquejada no es veu res.
**Sortida:** es pot vincular una persona i obrir el contacte; encara no hi ha files a Contactes.

## Fase 3 · Compte, sync adapter i obrir des de Contactes

- [x] `PersonRoute` i `PersonLockedRoute` amb `openNewEntry` (C9); el full s'obre una sola vegada (provat girant la pantalla) — `6921882`
- [x] Resolució pura de la fila (cada mimetype, protegida, arxivada, inexistent, flag apagat); mimetypes amb l'`applicationId` perquè debug i release no comparteixin files — `ad14faa`
- [x] Authenticator, sync adapter (que no sincronitza res), `contacts_authenticator.xml`, `contacts_syncadapter.xml` i `contacts.xml` (còpia a `src/debug` amb els mimetypes de `.dev`); tipus de compte per build type (`contacts_account_type`); `ContactsGateway`, `AndroidContactsGateway` i `FakeContactsGateway`. Permisos només al manifest de debug (C12, comprovat a l'APK de release) — `a25e1a6`
- [x] `ContactRowActivity` (transparent, sense historial) passa només dades a `MainActivity`, que torna a resoldre la ruta: un intent falsificat no se salta el bloqueig — `d58b841`
- [x] Cel·la al menú de debug: enllaçar el contacte triat amb la persona del mateix nom, i treure totes les files i la compte — `fe48539`

Validat a l'emulador 37.2 (Google Contacts): «View notes» obre la fitxa dins la tasca de Contactes (enrere torna a Contactes); «Create note» obre la fitxa amb el full; flag apagat → llista de persones; intent falsificat amb una protegida → «Unlock Luna Roja» i, després del PIN, la fitxa amb el full obert; Room desa el lookup key nou; «Remove contact rows» ho deixa tot net.

**Validació:** tests, `ktlintCheck`; emulador: amb files creades des del menú de debug, «Veure notes» obre la fitxa i «Crear nota» la fitxa amb el full obert.
**Sortida:** la cadena Contactes → Converses funciona amb files de prova.

## Fase 4 · Reconciliació, permisos, disparadors i Ajustos › Contactes

Segons `docs/contacts/disseny.md` (Ajustos 5h; l'explicació del permís 5b ja es fa a la fase 2 i aquí només es reutilitza per a «Activa»).

Decidit en començar (25-10-2026): el comptador d'Ajustos inclou protegides i arxivades; el contacte esborrat es tracta a la fase 5 (el reconciliador ja n'esborra les files òrfenes).

- [x] `ContactsGateway.rows()` torna també el contacte on està enganxada cada fila (`RawContacts.CONTACT_ID`) i el text de les files; `rowLabels()` dona el text de l'idioma del moment — `314a23c`
- [x] `ContactsPlan`: pla pur (compte, crear i esborrar) a partir de les persones vinculades, el flag, el permís, la compte i les files; 14 tests de JVM (protegides mai, arxivades sí, compte esborrada, idioma o contacte canviats, flag apagat, sense permís) — `7e33cfc`
- [x] `ContactsReconciler` (com `WearSnapshotPublisher`): escolta les persones vinculades (desar, protegir, esborrar, vincular), el flag i `refresh()` a `MainActivity.onResume` (permís, compte esborrada, idioma); desa el lookup key nou amb un `UPDATE` que no trepitja un vincle canviat entremig; sense permís treu la compte (el sistema n'esborra les files) i conserva els vincles; si el contacte ja no hi és, esborra les files. Cada persona va en el seu lot (`addRows`), no tot en un. 10 tests de JVM amb `FakeContactsGateway`, inclosa la idempotència — `974c6d9`
- [x] Ajustos › Contactes, només amb el flag i sense etiqueta PRO: mai activat («Activa» → 5b → permís), activat (comptador amb plurals, «Compte "Converses"» obre Ajustos › Comptes, «Gestiona el permís») i denegat («Dona permís»); textos, previews i captures — `9b215c5`

Validat a l'emulador 37.2 (25-10-2026): obrir l'app crea la compte i les files i tornar-hi no les refà; idioma de l'app a català → «Veure notes» / «Crear nota»; protegir → desapareixen, desprotegir → tornen; esborrar la compte a Ajustos › Comptes → tornen en obrir l'app; retirar el permís → ni compte ni files, el vincle es conserva i Ajustos diu «Sense permís»; «Dona permís» obre els ajustos de l'app i en tornar amb el permís les files tornen i Ajustos diu «Activat · 1 persona vinculada»; flag apagat → no queda ni la compte, encès → tornen; desvincular l'última persona → fora files i compte; vincular amb el selector → files a l'acte. No provat a l'emulador: l'estat «mai activat» i «Activa» (cal esborrar `contactsExplained`; la captura i el flux 5b de l'editor ho cobreixen).

**Validació:** tests, `ktlintCheck`; emulador: el cicle complet (vincular → files; protegir → desapareixen; desprotegir → tornen; esborrar persona → desapareixen; esborrar la compte a Ajustos → tornen en obrir l'app; apagar flag → no queda res ni la compte).
**Sortida:** la funció completa a l'emulador amb el flag encès.

## Fase 5 · Privacitat i casos límit

Decidit en començar (25-10-2026): C13 (un contacte, una persona) i C14 (contactes fusionats). Afegit durant la fase: treure d'Ajustos › Contactes les files de la compte i del permís.

- [x] Revisar tot el que s'escriu al proveïdor (emulador, `content query`): el raw contact no té nom (només `SYNC1` = id) i les files porten `data1` = id, `data2` = nom de l'app i `data3` = text fix. El nom que s'hi veu és el del contacte agregat. Sense canvis de codi
- [x] Files d'una persona que es protegeix mentre Contactes la mostra: la fila vella ja no existeix i obre l'app normal; amb un intent que encara porta la persona, `MainActivity` la torna a llegir i passa per `PersonLockedRoute` («Unlock Pau Vidal» → PIN → fitxa amb el full obert). Cobert per `ContactRowTest`. Sense canvis de codi
- [x] Contacte esborrat: el proveïdor en conserva l'id amb només les nostres files (un contacte sense nom a Contactes) i el lookup key encara resolia. Ara `rows()` marca les files soles, el pla les refà (i s'esborren) i `resolve` el dona per esborrat; el detall mostra «Aquest contacte ja no existeix» — `aebba2a`
- [x] Contacte fusionat: les files segueixen la persona i Room desa l'id i el lookup key nous (sense canvis). Dues persones al mateix contacte després d'una fusió: només en té files la primera (C14) — `13253bb`
- [x] Editor: no es pot vincular un contacte que ja és d'una altra persona (C13); text en/ca/es, preview i captura — `3193c5b`
- [x] Ajustos › Contactes: fora «Compte "Converses"» i «Gestiona el permís»; ara, activat, és una sola fila — `b7e4a28`
- [x] Desinstal·lar i reinstal·lar: no queden ni la compte, ni raw contacts, ni contactes sense nom. Sense canvis de codi

Validat a l'emulador 37.2 amb contactes locals (creats i esborrats amb `adb shell content`) i un PIN temporal. Validat per Bernat (25-10-2026): el desbloqueig en obrir una protegida des d'una fila (Luna Roja → «Crear nota») obre la fitxa amb el full; una protegida vinculada no té files a Contactes (C1).

**Validació:** tests de JVM dels casos; emulador; **humana:** desbloqueig biomètric en obrir una protegida des de Contactes.
**Sortida:** cap camí que filtri una protegida o dades de les notes.

## Fase 6 · Qualitat, textos, captures i Play

- [x] Google Contacts i la memòria cau dels tipus de compte: documentat com a limitació a «Riscos» del `README.md`. Provat a la 37.2: si Contactes s'obre mentre Converses no hi és i el procés continua viu, després d'instal·lar les files són al proveïdor però no surten. No ho arregla ni esperar, ni girar la pantalla, ni tornar a entrar, ni refer la compte (retirar i tornar a donar el permís), ni `install -r`; només que el procés es mori. Si Contactes ja coneixia el tipus de compte, surten de seguida. Sense canvis de codi — `fa17c9d`
- [x] Captures de referència: `validateDebugScreenshotTest` passa i cada estat (editor, 5b, detall, Ajustos, «ja vinculat») té referència clara i fosca. No ha calgut regenerar-ne cap
- [ ] TalkBack a la fila de l'editor i al detall. Al codi: els textos de les files no clicables (editor vinculat, «Sense permís», contacte esborrat i els punts de la 5b) es llegeixen com un sol element, i el títol de la 5b és un encapçalament. Les files clicables ja s'agrupaven — `c71f6de`. Falta la validació **humana** amb TalkBack
- [x] `maxSdkVersion` no eximeix de la declaració de `READ_CONTACTS` (la política és per a targetSdk 37 i el permís es demana a la 37). Esborrany de la declaració i de Data safety (sense canvis) a «Política» del `README.md`. No s'ha tocat Play
- [x] `docs/pro/README.md`: fila «Contact integration» actualitzada, què falta per obrir-ho al Pro i proves marcades
- [x] Baseline Profile: no cal mentre sigui darrere de flag; regenerar-la quan s'obri al Pro (apuntat a `docs/pro/README.md`)

**Sortida:** funció llesta per quan existeixi el Pro, apagada a la release.

## Fase 7 · Més endavant

- **Revisar C10 amb l'API 38:** si `WRITE_CONTACTS` deixa de funcionar, mirar si Google Contacts o Telèfon ja fan servir el People Directory, o quina alternativa hi ha.
- **Avís de trucada entrant** (`CallScreeningService`) per a persones vinculades: demana el rol de filtre de trucades i llegir telèfons del contacte; cal decidir-ne la privacitat (protegides).
- **Importar diversos contactes alhora** per crear persones: aquí es pot tornar a valorar contacts-android (C5).
