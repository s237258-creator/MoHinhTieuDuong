package vn.edu.doan.diabetes.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import vn.edu.doan.diabetes.model.PatientContext;
import vn.edu.doan.diabetes.model.PatientInput;
import vn.edu.doan.diabetes.model.PredictionRecordDocument;
import vn.edu.doan.diabetes.model.PredictionResult;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PredictionRepository {
    private static final String COLUMNS =
            "id, user_id, patient_name, input_data_json, patient_context_json, prediction_json, created_at";

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RowMapper<PredictionRecordDocument> recordMapper;

    public PredictionRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.recordMapper = (row, index) -> {
            try {
                return new PredictionRecordDocument(
                        row.getString("id"),
                        row.getString("user_id"),
                        row.getString("patient_name"),
                        this.objectMapper.readValue(row.getString("input_data_json"), PatientInput.class),
                        this.objectMapper.readValue(row.getString("patient_context_json"), PatientContext.class),
                        this.objectMapper.readValue(row.getString("prediction_json"), PredictionResult.class),
                        row.getTimestamp("created_at").toInstant());
            } catch (JsonProcessingException exception) {
                throw new SQLException("Bản ghi dự đoán trong SQL Server có JSON không hợp lệ.", exception);
            }
        };
    }

    public List<PredictionRecordDocument> findAll() {
        return jdbc.query("SELECT " + COLUMNS
                + " FROM dbo.diabetes_predictions ORDER BY created_at DESC", recordMapper);
    }

    public List<PredictionRecordDocument> findAllByUserId(String userId) {
        return jdbc.query("SELECT " + COLUMNS
                        + " FROM dbo.diabetes_predictions WHERE user_id = ? ORDER BY created_at DESC",
                recordMapper, userId);
    }

    public Optional<PredictionRecordDocument> findById(String id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM dbo.diabetes_predictions WHERE id = ?",
                        recordMapper, id)
                .stream().findFirst();
    }

    public Optional<PredictionRecordDocument> findByIdAndUserId(String id, String userId) {
        return jdbc.query("SELECT " + COLUMNS
                        + " FROM dbo.diabetes_predictions WHERE id = ? AND user_id = ?",
                        recordMapper, id, userId)
                .stream().findFirst();
    }

    public PredictionRecordDocument save(PredictionRecordDocument record) {
        String id = record.id() == null ? UUID.randomUUID().toString() : record.id();
        Instant createdAt = record.createdAt() == null ? Instant.now() : record.createdAt();
        PatientContext context = record.patientContext() == null
                ? new PatientContext(null, null) : record.patientContext();
        String inputJson = writeJson(record.inputData());
        String contextJson = writeJson(context);
        String predictionJson = writeJson(record.prediction());
        Timestamp created = Timestamp.from(createdAt);
        int updated = jdbc.update("""
                UPDATE dbo.diabetes_predictions
                SET user_id = ?, patient_name = ?, input_data_json = ?,
                    patient_context_json = ?, prediction_json = ?, created_at = ?
                WHERE id = ?
                """, record.userId(), record.patientName(), inputJson, contextJson,
                predictionJson, created, id);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO dbo.diabetes_predictions
                        (id, user_id, patient_name, input_data_json,
                         patient_context_json, prediction_json, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, id, record.userId(), record.patientName(), inputJson,
                    contextJson, predictionJson, created);
        }
        return new PredictionRecordDocument(id, record.userId(), record.patientName(),
                record.inputData(), context, record.prediction(), createdAt);
    }

    public void delete(PredictionRecordDocument record) {
        jdbc.update("DELETE FROM dbo.diabetes_predictions WHERE id = ?", record.id());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể tuần tự hóa dữ liệu lưu trong SQL Server.", exception);
        }
    }
}
