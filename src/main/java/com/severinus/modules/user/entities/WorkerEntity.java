package com.severinus.modules.user.entities;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "workers")
@Builder
public class WorkerEntity {
    
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String cpf_cnpj;

    @Column(name = "nome_completo")
    private String nomeCompleto;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String usuario;

    @Column(name = "user_password")
    private String password;

    @ElementCollection
    private List<String> certificados; // Lista de url dos certificados

    @ElementCollection
    private List<String> profissões; // Lista de url dos certificados
}
