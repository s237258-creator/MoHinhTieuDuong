package vn.edu.doan.diabetes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import vn.edu.doan.diabetes.model.UserDocument;
import vn.edu.doan.diabetes.repository.UserRepository;

import java.time.Instant;

@Component
public class BootstrapAdmin implements CommandLineRunner {
    private final UserRepository users;
    private final String phone;
    private final String password;
    private final String name;

    public BootstrapAdmin(
            UserRepository users,
            @Value("${app.bootstrap-admin.phone}") String phone,
            @Value("${app.bootstrap-admin.password}") String password,
            @Value("${app.bootstrap-admin.name}") String name) {
        this.users = users;
        this.phone = phone.trim();
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(String... args) {
        if (!phone.isEmpty() && !password.isEmpty() && users.findFirstAdmin().isEmpty()) {
            users.save(new UserDocument(null, phone, name, 30, "Nam",
                    new BCryptPasswordEncoder().encode(password), "admin", Instant.now()));
        }
    }
}
