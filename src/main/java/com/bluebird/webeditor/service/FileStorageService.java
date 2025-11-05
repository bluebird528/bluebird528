package com.bluebird.webeditor.service;

import com.bluebird.webeditor.config.FileUploadProperties;
import com.bluebird.webeditor.entity.FileMetadata;
import com.bluebird.webeditor.exception.FileNotFoundException;
import com.bluebird.webeditor.exception.FileStorageException;
import com.bluebird.webeditor.exception.InvalidFileException;
import com.bluebird.webeditor.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FileStorageService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileUploadProperties fileUploadProperties;

    @Transactional
    public FileMetadata storeFile(MultipartFile file) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String storedFilename = generateStoredFilename(originalFilename);

        try {
            // Create directory structure: uploads/YYYY/MM/DD/
            Path uploadPath = createDateBasedDirectory();
            Path targetLocation = uploadPath.resolve(storedFilename);

            // Copy file to target location
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Calculate checksum
            String checksum = calculateChecksum(file.getBytes());

            // Save metadata
            FileMetadata metadata = FileMetadata.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .filePath(targetLocation.toString())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .fileType(determineFileType(fileExtension, file.getContentType()))
                .fileExtension(fileExtension)
                .checksum(checksum)
                .build();

            FileMetadata saved = fileMetadataRepository.save(metadata);
            log.info("File stored successfully: {} -> {}", originalFilename, storedFilename);
            return saved;

        } catch (IOException ex) {
            throw new FileStorageException("파일 저장 중 오류가 발생했습니다: " + originalFilename, ex);
        }
    }

    public FileMetadata getFileMetadata(Long id) {
        return fileMetadataRepository.findById(id)
            .orElseThrow(() -> new FileNotFoundException("파일을 찾을 수 없습니다. ID: " + id));
    }

    public Resource loadFileAsResource(Long id) {
        FileMetadata metadata = getFileMetadata(id);

        try {
            Path filePath = Paths.get(metadata.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFoundException("파일을 찾을 수 없거나 읽을 수 없습니다: " + metadata.getOriginalFilename());
            }
        } catch (MalformedURLException ex) {
            throw new FileNotFoundException("파일을 찾을 수 없습니다: " + metadata.getOriginalFilename(), ex);
        }
    }

    public Page<FileMetadata> getAllFiles(Pageable pageable) {
        return fileMetadataRepository.findAllActive(pageable);
    }

    public Page<FileMetadata> getFilesByType(FileMetadata.FileType fileType, Pageable pageable) {
        return fileMetadataRepository.findByFileType(fileType, pageable);
    }

    @Transactional
    public void deleteFile(Long id) {
        FileMetadata metadata = getFileMetadata(id);

        try {
            // Delete physical file
            Path filePath = Paths.get(metadata.getFilePath());
            Files.deleteIfExists(filePath);

            // Mark as deleted in database
            metadata.markAsDeleted();
            fileMetadataRepository.save(metadata);

            log.info("File deleted successfully: {}", metadata.getOriginalFilename());
        } catch (IOException ex) {
            throw new FileStorageException("파일 삭제 중 오류가 발생했습니다: " + metadata.getOriginalFilename(), ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("파일이 비어있습니다.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename.contains("..")) {
            throw new InvalidFileException("파일 이름에 올바르지 않은 경로가 포함되어 있습니다: " + originalFilename);
        }

        String fileExtension = getFileExtension(originalFilename);
        String[] allowedExtensions = fileUploadProperties.getAllowedExtensionsArray();

        boolean isAllowed = Arrays.stream(allowedExtensions)
            .anyMatch(ext -> ext.trim().equalsIgnoreCase(fileExtension));

        if (!isAllowed) {
            throw new InvalidFileException(
                "허용되지 않는 파일 형식입니다: " + fileExtension +
                ". 허용된 형식: " + String.join(", ", allowedExtensions)
            );
        }

        if (file.getSize() > fileUploadProperties.getMaxSize()) {
            throw new InvalidFileException(
                "파일 크기가 최대 허용 크기를 초과했습니다: " +
                file.getSize() + " > " + fileUploadProperties.getMaxSize()
            );
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex + 1).toLowerCase();
    }

    private String generateStoredFilename(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        String extension = getFileExtension(originalFilename);
        return uuid + (extension.isEmpty() ? "" : "." + extension);
    }

    private Path createDateBasedDirectory() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = now.format(DateTimeFormatter.ofPattern("MM"));
        String day = now.format(DateTimeFormatter.ofPattern("dd"));

        Path uploadPath = Paths.get(fileUploadProperties.getDir(), year, month, day)
            .toAbsolutePath()
            .normalize();

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        return uploadPath;
    }

    private FileMetadata.FileType determineFileType(String extension, String mimeType) {
        String ext = extension.toLowerCase();

        if (Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg").contains(ext)) {
            return FileMetadata.FileType.IMAGE;
        } else if (Arrays.asList("doc", "docx", "pdf", "txt", "rtf", "odt").contains(ext)) {
            return FileMetadata.FileType.DOCUMENT;
        } else if (Arrays.asList("xls", "xlsx", "csv", "ods").contains(ext)) {
            return FileMetadata.FileType.SPREADSHEET;
        } else if (Arrays.asList("ppt", "pptx", "odp").contains(ext)) {
            return FileMetadata.FileType.PRESENTATION;
        } else if (Arrays.asList("zip", "rar", "7z", "tar", "gz").contains(ext)) {
            return FileMetadata.FileType.ARCHIVE;
        } else if (Arrays.asList("txt", "md", "log").contains(ext)) {
            return FileMetadata.FileType.TEXT;
        }

        return FileMetadata.FileType.OTHER;
    }

    private String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            log.error("Error calculating checksum", ex);
            return null;
        }
    }
}
