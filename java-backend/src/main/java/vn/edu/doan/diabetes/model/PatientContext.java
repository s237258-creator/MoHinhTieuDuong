package vn.edu.doan.diabetes.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PatientContext(
        List<String> comorbidities,
        @JsonProperty("lifestyle_habits") List<String> lifestyleHabits) {
    public PatientContext {
        comorbidities = comorbidities == null ? List.of() : List.copyOf(comorbidities);
        lifestyleHabits = lifestyleHabits == null ? List.of() : List.copyOf(lifestyleHabits);
    }
}
