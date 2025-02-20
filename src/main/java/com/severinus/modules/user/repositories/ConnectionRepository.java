package com.severinus.modules.user.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.severinus.modules.user.entities.ConnectionEntity;
import com.severinus.modules.user.entities.ConnectionEntity.ConnectionStatus;

public interface ConnectionRepository extends JpaRepository<ConnectionEntity, UUID>{
    
    public List<ConnectionEntity> findByStatus(ConnectionStatus status);
}
