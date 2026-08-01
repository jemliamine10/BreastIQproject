package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.LoginRequestDto;
import com.breastcancer.breastcancerbackend.dto.LoginResponseDto;
import com.breastcancer.breastcancerbackend.entity.User;
import com.breastcancer.breastcancerbackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Authentification simple : compare le mot de passe brut avec le passwordHash stocké.
     * (À remplacer par BCrypt quand la sécurité sera mise en place.)
     */
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        if (request == null) throw new BadRequestException("Payload requis.");

        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        if (email == null || email.isBlank()) throw new BadRequestException("Email requis.");

        String password = request.getPassword();
        if (password == null || password.isBlank()) throw new BadRequestException("Mot de passe requis.");

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Email ou mot de passe incorrect."));

        // comparaison brute (pas de hashing pour l'instant)
        if (!user.getPasswordHash().equals(password)) {
            throw new UnauthorizedException("Email ou mot de passe incorrect.");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Compte désactivé.");
        }

        // mettre à jour la date de dernière connexion
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return LoginResponseDto.of(user);
    }
}
