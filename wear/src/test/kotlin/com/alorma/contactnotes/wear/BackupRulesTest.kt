package com.alorma.contactnotes.wear

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRulesTest {
  private val domains = setOf("root", "database", "file", "sharedpref")

  private fun excluded(
    file: String,
    section: String? = null,
  ): Set<String> {
    var xml = File("src/main/res/xml/$file").readText()
    if (section != null) xml = xml.substringAfter("<$section>").substringBefore("</$section>")
    return Regex("""<exclude\s+domain="(\w+)"\s+path="\."\s*/>""").findAll(xml).map { it.groupValues[1] }.toSet()
  }

  @Test
  fun everyDomainIsExcludedFromTheLegacyBackup() = assertEquals(domains, excluded("backup_rules.xml"))

  @Test
  fun everyDomainIsExcludedFromTheCloudBackup() =
    assertEquals(domains, excluded("data_extraction_rules.xml", "cloud-backup"))

  @Test
  fun everyDomainIsExcludedFromTheDeviceTransfer() =
    assertEquals(domains, excluded("data_extraction_rules.xml", "device-transfer"))

  @Test
  fun theManifestDisablesBackupAndPointsToTheRules() {
    val manifest = File("src/main/AndroidManifest.xml").readText()
    assertTrue(manifest.contains("""android:allowBackup="false""""))
    assertTrue(manifest.contains("@xml/data_extraction_rules"))
    assertTrue(manifest.contains("@xml/backup_rules"))
  }
}
