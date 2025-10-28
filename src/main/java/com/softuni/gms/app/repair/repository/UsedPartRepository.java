package com.softuni.gms.app.repair.repository;

import com.softuni.gms.app.repair.model.UsedPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UsedPartRepository extends JpaRepository<UsedPart, UUID> {

}
