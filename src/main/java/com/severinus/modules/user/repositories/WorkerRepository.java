package com.severinus.modules.user.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.severinus.modules.user.entities.WorkerEntity;


public interface WorkerRepository extends JpaRepository<WorkerEntity, UUID> {
    
    public WorkerEntity findWorkerById(UUID id);
}
