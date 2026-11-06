IF DB_ID(N'MoHinhTieuDuong') IS NULL
BEGIN
    CREATE DATABASE MoHinhTieuDuong;
END;
GO

USE MoHinhTieuDuong;
GO

IF OBJECT_ID(N'dbo.diabetes_users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.diabetes_users (
        id NVARCHAR(36) NOT NULL PRIMARY KEY,
        phone_number NVARCHAR(11) NOT NULL UNIQUE,
        full_name NVARCHAR(200) NOT NULL,
        age INT NOT NULL,
        gender NVARCHAR(32) NOT NULL,
        password_hash NVARCHAR(100) NOT NULL,
        user_role NVARCHAR(16) NOT NULL,
        created_at DATETIME2(7) NOT NULL
    );
END;
GO

IF OBJECT_ID(N'dbo.diabetes_predictions', N'U') IS NULL
BEGIN
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
    );
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'IX_diabetes_predictions_user_created'
      AND object_id = OBJECT_ID(N'dbo.diabetes_predictions')
)
BEGIN
    CREATE INDEX IX_diabetes_predictions_user_created
    ON dbo.diabetes_predictions(user_id, created_at DESC);
END;
GO
