package net.parksy.foldercompare.fs;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CompareUtilTest {

    @Test
    void filesEqual_sameContent_true() throws Exception {
        Path dir = Files.createTempDirectory("cmp-");
        try {
            Path a = dir.resolve("a.txt");
            Path b = dir.resolve("b.txt");
            Files.writeString(a, "hello world\n");
            Files.writeString(b, "hello world\n");
            assertTrue(CompareUtil.filesEqual(a, b));
        } finally {
            // best-effort cleanup
            FileOperations.deleteRecursive(dir);
        }
    }

    @Test
    void filesEqual_diffContent_false() throws Exception {
        Path dir = Files.createTempDirectory("cmp-");
        try {
            Path a = dir.resolve("a.txt");
            Path b = dir.resolve("b.txt");
            Files.writeString(a, "hello world\n");
            Files.writeString(b, "hello there\n");
            assertFalse(CompareUtil.filesEqual(a, b));
        } finally {
            FileOperations.deleteRecursive(dir);
        }
    }

    @Test
    void directoriesEqual_sameFiles_true() throws Exception {
        Path left = Files.createTempDirectory("left-");
        Path right = Files.createTempDirectory("right-");
        try {
            Files.writeString(left.resolve("one.txt"), "1");
            Files.writeString(left.resolve("two.txt"), "22");

            Files.writeString(right.resolve("one.txt"), "1");
            Files.writeString(right.resolve("two.txt"), "22");

            assertTrue(CompareUtil.directoriesEqual(left, right));
        } finally {
            FileOperations.deleteRecursive(left);
            FileOperations.deleteRecursive(right);
        }
    }

    @Test
    void directoriesEqual_differentCounts_false() throws Exception {
        Path left = Files.createTempDirectory("left-");
        Path right = Files.createTempDirectory("right-");
        try {
            Files.writeString(left.resolve("one.txt"), "1");
            Files.writeString(left.resolve("two.txt"), "22");

            Files.writeString(right.resolve("one.txt"), "1");
            // missing two.txt

            assertFalse(CompareUtil.directoriesEqual(left, right));
        } finally {
            FileOperations.deleteRecursive(left);
            FileOperations.deleteRecursive(right);
        }
    }

    @Test
    void directoriesEqual_sameCountDifferentNames_false() throws Exception {
        Path left = Files.createTempDirectory("left-");
        Path right = Files.createTempDirectory("right-");
        try {
            Files.writeString(left.resolve("one.txt"), "1");
            Files.writeString(left.resolve("two.txt"), "22");

            Files.writeString(right.resolve("uno.txt"), "1");
            Files.writeString(right.resolve("dos.txt"), "22");

            assertFalse(CompareUtil.directoriesEqual(left, right));
        } finally {
            FileOperations.deleteRecursive(left);
            FileOperations.deleteRecursive(right);
        }
    }

    @Test
    void directoriesEqual_sameNamesDifferentContent_false() throws Exception {
        Path left = Files.createTempDirectory("left-");
        Path right = Files.createTempDirectory("right-");
        try {
            Files.writeString(left.resolve("file.txt"), "A");
            Files.writeString(right.resolve("file.txt"), "B");
            assertFalse(CompareUtil.directoriesEqual(left, right));
        } finally {
            FileOperations.deleteRecursive(left);
            FileOperations.deleteRecursive(right);
        }
    }
}
