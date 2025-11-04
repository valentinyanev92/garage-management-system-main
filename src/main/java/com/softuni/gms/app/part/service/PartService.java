package com.softuni.gms.app.part.service;

import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.repository.PartRepository;
import com.softuni.gms.app.web.dto.PartAddRequest;
import com.softuni.gms.app.web.dto.PartEditRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PartService {

    private final PartRepository partRepository;

    @Autowired
    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public Part findPartById(UUID partId) {
        return partRepository.findById(partId)
                .orElseThrow(() -> new NotFoundException("Part not found"));
    }

    @Cacheable(value = "parts")
    public List<Part> findAllParts() {
        return partRepository.findByIsDeletedFalse();
    }

    @CacheEvict(value = "parts", allEntries = true)
    public Part createPart(PartAddRequest partAddRequest) {
        LocalDateTime now = LocalDateTime.now();
        
        Part part = Part.builder()
                .name(partAddRequest.getName())
                .manufacturer(partAddRequest.getManufacturer())
                .price(partAddRequest.getPrice())
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return partRepository.save(part);
    }

    @CacheEvict(value = "parts", allEntries = true)
    public Part updatePart(UUID partId, PartEditRequest partEditRequest) {
        Part part = findPartById(partId);

        part.setName(partEditRequest.getName());
        part.setManufacturer(partEditRequest.getManufacturer());
        part.setPrice(partEditRequest.getPrice());
        part.setUpdatedAt(LocalDateTime.now());

        return partRepository.save(part);
    }

    @CacheEvict(value = "parts", allEntries = true)
    public void deletePart(UUID partId) {
        Part part = findPartById(partId);
        part.setDeleted(true);
        part.setUpdatedAt(LocalDateTime.now());
        partRepository.save(part);
    }
}
