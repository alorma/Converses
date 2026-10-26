package com.alorma.contactnotes.data.pro

import android.app.Activity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProRepositoryTest {
  @Test
  fun releaseWithoutKeyIsNeverProAndCannotBuy() =
    runTest {
      val repository = ProRepository(NoProDataSource)

      assertFalse(repository.isPro.value)
      assertNull(repository.price())
      assertEquals(PurchaseResult.Failed, repository.purchase(Activity()))
      assertEquals(RestoreResult.NothingToRestore, repository.restore())
    }

  @Test
  fun repositoryFollowsTheDataSource() {
    val source = LocalProDataSource()
    val repository = ProRepository(source)

    source.setPro(true)
    assertTrue(repository.isPro.value)

    source.setPro(false)
    assertFalse(repository.isPro.value)
  }
}
