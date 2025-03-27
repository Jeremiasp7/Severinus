package com.severinus.modules.user.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.severinus.modules.user.entities.ConnectionEntity;
import com.severinus.modules.user.entities.ConnectionEntity.ConnectionStatus;
import com.severinus.modules.user.entities.UserEntity;

public interface ConnectionRepository extends JpaRepository<ConnectionEntity, UUID>{
    
    Optional<ConnectionEntity> findBySolicitanteAndRecebedorAndStatus(UserEntity solicitante, UserEntity recebedor, ConnectionStatus status);

    public List<ConnectionEntity> findByUserAndStatus(UserEntity user, ConnectionStatus pendente);
}
