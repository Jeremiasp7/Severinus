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

    
    public ChatEntity criarChat(UserEntity user1, UserEntity user2, CreateChatDto dto) {
        List<ConnectionEntity> solicitacoes = connectionService.buscarSolicitacoes(user1);
        
        for (ConnectionEntity user : solicitacoes) {
            if (user.getSolicitante().equals(user2)) {
                if (user.getStatus().equals(ConnectionStatus.ACEITA)) {
                    ChatEntity chat = ChatEntity.builder()
                        .usuario1(dto.getUser1())
                        .usuario2(dto.getUser2())
                        .build();

                        return chatRepository.save(chat);
                }
            }
        }

        return null;
    }

    public void mandarMensagem(UserEntity remetente, UserEntity destinatario, String mensagem) {
        ChatEntity chat = chatRepository.findByUser1AndUser2(remetente, destinatario);

        if (chat == null) {
            chat = chatRepository.findByUser1AndUser2(destinatario, remetente);
        }

        MessageEntity message = MessageEntity.builder()
            .remetente(remetente)
            .destinatario(destinatario)
            .chat(chat)
            .conteudo(mensagem)
            .dataEnvio(LocalDateTime.now())
            .build();
        
        messageRepository.save(message);

        simpMessagingTemplate.convertAndSendToUser(destinatario.getNomeDeUsuario(), "/queue/messages", message);
    }

    public List<MessageEntity> historicoDoChat(UUID chatId) {
        // Busca o chat pelo chatId
        ChatEntity chat = chatRepository.getReferenceById(chatId);
    
        // Retorna as mensagens associadas a esse chat
        return messageRepository.findByChat(chat);
    }
    
}
