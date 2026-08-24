package com.pixsom.racebets.admin.race;

import com.pixsom.racebets.admin.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class RaceImageStorage {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final String URL_PREFIX = "/api/race-images/";

    private final Path storageDirectory;

    public RaceImageStorage(@Value("${racebets.race-images.directory:./uploads/race-images}") String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile image) {
        if (image == null || image.isEmpty() || image.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("L'image doit peser au maximum 5 Mo.");
        }

        try {
            byte[] header;
            try (InputStream inputStream = image.getInputStream()) {
                header = inputStream.readNBytes(12);
            }
            ImageFormat format = ImageFormat.fromHeader(header);
            if (format == null) {
                throw new BadRequestException("L'image doit être au format PNG, JPEG ou WebP.");
            }

            Files.createDirectories(storageDirectory);
            String filename = UUID.randomUUID() + format.extension;
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, storageDirectory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
            return URL_PREFIX + filename;
        } catch (IOException exception) {
            throw new BadRequestException("L'image n'a pas pu être enregistrée.");
        }
    }

    public Resource load(String filename) {
        if (!filename.matches("[0-9a-fA-F-]{36}\\.(png|jpg|webp)")) {
            throw new BadRequestException("Image invalide.");
        }

        Path file = storageDirectory.resolve(filename).normalize();
        if (!file.startsWith(storageDirectory) || !Files.isRegularFile(file)) {
            return null;
        }
        return new FileSystemResource(file);
    }

    public MediaType contentType(String filename) {
        return filename.endsWith(".png") ? MediaType.IMAGE_PNG
                : filename.endsWith(".jpg") ? MediaType.IMAGE_JPEG
                : MediaType.parseMediaType("image/webp");
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) {
            return;
        }
        String filename = imageUrl.substring(URL_PREFIX.length());
        if (!filename.matches("[0-9a-fA-F-]{36}\\.(png|jpg|webp)")) {
            return;
        }
        try {
            Files.deleteIfExists(storageDirectory.resolve(filename).normalize());
        } catch (IOException ignored) {
            // A stale image is harmless and can be cleaned up independently.
        }
    }

    private enum ImageFormat {
        PNG(".png"), JPEG(".jpg"), WEBP(".webp");

        private final String extension;

        ImageFormat(String extension) {
            this.extension = extension;
        }

        private static ImageFormat fromHeader(byte[] header) {
            if (header.length >= 8 && header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E
                    && header[3] == 0x47 && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
                return PNG;
            }
            if (header.length >= 3 && header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                return JPEG;
            }
            if (header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return WEBP;
            }
            return null;
        }
    }
}
