package com.birtthdayForTelegram.bot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class Config {
    private static final String CONFIG_PATH_PROPERTY = "birthdaybot.config";
    private static final String DEFAULT_CONFIG_PATH = "config.properties";
    private static final Properties PROPERTIES = load();

    private Config() {
    }

    public static String get(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("В конфигурации не задан параметр: " + key);
        }
        return value;
    }

    private static Properties load() {
        String configuredPath = System.getProperty(CONFIG_PATH_PROPERTY, DEFAULT_CONFIG_PATH);
        Path path = Paths.get(configuredPath);
        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось загрузить конфигурацию из " + path.toAbsolutePath()
                            + ". Создайте файл по образцу config.properties.example",
                    e
            );
        }
    }
}
