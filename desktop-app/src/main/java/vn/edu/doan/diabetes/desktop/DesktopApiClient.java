package vn.edu.doan.diabetes.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class DesktopApiClient {
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private volatile String accessToken;

    DesktopApiClient(String baseUrl) {
        String normalized = baseUrl.replaceAll("/+$", "");
        this.baseUrl = normalized.endsWith("/api")
                ? normalized.substring(0, normalized.length() - 4)
                : normalized;
    }

    ObjectMapper mapper() {
        return mapper;
    }

    String baseUrl() {
        return baseUrl;
    }

    void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    JsonNode get(String path) throws IOException, InterruptedException {
        return send("GET", path, null);
    }

    JsonNode post(String path, JsonNode body) throws IOException, InterruptedException {
        return send("POST", path, body);
    }

    JsonNode patch(String path, JsonNode body) throws IOException, InterruptedException {
        return send("PATCH", path, body);
    }

    JsonNode delete(String path) throws IOException, InterruptedException {
        return send("DELETE", path, null);
    }

    ObjectNode object() {
        return mapper.createObjectNode();
    }

    private JsonNode send(String method, String path, JsonNode body) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + "/api" + path))
                .timeout(Duration.ofSeconds(path.equals("/predict") ? 90 : 30))
                .header("Accept", "application/json");
        if (body != null) {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }
        String token = accessToken;
        if (token != null && !token.isBlank()) {
            request.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode value = response.body().isBlank()
                ? mapper.getNodeFactory().nullNode()
                : mapper.readTree(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = value.path("detail").asText(value.path("message").asText(
                    "Máy chủ trả về lỗi HTTP " + response.statusCode()));
            throw new IOException(message);
        }
        return value;
    }
}
