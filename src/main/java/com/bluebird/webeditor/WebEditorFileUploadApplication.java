package com.bluebird.webeditor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WebEditorFileUploadApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebEditorFileUploadApplication.class, args);
    }
}
