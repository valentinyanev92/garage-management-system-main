package com.softuni.gms.app.user;

import com.softuni.gms.app.config.TestJpaConfig;
import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.user.model.UserRole;
import com.softuni.gms.app.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@EntityScan("com.softuni.gms.app")
@EnableJpaRepositories("com.softuni.gms.app")
@Import(TestJpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryUTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_shouldReturnCorrectUser() {

        createUser("valyo", "v1@test.com", "359899000001");

        User result = userRepository.findByUsername("valyo").orElse(null);

        assertThat(result).isNotNull();
        Assertions.assertNotNull(result);
        assertThat(result.getUsername()).isEqualTo("valyo");
    }

    @Test
    void findByEmail_shouldReturnCorrectUser() {

        createUser("john", "john@test.com", "359899000002");

        User result = userRepository.findByEmail("john@test.com").orElse(null);

        assertThat(result).isNotNull();
        Assertions.assertNotNull(result);
        assertThat(result.getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void findByPhoneNumber_shouldReturnCorrectUser() {

        createUser("mike", "mike@test.com", "359899000003");

        User result = userRepository.findByPhoneNumber("359899000003").orElse(null);

        assertThat(result).isNotNull();
        Assertions.assertNotNull(result);
        assertThat(result.getPhoneNumber()).isEqualTo("359899000003");
    }

    @Test
    void findByUsername_shouldReturnEmptyForNonExisting() {

        Optional<User> result = userRepository.findByUsername("missing");

        assertThat(result).isEmpty();
    }


    private void createUser(String username, String email, String phone) {

        User user = User.builder()
                .username(username)
                .password("pass123")
                .firstName("Valentin")
                .lastName("Yanev")
                .email(email)
                .phoneNumber(phone)
                .role(UserRole.USER)
                .isActive(true)
                .hourlyRate(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        em.persistFlushFind(user);
    }
}
