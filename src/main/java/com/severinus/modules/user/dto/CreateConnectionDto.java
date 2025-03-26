package com.severinus.modules.user.dto;

import com.severinus.modules.user.entities.UserEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConnectionDto {
    
    private UserEntity solicitante;
    private UserEntity recebedor;
}
