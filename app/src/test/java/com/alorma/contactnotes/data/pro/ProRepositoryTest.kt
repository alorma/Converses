package com.alorma.contactnotes.data.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProRepositoryTest {
  @Test
  fun releaseIsNeverPro() {
    assertFalse(ProRepository(NoProDataSource).isPro.value)
  }

  @Test
  fun localIsNotProByDefault() {
    assertFalse(ProRepository(LocalProDataSource()).isPro.value)
  }

  @Test
  fun repositoryFollowsTheLocalSwitch() {
    val local = LocalProDataSource()
    val repository = ProRepository(local)

    local.setPro(true)
    assertTrue(repository.isPro.value)

    local.setPro(false)
    assertFalse(repository.isPro.value)
  }

  @Test
  fun localIsPersistedAndRestored() {
    val saved = mutableListOf<Boolean>()
    LocalProDataSource(persist = { saved += it }).setPro(true)
    assertEquals(listOf(true), saved)

    assertTrue(ProRepository(LocalProDataSource(initial = saved.last())).isPro.value)
  }
}
