package com.severinus.modules.chat.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.severinus.modules.chat.entities.ChatEntity;
import com.severinus.modules.user.entities.UserEntity;

public interface ChatRepository extends JpaRepository<ChatEntity, UUID>{
    
    public ChatEntity findByUser1AndUser2(UserEntity user1, UserEntity user2);
}
