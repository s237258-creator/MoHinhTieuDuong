package vn.edu.doan.diabetes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.doan.diabetes.model.PatientContext;
import vn.edu.doan.diabetes.model.PatientInput;
import vn.edu.doan.diabetes.model.PredictionRecordDocument;
import vn.edu.doan.diabetes.model.PredictionResult;
import vn.edu.doan.diabetes.model.UserDocument;
import vn.edu.doan.diabetes.repository.PredictionRepository;
import vn.edu.doan.diabetes.repository.UserRepository;
import vn.edu.doan.diabetes.security.TokenService;
import vn.edu.doan.diabetes.service.AuthService;
import vn.edu.doan.diabetes.service.PredictionService;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final UserRepository users;
    private final PredictionRepository predictions;
    private final AuthService auth;
    private final TokenService tokens;
    private final PredictionService predictionService;

    public ApiController(
            UserRepository users,
            PredictionRepository predictions,
            AuthService auth,
            TokenService tokens,
            PredictionService predictionService) {
        this.users = users;
        this.predictions = predictions;
        this.auth = auth;
        this.tokens = tokens;
        this.predictionService = predictionService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterBody body) {
        UserDocument user = auth.register(new AuthService.RegisterRequest(
                body.phoneNumber(), body.fullName(), body.age(), body.gender(), body.password()));
        return AuthService.publicUser(user);
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@Valid @RequestBody LoginBody body) {
        return auth.login(new AuthService.LoginRequest(body.phoneNumber(), body.password()));
    }

    @GetMapping("/users/me")
    public Map<String, Object> currentUser(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return AuthService.publicUser(tokens.requireUser(authorization));
    }

    @GetMapping("/users/")
    public List<Map<String, Object>> listUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return users.findAll().stream().map(AuthService::publicUser).toList();
    }

    @PatchMapping("/users/{userId}/role")
    public Map<String, String> updateRole(
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody RoleUpdate update) {
        requireAdmin(authorization);
        requireUuid(userId);
        if (!update.role().equals("admin") && !update.role().equals("user")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quyền không hợp lệ");
        }
        UserDocument existing = users.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        users.save(new UserDocument(existing.id(), existing.phoneNumber(), existing.fullName(), existing.age(),
                existing.gender(), existing.passwordHash(), update.role(), existing.createdAt()));
        return Map.of("message", "Đã cập nhật quyền thành " + update.role() + " thành công");
    }

    @PostMapping("/predict")
    public PredictionResult predict(@Valid @RequestBody PatientInput patient) {
        return predictionService.predict(patient);
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> saveRecord(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody SaveRecordBody body) {
        UserDocument user = tokens.requireUser(authorization);
        String patientName = body.patientName() == null || body.patientName().isBlank()
                || body.patientName().equals("Ẩn danh") ? user.fullName() : body.patientName();
        PredictionRecordDocument record = predictions.save(new PredictionRecordDocument(
                null,
                user.id(),
                patientName,
                body.inputData(),
                body.patientContext() == null ? new PatientContext(null, null) : body.patientContext(),
                body.prediction(),
                Instant.now()));
        return Map.of("id", record.id(), "message", "Đã lưu kết quả thành công!");
    }

    @GetMapping("/records")
    public Map<String, Object> getRecords(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "risk_level", required = false) String riskLevel) {
        UserDocument user = tokens.requireUser(authorization);
        if (page < 1 || limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang hoặc số lượng bản ghi không hợp lệ");
        }
        List<PredictionRecordDocument> filtered = recordsFor(user).stream()
                .filter(record -> riskLevel == null || riskLevel.isBlank()
                        || record.prediction().riskLevel().equals(riskLevel))
                .sorted(Comparator.comparing(PredictionRecordDocument::createdAt).reversed())
                .toList();
        int pages = Math.max(1, (int) Math.ceil(filtered.size() / (double) limit));
        int start = Math.min((page - 1) * limit, filtered.size());
        int end = Math.min(start + limit, filtered.size());
        return Map.of("records", filtered.subList(start, end), "total", filtered.size(), "page", page, "pages", pages);
    }

    @GetMapping("/records/{recordId}")
    public PredictionRecordDocument getRecord(
            @PathVariable String recordId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UserDocument user = tokens.requireUser(authorization);
        PredictionRecordDocument record = findRecord(recordId, user);
        return record;
    }

    @DeleteMapping("/records/{recordId}")
    public Map<String, String> deleteRecord(
            @PathVariable String recordId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UserDocument user = tokens.requireUser(authorization);
        PredictionRecordDocument record = findRecord(recordId, user);
        predictions.delete(record);
        return Map.of("message", "Đã xóa bản ghi thành công!");
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UserDocument user = tokens.requireUser(authorization);
        List<PredictionRecordDocument> records = recordsFor(user);
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("Thấp", 0);
        distribution.put("Trung bình", 0);
        distribution.put("Cao", 0);
        double ageTotal = 0;
        double bmiTotal = 0;
        double glucoseTotal = 0;
        for (PredictionRecordDocument record : records) {
            distribution.computeIfPresent(record.prediction().riskLevel(), (key, value) -> value + 1);
            ageTotal += record.inputData().age();
            bmiTotal += record.inputData().bmi();
            glucoseTotal += record.inputData().glucose();
        }
        int count = records.size();
        return Map.of(
                "total_records", count,
                "risk_distribution", distribution,
                "avg_age", roundAverage(ageTotal, count),
                "avg_bmi", roundAverage(bmiTotal, count),
                "avg_glucose", roundAverage(glucoseTotal, count),
                "high_risk_percentage", count == 0 ? 0.0
                        : Math.round(distribution.get("Cao") * 1000.0 / count) / 10.0);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        users.count();
        return Map.of("status", "ok", "services", Map.of("database", "ok", "model", "ok"));
    }

    private List<PredictionRecordDocument> recordsFor(UserDocument user) {
        return user.role().equals("admin")
                ? predictions.findAll()
                : predictions.findAllByUserId(user.id());
    }

    private PredictionRecordDocument findRecord(String recordId, UserDocument user) {
        requireUuid(recordId);
        return (user.role().equals("admin") ? predictions.findById(recordId)
                        : predictions.findByIdAndUserId(recordId, user.id()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy bản ghi hoặc không có quyền truy cập"));
    }

    private static void requireUuid(String id) {
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID không hợp lệ");
        }
    }

    private UserDocument requireAdmin(String authorization) {
        UserDocument user = tokens.requireUser(authorization);
        if (!user.role().equals("admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này");
        }
        return user;
    }

    private static double roundAverage(double total, int count) {
        return count == 0 ? 0 : Math.round(total / count * 10.0) / 10.0;
    }

    public record RegisterBody(
            @JsonProperty("phone_number") @NotBlank @Pattern(regexp = "^\\d{10,11}$") String phoneNumber,
            @JsonProperty("full_name") @NotBlank @Size(min = 2, max = 200) String fullName,
            @NotNull @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(120) Integer age,
            @NotBlank @Size(max = 32) String gender,
            @NotBlank @Size(min = 6) String password) {
    }

    public record LoginBody(
            @JsonProperty("phone_number") @NotBlank String phoneNumber,
            @NotBlank String password) {
    }

    public record RoleUpdate(@NotBlank String role) {
    }

    public record SaveRecordBody(
            @JsonProperty("patient_name") @Size(max = 200) String patientName,
            @JsonProperty("input_data") @NotNull @Valid PatientInput inputData,
            @JsonProperty("patient_context") PatientContext patientContext,
            @NotNull @Valid PredictionResult prediction) {
    }
}
