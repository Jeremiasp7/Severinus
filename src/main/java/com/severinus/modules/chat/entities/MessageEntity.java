package com.severinus.modules.chat.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import com.severinus.modules.user.entities.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "messages")
@Builder
public class MessageEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chat;

    @ManyToOne
    @JoinColumn(name = "destinatario_id")
    private UserEntity destinatario;

    @ManyToOne
    @JoinColumn(name = "remetente_id")
    private UserEntity remetente;

    @Column(nullable = false, length = 1000)
    private String conteudo;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime dataEnvio;

    private Boolean mensagemLida;
}
