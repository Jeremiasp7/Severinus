package com.severinus.modules.chat.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.severinus.modules.chat.entities.MessageEntity;
import com.severinus.modules.chat.services.ChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/history/{chatId}")
    public ResponseEntity<List<MessageEntity>> historicoDoChat(@PathVariable UUID chatId) {
        List<MessageEntity> historico = chatService.historicoDoChat(chatId);
        return ResponseEntity.ok(historico);
    }

    @MessageMapping("/sendMessage")
    public void processMessage(@Payload MessageEntity message) {
        chatService.mandarMensagem(message.getRemetente(), message.getDestinatario(), message.getConteudo());
        messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(), message);
    }
}
