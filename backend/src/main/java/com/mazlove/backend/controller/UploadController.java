package com.mazlove.backend.controller;

import com.mazlove.backend.service.UploadService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private final UploadService uploadService;

    public UploadController(UploadService uploadService) { this.uploadService = uploadService; }

    @PostMapping("/avatar")
    public Map<String, String> avatar(@AuthenticationPrincipal UserDetails user,
                                      @RequestParam("file") MultipartFile file) {
        return Map.of("url", uploadService.saveAvatar(user.getUsername(), file));
    }

    @PostMapping("/provider/{id}/photo")
    public Map<String, String> photo(@AuthenticationPrincipal UserDetails user, @PathVariable Long id,
                                     @RequestParam("file") MultipartFile file) {
        return Map.of("url", uploadService.saveProviderPhoto(user.getUsername(), id, file));
    }
}
