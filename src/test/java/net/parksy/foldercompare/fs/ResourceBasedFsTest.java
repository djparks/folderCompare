package net.parksy.foldercompare.fs;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import net.parksy.foldercompare.model.FileInfo;

import static org.junit.jupiter.api.Assertions.*;

class ResourceBasedFsTest {

    private Path resourceDir(String name) {
        URL url = getClass().getClassLoader().getResource(name);
        assertNotNull(url, "Resource directory not found: " + name);
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            fail("Bad URI for resource: " + name + ": " + e.getMessage());
            return null; // unreachable
        }
    }

    @Test
    void filesEqual_onSharedResourceFile_true() {
        Path f1 = resourceDir("folder1").resolve("both.txt");
        Path f2 = resourceDir("folder2").resolve("both.txt");
        assertTrue(CompareUtil.filesEqual(f1, f2));
    }

    @Test
    void directoriesEqual_differentResourceFolders_false() {
        Path d1 = resourceDir("folder1");
        Path d2 = resourceDir("folder2");
        assertFalse(CompareUtil.directoriesEqual(d1, d2));
    }

    @Test
    void directoriesEqual_sameFolder_true() {
        Path d1 = resourceDir("folder1");
        assertTrue(CompareUtil.directoriesEqual(d1, d1));
    }

    @Test
    void scanDir_fromResources_containsFilesAndSubdir() {
        Path d1 = resourceDir("folder1");
        Map<String, FileInfo> map = DirectoryScanner.scanDir(d1.toString());
        assertTrue(map.containsKey("both.txt"));
        assertTrue(map.containsKey("only1.txt"));
        assertTrue(map.containsKey("sub"));

        FileInfo file = map.get("both.txt");
        assertNotNull(file);
        assertFalse(file.isDirectory());
        assertTrue(file.getSize() > 0);

        FileInfo dir = map.get("sub");
        assertNotNull(dir);
        assertTrue(dir.isDirectory());
        assertEquals(-1L, dir.getSize());
    }
}
