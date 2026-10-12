package com.datdofishing.auth.web;

import com.datdofishing.auth.config.SigningKeyProvider;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Công khai khóa công khai để gateway và các service tự kiểm tra chữ ký JWT. */
@RestController
public class JwksController {

    private final SigningKeyProvider keys;

    public JwksController(SigningKeyProvider keys) {
        this.keys = keys;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return new JWKSet(keys.rsaKey().toPublicJWK()).toJSONObject();
    }
}
