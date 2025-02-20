package com.severinus.modules.user.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.severinus.modules.user.entities.ChatEntity;
import com.severinus.modules.user.entities.UserEntity;

public interface ChatRepository extends JpaRepository<ChatEntity, UUID> {
    
    public Optional<ChatEntity> findByUsuario1OrUsuario2(UserEntity usuario1, UserEntity usuario2);
}
