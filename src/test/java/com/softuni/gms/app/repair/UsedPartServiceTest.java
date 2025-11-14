package com.softuni.gms.app.repair;

import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.repair.model.RepairOrder;
import com.softuni.gms.app.repair.model.UsedPart;
import com.softuni.gms.app.repair.repository.UsedPartRepository;
import com.softuni.gms.app.repair.service.UsedPartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UsedPartServiceTest {

    @Mock
    private UsedPartRepository usedPartRepository;

    @InjectMocks
    private UsedPartService usedPartService;

    @Test
    void createUsedPart_shouldCreate() {

        RepairOrder repairOrder = new RepairOrder();
        Part part = Part.builder()
                .price(BigDecimal.ONE)
                .build();

        int quantity = 3;

        usedPartService.createUsedPart(repairOrder, part, quantity);

        ArgumentCaptor<UsedPart> captor = ArgumentCaptor.forClass(UsedPart.class);
        verify(usedPartRepository).save(captor.capture());

        UsedPart usedPart = captor.getValue();

        assertEquals(repairOrder, usedPart.getRepairOrder());
        assertEquals(part, usedPart.getPart());
        assertEquals(3, usedPart.getQuantity());
        assertEquals(BigDecimal.valueOf(3), usedPart.getTotalPrice());
    }
}
