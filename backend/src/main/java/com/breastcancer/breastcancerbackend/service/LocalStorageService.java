package com.breastcancer.breastcancerbackend.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

/**
 * Stockage local des fichiers (dev).
 * Les fichiers sont stockés dans le répertoire configuré par app.storage.local.path.
 */
@Service
public class LocalStorageService implements StorageService {

    private static final Logger LOG = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path rootLocation;

    public LocalStorageService(@Value("${app.storage.local.path:./uploads/documents}") String path) {
        this.rootLocation = Paths.get(path).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            LOG.info("Storage directory initialized: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le répertoire de stockage: " + rootLocation, e);
        }
    }

    @Override
    public void store(String key, InputStream inputStream, String contentType) {
        try {
            Path target = rootLocation.resolve(key).normalize();
            // Sécurité: empêcher path traversal
            if (!target.startsWith(rootLocation)) {
                throw new BadRequestException("Chemin de stockage invalide.");
            }
            // Créer les sous-répertoires si nécessaire
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Fichier stocké: {}", key);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du stockage du fichier: " + key, e);
        }
    }

    @Override
    public Resource load(String key) {
        try {
            Path file = rootLocation.resolve(key).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new BadRequestException("Chemin de stockage invalide.");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("Fichier introuvable: " + key);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("Fichier introuvable: " + key);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Path file = rootLocation.resolve(key).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new BadRequestException("Chemin de stockage invalide.");
            }
            Files.deleteIfExists(file);
            LOG.info("Fichier supprimé: {}", key);
        } catch (IOException e) {
            LOG.warn("Impossible de supprimer le fichier: {}", key, e);
        }
    }
}
