package com.severinus.modules.user.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.severinus.modules.user.entities.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID>{
    
    public List<UserEntity> findByNomeDeUsuario(String nomeDeUsuario);
}
