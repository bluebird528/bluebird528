package com.bluebird.webeditor;

import com.bluebird.webeditor.config.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class WebEditorFileUploadApplicationTests {

    @Test
    void contextLoads() {
        // Test that the application context loads successfully
    }
}
