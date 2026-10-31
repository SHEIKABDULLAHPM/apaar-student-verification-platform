package com.apaar.platform.ingestion.service;

import com.apaar.platform.common.exception.UnprocessableEntityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path storageLocation;

    public FileStorageService(@Value("${app.upload.storage-dir:data/uploads}") String storageDir) {
        this.storageLocation = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize upload storage directory at " + this.storageLocation, e);
        }
    }

    public record StoredFile(
            String storagePath,
            String originalFilename,
            String contentType,
            String checksum,
            long size
    ) {}

    public StoredFile storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new UnprocessableEntityException("EMPTY_FILE", "Uploaded file is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "upload.csv";
        }

        // Clean filename and prevent path traversal
        String cleanName = Paths.get(originalFilename).getFileName().toString();
        if (cleanName.contains("..")) {
            throw new UnprocessableEntityException("INVALID_FILENAME", "Filename contains invalid path sequence");
        }

        String extension = "";
        int dotIdx = cleanName.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = cleanName.substring(dotIdx).toLowerCase();
        }

        String storageFilename = UUID.randomUUID() + extension;
        Path targetPath = this.storageLocation.resolve(storageFilename).normalize();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size;
            try (InputStream is = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            String checksum = HexFormat.of().formatHex(digest.digest());

            try (InputStream is = file.getInputStream()) {
                size = Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                if (cleanName.endsWith(".csv")) contentType = "text/csv";
                else if (cleanName.endsWith(".xlsx")) contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                else if (cleanName.endsWith(".xls")) contentType = "application/vnd.ms-excel";
                else contentType = "application/octet-stream";
            }

            return new StoredFile(targetPath.toString(), cleanName, contentType, checksum, size);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to store uploaded file securely", e);
        }
    }

    public InputStream openStream(String storagePath) throws IOException {
        Path path = Paths.get(storagePath).normalize();
        if (!path.startsWith(this.storageLocation)) {
            throw new SecurityException("Attempted path traversal access to storage file");
        }
        return Files.newInputStream(path);
    }

    public Path getFilePath(String storagePath) {
        Path path = Paths.get(storagePath).normalize();
        if (!path.startsWith(this.storageLocation)) {
            throw new SecurityException("Attempted path traversal access to storage file");
        }
        return path;
    }

    public void deleteFile(String storagePath) {
        if (storagePath == null) return;
        try {
            Path path = Paths.get(storagePath).normalize();
            if (path.startsWith(this.storageLocation)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {}
    }
}
