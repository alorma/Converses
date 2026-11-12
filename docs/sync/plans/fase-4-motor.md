# Sincronització, fase 4: motor. Pla d'implementació

> **Per a qui l'executi amb agents:** primer la base comuna (tasca 0), en sèrie. Després les tasques A, B, C i D en paral·lel, cadascuna al seu worktree sobre `sync-fase-4`. Cada tasca només toca els fitxers que té assignats.

**Objectiu:** que les dades viatgin. Un dispositiu `Ready` puja el que ha canviat i baixa el que han canviat els altres, xifrat amb la clau de la fase 3. El primer dispositiu ho puja tot en activar-se; un dispositiu nou ho baixa tot en donar les paraules, i si ja tenia notes, les uneix amb les del núvol.

**Disseny:** `docs/sync/disseny.md` (seccions «Què es guarda a Firestore», «Regles de Firestore», «Canvis a Room», «Motor» i «Persones protegides») i `docs/sync/disseny/INSTRUCCIONS.md` (taulers 13g, 13h, 13m, 13m2 i l'estat de 13o).

## Què hi entra i què no

| Hi entra | Queda per a |
| --- | --- |
| Pujar i baixar persones (amb els fets fixats), entrades, tasques i canals propis | |
| Conflictes, esborrats, referències desordenades, canals duplicats | |
| Pujada inicial (13g, 13h) i baixada (13m2), amb progrés | «Amaga» amb notificació: vegeu les decisions |
| Fusió en un dispositiu que ja té notes (13m) | |
| Estat a 13o i a l'indicador: «Al dia», «Sincronitzant…», «Sense connexió · N canvis pendents» | |
| El dispositiu s'apunta a `devices` (nom xifrat, `addedAt`, `lastSyncAt`) i s'escriu `plan` | Límit de places (13l), dispositius a 13o, dispositiu tret: fase 5 |
| Regles definitives de Firestore, amb tests a l'emulador i al CI | Límit de 2/5 dispositius a les regles: fase 5 |
| | «No tinc les paraules» i «Comença de nou» (13n): fase 6, amb esborrar el compte (les dues buiden el núvol) **(B, 12 de novembre de 2026)** |

Tot continua darrere de `SyncFlag`. Amb el flag apagat no s'inicialitza res de nou: ni el motor, ni WorkManager, ni cap listener.

## Privacitat externa

No surt cap tipus de dada que la fase 3 no enviés ja a Firestore, tret dels **registres xifrats** que el disseny ja preveu («Privacitat externa» de `disseny.md`): el contingut, xifrat al dispositiu; la seva mida, el nombre de registres i les dates d'escriptura, visibles per a Google i per a Bernat. El nom del dispositiu va xifrat. Cap permís nou (vegeu la decisió sobre la notificació). La política i el formulari de Play continuen a la fase 7.

## Decisions preses en aquest pla

- **(C)** `recordId` és el `syncId` de la fila. No cal cap taula de correspondències ni cap canvi a Room: **la fase 4 no toca l'esquema**.
- **(C)** Un esborrat no deixa `c` buit, com deia el disseny, sinó xifrat amb només `{ "deletedAt": … }`: sense la data del client no es pot decidir si l'esborrat guanya a una edició. No revela res que `deleted` no digui ja.
- **(C)** Format del contingut: JSON amb `kotlinx.serialization`, una classe per tipus (`PersonRecord`, `EntryRecord`, `TaskRecord`, `ChannelRecord`) amb `type`, `v` (versió del tipus), `modifiedAt` i els camps de l'entitat. Les referències (`personId`, `customChannelId`) van per `syncId`. Camps desconeguts s'ignoren (`ignoreUnknownKeys`), perquè una versió més nova de l'app pugui afegir-ne sense trencar les velles. El format queda fixat amb un test com el de `RecordCipher`.
- **(C)** Treure la marca `dirty` en pujar és condicional: només si `modifiedAt` encara és el que s'ha pujat. Una edició feta mentre es pujava queda pendent i puja a la volta següent.
- **(C)** El motor escriu a Room amb mètodes nous dels DAOs (`applyRemote…`, `markClean…`) que **no segellen**: un canvi baixat no ha de tornar a pujar. No passen per `ConversesRepository`, i `SyncIdentityTest` no els veu.
- **(C)** Referències que arriben desordenades: una entrada o tasca sense la seva persona (o amb un canal propi que encara no hi és) espera en memòria fins al final de la baixada. El cursor no avança més enllà del primer registre que espera, de manera que si el procés mor, la baixada següent el torna a demanar. Al final d'una baixada completa, el que encara espera és d'una persona esborrada i es descarta. Així no cal cap taula nova.
- **(C)** Els propis registres que tornen en baixar (`by` és aquest dispositiu) s'ignoren si la versió local no és més antiga.
- **(C)** Agrupació: 2 segons després de l'última escriptura local, en lots de fins a 200 registres. Reintents amb espera creixent (5 s, 15 s, 1 min, 5 min) mentre l'app és visible; si es tanca amb canvis pendents, un `OneTimeWorkRequest` de WorkManager amb `NetworkType.CONNECTED`. La versió de WorkManager es comprova a la documentació oficial en afegir-la.
- **(B, 12 de novembre de 2026)** «Amaga» a la pujada i la baixada inicials tanca la pantalla i la feina continua a WorkManager **sense notificació**: una notificació demanaria `POST_NOTIFICATIONS`, un permís nou.
- **(C)** El dispositiu s'apunta a `devices` en arribar a `Ready`, amb un `deviceId` aleatori desat al DataStore `sync`. `plan` el posa `ProRepository.isPro`. El límit de places i la UI de dispositius són de la fase 5, però les regles ja exigeixen que `by` sigui a `devices`: és el que fa efectiva l'expulsió de la fase 5.
- **(C)** Els tests de les regles corren a l'emulador de Firestore (`@firebase/rules-unit-testing`), en un job nou del CI que només s'engega si canvien `firestore.rules` o els tests. Són la frontera de seguretat: valen la pena.

## Contracte (tasca 0)

`data/sync`:

| Peça | Què és |
| --- | --- |
| `SyncRecord` i les quatre classes de contingut | El JSON de cada tipus, amb `RecordCodec` (codificar i decodificar, i el test que fixa el format) |
| `RemoteRecord(id, sealed, deleted, by, updatedAt)` | Un registre tal com és a Firestore |
| `SyncRemote` | `push(uid, records)`, `pull(uid, since, limit)`, `listen(uid, since): Flow`, `registerDevice(uid, device)` |
| `SyncCursorStore` | Cursor de l'última baixada i `deviceId` (DataStore `sync`) |
| `LocalSyncSource` | El costat de Room que fa servir el motor: llegir els pendents (`dirty` i tombstones), marcar-los nets si no han canviat, aplicar registres baixats sense segellar |
| `SyncEngine` | `pushPending()`, `pullAll(onProgress)`, `merge(...)`. JVM pur |
| `SyncStatus` | `UpToDate(at)`, `Syncing(progress?)`, `Offline(pending)`, `Failed` |

`SyncRepository` guanya `status: StateFlow<SyncStatus>` i fa servir el motor quan és `Ready`. Els nous mètodes dels DAOs es declaren a la tasca 0 perquè les tasques A i C els facin servir sense tocar `Daos.kt`.

Falsos a `app/src/test/.../testing`: `FakeSyncRemote` (en memòria, amb `updatedAt` creixent i la possibilitat de fallar o tornar desordenat) i `FakeCursorStore`. `FakeDb` implementa els mètodes nous.

Textos nous a la tasca 0, en els tres idiomes: `up_t`, `done_t`, `done_s`, `done`, `merge_t`, `merge_b`, `st_ok`, `st_sync`, `st_pend`, amb el prefix `sync_`.

## Què cal mirar a la revisió

1. **Res surt sense xifrar:** cap camp d'una entitat, cap nom ni cap `id` local arriba a Firestore fora de `c`.
2. **Un canvi baixat no torna a pujar**, i una edició feta mentre es pujava no es perd.
3. **Conflictes:** guanya el `modifiedAt` més recent; en cas d'empat, el `deviceId` més gran; un esborrat guanya a una edició anterior i perd contra una de posterior.
4. **Persones protegides** arriben protegides, i cap text del motor (progrés, errors, logs) porta noms ni contingut.
5. **Un procés mort a mitja baixada** no deixa entrades òrfenes ni salta registres.
6. **Les regles** rebutgen: un altre usuari, un `by` que no és a `devices`, un document amb camps de més o de menys, i un `c` de més de 64 KB.
7. **Amb el flag apagat**, cap canvi visible, cap captura canvia, i ni WorkManager ni Firestore s'inicialitzen.

---

### Tasca 0: base comuna (en sèrie)

- [ ] El contracte, `RecordCodec` amb el seu test de format, els falsos i els mètodes nous dels DAOs (amb el seu test a `FakeDb`).
- [ ] `SyncStatus` a `SyncRepository`, sempre `UpToDate` fins que el motor hi sigui.
- [ ] Implementacions d'Android buides però segures, perquè el graf de Koin sigui complet a cada worktree.
- [ ] Els textos.

### Tasca A: el motor (JVM)

**Fitxers:** `data/sync/engine/` i els seus tests.

- [ ] `pushPending`: llegeix files `dirty` i tombstones, xifra amb `RecordCipher` (els fets fixats dins la persona), puja per lots i marca nets els que no han canviat. Els tombstones pujats s'esborren.
- [ ] `pullAll`: des del cursor amb un marge de 60 segons cap enrere, ordenat per `updatedAt`. Desxifra, resol el conflicte, aplica per ordre (canals, persones, entrades i tasques) i desa el cursor.
- [ ] Canals propis duplicats (mateix nom i grup): es fusionen en el de `syncId` més petit, les entrades s'hi remapen i l'altre deixa un tombstone.
- [ ] Un registre que no es desxifra o no es decodifica es salta i es compta (`trackError`, lloc `sync_record`), sense aturar la resta.
- [ ] Tests amb `FakeDb` i `FakeSyncRemote`: anada i tornada de cada tipus, conflictes (més nou, empat, esborrat abans i després), edició durant la pujada, referències desordenades, procés mort a mitja baixada, canals duplicats, persona protegida.

### Tasca B: Firestore i regles

**Fitxers:** `data/sync/FirestoreSyncRemote.kt`, `firestore.rules`, `firestore-tests/` (Node), `.github/workflows/firestore-rules.yml`.

- [ ] `FirestoreSyncRemote`: `push` amb `WriteBatch` i `updatedAt` = `FieldValue.serverTimestamp()`; `pull` amb `whereGreaterThan("updatedAt", since).orderBy("updatedAt").limit(n)` des del servidor; `listen` amb un `SnapshotListener` mentre l'app és visible; `registerDevice` amb una actualització de `devices.{deviceId}`.
- [ ] Regles: només el propietari; `users/{uid}` amb forma fixa; `records/{id}` amb forma fixa (`c`, `deleted`, `by`, `updatedAt == request.time`), `c` de 64 KB com a màxim i `by` dins de `devices`. Comprovar a la documentació el cost i el límit de lectures de `get()` dins d'un lot.
- [ ] Tests de les regles a l'emulador i el job del CI. Es despleguen amb la CLI (`firebase deploy --only firestore:rules`) només quan Bernat ho aprovi.

### Tasca C: quan sincronitza

**Fitxers:** `data/sync/SyncScheduler.kt`, `data/sync/SyncWorker.kt`, `SyncRepository.kt` (només la part del motor), `di/`, `ConversesApp.kt` (només l'arrencada), catàleg i `app/build.gradle.kts` per a WorkManager.

- [ ] En arribar a `Ready`: apuntar el dispositiu i fer una baixada.
- [ ] Mentre l'app és visible (`ProcessLifecycleOwner`): listener i pujada 2 segons després de l'última escriptura (un `Flow` del nombre de pendents).
- [ ] En passar a segon pla amb pendents: el `SyncWorker` de WorkManager. Sense flag o sense sessió, el worker no fa res.
- [ ] Reintents amb espera creixent; sense connexió, `Offline(pending)`.
- [ ] Activació (primer dispositiu): després de «Continua», pujada de tot amb progrés. Dispositiu nou: després de les paraules, fusió si cal i baixada de tot amb progrés.

### Tasca D: pantalles i estat

**Fitxers:** `ui/sync/` (contingut nou de pujada, baixada, fet i fusió; `SyncReadyContent.kt` per a l'estat; `SyncIndicator.kt`), previews, captures i analítica.

- [ ] 13g i 13m2: galeta, barra determinada pel nombre de registres, «Amaga».
- [ ] 13h: galeta `pri` amb `check`, el nom del dispositiu i «Fet». Sense «Afegeix un altre dispositiu» (QR) ni les places, que són de la fase 5: el text diu només el dispositiu.
- [ ] 13m: diàleg «Aquest dispositiu ja té notes», amb el recompte d'entrades i mai noms.
- [ ] Estat a 13o (`st_ok` amb «fa N minuts», `st_sync`, `st_pend`) i a l'indicador.
- [ ] Analítica: `sync_upload_finished` i `sync_download_finished` (`result`, sense recomptes), i `sync_merge_shown`. Al catàleg.

### Després de fusionar

- [ ] `./gradlew ktlintCheck testDebugUnitTest :app:assembleDebug` net, i el job de les regles en verd.
- [ ] Prova amb dos emuladors i el flag encès: activar al primer amb dades, entrar al segon amb dades pròpies (fusió), editar als dos, esborrar en un i editar a l'altre, persona protegida, i sense connexió.
- [ ] Desplegar les regles (amb el sí de Bernat).
- [ ] `docs/sync/disseny.md`: «Construït (fase 4)» i les decisions d'aquest pla que canvien el disseny (esborrats amb `c`).
