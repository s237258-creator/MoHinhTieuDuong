package vn.edu.doan.diabetes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public record UserDocument(
        String id,
        String phoneNumber,
        String fullName,
        Integer age,
        String gender,
        @JsonIgnore String passwordHash,
        String role,
        Instant createdAt) {
}
