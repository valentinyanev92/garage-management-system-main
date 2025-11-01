package com.softuni.gms.app.part.service;

import com.softuni.gms.app.exeption.NotFoundException;
import com.softuni.gms.app.part.model.Part;
import com.softuni.gms.app.part.repository.PartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public List<Part> findAllParts() {
        return partRepository.findAll();
    }
}
