package vn.edu.doan.diabetes.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.doan.diabetes.model.UserDocument;
import vn.edu.doan.diabetes.repository.UserRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class TokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final UserRepository users;
    private final byte[] secret;
    private final long expirationSeconds;

    public TokenService(
            ObjectMapper objectMapper,
            UserRepository users,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.objectMapper = objectMapper;
        this.users = users;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = Math.multiplyExact(expirationMinutes, 60);
    }

    public String createToken(UserDocument user) {
        try {
            String header = ENCODER.encodeToString(
                    objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(Map.of(
                    "sub", user.id(),
                    "role", user.role(),
                    "exp", Instant.now().getEpochSecond() + expirationSeconds)));
            String content = header + "." + payload;
            return content + "." + ENCODER.encodeToString(sign(content));
        } catch (JsonProcessingException | GeneralSecurityException exception) {
            throw new IllegalStateException("Không thể tạo access token", exception);
        }
    }

    public UserDocument requireUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw unauthorized();
        }

        try {
            String token = authorization.substring("Bearer ".length()).trim();
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !MessageDigest.isEqual(
                    sign(parts[0] + "." + parts[1]), DECODER.decode(parts[2]))) {
                throw unauthorized();
            }

            Map<?, ?> claims = objectMapper.readValue(DECODER.decode(parts[1]), Map.class);
            Object subject = claims.get("sub");
            Object expiry = claims.get("exp");
            if (!(subject instanceof String id) || !(expiry instanceof Number expiresAt)
                    || expiresAt.longValue() <= Instant.now().getEpochSecond()) {
                throw unauthorized();
            }
            return users.findById(id).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (java.io.IOException | GeneralSecurityException | IllegalArgumentException exception) {
            throw unauthorized();
        }
    }

    private byte[] sign(String content) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn");
    }
}
