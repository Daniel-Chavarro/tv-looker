package org.tvl.tvlooker.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.enums.UserAuthority;
import org.tvl.tvlooker.persistence.repository.UserRepository;

import org.jspecify.annotations.NonNull;

@Component
@ConditionalOnProperty(
        name = "admin.startup.enabled",
        havingValue = "true"
)
public class AdminStartupUtil implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminStartupUtil.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;
    private final String adminName;

    public AdminStartupUtil(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.startup.username}") String adminUsername,
            @Value("${admin.startup.password}") String adminPassword,
            @Value("${admin.startup.email}") String adminEmail,
            @Value("${admin.startup.name}") String adminName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
        this.adminName = adminName;
    }

    /**
     * Callback used to run the bean.
     *
     * @param args incoming application arguments
     */
    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            log.info("Skipping admin startup user creation because username '{}' already exists.", adminUsername);
            return;
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Skipping admin startup user creation because email '{}' already exists.", adminEmail);
            return;
        }

        UserEntity adminUser = UserEntity.builder()
                .username(adminUsername)
                .email(adminEmail)
                .name(adminName)
                .password(passwordEncoder.encode(adminPassword))
                .authority(UserAuthority.ADMIN)
                .build();

        userRepository.save(adminUser);
        log.info("Created initial admin user '{}' with ADMIN authority.", adminUsername);
    }
}
