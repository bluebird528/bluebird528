package com.bluebird.webeditor.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class FileStorageConfig {

    private final FileUploadProperties fileUploadProperties;

    @Bean
    public CommandLineRunner initUploadDirectory() {
        return args -> {
            Path uploadPath = Paths.get(fileUploadProperties.getDir()).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        };
    }
}
