package vn.edu.doan.diabetes.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public DatabaseInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("diabetes_users")) {
            jdbc.execute("""
                    CREATE TABLE dbo.diabetes_users (
                        id NVARCHAR(36) NOT NULL PRIMARY KEY,
                        phone_number NVARCHAR(11) NOT NULL UNIQUE,
                        full_name NVARCHAR(200) NOT NULL,
                        age INT NOT NULL,
                        gender NVARCHAR(32) NOT NULL,
                        password_hash NVARCHAR(100) NOT NULL,
                        user_role NVARCHAR(16) NOT NULL,
                        created_at DATETIME2(7) NOT NULL
                    )
                    """);
        }
        if (!tableExists("diabetes_predictions")) {
            jdbc.execute("""
                    CREATE TABLE dbo.diabetes_predictions (
                        id NVARCHAR(36) NOT NULL PRIMARY KEY,
                        user_id NVARCHAR(36) NOT NULL,
                        patient_name NVARCHAR(200) NOT NULL,
                        input_data_json NVARCHAR(MAX) NOT NULL,
                        patient_context_json NVARCHAR(MAX) NOT NULL,
                        prediction_json NVARCHAR(MAX) NOT NULL,
                        created_at DATETIME2(7) NOT NULL,
                        CONSTRAINT FK_diabetes_predictions_users
                            FOREIGN KEY (user_id) REFERENCES dbo.diabetes_users(id)
                    )
                    """);
        }
        if (!indexExists("IX_diabetes_predictions_user_created")) {
            jdbc.execute("""
                    CREATE INDEX IX_diabetes_predictions_user_created
                    ON dbo.diabetes_predictions(user_id, created_at DESC)
                    """);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys.indexes
                WHERE name = ? AND object_id = OBJECT_ID('dbo.diabetes_predictions')
                """, Integer.class, indexName);
        return count != null && count > 0;
    }
}
