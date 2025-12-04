package com.softuni.gms.app.part.init;

import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.repository.PartRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PartDataInitializer implements ApplicationRunner {

    private final PartRepository partRepository;

    public PartDataInitializer(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (partRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<Part> parts = List.of(
                Part.builder()
                        .name("Oil Filter")
                        .manufacturer("BMW")
                        .price(BigDecimal.valueOf(20))
                        .createdAt(now)
                        .updatedAt(now)
                        .isDeleted(false)
                        .build(),

                Part.builder()
                        .name("Air Filter")
                        .manufacturer("BMW")
                        .price(BigDecimal.valueOf(25))
                        .createdAt(now)
                        .updatedAt(now)
                        .isDeleted(false)
                        .build(),

                Part.builder()
                        .name("Brake Pads")
                        .manufacturer("ATE")
                        .price(BigDecimal.valueOf(120))
                        .createdAt(now)
                        .updatedAt(now)
                        .isDeleted(false)
                        .build(),

                Part.builder()
                        .name("Spark Plug")
                        .manufacturer("NGK")
                        .price(BigDecimal.valueOf(18))
                        .createdAt(now)
                        .updatedAt(now)
                        .isDeleted(false)
                        .build(),

                Part.builder()
                        .name("Engine Oil 5W30")
                        .manufacturer("Castrol")
                        .price(BigDecimal.valueOf(60))
                        .createdAt(now)
                        .updatedAt(now)
                        .isDeleted(false)
                        .build(),

                Part.builder()
                        .name("Coolant")
                        .manufacturer("BMW")
                        .price(BigDecimal.valueOf(35))
                        .createdAt(now)
                        .updatedAt(now)
                        .isDeleted(false)
                        .build()
        );

        partRepository.saveAll(parts);
    }
}
