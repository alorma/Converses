package com.alorma.contactnotes.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import android.provider.Settings
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alorma.contactnotes.R
import com.alorma.contactnotes.data.settings.OnboardingFlag
import com.alorma.contactnotes.designsystem.theme.OnboardingTitleStyle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val PageCount = 3

private class OnboardingPage(val title: Int, val body: Int, val illustration: @Composable (Boolean, Int, Boolean) -> Unit)

private val Pages = listOf(
  OnboardingPage(R.string.onb_1_title, R.string.onb_1_body) { active, key, reduced -> StepOneIllustration(active, key, reduced) },
  OnboardingPage(R.string.onb_2_title, R.string.onb_2_body) { active, key, reduced -> StepTwoIllustration(active, key, reduced) },
  OnboardingPage(R.string.onb_3_title, R.string.onb_3_body) { active, key, reduced -> StepThreeIllustration(active, key, reduced) },
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
  val flag: OnboardingFlag = koinInject()
  OnboardingContent(
    onFinish = {
      flag.disable()
      onFinished()
    },
  )
}

@Composable
fun OnboardingContent(onFinish: () -> Unit, initialPage: Int = 0, forceReducedMotion: Boolean = false) {
  val pagerState = rememberPagerState(initialPage = initialPage) { PageCount }
  val scope = rememberCoroutineScope()
  val isLast = pagerState.currentPage == PageCount - 1
  val context = LocalContext.current
  val reducedMotion = forceReducedMotion || remember {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
  }

  BackHandler(enabled = pagerState.currentPage > 0) {
    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      Box(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(64.dp),
        contentAlignment = Alignment.CenterEnd,
      ) {
        if (!isLast) {
          TextButton(onClick = onFinish, modifier = Modifier.padding(end = 12.dp)) {
            Text(
              text = stringResource(R.string.onb_skip),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 15.sp,
            )
          }
        }
      }
    },
    bottomBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(start = 28.dp, end = 28.dp, bottom = 32.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        PageIndicator(pagerState = pagerState)
        Button(
          onClick = {
            if (isLast) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
          },
          modifier = Modifier.height(56.dp),
          shape = CircleShape,
        ) {
          Text(stringResource(if (isLast) R.string.onb_start else R.string.onb_next), fontSize = 16.sp)
          if (!isLast) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
          }
        }
      }
    },
  ) { padding ->
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(padding)) { page ->
      OnboardingPageContent(page = Pages[page], active = pagerState.settledPage == page, reduced = reducedMotion)
    }
  }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, active: Boolean, reduced: Boolean) {
  val intro = rememberSequence(count = 2, active = active, replayKey = 0, reduced = reduced) { a ->
    go(a[0], 150, tween(500, easing = FastOutSlowInEasing))
    go(a[1], 300, tween(500, easing = FastOutSlowInEasing))
  }
  Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
    Box(
      modifier = Modifier
        .padding(horizontal = 20.dp)
        .fillMaxWidth()
        .height(380.dp)
        .clearAndSetSemantics {},
      contentAlignment = Alignment.TopCenter,
    ) {
      page.illustration(active, 0, reduced)
    }
    Column(modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 20.dp)) {
      Text(
        text = stringResource(page.title),
        style = OnboardingTitleStyle,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.graphicsLayer {
          alpha = intro[0].value
          translationY = (1f - intro[0].value) * 16.dp.toPx()
        },
      )
      Spacer(Modifier.height(12.dp))
      Text(
        text = stringResource(page.body),
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 17.sp,
        lineHeight = 24.65.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.graphicsLayer {
          alpha = intro[1].value
          translationY = (1f - intro[1].value) * 16.dp.toPx()
        },
      )
    }
  }
}

@Composable
private fun PageIndicator(pagerState: PagerState) {
  val scope = rememberCoroutineScope()
  val current = pagerState.currentPage
  val description = stringResource(R.string.onb_page, current + 1, PageCount)
  Row(
    modifier = Modifier.semantics { stateDescription = description },
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(PageCount) { index ->
      val selected = index == current
      val width = animateDpAsState(
        targetValue = if (selected) 28.dp else 8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "dotWidth",
      )
      val stepLabel = stringResource(R.string.onb_page, index + 1, PageCount)
      Box(
        modifier = Modifier
          .size(width = width.value + 8.dp, height = 48.dp)
          .clip(CircleShape)
          .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
          .semantics { contentDescription = stepLabel },
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier
            .width(width.value)
            .height(8.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        )
      }
    }
  }
}
