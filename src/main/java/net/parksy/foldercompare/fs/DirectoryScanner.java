package net.parksy.foldercompare.fs;

import net.parksy.foldercompare.model.FileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DirectoryScanner {
    private DirectoryScanner() {}

    /**
     * Scans a directory (non-recursive) and returns a map of name -> FileInfo sorted case-insensitively by name.
     */
    public static Map<String, FileInfo> scanDir(String pathText) {
        Map<String, FileInfo> map = new LinkedHashMap<>();
        if (pathText == null || pathText.isBlank()) {
            return map;
        }
        Path p = Path.of(pathText);
        if (!Files.isDirectory(p)) {
            return map;
        }
        try {
            Files.list(p)
                .sorted(Comparator.comparing(Path::getFileName, (a, b) -> a.toString().compareToIgnoreCase(b.toString())))
                .forEach(child -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(child, BasicFileAttributes.class);
                        boolean isDir = attrs.isDirectory();
                        long size = isDir ? -1L : attrs.size();
                        Instant mod = attrs.lastModifiedTime().toInstant();
                        map.put(child.getFileName().toString(), new FileInfo(child.getFileName().toString(), isDir, size, mod));
                    } catch (IOException ignored) {
                    }
                });
        } catch (IOException ignored) {
        }
        return map;
    }
}
