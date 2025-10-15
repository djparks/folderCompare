package net.parksy.foldercompare.model;

import java.io.File;

/**
 * Represents an aligned row for left/right comparison.
 */
public class PairedEntry {
    private final FileInfo left;
    private final FileInfo right;

    public PairedEntry(FileInfo left, FileInfo right) {
        this.left = left;
        this.right = right;
    }

    public FileInfo getLeft() { return left; }
    public FileInfo getRight() { return right; }

    // Left getters for TableView
    public String getLeftName() { return left == null ? "" : decorateName(left); }
    public String getLeftSizeDisplay() { return left == null ? "" : left.getSizeDisplay(); }
    public String getLeftModifiedDisplay() { return left == null ? "" : left.getModifiedDisplay(); }

    // Right getters
    public String getRightName() { return right == null ? "" : decorateName(right); }
    public String getRightSizeDisplay() { return right == null ? "" : right.getSizeDisplay(); }
    public String getRightModifiedDisplay() { return right == null ? "" : right.getModifiedDisplay(); }

    public boolean isOrphanLeft() { return left != null && right == null; }
    public boolean isOrphanRight() { return right != null && left == null; }

    public boolean isDifferent() {
        if (left == null || right == null) return false; // only flag mismatch when both exist
        if (left.isDirectory() != right.isDirectory()) return true;
        boolean sizeDiff = !left.isDirectory() && left.getSize() != right.getSize();
        var lm = left.getModified();
        var rm = right.getModified();
        boolean modDiff = (lm == null && rm != null) || (lm != null && rm == null) || (lm != null && !lm.equals(rm));
        return sizeDiff || modDiff;
    }

    private String decorateName(FileInfo fi) {
        return fi.isDirectory() ? fi.getName() + File.separator : fi.getName();
    }
}
