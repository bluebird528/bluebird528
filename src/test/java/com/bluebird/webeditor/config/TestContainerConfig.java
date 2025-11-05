package com.bluebird.webeditor.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfig {

    @Bean
    @ServiceConnection
    public MariaDBContainer<?> mariaDBContainer() {
        return new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    }
}
