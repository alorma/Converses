# Sincronització, fase 2: criptografia. Pla d'implementació

> **Per a qui l'executi amb agents:** SUB-SKILL OBLIGATÒRIA: superpowers:subagent-driven-development o superpowers:executing-plans, tasca a tasca. Els passos porten caselles (`- [ ]`).

**Objectiu:** les peces de criptografia de la sincronització (paraules de seguretat, claus, embolcall de la clau de dades i xifratge de registres), provades amb vectors coneguts i sense cap dependència d'Android ni de Firebase.

**Arquitectura:** un paquet `data/sync/crypto` de Kotlin pur sobre `javax.crypto` i `java.security`. Ningú el fa servir encara: la fase 3 hi connecta el compte i el magatzem de la clau, i la fase 4 el motor.

**Stack:** `javax.crypto` (AES-256-GCM, HMAC-SHA256), `java.security` (SHA-256, `SecureRandom`). Cap biblioteca nova.

**Disseny:** `docs/sync/disseny.md`, secció «Claus».

## Restriccions globals

- Cap criptografia inventada: només BIP39, HKDF (RFC 5869) i AES-256-GCM, cadascun comprovat amb vectors publicats.
- Secret de recuperació: 128 bits. Clau de dades: 256 bits. Nonce: 12 bytes aleatoris per xifratge. Etiqueta de GCM: 128 bits.
- Les 12 paraules són de la llista anglesa de BIP39, amb la seva suma de control.
- Les dades associades (AAD) inclouen l'UID, l'identificador del registre i la versió del format.
- Paraules incorrectes o dades manipulades no llancen cap excepció cap amunt: es detecten i es tornen com a resultat.
- Cap clau ni secret surt en un `toString()`, un missatge d'error o un log.
- Tot al paquet `com.alorma.contactnotes.data.sync.crypto`; tests JVM a `app/src/test`. Cap canvi a Room, a la UI ni al manifest. No surt res del telèfon.
- `./gradlew ktlintCheck testDebugUnitTest` net abans de cada commit. Feina a la branca `sync-fase-2`, sobre `sync-fase-1`.

## Decisions preses en aquest pla

- **(C)** Primitives de la plataforma, no Tink: tot el que cal ja hi és, i HKDF són quinze línies sobre HMAC que es comproven amb els vectors de l'RFC. Tink afegiria una dependència i pes a l'APK per no escriure-les.
- **(C)** La llista de paraules va en un fitxer Kotlin (`Bip39Words.kt`), no en un recurs: el codi continua sent JVM pur i el test en comprova el SHA-256 contra el de la llista oficial (`2f5eed53…dbda`).
- **(C)** La clau d'embolcall es deriva amb `HKDF-SHA256(ikm = secret, salt = buit, info = "converses/sync/v1/wrap")`. El secret ja té 128 bits d'entropia; no cal cap derivació lenta com les de contrasenyes.
- **(C)** El format xifrat és `nonce (12 bytes) || text xifrat i etiqueta`. La versió del format no va dins del blob: va a l'AAD i al document del compte (`format`), com diu el disseny.
- **(C)** L'AAD es construeix amb camps prefixats per la seva llargada (4 bytes), perquè dos conjunts de camps diferents no puguin donar mai els mateixos bytes.
- **(C)** El format JSON de cada tipus de registre no entra en aquesta fase: depèn de com el motor resol referències i enums desconeguts, i es decideix a la fase 4.

## Què cal mirar a la revisió

1. **Paraules amb majúscules, espais de més o salts de línia** (enganxades d'un gestor de contrasenyes): s'han d'acceptar. (Tasca 1)
2. **Una paraula que no és a la llista:** el resultat ha de dir quines posicions fallen, per pintar-les en vermell (13j). (Tasca 1)
3. **Dotze paraules vàlides amb la suma de control malament** (dues paraules intercanviades): no és el mateix error que «no obren aquest compte». (Tasca 1)
4. **Un registre copiat a un altre identificador o a un altre compte:** no s'ha de poder desxifrar. (Tasca 4)
5. **Un blob truncat o buit** que arriba del servidor: resultat buit, no una excepció. (Tasques 2 i 4)

---

### Tasca 1: paraules de seguretat (BIP39)

**Fitxers:** crea `data/sync/crypto/Bip39.kt` i `Bip39Words.kt`; test `data/sync/crypto/Bip39Test.kt`.

**Produeix:**

```kotlin
object Bip39 {
  const val WORD_COUNT = 12
  const val ENTROPY_BYTES = 16
  fun encode(entropy: ByteArray): List<String>        // exigeix 16 bytes
  fun decode(words: List<String>): Bip39Result        // normalitza: trim i minúscules
  fun isWord(word: String): Boolean
}

sealed interface Bip39Result {
  class Valid(val entropy: ByteArray) : Bip39Result
  data class WrongLength(val count: Int) : Bip39Result
  data class UnknownWords(val positions: List<Int>) : Bip39Result   // índexs des de 0
  data object BadChecksum : Bip39Result
}

internal val BIP39_ENGLISH: List<String>   // 2048 paraules, en l'ordre oficial
```

- [ ] **Pas 1: tests que fallen.**

| Test | Comprovació |
| --- | --- |
| `theWordListIsTheOfficialOne` | 2048 paraules; SHA-256 de les paraules acabades cadascuna en `\n` = `2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda` |
| `encodesTheReferenceVectors` | `00…00` → `abandon ×11 about`; `7f…7f` → `legal winner thank year wave sausage worth useful legal winner thank yellow`; `80…80` → `letter advice cage absurd amount doctor acoustic avoid letter advice cage above`; `ff…ff` → `zoo ×11 wrong` |
| `decodesTheReferenceVectors` | les quatre frases tornen la seva entropia |
| `decodeIsTheInverseOfEncode` | 200 entropies d'un `Random(1)` |
| `acceptsUpperCaseAndSurroundingSpaces` | `" Legal"`, `"WINNER\n"`… → `Valid` |
| `reportsThePositionsOfUnknownWords` | paraules 3 i 7 canviades per `"xyz"` i `""` → `UnknownWords(listOf(2, 6))` |
| `rejectsAWrongChecksum` | dues paraules diferents intercanviades → `BadChecksum` |
| `rejectsAWrongNumberOfWords` | 11 i 24 paraules → `WrongLength(11)`, `WrongLength(24)` |
| `encodeRejectsEntropyOfTheWrongSize` | 15 i 32 bytes → `IllegalArgumentException` |

- [ ] **Pas 2:** `./gradlew :app:testDebugUnitTest --tests '*Bip39Test'` falla perquè no compila.
- [ ] **Pas 3:** genera `Bip39Words.kt` a partir de la llista oficial (`bitcoin/bips`, `bip-0039/english.txt`) i implementa `Bip39`: 128 bits d'entropia més els 4 primers bits del seu SHA-256, en 12 grups d'11 bits.
- [ ] **Pas 4:** els tests passen.
- [ ] **Pas 5:** commit `Add the BIP39 security words`.

### Tasca 2: HKDF i AES-GCM

**Fitxers:** crea `Hkdf.kt` i `AesGcm.kt`; tests `HkdfTest.kt` i `AesGcmTest.kt`.

**Produeix:**

```kotlin
internal object Hkdf {
  fun sha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray
}

internal object AesGcm {
  const val NONCE_BYTES = 12
  /** nonce || text xifrat i etiqueta. */
  fun seal(key: ByteArray, plaintext: ByteArray, aad: ByteArray, random: SecureRandom): ByteArray
  /** `null` si la clau, l'AAD o les dades no quadren, o si el blob és massa curt. */
  fun open(key: ByteArray, sealed: ByteArray, aad: ByteArray): ByteArray?
}

/** Camps prefixats per la seva llargada (4 bytes, big-endian). */
internal fun aadOf(vararg fields: String): ByteArray
```

- [ ] **Pas 1: tests que fallen.**

| Test | Comprovació |
| --- | --- |
| `rfc5869TestCase1` | IKM `0b`×22, salt `000102…0c`, info `f0f1…f9`, L = 42 → `3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865` |
| `rfc5869TestCase3` | IKM `0b`×22, salt i info buits, L = 42 → `8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8` |
| `nistGcmVectorWithAnEmptyMessage` | clau de 32 zeros, nonce de 12 zeros (un `SecureRandom` fals), sense text ni AAD → el blob és el nonce i l'etiqueta `530f8afbc74536b9a963b4f1c4cb738b` |
| `opensWhatItSeals` | text i AAD arbitraris tornen iguals |
| `everySealUsesAFreshNonce` | dos segells del mateix text són diferents |
| `aWrongKeyAadOrAFlippedBitDoesNotOpen` | cada cas → `null` |
| `aTruncatedOrEmptyBlobDoesNotOpen` | 0, 11 i 27 bytes → `null` |
| `aadFieldsCannotBeConfusedByMovingABoundary` | `aadOf("ab", "c")` ≠ `aadOf("a", "bc")` |

- [ ] **Pas 2:** fallen. **Pas 3:** implementa (`Mac "HmacSHA256"`, `Cipher "AES/GCM/NoPadding"` amb `GCMParameterSpec(128, nonce)`). **Pas 4:** passen.
- [ ] **Pas 5:** commit `Add HKDF and the AES-GCM sealing`.

### Tasca 3: claus i embolcall

**Fitxers:** crea `SyncKeys.kt` i `KeyWrap.kt`; test `KeyWrapTest.kt`.

**Consumeix:** `Hkdf`, `AesGcm`, `aadOf`, `Bip39`.

**Produeix:**

```kotlin
const val SYNC_FORMAT = 1

/** El secret de recuperació (128 bits). Les paraules de seguretat en són la forma llegible. */
class RecoverySecret(bytes: ByteArray) {            // exigeix 16 bytes; en guarda una còpia
  val bytes: ByteArray                              // una còpia a cada lectura
  val words: List<String>                           // Bip39.encode
  companion object {
    fun generate(random: SecureRandom = SecureRandom()): RecoverySecret
    fun fromWords(words: List<String>): RecoverySecret?   // null si Bip39 no les dona per vàlides
  }
}

/** La clau de dades (256 bits) que xifra tots els registres. */
class DataKey(bytes: ByteArray) {                   // exigeix 32 bytes
  val bytes: ByteArray
  companion object { fun generate(random: SecureRandom = SecureRandom()): DataKey }
}

object KeyWrap {
  fun wrap(dataKey: DataKey, secret: RecoverySecret, uid: String, random: SecureRandom = SecureRandom()): ByteArray
  /** `null` si les paraules no són les d'aquest compte. */
  fun unwrap(wrapped: ByteArray, secret: RecoverySecret, uid: String): DataKey?
}
```

Les dues classes de clau tenen `equals` i `hashCode` pel contingut, i `toString()` torna el nom de la classe i prou. AAD de l'embolcall: `aadOf("wrap", SYNC_FORMAT.toString(), uid)`.

- [ ] **Pas 1: tests que fallen.**

| Test | Comprovació |
| --- | --- |
| `unwrapsWithTheSameWords` | `unwrap(wrap(key, secret, "u1"), RecoverySecret.fromWords(secret.words)!!, "u1") == key` |
| `otherWordsDoNotUnwrap` | un altre secret → `null` |
| `anotherAccountDoesNotUnwrap` | uid `"u2"` → `null` |
| `aTamperedWrappedKeyDoesNotUnwrap` | un bit canviat → `null` |
| `theWrappedKeyHasTheExpectedSize` | 12 + 32 + 16 = 60 bytes |
| `generatedSecretsAndKeysDiffer` | dos `generate()` són diferents; mides 16 i 32 |
| `keysDoNotLeakInToString` | `toString()` no conté cap byte de la clau en hexadecimal ni cap paraula |
| `fromWordsRejectsInvalidWords` | suma de control malament → `null` |
| `rejectsKeysOfTheWrongSize` | `RecoverySecret(ByteArray(15))`, `DataKey(ByteArray(16))` → `IllegalArgumentException` |

- [ ] **Passos 2-4:** fallen, implementa, passen.
- [ ] **Pas 5:** commit `Add the sync keys and the data key wrapping`.

### Tasca 4: xifratge de registres

**Fitxers:** crea `RecordCipher.kt`; test `RecordCipherTest.kt`; actualitza `docs/sync/disseny.md`.

**Consumeix:** `DataKey`, `AesGcm`, `aadOf`, `SYNC_FORMAT`.

**Produeix:**

```kotlin
/** Xifra i desxifra els registres d'un compte. Un registre només s'obre amb el seu compte i el seu identificador. */
class RecordCipher(
  private val key: DataKey,
  private val uid: String,
  private val random: SecureRandom = SecureRandom(),
) {
  fun encrypt(recordId: String, plaintext: ByteArray): ByteArray
  /** `null` si no és un registre d'aquest compte amb aquest identificador, o si l'han manipulat. */
  fun decrypt(recordId: String, sealed: ByteArray): ByteArray?
}
```

AAD: `aadOf("record", SYNC_FORMAT.toString(), uid, recordId)`.

- [ ] **Pas 1: tests que fallen.**

| Test | Comprovació |
| --- | --- |
| `decryptsWhatItEncrypts` | text amb accents i emojis, 0 bytes i 64 KB |
| `aRecordMovedToAnotherIdDoesNotDecrypt` | `decrypt("b", encrypt("a", …))` → `null` |
| `aRecordMovedToAnotherAccountDoesNotDecrypt` | un altre `uid`, mateixa clau → `null` |
| `anotherKeyDoesNotDecrypt` | → `null` |
| `theSameRecordEncryptsDifferentlyEachTime` | dos blobs diferents |
| `theCiphertextDoesNotContainThePlaintext` | el blob no conté els bytes de `"Marta Soler"` |
| `aTruncatedOrEmptyBlobDoesNotDecrypt` | 0 i 20 bytes → `null` |
| `theOverheadIs28Bytes` | mida = text + 12 + 16 |

- [ ] **Passos 2-4:** fallen, implementa, passen.
- [ ] **Pas 5:** a `docs/sync/disseny.md`, secció «Claus»: la fase 2 és construïda, amb les decisions d'aquest pla (plataforma i no Tink, derivació de la clau d'embolcall, format del blob, AAD); treu «Tink o plataforma» de «Queda per al pla».
- [ ] **Pas 6:** `./gradlew ktlintCheck testDebugUnitTest` i commit `Add the record encryption`.

## En acabar

- **Notes de versió, privacitat externa, Baseline Profile:** cap canvi. És codi que encara no crida ningú.
- **Següent:** les fases 3 a 6 necessiten el spike (`fase-0-spike.md`) i, per tant, la consola de Firebase.
