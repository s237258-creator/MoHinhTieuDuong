package vn.edu.doan.diabetes.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PatientInput(
        @NotNull @DecimalMin("0") @DecimalMax("20") Double pregnancies,
        @NotNull @DecimalMin("0") @DecimalMax("300") Double glucose,
        @JsonProperty("blood_pressure") @NotNull @DecimalMin("0") @DecimalMax("200") Double bloodPressure,
        @JsonProperty("skin_thickness") @NotNull @DecimalMin("0") @DecimalMax("100") Double skinThickness,
        @NotNull @DecimalMin("0") @DecimalMax("900") Double insulin,
        @NotNull @DecimalMin("0") @DecimalMax("70") Double bmi,
        @JsonProperty("diabetes_pedigree") @NotNull @DecimalMin("0") @DecimalMax("3") Double diabetesPedigree,
        @NotNull @DecimalMin("1") @DecimalMax("120") Double age) {
}
