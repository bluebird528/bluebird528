package com.bluebird.webeditor.dto;

import com.bluebird.webeditor.entity.FileMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {

    private Long id;
    private String originalFilename;
    private String storedFilename;
    private Long fileSize;
    private String mimeType;
    private FileMetadata.FileType fileType;
    private String fileExtension;
    private String downloadUrl;
    private LocalDateTime createdAt;

    public static FileUploadResponse from(FileMetadata metadata, String downloadUrl) {
        return FileUploadResponse.builder()
            .id(metadata.getId())
            .originalFilename(metadata.getOriginalFilename())
            .storedFilename(metadata.getStoredFilename())
            .fileSize(metadata.getFileSize())
            .mimeType(metadata.getMimeType())
            .fileType(metadata.getFileType())
            .fileExtension(metadata.getFileExtension())
            .downloadUrl(downloadUrl)
            .createdAt(metadata.getCreatedAt())
            .build();
    }
}
