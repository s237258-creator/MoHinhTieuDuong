package vn.edu.doan.diabetes.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record PredictionRecordDocument(
        String id,
        @JsonProperty("user_id") String userId,
        @JsonProperty("patient_name") String patientName,
        @JsonProperty("input_data") PatientInput inputData,
        @JsonProperty("patient_context") PatientContext patientContext,
        PredictionResult prediction,
        @JsonProperty("created_at") Instant createdAt) {
}
