package com.severinus.modules.chat.dto;

import com.severinus.modules.user.entities.UserEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChatDto {
    
    private UserEntity user1;
    private UserEntity user2;
}
