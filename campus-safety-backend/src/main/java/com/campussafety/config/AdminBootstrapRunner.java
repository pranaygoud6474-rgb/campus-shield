package com.campussafety.config;

import com.campussafety.entity.Role;
import com.campussafety.entity.User;
import com.campussafety.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            logger.info("Initial administrator already exists; bootstrap skipped.");
            return;
        }

        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalStateException("A valid bootstrap administrator email is required.");
        }
        if (password == null || password.length() < 16 || password.length() > 128) {
            throw new IllegalStateException("Bootstrap administrator password must be between 16 and 128 characters.");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException("Bootstrap administrator email belongs to an existing non-admin account.");
        }

        User administrator = new User(
                "Campus",
                "Administrator",
                normalizedEmail,
                passwordEncoder.encode(password),
                Role.ADMIN);
        userRepository.save(administrator);
        logger.info("Initial administrator account created.");
    }
}
