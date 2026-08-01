package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.LoginRequestDto;
import com.breastcancer.breastcancerbackend.dto.LoginResponseDto;
import com.breastcancer.breastcancerbackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/login
     * Body : { "email": "...", "password": "..." }
     * Retourne les infos utilisateur si les identifiants sont valides.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
