package com.datdofishing.promotion.config;

import com.datdofishing.common.security.ResourceServerSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return ResourceServerSecurity.build(http, new String[]{"/api/promotions/**"}, new String[]{});
    }

    @Bean
    public RestClient catalogClient(RestClient.Builder builder, @Value("${datdofishing.catalog-url}") String url) {
        return builder.baseUrl(url).build();
    }
}
