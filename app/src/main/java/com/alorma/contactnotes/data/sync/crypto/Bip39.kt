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

  private val notALetter = Regex("[^A-Za-z]+")

  /**
   * Només en queden les lletres: la llista és ASCII, i així no hi fan res els espais, les majúscules ni els caràcters
   * invisibles que s'enganxen en copiar (un espai d'amplada zero no es veu, i la paraula semblaria bona).
   */
  private fun normalize(word: String) = word.replace(notALetter, "").lowercase()

  /**
   * Separa una frase enganxada en paraules, vingui com vingui: amb espais, salts de línia, comes o numerada, com la
   * desen els gestors de contrasenyes. És l'única manera de partir-la, perquè cada pantalla no ho faci a la seva.
   */
  fun split(text: String): List<String> = text.split(notALetter).filter { it.isNotEmpty() }.map { it.lowercase() }

  fun isWord(word: String): Boolean = normalize(word) in indexOfWord

  /**
   * Les primeres [limit] paraules de la llista que comencen per [prefix], per ajudar a escriure-les. La llista està
   * ordenada alfabèticament, i per això n'hi ha prou de trobar la primera i seguir endavant.
   */
  fun suggestions(
    prefix: String,
    limit: Int,
  ): List<String> {
    val start = normalize(prefix)
    if (start.isEmpty() || limit <= 0) return emptyList()
    val found = BIP39_ENGLISH.binarySearch(start)
    val first = if (found >= 0) found else -(found + 1)
    return BIP39_ENGLISH.subList(first, BIP39_ENGLISH.size).take(limit).takeWhile { it.startsWith(start) }
  }

  fun encode(entropy: ByteArray): List<String> {
    require(entropy.size == ENTROPY_BYTES) { "The entropy must be $ENTROPY_BYTES bytes" }
    val bits = entropy.flatMap(::bitsOf) + checksumBits(entropy)
    return bits.chunked(BITS_PER_WORD).map { word -> BIP39_ENGLISH[word.fold(0) { acc, bit -> acc * 2 + bit }] }
  }

  /** Accepta majúscules i espais al voltant de cada paraula; una frase sencera es parteix abans amb [split]. */
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
