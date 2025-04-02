package com.severinus.modules.chat.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChatDto {
    
    private UUID utilizador1;
    private UUID utilizador2;
}
