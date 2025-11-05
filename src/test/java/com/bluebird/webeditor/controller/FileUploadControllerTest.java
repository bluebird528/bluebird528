package com.bluebird.webeditor.controller;

import com.bluebird.webeditor.config.TestContainerConfig;
import com.bluebird.webeditor.entity.FileMetadata;
import com.bluebird.webeditor.repository.FileMetadataRepository;
import com.bluebird.webeditor.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @AfterEach
    void cleanUp() {
        fileMetadataRepository.deleteAll();
    }

    @Test
    void uploadFile_shouldReturnSuccessResponse() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/files/upload").file(file))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.originalFilename").value("test.jpg"))
            .andExpect(jsonPath("$.mimeType").value("image/jpeg"))
            .andExpect(jsonPath("$.fileType").value("IMAGE"))
            .andExpect(jsonPath("$.downloadUrl").isNotEmpty());
    }

    @Test
    void uploadMultipleFiles_shouldReturnMultipleResponses() throws Exception {
        // Given
        MockMultipartFile file1 = new MockMultipartFile(
            "files",
            "test1.jpg",
            "image/jpeg",
            "test image 1".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
            "files",
            "test2.png",
            "image/png",
            "test image 2".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/files/upload/multiple")
                .file(file1)
                .file(file2))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].originalFilename").value("test1.jpg"))
            .andExpect(jsonPath("$[1].originalFilename").value("test2.png"));
    }

    @Test
    void uploadFile_shouldRejectInvalidExtension() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.exe",
            "application/x-msdownload",
            "malicious content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/files/upload").file(file))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid File"))
            .andExpect(jsonPath("$.message").value(containsString("허용되지 않는 파일 형식입니다")));
    }

    @Test
    void uploadFile_shouldRejectEmptyFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );

        // When & Then
        mockMvc.perform(multipart("/api/files/upload").file(file))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid File"))
            .andExpect(jsonPath("$.message").value(containsString("파일이 비어있습니다")));
    }

    @Test
    void getFileInfo_shouldReturnFileDetails() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "test pdf content".getBytes()
        );

        FileMetadata metadata = fileStorageService.storeFile(file);

        // When & Then
        mockMvc.perform(get("/api/files/{id}", metadata.getId()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(metadata.getId()))
            .andExpect(jsonPath("$.originalFilename").value("test.pdf"))
            .andExpect(jsonPath("$.mimeType").value("application/pdf"))
            .andExpect(jsonPath("$.fileType").value("DOCUMENT"));
    }

    @Test
    void getFileInfo_shouldReturn404ForNonExistentFile() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/files/{id}", 999L))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("File Not Found"));
    }

    @Test
    void downloadFile_shouldReturnFileContent() throws Exception {
        // Given
        String fileContent = "test file content for download";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-download.txt",
            "text/plain",
            fileContent.getBytes()
        );

        FileMetadata metadata = fileStorageService.storeFile(file);

        // When & Then
        mockMvc.perform(get("/api/files/{id}/download", metadata.getId()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/plain"))
            .andExpect(content().string(fileContent));
    }

    @Test
    void getAllFiles_shouldReturnPagedResults() throws Exception {
        // Given
        for (int i = 1; i <= 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "test" + i + ".jpg",
                "image/jpeg",
                ("test content " + i).getBytes()
            );
            fileStorageService.storeFile(file);
        }

        // When & Then
        mockMvc.perform(get("/api/files")
                .param("page", "0")
                .param("size", "10"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.files", hasSize(3)))
            .andExpect(jsonPath("$.totalCount").value(3))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void getFilesByType_shouldReturnFilteredResults() throws Exception {
        // Given
        MockMultipartFile imageFile = new MockMultipartFile(
            "file",
            "image.jpg",
            "image/jpeg",
            "image content".getBytes()
        );

        MockMultipartFile pdfFile = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            "pdf content".getBytes()
        );

        fileStorageService.storeFile(imageFile);
        fileStorageService.storeFile(pdfFile);

        // When & Then - Get only IMAGE files
        mockMvc.perform(get("/api/files/type/IMAGE")
                .param("page", "0")
                .param("size", "10"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.files", hasSize(1)))
            .andExpect(jsonPath("$.files[0].fileType").value("IMAGE"))
            .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void deleteFile_shouldRemoveFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "to-delete.jpg",
            "image/jpeg",
            "content to delete".getBytes()
        );

        FileMetadata metadata = fileStorageService.storeFile(file);
        Long fileId = metadata.getId();

        // When
        mockMvc.perform(delete("/api/files/{id}", fileId))
            .andDo(print())
            .andExpect(status().isNoContent());

        // Then - Verify file is marked as deleted
        FileMetadata deletedMetadata = fileMetadataRepository.findById(fileId).orElseThrow();
        assertThat(deletedMetadata.getDeleted()).isTrue();
    }
}
