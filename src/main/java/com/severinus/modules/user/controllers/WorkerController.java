package com.severinus.modules.user.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.severinus.modules.user.dto.CreateWorkerDto;
import com.severinus.modules.user.entities.WorkerEntity;
import com.severinus.modules.user.services.WorkerService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/worker")
public class WorkerController {
    
    @Autowired
    private WorkerService workerService;

    @PostMapping
    public ResponseEntity<WorkerEntity> criarTrabalhador(@RequestBody CreateWorkerDto dto) {
        WorkerEntity worker = workerService.criarTrabalhador(dto);
        return ResponseEntity.ok(worker);
    }

    @GetMapping("/workers")
    public List<WorkerEntity> buscarTodosTrabalhadores() {
        return workerService.buscarTrabalhadores();
    }
    
}
