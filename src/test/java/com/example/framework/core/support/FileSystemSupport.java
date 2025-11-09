package com.example.framework.core.support;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility helpers for filesystem operations shared across the framework.
 */
public final class FileSystemSupport {

    private static final Pattern ILLEGAL_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");

    private FileSystemSupport() {
    }

    public static Path ensureDirectory(Path directory) {
        if (directory == null) {
            return null;
        }
        try {
            Files.createDirectories(directory);
            return directory;
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("Unable to create directory: " + directory, e);
        }
    }

    public static Path buildArtifactPath(Path directory, String name, String extension) {
        ensureDirectory(directory);
        String safeName = sanitizeFileName(name);
        String ext = extension.startsWith(".") ? extension : "." + extension;
        return directory.resolve(safeName + ext).toAbsolutePath().normalize();
    }

    public static String sanitizeFileName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "artifact";
        }
        String normalized = Normalizer.normalize(candidate, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String cleaned = ILLEGAL_FILENAME_CHARS.matcher(normalized).replaceAll("_");
        String collapsed = cleaned.replaceAll("_+", "_");
        return collapsed.toLowerCase(Locale.ROOT);
    }
}

