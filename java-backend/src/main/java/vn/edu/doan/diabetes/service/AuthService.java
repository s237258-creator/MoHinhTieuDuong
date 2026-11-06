package vn.edu.doan.diabetes.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.doan.diabetes.model.UserDocument;
import vn.edu.doan.diabetes.repository.UserRepository;
import vn.edu.doan.diabetes.security.TokenService;

import java.time.Instant;
import java.util.Map;

@Service
public class AuthService {
    private final UserRepository users;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final TokenService tokens;

    public AuthService(UserRepository users, TokenService tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    public UserDocument register(RegisterRequest request) {
        if (users.findByPhoneNumber(request.phoneNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại này đã được đăng ký");
        }
        return users.save(new UserDocument(
                null,
                request.phoneNumber(),
                request.fullName(),
                request.age(),
                request.gender(),
                passwordEncoder.encode(request.password()),
                "user",
                Instant.now()));
    }

    public Map<String, Object> login(LoginRequest request) {
        UserDocument user = users.findByPhoneNumber(request.phoneNumber())
                .filter(account -> passwordEncoder.matches(request.password(), account.passwordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Số điện thoại hoặc mật khẩu không chính xác"));

        return Map.of(
                "access_token", tokens.createToken(user),
                "token_type", "bearer",
                "user", publicUser(user));
    }

    public static Map<String, Object> publicUser(UserDocument user) {
        return Map.of(
                "id", user.id(),
                "phone_number", user.phoneNumber(),
                "full_name", user.fullName(),
                "age", user.age(),
                "gender", user.gender(),
                "role", user.role(),
                "created_at", user.createdAt());
    }

    public record RegisterRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("phone_number")
            @jakarta.validation.constraints.Pattern(regexp = "^\\d{10,11}$") String phoneNumber,
            @com.fasterxml.jackson.annotation.JsonProperty("full_name")
            @jakarta.validation.constraints.Size(min = 2) String fullName,
            @jakarta.validation.constraints.NotNull
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(120) Integer age,
            @jakarta.validation.constraints.NotBlank String gender,
            @jakarta.validation.constraints.Size(min = 6) String password) {
    }

    public record LoginRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("phone_number") String phoneNumber,
            String password) {
    }
}
