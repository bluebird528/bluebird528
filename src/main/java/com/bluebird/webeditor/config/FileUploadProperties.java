package com.bluebird.webeditor.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "file.upload")
@Validated
@Getter
@Setter
public class FileUploadProperties {

    @NotBlank
    private String dir;

    @NotNull
    private Long maxSize;

    @NotBlank
    private String allowedExtensions;

    public String[] getAllowedExtensionsArray() {
        return allowedExtensions.split(",");
    }
}
