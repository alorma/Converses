package com.alorma.contactnotes.data.sync.crypto

import java.security.MessageDigest
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Bip39Test {
  private fun bytes(value: Int) = ByteArray(Bip39.ENTROPY_BYTES) { value.toByte() }

  private fun words(phrase: String) = phrase.split(" ")

  /** Els vectors de 128 bits de la referència de BIP39 (Trezor). */
  private val vectors =
    listOf(
      bytes(0x00) to
        words("abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"),
      bytes(0x7f) to words("legal winner thank year wave sausage worth useful legal winner thank yellow"),
      bytes(0x80) to words("letter advice cage absurd amount doctor acoustic avoid letter advice cage above"),
      bytes(0xff) to words("zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong"),
    )

  private fun entropyOf(result: Bip39Result): ByteArray = (result as Bip39Result.Valid).entropy

  @Test
  fun theWordListIsTheOfficialOne() {
    assertEquals(2048, BIP39_ENGLISH.size)
    val file = BIP39_ENGLISH.joinToString(separator = "") { "$it\n" }.toByteArray()
    val sha256 = MessageDigest.getInstance("SHA-256").digest(file).joinToString("") { "%02x".format(it) }
    assertEquals("2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda", sha256)
  }

  @Test
  fun encodesTheReferenceVectors() {
    vectors.forEach { (entropy, words) -> assertEquals(words, Bip39.encode(entropy)) }
  }

  @Test
  fun decodesTheReferenceVectors() {
    vectors.forEach { (entropy, words) -> assertArrayEquals(entropy, entropyOf(Bip39.decode(words))) }
  }

  @Test
  fun decodeIsTheInverseOfEncode() {
    val random = Random(1)
    repeat(200) {
      val entropy = random.nextBytes(Bip39.ENTROPY_BYTES)
      val words = Bip39.encode(entropy)
      assertEquals(Bip39.WORD_COUNT, words.size)
      assertArrayEquals(entropy, entropyOf(Bip39.decode(words)))
    }
  }

  @Test
  fun acceptsUpperCaseAndSurroundingSpaces() {
    val typed = words("legal winner thank year wave sausage worth useful legal winner thank yellow").toMutableList()
    typed[0] = " Legal"
    typed[1] = "WINNER\n"
    typed[11] = "\tyellow  "

    assertArrayEquals(bytes(0x7f), entropyOf(Bip39.decode(typed)))
  }

  @Test
  fun splitsAPastedPhraseHoweverItIsSeparated() {
    val twelve = words("legal winner thank year wave sausage worth useful legal winner thank yellow")

    listOf(
      twelve.joinToString(" "),
      twelve.joinToString("  "),
      twelve.joinToString("\n") + "\n",
      "  " + twelve.joinToString("\t") + "  ",
      twelve.joinToString(", "),
      twelve.mapIndexed { index, word -> "${index + 1}. $word" }.joinToString("\n"),
      "﻿" + twelve.joinToString("​  ").uppercase(),
    ).forEach { pasted ->
      assertEquals(pasted, twelve, Bip39.split(pasted))
      assertArrayEquals(bytes(0x7f), entropyOf(Bip39.decode(Bip39.split(pasted))))
    }
    assertEquals(emptyList<String>(), Bip39.split("  \n "))
  }

  @Test
  fun ignoresInvisibleCharactersStuckToAWord() {
    val typed = words("legal winner thank year wave sausage worth useful legal winner thank yellow").toMutableList()
    typed[0] = "​legal"
    typed[11] = "yellow﻿"

    assertArrayEquals(bytes(0x7f), entropyOf(Bip39.decode(typed)))
    assertTrue(Bip39.isWord("​zoo"))
  }

  @Test
  fun reportsThePositionsOfUnknownWords() {
    val typed = words("legal winner thank year wave sausage worth useful legal winner thank yellow").toMutableList()
    typed[2] = "xyz"
    typed[6] = ""

    assertEquals(Bip39Result.UnknownWords(listOf(2, 6)), Bip39.decode(typed))
  }

  @Test
  fun rejectsAWrongChecksum() {
    val swapped = words("winner legal thank year wave sausage worth useful legal winner thank yellow")

    assertEquals(Bip39Result.BadChecksum, Bip39.decode(swapped))
  }

  @Test
  fun rejectsAWrongNumberOfWords() {
    val twelve = words("legal winner thank year wave sausage worth useful legal winner thank yellow")

    assertEquals(Bip39Result.WrongLength(11), Bip39.decode(twelve.dropLast(1)))
    assertEquals(Bip39Result.WrongLength(24), Bip39.decode(twelve + twelve))
  }

  @Test
  fun encodeRejectsEntropyOfTheWrongSize() {
    assertThrows(IllegalArgumentException::class.java) { Bip39.encode(ByteArray(15)) }
    assertThrows(IllegalArgumentException::class.java) { Bip39.encode(ByteArray(32)) }
  }

  @Test
  fun knowsWhichWordsAreInTheList() {
    assertTrue(Bip39.isWord("zoo"))
    assertTrue(Bip39.isWord(" Abandon "))
    assertTrue(!Bip39.isWord("converses"))
  }
}
