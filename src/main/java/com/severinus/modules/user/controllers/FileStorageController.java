package com.severinus.modules.user.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.severinus.modules.user.entities.FileStorageProperties;
import com.severinus.modules.user.entities.WorkerEntity;
import com.severinus.modules.user.repositories.WorkerRepository;

import jakarta.servlet.http.HttpServletRequest;

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

    @PostMapping("/upload/certificates/{userId}")
    @Transactional
    public ResponseEntity<String> uploadFile(@PathVariable UUID userId, @RequestParam MultipartFile[] files) {
        WorkerEntity worker = workerRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Trabalhador não encontrado"));
        
        List<String> updatedCertificatePaths = new ArrayList<>(worker.getCertificados());
        List<String> failedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            @SuppressWarnings("null")
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());

            try {
                Path targetLocation = fileStorageLocation.resolve(fileName);
                file.transferTo(targetLocation);
                updatedCertificatePaths.add(targetLocation.toString());

                String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/download/")
                    .path(fileName)
                    .toUriString();
                updatedCertificatePaths.add(fileDownloadUri);
            } catch (IOException e) {
                failedFiles.add(fileName);
            }
        }

        if (!updatedCertificatePaths.isEmpty()) {
            worker.setCertificados(updatedCertificatePaths);
            workerRepository.save(worker);
        }

        if (!failedFiles.isEmpty()) {
            return ResponseEntity.badRequest().body("Falha ao fazer upload dos arquivos: " + String.join(", ", failedFiles));
        }
        
        return ResponseEntity.ok("Upload concluído com sucesso");
    }

    @GetMapping("/download/certificates/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) throws IOException {
        Path filePath = fileStorageLocation.resolve(fileName).normalize();

        try {
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +resource.getFilename() + "\"").body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }

    }

    @GetMapping("/certificates/{userId}")
    public ResponseEntity<List<String>> getUserCertificates(@PathVariable UUID userId) {
        WorkerEntity worker = workerRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Trabalhador não encontrado"));
        
        return ResponseEntity.ok(worker.getCertificados());
    }

}
