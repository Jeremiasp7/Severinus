package com.severinus.modules.user.services;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.severinus.modules.user.dto.CreateConnectionDto;
import com.severinus.modules.user.entities.ConnectionEntity;
import com.severinus.modules.user.entities.ConnectionEntity.ConnectionStatus;
import com.severinus.modules.user.repositories.ConnectionRepository;

@Service
public class ConnectionService {
    
    @Autowired
    private ConnectionRepository connectionRepository;

    public ConnectionEntity criarConexao(CreateConnectionDto dto) {
        ConnectionEntity connection = ConnectionEntity.builder()
            .solicitanteId(dto.getSolicitanteId())
            .recebedorId(dto.getRecebedorId())
            .status(ConnectionStatus.PENDENTE)
            .build();

        return connectionRepository.save(connection);
    }

    public List<ConnectionEntity> buscarSolicitacoes(UUID id) {
        return connectionRepository.findByIdAndStatus(id, ConnectionStatus.PENDENTE);
    }
}
