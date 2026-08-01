package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.entity.User;
import com.breastcancer.breastcancerbackend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

@Service
public class ProfilePhotoService {

    private static final Logger LOG = LoggerFactory.getLogger(ProfilePhotoService.class);

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB

    private final Path storageRoot;
    private final UserRepository userRepository;

    public ProfilePhotoService(
            @Value("${app.storage.profile-photos.path:./uploads/profile-photos}") String path,
            UserRepository userRepository
    ) {
        this.storageRoot = Paths.get(path).toAbsolutePath().normalize();
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(storageRoot);
            LOG.info("Profile photos storage initialized: {}", storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create profile-photos directory: " + storageRoot, e);
        }
    }

    // ==========================================
    // Upload / Replace
    // ==========================================

    @Transactional
    public String uploadPhoto(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Aucun fichier fourni.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Type de fichier non autorisé. Utilisez JPEG, PNG, WebP ou GIF.");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("Le fichier ne doit pas dépasser 5 Mo.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + userId));

        // Delete old photo if exists
        deletePhotoFile(userId);

        // Determine extension
        String ext = getExtension(file.getOriginalFilename(), contentType);
        String filename = userId.toString() + "." + ext;

        try {
            Path target = storageRoot.resolve(filename).normalize();
            if (!target.startsWith(storageRoot)) {
                throw new BadRequestException("Chemin de stockage invalide.");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Profile photo stored: {}", filename);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du stockage de la photo de profil.", e);
        }

        // Update user
        String photoUrl = "/api/profile-photos/" + userId;
        user.setProfilePhotoUrl(photoUrl);
        userRepository.save(user);

        return photoUrl;
    }

    // ==========================================
    // Load / Serve
    // ==========================================

    public Resource loadPhoto(UUID userId) {
        Path file = findPhotoFile(userId);
        if (file == null) {
            // Return default photo instead of throwing NotFoundException to keep console clean
            Path defaultPhoto = storageRoot.resolve("default/blank-profile.png");
            if (!Files.exists(defaultPhoto)) {
                // Return a simple empty resource or throw if absolutely necessary
                throw new NotFoundException("Aucune photo de profil.");
            }
            file = defaultPhoto;
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("Photo de profil introuvable.");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("Photo de profil introuvable.");
        }
    }

    public String detectContentType(UUID userId) {
        Path file = findPhotoFile(userId);
        if (file == null) return "image/png"; // Fallback for blank-profile.png
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    // ==========================================
    // Delete
    // ==========================================

    @Transactional
    public void deletePhoto(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + userId));

        deletePhotoFile(userId);
        user.setProfilePhotoUrl(null);
        userRepository.save(user);
        LOG.info("Profile photo deleted for user: {}", userId);
    }

    // ==========================================
    // Internal helpers
    // ==========================================

    private Path findPhotoFile(UUID userId) {
        String base = userId.toString();
        for (String ext : new String[]{"jpg", "jpeg", "png", "webp", "gif"}) {
            Path candidate = storageRoot.resolve(base + "." + ext);
            if (Files.exists(candidate)) return candidate;
        }
        return null;
    }

    private void deletePhotoFile(UUID userId) {
        Path existing = findPhotoFile(userId);
        if (existing != null) {
            try {
                Files.deleteIfExists(existing);
                LOG.info("Old profile photo deleted: {}", existing.getFileName());
            } catch (IOException e) {
                LOG.warn("Could not delete old profile photo: {}", existing, e);
            }
        }
    }

    private String getExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (Set.of("jpg", "jpeg", "png", "webp", "gif").contains(ext)) {
                return ext;
            }
        }
        // Fallback based on content type
        return switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "jpg";
        };
    }
}
