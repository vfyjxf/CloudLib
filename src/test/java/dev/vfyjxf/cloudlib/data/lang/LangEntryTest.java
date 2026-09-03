package dev.vfyjxf.cloudlib.data.lang;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangEntryTest {

    @Test
    void entryIsAPureKeyReference() {
        LangEntry entry = LangEntry.of("cloudlib.ui.test.entry");

        assertEquals("cloudlib.ui.test.entry", entry.key());
        assertEquals("cloudlib.ui.test.entry", entry.string());
        assertTrue(entry.component().getContents() instanceof TranslatableContents);
        TranslatableContents contents = (TranslatableContents) entry.component().getContents();
        assertEquals("cloudlib.ui.test.entry", contents.getKey());
    }

    @Test
    void suffixCreatesChainedReference() {
        LangEntry entry = LangEntry.of("cloudlib.ui.test.parent");
        LangEntry child = entry.suffix("desc");

        assertEquals("cloudlib.ui.test.parent.desc", child.key());
        assertSame(entry, entry.suffix(" "));
    }

}
