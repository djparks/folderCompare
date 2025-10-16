package net.parksy.foldercompare;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import net.parksy.foldercompare.model.FileInfo;
import net.parksy.foldercompare.model.PairedEntry;
import net.parksy.foldercompare.fs.DirectoryScanner;
import net.parksy.foldercompare.fs.FileOperations;
import net.parksy.foldercompare.prefs.HistoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class App extends Application {

    /**
     * Loads application icons from classpath resources.
     * Prefers PNG files under /icon directory; falls back to root resources if needed.
     * This works on macOS and Windows; JavaFX uses these sizes for taskbar/dock/title bar.
     */
    private List<Image> loadAppIcons() {
        String[] preferred = new String[] {
                "/icon/icons8-compare-16.png",
                "/icon/icons8-compare-32.png",
                "/icon/icons8-compare-64.png"
        };
        String[] fallback = new String[] {
                "/icons8-compare-16.png",
                "/icons8-compare-32.png",
                "/icons8-compare-64.png"
        };
        List<Image> images = new ArrayList<>();
        ClassLoader cl = getClass().getClassLoader();
        // Try preferred locations first
        for (String path : preferred) {
            try {
                var is = getClass().getResourceAsStream(path);
                if (is != null) {
                    images.add(new Image(is));
                }
            } catch (Exception ignored) {
            }
        }
        // If none loaded yet, try fallback root locations
        if (images.isEmpty()) {
            for (String path : fallback) {
                try {
                    var is = getClass().getResourceAsStream(path);
                    if (is != null) {
                        images.add(new Image(is));
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return images;
    }

    public static final int SIZE_COL_PREF_WIDTH = 40;
    public static final int SIZE_COL_MIN_WIDTH = 20;
    private final TextField leftPathField = new TextField();
    private final TextField rightPathField = new TextField();

    private HistoryService historyService;

    private final TableView<PairedEntry> leftTable = new TableView<>();
    private final TableView<PairedEntry> rightTable = new TableView<>();

    private final ObservableList<PairedEntry> items = FXCollections.observableArrayList();

    private Button copyBtn;
    private Button moveBtn;

    private final ComboBox<String> historyCombo = new ComboBox<>();
    private final ObservableList<String> historyItems = FXCollections.observableArrayList();

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void start(Stage stage) {
        stage.setTitle("Folder Compare");

        // Initialize services
        historyService = new HistoryService();

        // Toolbar at top
        copyBtn = new Button("Copy", new Label(Constants.ICON_COPY_NEUTRAL));
        copyBtn.setContentDisplay(ContentDisplay.LEFT);
        moveBtn = new Button("Move", new Label(Constants.ICON_MOVE_NEUTRAL));
        moveBtn.setContentDisplay(ContentDisplay.LEFT);
        Button deleteBtn = new Button("Delete", new Label(Constants.ICON_TRASH));
        deleteBtn.setContentDisplay(ContentDisplay.LEFT);
        Button refreshBtn = new Button("Refresh", new Label(Constants.ICON_REFRESH));
        refreshBtn.setContentDisplay(ContentDisplay.LEFT);
        Button swapBtn = new Button("Swap", new Label(Constants.ICON_SWAP));
        swapBtn.setContentDisplay(ContentDisplay.LEFT);

        historyCombo.setPromptText("Recent");
        historyCombo.setItems(historyItems);
        // Load persisted history
        loadHistoryFromPrefs();
        historyCombo.setOnAction(e -> {
            String sel = historyCombo.getSelectionModel().getSelectedItem();
            if (sel != null && sel.contains(" \u2194 ")) { // left ↔ right
                String[] parts = sel.split(" \u2194 ", 2);
                if (parts.length == 2) {
                    leftPathField.setText(parts[0]);
                    rightPathField.setText(parts[1]);
                    refresh();
                }
            }
        });

        ToolBar toolBar = new ToolBar(copyBtn, moveBtn, deleteBtn, new Separator(), refreshBtn, swapBtn, new Label("History:"), historyCombo);

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
        // Allow multiple selection on either side
        leftTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        rightTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        HBox center = new HBox(10, leftPane, rightPane);
        center.setPadding(new Insets(10));
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);
        leftPane.setFillWidth(true);
        rightPane.setFillWidth(true);
        // Allow the center area (which contains the tables) to grow/shrink with the window height
        VBox.setVgrow(center, Priority.ALWAYS);

        leftTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rightTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Provide context menus on rows to set base folder when right-clicking directories
        leftTable.setRowFactory(tv -> {
            TableRow<PairedEntry> row = new TableRow<>();
            MenuItem setBase = new MenuItem("Set as base folder");
            ContextMenu menu = new ContextMenu(setBase);
            setBase.setOnAction(evt -> {
                PairedEntry pe = row.getItem();
                if (pe != null && pe.getLeft() != null && pe.getLeft().isDirectory()) {
                    String base = leftPathField.getText();
                    if (base == null || base.isBlank()) return;
                    leftPathField.setText(Path.of(base).resolve(pe.getLeft().getName()).toString());
                    refresh();
                }
            });
            row.itemProperty().addListener((obs, oldV, newV) -> {
                boolean enable = newV != null && newV.getLeft() != null && newV.getLeft().isDirectory();
                setBase.setDisable(!enable);
                row.setContextMenu(enable ? menu : null);
            });
            row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    row.setContextMenu(null);
                }
            });
            return row;
        });

        rightTable.setRowFactory(tv -> {
            TableRow<PairedEntry> row = new TableRow<>();
            MenuItem setBase = new MenuItem("Set as base folder");
            ContextMenu menu = new ContextMenu(setBase);
            setBase.setOnAction(evt -> {
                PairedEntry pe = row.getItem();
                if (pe != null && pe.getRight() != null && pe.getRight().isDirectory()) {
                    String base = rightPathField.getText();
                    if (base == null || base.isBlank()) return;
                    rightPathField.setText(Path.of(base).resolve(pe.getRight().getName()).toString());
                    refresh();
                }
            });
            row.itemProperty().addListener((obs, oldV, newV) -> {
                boolean enable = newV != null && newV.getRight() != null && newV.getRight().isDirectory();
                setBase.setDisable(!enable);
                row.setContextMenu(enable ? menu : null);
            });
            row.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    row.setContextMenu(null);
                }
            });
            return row;
        });

        // Wire toolbar actions
        refreshBtn.setOnAction(e -> refresh());
        swapBtn.setOnAction(e -> {
            String l = leftPathField.getText();
            leftPathField.setText(rightPathField.getText());
            rightPathField.setText(l);
            refresh();
        });
        copyBtn.setOnAction(e -> handleCopy());
        moveBtn.setOnAction(e -> handleMove());
        deleteBtn.setOnAction(e -> handleDelete());
        // Enable Copy/Move/Delete only if some row is selected on either side
        var noSelection = Bindings.isEmpty(leftTable.getSelectionModel().getSelectedItems())
                .and(Bindings.isEmpty(rightTable.getSelectionModel().getSelectedItems()));
        copyBtn.disableProperty().bind(noSelection);
        moveBtn.disableProperty().bind(noSelection);
        deleteBtn.disableProperty().bind(noSelection);

        // Selection exclusivity and icon update
        leftTable.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<PairedEntry>) c -> {
            if (!leftTable.getSelectionModel().getSelectedItems().isEmpty()) {
                rightTable.getSelectionModel().clearSelection();
            }
            updateCopyButtonIcon();
            updateMoveButtonIcon();
        });
        rightTable.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<PairedEntry>) c -> {
            if (!rightTable.getSelectionModel().getSelectedItems().isEmpty()) {
                leftTable.getSelectionModel().clearSelection();
            }
            updateCopyButtonIcon();
            updateMoveButtonIcon();
        });

        // Sync column widths between left and right tables
        if (leftTable.getColumns().size() == rightTable.getColumns().size()) {
            for (int i = 0; i < leftTable.getColumns().size(); i++) {
                TableColumn<PairedEntry, ?> lc = leftTable.getColumns().get(i);
                TableColumn<PairedEntry, ?> rc = rightTable.getColumns().get(i);
                lc.prefWidthProperty().bindBidirectional(rc.prefWidthProperty());
            }
        }

        // Initialize icon state
        updateCopyButtonIcon();
        updateMoveButtonIcon();

        VBox root = new VBox(toolBar, center);

        Scene scene = new Scene(root, 1200, 700);
        stage.setScene(scene);

        // Load application icons (supports both Mac and Windows)
        List<Image> appIcons = loadAppIcons();
        if (!appIcons.isEmpty()) {
            stage.getIcons().setAll(appIcons);
        }

        // Set minimum window height to the startup height (i.e., just below the tables at launch)
        stage.setOnShown(e -> stage.setMinHeight(stage.getHeight()));
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
        // Prefer a smaller initial width for Size column
        sizeCol.setPrefWidth(SIZE_COL_PREF_WIDTH);
        sizeCol.setMinWidth(SIZE_COL_MIN_WIDTH);
        sizeCol.setMaxWidth(140);
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
        // Prefer a smaller initial width for Size column
        sizeCol.setPrefWidth(SIZE_COL_PREF_WIDTH);
        sizeCol.setMinWidth(SIZE_COL_MIN_WIDTH);
        sizeCol.setMaxWidth(140);
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
            boolean accept = db.hasFiles() && db.getFiles().stream().anyMatch(File::isDirectory);
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

    private void updateCopyButtonIcon() {
        if (copyBtn == null) return;
        boolean leftSelected = !leftTable.getSelectionModel().getSelectedItems().isEmpty();
        boolean rightSelected = !rightTable.getSelectionModel().getSelectedItems().isEmpty();
        String icon = Constants.ICON_COPY_NEUTRAL;
        if (leftSelected && !rightSelected) {
            icon = Constants.ICON_ARROW_RIGHT;
        } else if (rightSelected && !leftSelected) {
            icon = Constants.ICON_ARROW_LEFT;
        }
        copyBtn.setGraphic(new Label(icon));
    }

    private void updateMoveButtonIcon() {
        if (moveBtn == null) return;
        boolean leftSelected = !leftTable.getSelectionModel().getSelectedItems().isEmpty();
        boolean rightSelected = !rightTable.getSelectionModel().getSelectedItems().isEmpty();
        String icon = "⇢"; // default neutral icon
        if (leftSelected && !rightSelected) {
            icon = "→";
        } else if (rightSelected && !leftSelected) {
            icon = "←";
        }
        moveBtn.setGraphic(new Label(icon));
    }

    private void handleCopy() {
        // Determine which side is active
        boolean leftActive = !leftTable.getSelectionModel().getSelectedItems().isEmpty();
        boolean rightActive = !rightTable.getSelectionModel().getSelectedItems().isEmpty();
        if (!leftActive && !rightActive) {
            System.out.println("[INFO] No selection to copy.");
            return;
        }
        if (leftActive && rightActive) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Select rows on only one side to copy.", ButtonType.OK);
            a.setHeaderText("Ambiguous selection");
            a.showAndWait();
            return;
        }

        boolean leftToRight = leftActive; // if left is active, we copy left->right; otherwise right->left
        List<PairedEntry> selected = leftActive
                ? new ArrayList<>(leftTable.getSelectionModel().getSelectedItems())
                : new ArrayList<>(rightTable.getSelectionModel().getSelectedItems());

        // Collect targets (files and folders)
        List<FileInfo> targets = new ArrayList<>();
        for (PairedEntry pe : selected) {
            FileInfo fi = leftToRight ? pe.getLeft() : pe.getRight();
            if (fi != null) {
                targets.add(fi);
            }
        }
        if (targets.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Nothing selected to copy.", ButtonType.OK);
            a.setHeaderText("Nothing to copy");
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

        long dirCount = targets.stream().filter(FileInfo::isDirectory).count();
        long fileCount = targets.size() - dirCount;

        String direction = leftToRight ? "left → right" : "right → left";
        String header = "Copy " + targets.size() + (targets.size() == 1 ? " item?" : " items?");
        String content = "From " + direction + "\n\n" +
                "Files: " + fileCount + "\n" +
                "Folders: " + dirCount + "\n\n" +
                "Existing files with the same name will be overwritten.";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Copy");
        confirm.setHeaderText(header);
        confirm.setContentText(content);
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        ButtonType result = confirm.showAndWait().orElse(ButtonType.NO);
        if (result != ButtonType.YES) {
            return;
        }

        int success = 0;
        int fail = 0;
        for (FileInfo fi : targets) {
            Path src = srcDir.resolve(fi.getName());
            Path dst = dstDir.resolve(fi.getName());
            try {
                if (fi.isDirectory()) {
                    FileOperations.copyRecursive(src, dst);
                } else {
                    Files.createDirectories(dst.getParent());
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                }
                success++;
                System.out.println("[INFO] Copied " + (leftToRight ? "left->right" : "right->left") + ": " + src + " -> " + dst);
            } catch (Exception ex) {
                fail++;
                System.out.println("[WARN] Copy failed for '" + src + "': " + ex.getMessage());
            }
        }

        if (fail > 0) {
            Alert done = new Alert(Alert.AlertType.INFORMATION, success + " copied, " + fail + " failed.", ButtonType.OK);
            done.setHeaderText("Copy completed with issues");
            done.showAndWait();
        }

        refresh();
    }

    private void handleMove() {
        // Determine which side is active
        boolean leftActive = !leftTable.getSelectionModel().getSelectedItems().isEmpty();
        boolean rightActive = !rightTable.getSelectionModel().getSelectedItems().isEmpty();
        if (!leftActive && !rightActive) {
            System.out.println("[INFO] No selection to move.");
            return;
        }
        if (leftActive && rightActive) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Select rows on only one side to move.", ButtonType.OK);
            a.setHeaderText("Ambiguous selection");
            a.showAndWait();
            return;
        }

        boolean leftToRight = leftActive; // if left is active, move left->right; otherwise right->left
        List<PairedEntry> selected = leftActive
                ? new ArrayList<>(leftTable.getSelectionModel().getSelectedItems())
                : new ArrayList<>(rightTable.getSelectionModel().getSelectedItems());

        // Collect targets (files and folders)
        List<FileInfo> targets = new ArrayList<>();
        for (PairedEntry pe : selected) {
            FileInfo fi = leftToRight ? pe.getLeft() : pe.getRight();
            if (fi != null) {
                targets.add(fi);
            }
        }
        if (targets.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Nothing selected to move.", ButtonType.OK);
            a.setHeaderText("Nothing to move");
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

        long dirCount = targets.stream().filter(FileInfo::isDirectory).count();
        long fileCount = targets.size() - dirCount;

        String direction = leftToRight ? "left → right" : "right → left";
        String header = "Move " + targets.size() + (targets.size() == 1 ? " item?" : " items?");
        String content = "From " + direction + "\n\n" +
                "Files: " + fileCount + "\n" +
                "Folders: " + dirCount + "\n\n" +
                "Warning: Existing files with the same name at the destination will be overwritten.\n" +
                "Items will be removed from the source after moving.";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Move");
        confirm.setHeaderText(header);
        confirm.setContentText(content);
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        ButtonType result = confirm.showAndWait().orElse(ButtonType.NO);
        if (result != ButtonType.YES) {
            return;
        }

        int success = 0;
        int fail = 0;
        for (FileInfo fi : targets) {
            Path src = srcDir.resolve(fi.getName());
            Path dst = dstDir.resolve(fi.getName());
            try {
                if (fi.isDirectory()) {
                    FileOperations.moveRecursive(src, dst);
                } else {
                    // Try direct move, fallback to copy+delete
                    try {
                        Files.createDirectories(dst.getParent());
                        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception ex) {
                        FileOperations.copyRecursive(src, dst);
                        FileOperations.deleteRecursive(src);
                    }
                }
                success++;
                System.out.println("[INFO] Moved " + (leftToRight ? "left->right" : "right->left") + ": " + src + " -> " + dst);
            } catch (Exception ex) {
                fail++;
                System.out.println("[WARN] Move failed for '" + src + "': " + ex.getMessage());
            }
        }

        if (fail > 0) {
            Alert done = new Alert(Alert.AlertType.INFORMATION, success + " moved, " + fail + " failed.", ButtonType.OK);
            done.setHeaderText("Move completed with issues");
            done.showAndWait();
        }

        refresh();
    }

    private void handleDelete() {
        // Determine which side is active
        boolean leftActive = !leftTable.getSelectionModel().getSelectedItems().isEmpty();
        boolean rightActive = !rightTable.getSelectionModel().getSelectedItems().isEmpty();
        if (!leftActive && !rightActive) {
            System.out.println("[INFO] No selection to delete.");
            return;
        }
        if (leftActive && rightActive) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Select rows on only one side to delete.", ButtonType.OK);
            a.setHeaderText("Ambiguous selection");
            a.showAndWait();
            return;
        }

        boolean deleteLeft = leftActive; // if left is active, delete on left; otherwise on right
        List<PairedEntry> selected = deleteLeft
                ? new ArrayList<>(leftTable.getSelectionModel().getSelectedItems())
                : new ArrayList<>(rightTable.getSelectionModel().getSelectedItems());

        if (selected.isEmpty()) {
            return;
        }

        String targetFolder = deleteLeft ? leftPathField.getText() : rightPathField.getText();
        Path targetDir = Path.of(targetFolder == null ? "" : targetFolder);
        if (!Files.isDirectory(targetDir)) {
            Alert a = new Alert(Alert.AlertType.WARNING, "The target path must be a valid directory.", ButtonType.OK);
            a.setHeaderText("Invalid folder");
            a.showAndWait();
            return;
        }

        // Collect targets with names and directory flag
        List<FileInfo> targets = new ArrayList<>();
        for (PairedEntry pe : selected) {
            FileInfo fi = deleteLeft ? pe.getLeft() : pe.getRight();
            if (fi != null) {
                targets.add(fi);
            }
        }
        if (targets.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Nothing to delete.", ButtonType.OK);
            a.setHeaderText("No items");
            a.showAndWait();
            return;
        }

        long dirCount = targets.stream().filter(FileInfo::isDirectory).count();
        long fileCount = targets.size() - dirCount;

        String header = "Delete " + targets.size() + (targets.size() == 1 ? " item?" : " items?");
        StringBuilder content = new StringBuilder();
        content.append(deleteLeft ? "From LEFT folder" : "From RIGHT folder").append(" (\"")
                .append(targetDir).append("\")\n\n");
        content.append("Files: ").append(fileCount).append("\n");
        content.append("Folders: ").append(dirCount).append("\n\n");
        content.append("This action will permanently delete the selected items.\n");

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(header);
        confirm.setContentText(content.toString());
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        ButtonType result = confirm.showAndWait().orElse(ButtonType.NO);
        if (result != ButtonType.YES) {
            return;
        }

        int success = 0;
        int fail = 0;
        for (FileInfo fi : targets) {
            Path p = targetDir.resolve(fi.getName());
            try {
                if (fi.isDirectory()) {
                    FileOperations.deleteRecursive(p);
                } else {
                    Files.deleteIfExists(p);
                }
                success++;
                System.out.println("[INFO] Deleted: " + p);
            } catch (Exception ex) {
                fail++;
                System.out.println("[WARN] Delete failed for '" + p + "': " + ex.getMessage());
            }
        }

        if (fail > 0) {
            Alert done = new Alert(Alert.AlertType.INFORMATION, success + " deleted, " + fail + " failed.", ButtonType.OK);
            done.setHeaderText("Delete completed with issues");
            done.showAndWait();
        }

        refresh();
    }


    private void addToHistoryIfValid(String leftPath, String rightPath) {
        try {
            String l = leftPath == null ? "" : leftPath.trim();
            String r = rightPath == null ? "" : rightPath.trim();
            // Only add when both values are non-blank (do not require directories to exist)
            if (l.isBlank() || r.isBlank()) return;
            String label = l + " \u2194 " + r; // left ↔ right
            // Only add if not already present (do not reorder existing)
            if (historyItems.contains(label)) return;
            historyItems.add(0, label);
            // Cap at 10
            if (historyItems.size() > 10) {
                historyItems.remove(10, historyItems.size());
            }
            saveHistoryToPrefs();
        } catch (Exception ignored) {
        }
    }

    private void loadHistoryFromPrefs() {
        historyItems.setAll(historyService.loadHistory());
        // Ensure cap at 10 for UI list as well
        if (historyItems.size() > Constants.MAX_HISTORY_ITEMS) {
            historyItems.remove(Constants.MAX_HISTORY_ITEMS, historyItems.size());
        }
    }

    private void saveHistoryToPrefs() {
        historyService.saveHistory(historyItems);
    }

    private void refresh() {
        String leftPath = leftPathField.getText() == null ? "" : leftPathField.getText().trim();
        String rightPath = rightPathField.getText() == null ? "" : rightPathField.getText().trim();

        Map<String, FileInfo> leftMap = DirectoryScanner.scanDir(leftPath);
        Map<String, FileInfo> rightMap = DirectoryScanner.scanDir(rightPath);

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

        // Update history (only when both are valid directories)
        addToHistoryIfValid(leftPath, rightPath);
    }


    @Override
    public void stop() {
        // Ensure history is persisted on application exit
        saveHistoryToPrefs();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
