# Converses i els contactes del dispositiu — Fases

Context, abast, arquitectura, decisions (C1–C8) i riscos són a `docs/contacts/README.md`. Cada fase acaba amb alguna cosa que es pot provar; no es comença la següent sense validar l'anterior.

| Fase | Títol | Depèn de | Estat |
| --- | --- | --- | --- |
| 0 | Decisions i investigació (Play, Contactes a l'emulador, disseny) | — | Pendent |
| 1 | Flag, migració v3 i repositori | 0 | Pendent |
| 2 | Vincular i desvincular a l'editor; vincle al detall | 1, 0 (C8) | Pendent |
| 3 | Compte, sync adapter i obrir des de Contactes | 1 | Pendent |
| 4 | Reconciliació, permisos i disparadors | 2, 3 | Pendent |
| 5 | Privacitat i casos límit | 4 | Pendent |
| 6 | Qualitat, textos, captures i Play | 5 | Pendent |
| 7 | Més endavant | 6 | Idees |

---

Com es treballa cada fase (tasques, commits, validació, proposta d'inici): `.claude/rules/phased-work.md`.

---

## Fase 0 · Decisions i investigació

Sense codi de producte. **Bloquejant:** si Play o les apps de Contactes no ho permeten com esperem, el pla s'atura aquí.

- [ ] Política de Play vigent per a `READ_CONTACTS` i `WRITE_CONTACTS` (permisos sensibles, declaració, Data safety). Dir què caldrà declarar i si afecta la versió gratuïta.
- [ ] Prova de concepte **fora de `main`** (branca d'usar i llençar): compte, sync adapter, `contacts.xml` i una fila enganxada a un contacte. Comprovar a l'emulador que es veu i s'obre a **l'app Contactes de l'AOSP i a Google Contacts**, i on surt (secció «Connected apps» o similar).
- [ ] Comprovar què passa amb el lookup key després de l'agregació, i si el nom mostrat del contacte canvia (no hauria: el nostre raw contact no té nom).
- [ ] Decidir C6 (compte esborrada per l'usuari) i C7 (quan i com es demanen els permisos).
- [ ] Decidir si es desa `contactName` a Room (mostrar el vincle sense `READ_CONTACTS`) o només `contactLookupKey`.
- [ ] Demanar el disseny a Claude Design (C8): fila «Contacte» a l'editor, indicació al detall, explicació del permís i estat «contacte esborrat».

**Sortida:** decisions escrites al `README.md`, la prova de concepte vista a les dues apps de Contactes i el disseny.

## Fase 1 · Flag, migració v3 i repositori

- [ ] `ContactLinkFeature` (`key = "contact_link_enabled"`, `pro = true`) a `data/flags`, registrat a Koin i al menú de debug.
- [ ] `PersonEntity`: `contactLookupKey` (i `contactName`, si la fase 0 ho decideix) nul·lables. `ConversesDatabase` a versió 3, `3.json` exportat, `Migration2To3` registrada a `di/AppModules.kt`.
- [ ] Test de migració 2 → 3 amb `MigrationTestHelper` (les persones i les entrades queden intactes; les columnes noves són `null`).
- [ ] `ConversesRepository`: vincular i desvincular; tests de JVM amb `FakeDb`.
- [ ] Comprovar que l'snapshot de Wear i l'exportació no inclouen el vincle (test).

**Validació:** `testDebugUnitTest`, test de migració (instrumentat, a l'emulador), `ktlintCheck`.
**Sortida:** la BBDD d'una v2 real s'actualitza sense perdre res; encara no es veu res a la UI.

## Fase 2 · Vincular a l'editor; vincle al detall

- [ ] Editor (Editar / Nova persona), només amb el flag: fila «Contacte» que obre `PickContact`, mostra el nom triat i permet treure'l.
- [ ] Detall: «Vinculada a {nom}» i «Obre a Contactes» (`ACTION_VIEW` amb el lookup URI); estat «contacte esborrat» amb opció de desvincular.
- [ ] ViewModels amb tests de JVM (flag apagat: res; vincular; desvincular; contacte esborrat).
- [ ] Textos en en/ca/es; previews i captures (`...Content`).

**Validació:** tests, `ktlintCheck`, captures revisades; emulador: vincular, obrir a Contactes, desvincular, i amb el flag apagat no es veu res.
**Sortida:** es pot vincular una persona i obrir el contacte; encara no hi ha fila a Contactes.

## Fase 3 · Compte, sync adapter i obrir des de Contactes

- [ ] Authenticator, sync adapter (que no sincronitza res), `syncadapter.xml`, `authenticator.xml`, `contacts.xml` i icona.
- [ ] Activitat d'entrada `ACTION_VIEW` amb el mimetype propi: llegeix `SYNC1` i navega a `PersonRoute` o `PersonLockedRoute`; si el flag està apagat o la persona no hi és, obre l'app normal.
- [ ] `ContactsGateway` (interfície) i la seva implementació Android; `FakeContactsGateway` a `app/src/test/.../testing`.
- [ ] Tests de JVM de la resolució de l'entrada (id vàlid, protegida, inexistent, flag apagat).

**Validació:** tests, `ktlintCheck`; emulador: amb una fila creada des del menú de debug, tocar-la a Contactes obre la persona.
**Sortida:** la cadena Contactes → Converses funciona amb una fila de prova.

## Fase 4 · Reconciliació, permisos i disparadors

- [ ] `ContactsReconciler`: pla pur (crear i esborrar) a partir de les persones, el flag, els permisos i les files existents; aplicat amb `ContactsGateway` en un sol lot.
- [ ] Tornar a llegir i desar el lookup key després d'agregar.
- [ ] Disparadors: arrencada, desar persona, protegir o desprotegir, esborrar, canvi de flag, permisos concedits. Amb el flag apagat: esborrar-ho tot i treure la compte.
- [ ] Demanar els permisos segons C7, amb l'explicació dissenyada; sense permisos, el vincle es desa igualment i no hi ha fila.
- [ ] Tests de JVM del pla: idempotència, protegides mai, arxivades sí, contacte esborrat, flag apagat, permisos denegats.

**Validació:** tests, `ktlintCheck`; emulador: el cicle complet (vincular → fila; protegir → desapareix; desprotegir → torna; esborrar persona → desapareix; apagar flag → no queda res ni la compte).
**Sortida:** la funció completa a l'emulador amb el flag encès.

## Fase 5 · Privacitat i casos límit

- [ ] Revisar tot el que s'escriu al proveïdor: només text fix i `SYNC1` (C3); cap nom ni contingut.
- [ ] Fila d'una persona que es protegeix mentre Contactes la mostra: tocar-la passa per `PersonLockedRoute`.
- [ ] Compte esborrada per l'usuari (C6), contacte esborrat o fusionat amb un altre, i dues persones vinculades al mateix contacte.
- [ ] Desinstal·lar i reinstal·lar: no queden files òrfenes.

**Validació:** tests de JVM dels casos; emulador; **humana:** desbloqueig biomètric en obrir una protegida des de Contactes.
**Sortida:** cap camí que filtri una protegida o dades de les notes.

## Fase 6 · Qualitat, textos, captures i Play

- [ ] TalkBack a la fila de l'editor i al detall (**humana**).
- [ ] Captures de referència actualitzades (etiqueta `update-screenshots` al PR).
- [ ] `docs/pro/README.md`: fila «Contact integration» actualitzada i proves marcades.
- [ ] Preparar el text de Data safety i la declaració de permisos; **no es toca Play sense permís.**
- [ ] Baseline Profile: no cal mentre sigui darrere de flag (es revisa a la release, com sempre).

**Sortida:** funció llesta per quan existeixi el Pro, apagada a la release.

## Fase 7 · Més endavant

- **Avís de trucada entrant** (`CallScreeningService`) per a persones vinculades: demana el rol de filtre de trucades i llegir telèfons del contacte; cal decidir-ne la privacitat (protegides).
- **Importar diversos contactes alhora** per crear persones: aquí es pot tornar a valorar contacts-android (C5).
