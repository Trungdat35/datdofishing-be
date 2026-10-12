package com.datdofishing.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "datdofishing")
public record AppProperties(Google google, Admin admin) {

    public record Google(String clientId) {
    }

    public record Admin(String email, String password, String fullName) {
    }
}
