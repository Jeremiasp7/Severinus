package com.severinus.modules.user.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
@Entity(name = "posts")
@Builder
public class PostsEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "post_content")
    private String content;

    @Column(name = "post_tags")
    private List<String> tags;

    @ElementCollection
    private List<String> imagePaths; // base64

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity usuario;

    @Column(name = "post_data")
    @CreationTimestamp
    private LocalDateTime dataDoPost;
}
