package com.softuni.gms.app.part;

import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.repository.PartRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@EntityScan(basePackages = "com.softuni.gms.app")
@EnableJpaRepositories(basePackages = "com.softuni.gms.app")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PartRepositoryTest {

    @Autowired
    private PartRepository partRepository;

    @Test
    void findByIsDeletedFalse_shouldReturnOnlyActiveParts() {

        Part p1 = Part.builder()
                .name("Oil Filter")
                .manufacturer("TestCorp")
                .price(BigDecimal.TEN)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Part p2 = Part.builder()
                .name("Air Filter")
                .manufacturer("TestCorp")
                .price(BigDecimal.ONE)
                .isDeleted(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        partRepository.saveAll(List.of(p1, p2));

        List<Part> result = partRepository.findByIsDeletedFalse();

        assertThat(result)
                .hasSize(1)
                .extracting(Part::getName)
                .containsExactly("Oil Filter");
    }

}