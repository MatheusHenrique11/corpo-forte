package com.corpoforte.tracker.onboarding;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OnboardingWebConfig implements WebMvcConfigurer {

    private final OnboardingPendenteInterceptor onboardingPendenteInterceptor;

    public OnboardingWebConfig(OnboardingPendenteInterceptor onboardingPendenteInterceptor) {
        this.onboardingPendenteInterceptor = onboardingPendenteInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(onboardingPendenteInterceptor).addPathPatterns("/api/**");
    }
}
