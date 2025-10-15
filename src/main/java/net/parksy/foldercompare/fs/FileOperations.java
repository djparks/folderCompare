package net.parksy.foldercompare.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FileOperations {
    private FileOperations() {}

    public static void copyRecursive(Path src, Path dst) throws IOException {
        if (Files.isDirectory(src)) {
            Files.createDirectories(dst);
            try (var stream = Files.list(src)) {
                for (Path child : (Iterable<Path>) stream::iterator) {
                    copyRecursive(child, dst.resolve(child.getFileName().toString()));
                }
            }
        } else {
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void moveRecursive(Path src, Path dst) throws IOException {
        copyRecursive(src, dst);
        deleteRecursive(src);
    }

    public static void deleteRecursive(Path root) throws IOException {
        if (!Files.exists(root)) return;
        if (Files.isDirectory(root)) {
            try (var stream = Files.list(root)) {
                for (Path child : (Iterable<Path>) stream::iterator) {
                    deleteRecursive(child);
                }
            }
        }
        Files.deleteIfExists(root);
    }
}
