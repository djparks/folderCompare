package net.parksy.foldercompare.model;

import net.parksy.foldercompare.Constants;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Immutable file metadata used by the UI.
 */
public class FileInfo {
    private final SimpleStringProperty name = new SimpleStringProperty("");
    private final boolean directory;
    private final SimpleLongProperty size = new SimpleLongProperty(-1);
    private final SimpleObjectProperty<Instant> modified = new SimpleObjectProperty<>(null);

    public FileInfo(String name, boolean directory, long size, Instant modified) {
        this.name.set(name);
        this.directory = directory;
        this.size.set(size);
        this.modified.set(modified);
    }

    public String getName() { return name.get(); }
    public boolean isDirectory() { return directory; }
    public long getSize() { return size.get(); }
    public Instant getModified() { return modified.get(); }

    public String getSizeDisplay() {
        if (directory) return ""; // no size for directories per spec
        long s = getSize();
        return Long.toString(s);
    }

    public String getModifiedDisplay() {
        Instant m = getModified();
        if (m == null) return "";
        LocalDateTime ldt = LocalDateTime.ofInstant(m, ZoneId.systemDefault());
        return Constants.DATE_TIME_FORMATTER.format(ldt);
    }
}
