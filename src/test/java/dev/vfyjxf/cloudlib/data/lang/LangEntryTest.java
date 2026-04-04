package dev.vfyjxf.cloudlib.data.lang;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangEntryTest {

    @Test
    void referenceCreatesKeyOnlyEntry() {
        LangEntry entry = LangEntry.reference("cirrus.ui.test.entry");

        assertEquals("cirrus.ui.test.entry", entry.key());
        assertNull(entry.value());
        assertFalse(entry.hasDefaultValue());
        assertEquals("cirrus.ui.test.entry", entry.string());
        assertTrue(entry.component().getContents() instanceof TranslatableContents);
        TranslatableContents contents = (TranslatableContents) entry.component().getContents();
        assertEquals("cirrus.ui.test.entry", contents.getKey());
    }

    @Test
    void builderEntryKeepsDefaultValueAndCanCreateSuffixReferences() {
        LangEntry entry = new LangEntry("cirrus.ui.test.parent", "Parent");
        LangEntry child = entry.suffix("desc");

        assertTrue(entry.hasDefaultValue());
        assertEquals("cirrus.ui.test.parent.desc", child.key());
        assertFalse(child.hasDefaultValue());
    }
}
