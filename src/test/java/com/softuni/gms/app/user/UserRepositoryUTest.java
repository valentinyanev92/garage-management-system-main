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

    private void createUser(String username, String email, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("pass123");
        u.setFirstName("Valentin");
        u.setLastName("Yanev");
        u.setEmail(email);
        u.setPhoneNumber(phone);
        u.setRole(UserRole.USER);
        u.setIsActive(true);
        u.setHourlyRate(BigDecimal.ZERO);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());

        em.persistFlushFind(u);
    }

    @Test
    void findByUsername_shouldReturnCorrectUser() {
        createUser("valyo", "v1@test.com", "359899000001");

        var result = userRepository.findByUsername("valyo").orElse(null);

        assertThat(result).isNotNull();
        Assertions.assertNotNull(result);
        assertThat(result.getUsername()).isEqualTo("valyo");
    }

    @Test
    void findByEmail_shouldReturnCorrectUser() {
        createUser("john", "john@test.com", "359899000002");

        var result = userRepository.findByEmail("john@test.com").orElse(null);

        assertThat(result).isNotNull();
        Assertions.assertNotNull(result);
        assertThat(result.getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void findByPhoneNumber_shouldReturnCorrectUser() {
        createUser("mike", "mike@test.com", "359899000003");

        var result = userRepository.findByPhoneNumber("359899000003").orElse(null);

        assertThat(result).isNotNull();
        Assertions.assertNotNull(result);
        assertThat(result.getPhoneNumber()).isEqualTo("359899000003");
    }

    @Test
    void findByUsername_shouldReturnEmptyForNonExisting() {
        var result = userRepository.findByUsername("missing");

        assertThat(result).isEmpty();
    }
}
