package vn.edu.doan.diabetes.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiControllerValidationTest {
    @Test
    void registrationAndRecordNamesRespectSqlServerColumnLengths() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertTrue(validator.validateValue(ApiController.RegisterBody.class, "fullName", "N".repeat(200)).isEmpty());
            assertFalse(validator.validateValue(ApiController.RegisterBody.class, "fullName", "N".repeat(201)).isEmpty());
            assertTrue(validator.validateValue(ApiController.RegisterBody.class, "gender", "G".repeat(32)).isEmpty());
            assertFalse(validator.validateValue(ApiController.RegisterBody.class, "gender", "G".repeat(33)).isEmpty());
            assertTrue(validator.validateValue(ApiController.SaveRecordBody.class, "patientName", "N".repeat(200)).isEmpty());
            assertFalse(validator.validateValue(ApiController.SaveRecordBody.class, "patientName", "N".repeat(201)).isEmpty());
        }
    }
}
