package com.deepak.Attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OcrClientConfig {

    @Bean
    public WebClient.Builder ocrWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient ocrWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.build();
    }
}
