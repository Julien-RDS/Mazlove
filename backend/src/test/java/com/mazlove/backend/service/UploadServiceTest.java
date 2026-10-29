package com.mazlove.backend.service;

import com.mazlove.backend.exception.BadRequestException;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.models.Provider;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadServiceTest {
    @TempDir Path directory;
    UserRepository users = mock(UserRepository.class);
    ProviderRepository providers = mock(ProviderRepository.class);
    UploadService service;

    @BeforeEach void setup() { service = new UploadService(directory.toString(), users, providers); }

    private MockMultipartFile image() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("file", "../../evil.png", "image/png", output.toByteArray());
    }

    @Test void avatarIsStoredUnderGeneratedNameAndSaved() throws Exception {
        User user = new User();
        when(users.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        String url = service.saveAvatar("a@example.com", image());
        assertThat(url).startsWith("/uploads/avatars/").endsWith(".png").doesNotContain("evil");
        assertThat(Files.exists(directory.resolve(url.substring("/uploads/".length())))).isTrue();
        assertThat(user.getAvatar()).isEqualTo(url);
        verify(users).save(user);
    }

    @Test void nonOwnerCannotUploadProviderPhoto() throws Exception {
        User owner = new User(); owner.setEmail("owner@example.com");
        Provider provider = new Provider(); provider.setUser(owner);
        when(providers.findById(1L)).thenReturn(Optional.of(provider));
        assertThatThrownBy(() -> service.saveProviderPhoto("other@example.com", 1L, image()))
                .isInstanceOf(ForbiddenException.class);
        verify(providers, never()).save(any());
    }

    @Test void rejectsFakeImage() {
        when(users.findByEmail("a@example.com")).thenReturn(Optional.of(new User()));
        MockMultipartFile fake = new MockMultipartFile("file", "file.png", "image/png", "not an image".getBytes());
        assertThatThrownBy(() -> service.saveAvatar("a@example.com", fake)).isInstanceOf(BadRequestException.class);
        verify(users, never()).save(any());
    }

    @Test void ownerCanUploadProviderPhoto() throws Exception {
        User owner = new User(); owner.setEmail("owner@example.com");
        Provider provider = new Provider(); provider.setUser(owner);
        when(providers.findById(1L)).thenReturn(Optional.of(provider));
        String url = service.saveProviderPhoto("owner@example.com", 1L, image());
        assertThat(provider.getImages()).contains(url);
        assertThat(Files.exists(directory.resolve(url.substring("/uploads/".length())))).isTrue();
    }
}
