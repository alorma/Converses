package com.alorma.contactnotes.data.sync.crypto

import java.security.SecureRandom

/** Versió del format xifrat. Va a les dades associades de cada blob i al document del compte. */
const val SYNC_FORMAT = 1

/**
 * El secret de recuperació (128 bits), que es genera al primer dispositiu i no surt mai cap al servidor. Les paraules
 * de seguretat en són la forma llegible. Qui el té pot obrir la clau de dades, i per tant totes les dades del compte.
 */
class RecoverySecret(
  bytes: ByteArray,
) {
  private val secret = bytes.copyOf()

  init {
    require(secret.size == Bip39.ENTROPY_BYTES) { "The recovery secret must be ${Bip39.ENTROPY_BYTES} bytes" }
  }

  /** Una còpia a cada lectura. */
  val bytes: ByteArray get() = secret.copyOf()

  /** Les 12 paraules de seguretat, sempre en anglès. */
  val words: List<String> get() = Bip39.encode(secret)

  override fun equals(other: Any?): Boolean = other is RecoverySecret && secret.contentEquals(other.secret)

  override fun hashCode(): Int = secret.contentHashCode()

  /** Sense el contingut: un secret no ha de sortir mai en un log ni en un missatge d'error. */
  override fun toString(): String = "RecoverySecret"

  companion object {
    fun generate(random: SecureRandom = SecureRandom()): RecoverySecret =
      RecoverySecret(ByteArray(Bip39.ENTROPY_BYTES).also(random::nextBytes))

    /** `null` si no són 12 paraules vàlides; per saber què hi falla, [Bip39.decode]. */
    fun fromWords(words: List<String>): RecoverySecret? =
      (Bip39.decode(words) as? Bip39Result.Valid)?.let { RecoverySecret(it.entropy) }
  }
}

/** La clau de dades (256 bits) que xifra tots els registres del compte. Al servidor només n'hi ha la versió embolcallada. */
class DataKey(
  bytes: ByteArray,
) {
  private val key = bytes.copyOf()

  init {
    require(key.size == SIZE_BYTES) { "The data key must be $SIZE_BYTES bytes" }
  }

  /** Una còpia a cada lectura. */
  val bytes: ByteArray get() = key.copyOf()

  override fun equals(other: Any?): Boolean = other is DataKey && key.contentEquals(other.key)

  override fun hashCode(): Int = key.contentHashCode()

  override fun toString(): String = "DataKey"

  companion object {
    const val SIZE_BYTES = 32

    fun generate(random: SecureRandom = SecureRandom()): DataKey =
      DataKey(ByteArray(SIZE_BYTES).also(random::nextBytes))
  }
}
