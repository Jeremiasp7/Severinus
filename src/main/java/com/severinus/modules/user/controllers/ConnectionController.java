package com.severinus.modules.user.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.severinus.modules.user.dto.CreateConnectionDto;
import com.severinus.modules.user.entities.ConnectionEntity;
import com.severinus.modules.user.services.ConnectionService;

@RestController
@RequestMapping("/connection")
public class ConnectionController {
    
    @Autowired
    private ConnectionService connectionService;

    @PostMapping
    public ResponseEntity<ConnectionEntity> criarConexao(@RequestBody CreateConnectionDto dto) {
        ConnectionEntity connection = connectionService.criarConexao(dto);
        return ResponseEntity.ok(connection);
    }

    @GetMapping("/connections/{userId}")
    public List<ConnectionEntity> buscarConexoes(@PathVariable UUID userId) {

        return connectionService.buscarSolicitacoes(userId);
    }
}
