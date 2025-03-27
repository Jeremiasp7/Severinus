package com.severinus.modules.chat.entities;

import java.util.List;
import java.util.UUID;

import com.severinus.modules.user.entities.UserEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "chats")
@Builder
public class ChatEntity {
    
    @Id
    @GeneratedValue(strategy =  GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "usuario1_id", nullable = false)
    private UserEntity usuario1;

    @ManyToOne
    @JoinColumn(name = "usuario2_id", nullable = false)
    private UserEntity usuario2;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageEntity> mensagens;
}
