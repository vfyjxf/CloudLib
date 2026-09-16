package dev.vfyjxf.cloudlib.internal.ui.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StyleWatcherTest {

    @Test
    void themeFilesAreWatched() {
        assertTrue(StyleWatcher.isThemeFile("base.css"));
        assertTrue(StyleWatcher.isThemeFile("theme.json"));
        assertFalse(StyleWatcher.isThemeFile("pack.mcmeta"));
        assertFalse(StyleWatcher.isThemeFile("readme.md"));
        assertFalse(StyleWatcher.isThemeFile("other.json"));
        assertFalse(StyleWatcher.isThemeFile("base.css.bak"));
    }
}
