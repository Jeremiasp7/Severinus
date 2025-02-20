package com.severinus.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUserDto {
    
    private String cpf;
    private String email;
    private String nomeCompleto;
    private String nomeDeUsuario;
    private String password;
}
