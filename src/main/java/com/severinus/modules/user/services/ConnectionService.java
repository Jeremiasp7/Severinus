package com.severinus.modules.user.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.severinus.modules.user.dto.CreateConnectionDto;
import com.severinus.modules.user.entities.ConnectionEntity;
import com.severinus.modules.user.entities.ConnectionEntity.ConnectionStatus;
import com.severinus.modules.user.entities.UserEntity;
import com.severinus.modules.user.repositories.ConnectionRepository;

@Service
public class ConnectionService {
    
    @Autowired
    private ConnectionRepository connectionRepository;

    public ConnectionEntity criarConexao(CreateConnectionDto dto) {
        ConnectionEntity connection = ConnectionEntity.builder()
            .solicitante(dto.getSolicitante())
            .recebedor(dto.getRecebedor())
            .status(ConnectionStatus.PENDENTE)
            .build();

        return connectionRepository.save(connection);
    }

    public List<ConnectionEntity> buscarSolicitacoes(UserEntity user) {
        return connectionRepository.findByUserAndStatus(user, ConnectionStatus.PENDENTE);
    }
}
