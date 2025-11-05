package com.bluebird.webeditor.service;

import com.bluebird.webeditor.config.FileUploadProperties;
import com.bluebird.webeditor.entity.FileMetadata;
import com.bluebird.webeditor.exception.FileNotFoundException;
import com.bluebird.webeditor.exception.InvalidFileException;
import com.bluebird.webeditor.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileUploadProperties fileUploadProperties;

    @InjectMocks
    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(fileUploadProperties.getDir()).thenReturn(tempDir.toString());
        when(fileUploadProperties.getMaxSize()).thenReturn(52428800L); // 50MB
        when(fileUploadProperties.getAllowedExtensionsArray())
            .thenReturn(new String[]{"jpg", "jpeg", "png", "pdf", "txt"});
    }

    @Test
    void storeFile_shouldStoreFileSuccessfully() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test content".getBytes()
        );

        FileMetadata expectedMetadata = FileMetadata.builder()
            .id(1L)
            .originalFilename("test.jpg")
            .storedFilename("stored-test.jpg")
            .fileSize(file.getSize())
            .mimeType("image/jpeg")
            .fileType(FileMetadata.FileType.IMAGE)
            .build();

        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(expectedMetadata);

        // When
        FileMetadata result = fileStorageService.storeFile(file);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginalFilename()).isEqualTo("test.jpg");
        assertThat(result.getMimeType()).isEqualTo("image/jpeg");
        verify(fileMetadataRepository).save(any(FileMetadata.class));
    }

    @Test
    void storeFile_shouldThrowExceptionForEmptyFile() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(emptyFile))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("파일이 비어있습니다");
    }

    @Test
    void storeFile_shouldThrowExceptionForInvalidExtension() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.exe",
            "application/x-msdownload",
            "test content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(file))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("허용되지 않는 파일 형식입니다");
    }

    @Test
    void storeFile_shouldThrowExceptionForPathTraversal() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "../../../test.jpg",
            "image/jpeg",
            "test content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(file))
            .isInstanceOf(InvalidFileException.class)
            .hasMessageContaining("올바르지 않은 경로");
    }

    @Test
    void getFileMetadata_shouldReturnMetadata() {
        // Given
        Long fileId = 1L;
        FileMetadata expectedMetadata = FileMetadata.builder()
            .id(fileId)
            .originalFilename("test.jpg")
            .build();

        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(expectedMetadata));

        // When
        FileMetadata result = fileStorageService.getFileMetadata(fileId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(fileId);
        assertThat(result.getOriginalFilename()).isEqualTo("test.jpg");
    }

    @Test
    void getFileMetadata_shouldThrowExceptionWhenNotFound() {
        // Given
        Long fileId = 999L;
        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> fileStorageService.getFileMetadata(fileId))
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageContaining("파일을 찾을 수 없습니다");
    }

    @Test
    void getAllFiles_shouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<FileMetadata> files = List.of(
            FileMetadata.builder().id(1L).originalFilename("file1.jpg").build(),
            FileMetadata.builder().id(2L).originalFilename("file2.png").build()
        );
        Page<FileMetadata> expectedPage = new PageImpl<>(files, pageable, files.size());

        when(fileMetadataRepository.findAllActive(pageable)).thenReturn(expectedPage);

        // When
        Page<FileMetadata> result = fileStorageService.getAllFiles(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getFilesByType_shouldReturnFilteredResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        FileMetadata.FileType fileType = FileMetadata.FileType.IMAGE;
        List<FileMetadata> files = List.of(
            FileMetadata.builder().id(1L).originalFilename("file1.jpg").fileType(fileType).build()
        );
        Page<FileMetadata> expectedPage = new PageImpl<>(files, pageable, files.size());

        when(fileMetadataRepository.findByFileType(fileType, pageable)).thenReturn(expectedPage);

        // When
        Page<FileMetadata> result = fileStorageService.getFilesByType(fileType, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFileType()).isEqualTo(fileType);
    }
}
