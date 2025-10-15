package net.parksy.foldercompare;

import javafx.application.Application;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class App extends Application {

    private final TextField leftPathField = new TextField();
    private final TextField rightPathField = new TextField();

    private final TableView<PairedEntry> leftTable = new TableView<>();
    private final TableView<PairedEntry> rightTable = new TableView<>();

    private final ObservableList<PairedEntry> items = FXCollections.observableArrayList();

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void start(Stage stage) {
        stage.setTitle("Folder Compare");

        // Left panel
//        Label leftLabel = new Label("Folder 1:");
        leftPathField.setPromptText("Enter folder path and press Enter");
        leftPathField.setOnAction(e -> refresh());

        configureLeftTable();

        VBox leftPane = new VBox(6, //leftLabel,
                leftPathField, leftTable);
        leftPane.setPadding(new Insets(10));
        VBox.setVgrow(leftTable, Priority.ALWAYS);

        // Right panel
//        Label rightLabel = new Label("Folder 2:");
        rightPathField.setPromptText("Enter folder path and press Enter");
        rightPathField.setOnAction(e -> refresh());

        configureRightTable();

        VBox rightPane = new VBox(6, //rightLabel,
                rightPathField, rightTable);
        rightPane.setPadding(new Insets(10));
        VBox.setVgrow(rightTable, Priority.ALWAYS);

        // Both tables share the same items list to keep rows aligned
        leftTable.setItems(items);
        rightTable.setItems(items);

        HBox root = new HBox(10, leftPane, rightPane);
        root.setPadding(new Insets(10));
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);
        leftPane.setFillWidth(true);
        rightPane.setFillWidth(true);

        leftTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rightTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Scene scene = new Scene(root, 1200, 700);
        stage.setScene(scene);
        stage.show();
    }

    private void configureLeftTable() {
        TableColumn<PairedEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("leftName"));

        TableColumn<PairedEntry, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("leftSizeDisplay"));
        sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<PairedEntry, String> modCol = new TableColumn<>("Modified");
        modCol.setCellValueFactory(new PropertyValueFactory<>("leftModifiedDisplay"));

        leftTable.getColumns().setAll(nameCol, sizeCol, modCol);
    }

    private void configureRightTable() {
        TableColumn<PairedEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("rightName"));

        TableColumn<PairedEntry, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("rightSizeDisplay"));
        sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<PairedEntry, String> modCol = new TableColumn<>("Modified");
        modCol.setCellValueFactory(new PropertyValueFactory<>("rightModifiedDisplay"));

        rightTable.getColumns().setAll(nameCol, sizeCol, modCol);
    }

    private void refresh() {
        String leftPath = leftPathField.getText() == null ? "" : leftPathField.getText().trim();
        String rightPath = rightPathField.getText() == null ? "" : rightPathField.getText().trim();

        Map<String, FileInfo> leftMap = scanDir(leftPath);
        Map<String, FileInfo> rightMap = scanDir(rightPath);

        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(leftMap.keySet());
        names.addAll(rightMap.keySet());

        List<PairedEntry> list = new ArrayList<>();
        for (String n : names) {
            FileInfo l = leftMap.get(n);
            FileInfo r = rightMap.get(n);
            list.add(new PairedEntry(l, r));
        }

        items.setAll(list);
    }

    private Map<String, FileInfo> scanDir(String pathText) {
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
                    } catch (Exception ignored) {
                    }
                });
        } catch (Exception ignored) {
        }
        return map;
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Model classes
    public static class FileInfo {
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
            return DT_FMT.format(ldt);
        }
    }

    public static class PairedEntry {
        private final FileInfo left;
        private final FileInfo right;

        public PairedEntry(FileInfo left, FileInfo right) {
            this.left = left;
            this.right = right;
        }

        // Left getters for TableView
        public String getLeftName() { return left == null ? "" : decorateName(left); }
        public String getLeftSizeDisplay() { return left == null ? "" : left.getSizeDisplay(); }
        public String getLeftModifiedDisplay() { return left == null ? "" : left.getModifiedDisplay(); }

        // Right getters
        public String getRightName() { return right == null ? "" : decorateName(right); }
        public String getRightSizeDisplay() { return right == null ? "" : right.getSizeDisplay(); }
        public String getRightModifiedDisplay() { return right == null ? "" : right.getModifiedDisplay(); }

        private String decorateName(FileInfo fi) {
            return fi.isDirectory() ? fi.getName() + File.separator : fi.getName();
        }
    }
}
