package com.breastcancer.breastcancerbackend.service;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Abstraction pour le stockage de fichiers.
 * Implémentations possibles : local (dev), S3 (prod).
 */
public interface StorageService {

    /**
     * Stocker un fichier.
     * @param key         clé unique de stockage
     * @param inputStream contenu du fichier
     * @param contentType type MIME
     */
    void store(String key, InputStream inputStream, String contentType);

    /**
     * Charger un fichier en tant que Resource.
     * @param key clé unique de stockage
     * @return Resource du fichier
     */
    Resource load(String key);

    /**
     * Supprimer un fichier du stockage.
     * @param key clé unique de stockage
     */
    void delete(String key);
}
