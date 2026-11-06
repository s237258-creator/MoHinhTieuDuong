package vn.edu.doan.diabetes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.edu.doan.diabetes.model.PatientContext;
import vn.edu.doan.diabetes.model.PatientInput;
import vn.edu.doan.diabetes.model.PredictionResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PredictionServiceTest {
    @Test
    void javaInferenceMatchesExportedScikitLearnModel() {
        PredictionService service = new PredictionService(new ObjectMapper());

        PredictionResult typical = service.predict(new PatientInput(2d, 120d, 72d, 25d, 80d, 28.5d, 0.45d, 35d));
        PredictionResult low = service.predict(new PatientInput(0d, 80d, 60d, 20d, 50d, 22d, 0.2d, 25d));
        PredictionResult high = service.predict(new PatientInput(6d, 180d, 90d, 35d, 250d, 36d, 1.1d, 55d));

        assertEquals(0.32465146, typical.riskScore(), 1e-8);
        assertEquals("Trung bình", typical.riskLevel());
        assertEquals(0.03458333, low.riskScore(), 1e-8);
        assertEquals("Thấp", low.riskLevel());
        assertEquals(0.80970238, high.riskScore(), 1e-8);
        assertEquals("Cao", high.riskLevel());
        assertEquals(0.2719, typical.featuresImportance().get("Glucose"), 1e-4);
    }

    @Test
    void apiDtosUseExistingSnakeCaseJsonContract() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        PatientInput input = mapper.readValue("""
                {"pregnancies":2,"glucose":120,"blood_pressure":72,"skin_thickness":25,
                 "insulin":80,"bmi":28.5,"diabetes_pedigree":0.45,"age":35}
                """, PatientInput.class);
        PatientContext context = mapper.readValue(
                "{\"comorbidities\":[],\"lifestyle_habits\":[\"Ít vận động\"]}", PatientContext.class);

        assertEquals(72d, input.bloodPressure());
        assertEquals("Ít vận động", context.lifestyleHabits().getFirst());
        assertTrue(mapper.writeValueAsString(input).contains("\"blood_pressure\""));
        assertTrue(mapper.writeValueAsString(context).contains("\"lifestyle_habits\""));
    }
}
