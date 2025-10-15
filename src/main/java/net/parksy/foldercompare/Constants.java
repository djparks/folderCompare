package net.parksy.foldercompare;

import java.time.format.DateTimeFormatter;

/**
 * Centralized constants to avoid magic strings/numbers scattered in the code.
 */
public final class Constants {
    private Constants() {}

    // Preferences / history
    public static final String PREF_NODE = "net.parksy.foldercompare";
    public static final String PREF_HISTORY_COUNT = "history.count";
    public static final String PREF_HISTORY_PREFIX = "history.";
    public static final int MAX_HISTORY_ITEMS = 10;

    // UI symbols
    public static final String ICON_COPY_NEUTRAL = "⧉";
    public static final String ICON_MOVE_NEUTRAL = "⇢";
    public static final String ICON_ARROW_RIGHT = "→";
    public static final String ICON_ARROW_LEFT = "←";
    public static final String ICON_REFRESH = "↻";
    public static final String ICON_SWAP = "⇄";
    public static final String ICON_TRASH = "🗑";

    // Formatting
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
