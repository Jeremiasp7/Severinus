package com.severinus.modules.user.entities;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "connection")
@Builder
public class ConnectionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "usuario_solicitante_id")
    private UserEntity solicitante;

    @ManyToOne
    @JoinColumn(name = "usuario_recebedor_id")
    private UserEntity recebedor;

    @Enumerated(EnumType.STRING)
    private ConnectionStatus status;

    public enum ConnectionStatus {
        PENDENTE, ACEITA, RECUSADA
    }
}
