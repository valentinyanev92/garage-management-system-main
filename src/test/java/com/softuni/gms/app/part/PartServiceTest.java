package com.softuni.gms.app.part;

import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.repository.PartRepository;
import com.softuni.gms.app.part.service.PartService;
import com.softuni.gms.app.web.dto.PartAddRequest;
import com.softuni.gms.app.web.dto.PartEditRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PartServiceTest {

    @Mock
    private PartRepository partRepository;

    @InjectMocks
    private PartService partService;

    @Test
    void findPartById_shouldReturn_whenFound() {

        UUID id = UUID.randomUUID();
        Part part = Part.builder()
                .id(id)
                .build();

        when(partRepository.findById(id)).thenReturn(Optional.of(part));

        Part foundPart = partService.findPartById(id);

        assertSame(part, foundPart);
    }

    @Test
    void findPartById_shouldReturn_whenNotFound() {

        UUID id = UUID.randomUUID();

        when(partRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                partService.findPartById(id));
    }

    @Test
    void findAll_shouldReturn_whenFound() {

        Part part = Part.builder()
                .id(UUID.randomUUID())
                .build();

        when(partRepository.findByIsDeletedFalse()).thenReturn(List.of(part));

        int partResultSize = partService.findAllParts().size();

        assertEquals(1, partResultSize);
    }

    @Test
    void createPart_shouldCreatePart() {

        PartAddRequest partAddRequest = new PartAddRequest();
        partAddRequest.setName("Test");
        partAddRequest.setManufacturer("Test");
        partAddRequest.setPrice(BigDecimal.ONE);

        partService.createPart(partAddRequest);

        ArgumentCaptor<Part> captor = ArgumentCaptor.forClass(Part.class);
        verify(partRepository).save(captor.capture());

        Part savedPart = captor.getValue();

        assertEquals("Test", savedPart.getName());
        assertEquals("Test", savedPart.getManufacturer());
        assertEquals(BigDecimal.ONE, savedPart.getPrice());
        assertFalse(savedPart.isDeleted());
        assertNotNull(savedPart.getCreatedAt());
        assertNotNull(savedPart.getUpdatedAt());
    }

    @Test
    void updatePart_shouldUpdatePart() {

        UUID id = UUID.randomUUID();
        Part part = Part.builder()
                .id(id)
                .name("Old part")
                .manufacturer("Old times")
                .price(BigDecimal.ONE)
                .build();

        when(partRepository.findById(id)).thenReturn(Optional.of(part));

        PartEditRequest partEditRequest = new PartEditRequest();
        partEditRequest.setName("New part");
        partEditRequest.setManufacturer("New times");
        partEditRequest.setPrice(BigDecimal.TEN);

        partService.updatePart(id, partEditRequest);

        ArgumentCaptor<Part> captor = ArgumentCaptor.forClass(Part.class);
        verify(partRepository).save(captor.capture());

        Part savedPart = captor.getValue();
        assertEquals("New part", savedPart.getName());
        assertEquals("New times", savedPart.getManufacturer());
        assertEquals(BigDecimal.TEN, savedPart.getPrice());
        assertFalse(savedPart.isDeleted());
        assertNotNull(savedPart.getUpdatedAt());
    }

    @Test
    void deletePart_shouldMarkAsDeleted() {

        UUID id = UUID.randomUUID();
        Part part = Part.builder()
                .id(id)
                .isDeleted(false)
                .build();

        when(partRepository.findById(id)).thenReturn(Optional.of(part));

        partService.deletePart(id);

        assertTrue(part.isDeleted());
        verify(partRepository).save(part);
    }
}
