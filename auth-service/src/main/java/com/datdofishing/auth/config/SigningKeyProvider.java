package com.datdofishing.auth.config;

import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Khóa RSA dùng ký JWT. Lần đầu khởi động sẽ sinh khóa và lưu vào bảng signing_keys,
 * các lần sau (hoặc instance khác) đọc lại cùng khóa đó.
 */
@Component
public class SigningKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(SigningKeyProvider.class);

    private final RSAKey rsaKey;

    public SigningKeyProvider(JdbcTemplate jdbc) {
        this.rsaKey = loadOrCreate(jdbc);
    }

    public RSAKey rsaKey() {
        return rsaKey;
    }

    public RSAPublicKey publicKey() {
        try {
            return rsaKey.toRSAPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static RSAKey loadOrCreate(JdbcTemplate jdbc) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT kid, private_key, public_key FROM signing_keys ORDER BY created_at DESC LIMIT 1");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                RSAPrivateKey priv = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(
                        Base64.getDecoder().decode((String) row.get("private_key"))));
                RSAPublicKey pub = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(
                        Base64.getDecoder().decode((String) row.get("public_key"))));
                return new RSAKey.Builder(pub).privateKey(priv).keyID((String) row.get("kid")).build();
            }
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            String kid = UUID.randomUUID().toString();
            jdbc.update("INSERT INTO signing_keys(kid, private_key, public_key) VALUES (?, ?, ?)", kid,
                    Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()),
                    Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
            log.info("Đã tạo khóa ký JWT mới, kid={}", kid);
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate()).keyID(kid).build();
        } catch (Exception e) {
            throw new IllegalStateException("Không khởi tạo được khóa ký JWT", e);
        }
    }
}
