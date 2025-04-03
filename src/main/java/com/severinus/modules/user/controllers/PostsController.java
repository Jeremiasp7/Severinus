package com.severinus.modules.user.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.severinus.modules.user.dto.CreatePostDto;
import com.severinus.modules.user.entities.FileStorageProperties;
import com.severinus.modules.user.entities.PostsEntity;
import com.severinus.modules.user.repositories.UserRepository;
import com.severinus.modules.user.repositories.WorkerRepository;
import com.severinus.modules.user.services.PostService;

import jakarta.transaction.Transactional;

@Controller
@RequestMapping("/posts")
public class PostsController {
    
    @Autowired
    private PostService postService;

    private final Path fileStorageLocation;

    public PostsController(FileStorageProperties fileStorageProperties, WorkerRepository workerRepository, UserRepository userRepository) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
            .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @PostMapping("/{userId}")
    @Transactional
    public ResponseEntity<?> criarPost(@PathVariable UUID userId, @RequestParam MultipartFile[] images, @RequestBody CreatePostDto dto) {
        List<String> imagePaths = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
            
        for (MultipartFile image : images) {
            @SuppressWarnings("null")
            String fileName = StringUtils.cleanPath(image.getOriginalFilename());
                
            try {
                Path targetLocation = fileStorageLocation.resolve(fileName);
                image.transferTo(targetLocation);
                imagePaths.add(targetLocation.toString());
            } catch (IOException e) {
                failedFiles.add(fileName);
            }
        }

        if(!imagePaths.isEmpty()) {
            dto.setImages(imagePaths);
        }

        if(!failedFiles.isEmpty()) {
            return ResponseEntity.badRequest().body("Falha ao fazer upload dos arquivos: " + String.join(", ", failedFiles));
        }

        PostsEntity post = postService.criarPost(dto);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/get/{userId}")
    public List<PostsEntity> buscarTodosUsuarios(@RequestParam UUID userId) {
        return postService.buscarPosts(userId);
    }
}
