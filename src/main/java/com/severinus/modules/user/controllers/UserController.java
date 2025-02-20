package com.severinus.modules.user.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.severinus.modules.user.dto.CreateUserDto;
import com.severinus.modules.user.entities.UserEntity;
//import com.severinus.modules.user.repositories.UserRepository;
import com.severinus.modules.user.services.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/user")
public class UserController {
    
    //@Autowired
    //private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UserEntity> criarUsuario(@RequestBody CreateUserDto dto) {
        UserEntity user = userService.criarUsuario(dto);
        return ResponseEntity.ok(user);
    }
    
}
