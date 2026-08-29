package io.clamped.server;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Builds a DataSource from (in priority order):
 *   1. Environment variables: CLAMPED_JDBC_URL, CLAMPED_USERNAME, CLAMPED_PASSWORD
 *   2. Spring application.properties: spring.datasource.url / username / password
 *   3. ~/.clamped/config.properties (same file used by the CLI)
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        String url      = System.getenv("CLAMPED_JDBC_URL");
        String username = System.getenv("CLAMPED_USERNAME");
        String password = System.getenv("CLAMPED_PASSWORD");

        // If CLAMPED_JDBC_URL is in postgresql:// format, convert it
        if (url != null && url.startsWith("postgresql://")) {
            url = "jdbc:postgresql://" + url.substring("postgresql://".length());
        }

        // Fall back to DATABASE_URL (Railway standard) if CLAMPED_JDBC_URL not set
        if (url == null) {
            String databaseUrl = System.getenv("DATABASE_URL");
            if (databaseUrl != null) {
                // Convert postgresql://user:pass@host:port/db to jdbc:postgresql://host:port/db
                url = databaseUrl.replaceFirst("^postgresql://", "jdbc:postgresql://");
                // Extract username and password from URL for any field not already set separately
                if (username == null || password == null) {
                    try {
                        java.net.URI uri = new java.net.URI(databaseUrl.replaceFirst("^postgresql://", "http://"));
                        String userInfo = uri.getUserInfo();
                        if (userInfo != null && userInfo.contains(":")) {
                            if (username == null) username = userInfo.split(":")[0];
                            if (password == null) password = userInfo.split(":")[1];
                            // Remove credentials from URL
                            url = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        // Fall back to ~/.clamped/config.properties for any field still unset
        if (url == null || username == null || password == null) {
            Properties props = loadConfigFile();
            if (url == null)      url      = props.getProperty("jdbcUrl",  "jdbc:postgresql://localhost:5432/clamped_db");
            if (username == null) username = props.getProperty("username", "postgres");
            if (password == null) password = props.getProperty("password", "");
        }

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    private Properties loadConfigFile() {
        Path configFile = Paths.get(System.getProperty("user.home"), ".clamped", "config.properties");
        Properties props = new Properties();
        if (configFile.toFile().exists()) {
            try (FileInputStream fis = new FileInputStream(configFile.toFile())) {
                props.load(fis);
            } catch (IOException ignored) {}
        }
        return props;
    }
}
