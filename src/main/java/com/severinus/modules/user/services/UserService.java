package com.severinus.modules.user.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.severinus.modules.user.dto.CreateUserDto;
import com.severinus.modules.user.entities.UserEntity;
import com.severinus.modules.user.repositories.UserRepository;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;

    public UserEntity criarUsuario(CreateUserDto dto) {
        UserEntity user = UserEntity.builder()
            .nomeCompleto(dto.getNomeCompleto())
            .cpf(dto.getCpf())
            .nomeDeUsuario(dto.getNomeDeUsuario())
            .email(dto.getEmail())
            .password(dto.getPassword())
            .build();
        
        return userRepository.save(user);
    }

    public List<UserEntity> buscarUsuarios() {
        return userRepository.findAll();
    }
}
