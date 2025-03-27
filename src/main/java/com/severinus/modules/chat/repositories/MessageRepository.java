package com.severinus.modules.chat.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.severinus.modules.chat.entities.ChatEntity;
import com.severinus.modules.chat.entities.MessageEntity;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID>{
  
    public List<MessageEntity> findByChat(ChatEntity chat);
} 
