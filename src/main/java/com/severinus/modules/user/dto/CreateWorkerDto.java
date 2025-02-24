package com.severinus.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateWorkerDto {
    
    private String cpf_cnpj;
    private String nomeCompleto;
    private String email;
    private String nomeDeUsuario;
    private String password;

}
