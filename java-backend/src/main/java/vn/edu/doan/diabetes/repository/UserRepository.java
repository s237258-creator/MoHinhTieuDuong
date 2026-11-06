package vn.edu.doan.diabetes.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import vn.edu.doan.diabetes.model.UserDocument;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {
    private static final String COLUMNS =
            "id, phone_number, full_name, age, gender, password_hash, user_role, created_at";
    private static final RowMapper<UserDocument> USER_MAPPER = (row, index) -> new UserDocument(
            row.getString("id"),
            row.getString("phone_number"),
            row.getString("full_name"),
            row.getInt("age"),
            row.getString("gender"),
            row.getString("password_hash"),
            row.getString("user_role"),
            row.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserDocument> findByPhoneNumber(String phoneNumber) {
        return jdbc.query("SELECT " + COLUMNS + " FROM dbo.diabetes_users WHERE phone_number = ?",
                        USER_MAPPER, phoneNumber)
                .stream().findFirst();
    }

    public Optional<UserDocument> findFirstAdmin() {
        return jdbc.query("SELECT TOP (1) " + COLUMNS
                        + " FROM dbo.diabetes_users WHERE user_role = 'admin' ORDER BY created_at",
                        USER_MAPPER)
                .stream().findFirst();
    }

    public Optional<UserDocument> findById(String id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM dbo.diabetes_users WHERE id = ?",
                        USER_MAPPER, id)
                .stream().findFirst();
    }

    public List<UserDocument> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM dbo.diabetes_users ORDER BY created_at DESC",
                USER_MAPPER);
    }

    public long count() {
        Long count = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.diabetes_users", Long.class);
        return count == null ? 0 : count;
    }

    public UserDocument save(UserDocument user) {
        String id = user.id() == null ? UUID.randomUUID().toString() : user.id();
        Instant createdAt = user.createdAt() == null ? Instant.now() : user.createdAt();
        Timestamp created = Timestamp.from(createdAt);
        int updated = jdbc.update("""
                UPDATE dbo.diabetes_users
                SET phone_number = ?, full_name = ?, age = ?, gender = ?,
                    password_hash = ?, user_role = ?, created_at = ?
                WHERE id = ?
                """, user.phoneNumber(), user.fullName(), user.age(), user.gender(),
                user.passwordHash(), user.role(), created, id);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO dbo.diabetes_users
                        (id, phone_number, full_name, age, gender, password_hash, user_role, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, user.phoneNumber(), user.fullName(), user.age(), user.gender(),
                    user.passwordHash(), user.role(), created);
        }
        return new UserDocument(id, user.phoneNumber(), user.fullName(), user.age(), user.gender(),
                user.passwordHash(), user.role(), createdAt);
    }
}
