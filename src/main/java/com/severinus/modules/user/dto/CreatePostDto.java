package com.severinus.modules.user.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePostDto {
    
    private String content;
    private List<String> tags;
    private UUID utilizador;
    private List<String> images;
}
