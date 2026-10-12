package com.swipehire.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Where uploaded photos, uploaded resumes and generated PDFs live on disk (outside the WAR). */
@Component
public class Storage {

    private final Path root;

    public Storage(@Value("${swipehire.storage-dir}") String rootDir) {
        this.root = Path.of(rootDir).toAbsolutePath().normalize();
    }

    public Path photos() {
        return dir("photos");
    }

    public Path uploads() {
        return dir("resume-uploads");
    }

    public Path generatedPdfs() {
        return dir("tailored-resumes");
    }

    public Path root() {
        return root;
    }

    private Path dir(String name) {
        Path p = root.resolve(name);
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return p;
    }
}
