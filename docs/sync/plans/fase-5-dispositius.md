# Sincronització, fase 5: dispositius. Pla d'implementació

> **Per a qui l'executi amb agents:** primer la base comuna (tasca 0), en sèrie. Després les tasques A, B i C en paral·lel, cadascuna al seu worktree sobre `sync-fase-5`. Cada tasca només toca els fitxers que té assignats. La D va al final, sobre les tres.

**Objectiu:** que els dispositius d'un compte es puguin veure i gestionar. Un compte té 2 places (5 amb Pro). Un dispositiu que entra amb les places plenes tria quin en surt. Un dispositiu tret deixa de sincronitzar, conserva les dades i ho diu. Tancar la sessió allibera la plaça.

**Disseny:** `docs/sync/disseny.md` (seccions «Com es veu per a l'usuari», «Què es guarda a Firestore», «Regles de Firestore», «Motor», l'apartat «Dispositiu tret», i «Compte») i `docs/sync/disseny/INSTRUCCIONS.md` (taulers 13a, 13b, 13c, 13l, 13o, 13o2 i 13q). Tot continua darrere de `SyncFlag`.

## Què hi entra i què no

| Hi entra | Queda per a |
| --- | --- |
| Límit de 2 i 5 places a les regles de Firestore, amb tests | |
| Places plenes en entrar (13l), amb la fila de Pro | |
| Secció «Dispositius» a 13o: «N de M», files, «Canvia el nom» i «Treu» amb confirmació (13q) | Fila «Afegeix un dispositiu» i QR (13p, 13k): sense data **(B, 12 de novembre de 2026)** |
| Dispositiu tret: estat `Removed`, 13o2, avís al llistat (13c), insígnia a l'indicador i a Ajustos | |
| Tancar la sessió treu el dispositiu de `devices` | Esborrar el compte, «No tinc les paraules», «Comença de nou»: fase 6 |
| `plan` al dia quan canvia `isPro` | Lligar RevenueCat amb el compte: fora d'abast |

## Privacitat externa

No surt cap dada nova. `devices`, `plan` i el nom xifrat del dispositiu ja hi eren des de la fase 4. Aquesta fase només els edita: canviar un nom, treure una entrada i actualitzar `lastSyncAt` més sovint. Cap permís nou. La política i el formulari de Play continuen a la fase 7.

## Decisions preses en aquest pla

- **(B, 12 de novembre de 2026)** Sense QR. La fila «Afegeix un dispositiu» de 13o no es dibuixa, i el botó «Afegeix un altre dispositiu» de 13h tampoc.
- **(B, 12 de novembre de 2026)** Tancar la sessió sense connexió tanca igualment, perquè les dades es queden al dispositiu. La plaça continua ocupada al núvol fins que es tregui des d'un altre dispositiu o fins que aquest torni a entrar (vegeu el punt següent).
- **(B, 13 de novembre de 2026)** «Ara no» a 13l des d'un dispositiu tret («Torna a entrar» amb les places plenes) només tanca la pantalla: el dispositiu es queda `Removed`, amb la clau i la sessió. Només en entrar per primera vegada «Ara no» tanca la sessió.
- **(C)** El `deviceId` ja **no s'esborra** en tancar la sessió: passa a un DataStore propi (`sync_device`), fora del `sync` que `setSession(false)` buida. Tornar a entrar amb el mateix compte des del mateix dispositiu reaprofita la seva entrada si encara hi és, i no ocupa cap plaça més. El cursor sí que s'esborra, com ara. Un factory reset, una desinstal·lació o un «esborra dades» donen un `deviceId` nou, i l'entrada vella es treu des de 13l o des de la llista.
- **(C)** El dispositiu **recorda que s'ha apuntat** (`registered` al DataStore `sync`, s'esborra en tancar la sessió). Si ja s'havia apuntat i ara no és a `devices`, no s'hi torna a apuntar: passa a `Removed`. Només «Torna a entrar» el torna a apuntar. Sense aquesta marca, el dispositiu tret es tornaria a afegir sol en la sincronització següent. És una protecció de l'app: les regles no poden distingir un dispositiu nou d'un de tret.
- **(C)** Places plenes: abans d'apuntar-se, el dispositiu llegeix `devices` i `plan` del servidor. Si hi ha `count >= límit` i aquest `deviceId` no hi és, mostra 13l en lloc de seguir. «Treu-lo i continua» fa una sola transacció que treu el triat i afegeix aquest dispositiu. Si dos dispositius entren alhora, les regles rebutgen el segon. L'app tracta aquest rebuig (`PERMISSION_DENIED` en apuntar-se) com «places plenes»: torna a llegir i ensenya 13l.
- **(C)** Ordre en un dispositiu que entra: clau → places (si cal) → fusió (si hi ha entrades locals) → baixada. Al primer dispositiu (activació) no cal mirar res: el compte no té dispositius.
- **(C)** Límit: `free` → 2, `pro` → 5. El límit es calcula a partir del `plan` que hi ha al document, no de l'`isPro` local. Així l'app i les regles diuen el mateix. Si aquest dispositiu és Pro i el document encara diu `free`, en entrar s'escriu primer `plan: "pro"`.
- **(C)** El `plan` s'escriu en apuntar-se (com ara) i, mentre és `Ready`, cada vegada que canvia `ProRepository.isPro`. Baixar de Pro amb més de 2 dispositius no treu ningú: simplement no se'n poden afegir més. `plan` el declara l'app, i una app modificada es podria declarar Pro. És el risc que el disseny ja acceptava.
- **(C, 13 de novembre de 2026, amb el vistiplau de B)** Apuntar-se només fa **pujar** el `plan`: un dispositiu gratuït que entra (o torna a entrar) en un compte `pro` no el baixa. El baixa només el dispositiu que deixa de ser Pro (`isPro` passa de cert a fals). Es va veure a la prova: el telèfon gratuït, en tornar a entrar, deixava a 2 places un compte amb una tauleta Pro. Si el dispositiu Pro es perd, el compte es queda a 5: el mateix risc acceptat de més amunt.
- **(C, 13 de novembre de 2026, amb el vistiplau de B)** `out_b` perd «Per tornar-hi, cal treure'n un altre»: si hi ha plaça, «Torna a entrar» funciona directament, i si no n'hi ha ja surt 13l.
- **(C)** El nom només s'escriu en crear l'entrada. `registerDevice` ja no el sobreescriu en cada sessió, perquè un nom canviat des d'un altre dispositiu no es perdi. Canviar el nom el torna a xifrar amb la mateixa AAD (`device-name:{deviceId}`). Qualsevol dispositiu del compte pot canviar el nom de qualsevol altre (13o).
- **(C)** `devices.{id}.lastSyncAt` s'actualitza en apuntar-se i, després, en acabar bé una sincronització, com a molt cada 15 minuts. Així la llista diu una cosa raonable sense una escriptura del document per cada pujada.
- **(C)** Detectar que t'han tret:
  - mentre l'app és visible, un listener sobre `users/{uid}` (el mateix que alimenta la llista de 13o) veu que `devices` ja no té aquest `deviceId`;
  - si l'app era tancada, ho veu la lectura que fa `registerDevice` a la primera sincronització;
  - si una pujada és rebutjada per les regles (`PERMISSION_DENIED`), l'app torna a llegir `devices` per confirmar-ho.

  En tots tres casos, el motor s'atura (sense listener, sense worker, `SyncScheduler` no fa res) i l'estat passa a `SyncState.Removed(account)`.
- **(C)** Un dispositiu tret **conserva la clau i la sessió**. «Torna a entrar» no demana les paraules: si hi ha plaça, s'apunta i fa una pujada i una baixada, que és la fusió (ja és la unió). Si no n'hi ha, va a 13l. Els canvis fets mentre era fora es queden `dirty` i pugen en tornar.
- **(C) Límit que no es pot evitar:** treure un dispositiu li impedeix **escriure**, no **llegir**. La sessió de Google i la clau es queden en aquell dispositiu, i Firestore no sap quin dispositiu llegeix. Contra un mòbil perdut o robat, la solució de debò és «Comença de nou» (una clau nova, fase 6) o tancar la sessió de Google des del compte de Google. Es diu a `disseny.md`, no a la UI.
- **(C)** Tancar la sessió: primer treu aquest dispositiu de `devices` (amb el mateix temps d'espera de 20 s que les pujades) i després fa el que ja feia. Si falla per connexió, tanca igualment. Si està a `Removed`, no cal treure res.
- **(C)** La fila de Pro de 13l i de 13o obre el paywall amb un benefici nou, `ProBenefit.Devices` («Fins a 5 dispositius amb Pro»), que només surt a la llista de beneficis amb `SyncFlag` encès. L'origen d'analítica és `sync`. Amb el flag apagat, el paywall i les seves captures no canvien.
- **(C)** Treure el dispositiu propi des de la llista no surt al menú: per a això hi ha «Tanca la sessió». La fila «Aquest dispositiu» només té «Canvia el nom».
- **(C)** Regles del límit:
  - El límit es mira amb el `plan` que queda després de l'escriptura (`request.resource.data.plan`). Si es mirés el que hi ha ara, un dispositiu Pro no podria entrar en un compte que encara diu `free`. És el mateix risc acceptat de més amunt.
  - Es compta amb `devices.keys().size()` i es compara amb el de `resource`.
  - Les regles no poden recórrer un mapa. Per això no es comprova la forma de cada entrada: es manté `devices is map`. La forma la posa l'app, i un compte que se l'espatlla només s'espatlla a si mateix.
  - `addedAt` i `lastSyncAt` són sempre `FieldValue.serverTimestamp()`.
- **(C)** Com es llegeix la marca `registered` en apuntar-se:

  | Marca | Aquest `deviceId` a `devices` | Què fa |
  | --- | --- | --- |
  | Hi és | Hi és | Segueix |
  | Hi és | No hi és | `Removed` |
  | No hi és | Hi és | Desa la marca i segueix (els dispositius de prova de la fase 4, o tornar a entrar després de tancar la sessió sense connexió) |
  | No hi és | No hi és | S'apunta, o 13l si les places són plenes |

  Esborrar les dades de l'app també esborra `sync_device`, i el dispositiu torna com a nou.
- **(C)** Si una entrada ja existeix, `joinDevice` només actualitza `lastSyncAt` i `plan`. El nom no el toca.
- **(C)** Primer es mira si aquest dispositiu ja és a `devices`, i després el límit. Un dispositiu que ja hi és no veu mai 13l, encara que el compte hagi baixat de Pro amb 5 dispositius.
- **(C)** Si en tancar la sessió el temps d'espera s'esgota i l'esborrat arriba tard al servidor, no passa res: esborrar l'entrada és idempotent. Si ja no hi ha sessió d'Auth quan arriba, les regles el rebutgen i la plaça es treu des d'un altre dispositiu.
- **(C)** Una pujada rebutjada amb `PERMISSION_DENIED` torna a llegir `devices`. Si aquest dispositiu hi continua, és un error normal (`Failed`) i l'estat no canvia.
- **(C)** El `deviceId` es mou a `sync_device` quan algú el demana per primera vegada, i amb el flag apagat no el demana ningú. Per a qui no sincronitza no canvia res.
- **(C)** `FakeSyncRemote` i `FakeDeviceStore` són de la tasca 0. Les tasques A i B no els toquen.
- **(C)** Cap nom de dispositiu surt als logs, a l'analítica ni als errors. Els noms es desxifren al repositori. Si un nom no es pot desxifrar, la fila diu «Dispositiu» (text nou), sense error a la UI. L'error es compta a `sync_record`.

## Contracte (tasca 0)

`data/sync`:

| Peça | Què és |
| --- | --- |
| `DeviceInfo(id, name, addedAt, lastSyncAt, isThis)` | Una fila de la llista, amb el nom ja desxifrat; `toString()` sense el nom |
| `RemoteDevices(plan, devices: List<RemoteDeviceEntry>)` | El que hi ha a `users/{uid}`: el pla i, per a cada dispositiu, `id`, `sealedName`, `addedAt` i `lastSyncAt` |
| `SyncRemote` | Guanya `loadDevices(uid)`, `observeDevices(uid): Flow`, `joinDevice(uid, device, removing: String?)` (en una transacció: treu `removing` si cal, afegeix o actualitza aquest i escriu `plan`; distingeix `Full` de `Failed`), `removeDevice(uid, id)`, `renameDevice(uid, id, sealedName)`, `touchDevice(uid, id)` (`lastSyncAt`) i `setPlan(uid, pro)` |
| `DeviceStore` | `deviceId()` estable (DataStore `sync_device`) i `registered`/`setRegistered` (DataStore `sync`) |
| `SyncState.Removed(account)` | Dispositiu tret, amb clau i dades |
| `SlotsPrompt` | Al repositori: `slotsPrompt: StateFlow<SlotsPrompt?>`, amb el límit, els dispositius que hi ha (sense aquest) i si es pot passar a Pro |
| `SyncRepository` | `devices: Flow<List<DeviceInfo>>`, `deviceLimit: Flow<Int>`, `chooseDeviceToRemove(id)` (de 13l), `cancelSlots()` («Ara no», tanca la sessió), `rejoin()` («Torna a entrar»), `removeDevice(id)`, `renameDevice(id, name)` |

Falsos: `FakeSyncRemote` guarda `devices` i `plan`, aplica el límit com les regles i pot simular que un altre dispositiu treu aquest. `FakeDeviceStore`.

Textos nous a la tasca 0, en els tres idiomes, amb el prefix `sync_` (taula de `INSTRUCCIONS.md`): `full_t`, `full_b`, `pro5`, `devices`, `this_dev`, `out_t`, `out_b`, `st_out`, el títol i el botó de «Treure un dispositiu», «Canvia el nom», «Treu», «Torna a entrar», «Treu-lo i continua», «Ara no», «N de M», el diàleg de canviar el nom i «Dispositiu» per al nom que no es pot llegir. Els textos que no surtin a la taula els proposa la tasca 0, amb el to de la resta.

## Què cal mirar a la revisió

1. **Les regles** rebutgen afegir un tercer dispositiu amb `free` i un sisè amb `pro`. Accepten treure'n, canviar el nom, `lastSyncAt` i `plan` amb el compte ple, i un compte que baixa de `pro` a `free` amb 5 dispositius continua podent escriure el document.
2. **Un dispositiu tret no s'hi torna a apuntar sol**, ni després de reiniciar l'app ni amb el worker.
3. **Places plenes:** cap dispositiu triat per defecte; «Ara no» tanca la sessió i no deixa res al núvol de nou; la transacció treu i afegeix alhora.
4. **Tancar la sessió** treu l'entrada amb connexió i tanca igualment sense; el `deviceId` sobreviu.
5. **Cap nom** de dispositiu ni de persona surt en un log, un error o un esdeveniment.
6. **Amb el flag apagat**, cap canvi visible i cap captura canvia (tampoc el paywall).

---

### Tasca 0: base comuna (en sèrie)

- [ ] El contracte i els falsos. El `deviceId` es mou al DataStore `sync_device`: si n'hi havia un a `sync`, es copia la primera vegada, perquè els dispositius de prova de la fase 4 no perdin la plaça.
- [ ] `SyncState.Removed` a tots els `when` (indicador, Ajustos, `SyncScreen`), de moment amb el mateix aspecte que `Ready`.
- [ ] Els textos.

### Tasca A: repositori i places (JVM)

**Fitxers:** `data/sync/SyncRepository.kt`, `data/sync/SyncScheduler.kt` (només per aturar-se a `Removed`), i els seus tests.

- [ ] `registerDevice` → `joinDevice`: llegeix `devices`. Si ja s'havia apuntat i ara no hi és, passa a `Removed`. Si no hi és i les places són plenes, `slotsPrompt`. Si no, s'apunta. En entrar amb les paraules, va abans de la fusió i la baixada.
- [ ] 13l: `chooseDeviceToRemove` (transacció), `cancelSlots`, i el rebuig de les regles com a «places plenes».
- [ ] `Removed`: aturar el motor, conservar la clau, `rejoin()`.
- [ ] `devices` i `deviceLimit` a partir de `observeDevices`, amb els noms desxifrats. El listener també detecta que t'han tret.
- [ ] `removeDevice`, `renameDevice`, `plan` quan canvia `isPro` i `lastSyncAt` cada 15 minuts com a molt.
- [ ] `signOut` treu l'entrada abans de tancar, amb temps d'espera, i esborra `registered`.
- [ ] Tests: entrar amb plaça, entrar ple i treure'n un, «Ara no», dues entrades alhora, tret mentre és visible, tret amb l'app tancada, pujada rebutjada, «Torna a entrar» amb plaça i sense, tancar la sessió amb connexió i sense (i tornar a entrar amb el mateix `deviceId`), canviar a Pro, baixar de Pro amb 5.

### Tasca B: Firestore i regles

**Fitxers:** `data/sync/FirestoreSyncRemote.kt`, `data/sync/DataStoreSyncCursorStore.kt` (o un `DataStoreDeviceStore.kt` nou), `firestore.rules`, `firestore-tests/`.

- [ ] Les operacions noves de `SyncRemote`. `joinDevice` en una transacció; `PERMISSION_DENIED` en apuntar-se torna `Full`, i en pujar es distingeix de la resta d'errors perquè el repositori ho pugui comprovar.
- [ ] Regles: `devices` pot créixer només fins a 2 (`free`) o 5 (`pro`), segons el `plan` que queda; es pot quedar igual o fer-se petit sempre.
- [ ] Tests de les regles per a tots els casos del punt 1 de la revisió.
- [ ] Desplegar les regles només quan Bernat ho aprovi.

### Tasca C: Pro

**Fitxers:** `ui/paywall/` (el benefici `Devices`, només amb `SyncFlag`), els seus textos i les seves captures.

- [ ] `ProBenefit.Devices`, fora de la llista quan el flag és apagat. Previsualització i captura amb el flag encès.
- [ ] `docs/pro/README.md`: les 5 places són un benefici de Pro, darrere del flag.

### Tasca D: pantalles (sobre A, B i C)

**Fitxers:** `ui/sync/` (contingut nou de places i de dispositius, `SyncReadyContent.kt`, `SyncIndicator.kt`), la fila d'Ajustos, l'avís del llistat (`ui/people`), rutes i navegació, previews, captures i analítica.

- [ ] 13l (`SlotsRoute`, un formulari: diàleg de 560 dp en pantalla gran): radios sense res triat, la fila discontínua de Pro, «Treu-lo i continua» i «Ara no».
- [ ] 13o: secció «Dispositius» amb «N de M», les files, el menú i la fila de Pro. Diàleg per canviar el nom i confirmació per treure (13q).
- [ ] 13o2: l'avís `err-c`, sense l'estat. La llista mostra els dispositius que sincronitzen, i hi ha «Torna a entrar».
- [ ] 13c: la targeta fixa a dalt del llistat de persones, sota l'avís de canals si n'hi ha. A pantalla gran, a dalt del panell del llistat.
- [ ] Indicador amb `priority_high` sobre `err`; fila d'Ajustos amb la icona i el subtítol en `err`.
- [ ] Analítica: `sync_slots_shown`, `sync_device_removed` (`from`: `slots` o `list`), `sync_device_renamed`, `sync_rejoined` (`result`: `done`, `full`, `offline`, `failed`) i `sync_removed_seen` (una vegada per dispositiu tret). Al catàleg de `docs/analytics/README.md`. Mai noms ni recomptes.

### Després de fusionar

- [ ] `./gradlew ktlintCheck testDebugUnitTest :app:assembleDebug` net, i el job de les regles en verd.
- [ ] Desplegar les regles (amb el sí de Bernat).
- [ ] Prova amb els dos emuladors i el flag encès: posar un nom; tancar la sessió en un (l'entrada desapareix de l'altre) i tornar a entrar; esborrar les dades de la tauleta i entrar-hi de nou amb les places plenes (13l, treure l'entrada vella); des del telèfon, treure la tauleta (13c, 13o2, «Torna a entrar»); el rebuig de les regles a una pujada.
- [ ] `docs/sync/disseny.md`: «Construït (fase 5)», el límit de lectura de «Dispositiu tret» i el `deviceId` que sobreviu a tancar la sessió.
