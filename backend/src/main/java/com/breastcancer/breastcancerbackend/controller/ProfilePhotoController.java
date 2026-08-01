package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.service.ProfilePhotoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile-photos")
public class ProfilePhotoController {

    private final ProfilePhotoService profilePhotoService;

    public ProfilePhotoController(ProfilePhotoService profilePhotoService) {
        this.profilePhotoService = profilePhotoService;
    }

    /**
     * POST /api/profile-photos/upload/{userId}
     * Upload or replace a profile photo.
     */
    @PostMapping("/upload/{userId}")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @PathVariable UUID userId,
            @RequestParam("file") MultipartFile file
    ) {
        String url = profilePhotoService.uploadPhoto(userId, file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * PUT /api/profile-photos/upload/{userId}
     * Replace an existing profile photo (same logic as POST).
     */
    @PutMapping("/upload/{userId}")
    public ResponseEntity<Map<String, String>> replacePhoto(
            @PathVariable UUID userId,
            @RequestParam("file") MultipartFile file
    ) {
        String url = profilePhotoService.uploadPhoto(userId, file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * GET /api/profile-photos/{userId}
     * Serve the profile photo as binary image.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Resource> getPhoto(@PathVariable UUID userId) {
        Resource resource = profilePhotoService.loadPhoto(userId);
        String contentType = profilePhotoService.detectContentType(userId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    /**
     * DELETE /api/profile-photos/{userId}
     * Delete the profile photo.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID userId) {
        profilePhotoService.deletePhoto(userId);
        return ResponseEntity.noContent().build();
    }
}
