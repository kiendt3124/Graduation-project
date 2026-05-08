package com.uet.expensetracker.features.money.ui.onboarding.onboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.uet.expensetracker.R
import com.uet.expensetracker.features.money.ui.navigation.screen.Screen
import com.uet.expensetracker.features.money.ui.onboarding.onboard.components.OnboardingItem
import com.uet.expensetracker.ui.theme.AppColor
import com.uet.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController
) {
    val slides = getOnboardingSlides()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    fun navigateToWalletSetup() {
        // Skip flow (no login): keep existing behavior, do NOT allow skipping WalletSetup
        navController.navigate(Screen.WalletSetup.createRoute(allowSkip = false)) {
            popUpTo(Screen.Onboarding.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateToGoogleLogin() {
        navController.navigate(Screen.GoogleLogin.route) {
            launchSingleTop = true
        }
    }

    Surface(color = AppColor.Light.PrimaryColor.containerColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 16.dp)
            ) { page ->
                val slide = slides[page]
                OnboardingItem(
                    title = slide.title,
                    description = slide.description,
                    topIllustrationRes = slide.illustrationRes
                )
            }

            DotsIndicator(
                totalDots = slides.size,
                selectedIndex = pagerState.currentPage
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (pagerState.currentPage < slides.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        // Last page: login flow
                        navigateToGoogleLogin()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColor.Light.PrimaryColor.TextButtonColor,
                    contentColor = Color.White
                )
            ) {
                Text(text = if (pagerState.currentPage == slides.lastIndex) stringResource(R.string.onboarding_login_or_create) else stringResource(R.string.onboarding_continue))
            }

            TextButton(
                onClick = { navigateToWalletSetup() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == slides.lastIndex) stringResource(R.string.onboarding_skip_for_now) else stringResource(R.string.onboarding_skip),
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.height(8.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun DotsIndicator(
    totalDots: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalDots) { index ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 12.dp else 8.dp)
                    .background(
                        color = if (isSelected) AppColor.Light.PrimaryColor.TextButtonColor else Color(0xFFCBD5E1),
                        shape = CircleShape
                    )
            )
        }
    }
}

private data class OnboardingSlide(
    val title: String,
    val description: String,
    val illustrationRes: Int
)

@Composable
private fun getOnboardingSlides(): List<OnboardingSlide> = listOf(
    OnboardingSlide(
        title = stringResource(R.string.onboarding_welcome_title),
        description = stringResource(R.string.onboarding_welcome_description),
        illustrationRes = R.drawable.img_bg_ob1
    ),
    OnboardingSlide(
        title = stringResource(R.string.onboarding_track_expense_title),
        description = stringResource(R.string.onboarding_track_expense_description),
        illustrationRes = R.drawable.img_bg_ob2
    ),
    OnboardingSlide(
        title = stringResource(R.string.onboarding_smart_budgeting_title),
        description = stringResource(R.string.onboarding_smart_budgeting_description),
        illustrationRes = R.drawable.img_bg_ob3
    ),
    OnboardingSlide(
        title = stringResource(R.string.onboarding_all_set_title),
        description = stringResource(R.string.onboarding_all_set_description),
        illustrationRes = R.drawable.img_bg_ob4
    )
)

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun OnboardingScreenPreview() {
    ExpenseTrackerTheme {
        OnboardingItem(
            title = stringResource(R.string.onboarding_welcome_title),
            description = stringResource(R.string.onboarding_welcome_description),
            topIllustrationRes = R.drawable.img_bg_ob1
        )
    }
}

 