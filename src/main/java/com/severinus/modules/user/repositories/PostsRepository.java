package com.severinus.modules.user.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.severinus.modules.user.entities.PostsEntity;
import java.time.LocalDateTime;


public interface PostsRepository extends JpaRepository<PostsEntity, UUID>{
    
    public List<PostsEntity> findByUsuarioId(UUID usuarioId);

    public List<PostsEntity> findByTags(List<String> tags);

    public List<PostsEntity> findByDataDoPost(LocalDateTime dataDoPost);
}
