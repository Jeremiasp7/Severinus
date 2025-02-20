package com.severinus.modules.user.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.severinus.modules.user.entities.MessageEntity;
import java.time.LocalDateTime;


public interface MessageRepository extends JpaRepository<MessageEntity, UUID>{
    
    public List<MessageEntity> findByDataEnvio(LocalDateTime dataEnvio);
}
