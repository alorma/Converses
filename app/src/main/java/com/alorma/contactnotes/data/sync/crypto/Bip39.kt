package com.alorma.contactnotes.data.sync.crypto

import java.security.MessageDigest

/** El que surt de llegir unes paraules de seguretat. */
sealed interface Bip39Result {
  class Valid(
    val entropy: ByteArray,
  ) : Bip39Result

  /** No són 12 paraules. */
  data class WrongLength(
    val count: Int,
  ) : Bip39Result

  /** Paraules que no són a la llista; [positions] comença per 0. */
  data class UnknownWords(
    val positions: List<Int>,
  ) : Bip39Result

  /** Totes són de la llista, però no formen una frase vàlida: n'hi ha alguna de canviada o fora de lloc. */
  data object BadChecksum : Bip39Result
}

/**
 * Les paraules de seguretat (`docs/sync`): la forma llegible del secret de recuperació. És BIP39 amb la llista anglesa
 * i 128 bits: l'entropia i els 4 primers bits del seu SHA-256 fan 132 bits, que són 12 paraules d'11 bits.
 */
object Bip39 {
  const val WORD_COUNT = 12
  const val ENTROPY_BYTES = 16

  private const val BITS_PER_WORD = 11
  private const val CHECKSUM_BITS = 4

  private val indexOfWord: Map<String, Int> by lazy {
    BIP39_ENGLISH.withIndex().associate { (index, word) -> word to index }
  }

  private fun normalize(word: String) = word.trim().lowercase()

  fun isWord(word: String): Boolean = normalize(word) in indexOfWord

  fun encode(entropy: ByteArray): List<String> {
    require(entropy.size == ENTROPY_BYTES) { "The entropy must be $ENTROPY_BYTES bytes" }
    val bits = entropy.flatMap(::bitsOf) + checksumBits(entropy)
    return bits.chunked(BITS_PER_WORD).map { word -> BIP39_ENGLISH[word.fold(0) { acc, bit -> acc * 2 + bit }] }
  }

  /** Accepta majúscules i espais al voltant de cada paraula, com surten d'enganxar-les. */
  fun decode(words: List<String>): Bip39Result {
    if (words.size != WORD_COUNT) return Bip39Result.WrongLength(words.size)
    val indices = words.map { indexOfWord[normalize(it)] }
    val unknown = indices.withIndex().filter { it.value == null }.map { it.index }
    if (unknown.isNotEmpty()) return Bip39Result.UnknownWords(unknown)

    val bits = indices.flatMap { index -> (BITS_PER_WORD - 1 downTo 0).map { (index!! shr it) and 1 } }
    val entropy =
      bits
        .take(ENTROPY_BYTES * Byte.SIZE_BITS)
        .chunked(Byte.SIZE_BITS)
        .map { byte -> byte.fold(0) { acc, bit -> acc * 2 + bit }.toByte() }
        .toByteArray()
    val checksumMatches = bits.takeLast(CHECKSUM_BITS) == checksumBits(entropy)
    return if (checksumMatches) Bip39Result.Valid(entropy) else Bip39Result.BadChecksum
  }

  private fun bitsOf(byte: Byte): List<Int> = (Byte.SIZE_BITS - 1 downTo 0).map { (byte.toInt() shr it) and 1 }

  private fun checksumBits(entropy: ByteArray): List<Int> =
    bitsOf(MessageDigest.getInstance("SHA-256").digest(entropy).first()).take(CHECKSUM_BITS)
}
