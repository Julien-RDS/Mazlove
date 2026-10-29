package com.mazlove.backend.service;

import com.mazlove.backend.exception.BadRequestException;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.exception.NotFoundException;
import com.mazlove.backend.exception.UnauthorizedException;
import com.mazlove.backend.models.Provider;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
public class UploadService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 25_000_000L;
    private final Path root;
    private final UserRepository users;
    private final ProviderRepository providers;

    public UploadService(@Value("${upload.dir:./uploads}") String uploadDir,
                         UserRepository users, ProviderRepository providers) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        this.users = users;
        this.providers = providers;
    }

    @Transactional
    public String saveAvatar(String email, MultipartFile file) {
        User user = users.findByEmail(email).orElseThrow(() -> new UnauthorizedException("Compte introuvable."));
        String url = store(file, "avatars");
        user.setAvatar(url);
        users.save(user);
        return url;
    }

    @Transactional
    public String saveProviderPhoto(String email, Long id, MultipartFile file) {
        Provider provider = providers.findById(id).orElseThrow(() -> new NotFoundException("Praticien introuvable."));
        if (!provider.getUser().getEmail().equals(email)) {
            throw new ForbiddenException("Cette fiche appartient à un autre praticien.");
        }
        String url = store(file, "providers/" + id);
        provider.getImages().add(url);
        providers.save(provider);
        return url;
    }

    private String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new BadRequestException("La photo doit peser entre 1 octet et 5 Mo.");
        }
        try (InputStream input = file.getInputStream(); ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) throw new BadRequestException("Image JPEG ou PNG invalide.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw new BadRequestException("Image JPEG ou PNG invalide.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png")) {
                    throw new BadRequestException("Seuls les formats JPEG et PNG sont acceptés.");
                }
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new BadRequestException("Dimensions de l'image trop grandes.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw new BadRequestException("Image invalide.");
                String extension = format.equals("png") ? "png" : "jpg";
                Path directory = root.resolve(folder).normalize();
                Files.createDirectories(directory);
                String name = UUID.randomUUID() + "." + extension;
                Path destination = directory.resolve(name);
                try {
                    if (!ImageIO.write(image, extension, destination.toFile())) throw new IOException("Encodeur indisponible");
                } catch (IOException | RuntimeException e) {
                    Files.deleteIfExists(destination);
                    throw e;
                }
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                try { Files.deleteIfExists(destination); } catch (IOException ignored) { }
                            }
                        }
                    });
                }
                return "/uploads/" + folder + "/" + name;
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new BadRequestException("Impossible de lire ou enregistrer cette image.");
        }
    }
}
