# Sincronització, fase 6: compte. Pla d'implementació

> **Per a qui l'executi amb agents:** primer la base comuna (tasca 0), en sèrie. Després les tasques A i B en paral·lel, cadascuna al seu worktree sobre `sync-fase-6`. Cada tasca només toca els fitxers que té assignats. La D va al final, sobre les dues.

**Objectiu:** que un compte es pugui esborrar des de l'app i que, sense les paraules, un dispositiu que té les dades pugui començar de nou amb paraules noves. Una pàgina web explica com demanar l'esborrat sense l'app.

**Disseny:** `docs/sync/disseny.md` (seccions «Com es veu per a l'usuari», «Compte», «Motor» i «Privacitat externa») i `docs/sync/disseny/INSTRUCCIONS.md` (taulers 13i, 13n, 13o, 13o2 i 13q, i els textos `no_words`, `nw_*`, `restart`, `delete`, `delete_s`, `c3_*`, `c4_*` i `del_offline`). Tot continua darrere de `SyncFlag`.

## Què hi entra i què no

| Hi entra | Queda per a |
| --- | --- |
| «Esborra el compte» a 13o i a 13o2, amb la confirmació (13q) | |
| «No tinc les paraules» a 13i i la pantalla 13n | El QR de la targeta 1: sense data **(B, 12 de novembre de 2026)** |
| «Comença de nou» des de 13n, amb la confirmació (13q) i paraules noves | |
| Els altres dispositius veuen que el compte s'ha esborrat o que la clau ha canviat | |
| Arreglar que activar un compte només pugi el que era pendent | |
| El text de la pàgina web d'esborrat, a aquest pla | Publicar-la a ConversesLanding: fase 7, amb la política de privacitat **(B, 13 de novembre de 2026)** |
| Com esborra Bernat un compte a mà, a `disseny.md` | |

## Privacitat externa

No surt cap dada nova i no hi ha cap permís nou. Aquesta fase **esborra** dades del núvol (registres, document del compte i usuari d'Auth) i en torna a pujar les que ja s'hi pujaven. Al formulari de Play, a la fase 7, la resposta «Els usuaris poden demanar que s'esborrin les dades» passa a ser «sí», amb l'adreça de la pàgina web. La política de privacitat ha de dir com s'esborra el compte (també a la fase 7).

## Decisions preses en aquest pla

- **(B, 13 de novembre de 2026)** Els **altres dispositius**, quan el compte s'ha esborrat, **tanquen la sessió sols**, sense cap avís: les dades es queden i l'indicador passa a «sense sessió». És una excepció acceptada a «Que un dispositiu deixi de sincronitzar en silenci»: el compte ja no existeix i ho ha decidit l'usuari mateix.
- **(B, 13 de novembre de 2026)** La pàgina web es prepara ara (text a sota) i es publica a la fase 7.
- **(C)** **Esborrar el compte demana sempre el full «Continua amb Google»**, just després de confirmar i abans d'esborrar res. Firebase només deixa esborrar un usuari d'Auth si la sessió s'ha iniciat fa poc, i així no pot passar que el núvol quedi buit amb el correu encara a Firebase. Tancar el full no esborra res. Si s'hi tria un compte diferent, no s'esborra res i surt un snackbar (text nou).
- **(C)** Esborrar el compte, en ordre:
  1. Sense connexió, no comença: snackbar `del_offline`. Ho diu el primer pas que falla, no cap comprovació de xarxa a part.
  2. El full de Google (reautenticar amb el mateix `uid`).
  3. `devices` es buida: des d'aquí les regles rebutgen qualsevol registre que escriguin els altres dispositius.
  4. Els registres s'esborren per lots de 200.
  5. Es treu el document `users/{uid}`.
  6. Es treu l'usuari d'Auth.
  7. Al dispositiu: es tanca la sessió com sempre i totes les files queden pendents (vegeu més avall). Les dades locals no es toquen.

  Si falla a mig camí (connexió, temps d'espera), surt un error i el dispositiu queda com estigués. Si `devices` ja era buit, la sessió següent el veurà com a `Removed`, i des de 13o2 es pot tornar a esborrar. Tots els passos es poden repetir sense efectes. Si l'últim pas falla, el núvol ja és buit: el dispositiu tanca la sessió igualment i l'usuari d'Auth, sense dades, el treu Bernat a mà quan l'hi demanin.
- **(C)** Esborrar el compte surt a 13o (`Ready`) i a 13o2 (`Removed`). A `NeedsKey` no surt: sense les paraules queda «Comença de nou» (si hi ha dades) o la pàgina web.
- **(C)** **Com sap un altre dispositiu que el compte ja no hi és:** el listener de `users/{uid}` veu que el document no existeix i ho confirma llegint-lo del servidor (la mateixa precaució que amb `Removed`). També ho veu `registerDevice` en arrencar, i qualsevol crida que torni que l'usuari d'Auth ja no existeix. En tots els casos, es tanca la sessió com si l'usuari ho hagués tocat i totes les files queden pendents.
- **(C)** **«No tinc les paraules» (13n)** s'obre des del peu de 13i, com un formulari (diàleg de 560 dp en pantalla gran). Sense QR, la targeta 1 no té botó i el text perd el QR: «Obre-hi Converses › Sincronització › Paraules de seguretat per veure-les.» (text nou en tres idiomes). La targeta 2 només surt si aquest dispositiu té alguna persona. La 3, sense botó, com al disseny.
- **(C)** **«Comença de nou»**, en ordre:
  1. Confirmació `c4` (13q).
  2. Es generen paraules noves i es mostren com a l'activació (13e empremta, 13f paraules, «Les he guardat» i «Continua»), amb `FLAG_SECURE`. Fins a «Continua», al núvol no ha canviat res. Tancar abans torna a 13i, **no** tanca la sessió.
  3. «Continua» fa una sola transacció a `users/{uid}`: la clau embolcallada nova, `format`, `devices` amb només aquest dispositiu (amb el nom xifrat amb la clau nova) i `plan` segons aquest dispositiu. Des d'aquí, els altres dispositius no poden escriure.
  4. La clau es desa al dispositiu i s'aixeca una marca local, `restartPending` (DataStore `sync`).
  5. S'esborren tots els registres per lots de 200: estan xifrats amb la clau vella i ja no els pot llegir ningú.
  6. Totes les files queden pendents, els tombstones es llencen (al núvol no hi ha res per esborrar) i el cursor torna a zero.
  7. Pujada inicial (13g) i «Sincronització activada» (13h). Després s'abaixa la marca.

  Si el procés mor després del pas 3, en arrencar amb `restartPending` es repeteixen els passos 5 a 7. Repetir-los no fa cap mal.
- **(C)** Mentre es mostren les paraules noves, l'estat és `SyncState.Activating(account, replacing = true)`. El `cancelActivation` d'aquest cas torna a `NeedsKey` sense tancar la sessió.
- **(C)** **Com sap un altre dispositiu que la clau ha canviat:** `RemoteDevices` porta també `wrappedKey` i `format`. Abans de mirar si aquest dispositiu és a `devices`, el repositori comprova que la clau del document s'obre amb el secret desat. Si no s'obre, el dispositiu llença la clau local, el cursor i la marca `registered`, deixa totes les files pendents i passa a `NeedsKey`. Ho comprova al listener, a `registerDevice` i a `rejoin()`, perquè «Torna a entrar» des de `Removed` no pugi mai res xifrat amb la clau vella. Així, «Comença de nou» no es veu als altres com un `Removed` sinó com «falten les paraules».
- **(C)** **Totes les files pendents en enllaçar amb un núvol nou.** Avui, activar un compte només puja el que era `dirty`. Un dispositiu que havia sincronitzat, tanca la sessió i activa un altre compte (o el mateix després d'esborrar-lo) no hi puja res del que ja s'havia pujat abans. Es marquen totes les files com a pendents (sense tocar-ne `modifiedAt` ni `updatedAt`) en aquests casos:
  - en activar (`Entry.Activated`), llençant també els tombstones;
  - en «Comença de nou»;
  - en tancar la sessió perquè el compte s'ha esborrat, aquí o en un altre dispositiu;
  - quan la clau ha canviat;
  - abans de la fusió (13m), perquè «No se'n perd cap» sigui veritat també per a les files que ja s'havien pujat a una altra clau. La baixada va primer, com sempre, i les files que perden contra el núvol queden netes.
- **(C)** Cap pas de l'esborrat ni de «Comença de nou» surt als logs amb el compte, el correu ni noms. Els errors van a `trackError` amb un lloc nou, `sync_delete`, sense `message`.

## Contracte (tasca 0)

`data/sync`:

| Peça | Què és |
| --- | --- |
| `AuthGateway.reauthenticate(activity, uid): ReauthResult` | El full de Google i `FirebaseUser.reauthenticate`. `ReauthResult`: `Done`, `Cancelled`, `OtherAccount`, `Offline`, `Failed` |
| `AuthGateway.deleteUser(): RemoteResult<Unit>` | `FirebaseUser.delete()` |
| `AccountRemote.replace(uid, account, device: RemoteDevice): RemoteResult<Unit>` | La transacció de «Comença de nou»: clau, `format`, `devices` amb només aquest dispositiu i `plan` |
| `AccountRemote.delete(uid): RemoteResult<Unit>` | Treu `users/{uid}` |
| `SyncRemote.clearDevices(uid)`, `SyncRemote.deleteAllRecords(uid)` | `devices` buit; tots els registres esborrats per lots de 200 |
| `RemoteDevices.wrappedKey`, `RemoteDevices.format` | Per veure si la clau ha canviat. `loadDevices` torna `Ok(null)` i `observeDevices` emet `null` si el document no existeix |
| `RemoteResult.Gone` | L'usuari d'Auth ja no existeix (`FirebaseAuthInvalidUserException`, o Firestore `UNAUTHENTICATED` amb la sessió que l'app creia vàlida) |
| `LocalSyncSource.markAllDirty()`, `LocalSyncSource.dropTombstones()`, `LocalSyncSource.hasPeople()` | Consultes noves als DAOs, sense canvis d'esquema: `UPDATE … SET dirty = 1`, sense tocar les dates |
| `SyncLocalStore.restartPending` | La marca de «Comença de nou» (DataStore `sync`) |
| `SyncState.Activating(account, replacing)` | `replacing = false` per defecte |
| `SyncRepository` | `deleteAccount(activity): DeleteOutcome` (`Done`, `Cancelled`, `OtherAccount`, `Offline`, `Failed`), `canStartOver: Flow<Boolean>`, `startOver(): StartOverOutcome` (`Started`, `Offline`, `Failed`), i stubs a la resta |

Falsos: `FakeSync` (les quatre portes) guanya l'usuari d'Auth que es pot esborrar i reautenticar, el document que pot desaparèixer o canviar de clau des d'«un altre dispositiu», i els registres que es poden comptar després d'esborrar-los.

Textos nous a la tasca 0, en els tres idiomes, amb el prefix `sync_`: `no_words`, `nw_1t`, `nw_1s` (sense el QR, vegeu més amunt), `nw_2t`, `nw_2s`, `restart`, `nw_3t`, `nw_3s`, `delete`, `delete_s`, `c3_t`, `c3_b`, `c3_go`, `c4_t`, `c4_b`, `del_offline`, l'error d'esborrar a mitges, l'error de compte diferent i el títol de 13n. Els que no són a la taula de `INSTRUCCIONS.md` els proposa la tasca 0, amb el to de la resta.

## Text de la pàgina web d'esborrat (per aprovar; es publica a la fase 7)

A ConversesLanding, `delete-account/`, `ca/esborrar-compte/` i `es/borrar-cuenta/`, amb l'estil de `privacy/`. En català:

> **Esborrar el compte de Converses**
>
> Converses funciona sense compte. Només en tens si has activat la sincronització amb el teu compte de Google.
>
> **Des de l'app:** Ajustos › Compte › Sincronització › Esborra el compte. S'esborren les dades del núvol, el compte i l'usuari de sincronització, i els altres dispositius deixen de sincronitzar. Les dades del telèfon es queden.
>
> **Sense l'app** (per exemple, si ja no tens el telèfon): escriu a feedback@converses.app des de l'adreça del compte de Google amb què sincronitzaves, amb l'assumpte «Esborrar el compte». L'esborrarem en un termini de 30 dies i t'avisarem quan estigui fet.
>
> **Què s'esborra:** les dades xifrades del núvol, la llista de dispositius, el pla i el compte de sincronització (correu i identificador). No guardem cap còpia.
>
> **Què no s'esborra:** les dades que tinguis als teus dispositius, i les estadístiques d'ús anònimes, si les havies acceptat, que no estan lligades al compte i caduquen soles (vegeu la política de privacitat).

Els 30 dies són una proposta: cal que Bernat els confirmi. El termini de les estadístiques s'ha de copiar de la política de privacitat quan es publiqui.

## Què cal mirar a la revisió

1. **Esborrar el compte** no toca cap dada local, demana el full de Google abans d'esborrar res i no esborra res si el compte és un altre o si es tanca el full.
2. **Els altres dispositius** no poden escriure des del pas 3, i acaben sense sessió, amb les dades i amb totes les files pendents.
3. **«Comença de nou»** no canvia res del núvol fins a «Continua». Després, al núvol només queden registres xifrats amb la clau nova. Un procés mort a mig camí acaba la feina en arrencar.
4. **Un dispositiu amb la clau vella** passa a `NeedsKey` i no puja res, tampoc des de «Torna a entrar».
5. **Activar un compte** puja totes les files, no només les pendents.
6. **Cap nom**, correu ni paraula surt en un log, un error o un esdeveniment.
7. **Amb el flag apagat**, cap canvi visible i cap captura canvia.

---

### Tasca 0: base comuna (en sèrie)

- [ ] El contracte, els falsos i els stubs.
- [ ] `Activating(replacing)` a tots els `when`, de moment igual que ara.
- [ ] Els textos.

### Tasca A: repositori (JVM)

**Fitxers:** `data/sync/SyncRepository.kt` i els seus tests.

- [ ] `deleteAccount` en l'ordre de més amunt, i què queda després de cada error.
- [ ] `startOver`, `restartPending` en arrencar i `cancelActivation` amb `replacing`.
- [ ] Compte desaparegut i usuari d'Auth esborrat (listener, `registerDevice`, `Gone`): tancar la sessió sense avís.
- [ ] Clau canviada (listener, `registerDevice`, `rejoin`): `NeedsKey`, sense pujar res.
- [ ] `markAllDirty` en activar, en començar de nou, abans de la fusió, en perdre el compte i en canviar la clau.
- [ ] Tests: esborrar amb èxit; amb el full tancat; amb un altre compte; sense connexió al primer pas; a mitges (i repetir-ho des de `Removed`); l'últim pas falla. Un altre dispositiu veu el compte esborrat amb l'app oberta i en arrencar. «Comença de nou» complet; tancat abans de «Continua»; procés mort després de la transacció. Un altre dispositiu amb la clau vella, en directe i amb «Torna a entrar». Activar després de tancar la sessió puja totes les files. Fusió: una fila igual a la del núvol no es duplica ni canvia.

### Tasca B: Firebase, Room i regles

**Fitxers:** `data/sync/FirebaseAuthGateway.kt`, `data/sync/FirestoreAccountRemote.kt`, `data/sync/FirestoreSyncRemote.kt`, `data/sync/RoomLocalSyncSource.kt`, `data/sync/DataStoreSyncLocalStore.kt`, els DAOs (només les consultes noves), `firestore.rules` (si cal), `firestore-tests/`, i els tests de `RoomLocalSyncSource` amb `FakeDb`.

- [ ] `reauthenticate` amb Credential Manager (com `signIn`) i `deleteUser`. `OtherAccount` si el token és d'un altre `uid`.
- [ ] `replace` en una transacció; `delete`; `clearDevices`; `deleteAllRecords` per lots de 200, amb el temps d'espera de les pujades per lot.
- [ ] `wrappedKey` i `format` a `RemoteDevices`, i el document que no existeix (`null`).
- [ ] `RemoteResult.Gone` a les crides que en poden tornar.
- [ ] `markAllDirty`, `dropTombstones` i `hasPeople`, sense canvis a l'esquema de Room (cap migració).
- [ ] Tests de les regles: el propietari pot buidar `devices`, canviar `wrappedKey` amb una transacció que deixa un sol dispositiu, esborrar els registres i el document; un altre usuari no pot fer res de tot això; un dispositiu que ja no és a `devices` no pot escriure registres. Si les regles d'ara ja ho cobreixen, no es toquen.
- [ ] Desplegar les regles només si canvien i amb el sí de Bernat.

### Tasca D: pantalles (sobre A i B)

**Fitxers:** `ui/sync/` (13n nova, `SyncReadyContent.kt`, el contingut de 13o2, `SyncNeedsKeyContent.kt`, el flux de les paraules), rutes i navegació, previews, captures i analítica.

- [ ] 13i: «No tinc les paraules» al peu, que obre 13n (`NoWordsRoute`, un formulari).
- [ ] 13n: les tres targetes numerades. La 2, amb «Comença de nou», només si `canStartOver`. Porta a `c4` i a les paraules noves.
- [ ] 13o i 13o2: la fila «Esborra el compte» (en `err`, amb `delete_s`) sota «Tanca la sessió», amb `c3`. Mentre esborra, un indicador de progrés i els botons desactivats. Els snackbars de sense connexió, de compte diferent i d'error.
- [ ] Analítica: la pantalla `sync_no_words`; `sync_account_deleted` (`result`: `done`, `cancelled`, `other_account`, `offline`, `failed`); `sync_start_over` (`result`: `started`, `cancelled`, `offline`, `failed`); i `sync_account_gone` (`reason`: `deleted` o `key_changed`), quan aquest dispositiu ho descobreix. Al catàleg de `docs/analytics/README.md`. Mai el compte ni recomptes.

### Després de fusionar

- [ ] `./gradlew ktlintCheck testDebugUnitTest :app:assembleDebug` net, i el job de les regles en verd.
- [ ] Prova amb els dos emuladors i el flag encès, amb un compte de prova:
  - «Comença de nou» a la tauleta sense les paraules: el telèfon passa a «falten les paraules», hi entra amb les noves i la fusió no perd res.
  - Esborrar el compte des del telèfon: la tauleta tanca la sessió sola, Firestore queda sense `users/{uid}` i Auth sense l'usuari.
  - Tornar a activar des del telèfon: puja totes les files.
  - El full de Google tancat i sense connexió.
- [ ] `docs/sync/disseny.md`: «Construït (fase 6)», i com esborra Bernat un compte a mà: l'usuari a la consola d'Authentication per correu, i `firebase firestore:delete users/<uid> --recursive`.
