package net.parksy.foldercompare.prefs;

import net.parksy.foldercompare.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages persistence of the left/right folder history.
 */
public class HistoryService {
    private final Preferences prefs;

    public HistoryService() {
        this.prefs = Preferences.userRoot().node(Constants.PREF_NODE);
    }

    public List<String> loadHistory() {
        int count = prefs.getInt(Constants.PREF_HISTORY_COUNT, 0);
        List<String> res = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String val = prefs.get(Constants.PREF_HISTORY_PREFIX + i, null);
            if (val != null && !val.isBlank()) {
                res.add(val);
            }
        }
        return res;
    }

    public void saveHistory(List<String> items) {
        int count = Math.min(items == null ? 0 : items.size(), Constants.MAX_HISTORY_ITEMS);
        prefs.putInt(Constants.PREF_HISTORY_COUNT, count);
        for (int i = 0; i < count; i++) {
            prefs.put(Constants.PREF_HISTORY_PREFIX + i, items.get(i));
        }
        // clear remnants
        for (int i = count; i < Constants.MAX_HISTORY_ITEMS; i++) {
            prefs.remove(Constants.PREF_HISTORY_PREFIX + i);
        }
    }
}
