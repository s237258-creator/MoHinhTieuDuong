package vn.edu.doan.diabetes.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record PredictionResult(
        @JsonProperty("risk_score") double riskScore,
        @JsonProperty("risk_level") String riskLevel,
        @JsonProperty("risk_percentage") double riskPercentage,
        String recommendation,
        @JsonProperty("features_importance") Map<String, Double> featuresImportance) {
}
