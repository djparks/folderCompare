package net.parksy.foldercompare;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

        // Toolbar at top
        Button copyBtn = new Button("Copy", new Label("⧉"));
        copyBtn.setContentDisplay(ContentDisplay.LEFT);
        Button refreshBtn = new Button("Refresh", new Label("↻"));
        refreshBtn.setContentDisplay(ContentDisplay.LEFT);
        Button swapBtn = new Button("Swap", new Label("⇄"));
        swapBtn.setContentDisplay(ContentDisplay.LEFT);
        ToolBar toolBar = new ToolBar(copyBtn, refreshBtn, swapBtn);

        // Left panel
        leftPathField.setPromptText("Enter folder path and press Enter or drop a folder here");
        leftPathField.setOnAction(e -> refresh());
        // Drag & drop for folders
        addFolderDragDrop(leftPathField);

        configureLeftTable();

        VBox leftPane = new VBox(6,
                leftPathField, leftTable);
        leftPane.setPadding(new Insets(10));
        VBox.setVgrow(leftTable, Priority.ALWAYS);

        // Right panel
        rightPathField.setPromptText("Enter folder path and press Enter or drop a folder here");
        rightPathField.setOnAction(e -> refresh());
        // Drag & drop for folders
        addFolderDragDrop(rightPathField);

        configureRightTable();

        VBox rightPane = new VBox(6,
                rightPathField, rightTable);
        rightPane.setPadding(new Insets(10));
        VBox.setVgrow(rightTable, Priority.ALWAYS);

        // Both tables share the same items list to keep rows aligned
        leftTable.setItems(items);
        rightTable.setItems(items);

        HBox center = new HBox(10, leftPane, rightPane);
        center.setPadding(new Insets(10));
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);
        leftPane.setFillWidth(true);
        rightPane.setFillWidth(true);

        leftTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rightTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Wire toolbar actions
        refreshBtn.setOnAction(e -> refresh());
        swapBtn.setOnAction(e -> {
            String l = leftPathField.getText();
            leftPathField.setText(rightPathField.getText());
            rightPathField.setText(l);
            refresh();
        });
        copyBtn.setOnAction(e -> handleCopy());
        // Enable Copy only if some row is selected on either side
        copyBtn.disableProperty().bind(
                Bindings.isNull(leftTable.getSelectionModel().selectedItemProperty())
                        .and(Bindings.isNull(rightTable.getSelectionModel().selectedItemProperty()))
        );

        VBox root = new VBox(toolBar, center);

        Scene scene = new Scene(root, 1200, 700);
        stage.setScene(scene);
        stage.show();
    }

    private void configureLeftTable() {
        TableColumn<PairedEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("leftName"));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setGraphic(null);
                applyStylingToCell(this, true);
            }
        });

        TableColumn<PairedEntry, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("leftSizeDisplay"));
        sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        sizeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setGraphic(null);
                applyStylingToCell(this, true);
            }
        });

        TableColumn<PairedEntry, String> modCol = new TableColumn<>("Modified");
        modCol.setCellValueFactory(new PropertyValueFactory<>("leftModifiedDisplay"));
        modCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setGraphic(null);
                applyStylingToCell(this, true);
            }
        });

        leftTable.getColumns().setAll(nameCol, sizeCol, modCol);
    }

    private void configureRightTable() {
        TableColumn<PairedEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("rightName"));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setGraphic(null);
                applyStylingToCell(this, false);
            }
        });

        TableColumn<PairedEntry, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("rightSizeDisplay"));
        sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        sizeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setGraphic(null);
                applyStylingToCell(this, false);
            }
        });

        TableColumn<PairedEntry, String> modCol = new TableColumn<>("Modified");
        modCol.setCellValueFactory(new PropertyValueFactory<>("rightModifiedDisplay"));
        modCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setGraphic(null);
                applyStylingToCell(this, false);
            }
        });

        rightTable.getColumns().setAll(nameCol, sizeCol, modCol);
    }

    private void applyStylingToCell(TableCell<PairedEntry, String> cell, boolean leftSide) {
        TableRow<PairedEntry> row = cell.getTableRow();
        PairedEntry pe = row == null ? null : row.getItem();
        Color color = Color.BLACK;
        if (pe != null) {
            boolean orphan = leftSide ? pe.isOrphanLeft() : pe.isOrphanRight();
            if (orphan) {
                color = Color.PURPLE;
            } else if (pe.isDifferent()) {
                color = Color.RED;
            }
        }
        cell.setTextFill(color);
    }

    private void addFolderDragDrop(TextField field) {
        field.setOnDragOver((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            boolean accept = db.hasFiles() && db.getFiles().stream().anyMatch(f -> f.isDirectory());
            if (accept) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        field.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File dir = db.getFiles().stream().filter(File::isDirectory).findFirst().orElse(null);
                if (dir != null) {
                    field.setText(dir.getAbsolutePath());
                    refresh();
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void handleCopy() {
        PairedEntry sel = leftTable.getSelectionModel().getSelectedItem();
        boolean fromLeft = true;
        if (sel == null) {
            sel = rightTable.getSelectionModel().getSelectedItem();
            fromLeft = false;
        }
        if (sel == null) {
            System.out.println("[INFO] No selection to copy.");
            return;
        }
        try {
            boolean canCopy = false;
            boolean leftToRight = false;
            String name = null;
            if (sel.isOrphanLeft() && sel.left != null && !sel.left.isDirectory()) {
                canCopy = true;
                leftToRight = true;
                name = sel.left.getName();
            } else if (sel.isOrphanRight() && sel.right != null && !sel.right.isDirectory()) {
                canCopy = true;
                leftToRight = false;
                name = sel.right.getName();
            }

            if (!canCopy) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Select a single file that exists on one side only to copy.", ButtonType.OK);
                a.setHeaderText("Copy not available");
                a.showAndWait();
                return;
            }

            String srcFolder = leftToRight ? leftPathField.getText() : rightPathField.getText();
            String dstFolder = leftToRight ? rightPathField.getText() : leftPathField.getText();
            Path srcDir = Path.of(srcFolder == null ? "" : srcFolder);
            Path dstDir = Path.of(dstFolder == null ? "" : dstFolder);
            if (!(Files.isDirectory(srcDir) && Files.isDirectory(dstDir))) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Both left and right paths must be valid directories.", ButtonType.OK);
                a.setHeaderText("Invalid folders");
                a.showAndWait();
                return;
            }

            String direction = leftToRight ? "left → right" : "right → left";
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Copy");
            confirm.setHeaderText("Copy file?");
            confirm.setContentText("Copy '" + name + "' from " + direction + "? This will overwrite if the file exists.");
            confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            ButtonType result = confirm.showAndWait().orElse(ButtonType.NO);
            if (result != ButtonType.YES) {
                return;
            }

            Path src = srcDir.resolve(name);
            Path dst = dstDir.resolve(name);
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[INFO] Copied " + (leftToRight ? "left->right" : "right->left") + ": " + src + " -> " + dst);
        } catch (Exception ex) {
            System.out.println("[WARN] Copy failed: " + ex.getMessage());
        }
        refresh();
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

        public boolean isOrphanLeft() { return left != null && right == null; }
        public boolean isOrphanRight() { return right != null && left == null; }

        public boolean isDifferent() {
            if (left == null || right == null) return false; // only flag mismatch when both exist
            if (left.isDirectory() != right.isDirectory()) return true;
            boolean sizeDiff = !left.isDirectory() && left.getSize() != right.getSize();
            Instant lm = left.getModified();
            Instant rm = right.getModified();
            boolean modDiff = (lm == null && rm != null) || (lm != null && rm == null) || (lm != null && !lm.equals(rm));
            return sizeDiff || modDiff;
        }

        private String decorateName(FileInfo fi) {
            return fi.isDirectory() ? fi.getName() + File.separator : fi.getName();
        }
    }
}
