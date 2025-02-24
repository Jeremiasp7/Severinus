package com.severinus.modules.user.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.severinus.modules.user.dto.CreateWorkerDto;
import com.severinus.modules.user.entities.WorkerEntity;
import com.severinus.modules.user.repositories.WorkerRepository;

@Service
public class WorkerService {
    
    @Autowired
    private WorkerRepository workerRepository;

    public WorkerEntity criarTrabalhador(CreateWorkerDto dto) {
        WorkerEntity worker = WorkerEntity.builder()
            .cpf_cnpj(dto.getCpf_cnpj())
            .nomeCompleto(dto.getNomeCompleto())
            .nomeDeUsuario(dto.getNomeDeUsuario())
            .email(dto.getEmail())
            .password(dto.getPassword())
            .build();

        return workerRepository.save(worker);
    }

    public List<WorkerEntity> buscarTrabalhadores() {
        return workerRepository.findAll();
    }

}
