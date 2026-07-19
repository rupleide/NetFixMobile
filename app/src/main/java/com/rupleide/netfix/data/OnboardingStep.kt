package com.rupleide.netfix.data

sealed class OnboardingStep {
    object Welcome : OnboardingStep()
    object DeviceType : OnboardingStep()
    object TelegramProxyQuestion : OnboardingStep()
    object YoutubeBypassQuestion : OnboardingStep()
    object StrategyTestingExisting : OnboardingStep()
    object StrategyTestingRunning : OnboardingStep()
    object StrategyTestingResult : OnboardingStep()
    object AboutAndSupport : OnboardingStep()
    object Summary : OnboardingStep()
    object FinalGreeting : OnboardingStep()
}
