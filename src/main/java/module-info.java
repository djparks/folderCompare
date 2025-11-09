module net.parksy.foldercompare {
    requires javafx.controls;
    requires java.prefs;
    
    // Open packages that JavaFX reflects into (e.g., PropertyValueFactory)
    opens net.parksy.foldercompare to javafx.graphics, javafx.base;
    opens net.parksy.foldercompare.model to javafx.base;
    opens net.parksy.foldercompare.fs to javafx.base;
    opens net.parksy.foldercompare.prefs to javafx.base;

    // Export the main UI package (optional, but conventional for modular apps)
    exports net.parksy.foldercompare;
}