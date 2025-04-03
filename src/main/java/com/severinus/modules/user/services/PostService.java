package com.severinus.modules.user.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.severinus.modules.user.dto.CreatePostDto;
import com.severinus.modules.user.entities.PostsEntity;
import com.severinus.modules.user.repositories.PostsRepository;

@Service
public class PostService {
    
    @Autowired
    private PostsRepository postsRepository;

    public PostsEntity criarPost(CreatePostDto dto) {
        PostsEntity post = PostsEntity.builder()
            .content(dto.getContent())
            .dataDoPost(LocalDateTime.now())
            .tags(dto.getTags())
            .utilizador(dto.getUtilizador())
            .imagePaths(dto.getImages())
            .build();
        
        return postsRepository.save(post);
    }

    public List<PostsEntity> buscarPosts(UUID id) {
        return postsRepository.findByUsuarioId(id);
    }
}
