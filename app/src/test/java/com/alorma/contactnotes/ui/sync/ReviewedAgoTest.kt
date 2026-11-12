package com.alorma.contactnotes.ui.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewedAgoTest {
  private val day = 24L * 60 * 60 * 1000
  private val now = 1_000_000L * day

  private fun ago(days: Long) = reviewedAgo(now - days * day, now)

  @Test
  fun `less than a day is today, even a future date`() {
    assertEquals(ReviewedAgo.Today, reviewedAgo(now, now))
    assertEquals(ReviewedAgo.Today, reviewedAgo(now - day + 1, now))
    assertEquals(ReviewedAgo.Today, reviewedAgo(now + day, now))
  }

  @Test
  fun `one to 29 days are days`() {
    assertEquals(ReviewedAgo.Days(1), ago(1))
    assertEquals(ReviewedAgo.Days(29), ago(29))
  }

  @Test
  fun `30 to 364 days are months`() {
    assertEquals(ReviewedAgo.Months(1), ago(30))
    assertEquals(ReviewedAgo.Months(4), ago(125))
    assertEquals(ReviewedAgo.Months(11), ago(364))
  }

  @Test
  fun `a year or more are years`() {
    assertEquals(ReviewedAgo.Years(1), ago(365))
    assertEquals(ReviewedAgo.Years(2), ago(800))
  }
}
