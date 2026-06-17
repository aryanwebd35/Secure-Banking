package net.javaguides.Banking_app.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ClerkTokenValidator {

    @Value("${clerk.jwks-url}")
    private String jwksUrl;

    private final Map<String, Key> publicKeysCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Claims verifyAndExtractClaims(String token) {
        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                        @Override
                        public Key resolveSigningKey(JwsHeader header, Claims claims) {
                            String kid = header.getKeyId();
                            if (kid == null) {
                                return null;
                            }
                            return publicKeysCache.computeIfAbsent(kid, k -> fetchPublicKeyFromJwks(k));
                        }
                    })
                    .build();

            return parser.parseClaimsJws(token).getBody();
        } catch (Exception e) {
            log.error("Failed to verify Clerk token: {}", e.getMessage());
            return null;
        }
    }

    private Key fetchPublicKeyFromJwks(String kid) {
        try {
            log.info("Fetching Clerk public keys from JWKS URL: {}", jwksUrl);
            InputStream is = new URL(jwksUrl).openStream();
            JsonNode root = objectMapper.readTree(is);
            JsonNode keysNode = root.get("keys");
            if (keysNode != null && keysNode.isArray()) {
                for (JsonNode keyNode : keysNode) {
                    if (kid.equals(keyNode.get("kid").asText())) {
                        String modulusBase64 = keyNode.get("n").asText();
                        String exponentBase64 = keyNode.get("e").asText();

                        byte[] nb = Base64.getUrlDecoder().decode(modulusBase64);
                        byte[] eb = Base64.getUrlDecoder().decode(exponentBase64);

                        BigInteger modulus = new BigInteger(1, nb);
                        BigInteger exponent = new BigInteger(1, eb);

                        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        return kf.generatePublic(spec);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching/parsing JWKS keys: {}", e.getMessage(), e);
        }
        return null;
    }
}
