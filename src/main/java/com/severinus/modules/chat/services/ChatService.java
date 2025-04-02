package com.severinus.modules.chat.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.severinus.modules.chat.dto.CreateChatDto;
import com.severinus.modules.chat.entities.ChatEntity;
import com.severinus.modules.chat.entities.MessageEntity;
import com.severinus.modules.chat.repositories.ChatRepository;
import com.severinus.modules.chat.repositories.MessageRepository;
import com.severinus.modules.user.entities.UserEntity;
import com.severinus.modules.user.entities.ConnectionEntity.ConnectionStatus;
import com.severinus.modules.user.repositories.UserRepository;
import com.severinus.modules.user.services.ConnectionService;
import com.severinus.modules.user.entities.ConnectionEntity;


@Service
public class ChatService {
    
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private UserRepository userRepository;

    
    public ChatEntity criarChat(UUID id1, UUID id2, CreateChatDto dto) {
        List<ConnectionEntity> solicitacoes = connectionService.buscarSolicitacoes(id1);
        
        for (ConnectionEntity conec : solicitacoes) {
            if (conec.getSolicitanteId().equals(id2)) {
                if (conec.getStatus().equals(ConnectionStatus.ACEITA)) {
                    ChatEntity chat = ChatEntity.builder()
                        .utilizador1(dto.getUtilizador1())
                        .utilizador2(dto.getUtilizador2())
                        .build();

                        return chatRepository.save(chat);
                }
            }
        }

        return null;
    }

    public void mandarMensagem(UUID remetenteId, UUID destinatarioId, String mensagem) {
        ChatEntity chat = chatRepository.findByConsumer1AndConsumer2(remetenteId, destinatarioId);

        if (chat == null) {
            chat = chatRepository.findByConsumer1AndConsumer2(destinatarioId, remetenteId);
        }

        MessageEntity message = MessageEntity.builder()
            .remetente(remetenteId)
            .destinatario(destinatarioId)
            .chat(chat)
            .conteudo(mensagem)
            .dataEnvio(LocalDateTime.now())
            .build();
        
        messageRepository.save(message);

        UserEntity user = userRepository.findUserById(destinatarioId);

        simpMessagingTemplate.convertAndSendToUser(user.getNomeDeUsuario(), "/queue/messages", message);
    }

    public List<MessageEntity> historicoDoChat(UUID chatId) {
        ChatEntity chat = chatRepository.getReferenceById(chatId);
    
        return messageRepository.findByChat(chat);
    }
    
}
