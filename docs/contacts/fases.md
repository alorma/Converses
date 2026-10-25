# Converses i els contactes del dispositiu — Fases

Context, abast, arquitectura, decisions (C1–C11) i riscos són a `docs/contacts/README.md`. Cada fase acaba amb alguna cosa que es pot provar; no es comença la següent sense validar l'anterior.

| Fase | Títol | Depèn de | Estat |
| --- | --- | --- | --- |
| 0 | Decisions i investigació (Play, Contactes a l'emulador, disseny) | — | Fet (25-10-2026), disseny pendent |
| 1 | Flag, migració v3 i repositori | 0 | Feta, pendent de validar |
| 2 | Vincular i desvincular a l'editor; vincle al detall | 1, 0 (C8) | Pendent (espera disseny) |
| 3 | Compte, sync adapter i obrir des de Contactes | 1 | Pendent |
| 4 | Reconciliació, permisos i disparadors | 2, 3 | Pendent (espera disseny) |
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
- [ ] Demanar el disseny a Claude Design (C8): encàrrec escrit al `README.md`, el passa Bernat
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

Necessita el disseny (C8).

- [ ] Editor (Editar / Nova persona), només amb el flag: fila «Contacte» que obre `PickContact`, mostra el nom triat i permet treure'l.
- [ ] Detall: «Vinculada a {nom}» i «Obre a Contactes» (`ACTION_VIEW` amb el lookup URI); estat «contacte esborrat» amb opció de desvincular.
- [ ] ViewModels amb tests de JVM (flag apagat: res; vincular; desvincular; contacte esborrat).
- [ ] Textos en en/ca/es; previews i captures (`...Content`).

**Validació:** tests, `ktlintCheck`, captures revisades; emulador: vincular, obrir a Contactes, desvincular, i amb el flag apagat no es veu res.
**Sortida:** es pot vincular una persona i obrir el contacte; encara no hi ha files a Contactes.

## Fase 3 · Compte, sync adapter i obrir des de Contactes

- [ ] Authenticator, sync adapter (que no sincronitza res), `syncadapter.xml`, `authenticator.xml` i `contacts.xml` amb els dos mimetypes; tipus de compte diferent a debug i a release.
- [ ] `PersonRoute` amb un paràmetre per obrir el `NewEntrySheet` d'entrada (C9).
- [ ] Activitat d'entrada `ACTION_VIEW` per als dos mimetypes: llegeix el `personId` de la fila i navega segons C9 (fitxa, o fitxa amb el full obert); `PersonLockedRoute` si és protegida; l'app normal si el flag està apagat o la persona no hi és.
- [ ] `ContactsGateway` (interfície) i la seva implementació Android; `FakeContactsGateway` a `app/src/test/.../testing`.
- [ ] Tests de JVM de la resolució de l'entrada (cada mimetype, protegida, inexistent, flag apagat).

**Validació:** tests, `ktlintCheck`; emulador: amb files creades des del menú de debug, «Veure notes» obre la fitxa i «Crear nota» la fitxa amb el full obert.
**Sortida:** la cadena Contactes → Converses funciona amb files de prova.

## Fase 4 · Reconciliació, permisos i disparadors

Necessita el disseny de l'explicació del permís (C8).

- [ ] `ContactsReconciler`: pla pur (crear i esborrar) a partir de les persones, el flag, els permisos i les files existents; aplicat amb `ContactsGateway` en un sol lot.
- [ ] Tornar a llegir i desar el lookup key després d'agregar.
- [ ] Disparadors: arrencada (tornant a crear la compte si l'usuari l'ha esborrada, C6), desar persona, protegir o desprotegir, esborrar, canvi de flag, permisos concedits. Amb el flag apagat: esborrar-ho tot i treure la compte.
- [ ] Demanar els permisos segons C7, amb l'explicació dissenyada; sense permisos, el vincle es desa igualment i no hi ha files.
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
