package com.datdofishing.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "datdofishing.jwt")
public record JwtProperties(String issuer, Duration accessTtl, Duration refreshTtl) {
}
