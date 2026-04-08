package com.phoneclaw.ai.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneclaw.ai.ui.theme.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onComplete()
        }
    }

    // Sync pager position when ViewModel step changes
    LaunchedEffect(uiState.currentStep) {
        if (pagerState.currentPage != uiState.currentStep) {
            pagerState.animateScrollToPage(uiState.currentStep)
        }
    }

    // Keep ViewModel in sync when user swipes manually.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.goToStep(page)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasBg,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !uiState.isDownloading,
            ) { page ->
                when (page) {
                    0 -> WelcomeStep(
                        onNext = { viewModel.nextStep() },
                    )
                    1 -> ModelDownloadStep(
                        isDownloading = uiState.isDownloading,
                        downloadProgress = uiState.downloadProgress,
                        onDownload = { viewModel.startModelDownload() },
                        onSkip = { viewModel.skipModelDownload() },
                    )
                    2 -> PermissionsStep(
                        onNext = { viewModel.nextStep() },
                    )
                    3 -> CloudSetupStep(
                        onSaveKey = { key -> viewModel.saveApiKey(key) },
                        onSkip = { viewModel.skipCloudSetup() },
                    )
                }
            }

            // Back button
            if (uiState.currentStep > 0 && !uiState.isDownloading) {
                IconButton(
                    onClick = { viewModel.prevStep() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = ForegroundSecondary,
                    )
                }
            }

            // Page indicator dots
            PageIndicator(
                currentPage = pagerState.currentPage,
                pageCount = PAGE_COUNT,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun PageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val dotWidth by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = tween(durationMillis = 250),
                label = "dot_width_$index",
            )
            val dotColor by animateColorAsState(
                targetValue = if (isSelected) AccentViolet else Color(0xFFD1D5DB),
                animationSpec = tween(durationMillis = 250),
                label = "dot_color_$index",
            )
            Box(
                modifier = Modifier
                    .width(dotWidth)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}
