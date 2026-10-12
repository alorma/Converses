package com.alorma.contactnotes.wear.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alorma.contactnotes.shared.WEAR_PROTOCOL_VERSION
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PrivacyAuditTest {
  @get:Rule
  val folder = TemporaryFolder()

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  @After
  fun tearDown() = scope.cancel()

  // Peor cas: un xifrat que deixa el text en clar, per veure què arriba realment al disc
  private val plainCipher =
    object : SnapshotCipher {
      override fun encrypt(plain: String) = plain

      override fun decrypt(encoded: String) = encoded
    }

  // El mòbil mai envia contingut d'una protegida, però el rellotge no s'hi pot fiar: l'instantani es fabrica a mà
  private fun payloadWithLeakingProtectedPerson(): String =
    """
    {"version":$WEAR_PROTOCOL_VERSION,"generatedAt":1,"appLockActive":true,"people":[
    {"id":1,"name":"Ana","avatarColor":"Mint","avatarShape":"Sunny","isProtected":true,"lastEntryAt":99,
     "entries":[{"id":1,"kind":"SAID","channel":"CHAT","text":"SECRET-ENTRY","occurredAt":99}]}]}
    """.trimIndent()

  @Test
  fun noProtectedContentReachesTheDiskEvenIfThePayloadCarriesIt() =
    runTest {
      val file = folder.newFile("snapshot.preferences_pb")
      val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
      val store = WearSnapshotStore(dataStore, { true }, plainCipher)

      val raw = payloadWithLeakingProtectedPerson()
      assertTrue("la càrrega de prova ha de dur el secret", raw.contains("SECRET-ENTRY"))
      assertTrue(store.save(raw))

      val onDisk = file.readBytes().toString(Charsets.ISO_8859_1)
      assertTrue("la persona sí que s'ha de desar", onDisk.contains("Ana"))
      assertFalse(onDisk.contains("SECRET-ENTRY"))
      val reloaded = store.stored.first()!!.snapshot
      assertTrue(
        reloaded.people
          .single()
          .entries
          .isEmpty(),
      )
    }

  @Test
  fun theWatchAppNeverLogsNorPostsNotificationsNorUsesOtherStorage() {
    val forbidden =
      listOf(
        "Log.",
        "println(",
        "Timber",
        "Notification",
        "getSharedPreferences",
        "openFileOutput",
        "FileWriter",
        "writeText(",
        "externalCacheDir",
        "getExternalFilesDir",
        "cacheDir",
      )
    val offenders =
      File("src/main/kotlin")
        .walkTopDown()
        .filter { it.extension == "kt" }
        .flatMap { file ->
          file.readLines().mapIndexedNotNull { i, line ->
            val code = line.substringBefore("//").trim()
            if (code.startsWith("*") || code.startsWith("/*")) return@mapIndexedNotNull null
            forbidden.firstOrNull { it in code }?.let { "${file.name}:${i + 1} -> $it" }
          }
        }.toList()
    assertTrue("Ús prohibit al rellotge: $offenders", offenders.isEmpty())
  }
}
