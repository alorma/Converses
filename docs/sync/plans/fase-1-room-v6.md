# Sincronització, fase 1: identitat de sincronització a Room (v5 → v6). Pla d'implementació

> **Per a qui l'executi amb agents:** SUB-SKILL OBLIGATÒRIA: superpowers:subagent-driven-development (recomanada) o superpowers:executing-plans, tasca a tasca. Els passos porten caselles (`- [ ]`) per seguir-los.

**Objectiu:** cada persona, entrada, tasca i canal propi té un identificador global, una data de modificació i una marca de «pendent de pujar», i cada esborrat deixa un registre, sense cap canvi visible per a l'usuari.

**Arquitectura:** tres columnes noves a quatre taules i una taula `sync_tombstone`, amb una `Migration` manual que omple un UUID per fila. El `ConversesRepository` segella (`modifiedAt`, `dirty`) cada escriptura; els esborrats i les consultes en bloc ho fan dins de transaccions dels DAOs, perquè les cascades no se n'escapin. Cap peça de sincronització llegeix encara aquestes dades.

**Stack:** Room 2.8.5 amb KSP, Koin, tests JVM amb `FakeDb`, tests instrumentats amb `MigrationTestHelper` i Room en memòria.

**Disseny:** `docs/sync/disseny.md`, secció «Canvis a Room (v5 → v6)». Qui executa llegeix tots dos documents.

## Restriccions globals

- Els `id` locals no canvien: la UI, les rutes i les claus foranes continuen igual.
- Tot canvi d'esquema porta: `version = 6`, `app/schemas/6.json` versionat, `Migration` registrada a `di/AppModules.kt` i test amb `MigrationTestHelper`. No s'edita cap esquema ja exportat (`1.json`…`5.json`).
- La migració és manual (`Migration5To6`), no `AutoMigration`: ha d'omplir un `syncId` diferent per fila.
- Cap canvi visible: ni pantalles, ni textos, ni `strings.xml`. Sense feature flag, perquè una migració no es pot amagar; el disseny la dona per publicable sola.
- No surt res del telèfon: ni biblioteques ni permisos nous. La regla de privacitat externa no s'activa.
- `pinned_fact` no té identitat pròpia: viatja dins de la seva persona, i canviar un fet marca la persona.
- `person` no rep `modifiedAt`: reaprofita `updatedAt`.
- Tests instrumentats: sempre en un **emulador**, mai al telèfon de Bernat (`connectedAndroidTest` desinstal·la l'app).
- Commits petits, i **només amb el vistiplau de Bernat** a cada un. Feina a la branca `sync-fase-1`.
- `./gradlew ktlintCheck` net abans de cada commit.

## Decisions preses en aquest pla

- **(B)** `dirty` es marca als DAOs i al repositori, no amb triggers.
- **(C)** L'estat del motor (cursor de l'última baixada) no entra a la v6: anirà a un DataStore a la fase 4, i així aquesta migració no porta cap taula que encara no fa servir ningú.
- **(C)** Les files existents queden `dirty = 1`; `modifiedAt` s'omple amb `occurredAt` (entrades), `COALESCE(doneAt, createdAt)` (tasques) i `createdAt` (canals).
- **(C)** Tornar a inserir una fila esborrada («Desfés» d'una entrada) esborra el seu tombstone i la segella amb l'hora actual, perquè a la fase 4 una edició posterior guanyi a l'esborrat.
- **(C)** Sense sessió els tombstones s'acumulen (una fila petita per esborrat). La fase 4 els neteja en pujar-los, o en la pujada inicial del primer dispositiu.

## Què cal mirar a la revisió

Cada línia té el seu test a la tasca que s'indica.

1. **«Desfés» després d'esborrar una entrada:** la fila torna amb el mateix `syncId`; no pot quedar alhora viva i amb tombstone. (Tasca 3)
2. **Escriure una fila que ja estava neta:** les pantalles desen còpies d'entitats carregades amb `dirty = false`; el repositori ha de forçar `dirty = true` i no refiar-se del valor per defecte. (Tasca 2)
3. **Esborrar una persona amb entrades i tasques:** la cascada de SQLite no avisa ningú; calen tombstones de la persona, de cada entrada i de cada tasca. (Tasca 3)
4. **Esborrar un canal propi:** les seves entrades passen a `customChannelId = NULL` per la clau forana; han de quedar marcades, o l'altre dispositiu les continuaria veient al canal. (Tasca 3)
5. **Un camí d'escriptura nou que s'oblidi de segellar:** un test recorre els mètodes públics del repositori i falla si n'hi ha cap sense classificar. (Tasca 3)

Fora de tests: `SampleData` i «esborra-ho tot» del menú de debug escriuen pels DAOs sense passar pel repositori. Les files de mostra neixen `dirty = true` pels valors per defecte de l'entitat, i `clearAllTables()` també buida `sync_tombstone`. Només existeixen en debug; n'hi ha prou de comprovar-ho a mà a la tasca 3.

---

## Fitxers

| Fitxer | Canvi |
| --- | --- |
| `app/src/main/java/com/alorma/contactnotes/data/db/Entities.kt` | Columnes noves, índexs únics, `SyncTombstoneEntity`, `newSyncId()` |
| `app/src/main/java/com/alorma/contactnotes/data/db/ConversesDatabase.kt` | `version = 6`, entitat nova |
| `app/src/main/java/com/alorma/contactnotes/data/db/Migrations.kt` | `Migration5To6` |
| `app/src/main/java/com/alorma/contactnotes/data/db/Daos.kt` | `EntryDao` i `TaskDao` passen a `abstract class`; transaccions d'esborrat; segell a les consultes en bloc |
| `app/src/main/java/com/alorma/contactnotes/data/ConversesRepository.kt` | Rellotge injectat; segell a cada escriptura |
| `app/src/main/java/com/alorma/contactnotes/di/AppModules.kt` | Registra `Migration5To6`; el repositori deixa de ser `singleOf` |
| `app/schemas/com.alorma.contactnotes.data.db.ConversesDatabase/6.json` | Generat per Room en compilar |
| `app/src/test/java/com/alorma/contactnotes/testing/FakeDb.kt` | `tombstones`, `markAllClean()`, consultes noves dels DAOs falsos |
| `app/src/test/java/com/alorma/contactnotes/data/SyncIdentityTest.kt` | Nou: segell, tombstones i guarda dels camins d'escriptura |
| `app/src/androidTest/java/com/alorma/contactnotes/data/db/MigrationTest.kt` | Tests 5 → 6 i 1 → 6 |
| `app/src/androidTest/java/com/alorma/contactnotes/data/db/SyncIdentityDaoTest.kt` | Nou: l'SQL real de segells i tombstones |
| `docs/sync/disseny.md`, `CLAUDE.md` | Decisions preses i regla per als camins d'escriptura nous |

---

### Tasca 1: esquema v6 i migració

**Fitxers:** `Entities.kt`, `ConversesDatabase.kt`, `Migrations.kt`, `Daos.kt` (només la consulta `observeList`), `AppModules.kt`, `6.json`, `MigrationTest.kt`.

**Interfícies que produeix:**

```kotlin
// Entities.kt
fun newSyncId(): String  // UUID.randomUUID().toString()

// PersonEntity: camps nous, al final
val syncId: String = newSyncId()
@ColumnInfo(defaultValue = "1") val dirty: Boolean = true

// EntryEntity, TaskEntity, CustomChannelEntity: camps nous, al final
val syncId: String = newSyncId()
@ColumnInfo(defaultValue = "0") val modifiedAt: Long = 0
@ColumnInfo(defaultValue = "1") val dirty: Boolean = true

@Entity(tableName = "sync_tombstone")
data class SyncTombstoneEntity(
  @PrimaryKey val syncId: String,
  val deletedAt: Long,
  @ColumnInfo(defaultValue = "1") val dirty: Boolean = true,
)

// Migrations.kt
class Migration5To6 : Migration(5, 6)
```

Cada una de les quatre taules afegeix `Index("syncId", unique = true)` als seus `indices`.

- [ ] **Pas 1: escriu els tests de migració, que fallen.** A `MigrationTest.kt`:

```kotlin
private val uuid = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")

@Test
fun migrate5To6GivesEveryRowItsOwnSyncIdAndKeepsTheData() {
  helper.createDatabase(TEST_DB, 5).use { db ->
    db.insertPerson(1)
    db.execSQL(
      "INSERT INTO person (id, name, alias, avatarShape, avatarColor, isProtected, archived, notes, createdAt, " +
        "updatedAt) VALUES (2, 'P2', 'Alias', 'Sunny', 'Rose', 1, 1, 'n', 10, 20)",
    )
    db.execSQL("INSERT INTO custom_channel (id, channelType, name, createdAt) VALUES (3, 'CHAT', 'WhatsApp', 30)")
    db.execSQL(
      "INSERT INTO entry (id, personId, kind, channelType, text, occurredAt, customChannelId) " +
        "VALUES (5, 1, 'HEARD', 'CHAT', 'a', 100, 3)",
    )
    db.execSQL(
      "INSERT INTO entry (id, personId, kind, channelType, text, occurredAt) VALUES (9, 2, 'SAID', 'PHONE', 'b', 200)",
    )
    db.execSQL("INSERT INTO task (id, personId, text, dueDate, createdAt, doneAt) VALUES (1, 1, 'open', NULL, 40, NULL)")
    db.execSQL("INSERT INTO task (id, personId, text, dueDate, createdAt, doneAt) VALUES (2, 1, 'done', 7, 40, 50)")
    db.execSQL("INSERT INTO pinned_fact (personId, label, value, position) VALUES (2, 'L', 'V', 0)")
  }

  val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migration5To6())

  val ids =
    db.strings(
      "SELECT syncId FROM person UNION ALL SELECT syncId FROM entry " +
        "UNION ALL SELECT syncId FROM task UNION ALL SELECT syncId FROM custom_channel",
    )
  assertEquals(7, ids.size)
  assertEquals(7, ids.toSet().size)
  assertTrue(ids.all(uuid::matches))
  assertEquals(
    listOf("Alias|Sunny|Rose|1|1|n|10|20|1"),
    db.strings(
      "SELECT alias || '|' || avatarShape || '|' || avatarColor || '|' || isProtected || '|' || archived || '|' || " +
        "notes || '|' || createdAt || '|' || updatedAt || '|' || dirty FROM person WHERE id = 2",
    ),
  )
  assertEquals(
    listOf("5|1|a|3|100|1", "9|2|b|-|200|1"),
    db.strings(
      "SELECT id || '|' || personId || '|' || text || '|' || IFNULL(customChannelId, '-') || '|' || " +
        "modifiedAt || '|' || dirty FROM entry ORDER BY id",
    ),
  )
  assertEquals(listOf("open|40|1", "done|50|1"), db.strings("SELECT text || '|' || modifiedAt || '|' || dirty FROM task ORDER BY id"))
  assertEquals(listOf("WhatsApp|30|1"), db.strings("SELECT name || '|' || modifiedAt || '|' || dirty FROM custom_channel"))
  assertEquals(listOf("2|V"), db.strings("SELECT personId || '|' || value FROM pinned_fact"))
  assertEquals(listOf("0"), db.strings("SELECT COUNT(*) FROM sync_tombstone"))
}

@Test
fun migrate5To6OnAnEmptyDatabase() {
  helper.createDatabase(TEST_DB, 5).close()
  val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migration5To6())
  assertEquals(listOf("0"), db.strings("SELECT COUNT(*) FROM person"))
}

@Test
fun migrate5To6RejectsTwoRowsWithTheSameSyncId() {
  helper.createDatabase(TEST_DB, 5).use { db -> db.insertPerson(1) }
  val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migration5To6())
  val taken = db.strings("SELECT syncId FROM person").single()
  assertThrows(SQLiteConstraintException::class.java) {
    db.execSQL(
      "INSERT INTO person (name, isProtected, archived, createdAt, updatedAt, syncId, dirty) " +
        "VALUES ('P2', 0, 0, 0, 0, '$taken', 1)",
    )
  }
}

@Test
fun migrate1To6RunsEveryMigration() {
  helper.createDatabase(TEST_DB, 1).use { db ->
    db.insertPerson(1)
    db.insertChannel(1, "WHATSAPP")
    db.insertEntry(1, "TELEGRAM")
  }
  val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migration1To2(), Migration2To3(), Migration5To6())
  assertEquals(listOf("CHAT"), db.strings("SELECT channelType FROM entry"))
  assertTrue(db.strings("SELECT syncId FROM person UNION ALL SELECT syncId FROM entry").all(uuid::matches))
}
```

- [ ] **Pas 2: comprova que no compilen** (`Migration5To6` no existeix): `./gradlew :app:compileDebugAndroidTestKotlin`.

- [ ] **Pas 3: afegeix els camps i els índexs a `Entities.kt`, `SyncTombstoneEntity`, i puja `ConversesDatabase` a `version = 6`** amb l'entitat nova a `entities`. No s'hi afegeix cap `AutoMigration`.

- [ ] **Pas 4: afegeix les tres columnes de l'última entrada a `PersonDao.observeList`**, perquè `PersonListItem.lastEntry` és un `EntryEntity` incrustat amb prefix `last_`: `e.syncId AS last_syncId, e.modifiedAt AS last_modifiedAt, e.dirty AS last_dirty`.

- [ ] **Pas 5: escriu `Migration5To6` a `Migrations.kt`.** Per a cada taula, en aquest ordre: afegir columnes, omplir, crear l'índex. Els noms dels índexs han de ser els que Room genera (`index_<taula>_syncId`).

```kotlin
private const val RANDOM_UUID =
  "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-' || " +
    "substr('89ab', abs(random()) % 4 + 1, 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6)))"
```

| Taula | Columnes (`ALTER TABLE … ADD COLUMN`) | `UPDATE` |
| --- | --- | --- |
| `person` | `syncId TEXT NOT NULL DEFAULT ''`, `dirty INTEGER NOT NULL DEFAULT 1` | `syncId = $RANDOM_UUID` |
| `entry` | les dues anteriors i `modifiedAt INTEGER NOT NULL DEFAULT 0` | `syncId = $RANDOM_UUID, modifiedAt = occurredAt` |
| `task` | com `entry` | `syncId = $RANDOM_UUID, modifiedAt = COALESCE(doneAt, createdAt)` |
| `custom_channel` | com `entry` | `syncId = $RANDOM_UUID, modifiedAt = createdAt` |

Després de cada taula: `CREATE UNIQUE INDEX IF NOT EXISTS index_<taula>_syncId ON <taula> (syncId)`. Al final: `CREATE TABLE IF NOT EXISTS sync_tombstone (syncId TEXT NOT NULL, deletedAt INTEGER NOT NULL, dirty INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(syncId))`.

`syncId` no declara valor per defecte a l'entitat i la migració el crea amb `DEFAULT ''`. Si la validació del pas 8 ho rebutja, declara `@ColumnInfo(defaultValue = "")` al camp, regenera `6.json` i torna-ho a provar; no canviïs la migració per recrear les taules.

- [ ] **Pas 6: registra-la a `di/AppModules.kt`:** `.addMigrations(Migration1To2(), Migration2To3(), Migration5To6())`.

- [ ] **Pas 7: compila i comprova l'esquema.** `./gradlew :app:assembleDebug` genera `app/schemas/…/6.json`. Comprova que hi surten les quatre taules amb `syncId` i el seu índex únic, `sync_tombstone`, i que `git status` no mostra cap canvi a `1.json`…`5.json`.

- [ ] **Pas 8: executa els tests de migració en un emulador.**

```bash
ANDROID_SERIAL=<emulador> ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.alorma.contactnotes.data.db.MigrationTest
```

Esperat: tots passen, inclosos els de versions anteriors.

- [ ] **Pas 9: tests JVM i estil.** `./gradlew testDebugUnitTest ktlintCheck`. Esperat: verd; els camps nous tenen valor per defecte i cap test existent s'ha de tocar.

- [ ] **Pas 10: prova d'actualització real en un emulador.** Instal·la l'APK de `main` (v5), crea una persona amb dues entrades i una tasca, instal·la a sobre l'APK d'aquesta branca (`adb install -r`) i comprova que tot hi és i que es pot afegir, editar i esborrar.

- [ ] **Pas 11: commit** (amb el vistiplau de Bernat): `Add the sync identity columns and the v6 migration`.

---

### Tasca 2: segellar cada escriptura

**Fitxers:** `ConversesRepository.kt`, `Daos.kt`, `AppModules.kt`, `FakeDb.kt`, `SyncIdentityTest.kt` (nou), `SyncIdentityDaoTest.kt` (nou).

**Interfícies que consumeix:** els camps de la tasca 1.

**Interfícies que produeix:**

```kotlin
class ConversesRepository(
  personDao: PersonDao, entryDao: EntryDao, pinnedFactDao: PinnedFactDao,
  customChannelDao: CustomChannelDao, taskDao: TaskDao,
  private val now: () -> Long = System::currentTimeMillis,
)

// TaskDao
suspend fun setDoneAt(id: Long, doneAt: Long?, now: Long)   // també posa modifiedAt = :now, dirty = 1

// CustomChannelDao
open suspend fun changeGroup(channel: CustomChannelEntity, channelType: ChannelType, now: Long)
protected abstract suspend fun moveEntries(channelId: Long, channelType: ChannelType, now: Long)  // segella les entrades

// FakeDb
fun markAllClean()   // posa dirty = false a totes les files, com després d'una pujada
```

A Koin, `singleOf(::ConversesRepository)` passa a `single { ConversesRepository(get(), get(), get(), get(), get()) }`, perquè `singleOf` intentaria resoldre el rellotge.

Regla del segell: tota escriptura d'una entitat sencera surt del repositori amb `dirty = true` i `modifiedAt = now()` (`updatedAt = now()` a la persona), sigui quin sigui el valor que portava. `createCustomChannel` i `markTaskDone` conserven els seus paràmetres d'hora i els fan servir també per al segell.

- [ ] **Pas 1: escriu els tests, que fallen.** `SyncIdentityTest.kt`, amb `FakeDb` i un repositori construït amb `now = { 5_000 }` sobre els mateixos DAOs falsos. Cada test parteix de `db.markAllClean()` després de preparar les dades.

| Test | Acció | Comprovació |
| --- | --- | --- |
| `addEntryStampsIt` | `addEntry(EntryEntity(…, dirty = false))` | la fila té `dirty`, `modifiedAt == 5_000` |
| `updateEntryStampsACleanRow` | `updateEntry(clean.copy(text = "x"))` | `dirty`, `modifiedAt == 5_000`, mateix `syncId` |
| `moveEntryStampsIt` | `moveEntry(entry, other.id)` | `dirty`, `modifiedAt == 5_000`, `personId == other.id` |
| `savePersonStampsANewOne` | `savePerson(new, facts)` | `dirty`, `updatedAt == 5_000` |
| `savePersonWithOnlyFactsChangedStampsThePerson` | `savePerson(clean, altresFets)` | `dirty`, `updatedAt == 5_000` |
| `setArchivedAndSetProtectedStampThePerson` | cada una sobre una persona neta | `dirty`, `updatedAt == 5_000` |
| `saveTaskStampsNewAndEdited` | `saveTask` amb `id == 0` i amb una de neta | `dirty`, `modifiedAt == 5_000` |
| `markTaskDoneAndReopenStampIt` | `markTaskDone(task, at = 7_000)`; després de netejar, `reopenTask(task)` | `modifiedAt == 7_000` i després `5_000`; `dirty` tots dos cops |
| `createAndRenameCustomChannelStampIt` | `createCustomChannel(CHAT, "A", now = 7_000)`; `renameCustomChannel` sobre un de net | `modifiedAt == 7_000` / `5_000`; `dirty` |
| `changingAChannelGroupStampsItsEntries` | `changeCustomChannelGroup(channel, SOCIAL)` amb dues entrades netes al canal i una fora | canal i les dues entrades `dirty` amb `5_000`; la de fora continua neta |
| `aRejectedChannelNameStampsNothing` | `renameCustomChannel` amb un nom ja pres | cap fila `dirty` |
| `readsDoNotStamp` | `getPerson`, `getEntries`, `observePeople().first()` | cap fila `dirty` |

- [ ] **Pas 2: comprova que fallen:** `./gradlew :app:testDebugUnitTest --tests '*SyncIdentityTest'`.

- [ ] **Pas 3: implementa el rellotge i el segell al repositori**, i els dos canvis de signatura dels DAOs amb el seu SQL (`UPDATE task SET doneAt = :doneAt, modifiedAt = :now, dirty = 1 WHERE id = :id`; `UPDATE entry SET channelType = :channelType, modifiedAt = :now, dirty = 1 WHERE customChannelId = :channelId`). Actualitza els DAOs falsos i `markAllClean()` a `FakeDb`, i el registre de Koin.

- [ ] **Pas 4: comprova que passen**, i que la resta també: `./gradlew testDebugUnitTest`. Si algun test existent comparava `updatedAt` amb un valor que posava la pantalla, ara el posa el repositori: ajusta el test, no el segell.

- [ ] **Pas 5: l'SQL real.** `SyncIdentityDaoTest.kt`, Room en memòria com `TaskDaoTest`:

```kotlin
@Test fun setDoneAtStampsTheTask()            // modifiedAt = now, dirty = 1, doneAt canviat
@Test fun changeGroupStampsTheEntriesOfTheChannelOnly()
@Test fun observeListReadsTheLastEntryWithItsSyncColumns()  // lastEntry.syncId == el de l'entrada inserida
```

Executa'ls a l'emulador amb `-Pandroid.testInstrumentationRunnerArguments.class=com.alorma.contactnotes.data.db.SyncIdentityDaoTest`.

- [ ] **Pas 6: commit** (amb el vistiplau de Bernat): `Stamp every write with its modification time and the dirty mark`.

---

### Tasca 3: registrar els esborrats

**Fitxers:** `Daos.kt`, `ConversesRepository.kt`, `FakeDb.kt`, `SyncIdentityTest.kt`, `SyncIdentityDaoTest.kt`, `CustomChannelDaoTest.kt` i `TaskDaoTest.kt` (si criden mètodes que canvien), `docs/sync/disseny.md`, `CLAUDE.md`.

**Interfícies que produeix.** `EntryDao` i `TaskDao` passen d'`interface` a `abstract class`, com `PersonDao`. A cada DAO, `delete` passa a ser `protected` i l'únic esborrat públic és el que deixa tombstone:

```kotlin
// PersonDao: tombstone de la persona, de les seves entrades i de les seves tasques, i després l'esborra
@Transaction open suspend fun deleteWithTombstones(person: PersonEntity, now: Long)

// EntryDao
@Transaction open suspend fun insertLive(entry: EntryEntity): Long          // esborra el tombstone d'aquest syncId i insereix
@Transaction open suspend fun deleteWithTombstone(entry: EntryEntity, now: Long)
@Transaction open suspend fun deleteInRangeWithTombstones(
  personId: Long, channelType: ChannelType, from: Long, to: Long, now: Long,
): Int

// TaskDao
@Transaction open suspend fun deleteWithTombstone(task: TaskEntity, now: Long)

// CustomChannelDao: segella les entrades del canal, tombstone del canal, i després l'esborra
@Transaction open suspend fun deleteWithTombstone(channel: CustomChannelEntity, now: Long)

// FakeDb
val tombstones: MutableStateFlow<List<SyncTombstoneEntity>>
```

Els tombstones s'escriuen amb `INSERT OR REPLACE INTO sync_tombstone (syncId, deletedAt, dirty)`: un per a la fila (`VALUES`) o un per fila afectada (`SELECT syncId, :now, 1 FROM entry WHERE …`). Cada transacció es compon de consultes `protected abstract` que el DAO fals implementa, perquè el codi de la transacció sigui el mateix a Room i a `FakeDb`. El repositori crida aquests mètodes amb `now()`; `addEntry` crida `insertLive`.

- [ ] **Pas 1: escriu els tests, que fallen.** A `SyncIdentityTest.kt`:

| Test | Acció | Comprovació |
| --- | --- | --- |
| `deleteEntryLeavesATombstone` | `deleteEntry(entry)` | `tombstones == [SyncTombstoneEntity(entry.syncId, 5_000, true)]` |
| `undoingAnEntryDeleteRemovesItsTombstone` | `deleteEntry(entry)`; `addEntry(entry)` | sense tombstones; la fila hi és amb el mateix `syncId`, `dirty`, `modifiedAt == 5_000` |
| `deletePersonLeavesTombstonesForItsEntriesAndTasks` | persona amb 2 entrades, 1 tasca i 1 fet; una altra persona amb 1 entrada | 4 tombstones amb els `syncId` de la persona, les 2 entrades i la tasca; cap de l'altra persona ni del fet |
| `deleteEntriesInRangeLeavesATombstonePerEntry` | 3 entrades, 2 dins del rang | retorna 2; 2 tombstones amb els seus `syncId` |
| `deleteTaskLeavesATombstone` | `deleteTask(task)` | 1 tombstone |
| `deletingAChannelStampsItsEntriesAndLeavesATombstone` | canal amb 2 entrades netes | 1 tombstone (el canal); les 2 entrades `dirty`, `modifiedAt == 5_000`, `customChannelId == null` |
| `deletingTheSameSyncIdTwiceKeepsOneTombstone` | esborra, desfés, torna a esborrar | 1 tombstone |
| `everyWriteMethodOfTheRepositoryIsCovered` | vegeu a sota | |

```kotlin
/** Si afegeixes un mètode d'escriptura al repositori: segella'l, afegeix-ne el test i apunta'l aquí. */
private val writes =
  setOf(
    "savePerson", "deletePerson", "setArchived", "setProtected",
    "addEntry", "updateEntry", "deleteEntry", "moveEntry", "deleteEntriesInRange",
    "saveTask", "deleteTask", "markTaskDone", "reopenTask",
    "createCustomChannel", "renameCustomChannel", "changeCustomChannelGroup", "saveCustomChannel", "deleteCustomChannel",
  )
private val reads = Regex("^(observe|get|is|count).*")

@Test
fun everyWriteMethodOfTheRepositoryIsCovered() {
  val public =
    ConversesRepository::class.java.declaredMethods
      .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic && '$' !in it.name }
      .map { it.name }
      .toSet()
  assertEquals(writes, public.filterNot(reads::matches).toSet())
}
```

- [ ] **Pas 2: comprova que fallen:** `./gradlew :app:testDebugUnitTest --tests '*SyncIdentityTest'`.

- [ ] **Pas 3: implementa les transaccions als DAOs, els DAOs falsos i les crides del repositori.** Els DAOs falsos mantenen el que ja imiten de les claus foranes (cascada en esborrar una persona, `SET NULL` en esborrar un canal).

- [ ] **Pas 4: comprova que passen:** `./gradlew testDebugUnitTest`.

- [ ] **Pas 5: l'SQL real.** A `SyncIdentityDaoTest.kt`, amb `PRAGMA foreign_keys` com el deixa Room:

```kotlin
@Test fun deletingAPersonLeavesTombstonesForThePersonItsEntriesAndItsTasks()  // i les files ja no hi són
@Test fun deleteInRangeLeavesATombstoneOnlyForTheDeletedEntries()
@Test fun deletingAChannelStampsItsEntriesAndSetsThemToNoChannel()
@Test fun insertLiveRemovesTheTombstoneOfThatSyncId()
```

Executa tots els tests instrumentats de `data.db` a l'emulador (els de `CustomChannelDaoTest` i `TaskDaoTest` també, perquè les signatures han canviat).

- [ ] **Pas 6: comprova el debug a mà** en un emulador: «Dades de mostra» crea persones que es veuen i s'editen; «esborra-ho tot» deixa l'app buida i funcional.

- [ ] **Pas 7: documenta-ho.**
  - `docs/sync/disseny.md`: a «Canvis a Room», la fase 1 passa a «construïda»; `dirty` es marca als DAOs i al repositori **(B)**; el cursor va a DataStore a la fase 4 **(C)**; treu «On es marca `dirty`» de «Queda per al pla».
  - `CLAUDE.md`, a la línia de Room de «Architecture»: tota escriptura nova de `ConversesRepository` ha de segellar `dirty` i `modifiedAt`, tot esborrat nou ha de deixar un `sync_tombstone`, i `SyncIdentityTest` falla si un mètode d'escriptura no hi és classificat.

- [ ] **Pas 8: `./gradlew ktlintCheck testDebugUnitTest :app:assembleDebug`** i **commit** (amb el vistiplau de Bernat): `Record deletions as sync tombstones`.

---

## En acabar

- **Notes de versió:** cap punt. No hi ha res que l'usuari pugui veure ni mesurar.
- **Privacitat externa:** cap canvi; no surt res del telèfon.
- **Baseline Profile:** no cal regenerar-lo per aquesta fase (canvis a la capa de dades, cap pantalla ni l'arrencada).
- **Següent:** la fase 2 (criptografia, JVM pur) no depèn del spike i es pot planificar de seguida; les fases 3 a 6 esperen el resultat de `fase-0-spike.md`.
