package com.bluebird.webeditor.controller;

import com.bluebird.webeditor.dto.FileListResponse;
import com.bluebird.webeditor.dto.FileUploadResponse;
import com.bluebird.webeditor.entity.FileMetadata;
import com.bluebird.webeditor.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
        @RequestParam("file") MultipartFile file
    ) {
        log.info("File upload request received: {}", file.getOriginalFilename());

        FileMetadata metadata = fileStorageService.storeFile(file);
        String downloadUrl = generateDownloadUrl(metadata.getId());

        FileUploadResponse response = FileUploadResponse.from(metadata, downloadUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileUploadResponse>> uploadMultipleFiles(
        @RequestParam("files") MultipartFile[] files
    ) {
        log.info("Multiple file upload request received: {} files", files.length);

        List<FileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            FileMetadata metadata = fileStorageService.storeFile(file);
            String downloadUrl = generateDownloadUrl(metadata.getId());
            responses.add(FileUploadResponse.from(metadata, downloadUrl));
        }

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileUploadResponse> getFileInfo(@PathVariable Long id) {
        FileMetadata metadata = fileStorageService.getFileMetadata(id);
        String downloadUrl = generateDownloadUrl(id);

        FileUploadResponse response = FileUploadResponse.from(metadata, downloadUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
        @PathVariable Long id,
        HttpServletRequest request
    ) throws IOException {
        FileMetadata metadata = fileStorageService.getFileMetadata(id);
        Resource resource = fileStorageService.loadFileAsResource(id);

        String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
            .body(resource);
    }

    @GetMapping
    public ResponseEntity<FileListResponse> getAllFiles(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FileMetadata> filePage = fileStorageService.getAllFiles(pageable);
        List<FileUploadResponse> files = filePage.getContent().stream()
            .map(metadata -> FileUploadResponse.from(metadata, generateDownloadUrl(metadata.getId())))
            .toList();

        FileListResponse response = FileListResponse.builder()
            .files(files)
            .totalCount(filePage.getTotalElements())
            .page(page)
            .size(size)
            .totalPages(filePage.getTotalPages())
            .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{fileType}")
    public ResponseEntity<FileListResponse> getFilesByType(
        @PathVariable FileMetadata.FileType fileType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<FileMetadata> filePage = fileStorageService.getFilesByType(fileType, pageable);
        List<FileUploadResponse> files = filePage.getContent().stream()
            .map(metadata -> FileUploadResponse.from(metadata, generateDownloadUrl(metadata.getId())))
            .toList();

        FileListResponse response = FileListResponse.builder()
            .files(files)
            .totalCount(filePage.getTotalElements())
            .page(page)
            .size(size)
            .totalPages(filePage.getTotalPages())
            .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        log.info("File delete request received: ID {}", id);
        fileStorageService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    private String generateDownloadUrl(Long fileId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/files/")
            .path(fileId.toString())
            .path("/download")
            .toUriString();
    }
}
