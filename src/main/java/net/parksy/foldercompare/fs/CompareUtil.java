package net.parksy.foldercompare.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Utilities for efficient file/directory comparisons.
 */
public final class CompareUtil {
    private CompareUtil() {}

    /**
     * Efficiently compare file contents using size pre-check and Files.mismatch.
     */
    public static boolean filesEqual(Path a, Path b) {
        try {
            if (!(Files.isRegularFile(a) && Files.isRegularFile(b))) return false;
            long sa = Files.size(a);
            long sb = Files.size(b);
            if (sa != sb) return false;
            return Files.mismatch(a, b) == -1L;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Non-recursive directory comparison of regular files only.
     * Directories are considered different if:
     * - Either path is not a directory
     * - The number of regular files differs
     * - The set of regular file names differs (case-sensitive)
     * - Any corresponding file by name has different content per filesEqual
     */
    public static boolean directoriesEqual(Path a, Path b) {
        try {
            if (!(Files.isDirectory(a) && Files.isDirectory(b))) return false;

            // Collect regular files only (non-recursive)
            Set<String> namesA;
            Set<String> namesB;
            try (var s = Files.list(a)) {
                namesA = s.filter(Files::isRegularFile)
                          .map(p -> p.getFileName().toString())
                          .collect(Collectors.toCollection(TreeSet::new));
            }
            try (var s = Files.list(b)) {
                namesB = s.filter(Files::isRegularFile)
                          .map(p -> p.getFileName().toString())
                          .collect(Collectors.toCollection(TreeSet::new));
            }

            if (namesA.size() != namesB.size()) return false;
            if (!namesA.equals(namesB)) return false;

            // Compare each file by content
            for (String name : namesA) {
                Path pa = a.resolve(name);
                Path pb = b.resolve(name);
                if (!filesEqual(pa, pb)) return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
