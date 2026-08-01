package com.breastcancer.breastcancerbackend.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Catégories de documents médicaux.
 * Les slugs correspondent EXACTEMENT au frontend Angular.
 */
public enum DocumentCategory {

    COMPTE_RENDU("compte-rendu"),
    ORDONNANCE("ordonnance"),
    BILAN("bilan"),
    IMAGERIE("imagerie"),
    AUTRE("autre");

    private final String slug;

    private static final Map<String, DocumentCategory> SLUG_MAP = new HashMap<>();

    static {
        for (DocumentCategory c : values()) {
            SLUG_MAP.put(c.slug, c);
        }
    }

    DocumentCategory(String slug) {
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }

    /**
     * Lookup par slug frontend. Lève IllegalArgumentException si slug inconnu.
     */
    public static DocumentCategory fromSlug(String slug) {
        DocumentCategory cat = SLUG_MAP.get(slug);
        if (cat == null) {
            throw new IllegalArgumentException("Catégorie inconnue: " + slug);
        }
        return cat;
    }
}
