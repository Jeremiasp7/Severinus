package com.severinus.modules.user.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.severinus.modules.user.entities.FileStorageProperties;
import com.severinus.modules.user.entities.WorkerEntity;
import com.severinus.modules.user.repositories.WorkerRepository;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@RequestMapping("/api/files")
public class FileStorageController {
    
    private final Path fileStorageLocation;

    private final WorkerRepository workerRepository;

    public FileStorageController(FileStorageProperties fileStorageProperties, WorkerRepository workerRepository) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
            .toAbsolutePath().normalize();
        this.workerRepository = workerRepository;

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @PostMapping("/upload/{userId}")
    @Transactional
    public ResponseEntity<String> uploadFile(@PathVariable UUID userId,@RequestParam MultipartFile[] files) {
        WorkerEntity worker = workerRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Trabalhador não encontrado"));
        
        List<String> updatedCertificatePaths = new ArrayList<>(worker.getCertificados());

        for (MultipartFile file : files) {
            @SuppressWarnings("null")
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());

            try {
                Path targetLocation = fileStorageLocation.resolve(fileName);
                file.transferTo(targetLocation);
    
                updatedCertificatePaths.add(targetLocation.toString());
                return ResponseEntity.ok("Upload completed!");
            } catch (IOException e) {
                return ResponseEntity.badRequest().body("Falha no upload do arquivo" +fileName);
            }
        }

        worker.setCertificados(updatedCertificatePaths);
        workerRepository.save(worker);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/certificates/{userId}")
    public ResponseEntity<List<String>> getUserCertificates(@PathVariable UUID userId) {
        WorkerEntity worker = workerRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Trabalhador não encontrado"));
        
        return ResponseEntity.ok(worker.getCertificados());
    }

}
