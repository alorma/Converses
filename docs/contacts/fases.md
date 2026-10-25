# Converses i els contactes del dispositiu — Fases

Context, abast, arquitectura, decisions (C1–C11) i riscos són a `docs/contacts/README.md`. Cada fase acaba amb alguna cosa que es pot provar; no es comença la següent sense validar l'anterior.

| Fase | Títol | Depèn de | Estat |
| --- | --- | --- | --- |
| 0 | Decisions i investigació (Play, Contactes a l'emulador, disseny) | — | Fet (25-10-2026) |
| 1 | Flag, migració v3 i repositori | 0 | Feta i validada (25-10-2026) |
| 2 | Vincular i desvincular a l'editor; vincle al detall | 1, 0 (C8) | Pendent |
| 3 | Compte, sync adapter i obrir des de Contactes | 1 | Feta i validada (25-10-2026) |
| 4 | Reconciliació, permisos, disparadors i Ajustos › Contactes | 2, 3 | Pendent |
| 5 | Privacitat i casos límit | 4 | Pendent |
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
- [ ] Editor (Editar / Nova persona), només amb el flag: secció «Contacte» amb els tres estats, `PickContact`, «Dona permís» (ajustos de l'app si ja no es pot preguntar) i l'explicació 5b; textos en/ca/es, previews i captures
- [ ] `PersonDetailViewModel`: vincle resolt en obrir (vinculada, contacte esborrat; res sense permís ni amb el flag apagat), refresc del lookup key i el nom, «Desvincula»; tests de JVM amb `FakeContactsGateway`
- [ ] Detall: «Vinculada a {nom}» i «Obre a Contactes» (`ACTION_VIEW` amb el lookup URI); targeta «contacte esborrat» amb «Desvincula»; textos, previews i captures
- [ ] Validació a l'emulador 37.2

**On s'ha quedat (25-10-2026, per a la sessió següent):** fetes les tasques 1–3, continuar per la UI de l'editor. El que ja hi ha:

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

- [ ] Ajustos › Contactes, només amb el flag i sense etiqueta PRO: mai activat («Activa»), activat (comptador amb plurals, compte, gestionar el permís) i denegat.

- [ ] `ContactsReconciler`: pla pur (crear i esborrar) a partir de les persones, el flag, els permisos i les files existents; aplicat amb `ContactsGateway` en un sol lot.
- [ ] Tornar a llegir i desar el lookup key després d'agregar (`addRows` ja el torna; falta fer-ho des del reconciliador).
- [ ] El text de les files s'escriu a l'idioma del moment: refer-les quan canviï l'idioma de l'app o del sistema.
- [ ] Disparadors: arrencada (tornant a crear la compte si l'usuari l'ha esborrada, C6), desar persona, protegir o desprotegir, esborrar, canvi de flag, permisos concedits. Amb el flag apagat: esborrar-ho tot i treure la compte.
- [ ] Permisos concedits o retirats fora de l'editor (Ajustos de l'app, «Activa»): les files segueixen l'estat; sense permisos, els vincles es conserven sense files.
- [ ] Tests de JVM del pla: idempotència, protegides mai, arxivades sí, contacte esborrat, compte esborrada, flag apagat, permisos denegats.

**Validació:** tests, `ktlintCheck`; emulador: el cicle complet (vincular → files; protegir → desapareixen; desprotegir → tornen; esborrar persona → desapareixen; esborrar la compte a Ajustos → tornen en obrir l'app; apagar flag → no queda res ni la compte).
**Sortida:** la funció completa a l'emulador amb el flag encès.

## Fase 5 · Privacitat i casos límit

- [ ] Revisar tot el que s'escriu al proveïdor: només text fix i l'id de la persona (C3); cap nom ni contingut.
- [ ] Files d'una persona que es protegeix mentre Contactes la mostra: tocar-les passa per `PersonLockedRoute`.
- [ ] Contacte esborrat o fusionat amb un altre, i dues persones vinculades al mateix contacte.
- [ ] Desinstal·lar i reinstal·lar: no queden files òrfenes.

**Validació:** tests de JVM dels casos; emulador; **humana:** desbloqueig biomètric en obrir una protegida des de Contactes.
**Sortida:** cap camí que filtri una protegida o dades de les notes.

## Fase 6 · Qualitat, textos, captures i Play

- [ ] TalkBack a la fila de l'editor i al detall (**humana**).
- [ ] Captures de referència actualitzades (etiqueta `update-screenshots` al PR).
- [ ] `docs/pro/README.md`: fila «Contact integration» actualitzada i proves marcades.
- [ ] Verificar si `maxSdkVersion` eximeix de la declaració de `READ_CONTACTS`; preparar el text de Data safety i la declaració de permisos. **No es toca Play sense permís.**
- [ ] Baseline Profile: no cal mentre sigui darrere de flag (es revisa a la release, com sempre).

**Sortida:** funció llesta per quan existeixi el Pro, apagada a la release.

## Fase 7 · Més endavant

- **Revisar C10 amb l'API 38:** si `WRITE_CONTACTS` deixa de funcionar, mirar si Google Contacts o Telèfon ja fan servir el People Directory, o quina alternativa hi ha.
- **Avís de trucada entrant** (`CallScreeningService`) per a persones vinculades: demana el rol de filtre de trucades i llegir telèfons del contacte; cal decidir-ne la privacitat (protegides).
- **Importar diversos contactes alhora** per crear persones: aquí es pot tornar a valorar contacts-android (C5).
