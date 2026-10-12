package com.datdofishing.order.config;

import com.datdofishing.common.security.ResourceServerSecurity;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableMethodSecurity
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
public class AppConfig {

    /** Đặt hàng và xem đơn đều cần đăng nhập. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return ResourceServerSecurity.build(http, new String[]{}, new String[]{});
    }

    @Bean
    public LockProvider lockProvider(JdbcTemplate jdbc) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(jdbc)
                .usingDbTime()
                .build());
    }

    @Bean
    public RestClient catalogRestClient(RestClient.Builder builder, @Value("${datdofishing.catalog-url}") String url) {
        return builder.clone().baseUrl(url).requestFactory(timeouts()).build();
    }

    @Bean
    public RestClient promotionRestClient(RestClient.Builder builder, @Value("${datdofishing.promotion-url}") String url) {
        return builder.clone().baseUrl(url).requestFactory(timeouts()).build();
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(2));
        f.setReadTimeout(Duration.ofSeconds(5));
        return f;
    }
}
