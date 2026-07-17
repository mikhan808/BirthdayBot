package com.birtthdayForTelegram.bot;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

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
        Path path = findConfigPath();
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

    private static Path findConfigPath() {
        String configuredPath = System.getProperty(CONFIG_PATH_PROPERTY);
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            return Paths.get(configuredPath).toAbsolutePath().normalize();
        }

        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(Paths.get(DEFAULT_CONFIG_PATH).toAbsolutePath().normalize());
        addCodeLocationCandidates(candidates);

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException(
                "Не удалось найти " + DEFAULT_CONFIG_PATH + ". Проверены пути: "
                        + joinPaths(candidates)
                        + ". Создайте файл по образцу config.properties.example или задайте -D"
                        + CONFIG_PATH_PROPERTY + "=полный_путь_к_файлу"
        );
    }

    private static void addCodeLocationCandidates(Set<Path> candidates) {
        try {
            Path codeLocation = Paths.get(
                    Config.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).toAbsolutePath().normalize();
            boolean directory = Files.isDirectory(codeLocation);
            Path directoryPath = directory ? codeLocation : codeLocation.getParent();

            if (directoryPath == null) {
                return;
            }

            candidates.add(directoryPath.resolve(DEFAULT_CONFIG_PATH).normalize());
            Path parent = directoryPath.getParent();
            if (parent != null) {
                candidates.add(parent.resolve(DEFAULT_CONFIG_PATH).normalize());
                if (directory) {
                    Path grandParent = parent.getParent();
                    if (grandParent != null) {
                        candidates.add(grandParent.resolve(DEFAULT_CONFIG_PATH).normalize());
                    }
                }
            }
        } catch (URISyntaxException | RuntimeException ignored) {
            // Явный путь и текущая рабочая папка остаются доступными.
        }
    }

    private static String joinPaths(Set<Path> paths) {
        StringBuilder result = new StringBuilder();
        for (Path path : paths) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(path);
        }
        return result.toString();
    }
}
