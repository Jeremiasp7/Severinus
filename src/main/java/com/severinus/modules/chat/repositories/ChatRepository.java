package com.severinus.modules.chat.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.severinus.modules.chat.entities.ChatEntity;

public interface ChatRepository extends JpaRepository<ChatEntity, UUID>{
    
    public ChatEntity findByConsumer1AndConsumer2(UUID consumer1Id, UUID consumer2Id);
}
